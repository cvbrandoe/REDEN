package fr.lip6.reden.nelinker;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.SimpleSelector;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.util.FileManager;
import org.apache.jena.util.iterator.ExtendedIterator;

/**
 * This class implements the method for RDF data retrieval and graph fusion.
 * 
 * @author Brando & Frontini
 */
public class GraphHandlerNEL {

	private static Logger logger = Logger.getLogger(GraphHandlerNEL.class);
	
	/**
	 * Stores RDF files into a local folder.
	 * @param rdfData
	 * @param mentionsWithURIs
	 * @param baseUris
	 */
	public static void retrieveBaseRDFData(String rdfData, Map<String, List<List<String>>> mentionsWithURIs,
			String baseUris) {
		
		File dirF = new File(rdfData);
		if (!dirF.exists())
			dirF.mkdir();
		List<String> alreadyProcessedURI = new ArrayList<String>();
		for (List<List<String>> uriLists : mentionsWithURIs.values()) {
			for (List<String> uriList : uriLists) {
				for (String uri : uriList) {
					//only allow uris from the KBs configured in the config.properties (baseURIs)
					if (uri.contains(baseUris.trim())) { 
						if (!alreadyProcessedURI.contains(uri)) {
							retrieveRDF(uri, rdfData);
							alreadyProcessedURI.add(uri);
						}
					}
				}
			}
		}		
	}
	
	/**
	 * Create RDF model with URIs from base KB from local files.
	 * @param rdfData
	 * @param mentionsWithURIs
	 * @param baseUri
	 * @return
	 */
	public static Model loadBaseRDFModel(String rdfData, Map<String, List<List<String>>> mentionsWithURIs,
			String baseUri) {
		
		Model model = ModelFactory.createDefaultModel();
		File dirF = new File(rdfData);
		if (dirF.exists()) {			
			List<String> alreadyProcessedURI = new ArrayList<String>();
			for (List<List<String>> uriLists : mentionsWithURIs.values()) {
				for (List<String> uriList : uriLists) {
					for (String uri : uriList) {
						//only allow uris from the KBs configured in the config.properties (baseURIs)
						if (uri.contains(baseUri.trim())) { 
							if (!alreadyProcessedURI.contains(uri)) {
								if (new File(rdfData + "/file" + Util.replaceNonAlphabeticCharacters(uri)+ ".n3").exists()) {
									model.read(rdfData + "/file"
											+ Util.replaceNonAlphabeticCharacters(uri) + ".n3");
									alreadyProcessedURI.add(uri);
								}
							}
						}
					}
				}
			}
		}		
		return model;
	}

