package fr.ign.georeden.kb;

/**
 * The Enum ToponymType.
 */
public enum ToponymType {
	REGION("dbpedia-owl:Region"), SETTLEMENT("dbpedia-owl:Settlement"), TERRITORY(
			"dbpedia-owl:Territory"), 
	NATURAL_PLACE("dbpedia-owl:NaturalPlace"), 
	MOUNTAIN("dbpedia-owl:Mountain"), 
	VOLCANO("dbpedia-owl:Volcano"), 
	BODY_OF_WATER("dbpedia-owl:BodyOfWater"), 
	PLACE("dbpedia-owl:Place"),  
	UNKNOWN("unknown");
	private final String type;

	private ToponymType(String type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return this.type;
	}
	
	/**
	 * Return the toponym type from the enum that match the parameter.
	 *
	 * @param toponymType the toponym type
	 * @return the toponym type
	 */
	public static ToponymType where(String toponymType) {
		String suffix = toponymType.substring(toponymType.lastIndexOf(":"));
		for (int i = 0; i < ToponymType.values().length; i++) {
			ToponymType toponymTypeToCompare = ToponymType.values()[i];
			if (toponymTypeToCompare.equals(UNKNOWN))
				continue;
			String suffixToCompare = toponymTypeToCompare.toString().substring(toponymTypeToCompare.toString().lastIndexOf(":"));
			if (suffixToCompare.equalsIgnoreCase(suffix))
				return toponymTypeToCompare;
		}
		return ToponymType.UNKNOWN;
	}
	
	public static Boolean areTheSame(ToponymType typeTopo, String typeCandidate) {
		String suffixCandidate = typeCandidate.substring(typeCandidate.lastIndexOf(":"));
		String suffixTopo = "unknown";
		if (typeCandidate.contains(":")) 
			suffixTopo = typeTopo.toString().substring(typeCandidate.lastIndexOf(":"));
		return suffixTopo.equalsIgnoreCase(suffixCandidate);
	}
}
