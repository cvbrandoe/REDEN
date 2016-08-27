package fr.ign.georeden.algorithms.graph.matching;

import org.apache.jena.rdf.model.Resource;

public class Substitution implements IPathMatching {
	private Resource deletedNode;
	private Resource insertedNode;
	private double cost;

	public Substitution(Resource deletedNode, Resource insertedNode, double cost) {
		this.deletedNode = deletedNode;
		this.insertedNode = insertedNode;
		this.cost = cost;
	}

	public Resource getDeletedNode() {
		return this.deletedNode;
	}

	public Resource getInsertedNode() {
		return this.insertedNode;
	}

	@Override
	public double getCost() {
		return this.cost;
	}

	@Override
	public String toString() {
		return "Substitution : " + this.deletedNode + " -> " + this.insertedNode + " (" + this.cost + ")";
	}
}
