package fr.ign.georeden.algorithms.graph.matching;

import java.util.List;

import org.apache.jena.rdf.model.Model;

public class MatchingResult {
	private float costEdition;
	private Model model;
	private List<IPathMatching> editionPath;
	
	public MatchingResult(Model model, List<IPathMatching> editionPath, float costEdition) {
		this.costEdition = costEdition;
		this.model = model;
		this.editionPath = editionPath;
	}
	
	public float getCostEdition() {
		return this.costEdition;
	}
	public Model getModel() {
		return this.model;
	}
	public List<IPathMatching> getEditionPath() {
		return this.editionPath;
	}
}
