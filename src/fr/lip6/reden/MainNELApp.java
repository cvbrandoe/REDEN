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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.FileWriterWithEncoding;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.hp.hpl.jena.rdf.model.Model;

import fr.lip6.ldcrawler.AppAdhoc;

/**
 * This class implements the main method to launch the proposed 
 * graph-based algorithm and domain-adapted named entity linker (NEL).
 * 
 * @author @author Brando & Frontini - Labex OBVIL - Université Paris-Sorbonne - UPMC
 *         LIP6
 */
public class MainNELApp {

	static Map<String, Double> edgeFrequenceByLabel = new HashMap<String, Double>();
	
	public static void main(String[] args) {
		if (args.length > 0 && args.length <= 5) {
			namedEntityLinking("config/config.properties", args);
		} else {
			System.out.println("Two modes possible for providing arguments: "
							+ "1) <tei-fileName.xml> [-printEval] [-createIndex] [-relsFile=<file>] [-outDir=<dir>] or"
							+ "2) -createDico bnf|dbpediafr|all");
		}
	}

	/**
	 * Named entity linking main method.
	 * 
	 * @param propertiesFile
	 *            , the parameter file
	 */
	@SuppressWarnings("rawtypes")
	public static void namedEntityLinking(String propertiesFile, String[] args) {

		try {

			Date startMain = new Date();
			
			//only builds the dico, skips NEL
			if (args[0].equals("-createDico")) {
				AppAdhoc.crawlsLinkedData("config/config.properties", args[1]);
				System.out.println("Building dictionary for NEL");
				return;
			}
						
			if (!args[0].endsWith(".xml")) {
				System.out.println("Two modes possible for providing arguments: "
						+ "1) <tei-fileName.xml> [-printEval] [-createIndex] [-relsFile=<file>] [-outDir=<dir>] or"
						+ "2) -createDico bnf|dbpediafr|all");
				return;
			}

			if ( ( (args.length == 2 && args[1].equals("-printEval")) || (args.length == 3 && ( args[1].equals("-printEval") || args[2].equals("-printEval") )) )
					&& !new File(args[0].replace(".xml", "-gold.xml")).exists()) {
				System.out.println("Gold file doesn't exist: "
						+ args[0].replace(".xml", "-gold.xml"));
				return;
			}

			//reading relation weight parameter file
			File relsFile = null;
			for (String r : args) {
				if (r.contains("-relsFile")) {
					relsFile = new File(r.split("=")[1]);
				}
			}
			
			//if output directory is parameter
			String outDir = "";
			for (String r : args) {
				if (r.contains("-outDir")) {
					outDir =r.split("=")[1];
					new File(outDir).mkdir();
					outDir += "/";
				}
			}
			
			// reading parameters
			Properties prop = new Properties();
			InputStream input = new FileInputStream(propertiesFile);
			prop.load(input);
			String annotationTag = prop.getProperty("namedEntityTag");
			String cl = prop.getProperty("NERclassName");
			String[] baseUris = prop.getProperty("baseURIs").split(",");
			String[] provBaseURI = prop.getProperty("avoidPredicatesBaseURI").split(",");
			String measure = prop.getProperty("centralityMeasure");
			String useindex = prop.getProperty("useDicoIndex");
			String indexDir = prop.getProperty("indexDir");
			String preferedURI = prop.getProperty("preferedURIOrder");
			String nameMainFolderDico = prop.getProperty("nameMainFolderDico");
			String rdfData = prop.getProperty("rdfData");
			String xpathExpresion= prop.getProperty("xpathExpresion");
			String propertyTagRef = prop.getProperty("propertyTagRef");
			
			// checking if we need to create an index
			if ((args.length >= 2 && args[1].equals("-createIndex"))
					|| (args.length >= 3 && args[2].equals("-createIndex"))) {
				System.out.println("(Re-)create index");
				FileUtils.deleteDirectory(new File(indexDir));
				DicoProcessingNEL.createIndex(indexDir, nameMainFolderDico);
			}
			int countMention = 0;
			int countParagraph = 0;

			DocumentBuilder b = DocumentBuilderFactory.newInstance()
					.newDocumentBuilder();
			File inputFiles = new File(args[0]);

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

			for (int j = 0; j < files.size(); j++) {
				
				//cleaning output files
				new File(outDir+files.get(j).getName().replace(".xml",
						"-resFinalGraphsV3.txt")).delete();
				
				FileWriterWithEncoding writerGraph = new FileWriterWithEncoding(outDir+files.get(j).getName().replace(".xml",
						"-resFinalGraphsV3.txt"), "UTF-8");
			
				org.w3c.dom.Document doc = b.parse(new FileInputStream(files
						.get(j)));
				XPath xPath = XPathFactory.newInstance().newXPath();
				NodeList nodes = (NodeList) xPath.evaluate(
						xpathExpresion,
						doc.getDocumentElement(), XPathConstants.NODESET);
				for (int i = 0; i < nodes.getLength(); ++i) {
					List<String> annotationsParagraph = new ArrayList<String>();
					Element e = (Element) nodes.item(i);
					NodeList nodesChild = (NodeList) xPath.evaluate(".//"
							+ annotationTag, e, XPathConstants.NODESET);
					for (int k = 0; k < nodesChild.getLength(); ++k) {
						Element child = (Element) nodesChild.item(k);
						annotationsParagraph.add(child.getTextContent());
						countMention++;
					}
					System.out.println("processing paragraph #"
							+ countParagraph);
					// look for URIs in dictionary
					Map<String, List<List<String>>> mentionsWithURIs = null;
					if (useindex.equalsIgnoreCase("true")) { // version with
																// index
						mentionsWithURIs = DicoProcessingNEL.retrieveMentionsURIsFromDicoWithIndex(
								cl, annotationsParagraph, indexDir);
					} else {
						mentionsWithURIs = DicoProcessingNEL.retrieveMentionsURIsFromDico(
								nameMainFolderDico, cl, annotationsParagraph);
					}
					String caseR = checkConditionsToNEL(mentionsWithURIs,
							annotationsParagraph);
					// Regular case
					if (caseR.equalsIgnoreCase("OK")) {
						
						//printing number of ambiguous URIs per mention
						Iterator<String> mentions = mentionsWithURIs.keySet().iterator();
						while (mentions.hasNext()) {
							String mention = mentions.next();
							Integer sizeReferents = mentionsWithURIs.get(mention).size();
							if (sizeReferents > 1) {
								System.out.println("Number of ambiguous referents (URIs) for "+mention+ " is "+sizeReferents);
								//TODO output to a file
							}
						}
						
						// create inverted index
						Map<String, String> invertedIndex = DicoProcessingNEL.buildInvertedIndex(mentionsWithURIs);
						// create RDF sub-graph from URIs of mentions in the
						// current paragraph
						
						Model model = RDFProcessingNEL.aggregateRDFSubGraphsFromURIs(rdfData,
								mentionsWithURIs, annotationsParagraph,
								baseUris);
						if (model != null) {
							
							// Fuse RDF graphs into a single graph (JGraphT format)
							SimpleDirectedWeightedGraph<String, LabeledEdge> graph = GraphProcessingNEL.fuseRDFGraphsIntoJGTGraph(
									model, provBaseURI, mentionsWithURIs, relsFile);
							// Simplify graph, compute centrality, choose the higher score
							Map<String, String> choosenUris = GraphProcessingNEL.simplifyGraphsAndCalculateCentrality(
									graph, mentionsWithURIs, annotationsParagraph,
									baseUris, invertedIndex, measure, preferedURI, files
									.get(j).getName(), countParagraph, writerGraph, edgeFrequenceByLabel);
							// write results in TEI
							if (choosenUris != null) {
								ResultsAndEvaluationNEL.produceResults(files.get(j), annotationTag,
										choosenUris, mentionsWithURIs, e, doc, outDir, propertyTagRef);									
							}
							System.out.println("finish");
						}
					} else if (caseR.equalsIgnoreCase("NoMentionsAnnotated")) {
						System.out
								.println("Current paragraph does not need to be processed: there is no mentions in document");
					} else {
						System.out.println("There are no ambiguous mentions");
						ResultsAndEvaluationNEL.produceResultsSimple(files.get(j), annotationTag,
									mentionsWithURIs, e, doc, outDir, propertyTagRef);
						// case: "NoAmbiguity", when there are no
						// ambiguities, assign them directly to the TEI output
						// file
					}
					countParagraph++;
				}
				System.out
						.println("number of all mentions (including duplicates): "
								+ countMention + " in file " + files.get(j));
				//print relation frequency per label
				ResultsAndEvaluationNEL.printRelationFrequency(edgeFrequenceByLabel, files.get(j).getName(), outDir);
				writerGraph.close();
			}
			Date endMain = new Date();
			System.out.println("Global Time: "
					+ (endMain.getTime() - startMain.getTime()) / 60 + "secs");

			// evaluation
			if ((args.length >= 2 && args[1].equals("-printEval"))
					|| (args.length >= 3 && args[2].equals("-printEval"))) {
				String output = args[0].replace(".xml", "-outV3.xml");
				String[] output2 = output.split("/");
				if (new File(outDir+output2[output2.length-1]).exists()) {
					System.out.println("Printing evaluation");
					ResultsAndEvaluationNEL.evaluation(args[0], annotationTag, xpathExpresion, outDir, propertyTagRef);
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
