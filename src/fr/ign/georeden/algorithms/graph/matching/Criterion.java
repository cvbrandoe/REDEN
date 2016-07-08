package fr.ign.georeden.algorithms.graph.matching;

/**
 * The Class Criterion.
 */
public class Criterion {
	
	private String name;
	private float weight;
	private float veto;

	/** The criterion for the label or name comparison betwwen a toponym and a candidate. */
	public static final Criterion scoreText = new Criterion("scoreText", 0.2f, 0.8f);
	
	/** The criterion for the type comparison betwwen a toponym and a candidate. */
	public static final Criterion scoreType = new Criterion("scoreType", 0.25f, 0.8f);
	public static final Criterion pathLength = new Criterion("pathLength", 0.1f, 0.8f);
	public static final Criterion rlspExistance = new Criterion("rlspExistance", 0.25f, 0.8f);
	
	/**
	 * Instantiates a new criterion.
	 *
	 * @param name the name
	 * @param weight the weight
	 * @param veto the veto
	 */
	public Criterion(String name, float weight, float veto) {
		this.name = name;
		this.weight = weight;
		this.veto = veto;
	}
	
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
