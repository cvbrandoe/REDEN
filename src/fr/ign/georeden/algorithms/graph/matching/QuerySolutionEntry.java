package fr.ign.georeden.algorithms.graph.matching;

import org.apache.jena.rdf.model.Resource;

public class QuerySolutionEntry {
	private Resource sequence;
	private Resource route;
	private Resource bag;
	private Resource waypoint;
	private Resource spatialReference;
	private Resource spatialReferenceAlt;
	private Integer id;

	public QuerySolutionEntry(Resource sequence, Resource route, Resource bag, Resource waypoint,
			Resource spatialReference, Resource spatialReferenceAlt, Integer id) {
		this.sequence = sequence;
		this.route = route;
		this.bag = bag;
		this.waypoint = waypoint;
		this.spatialReference = spatialReference;
		this.spatialReferenceAlt = spatialReferenceAlt;
		this.id = id;
	}

	public Resource getSequence() {
		return this.sequence;
	}

	public Resource getRoute() {
		return this.route;
	}

	public Resource getBag() {
		return this.bag;
	}

	public Resource getWaypoint() {
		return this.waypoint;
	}

	public Resource getSpatialReference() {
		return this.spatialReference;
	}

	public Resource getSpatialReferenceAlt() {
		return this.spatialReferenceAlt;
	}

	public Integer getId() {
		return this.id;
	}
}
