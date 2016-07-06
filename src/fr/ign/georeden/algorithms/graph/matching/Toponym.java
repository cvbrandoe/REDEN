package fr.ign.georeden.algorithms.graph.matching;

import java.util.ArrayList;
import java.util.List;

import fr.ign.georeden.kb.ToponymType;

/**
 * The Class Toponym.
 */
public class Toponym {
	private String resource;
	private String name;
	private Integer xmlId;
	private List<ToponymType> types;
	private List<CriterionToponymCandidate> scoreCriterionToponymCandidate;
	private List<CriterionToponymCandidate> typeCriterionToponymCandidate;
	
	/**
	 * Instantiates a new toponym.
	 *
	 * @param resource the resource
	 * @param xmlId the xml id
	 * @param name the name
	 * @param types the types
	 */
	public Toponym(String resource, Integer xmlId, String name, List<ToponymType> types) {
		this.name = name;
		this.xmlId = xmlId;
		this.types = types;
		this.scoreCriterionToponymCandidate = new ArrayList<>();
		this.typeCriterionToponymCandidate = new ArrayList<>();
	}
	
	/**
	 * Instantiates a new toponym.
	 *
	 * @param resource the resource
	 * @param xmlId the xml id
	 * @param name the name
	 * @param type the type
	 */
	public Toponym(String resource, Integer xmlId, String name, ToponymType type) {
		this.name = name;
		this.xmlId = xmlId;
		this.types = new ArrayList<>();
		this.types.add(type);
		this.scoreCriterionToponymCandidate = new ArrayList<>();
		this.typeCriterionToponymCandidate = new ArrayList<>();
	}
	
	public String getResource() {
		return this.resource;
	}
	public String getName() {
		return this.name;
	}
	public Integer getXmlId() {
		return this.xmlId;
	}
	public List<ToponymType> getType() {
		return this.types;
	}
	
	/**
	 * Adds the score to the other ScoreCriterionToponymCandidate.
	 *
	 * @param score the score
	 */
	public void addScoreCriterionToponymCandidate(CriterionToponymCandidate score) {
		this.scoreCriterionToponymCandidate.add(score);
	}
	
	/**
	 * Adds the type to the other TypeCriterionToponymCandidate.
	 *
	 * @param type the type
	 */
	public void addTypeCriterionToponymCandidate(CriterionToponymCandidate type) {
		this.typeCriterionToponymCandidate.add(type);
	}
	public List<CriterionToponymCandidate> getScoreCriterionToponymCandidate() {
		return this.scoreCriterionToponymCandidate;
	}
	public List<CriterionToponymCandidate> getTypeCriterionToponymCandidate() {
		return this.typeCriterionToponymCandidate;
	}
}
