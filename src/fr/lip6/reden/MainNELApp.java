package fr.lip6.reden;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.FileWriterWithEncoding;
import org.apache.log4j.Logger;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import org.apache.jena.rdf.model.Model;

import fr.lip6.reden.enrichne.AuthorsEnrichment;
import fr.lip6.reden.enrichne.GeodataGeneration;
import fr.lip6.reden.enrichne.EnrichmentHandler;
import fr.lip6.reden.ldextractor.AppAdhoc;
import fr.lip6.reden.nelinker.CentralityHandler;
import fr.lip6.reden.nelinker.DicoProcessingNEL;
import fr.lip6.reden.nelinker.EvalInfo;
import fr.lip6.reden.nelinker.GraphHandlerNEL;
import fr.lip6.reden.nelinker.LabeledEdge;
import fr.lip6.reden.nelinker.ResultsAndEvaluationNEL;

/**
 * This class implements the main method to launch the proposed 
 * graph-based algorithm and domain-adapted named entity linker (NEL).
 * 
 * @author @author Brando & Frontini
 */
public class MainNELApp {

	private static Logger logger = Logger.getLogger(MainNELApp.class);
	
	static Map<String, Double> edgeFrequenceByLabel = new HashMap<String, Double>();
	
	public static void main(String[] args) {
		if (args.length > 0 && args.length <= 6) {
			Date start = new Date();	
			namedEntityLinking(args);
			Date end = new Date();
			logger.info("REDEN finished in "+ (end.getTime() - start.getTime()) / 60 + " secs");
		} else {
			System.out.println("Three modes possible for providing arguments: "
					+ "1) <config_file> <tei-fileName.xml> [-printEval] [-createIndex] [-relsFile=<file>] [-outDir=<dir>] or"
					+ "2) <config_file> -createDico=bnf|bnf-all|dbpediafr|dbpediafr-author|getty-per|bne|bne-all|all|LGD-loc"
					+ "3) <config_file> <tei-fileName-withURIs.xml> -produceData4Visu=<output.json> -propsFile=<config_ld_properties>");
		}
	}

