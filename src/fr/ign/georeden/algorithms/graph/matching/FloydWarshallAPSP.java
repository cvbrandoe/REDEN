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
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.jena.ontology.OntTools.Path;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.log4j.Logger;

public class FloydWarshallAPSP implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger(FloydWarshallAPSP.class);
	private Map<String, Map<String, Float>> dist;
	private transient List<Statement> statements;
	private transient Set<String> nodes;
	private int nbVertex;
	
	public FloydWarshallAPSP(Model model) {
		logger.info("Algo Floyd-Warshall");
		logger.info("Récupération des statements");
		this.statements = model.listStatements().toList();// statements = tous les statements du graph dans lequel on veut les chemins
		this.nodes = new HashSet<>(model.listSubjects().toList().stream().map(m -> m.toString()).collect(Collectors.toList()));
		this.nodes.addAll(model.listObjects().toList().stream().map(m -> m.toString()).collect(Collectors.toList()));
		this.nbVertex = nodes.size(); 
		this.dist = new ConcurrentHashMap<>(nbVertex);
		this.next = new ConcurrentHashMap<>(nbVertex);
		createStructures();
	}
	
	private void createStructures() {
		logger.info("Création de la structure");
		nodes.parallelStream().forEach(n1 -> {
			Map<String, Float> mapSecondNode = new ConcurrentHashMap<>();
			mapSecondNode.put(n1, 0.0f);
			dist.put(n1, mapSecondNode);
		});
		
		// !!!!!!!!!!! Pour prendre moins de place, on ne met que quand pas infini
//		Map<String, Float> mapSecondNode = new ConcurrentHashMap<>(nbVertex);
//		logger.info("Création de la structure 1");
//		nodes.parallelStream().forEach(n2 -> mapSecondNode.put(n2, Float.POSITIVE_INFINITY));
//		logger.info("Création de la structure 2");
//		nodes.parallelStream().forEach(n1 -> {
//			Map<String, Float> mapToAdd = new HashMap<>(mapSecondNode);
//			mapToAdd.put(n1, 0.0f);
//			dist.put(n1, mapToAdd);
//		});

		logger.info("Initialisation des voisins");
		// on initialise les voisins à 1
		statements.parallelStream().forEach(s -> {
			String n1 = s.getSubject().toString();
			String n2 = s.getObject().toString();
			Map<String, Float> mapToChange = dist.get(n1);
			mapToChange.put(n2, 1.0f);
			Map<String, String> mapNext = new HashMap<>();
			mapNext.put(n2, n2);
			next.put(n1, mapNext);
		});
	}
	void compute() {
		/*
		  	1 let dist be a |V| × |V| array of minimum distances initialized to ∞ (infinity)
			2 for each vertex v
			3    dist[v][v] ← 0
			4 for each edge (u,v)
			5    dist[u][v] ← w(u,v)  // the weight of the edge (u,v)
			6 for k from 1 to |V|
			7    for i from 1 to |V|
			8       for j from 1 to |V|
			9          if dist[i][j] > dist[i][k] + dist[k][j] 
			10             dist[i][j] ← dist[i][k] + dist[k][j]
			11         end if
		*/
		
		int k = 1;
		for (String nodeK : dist.keySet()) {
			logger.info("étape " + k + " sur " + nbVertex);
			for (String nodeI : dist.keySet()) {
				for (String nodeJ : dist.keySet()) {
					if (dist.get(nodeI).containsKey(nodeK) && dist.get(nodeK).containsKey(nodeJ)) {
						// cela veut dire que dist[i][k] et dist[k][j] ont une valeur différente de l'infini, donc on regarde
						float value = dist.get(nodeI).get(nodeK) + dist.get(nodeK).get(nodeJ);
						if (!dist.get(nodeI).containsKey(nodeJ) || dist.get(nodeI).get(nodeJ) > value) {
							// cela veut dire que dist[i][j] est à l'infini. Donc updater la valeur est forcément intéressant
							// ou que dist[i][j] n'est pas à l'infini, mais qd meme plus élevé que value
							Map<String, Float> mapToChange = dist.get(nodeI);
							mapToChange.put(nodeJ, value);
							Map<String, String> nextTochange = next.get(nodeI);
							if (nextTochange.containsKey(nodeK)) 
								nextTochange.put(nodeJ, nextTochange.get(nodeK));
						}
					}
				}
			}
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
				&& dist.get(startString).get(endString) < Float.POSITIVE_INFINITY;
	}
	
	public Path getPath(RDFNode start, RDFNode end) {
		/* A AJOUTER DANS LA FONCTION compute §§§§§§§§§§§§§
		 * procedure FloydWarshallWithPathReconstruction ()
			   for each edge (u,v)
			      dist[u][v] ← w(u,v)  // the weight of the edge (u,v)
			      next[u][v] ← v §§§§§§§§§§§§§§§§§§§§§§§§§
			   for k from 1 to |V| // standard Floyd-Warshall implementation
			      for i from 1 to |V|
			         for j from 1 to |V|
			            if dist[i][k] + dist[k][j] < dist[i][j] then
			               dist[i][j] ← dist[i][k] + dist[k][j]
			               next[i][j] ← next[i][k] §§§§§§§§§§§§§§§§§§§§§§§
		 * */
		Path path = null;
		if (hasPath(start, end)) {
			path = new Path();
		}
		return path;
		
		/*procedure Path(u, v)
		   if next[u][v] = null then
		       return []
		   path = [u]
		   while u ≠ v
		       u ← next[u][v]
		       path.append(u)
		   return path
		 */
	}
	private Map<String, Map<String, String>> next; // supprimer le transient une fois Statement remplacé par StatementSerializable
	
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
