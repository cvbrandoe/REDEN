package fr.ign.georeden.algorithms.graph.matching;

import java.util.List;

import org.apache.jena.rdf.model.Model;

public class MatchingResult {
	private double costEdition;
	private Model model;
	private List<IPathMatching> editionPath;
	
	public MatchingResult(Model model, List<IPathMatching> editionPath, double costEdition) {
		this.costEdition = costEdition;
		this.model = model;
		this.editionPath = editionPath;
	}
	
	public double getCostEdition() {
		return this.costEdition;
	}
	public Model getModel() {
		return this.model;
	}
	public List<IPathMatching> getEditionPath() {
		return this.editionPath;
	}
}
