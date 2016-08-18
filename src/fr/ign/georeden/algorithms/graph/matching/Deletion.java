package fr.ign.georeden.algorithms.graph.matching;

import org.apache.jena.rdf.model.Resource;

public class Deletion implements IPathMatching {
	private Resource deletedNode;
	private float cost;

	public Deletion(Resource deletedNode, float cost) {
		this.deletedNode = deletedNode;
		this.cost = cost;
	}

	public Resource getDeletedNode() {
		return this.deletedNode;
	}

	@Override
	public float getCost() {
		return this.cost;
	}

	@Override
	public String toString() {
		return "Deletion : " + this.deletedNode + " (" + this.cost + ")";
	}
}
