package fr.lip6.reden.nelinker;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.output.FileWriterWithEncoding;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

/**
 * Some utility methods.
 * 
 * @author @author Brando & Frontini
 */

public class Util {

	/**
	 * Print graph to standard output.
	 * @param graph, the given graph
	 * @param out, where to write the output
	 */
	@SuppressWarnings("rawtypes")
	public static void printGraph(
			SimpleDirectedWeightedGraph<String, LabeledEdge> graph,
			FileWriterWithEncoding out) {
		try {
			for (LabeledEdge edge : graph.edgeSet()) {
				out.write(graph.getEdgeSource(edge) + " (" + edge.toString() + " (weight:"+ graph.getEdgeWeight(edge)+") "
						+ ") " + graph.getEdgeTarget(edge) + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	/**
	 * Helper method to order a map by value.
	 * 
	 * @param map
	 *            , the unordered map
	 * @return the ordered map
	 */
	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(
			Map<K, V> map) {
		List<Map.Entry<K, V>> list = new LinkedList<Map.Entry<K, V>>(
				map.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
			public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
				return (o1.getValue()).compareTo(o2.getValue());
			}
		});

		Map<K, V> result = new LinkedHashMap<K, V>();
		for (Map.Entry<K, V> entry : list) {
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}
	
	/**
	 * String cleaning.
	 * @param in
	 * @return
	 */
	public static String replaceNonAlphabeticCharacters(String in) {
		Pattern p = Pattern.compile("\\s|'|-");
		Matcher m = p.matcher(in);
		String texteRemplace = m.replaceAll("").replaceAll("/", "-").replaceAll(":", "");
		return texteRemplace.toLowerCase();
	}
	
	/**
	 * Decodes URI.
	 * 
	 * @param s, the URI
	 * @return the new URI
	 */
	public static String decompose(String s) {
		try {
			if (s.startsWith("http:")) {
				return URLDecoder.decode(s, "UTF-8");
			} else {
				return s;
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return "";
	}
}