	/**
	 * Download RDF data from the URIs of resources referenced via equivalence links and loads them into the given model.
	 * Equivalent resources may also be available in the dictionary.
	 * @param rdfData
	 * @param mentionsWithURIs
	 * @param baseURL
	 * @param crawlSameAs
	 * @param sameAsproperty
	 */
	public static Model retrieveAndLoadSameAsRDFData(Model model, String rdfData, Map<String, List<List<String>>> mentionsWithURIs,
			String baseURL, String crawlSameAs, String sameAsproperty, Map<String,Set<String>> baseURIsAndEquivalentURIs, 
			String kBsLocalNoNetwork) {
		
		Property prop = model
				.getProperty(sameAsproperty);
		File dirF = new File(rdfData);
		if (!dirF.exists())
			dirF.mkdir();
		
		List<String> alreadyProcessedURI = new ArrayList<String>();
		for (List<List<String>> uriLists : mentionsWithURIs.values()) {
			for (List<String> uriList : uriLists) {
				Set<String> sameAsUris = new HashSet<String>();
				String baseURI = "";
				for (String uri : uriList) {
					//case 1	
					if (uri.contains(baseURL.trim()) ) { //base KB  
						baseURI = uri;
						//look for sameAs links within the model
						Resource individualSameAs = model.getResource(uri);
						SimpleSelector ss = new SimpleSelector(
									individualSameAs, prop, (RDFNode) null);
						ExtendedIterator<Statement> iter = model.listStatements(ss);
						while (iter.hasNext()) {
							Statement stmt = iter.next();
							RDFNode object = stmt.getObject();
							String newURI = Util.replaceNonAlphabeticCharacters(Util.decompose(object.toString()));
							String filename = rdfData + "/file" + newURI + ".n3";
							if (!crawlSameAs.equalsIgnoreCase("ALL")) {
								if (object.toString().startsWith(crawlSameAs)) {
									//download equivalent resource using a give prefix
									if (kBsLocalNoNetwork.equalsIgnoreCase("false"))
										retrieveRDF(Util.decompose(object.toString()), rdfData);																
									if (new File(filename).exists()) {
										if (!alreadyProcessedURI.contains(object.toString())) { //avoid loading twice
											sameAsUris.add(Util.decompose(object.toString()));	
											model.read(filename);
											alreadyProcessedURI.add(object.toString());
										}
									}
								}
							} else {
								//download equivalent resource (without filter)
								if (kBsLocalNoNetwork.equalsIgnoreCase("false"))
									retrieveRDF(Util.decompose(object.toString()), rdfData);
								if (new File(filename).exists()) {
									if (!alreadyProcessedURI.contains(object.toString())) { //avoid loading twice
										sameAsUris.add(Util.decompose(object.toString()));	
										model.read(filename);
										alreadyProcessedURI.add(object.toString());
									}
								}
							}
						}							
					} else { //case 2: if URI of equivalent resources are available in the dictionary, these are loaded
						String newURI = Util.replaceNonAlphabeticCharacters(Util.decompose(uri));
						String filename = rdfData + "/file" + newURI + ".n3";
						if (kBsLocalNoNetwork.equalsIgnoreCase("false"))
							retrieveRDF(Util.decompose(uri), rdfData);	//download data															
						if (new File(filename).exists()) {
							if (!alreadyProcessedURI.contains(uri)) { //avoid loading twice
								model.read(filename); //load it
								sameAsUris.add(Util.decompose(uri));
								alreadyProcessedURI.add(uri);
							}
						}													
					}										
				}
				if (baseURIsAndEquivalentURIs.get(baseURI) == null) {
					baseURIsAndEquivalentURIs.put(baseURI, sameAsUris);
				} else {
					baseURIsAndEquivalentURIs.get(baseURI).addAll(sameAsUris);
				}
				
			}
		}
		/*try {
			RDFDataMgr.write(new FileOutputStream(new File("output/rdfgraph.txt")), model, Lang.TURTLE) ;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}*/
		return model;
	}
	
