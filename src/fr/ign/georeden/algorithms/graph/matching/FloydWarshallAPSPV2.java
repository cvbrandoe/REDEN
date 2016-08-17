package fr.ign.georeden.algorithms.graph.matching;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.log4j.Logger;

/**
 * The Class FloydWarshall (All Pairs Shortest Path).
 */
public class FloydWarshallAPSPV2 implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger(FloydWarshallAPSP.class);
	
	private short[][] dist;
	private SerializableStatement[][] next; // supprimer le transient une fois Statement remplacé par StatementSerializable
	
	private transient List<Statement> statements;
	private List<String> nodes;
	private int nbVertex;
	
	/**
	 * Instantiates a new floyd warshall APSP.
	 *
	 * @param model the model
	 */
	public FloydWarshallAPSPV2(Model model) {
		logger.info("Algo Floyd-Warshall");
		logger.info("Récupération des statements");
		this.statements = model.listStatements().toList();// statements = tous les statements du graph dans lequel on veut les chemins
		logger.info("Number of statements : " + statements.size());
		this.nodes = new ArrayList<>(new HashSet<>(model.listSubjects().toList().stream().map(m -> m.toString()).collect(Collectors.toList())));
		//this.nodes.addAll(model.listObjects().toList().stream().map(m -> m.toString()).collect(Collectors.toList()));		
		this.nbVertex = nodes.size(); 
		logger.info("Number of vertex : " + this.nbVertex);
		this.dist = new short[nbVertex][nbVertex];
		this.next = new SerializableStatement[nbVertex][nbVertex];
		createStructures();
	}
	
	private void createStructures() {
		logger.info("Création de la structure");
		Arrays.fill(dist, Short.MAX_VALUE);
		Arrays.fill(next, null);
		
		nodes.parallelStream().forEach(n1 -> {
			int index = nodes.indexOf(n1);
			dist[index][index] = (short) 0;
		});

		logger.info("Initialisation des voisins");
		// on initialise les voisins à 1
		statements.stream().forEach(s -> {
			String n1 = s.getSubject().toString();
			String n2 = s.getObject().toString();
			int indexN1 = nodes.indexOf(n1);
			int indexN2 = nodes.indexOf(n2);
			dist[indexN1][indexN2] = (short)1;
			next[indexN1][indexN2] = new SerializableStatement(s);
		});
	}
	
	/**
	 * Compute the algorithm.
	 */
	void compute() {		
		int count = 1;		
		
		for (int k = 0; k < nbVertex; k++) {
			logger.info("étape " + count + " sur " + nbVertex);
			final int kFinal = k;
			IntStream.range(0, nbVertex).parallel().forEach(i -> {
				if (next[kFinal][i] != null) {// optimization
					for (int j = 0; j < nbVertex; j++) {
						if (dist[i][kFinal] < Short.MAX_VALUE && dist[kFinal][j] < Short.MAX_VALUE) {
							// cela veut dire que dist[i][k] et dist[k][j] ont une valeur différente de l'infini, donc on regarde
							short value = (short) (dist[i][kFinal] + dist[kFinal][j]);
							if (dist[i][j] > value) {
								// cela veut dire que dist[i][j] est à l'infini. Donc updater la valeur est forcément intéressant
								// ou que dist[i][j] n'est pas à l'infini, mais qd meme plus élevé que value
								dist[i][j] = value;
								next[i][j] = next[kFinal][j];
							}
						}
					}
				}
			});
			count++;
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
		int indexN1 = nodes.indexOf(startString);
		int indexN2 = nodes.indexOf(endString);
		return dist[indexN1][indexN2] < Short.MAX_VALUE;
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
		String startString = start.toString();
		String endString = end.toString();
		int indexN1 = nodes.indexOf(startString);
		int indexN2 = nodes.indexOf(endString);
        for (SerializableStatement e = next[indexN1][indexN2]; e != null; e = next[indexN1][nodes.indexOf(e.getSubject().toString())]) {
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
	public static FloydWarshallAPSPV2 deserialize(String path) {
		FloydWarshallAPSPV2 result = null;
		try (FileInputStream fis = new FileInputStream(path)) {
			try (ObjectInputStream objectInputStream = new ObjectInputStream(fis)) {
				result = (FloydWarshallAPSPV2) objectInputStream.readObject();
			} catch (ClassNotFoundException e) {
				logger.error(e);
			}
		} catch (IOException e) {
			logger.error(e);
		}
		return result;
	}
	
}
