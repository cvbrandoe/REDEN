package fr.ign.georeden.algorithms.graph.matching;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.log4j.Logger;
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

public class DijkstraSP {
	private static Logger logger = Logger.getLogger(DijkstraSP.class);
    private Map<RDFNode, Double> distTo;          // distTo[v] = distance  of shortest s->v path
    private Map<RDFNode, Statement> edgeTo;    // edgeTo[v] = last edge on shortest s->v path
    private IndexMinPQ<Double> pq;    // priority queue of vertices
    private List<RDFNode> nodes; // nodes of the graph

    /**
     * Computes a shortest-paths tree from the source vertex <tt>s</tt> to every other
     * vertex in the edge-weighted digraph <tt>G</tt>.
     *
     * @param  G the edge-weighted digraph
     * @param  s the source vertex
     * @throws IllegalArgumentException if an edge weight is negative
     * @throws IllegalArgumentException unless 0 &le; <tt>s</tt> &le; <tt>V</tt> - 1
     */
    public DijkstraSP(Model G, RDFNode s, Set<RDFNode> nodesToVisitBeforeStop) {
//        for (Statement e : G.edges()) {
//            if (e.weight() < 0)
//                throw new IllegalArgumentException("edge " + e + " has negative weight");
//        }
    	Set<Integer> indexesToVisitBeforeStop = new HashSet<>(); // utilisées pour savoir quelle index on doit visiter avant d'arrêter
    	Set<Integer> indexesVisited = new HashSet<>(); // utilisées pour savoir quelle index on doit visiter avant d'arrêter
    	Set<RDFNode> set = new HashSet<>(G.listSubjects().toList());
    	set.addAll(G.listObjects().toList());
    	nodes = new ArrayList<>(set);
        distTo = new HashMap<>();
        edgeTo = new HashMap<>();
        for (int v = 0; v < nodes.size(); v++) {
            distTo.put(nodes.get(v), Double.POSITIVE_INFINITY);
            if (nodesToVisitBeforeStop.contains(nodes.get(v)))
            	indexesToVisitBeforeStop.add(v);
        }
        distTo.put(s, 0.0);

        // relax vertices in order of distance from s
        pq = new IndexMinPQ<Double>(nodes.size());
        pq.insert(nodes.indexOf(s), distTo.get(s));
        while (!pq.isEmpty()) {
            int v = pq.delMin();
            indexesVisited.add(v);
            if (indexesVisited.containsAll(indexesToVisitBeforeStop))
            	break;
            for (Statement e : G.listStatements((Resource)nodes.get(v), null, (RDFNode)null).toList())
                relax(e);
        }

//        // check optimality conditions
//        assert check(G, s);
    }
    private double weight = 1.0;
    // relax edge e and update pq if changed
    private void relax(Statement e) {
        RDFNode v = e.getSubject(), w = e.getObject();
        if (distTo.get(w) > distTo.get(v) + weight) {
            distTo.put(w, distTo.get(v) + weight);
            edgeTo.put(w, e);
            if (pq.contains(nodes.indexOf(w))) pq.decreaseKey(nodes.indexOf(w), distTo.get(w));
            else                pq.insert(nodes.indexOf(w), distTo.get(w));
        }
    }

    /**
     * Returns the length of a shortest path from the source vertex <tt>s</tt> to vertex <tt>v</tt>.
     * @param  v the destination vertex
     * @return the length of a shortest path from the source vertex <tt>s</tt> to vertex <tt>v</tt>;
     *         <tt>Double.POSITIVE_INFINITY</tt> if no such path
     */
    public double distTo(RDFNode v) {
        return distTo.get(v);
    }

    /**
     * Returns true if there is a path from the source vertex <tt>s</tt> to vertex <tt>v</tt>.
     *
     * @param  v the destination vertex
     * @return <tt>true</tt> if there is a path from the source vertex
     *         <tt>s</tt> to vertex <tt>v</tt>; <tt>false</tt> otherwise
     */
    public boolean hasPathTo(RDFNode v) {
        return distTo.get(v) < Double.POSITIVE_INFINITY;
    }

