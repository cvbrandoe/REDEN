package fr.ign.georeden.algorithms.graph.matching;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.TreeSet;

import org.apache.jena.atlas.lib.NotImplemented;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

@Deprecated
public class AStar {
	
	public static void shortestPath(Model graph, Resource goal, Resource start) {
		LinkedList<Noeud> closedList = new LinkedList<>();
		PriorityQueue<Noeud> openList = new PriorityQueue<>(new TreeSet<Noeud>(new NodeComparator()));
		Noeud goalNode = new Noeud(goal, 0, 0);
		openList.add(new Noeud(start, 0, 0));
		while (!openList.isEmpty()) {
			Noeud u = openList.poll();
			if (u == goal) {
				reconstituerChemin(u);
				// terminer le programme
			}
			List<Noeud> allowedNeighbour = getAllowedNeighbour(u);
			for (Noeud v : allowedNeighbour) {
				if (!((closedList.contains(v) || openList.contains(v)) && hasLowerCost(v, u))) {
					v.cout = u.cout + 1 ;
					v.heuristique = v.cout + distance(v, goalNode);
					openList.add(v);
				}
			}
			closedList.add(u);
		}
		// terminer le programme avec erreur
	}
	
	public static void reconstituerChemin(Noeud u) {
		throw new NotImplemented();
	}
	
	/**
	 * Checks for lower cost. Return true if r1 cost less than r2.
	 *
	 * @param r1 the r1
	 * @param r2 the r2
	 * @return true, if successful
	 */
	public static boolean hasLowerCost(Noeud r1, Noeud r2) {
		return r1.cout < r2.cout;
	}
	
	public static List<Noeud> getAllowedNeighbour(Noeud r) {
		throw new NotImplemented();
	}
	
	public static int distance(Noeud n1, Noeud n2) {
//		throw new NotImplemented();
		return n1.cout + 1;
	}

}
class Noeud {
	Resource r;
	int cout;
	int heuristique;
	public Noeud(Resource r, int cout, int heuristique) {
		this.r = r;
		this.cout = cout;
		this.heuristique = heuristique;
	}
}
class NodeComparator implements Comparator<Noeud>
{
    @Override
    public int compare(Noeud n1, Noeud n2)
    {
    	if (n1.heuristique < n2.heuristique)  
    		return 1;
    	else if (n1.heuristique  == n2.heuristique) 
    		return 0;
    	else
    		return -1;
    }
}