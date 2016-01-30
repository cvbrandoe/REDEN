package fr.lip6.reden.extra;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Auxiliary class to highlight differences and similarities 
 * among two NEL outputs according to the LD referents chosen.
 * 
 * @author @author Brando & Frontini - Labex OBVIL - Université Paris-Sorbonne - UPMC
 *         LIP6
 */
public class CompareResults {

	public static Set<String> read(String file) {
		
		BufferedReader br = null;
		Set<String> out = new HashSet<String>();
		try {
			String sCurrentLine;
			br = new BufferedReader(new FileReader(file));
			while ((sCurrentLine = br.readLine()) != null) {
				String placeName = sCurrentLine.split(":")[1].replace("count", "").trim();
				out.add(placeName);	
				//System.out.println(file+" "+ placeName);
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return out;
	}
	
	public static Set<String> intersection(Set<String> l1, Set<String> l2) {
		Set<String> intersection = new HashSet<String>(l1); // use the copy constructor
		intersection.retainAll(l2);
		return intersection;
	}
	
	public static Set<String> diff(Set<String> l1, Set<String> l2) {
		Set<String> copyl1 = new HashSet<String>(l1);
		copyl1.removeAll(l2);
		return copyl1;
	}
	
	public static void printSet(Set<String> s) {
		Iterator<String> it = s.iterator();
		while (it.hasNext()) {
			System.out.print(it.next()+"|"); 
		}
		
	}
	
	public static void missingInBoth() {
		String file1 = "output\\dbpedia\\apollinaire_heresiarque-et-cie-outV3.xml";
		String file2 = "output\\geonames\\apollinaire_heresiarque-et-cie-resCorrectMentionsV3.txt";
	}
	
	public static void firstProcessing() {
		
		//String file1 = "output\\dbpedia\\apollinaire_heresiarque-et-cie-resCorrectMentionsV3.txt";
		//String file2 = "output\\geonames\\apollinaire_heresiarque-et-cie-resCorrectMentionsV3.txt";
		String file1 = "output\\dbpedia\\renan_nation_only_placeNameTag-resCorrectMentionsV3.txt";
		String file2 = "output\\geonames\\renan_nation_only_placeNameTag-resCorrectMentionsV3.txt";
		
		System.out.println("IN APOLLINAIRE");
		Set<String> file1List = read(file1);
		Set<String> file2List = read(file2);
		
		System.out.println("mentions having a correct referent in both DBpedia and Geonames are");
		Set<String> intersection = intersection(file1List, file2List); //intersection now contains only elements in both sets
		printSet(intersection);
		System.out.println("");
		System.out.println("Total are: "+intersection.size());
		
		System.out.println("mentions having a correct referent in only DBpedia but not in Geoanmes");
		Set<String> diff = diff(file1List, file2List);
		printSet(diff);
		System.out.println("");
		System.out.println("Total are: "+diff.size());
		
		System.out.println("mentions having a correct referent in only Geoanmes but not in DBpedia");
		Set<String> diff2 = diff(file2List, file1List);
		printSet(diff2);
		System.out.println("");
		System.out.println("Total are: "+diff2.size());
	}
	
	public static void main(String[] args) {
		firstProcessing();
		//missingInBoth();
	}
}
