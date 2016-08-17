package fr.ign.georeden.algorithms.graph.matching;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
//import org.apache.log4j.Logger
/**
 *  The <tt>DijkstraSP</tt> class represents a data type for solving the
 *  single-source shortest paths problem in edge-weighted digraphs
 *  where the edge weights are nonnegative.
 *  <p>
 *  This implementation uses Dijkstra's algorithm with a binary heap.
 *  The constructor takes time proportional to <em>E</em> log <em>V</em>,
 *  where <em>V</em> is the number of vertices and <em>E</em> is the number of edges.
 *  Afterwards, the <tt>distTo()</tt> and <tt>hasPathTo()</tt> methods take
 *  constant time and the <tt>pathTo()</tt> method takes time proportional to the
 *  number of edges in the shortest path returned.
 *  <p>
 *  For additional documentation,    
 *  see <a href="http://algs4.cs.princeton.edu/44sp">Section 4.4</a> of    
 *  <i>Algorithms, 4th Edition</i> by Robert Sedgewick and Kevin Wayne. 
 *
 *  @author Robert Sedgewick
 *  @author Kevin Wayne
 */

public class DijkstraSP implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
    private transient double weight = 1.0;
	//private static Logger logger = Logger.getLogger(DijkstraSP.class)
    private Map<String, Double> distTo;          // distTo[v] = distance  of shortest s->v path
    private Map<String, SerializableStatement> edgeTo;    // edgeTo[v] = last edge on shortest s->v path
    private transient IndexMinPQ<Double> pq;    // priority queue of vertices
    private transient Map<RDFNode, Integer> nodes; // nodes of the graph
    private transient Map<Integer, RDFNode> nodesIndexs; // indexes of nodes of the graph
    private transient Map<RDFNode, List<Statement>> statementsByNodes; // indexes of nodes of the graph
    private String node;

    /**
     * Computes a shortest-paths tree from the source vertex <tt>s</tt> to every other
     * vertex in the edge-weighted digraph <tt>G</tt>.
     *
     * @param graph the graph
     * @param s the s
     * @param nodesToVisitBeforeStop the nodes to visit before stop
     * @throws IllegalArgumentException unless 0 &le; <tt>s</tt> &le; <tt>V</tt> - 1
     */
    @Deprecated
    public DijkstraSP(Model graph, RDFNode s, Set<RDFNode> nodesToVisitBeforeStop) {
//    	node = s.toString();
//    	Set<Integer> indexesToVisitBeforeStop = new HashSet<>(); // utilisées pour savoir quelle index on doit visiter avant d'arrêter
//    	Set<Integer> indexesVisited = new HashSet<>(); // utilisées pour savoir quelle index on doit visiter avant d'arrêter
//    	Set<String> set = new HashSet<>(graph.listSubjects().toList().stream().map(m -> m.toString()).collect(Collectors.toList()));
//    	set.addAll(graph.listObjects().toList().stream().map(m -> m.toString()).collect(Collectors.toList()));
//    	nodes = new ArrayList<>();//set
//        distTo = new HashMap<>();
//        edgeTo = new HashMap<>();
//        for (int v = 0; v < nodes.size(); v++) {
//            distTo.put(nodes.get(v), Double.POSITIVE_INFINITY);
//            if (nodesToVisitBeforeStop.contains(nodes.get(v)))
//            	indexesToVisitBeforeStop.add(v);
//        }
//        distTo.put(node, 0.0);
//
//        // relax vertices in order of distance from s
//        pq = new IndexMinPQ<>(nodes.size());
//        pq.insert(nodes.indexOf(node), distTo.get(node));
//        while (!pq.isEmpty()) {
//            int v = pq.delMin();
//            indexesVisited.add(v);
//            if (indexesVisited.containsAll(indexesToVisitBeforeStop))
//            	break;
//            for (Statement e : graph.listStatements(graph.getResource(nodes.get(v)), null, (RDFNode)null).toList())
//                relax(e);
//        }
    }
    
	/**
	 * Instantiates a new Dijkstra SP.
	 *
	 * @param graph the graph
	 * @param s the s
	 */
	public DijkstraSP(Model graph, RDFNode s) {
    	node = s.toString();
    	// on récupère tous les noeuds objets d'un statement
		Set<RDFNode> set = new HashSet<>(graph.listObjects().toList());
				//.stream().map(m -> m.toString()).collect(Collectors.toList()));
//				graph.listSubjects().toList().stream().map(m -> m.toString()).collect(Collectors.toList()));
		//set.addAll(graph.listSubjects().toList());
		this.nodes = new HashMap<>();
		this.nodesIndexs = new HashMap<>();
		int ind = 0;
		for (RDFNode rdfNode : set) {
			nodes.put(rdfNode, ind);
			nodesIndexs.put(ind, rdfNode);
			ind++;
		}
		this.statementsByNodes = new HashMap<>();
		for (Statement statement : graph.listStatements().toList()) {
			Resource subject = statement.getSubject();
			List<Statement> statements = statementsByNodes.containsKey(subject) ? statementsByNodes.get(subject) : new ArrayList<>();
			statements.add(statement);
			statementsByNodes.put(subject, statements);
		}
		
		distTo = new HashMap<>();
		edgeTo = new HashMap<>();
//		for (int v = 0; v < nodes.size(); v++) {
//			distTo.put(nodes.get(v), Double.POSITIVE_INFINITY);
//		}
		distTo.put(node, 0.0);

		// relax vertices in order of distance from s
		pq = new IndexMinPQ<>(nodes.size());
		pq.insert(nodes.get(s), distTo.get(node));
		while (!pq.isEmpty()) {
			int v = pq.delMin();
			//graph.listStatements((Resource)nodes.get(v), null, (RDFNode) null).toList().parallelStream().forEach(e -> relax(e))
			RDFNode minNode = nodesIndexs.get(v);
			List<Statement> statements = statementsByNodes.get(minNode);
			if (statements != null) {
				for (Statement e : statements) {//graph.listStatements((Resource)nodesIndexs.get(v), null, (RDFNode) null).toList())
					relax(e);
				}
			}
		}
	}
	
