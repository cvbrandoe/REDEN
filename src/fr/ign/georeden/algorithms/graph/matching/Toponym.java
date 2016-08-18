package fr.ign.georeden.algorithms.graph.matching;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.rdf.model.Resource;

import fr.ign.georeden.kb.ToponymType;

/**
 * The Class Toponym.
 */
public class Toponym {
	private Resource resource;
	private String name;
	private Integer xmlId;
	private ToponymType type;
//	private List<ToponymType> types;
	private List<CriterionToponymCandidate> scoreCriterionToponymCandidate;
	private List<CriterionToponymCandidate> typeCriterionToponymCandidate;

	private Resource referent;
	
	/**
	 * Instantiates a new toponym.
	 *
	 * @param resource the resource
	 * @param xmlId the xml id
	 * @param name the name
	 * @param types the types
	 */
	public Toponym(Resource resource, Integer xmlId, String name, ToponymType type) {
		this.resource = resource;
		this.name = name;
		this.xmlId = xmlId;
		this.type = type;
		this.scoreCriterionToponymCandidate = new ArrayList<>();
		this.typeCriterionToponymCandidate = new ArrayList<>();
	}
	
//	/**
//	 * Instantiates a new toponym.
//	 *
//	 * @param resource the resource
//	 * @param xmlId the xml id
//	 * @param name the name
//	 * @param type the type
//	 */
//	public Toponym(String resource, Integer xmlId, String name, ToponymType type) {
//		this.name = name;
//		this.xmlId = xmlId;
//		this.types = new ArrayList<>();
//		this.types.add(type);
//		this.scoreCriterionToponymCandidate = new ArrayList<>();
//		this.typeCriterionToponymCandidate = new ArrayList<>();
//	}
	
	public Resource getResource() {
		return this.resource;
	}
	public Resource getReferent() {
		return this.referent;
	}
	public void setReferent(Resource referent) {
		this.referent = referent;
	}
	public String getName() {
		return this.name;
	}
	public Integer getXmlId() {
		return this.xmlId;
	}
	public ToponymType getType() {
		return this.type;
	}
	
	/**
	 * Adds the score to the other ScoreCriterionToponymCandidate.
	 *
	 * @param score the score
	 */
	public void addScoreCriterionToponymCandidate(CriterionToponymCandidate score) {
		if (this.scoreCriterionToponymCandidate == null)
			this.scoreCriterionToponymCandidate = new ArrayList<>();
		this.scoreCriterionToponymCandidate.add(score);
	}
	public void clearAndAddAllScoreCriterionToponymCandidate(List<CriterionToponymCandidate> scores) {
		this.scoreCriterionToponymCandidate.clear();
		this.scoreCriterionToponymCandidate.addAll(scores);
	}
	public void clearAllScoreCriterionToponymCandidate() {
		this.scoreCriterionToponymCandidate.clear();
	}
	
	/**
	 * Adds the type to the other TypeCriterionToponymCandidate.
	 *
	 * @param type the type
	 */
	@Deprecated
	public void addTypeCriterionToponymCandidate(CriterionToponymCandidate type) {
		this.typeCriterionToponymCandidate.add(type);
	}
	public List<CriterionToponymCandidate> getScoreCriterionToponymCandidate() {
		return this.scoreCriterionToponymCandidate;
	}
	@Deprecated
	public List<CriterionToponymCandidate> getTypeCriterionToponymCandidate() {
		return this.typeCriterionToponymCandidate;
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
	    if (!(other instanceof Toponym))
	    	return false;
	    Toponym otherToponym = (Toponym)other;
	    return resource.equals(otherToponym.resource);
	}
}
