package fr.ign.georeden.graph;

import org.jgrapht.graph.DefaultWeightedEdge;

public class LabeledEdge<V, E> extends DefaultWeightedEdge {
	private static final long serialVersionUID = 1L;
	private V vertex1;
	private V vertex2;
	private E label;

	public LabeledEdge(V vertex1, V vertex2, E label) {
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
		return label.toString();
	}

	public String toFullString() {
		return vertex1 + " | " + label + " -> " + vertex2;
	}
}