//	private double calculateAverage(List<Long> marks) {
//		double sum = 0;
//		  if(!marks.isEmpty()) {
//		    for (long mark : marks) {
//		        sum += mark;
//		    }
//		    return sum / marks.size();
//		  }
//		  return sum;
//		}
    // relax edge e and update pq if changed
    private void relax(Statement e) {
        RDFNode v = e.getSubject();
        RDFNode w = e.getObject();
        // si w n'est pas dans distTo (ça valeur est donc infini) ou si sa distance est plus grande, on la met à jour
        if (!distTo.containsKey(w.toString()) || distTo.get(w.toString()) > distTo.get(v.toString()) + weight) {
            distTo.put(w.toString(), distTo.get(v.toString()) + weight);
            edgeTo.put(w.toString(), new SerializableStatement(e));
            if (pq.contains(nodes.get(w))) 
            	pq.decreaseKey(nodes.get(w), distTo.get(w.toString()));
            else
            	pq.insert(nodes.get(w), distTo.get(w.toString()));
        }
    }

	public Resource getNode(Model source) {
		return source.getResource(this.node);
	}
    /**
     * Returns the length of a shortest path from the source vertex <tt>s</tt> to vertex <tt>v</tt>.
     * @param  v the destination vertex
     * @return the length of a shortest path from the source vertex <tt>s</tt> to vertex <tt>v</tt>;
     *         <tt>Double.POSITIVE_INFINITY</tt> if no such path
     */
    public double distTo(RDFNode v) {
        return distTo.get(v.toString());
    }

    /**
     * Returns true if there is a path from the source vertex <tt>s</tt> to vertex <tt>v</tt>.
     *
     * @param  v the destination vertex
     * @return <tt>true</tt> if there is a path from the source vertex
     *         <tt>s</tt> to vertex <tt>v</tt>; <tt>false</tt> otherwise
     */
    public boolean hasPathTo(RDFNode v) {
        return distTo.containsKey(v.toString()) && distTo.get(v.toString()) < Double.POSITIVE_INFINITY;
    }

    /**
     * Returns a shortest path from the source vertex <tt>s</tt> to vertex <tt>v</tt>.
     *
     * @param v the v
     * @param graph the graph
     * @return a shortest path from the source vertex <tt>s</tt> to vertex <tt>v</tt>
     *         as an iterable of edges, and <tt>null</tt> if no such path
     */
    public Iterable<Statement> pathTo(RDFNode v, Model graph) {
        if (!hasPathTo(v)) 
        	return null;
        Stack<Statement> path = new Stack<>();
        for (SerializableStatement e = edgeTo.get(v.toString()); e != null; e = edgeTo.get(e.getSubject())) {
            path.push(e.toStatement(graph));
        }
        return path;
    }
    
    public void serialize(String path) {
		try (FileOutputStream fout = new FileOutputStream(path)) {

			try (ObjectOutputStream oos = new ObjectOutputStream(fout)) {
				oos.writeObject(this);
			}

		} catch (Exception e) {
		}
	}
   

}

