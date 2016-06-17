package fr.ign.georeden.algorithms.string;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class StringComparisonMetaphone implements IStringComparison {

	public String[] tokenizeString(String s) {
		StringTokenizer st = new StringTokenizer(s);
		List<String> tokens = new ArrayList<>();
	     while (st.hasMoreTokens()) {
	    	 tokens.add(st.nextToken());
	     }
	     return tokens.toArray(new String[tokens.size()]);
	}
	
	@Override
	public float computeSimilarity(String s1, String s2) {
		Metaphone3 metaphoneS1 = new Metaphone3(s1);
		metaphoneS1.Encode();
		String primary1 = metaphoneS1.GetMetaph();
		String alt1 = metaphoneS1.GetAlternateMetaph();
		

		Metaphone3 metaphoneS2 = new Metaphone3(s2);
		metaphoneS2.Encode();
		String primary2 = metaphoneS2.GetMetaph();
		String alt2 = metaphoneS2.GetAlternateMetaph();
		
		if (primary1.equals(primary2)) {
			if (alt1 != null) {
				if (alt1.equals(alt2))
					return 1f;
			}
			else if (alt2 == null) {
				return 1f;
			}
		}
		return 0f;
	}

}
