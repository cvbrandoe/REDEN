package fr.ign.georeden.algorithms.graph.matching;

import java.util.ArrayList;
import java.util.List;

import fr.ign.georeden.kb.SpatialRelationship;

public class LinkedToponyms {
	private Toponym t1;
	private Toponym t2;
	private SpatialRelationship rlsp;
	private List<CandidateCouple> couples;
	
	public LinkedToponyms(Toponym t1, Toponym t2, SpatialRelationship rlsp) {
		this.t1 = t1;
		this.t2 = t2;
		this.rlsp = rlsp;
		this.couples = new ArrayList<>();
	}
	
	public Toponym getT1() {
		return this.t1;
	}
	public Toponym getT2() {
		return this.t2;
	}
	public SpatialRelationship getRlsp() {
		return this.rlsp;
	}
	public List<CandidateCouple> getCandidateCouples() {
		return this.couples;
	}
}
