package fr.ign.georeden.algorithms.graph.matching;

/**
 * The score betwwen a toponym and a candidate.
 */
public class Score {
	private Candidate candidate;
	private float score;
	
	/**
	 * Instantiates a new score.
	 *
	 * @param id the id
	 * @param type the type
	 * @param candidate the candidate
	 * @param score the score
	 */
	public Score(Candidate candidate, float score) {
		this.candidate = candidate;
		this.score = score;
	}
	
	/**
	 * Gets the candidate.
	 *
	 * @return the candidate
	 */
	public Candidate getCandidate() {
		return candidate;
	}
	
	/**
	 * Gets the score.
	 *
	 * @return the score
	 */
	public float getScore() {
		return score;
	}
}
