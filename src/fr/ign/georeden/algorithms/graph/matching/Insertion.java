package fr.ign.georeden.algorithms.graph.matching;

import org.apache.jena.rdf.model.Resource;

public class Insertion implements IPathMatching {
	private Resource insertedNode;
	private double cost;

	public Insertion(Resource insertedNode, double cost) {
		this.insertedNode = insertedNode;
		this.cost = cost;
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
		return "Insertion : " + this.insertedNode + " (" + this.cost + ")";
	}
}
