package fr.ign.georeden.kb;

/**
 * The Enum ToponymType.
 */
public enum ToponymType {
	REGION("dbpedia-owl:Region"), SETTLEMENT("dbpedia-owl:Settlement"), TERRITORY(
			"dbpedia-owl:Territory"), NATURAL_PLACE("dbpedia-owl:NaturalPlace"), UNKNOWN("unknown");
	private final String type;

	private ToponymType(String type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return this.type;
	}
}
