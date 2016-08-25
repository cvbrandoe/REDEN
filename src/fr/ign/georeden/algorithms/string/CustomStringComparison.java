package fr.ign.georeden.algorithms.string;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class CustomStringComparison implements IStringComparison {

	private static final String[] SET_VALUES = new String[] { "au", 
			"aux", 
			"avec", 
			"ce", 
			"ces", 
			"dans", 
			"de", 
			"des", 
			"du", 
			"elle", 
			"en", 
			"et", 
			"eux", 
			"il", 
			"je", 
			"la", 
			"le", 
			"leur", 
			"lui", 
			"ma", 
			"mais", 
			"me", 
			"même", 
			"mes", 
			"moi", 
			"mon", 
			"ne", 
			"nos", 
			"notre", 
			"nous", 
			"on", 
			"ou", 
			"par", 
			"pas", 
			"pour", 
			"qu", 
			"que", 
			"qui", 
			"sa", 
			"se", 
			"ses", 
			"son", 
			"sur", 
			"ta", 
			"te", 
			"tes", 
			"toi", 
			"ton", 
			"tu", 
			"un", 
			"une", 
			"vos", 
			"votre", 
			"vous", 
			"l", 
			"c", 
			"d", 
			"j", 
			"l", 
			"à", 
			"m", 
			"n", 
			"s", 
			"t", 
			"y", 
			"été", 
			"étée", 
			"étées", 
			"étés", 
			"étant", 
			"suis", 
			"es", 
			"est", 
			"sommes", 
			"êtes", 
			"sont", 
			"serai", 
			"seras", 
			"sera", 
			"serons", 
			"serez", 
			"seront", 
			"serais", 
			"serait", 
			"serions", 
			"seriez", 
			"seraient", 
			"étais", 
			"était", 
			"étions", 
			"étiez", 
			"étaient", 
			"fus", 
			"fut", 
			"fûmes", 
			"fûtes", 
			"furent", 
			"sois", 
			"soit", 
			"soyons", 
			"soyez", 
			"soient", 
			"fusse", 
			"fusses", 
			"fût", 
			"fussions", 
			"fussiez", 
			"fussent", 
			"ayant", 
			"eu", 
			"eue", 
			"eues", 
			"eus", 
			"ai", 
			"as", 
			"avons", 
			"avez", 
			"ont", 
			"aurai", 
			"auras", 
			"aura", 
			"aurons", 
			"aurez", 
			"auront", 
			"aurais", 
			"aurait", 
			"aurions", 
			"auriez", 
			"auraient", 
			"avais", 
			"avait", 
			"avions", 
			"aviez", 
			"avaient", 
			"eut", 
			"eûmes", 
			"eûtes", 
			"eurent", 
			"aie", 
			"aies", 
			"ait", 
			"ayons", 
			"ayez", 
			"aient", 
			"eusse", 
			"eusses", 
			"eût", 
			"eussions", 
			"eussiez", 
			"eussent", 
			"ceci", 
			"cela", 
			"celà", 
			"cet", 
			"cette", 
			"ici", 
			"ils", 
			"les", 
			"leurs", 
			"quel", 
			"quels", 
			"quelle", 
			"quelles", 
			"sans", 
			"soi",
			"marie"};
	private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(SET_VALUES));
	@Override
	public double computeSimilarity(String string1, String string2) {
		if (string1 == null || string2 == null)
			return 0.0;
		// si la chaine est du type Ville sur Rivière (ex: Neuilly sur Seine), on effectue un traitement à part
		String s1 = string1.toLowerCase();
		String s2 = string2.toLowerCase();
		if (s1.contains(" sur")) {
			int index = s1.indexOf(" sur");
			s1 = s1.substring(0, index);
		} else if (s1.contains("-sur")) {
			int index = s1.indexOf("-sur");
			s1 = s1.substring(0, index);
		}
		if (s2.contains(" sur")) {
			int index = s2.indexOf(" sur");
			s2 = s2.substring(0, index);
		} else if (s2.contains("-sur")) {
			int index = s2.indexOf("-sur");
			s2 = s2.substring(0, index);
		}
		// On vérifie si les bags ont un élément en commun
		Set<String> token1 = tokenize(s1);
		Set<String> token2 = tokenize(s2);
		if (token1.removeAll(token2))
			return 1.0;
		
		// on vérifie si le nom n'a pas été concaténé (ex: Donne-marie -> Donnemarie)
		String newS1 = string1.replace("-", "").replace(" ", "").replace("'", "").toLowerCase();
		String newS2 = string2.replace("-", "").replace(" ", "").replace("'", "").toLowerCase();
		if (newS1.equals(newS2))
			return 1.0;
		
		double slev = 0;
		TokenWiseSimilarity tws = new TokenWiseSimilarity(string1, string2, slev);		
		return tws.calcule();
	}
	
	private Set<String> tokenize(String s) {
		Set<String> tokens = new HashSet<>();
		if (s == null || s.isEmpty())
			return tokens;	
		String[] tokensTmp = s.toLowerCase().split("-| |'");
		for (String string : tokensTmp) {
			String tmpS = string.trim();
			if (!tmpS.isEmpty() && !STOP_WORDS.contains(tmpS))
				tokens.add(tmpS);
		}
		return tokens;
	}

}
