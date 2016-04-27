package fr.ign.georeden.kb;

/**
 * The Enum SpatialRelationship.
 */
public enum SpatialRelationship {

	/** near of */
	NEAR("rlsp:near"),
	/** north of */
	NORTH_OF("rlsp:northOf"),
	/** south of */
	SOUTH_OF("rlsp:southOf"),
	/** east of */
	EAST_OF("rlsp:eastOf"),
	/** west of */
	WEST_OF("rlsp:westOf"),
	/** north-east of */
	NORTH_EAST_OF("rlsp:northEastOf"),
	/** north-west of */
	NORTH_WEST_OF("rlsp:northWestOf"),
	/** south-east of */
	SOUTH_EAST_OF("rlsp:southEastOf"),
	/** sout-west of */
	SOUTH_WEST_OF("rlsp:southWestOf");
	private final String relationship;

	private SpatialRelationship(String relationship) {
		this.relationship = relationship;
	}

	@Override
	public String toString() {
		return this.relationship;
	}
}
