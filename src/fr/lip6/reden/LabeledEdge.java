package fr.lip6.reden;

import org.jgrapht.graph.DefaultWeightedEdge;

/**
 * This is an utility class to handle labeled edges in our JGraphT graphs.
 * 
 * @author @author Brando & Frontini
 */
@SuppressWarnings("serial")
public class LabeledEdge<V> extends DefaultWeightedEdge {
    private V vertex1;
    private V vertex2;
    private String label;

    public LabeledEdge(V vertex1, V vertex2, String label) {
        this.vertex1 = vertex1;
        this.vertex2 = vertex2;
        this.label = label;
    }

    public V getVertex1() {
        return vertex1;
    }

    public V getVertex2() {
        return vertex2;
    }

    public String toString() {
        return label;
    }
}