    /**
     * Returns a shortest path from the source vertex <tt>s</tt> to vertex <tt>v</tt>.
     *
     * @param  v the destination vertex
     * @return a shortest path from the source vertex <tt>s</tt> to vertex <tt>v</tt>
     *         as an iterable of edges, and <tt>null</tt> if no such path
     */
    public Iterable<Statement> pathTo(RDFNode v) {
        if (!hasPathTo(v)) 
        	return null;
        Stack<Statement> path = new Stack<>();
        for (Statement e = edgeTo.get(v); e != null; e = edgeTo.get(e.getSubject())) {
            path.push(e);
        }
        return path;
    }


    // check optimality conditions:
    // (i) for all edges e:            distTo[e.to()] <= distTo[e.from()] + e.weight()
    // (ii) for all edge e on the SPT: distTo[e.to()] == distTo[e.from()] + e.weight()
    private boolean check(Model G, RDFNode s) {

//        // check that edge weights are nonnegative
//        for (Statement e : G.edges()) {
//            if (e.weight() < 0) {
//                System.err.println("negative edge weight detected");
//                return false;
//            }
//        }

        // check that distTo[v] and edgeTo[v] are consistent
        if (distTo.get(s) != 0.0 || edgeTo.get(s) != null) {
            logger.error("distTo[s] and edgeTo[s] inconsistent");
            return false;
        }
        for (int v = 0; v < nodes.size(); v++) {
            if (nodes.get(v) == s) 
            	continue;
            if (edgeTo.get(nodes.get(v)) == null && distTo.get(nodes.get(v)) != Double.POSITIVE_INFINITY) {
            	logger.error("distTo[] and edgeTo[] inconsistent");
                return false;
            }
        }

        // check that all edges e = v->w satisfy distTo[w] <= distTo[v] + e.weight()
        for (int v = 0; v < nodes.size(); v++) {
            for (Statement e : G.listStatements((Resource)nodes.get(v), null, (RDFNode)null).toList()) {
                RDFNode w = e.getObject();
                if (distTo.get(nodes.get(v)) + weight < distTo.get(w)) {
                    logger.error("edge " + e + " not relaxed");
                    return false;
                }
            }
        }

        // check that all edges e = v->w on SPT satisfy distTo[w] == distTo[v] + e.weight()
        for (int w = 0; w < nodes.size(); w++) {
            if (edgeTo.get(nodes.get(w)) == null) 
            	continue;
            Statement e = edgeTo.get(nodes.get(w));
            RDFNode v = e.getSubject();
            if (nodes.get(w) != e.getObject()) 
            	return false;
            if (distTo.get(v) + weight != distTo.get(nodes.get(w))) {
                logger.error("edge " + e + " on shortest path not tight");
                return false;
            }
        }
        return true;
    }


//    /**
//     * Unit tests the <tt>DijkstraSP</tt> data type.
//     */
//    public static void main(String[] args) {
//        //System.in in = new In(args[0]);
//        EdgeWeightedDigraph G = new EdgeWeightedDigraph(in);
//        int s = Integer.parseInt(args[1]);
//
//        // compute shortest paths
//        DijkstraSP sp = new DijkstraSP(G, s);
//
//
//        // print shortest path
//        for (int t = 0; t < nodes.size(); t++) {
//            if (sp.hasPathTo(t)) {
//                logger.info(String.format("%d to %d (%.2f)  ", s, t, sp.distTo(t)));
//                for (DirectedEdge e : sp.pathTo(t)) {
//                	logger.print(e + "   ");
//                }
//                logger.info("");
//            }
//            else {
//            	logger.info(String.format("%d to %d         no path\n", s, t));
//            }
//        }
//    }

}

