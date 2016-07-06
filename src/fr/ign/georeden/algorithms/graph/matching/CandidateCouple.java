package fr.ign.georeden.algorithms.graph.matching;

public class CandidateCouple {
	private Candidate c1;
	private Candidate c2;
	private Criterion pathCriterion;
	private Criterion rlspCriterion;
	private float pathValue;
	private float rlspValue;
	private LinkedToponyms linkedToponyms;
	
	public CandidateCouple(Candidate c1, Candidate c2, Criterion pathCriterion, Criterion rlspCriterion, LinkedToponyms linkedToponyms) {
		this.c1 = c1;
		this.c2 = c2;
		this.pathCriterion = pathCriterion;
		this.rlspCriterion = rlspCriterion;
		this.linkedToponyms = linkedToponyms;
	}
	
	public Candidate getC1() {
		return this.c1;
	}
	public Candidate getC2() {
		return this.c2;
	}
	public LinkedToponyms getLinkedToponyms() {
		return this.linkedToponyms;
	}
}
