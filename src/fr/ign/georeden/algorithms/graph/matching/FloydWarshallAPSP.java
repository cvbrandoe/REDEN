package fr.ign.georeden.algorithms.graph.matching;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.log4j.Logger;

/**
 * The Class FloydWarshall (All Pairs Shortest Path).
 */
public class FloydWarshallAPSP implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger(FloydWarshallAPSP.class);
	
	private Map<String, Map<String, Short>> dist;
	private Map<String, Map<String, SerializableStatement>> next; // supprimer le transient une fois Statement remplacé par StatementSerializable
	
	private transient List<Statement> statements;
	private transient Set<String> nodes;
	private int nbVertex;
	
	/**
	 * Instantiates a new floyd warshall APSP.
	 *
	 * @param model the model
	 */
	public FloydWarshallAPSP(Model model) {
		logger.info("Algo Floyd-Warshall");
		logger.info("Récupération des statements");
		this.statements = model.listStatements().toList();// statements = tous les statements du graph dans lequel on veut les chemins
		logger.info("Number of statements : " + statements.size());
		this.nodes = new HashSet<>(model.listSubjects().toList().stream().map(m -> m.toString()).collect(Collectors.toList()));
		//this.nodes.addAll(model.listObjects().toList().stream().map(m -> m.toString()).collect(Collectors.toList()));		
		this.nbVertex = nodes.size(); 
		logger.info("Number of vertex : " + this.nbVertex);
		this.dist = new ConcurrentHashMap<>(nbVertex);
		this.next = new ConcurrentHashMap<>();
		createStructures();
	}
	
	private void createStructures() {
		logger.info("Création de la structure");
//		Map<String, Map<String, Short>> distTmp = new ConcurrentHashMap<>(nbVertex);
//		Map<String, Map<String, SerializableStatement>> nextTmp = new ConcurrentHashMap<>();
//		nodes.parallelStream().forEach(n1 -> {
//			Map<String, Short> mapSecondNode = new ConcurrentHashMap<>();
//			mapSecondNode.put(n1, (short) 0);
//			dist.put(n1, mapSecondNode);
//		});

		logger.info("Initialisation des voisins");
		// on initialise les voisins à 1
		statements.stream().forEach(s -> {
			String n1 = s.getSubject().toString();
			String n2 = s.getObject().toString();
			Map<String, Short> mapToChange = dist.containsKey(n1) ? dist.get(n1) : new ConcurrentHashMap<>();
			mapToChange.put(n2, (short)1);
			dist.put(n1, mapToChange);
			Map<String, SerializableStatement> mapNext = next.containsKey(n1) ? next.get(n1) : new ConcurrentHashMap<>();
			mapNext.put(n2, new SerializableStatement(s));
			next.put(n1, mapNext);
		});
//		this.dist.putAll(distTmp);
//		this.next.putAll(nextTmp);
	}
	
	/**
	 * Compute the algorithm.
	 */
	void compute() {		
		int k = 1;		
		
		for (String nodeK : dist.keySet()) {
			logger.info("étape " + k + " sur " + nbVertex);
			dist.keySet().parallelStream().forEach(nodeI -> {
				if (next.containsKey(nodeK) && next.get(nodeK).containsKey(nodeI)) {// optimization
					for (String nodeJ : dist.keySet()) {
						if (dist.containsKey(nodeI) && dist.containsKey(nodeK) && dist.get(nodeI).containsKey(nodeK) && dist.get(nodeK).containsKey(nodeJ)) {// cela veut dire que dist[i][k] et dist[k][j] ont une valeur différente de l'infini, donc on regarde							
							short value = (short) (dist.get(nodeI).get(nodeK) + dist.get(nodeK).get(nodeJ));
							if (!dist.get(nodeI).containsKey(nodeJ) || dist.get(nodeI).get(nodeJ) > value) {// cela veut dire que dist[i][j] est à l'infini. Donc updater la valeur est forcément intéressant ou que dist[i][j] n'est pas à l'infini, mais qd meme plus élevé que value								
								Map<String, Short> mapToChange = dist.get(nodeI);
								mapToChange.put(nodeJ, value);
								Map<String, SerializableStatement> nextTochange = next.get(nodeI);
								SerializableStatement ss = null;
								if (next.get(nodeK).containsKey(nodeJ)) {
									ss = next.get(nodeK).get(nodeJ);
								} else if (next.get(nodeI).containsKey(nodeK)) {
									ss = next.get(nodeI).get(nodeK);
								}
								if (ss != null)
									nextTochange.put(nodeJ, ss);
							}
						}
					}
				}
			});
			k++;
		}
	}
	
	/**
	 * Checks if a path exists between two nodes.
	 *
	 * @param start the start
	 * @param end the end
	 * @return true, if successful
	 */
	public boolean hasPath(RDFNode start, RDFNode end) {
		if (start == null || end == null || start.toString().isEmpty() || end.toString().isEmpty())
			return false;
		String startString = start.toString();
		String endString = end.toString();
		return dist.containsKey(startString) 
				&& dist.get(startString).containsKey(endString) 
				&& dist.get(startString).get(endString) < Short.MAX_VALUE;
	}
	
	/**
	 * Gets the path between the two nodes if it exists.
	 *
	 * @param start the start
	 * @param end the end
	 * @param graph the graph
	 * @return the path
	 */
	public Iterable<Statement> getPath(RDFNode start, RDFNode end, Model graph) {
		if (!hasPath(start, end)) 
			return null;
		Stack<Statement> path = new Stack<>();
        for (SerializableStatement e = next.get(start.toString()).get(end.toString()); e != null; e = next.get(start.toString()).get(e.getSubject())) {
            path.push(e.toStatement(graph));
        }
		return path;
	}
	
	/**
	 * Serialize.
	 *
	 * @param path the path
	 */
	public void serialize(String path) {
		try (FileOutputStream fout = new FileOutputStream(path)) {

			try (ObjectOutputStream oos = new ObjectOutputStream(fout)) {
				oos.writeObject(this);
			}
			logger.info("Serialization done !");

		} catch (Exception e) {
			logger.error(e);
		}
	}
	
	/**
	 * Deserialize.
	 *
	 * @param path the path
	 * @return the floyd warshall APSP
	 */
	public static FloydWarshallAPSP deserialize(String path) {
		FloydWarshallAPSP result = null;
		try (FileInputStream fis = new FileInputStream(path)) {
			try (ObjectInputStream objectInputStream = new ObjectInputStream(fis)) {
				result = (FloydWarshallAPSP) objectInputStream.readObject();
			} catch (ClassNotFoundException e) {
				logger.error(e);
			}
		} catch (IOException e) {
			logger.error(e);
		}
		return result;
	}
	
}
