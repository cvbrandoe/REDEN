package fr.ign.georeden.graph;

import fr.ign.georeden.kb.ToponymType;

public class Toponym {
	private String id;
	private String name;
	private ToponymType type;

	public Toponym(String name, ToponymType type, String id) {
		this.name = name;
		this.type = type;
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public ToponymType getType() {
		return type;
	}

	public String getId() {
		return id;
	}

	@Override
	public String toString() {
		return name + " (" + id + " / " + type + ")";
	}

	@Override
	public boolean equals(Object other) {
		if (other == null)
			return false;
		if (other == this)
			return true;
		if (!(other instanceof Toponym))
			return false;
		Toponym otherToponym = (Toponym) other;
		return this.id.equalsIgnoreCase(otherToponym.id);
	}
}
