package fr.ign.georeden.algorithms.graph.matching;

public class Candidate {
	private String resource;
	private String label;
	private String name;
	
	public String getResource() {
		return resource;
	}
	public String getLabel() {
		return label;
	}
	public String getName() {
		return name;
	}
	
	public Candidate(String resource, String label, String name) {
		this.resource = resource;
		this.label = label;
		this.name = name;
	}
}
