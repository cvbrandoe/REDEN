package fr.ign.georeden.algorithms.graph.matching;

import java.util.HashSet;
import java.util.Set;
/**
 * Represent a candidate from the KB.
 * @author PHParis
 *
 */
public class Candidate {
	private String resource;
	private String label;
	private String name;
	private Set<String> types;

	
	/**
	 * Instantiates a new candidate.
	 *
	 * @param resource the resource
	 * @param label the label
	 * @param name the name
	 * @param types the types
	 */
	public Candidate(String resource, String label, String name, Set<String> types) {
		this.resource = resource;
		this.label = label;
		this.name = name;
		this.types = types;
	}
	
	/**
	 * Instantiates a new candidate.
	 *
	 * @param resource the resource
	 * @param label the label
	 * @param name the name
	 * @param type the type
	 */
	public Candidate(String resource, String label, String name, String type) {
		this.resource = resource;
		this.label = label;
		this.name = name;
		this.types = new HashSet<>();
		types.add(type);
	}
	
	
	/**
	 * Gets the resource from the KB.
	 *
	 * @return the resource
	 */
	public String getResource() {
		return resource;
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
	 * Gets the name.
	 *
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	
	
	/**
	 * Gets the types.
	 *
	 * @return the types
	 */
	public Set<String> getTypes() {
		return types;
	}
	
	public void addType(String type) {
		types.add(type);
	}
	public void addTypes(Set<String> types) {
		types.addAll(types);
	}
}
