package fr.ign.georeden.algorithms.string;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import info.debatty.java.stringsimilarity.Cosine;

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
			"marie",
			"saint",
			"sainte",
			"forêt",
			"mont",
			"ville",
			"bourg",
			"grand",
			"blanc"};
	private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(SET_VALUES));
	@Override
	public double computeSimilarity(String string1, String string2) {
		// on découpe le nom du topo en token (espace, tiret, apostrophe)
		//nomTopo.split(regex)
		// avec une liste de stop words on supprime les parties inutiles (le, la, les, l', sur) 
		// (traitement particulier pour Saint et Sainte ?)
		// Pour chaque candidat, on le découpe de la même manière tout en supprimant les token inutiles avec des stopword
		// si un des token du candidat correspond exactement à un des token du topo, on le sélectionne automatiquement avec un haut score (1.0 ou 0.9 ?)
		// sinon on utilise une mesure et on sélectionne en fonction de cette mesure
		if (string1 == null || string2 == null)
			return 0.0;
		// si la chaine est du type Ville sur Rivière (ex: Neuilly sur Seine ou Availles-en-Châtellerault ou 
		// Bonchamp-lès-Laval, Monestier-de-Clermont), on effectue un traitement à part
		String s1 = string1.toLowerCase();
		String s2 = string2.toLowerCase();
		if (!s1.startsWith("saint") && !s2.startsWith("saint")) {
			s1 = removeSuffix(s1, "sur");
			s2 = removeSuffix(s2, "sur");
			s1 = removeSuffix(s1, "en");
			s2 = removeSuffix(s2, "en");
			s1 = removeSuffix(s1, "lès");
			s2 = removeSuffix(s2, "lès");
			s1 = removeSuffix(s1, "de");
			s2 = removeSuffix(s2, "de");
			// On vérifie si les bags ont un élément en commun
			Set<String> token1 = tokenize(s1);
			Set<String> token2 = tokenize(s2);
			if (!token1.isEmpty() && !token2.isEmpty() && token1.removeAll(token2))
				return 1.0;
		}
		
		// on vérifie si le nom n'a pas été concaténé (ex: Donne-marie -> Donnemarie)
		String newS1 = string1.replace("-", "").replace(" ", "").replace("'", "").toLowerCase();
		String newS2 = string2.replace("-", "").replace(" ", "").replace("'", "").toLowerCase();
		if (newS1.equals(newS2))
			return 1.0;
		Cosine cosine = new Cosine();
		return cosine.similarity(string1, string2);
//		double slev = 0;
//		TokenWiseSimilarity tws = new TokenWiseSimilarity(string1, string2, slev);		
//		return tws.calcule();
	}
	
	/**
	 * Removes the suffix and the later part from the string s if the suffix preceded by a white space or a hyphen is present in s.
	 *
	 * @param stringToCheck the string to check
	 * @param suffix the suffix
	 * @return the string
	 */
	private String removeSuffix(String stringToCheck, String suffix) {
		String s = stringToCheck;
		int index = -1;
		if (s.contains(" " + suffix + " "))
			index = s.indexOf(" " + suffix + " ");
		else if (s.contains("-" + suffix + " "))
			index = s.indexOf("-" + suffix + "-");
		if (index > -1 && s.length() > index)
			s = s.substring(0, index);
		return s;
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
