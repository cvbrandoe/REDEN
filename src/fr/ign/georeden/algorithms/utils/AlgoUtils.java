package fr.ign.georeden.algorithms.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class AlgoUtils {
	/**
	 * 
	 * @param s
	 * @return
	 */
	public static String[] tokenizeString(String s) {
		StringTokenizer st = new StringTokenizer(s);
		List<String> tokens = new ArrayList<>();
	     while (st.hasMoreTokens()) {
	    	 tokens.add(st.nextToken());
	     }
	     return tokens.toArray(new String[tokens.size()]);
	}
}
