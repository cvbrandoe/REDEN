package fr.lip6.reden;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.output.FileWriterWithEncoding;
import org.apache.log4j.Logger;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

import dk.aaue.sna.alg.centrality.BrandesBetweennessCentrality;
import dk.aaue.sna.alg.centrality.CentralityMeasure;
import dk.aaue.sna.alg.centrality.CentralityResult;
import dk.aaue.sna.alg.centrality.DegreeCentrality;
import dk.aaue.sna.alg.centrality.EigenvectorCentrality;
import dk.aaue.sna.alg.centrality.FreemanClosenessCentrality;

/**
 * This class implements methods related to the graph fusion.
 * 
 * @author @author Brando & Frontini - Labex OBVIL - Université Paris-Sorbonne - UPMC
 *         LIP6
 */
public class GraphProcessingNEL {

	private static Logger logger = Logger.getLogger(GraphProcessingNEL.class);
	
	/**
	 * @param models
	 *            , the RDF graphs for mentions per paragraph
	 * @param provBaseURI
	 *            , the base URI of the predicates to remove
	 * @return, the fuse JGraphT graph
	 */
	/**
	 * Fuses RDF graphs into a single graph JGraphT, fuse homologuous
	 * individuals thanks to SameAs links and aggregates links of the duplicates
	 * into a single vertex.
	 * @param model, the RDF graphs for mentions per paragraph
	 * @param provBaseURI
	 * @param mentionsWithURIs, URIs per mention
	 * @param relsFile, where to write predicate frequence (for debugging purposes)
	 * @return the fuse graph
	 */
	@SuppressWarnings("rawtypes")
	public static SimpleDirectedWeightedGraph<String, LabeledEdge> fuseRDFGraphsIntoJGTGraph(
			Model model, String[] provBaseURI,
			Map<String, List<List<String>>> mentionsWithURIs, File relsFile, String crawlSameAs) {

		SimpleDirectedWeightedGraph<String, LabeledEdge> graph = new SimpleDirectedWeightedGraph<String, LabeledEdge>(
				LabeledEdge.class);
		Property prop = model
				.getProperty("http://www.w3.org/2002/07/owl#sameAs");
		Property propBDay = model.getProperty("http://vocab.org/bio/0.1/birth");
		Property propDDay = model.getProperty("http://vocab.org/bio/0.1/death");
		Date start = new Date();
		// the set of URIs of mentions
		Set<String> mentions = mentionsWithURIs.keySet();
		List<String> mentionUrisF = new ArrayList<String>();
		for (String mention : mentions) {
			List<List<String>> mentionUris = mentionsWithURIs.get(mention);
			for (List<String> uriL : mentionUris) {
				for (String uri : uriL) {
					mentionUrisF.add(uri);
				}
			}
		}
		HashMap<String, Double> relsAndWei = new HashMap<String, Double>();		
		try {
			if (relsFile != null) {
				if (relsFile.exists()) {
					FileReader fr = new FileReader(relsFile);
					BufferedReader br = new BufferedReader(fr); 
					String s; 
					while((s = br.readLine()) != null) { 
						String[] li = s.split(" ");
						relsAndWei.put(li[0].toLowerCase(), Double.parseDouble(li[1]));
					} 
					fr.close();
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// for all involved BNF URIs
		HashMap<String, Integer> referenceURIsBirthDayInfo = new HashMap<String, Integer>();
		HashMap<String, Integer> referenceURIsDeathDayInfo = new HashMap<String, Integer>();

		for (String uri : mentionUrisF) {
			Resource individual = model.getResource(uri);
			List<String> sameAsURIsIndividual = RDFProcessingNEL.obtainPotentiallyIdenticalIndividuals(
					individual.getURI(), model, crawlSameAs);

			//TODO particular for BnF data, add dates associated to authors as nodes in the graph. Avoid this.
			if (uri.contains("data.bnf.fr")) { 
				String vertex1 = RDFProcessingNEL.decompose(uri);
				graph.addVertex(vertex1);
				for (String uriAlias : sameAsURIsIndividual) {

					Resource individualSameAs = model.getResource(uriAlias);
					// obtaining properties of every URI concerning a single
					// individual
					SimpleSelector ss = new SimpleSelector(individualSameAs,
							(Property) null, (RDFNode) null);
					ExtendedIterator<Statement> iter = model.listStatements(ss);
					while (iter.hasNext()) {
						Statement stmt = iter.next();
						Property predicate = stmt.getPredicate();
						RDFNode object = stmt.getObject();
						if (predicate.equals(propBDay)) {
							referenceURIsBirthDayInfo.put(uri,
									DateSpecificProcessingNEL.processDate((String) object.asLiteral()
											.getValue()));
						}
						if (predicate.equals(propDDay)) {
							referenceURIsDeathDayInfo.put(uri,
									DateSpecificProcessingNEL.processDate((String) object.asLiteral()
											.getValue()));
						}
						if (!predicate.equals(prop)) {
							String vertex2 = RDFProcessingNEL.decompose(object.toString());
							List<String> targetAliases = RDFProcessingNEL.obtainPotentiallyIdenticalIndividuals(
									vertex2, model, crawlSameAs);
							if (targetAliases.size() == 1) {
								graph.addVertex(vertex2);
								LabeledEdge edge = new LabeledEdge<String>(
										vertex1, vertex2, predicate.getURI()); 
								//relation weight in parameter file
								Set<String> relsUris = relsAndWei.keySet();
								if (relsUris.contains(predicate.getURI().toLowerCase())) {
									for (String relsU : relsUris) {
										if (relsU.equals(predicate.getURI().toLowerCase())) {
											graph.setEdgeWeight(edge, relsAndWei.get(relsU));
										}
									}
								} else {
									graph.setEdgeWeight(edge, 1.0);
								}
								graph.addEdge(vertex1, vertex2, edge);
							}
						}
					}
				}
			}
		}
		logger.info("vertex size: " + graph.vertexSet().size());
		logger.info("edge size: " + graph.edgeSet().size());
		Date end = new Date();
		logger.info("Finished convertJenaGraphToJGraphT in "
				+ (end.getTime() - start.getTime()) / 60 + "secs");
		return graph;

	}

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
			List<String> mentionsPerParagraph, String[] baseURIS,
			Map<String, String> invertedIndex, String measure,
			String preferedURI, String namefile,
			Integer countParagraph, FileWriterWithEncoding writerGraph, 
			Map<String, Double>edgeFrequenceByLabel, 
			Map<String, Double> choosenScoresperMention) {

		Map<String, String> choosenUris = new HashMap<String, String>();

		try {
			
			Date start = new Date();
			List<String> urisColoredNodes = new ArrayList<String>();
			for (String mention : mentionsPerParagraph) {
				List<List<String>> listUrisCurrentMention = mentionsWithURIs
						.get(mention);
				if (listUrisCurrentMention != null) {
					for (List<String> listUris : listUrisCurrentMention) {
						for (String uri : listUris) {
							for (String baseURL2 : baseURIS) {
								String baseURL = baseURL2.trim();
								if (uri.contains(baseURL)) { // avoid idref URI
									urisColoredNodes.add(uri);
									if (!urisColoredNodes
											.contains(RDFProcessingNEL.decompose(uri)))
										urisColoredNodes.add(RDFProcessingNEL.decompose(uri));

								}
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
								if (urisColoredNodes.contains(RDFProcessingNEL.decompose(uri))) { // uri
																					// of
																					// a
																					// candidate
									CentralityResult<String> cr = cm
											.calculate();
									Double val = cr.get(RDFProcessingNEL.decompose(uri));
									if (cr.get(RDFProcessingNEL.decompose(uri)) != null) {
										results.put(RDFProcessingNEL.decompose(uri), val);
									} else {
										results.put(RDFProcessingNEL.decompose(uri), 0.0);
									}
								}
							}
						}
					}
					Map<String, Double> orderedMap = sortByValue(results);
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
				printGraph(graph, writerGraph);
			}
			Date end = new Date();
			logger.info("Finished simplifyGraphsAndCalculateCentrality in "
							+ (end.getTime() - start.getTime()) / 60 + "secs");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return choosenUris;

	}
	
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

}
