package fr.ign.georeden.talismane;

public class TalismaneVertex {
	private String label;
	private String id;	
	private String type;
	
	public TalismaneVertex(String id, String label, String type) {
		this.label = label;
		this.id = id;	
		this.type = type;	
	}
	
	public String getId() {
		return id;
	}
	
	public String getLabel() {
		return label;
	}
	
	public String getType() {
		return type;
	}
	
	@Override
	public String toString() {
		return this.id + " : " + this.label + " (" + type + ")"; 
	}
}
