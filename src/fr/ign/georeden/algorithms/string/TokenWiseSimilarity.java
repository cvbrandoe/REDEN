package fr.ign.georeden.algorithms.string;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import uk.ac.shef.wit.simmetrics.similaritymetrics.InterfaceStringMetric;
import uk.ac.shef.wit.simmetrics.similaritymetrics.Levenshtein;
import uk.ac.shef.wit.simmetrics.tokenisers.TokeniserWhitespace;

public class TokenWiseSimilarity {

	public TokenWiseSimilarity(String chaine1, String chaine2, Double slev) {
		this.chaine1 = chaine1.toLowerCase();
		this.chaine2 = chaine2.toLowerCase();
		this.slev = slev;
		this.stopWords = new ArrayList<String>();
		this.stopWords.clear();

	}

	private String chaine1;
	private String chaine2;
	private List<String> stopWords;
	private Double wtc1 = 1.0;
	private Double wtc2 = 1.0;
	private double slev;

	public double calcule() {
		//System.out.println("On compare " + this.chaine1 + " and " + this.chaine2);
		double valeur = -1.0;
		List<Mapping> matchedTokens = new ArrayList<Mapping>();
		List<Mapping> unMatchedTokens = new ArrayList<Mapping>();

		// On tokenise les chaines en entree
		TokeniserWhitespace tw1 = new TokeniserWhitespace();
		TokeniserWhitespace tw2 = new TokeniserWhitespace();
		ArrayList<String> tokens1 = tw1.tokenizeToArrayList(this.chaine1);
		ArrayList<String> tokens2 = tw1.tokenizeToArrayList(this.chaine2);

		if ((tokens1.size() != 0) && (tokens2.size() != 0)) {

			// Pour chaque token de la chaine 1 on va calculer sa similarité de
			// Levenstein avec les tokens de la chaîne 2 et retenir les paires
			// dotées d'un score supérieur au seuil slev
			for (String string1 : tokens1) {
				for (String string2 : tokens2) {
					Levenshtein lev = new Levenshtein();
					double tempscore = (double) lev.getSimilarity(string1, string2);
					if (tempscore >= this.slev) {
						Mapping m = new Mapping(string1, string2);
						//System.out.println("trouvé!");
						m.setScore(tempscore);
						//System.out.println(tempscore);
						matchedTokens.add(m);
					} else {
						Mapping m = new Mapping(string1, string2);
						m.setScore(0.0);
						unMatchedTokens.add(m);
					}
				}
			}

			Double dividende = 0.0;
			for (Mapping mt : matchedTokens) {
				dividende = dividende + (mt.getScore() * this.wtc1 * this.wtc2);
			}
			//System.out.println("dividende= " + dividende);
			Double diviseurpartiel = 0.0;
			for (Mapping umt : unMatchedTokens) {
				diviseurpartiel = diviseurpartiel
						+ ((1 - umt.getScore()) * (this.wtc1 * this.wtc1 + this.wtc2 * this.wtc2));
			}
			for (Mapping mt : matchedTokens) {
				diviseurpartiel = diviseurpartiel
						+ ((1 - mt.getScore()) * (this.wtc1 * this.wtc1 + this.wtc2 * this.wtc2));
			}
			//System.out.println("diviseurpartiel= " + diviseurpartiel);

			valeur = dividende / (dividende + diviseurpartiel);
			//System.out.println("On obtient***************************** " + valeur);
			return valeur;
		} // Une des deux chaines est nulle!
		return valeur;
	}

	/** Version bidouillee */
	public double calculeSimple() {// System.out.println("On compare
									// "+this.chaine1+" and "+this.chaine2);
		double valeur = -1.0;
		List<Mapping> bestMatches = new ArrayList<Mapping>();

		TokeniserWhitespace tw1 = new TokeniserWhitespace();
		TokeniserWhitespace tw2 = new TokeniserWhitespace();

		ArrayList<String> tokens1 = tw1.tokenizeToArrayList(this.chaine1);
		ArrayList<String> tokens2 = tw1.tokenizeToArrayList(this.chaine2);

		if ((tokens1.size() != 0) && (tokens2.size() != 0)) {

			// Gestion des stopWords
			if (this.stopWords.size() != 0) {
				for (String sw : this.stopWords) {
					if (tokens1.contains(sw)) {
						tokens1.remove(sw);
					}
					if (tokens2.contains(sw)) {
						tokens2.remove(sw);
					}
				}
			}

			// Pour chaque token de la chaine 1 on va calculer son meilleur
			// Levenstein avec les tokens de la chaîne 2
			for (String string1 : tokens1) {
				double score = 0.0;
				String stringMax = "";
				for (String string2 : tokens2) {
					Levenshtein lev = new Levenshtein();
					double tempscore = (double) lev.getSimilarity(string1, string2);
					if (tempscore > score) {
						score = tempscore;
						stringMax = string2;
					} else {
						continue;
					}
				}
				double temp = score;
				Mapping m = new Mapping(string1, stringMax, temp);
				bestMatches.add(m);
				// System.out.println("best match between "+string1+" and
				// "+stringMax +" = "+temp+"
				// **************************************************************");
			}
			// Pour chaque token de la chaine 2 on va calculer son meilleur
			// Levenstein avec les tokens de la chaîne 1
			for (String string2 : tokens2) {
				double score = 0.0;
				String stringMax = "";
				for (String string1 : tokens1) {
					Levenshtein lev = new Levenshtein();
					double tempscore = (double) lev.getSimilarity(string2, string1);
					if (tempscore > score) {
						score = tempscore;
						stringMax = string1;
					} else {
						continue;
					}
				}
				double temp = score;
				Mapping m = new Mapping(string2, stringMax, temp);
				bestMatches.add(m);
				// System.out.println("best match between "+string1+" and
				// "+stringMax +" = "+temp+"
				// **************************************************************");
			}

			double dividende = 0.0;
			int diviseur;
			diviseur = tokens1.size() + tokens2.size();

			for (Mapping m : bestMatches) {
				dividende = dividende + m.getScore();
			}

			valeur = dividende / diviseur;

		}
		return valeur;
	}

	public List<String> getStopWords() {
		return stopWords;
	}

	public void setStopWords(List<String> stopWords) {
		this.stopWords = stopWords;
	}

	public void cleanBracketsAndTheirContent() {
		if (this.chaine1.contains("(")) {
			this.chaine1 = this.chaine1.replaceAll("\\(.*\\)", "");
		}
		if (this.chaine2.contains("(")) {
			this.chaine2 = this.chaine2.replaceAll("\\(.*\\)", "");
		}

	}

	public void quotationMark2WhiteSpace() {
		if (this.chaine1.contains("'")) {
			this.chaine1 = this.chaine1.replace("'", " ");
		}
		if (this.chaine2.contains("'")) {
			this.chaine2 = this.chaine2.replace("'", " ");
		}

	}

}
