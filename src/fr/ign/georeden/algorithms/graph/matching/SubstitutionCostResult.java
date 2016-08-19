package fr.ign.georeden.algorithms.graph.matching;

import java.util.List;

import org.apache.jena.rdf.model.Resource;

public class SubstitutionCostResult {
	private Resource r1;
	private Resource r2;
	private float labelCost;
	private float linkCost;
	private float rlspCost;
	private float totalCost;
	
	public SubstitutionCostResult(Resource r1, Resource r2, float labelCost, float linkCost, float rlspCost, float totalCost) {
		this.r1 = r1;
		this.r2 = r2;
		this.labelCost = labelCost;
		this.linkCost = linkCost;
		this.rlspCost = rlspCost;
		this.totalCost = totalCost;
	}
	
	public Resource getResourceR1() {
		return this.r1;
	}
	public Resource getResourceR2() {
		return this.r2;
	}
	public float getLabelCost() {
		return this.labelCost;
	}
	public float getLinkCost() {
		return this.linkCost;
	}
	public float getRLSPCost() {
		return this.rlspCost;
	}
	public float getTotalCost() {
		return this.totalCost;
	}
	
	public boolean contains(Resource r1, Resource r2) {
		if (r1 == null || r2 == null)
			return false;
		return (this.r1.toString().equals(r1.toString()) && this.r2.toString().equals(r2.toString())) ||
				(this.r1.toString().equals(r2.toString()) && this.r2.toString().equals(r1.toString()));
	}
	public static boolean contains(List<SubstitutionCostResult> list, Resource r1, Resource r2) {
		return list.stream().anyMatch(l -> l.contains(r1, r2));
	}
	public static SubstitutionCostResult get(List<SubstitutionCostResult> list, Resource r1, Resource r2) {
		if (contains(list, r1, r2))
			return list.stream().filter(l -> l.contains(r1, r2)).findFirst().get();
		return null;
	}
}
