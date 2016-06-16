package fr.ign.georeden.algorithms.string;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class StringComparison {
	DamerauLevenshteinAlgorithm damLev;
	public StringComparison() {
		damLev = new DamerauLevenshteinAlgorithm(1, 1, 1, 1);
	}
	
	/**
	 * 
	 * @param s
	 * @return
	 */
	public String[] tokenizeString(String s) {
		StringTokenizer st = new StringTokenizer(s);
		List<String> tokens = new ArrayList<>();
	     while (st.hasMoreTokens()) {
	    	 tokens.add(st.nextToken());
	     }
	     return tokens.toArray(new String[tokens.size()]);
	}
	
	/**
	 * s1 and s2 must be tokenized. Return the value of lambdaIJ
	 * @param s1
	 * @param s2
	 * @return
	 */
	public float lambdaIJ(String s1, String s2) {
		return 1 - ((float)damLev.execute(s1, s2) / Math.max(s1.length(), s2.length()));
	}
}
