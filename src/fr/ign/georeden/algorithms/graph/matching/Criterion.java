package fr.ign.georeden.algorithms.graph.matching;

/**
 * The Class Criterion.
 */
public class Criterion {
	private String name;
	private float weight;
	private float veto;
	public float getWeight() {
		return weight;
	}
	public float getVeto() {
		return veto;
	}
	public String getName() {
		return name;
	}
}