	/**
	 * Fuse and JGraphT conversion.
	 * @param model
	 * @param mentionsWithURIs
	 * @param relsFile
	 * @param crawlSameAs
	 * @param sameAsProperty
	 * @param baseURIsAndEquivalentURIs
	 * @param baseURI
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public static SimpleDirectedWeightedGraph<String, LabeledEdge> fuseRDFGraphsIntoJGTGraph(
			Model model, Map<String, List<List<String>>> mentionsWithURIs, 
			File relsFile, String crawlSameAs, String sameAsProperty,
			Map<String,Set<String>> baseURIsAndEquivalentURIs, String baseURI) {

		baseURI = baseURI.trim();
		SimpleDirectedWeightedGraph<String, LabeledEdge> graph = new SimpleDirectedWeightedGraph<String, LabeledEdge>(
				LabeledEdge.class);
		Property prop = model.getProperty(sameAsProperty);

		// the set of URIs of mentions		
		Set<String> mentions = mentionsWithURIs.keySet();
		List<String> baseUris = new ArrayList<String>();
		for (String mention : mentions) {
			List<List<String>> mentionUris = mentionsWithURIs.get(mention);
			for (List<String> uriL : mentionUris) {
				for (String uri : uriL) {
					if (uri.contains(baseURI.trim())) { //it is an URI from the base KB
						baseUris.add(uri);
					}
				}
			}
		}
		
		//assigning weights (if relation file available)
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
		
			for (String uri : baseUris) {
				Resource individual = model.getResource(uri);
				Set<String> sameAsURIsIndividual = baseURIsAndEquivalentURIs.get(individual.getURI());
	
				String vertex1 = Util.decompose(uri);
				graph.addVertex(vertex1);
				for (String uriAlias : sameAsURIsIndividual) {
					if (!uri.equalsIgnoreCase(uriAlias)) { //avoiding loops
						Resource individualSameAs = model.getResource(uriAlias);
						// obtaining properties of every URI concerning a single
						// individual
						SimpleSelector ss = new SimpleSelector(individualSameAs,
								(Property) null, (RDFNode) null);
						ExtendedIterator<Statement> iter = model.listStatements(ss);
						Boolean isEmpt = false;
						while (iter.hasNext()) {
							isEmpt = true;
							Statement stmt = iter.next();
							Property predicate = stmt.getPredicate();
							RDFNode object = stmt.getObject();
							if (!predicate.equals(prop)) { // other predicates != than sameAs
								String vertex2 = Util.decompose(object.toString());
								if (!vertex1.equalsIgnoreCase(vertex2)) {
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
									//System.out.println("VERTEX1 "+vertex1 + " VERTEX2: "+vertex2 + " edge: "+edge);
								}
							}
						}
						if (!isEmpt) { //test different encoding, particular to old DBpedia URIs in ASCII-US, find proper fix
							String[] part = uriAlias.split("/");
							String encoUri =  URLEncoder.encode(part[part.length-1], "UTF-8");
							individualSameAs = model.getResource(uriAlias.replace(part[part.length-1], encoUri));
							
							// obtaining properties of every URI concerning a single
							// individual
							ss = new SimpleSelector(individualSameAs,
									(Property) null, (RDFNode) null);
							iter = model.listStatements(ss);
							while (iter.hasNext()) {
								Statement stmt = iter.next();
								Property predicate = stmt.getPredicate();
								RDFNode object = stmt.getObject();
								if (!predicate.equals(prop)) { // other predicates != than sameAs
									String vertex2 = Util.decompose(object.toString());
									if (!vertex1.equalsIgnoreCase(vertex2)) {
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
										//System.out.println("VERTEX1 "+vertex1 + " VERTEX2: "+vertex2 + " edge: "+edge);
									}
								}
							}
							
						}
					}				
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		logger.info("vertex size: " + graph.vertexSet().size());
		logger.info("edge size: " + graph.edgeSet().size());
		return graph;

	}


	/**
	 * Retrieves the RDF from a given resource by its URI.
	 * @param uri
	 * @param dir
	 */	
	public static void retrieveRDF(String uri, String dir) {
		try {
			File f = new File(dir + "/file" + Util.replaceNonAlphabeticCharacters(uri) + ".n3");
			// to go faster (remove f.exists if we want
			// to update local triples)
			if (!f.exists() || FileUtils.readFileToString(f).trim().isEmpty()) {
				Model model = ModelFactory.createDefaultModel();
				if (uri.contains("dbpedia")) {
					InputStream in = FileManager.get().open(uri+".ntriples"); //TODO can be generic, to test
					if (in != null) {
						model.read(in, null, "N3");	
					} else {
						logger.info("skip URI: " + uri);
						return;
					}
				} else {
					model.read(uri);
				}

				OutputStream fileOutputStream = new FileOutputStream(f);
				OutputStreamWriter out = new OutputStreamWriter(
						fileOutputStream, "UTF-8");
				model.write(out, "N3");
				logger.info("downloaded from uri: " + uri + " and file " + dir + "/file"
						+ Util.replaceNonAlphabeticCharacters(uri) + ".n3");				
				out.close();
				fileOutputStream.close();				

			} else {				
			}
		} catch (Exception ignore) {
			logger.info("problem with URI (not found or bad syntax): " + uri); //not found or bad syntax, etc. so ignore
		}
	}
	
}
