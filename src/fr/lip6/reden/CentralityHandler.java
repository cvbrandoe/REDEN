package fr.lip6.reden;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.output.FileWriterWithEncoding;
import org.apache.log4j.Logger;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import dk.aaue.sna.alg.centrality.BrandesBetweennessCentrality;
import dk.aaue.sna.alg.centrality.CentralityMeasure;
import dk.aaue.sna.alg.centrality.CentralityResult;
import dk.aaue.sna.alg.centrality.DegreeCentrality;
import dk.aaue.sna.alg.centrality.EigenvectorCentrality;
import dk.aaue.sna.alg.centrality.FreemanClosenessCentrality;

/**
 * This class implements the method for graph centrality calculation.
 * 
 * @author @author Brando & Frontini
 */
public class CentralityHandler {

	private static Logger logger = Logger.getLogger(CentralityHandler.class);
	
	/**
	 * 
	 * 
	 * @param graph
	 *            , the graph of URIs
	 * @param mentionsWithURIs
	 *            , the mentions and their URIs for every possible candidate
	 * @param mentionsPerParagraph
	 *            , mentions of the paragraph
	 * @param baseURIS
	 *            , URIs to consider as vertex of the graph
	 * @param invertedIndex
	 *            , index of URI to obtain mention
	 * @return the chosen URIs
	 */
	/**
	 * Keeps only meaningful edges and vertex of the graph.
	 * @param graph, the fused graph
	 * @param mentionsWithURIs, the mentions and their URIs for every possible candidate
	 * @param mentionsPerParagraph, mentions of the paragraph
	 * @param baseURIS, reference source
	 * @param invertedIndex, index of URI to obtain mention
	 * @param measure, the centrality measure
	 * @param preferedURI, the URI to use in the XML-TEI output
	 * @param namefile, name of the source TEI-XML file
	 * @param countParagraph, number of the current paragraph
	 * @param writerGraph, where to write the final graph (for debugging purposes)
	 * @param edgeFrequenceByLabel, frequency by predicate
	 * @return the chosen URIs
	 */
	@SuppressWarnings("rawtypes")
	public static Map<String, String> simplifyGraphsAndCalculateCentrality(
			SimpleDirectedWeightedGraph<String, LabeledEdge> graph,
			Map<String, List<List<String>>> mentionsWithURIs,
			List<String> mentionsPerParagraph, String baseURIS,
			Map<String, String> invertedIndex, String measure,
			String preferedURI, String namefile,
			Integer countParagraph, FileWriterWithEncoding writerGraph, 
			Map<String, Double>edgeFrequenceByLabel, 
			Map<String, Double> choosenScoresperMention) {

		Map<String, String> choosenUris = new HashMap<String, String>();

		try {
			
			List<String> urisColoredNodes = new ArrayList<String>();
			for (String mention : mentionsPerParagraph) {
				List<List<String>> listUrisCurrentMention = mentionsWithURIs
						.get(mention);
				if (listUrisCurrentMention != null) {
					for (List<String> listUris : listUrisCurrentMention) {
						for (String uri : listUris) {
							String baseURL = baseURIS.trim();
								if (uri.contains(baseURL)) { // avoid some URI
									urisColoredNodes.add(uri);
									if (!urisColoredNodes
											.contains(Util.decompose(uri)))
										urisColoredNodes.add(Util.decompose(uri)); 
									//the candidates

								}
							}
					}
				}
			}

			// filtering non-interesting nodes and edges
			List<String> vertexToDelete = new ArrayList<String>();
			for (String vertex : graph.vertexSet()) {
				if (!urisColoredNodes.contains(vertex)) {
					Set<String> vertexCheck = new HashSet<String>();

					for (LabeledEdge edgeOfVertex : graph.edgesOf(vertex)) {
						String vertex1 = graph.getEdgeSource(edgeOfVertex);
						String vertex2 = graph.getEdgeTarget(edgeOfVertex);
						if (!vertex1.equals(vertex)
								&& urisColoredNodes.contains(vertex1)) {
							vertexCheck.add(invertedIndex.get(vertex1));
						}
						if (!vertex2.equals(vertex)
								&& urisColoredNodes.contains(vertex2)) {
							vertexCheck.add(invertedIndex.get(vertex2));
						}
					}
					if (vertexCheck.size() < 2) {
						vertexToDelete.add(vertex);
					}
				}
			}
			graph.removeAllVertices(vertexToDelete);
			
			
			//for debugging: relation frequency
			for (LabeledEdge edge : graph.edgeSet()) {
				if (edgeFrequenceByLabel.get(edge.toString()) == null) {
					edgeFrequenceByLabel.put(edge.toString(), 1.0);
				} else {
					Double val = edgeFrequenceByLabel.get(edge.toString());
					val++;
					edgeFrequenceByLabel.put(edge.toString(), val);
				}
			}
			
			// calculate centrality
			CentralityMeasure<String> cm = null;
			logger.info("Centrality measure used is " + measure);
			if (measure.equals("DegreeCentrality")) {
				cm = new DegreeCentrality<String, LabeledEdge>(graph);
			} else if (measure.equals("BrandesBetweennessCentrality")) {
				cm = new BrandesBetweennessCentrality<String, LabeledEdge>(
						graph);
			} else if (measure.equals("FreemanClosenessCentrality")) {
				cm = new FreemanClosenessCentrality<String, LabeledEdge>(
						graph);
			} else if (measure.equals("EigenvectorCentrality")) {				
				cm = new EigenvectorCentrality<String, LabeledEdge>(graph);
			} else {
				System.out.println("please provide valid centrality measure");
				return null;
			}
			if (cm != null) {
				for (String key : mentionsWithURIs.keySet()) {
					Map<String, Double> results = new HashMap<String, Double>();
					List<List<String>> listuris = mentionsWithURIs.get(key);
					if (listuris != null) {
						for (List<String> uris : listuris) {
							for (String uri : uris) {
								if (urisColoredNodes.contains(Util.decompose(uri))) {
									CentralityResult<String> cr = cm
											.calculate();
									Double val = cr.get(Util.decompose(uri));
									if (cr.get(Util.decompose(uri)) != null) {
										results.put(Util.decompose(uri), val);
									} else {
										results.put(Util.decompose(uri), 0.0);
									}
								}
							}
						}
					}
					Map<String, Double> orderedMap = Util.sortByValue(results);
					logger.info("For mention: " + key);
					for (String ur : orderedMap.keySet()) {
						if (orderedMap.get(ur) != 0) {
							logger.info("Centrality of " + ur + " is: "
									+ orderedMap.get(ur));
						}
					}
					// choose the highest
					String[] o = {};
					o = orderedMap.keySet().toArray(o);
					if (o.length > 0) { // there are uris
						// select preferred URI, the one defined in
						// config.parameters
						String selectedURI = "";
						String correspondingMention = invertedIndex
								.get(o[o.length - 1]);
						List<List<String>> correspondingURIs = mentionsWithURIs
								.get(correspondingMention);
						for (List<String> uris : correspondingURIs) {
							if (uris.contains(o[o.length - 1])) { // it is the
																	// right
																	// list
								boolean found = false;
								for (String uri : uris) {
									if (preferedURI.equals("ALL")) {
										selectedURI += uri + " ";
										found = true;
									} else if (uri.contains(preferedURI)) {
										selectedURI = uri;
										found = true;
									}
								}
								if (!found) {
									selectedURI = o[o.length - 1]; // default
																	// URI
								}

							}
						}
						choosenUris.put(key, selectedURI.trim());
						choosenScoresperMention.put(key, orderedMap.get(o[o.length - 1]));
					}
				}
				// printing graph
				writerGraph.write("Paragraph# " + countParagraph + "\n");
				Util.printGraph(graph, writerGraph);
			}			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return choosenUris;

	}
	
}
