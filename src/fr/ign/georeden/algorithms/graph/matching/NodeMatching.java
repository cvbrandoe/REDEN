package fr.ign.georeden.algorithms.graph.matching;

import java.util.ArrayList;
import java.util.List;


// TODO: Auto-generated Javadoc
/**
 * Contains the scores for each nodes of the TEI that share the same label.
 */
public class NodeMatching {
	
	/** The resource. */
	private String resource;
	
	/** The type. */
	private String type;
	
	/** The label. */
	private String label;
	
	/** The id. */
	private Integer id;

	/** The scores. */
	private List<Score> scores;
	
	/**
	 * Instantiates a new node matching.
	 *
	 * @param label the label
	 * @param type the type
	 * @param id the id
	 * @param resource the resource
	 */
	public NodeMatching(String label, String type, Integer id, String resource) {
		this.label = label;
		this.id = id;
		this.resource = resource;
		this.type = type;
		this.scores = new ArrayList<>();
	}

	/**
	 * Gets the label.
	 *
	 * @return the label
	 */
	public String getLabel() {
		return label;
	}
	
	/**
	 * Gets the id.
	 *
	 * @return the id
	 */
	public Integer getId() {
		return id;
	}
	
	/**
	 * Gets the type.
	 *
	 * @return the type
	 */
	public String getType() {
		return type;
	}
	
	/**
	 * Gets the resource.
	 *
	 * @return the resource
	 */
	public String getResource() {
		return resource;
	}
	
	/**
	 * Adds the score.
	 *
	 * @param score
	 *            the score
	 */
	public void addScore(Score score) {
		scores.add(score);
	}

	/**
	 * Gets the scores.
	 *
	 * @return the scores
	 */
	public List<Score> getScores() {
		return scores;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return resource.hashCode();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object other) {
		if (other == null) 
			return false;
	    if (other == this) 
	    	return true;
	    if (!(other instanceof NodeMatching))
	    	return false;
	    NodeMatching otherNodeMatching = (NodeMatching)other;
	    return resource.equals(otherNodeMatching.resource);
	}
}