	/**
	 * Named entity linking main method.
	 * 
	 * @param propertiesFile
	 *            , the parameter file
	 */
	@SuppressWarnings("rawtypes")
	public static void namedEntityLinking(String[] args) {

		try {

			String propertiesFile = args[0];
			Date startMain = new Date();
			
			Map<String, String> argsMap = processArguments(args);
			
			//only builds the dico, skips NEL
			if (argsMap.containsKey("createDico")) {
				AppAdhoc.crawlsLinkedData(argsMap.get("config"), argsMap.get("createDico"));
				logger.info("Building dictionary for NEL");
				return;
			}
			
			if (!argsMap.containsKey("tei")) { //there is no TEI file
				System.out.println("Three modes possible for providing arguments: "
						+ "1) <config_file> <tei-fileName.xml> [-printEval] [-createIndex] [-relsFile=<file>] [-outDir=<dir>] or"
						+ "2) <config_file> -createDico=bnf|bnf-all|dbpediafr|dbpediafr-author|getty-per|bne|bne-all|all|LGD-loc"
						+ "3) <config_file> <tei-fileName-withURIs.xml> -produceData4Visu=<output.json> -propsFile=<config_ld_properties>");
				return;
			}
			
			// reading parameters
			Properties prop = new Properties();
			InputStream input = new FileInputStream(propertiesFile);
			prop.load(input);
			String annotationTag = prop.getProperty("namedEntityTag");
			String cl = prop.getProperty("NERclassName");
			//String[] baseUris = prop.getProperty("baseURIs").split(",");
			String baseUris = prop.getProperty("baseURIs");
			String measure = prop.getProperty("centralityMeasure");
			String useindex = prop.getProperty("useDicoIndex");
			String indexDir = prop.getProperty("indexDir");
			String preferedURI = prop.getProperty("preferedURIOrder");
			String nameMainFolderDico = prop.getProperty("nameMainFolderDico");
			String rdfData = prop.getProperty("rdfData");
			String xpathExpresion= prop.getProperty("xpathExpresion");
			String propertyTagRef = prop.getProperty("propertyTagRef");
			String addScores = prop.getProperty("addScores");
			String crawlSameAs = prop.getProperty("crawlSameAs");
			String sameAsproperty = prop.getProperty("sameAsproperty");
			String kBsLocalNoNetwork = prop.getProperty("KBsLocalNoNetwork");
			
			//produces visualization data, skips NEL
			if (argsMap.containsKey("produceData4Visu")) {
				
				if (annotationTag.toLowerCase().startsWith("placename")) {
					//reads TEI, gets toponyms and retrieve RDF
					Map<String, Map<String, String>> toponyms = EnrichmentHandler.readTEI(argsMap.get("tei"),
							propertyTagRef,	xpathExpresion, annotationTag, rdfData);
					//attribute geo-coordinates
					toponyms = GeodataGeneration.assignGeoCoordinates(toponyms, argsMap.get("propsFile"), rdfData);
					//produces the GeoJson file
					EnrichmentHandler.toJson(toponyms, argsMap.get("produceData4Visu"));					
					return;
				} else if (annotationTag.toLowerCase().startsWith("persname")) {
					// e.g: output\thibaudet_reflexions-outV3-enrichment.xml output\authorInformation.json bnf config\authors.properties ref_auto
					//reads TEI, gets authors and retrieve RDF
					Map<String, Map<String, String>> authors = EnrichmentHandler.readTEI(argsMap.get("tei"), propertyTagRef, xpathExpresion, annotationTag, rdfData);
					//attribute pics
					authors = AuthorsEnrichment.assignAuthorsPropValue(authors,  argsMap.get("propsFile"), rdfData);
					//produces the GeoJson file
					EnrichmentHandler.toJson(authors, argsMap.get("produceData4Visu")); //TODO here, change to JSON instead GeoJson
					return;
				} else {
					System.out.println("Set appropriate value to the namedEntityTag property in the Reden configuration file");
					return;
				}
			}
			
			if (argsMap.containsKey("printEval") && !new File(argsMap.get("tei").replace(".xml", "-gold.xml")).exists()) {
				System.out.println("Gold file doesn't exist: "
						+ argsMap.get("tei").replace(".xml", "-gold.xml"));
				return;
			}
			
			//reading relation weight parameter file
			File relsFile = null;
			if (argsMap.containsKey("relsFile")) {
				relsFile = new File(argsMap.get("relsFile"));
			}
			
			//if output directory is parameter
			String outDir = "";
			if (argsMap.containsKey("outDir")) {
				outDir = argsMap.get("outDir");
				new File(outDir).mkdir();				
				outDir += "/";
			}
						
			// checking if we need to create an index
			if (argsMap.containsKey("createIndex")) {
				logger.info("(Re-)create index");
				int ind = 0;
				for (String indexDirName : indexDir.split(",")) { 
					FileUtils.deleteDirectory(new File(indexDirName.trim()));
					DicoProcessingNEL.createIndex(indexDir, nameMainFolderDico.split(",")[ind].trim(), "nameForm");
				}
			}
			int countMention = 0;
			int countParagraph = 0;

			//check input TEI files
			DocumentBuilder b = DocumentBuilderFactory.newInstance()
					.newDocumentBuilder();
			File inputFiles = new File(argsMap.get("tei"));

			// check file or folder
			List<File> files = new ArrayList<File>();
			if (inputFiles.isFile()) {
				files.add(inputFiles);
			} else if (inputFiles.isDirectory()) {
				File[] f = inputFiles.listFiles();
				for (int j = 0; j < f.length; j++) {
					files.add(f[j]);
				}
			} else {
				System.out.println("Input neither file nor folder");
			}

			//NEL evaluation information
			List<Map<String, List<List<String>>>> allMentionsWithUrisPerContextinText =
					new ArrayList<Map<String, List<List<String>>>>();
			
			for (int j = 0; j < files.size(); j++) {
				
				//cleaning output files
				new File(outDir+files.get(j).getName().replace(".xml",
						"-resFinalGraphsV3.txt")).delete();
				
				FileWriterWithEncoding writerGraph = new FileWriterWithEncoding(outDir+files.get(j).getName().replace(".xml",
						"-resFinalGraphsV3.txt"), "UTF-8");
			
				//info about ambiguous mentions
				String nameAmbFile = outDir+files.get(j).getName().replace(".xml", "-ambigousMentions.txt");
				new File(nameAmbFile).delete();
				
				FileWriterWithEncoding ambigF = new FileWriterWithEncoding(nameAmbFile, "UTF-8");
				
				org.w3c.dom.Document doc = b.parse(new FileInputStream(files
						.get(j)));
				XPath xPath = XPathFactory.newInstance().newXPath();
				NodeList nodes = (NodeList) xPath.evaluate(
						xpathExpresion,
						doc.getDocumentElement(), XPathConstants.NODESET);
				
				for (int i = 0; i < nodes.getLength(); ++i) {
					Map<String, List<List<String>>> allMentionsWithURIs = new HashMap<String, List<List<String>>>();
					List<String> allAnnotationsParagraph = new ArrayList<String>();
					
					Element e = (Element) nodes.item(i);
					int ind = 0;
					for (String annoTag : annotationTag.split(",")) {
						List<String> annotationsParagraph = new ArrayList<String>();
						Map<String, List<List<String>>> mentionsWithURIs = new HashMap<String, List<List<String>>>();
						NodeList nodesChild = (NodeList) xPath.evaluate(".//"
								+ annoTag, e, XPathConstants.NODESET);
						for (int k = 0; k < nodesChild.getLength(); ++k) {
							Element child = (Element) nodesChild.item(k);
							annotationsParagraph.add(child.getTextContent());
							countMention++;
						}
						allAnnotationsParagraph.addAll(annotationsParagraph);
						logger.info("processing text portion according to chosen context #"
							+ countParagraph);
						// look for URIs in dictionary
						if (useindex.equalsIgnoreCase("true")) { // version with
																// index
							mentionsWithURIs = DicoProcessingNEL.retrieveMentionsURIsFromDicoWithIndex(
								cl, annotationsParagraph, indexDir.split(",")[ind].trim());
						} else {
							//DEPRECATED: mentionsWithURIs = DicoProcessingNEL.retrieveMentionsURIsFromDico(
							//		nameMainFolderDico, cl, annotationsParagraph);
						}
						allMentionsWithURIs.putAll(mentionsWithURIs); 
						//TODO we overwrite for instance persons named "France" by the place called also "France"
						ind++;
					}
					logger.info("Total number of mentions (all NE types included): "+allMentionsWithURIs.size());
					
					String caseR = checkConditionsToNEL(allMentionsWithURIs,
							allAnnotationsParagraph);
					// Regular case
					if (caseR.equalsIgnoreCase("OK")) {
						
						//printing number of ambiguous URIs per mention
						int countNumberURIsToProcess = 0;
						Iterator<String> mentions = allMentionsWithURIs.keySet().iterator();
						while (mentions.hasNext()) {
							String mention = mentions.next();
							Integer sizeReferents = allMentionsWithURIs.get(mention).size();
							countNumberURIsToProcess += sizeReferents;
							logger.info("Number of referents (URIs) for "+mention+ " is "+sizeReferents);
							ambigF.write("Number of referents (URIs) for "+mention+ " is "+sizeReferents+"\n");							
						}
						logger.info("Total number of URIs to process (very approximative) : "+ countNumberURIsToProcess);
						
						//download base RDF data NEW VERSION
						if (kBsLocalNoNetwork.equalsIgnoreCase("false"))
							GraphHandlerNEL.retrieveBaseRDFData(rdfData, allMentionsWithURIs, baseUris);						
						
						//load base model (for minimizing possible errors, we separate both steps)
						Model model = GraphHandlerNEL.loadBaseRDFModel(rdfData, allMentionsWithURIs, baseUris);
						
						if (model != null) {
						
							Map<String,Set<String>> baseURIsAndEquivalentURIs = new HashMap<String,Set<String>>();
							
							//download RDF data via sameAs links and loads them into memory
							model = GraphHandlerNEL.retrieveAndLoadSameAsRDFData(model, rdfData, allMentionsWithURIs, 
									baseUris, crawlSameAs, sameAsproperty, baseURIsAndEquivalentURIs, kBsLocalNoNetwork);
							
							SimpleDirectedWeightedGraph<String, LabeledEdge> graph =
									GraphHandlerNEL.fuseRDFGraphsIntoJGTGraph(
									model, allMentionsWithURIs, relsFile, crawlSameAs, sameAsproperty, 
									baseURIsAndEquivalentURIs, baseUris);
							
							// Simplify graph, compute centrality, choose the higher score
							Map<String, Double> choosenScoresperMention = new HashMap<String, Double>();
							
							// create inverted index
							Map<String, String> invertedIndex = DicoProcessingNEL.buildInvertedIndex(allMentionsWithURIs);
							
							Map<String, String> choosenUris = CentralityHandler.simplifyGraphsAndCalculateCentrality(
									graph, allMentionsWithURIs, allAnnotationsParagraph,
									baseUris, invertedIndex, measure, preferedURI, files
									.get(j).getName(), countParagraph, writerGraph, edgeFrequenceByLabel, choosenScoresperMention); 
							
							// write results in TEI
							if (choosenUris != null) {
								for (String annoTag : annotationTag.split(",")) {
									//TODO does not deal with for instance persons named "France" by the place called also "France", 
									//it chooses the latest NE type of the list provided in the config
									ResultsAndEvaluationNEL.produceResults(files.get(j), annoTag.trim(),  
										choosenUris, allMentionsWithURIs, e, doc, outDir, propertyTagRef, 
										choosenScoresperMention, addScores);	
								}
							}
							logger.info("finish");
						}
					} else if (caseR.equalsIgnoreCase("NoMentionsAnnotated")) {
						logger.info("Current paragraph does not need to be processed: there is no mentions in document");
					} else {
						logger.info("There are no ambiguous mentions");
						for (String annoTag : annotationTag.split(",")) {
							ResultsAndEvaluationNEL.produceResultsSimple(files.get(j), annoTag.trim(),
									allMentionsWithURIs, e, doc, outDir, propertyTagRef);
							// case: "NoAmbiguity", when there are no
							// ambiguities, assign them directly to the TEI output
							// file
						}
					}
					countParagraph++;
					allMentionsWithUrisPerContextinText.add(allMentionsWithURIs);
				}
				logger.info("number of all mentions (including duplicates): "
								+ countMention + " in file " + files.get(j));
				//print relation frequency per label
				ResultsAndEvaluationNEL.printRelationFrequency(edgeFrequenceByLabel, files.get(j).getName(), outDir);
				writerGraph.close();
				ambigF.close();
			}
			Date endMain = new Date();
			logger.info("Global Time: "
					+ (endMain.getTime() - startMain.getTime()) / 60 + "secs");

			// evaluation (reading the gold)
			if (argsMap.containsKey("printEval")) {
				String output = argsMap.get("tei").replace(".xml", "-outV3.xml");
				String[] output2 = output.split("/");
				if (new File(outDir+output2[output2.length-1]).exists()) {
					logger.info("Printing evaluation");
					//compare with gold and collect information necessary to compute the results
					List<EvalInfo> collectedResults = ResultsAndEvaluationNEL.compareResultsWithGold(argsMap.get("tei"), annotationTag, xpathExpresion, outDir, propertyTagRef, allMentionsWithUrisPerContextinText);
					//compute final results
					ResultsAndEvaluationNEL.computeFinalResults(collectedResults);
					
				} else {
					System.out.println("Output file doesn't exist: " + outDir+output2[output2.length-1]);
				}
			}
		} catch (ParserConfigurationException e1) {
			e1.printStackTrace();
		} catch (XPathExpressionException e1) {
			e1.printStackTrace();
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (SAXException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	
	/**
	 * Organize command line input arguments into a map to facilitate manipulation in main.
	 * @param args
	 * @return
	 */
	private static Map<String, String> processArguments(String[] args) {
		
		Map<String, String> argMap = new HashMap<String,String>();
		// first argument is always config file
		argMap.put("config", args[0]);		
		for (String argA : args) {
			if (argA.endsWith(".xml")) {
				argMap.put("tei", argA);				
			} else if (argA.equals("-printEval")) {
				argMap.put("printEval", "true");				
			} else if (argA.equals("-createIndex")) {
				argMap.put("createIndex", "true");
			} else if (argA.startsWith("-relsFile")) {
				argMap.put("relsFile", argA.split("=")[1].trim());
			} else if (argA.startsWith("-outDir")) {
				argMap.put("outDir", argA.split("=")[1].trim());
			} else if (argA.startsWith("-produceData4Visu")) {
				argMap.put("produceData4Visu", argA.split("=")[1].trim());				
			} else if (argA.startsWith("-createDico")) {
				argMap.put("createDico", argA.split("=")[1].trim());
			} else if (argA.startsWith("-propsFile")) {
				argMap.put("propsFile", argA.split("=")[1].trim());
			} 
		}		
		return argMap;		
	}

	/**
	 * It checks whether the conditions that need to be fulfilled to use the
	 * proposed NEL method: there are at least two mentions and one of them is
	 * ambiguous.
	 * 
	 * @param mentionsWithURIs
	 *            , the mentions and their URIs for every possible candidate
	 * @param annotationsParagraph
	 *            , the list of mentions
	 * @return whether the conditions are fulfilled or not
	 */
	public static String checkConditionsToNEL(
			Map<String, List<List<String>>> mentionsWithURIs,
			List<String> annotationsParagraph) {
		if (annotationsParagraph.size() == 0) {
			return "NoMentionsAnnotated";
		} else {
			Boolean isIn = false;
			int counter = 0;
			while (counter < annotationsParagraph.size() && !isIn) {
				String mention = annotationsParagraph.get(counter);

				if (mentionsWithURIs.get(mention) != null) {
					if (mentionsWithURIs.get(mention).size() > 1) {
						isIn = true; // there is at least one ambiguous mention
					}
				}
				counter++;
			}
			if (!isIn) {
				return "NoAmbiguity";
			}
		}
		return "Ok";

	}
}
