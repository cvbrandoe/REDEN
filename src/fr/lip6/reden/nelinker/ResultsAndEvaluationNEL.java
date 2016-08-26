package fr.lip6.reden.nelinker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This class implements the methods for writing TEI output and 
 * to evaluate results in the presence of the corresponding gold file
 * 
 * @author @author Brando & Frontini
 */
public class ResultsAndEvaluationNEL {

	private static Logger logger = Logger.getLogger(ResultsAndEvaluationNEL.class);
	
	/**
	 * Writes selected URIs into the a new TEI file equivalent to the input
	 * file.
	 * 
	 * @param fileName
	 *            , the TEI input file
	 * @param annotationTag
	 *            , the named-entity tag (ex : persName)
	 * @param choosenUris
	 *            , the chosen URIs for every mention
	 * @param mentionsWithURIs
	 *            , the mentions and their URIs for every possible candidate
	 * @param e
	 *            , the current XML node (per paragraph
	 *            <p>
	 *            )
	 * @param doc
	 *            , the pointer to the DOM document
	 */
	public static void produceResults(File fileName, String annotationTag,
			Map<String, String> choosenUris,
			Map<String, List<List<String>>> mentionsWithURIs, Element e,
			org.w3c.dom.Document doc, String outDir, String propertyTagRef, 
			Map<String, Double> choosenScoresperMention, String addScores) {

		Date start = new Date();
		//Integer correctlyIdentified = 0;
		Integer nb = 0;
		List<List<String>> out = new ArrayList<List<String>>();
		try {

			XPath xPath = XPathFactory.newInstance().newXPath();
			List<String> annotationsParagraph = new ArrayList<String>();
			NodeList nodesChild = (NodeList) xPath.evaluate(".//"
					+ annotationTag, e, XPathConstants.NODESET);
			for (int k = 0; k < nodesChild.getLength(); ++k) {
				Element child = (Element) nodesChild.item(k);
				annotationsParagraph.add(child.getTextContent());
				if (addScores.equals("true"))
					child.setAttribute(propertyTagRef,
						choosenUris.get(child.getTextContent())+"("+ choosenScoresperMention.get(child.getTextContent())+")");
				else 
					child.setAttribute(propertyTagRef,
							choosenUris.get(child.getTextContent()));
			}
			if (annotationsParagraph.size() > 0) {
				out.add(annotationsParagraph);
			}
			logger.info("NB: " + nb);
			//System.out.println("Identified: " + correctlyIdentified);
			Transformer transformer;
			transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");

			// initialize StreamResult with File object to save to file
			StreamResult result = new StreamResult(new StringWriter());
			DOMSource source = new DOMSource(doc);
			transformer.transform(source, result);

			String xmlString = result.getWriter().toString();
			PrintWriter writer = new PrintWriter(outDir+fileName.getName().replace(
					".xml", "-outV3.xml"), "UTF-8");
			writer.println(xmlString);
			writer.close();
			Date end = new Date();
			logger.info("Finished produceResults in "
					+ (end.getTime() - start.getTime()) / 60 + "secs");
		} catch (XPathExpressionException e1) {
			e1.printStackTrace();
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (TransformerConfigurationException e2) {
			e2.printStackTrace();
		} catch (TransformerFactoryConfigurationError e2) {
			e2.printStackTrace();
		} catch (TransformerException e2) {
			e2.printStackTrace();
		}
	}
	
	/**
	 * Write results in the most simple cases, when there is no ambiguity.
	 * @param fileName, input file
	 * @param annotationTag, name of the NE tag
	 * @param mentionsWithURIs, list of uris per mention
	 * @param e, current xml element
	 * @param doc, xml document
	 */
	public static void produceResultsSimple(File fileName, String annotationTag,
			Map<String, List<List<String>>> mentionsWithURIs, Element e,
			org.w3c.dom.Document doc, String outDir, String propertyTagRef) {

		List<List<String>> out = new ArrayList<List<String>>();
		try {

			XPath xPath = XPathFactory.newInstance().newXPath();
			List<String> annotationsParagraph = new ArrayList<String>();
			NodeList nodesChild = (NodeList) xPath.evaluate(".//"
					+ annotationTag, e, XPathConstants.NODESET);
			for (int k = 0; k < nodesChild.getLength(); ++k) {
				Element child = (Element) nodesChild.item(k);
				annotationsParagraph.add(child.getTextContent());
				String finUri = "";
				if (mentionsWithURIs.get(child.getTextContent()) != null) {
					for (String uri : mentionsWithURIs.get(child.getTextContent()).get(0)) {
						finUri += uri + " ";
					}
					child.setAttribute(propertyTagRef, finUri.trim());
				}
			}
			if (annotationsParagraph.size() > 0) {
				out.add(annotationsParagraph);
			}
			Transformer transformer;
			transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");

			// initialize StreamResult with File object to save to file
			StreamResult result = new StreamResult(new StringWriter());
			DOMSource source = new DOMSource(doc);
			transformer.transform(source, result);

			String xmlString = result.getWriter().toString();
			PrintWriter writer = new PrintWriter(outDir+fileName.getName().replace(
					".xml", "-outV3.xml"), "UTF-8");
			writer.println(xmlString);
			writer.close();
		} catch (XPathExpressionException e1) {
			e1.printStackTrace();
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (TransformerConfigurationException e2) {
			e2.printStackTrace();
		} catch (TransformerFactoryConfigurationError e2) {
			e2.printStackTrace();
		} catch (TransformerException e2) {
			e2.printStackTrace();
		}
	}
	
	/**
	 * It performs the evaluation of the result given by the NEL algorithm with
	 * respect to a gold file.
	 * 
	 * @param namefile
	 *            , name of the input file
	 * @param annotationTag
	 *            , name of the annotation tag (e.g. : <persName>)
	 */
	public static List<EvalInfo> compareResultsWithGold(String namefile, String annotationTag, String xpathExpresion, String outDir, String propertyTagRef, 
			List<Map<String, List<List<String>>>> allMentionsWithUrisPerContextinText) {
		
		List<EvalInfo> collectedResults = new ArrayList<EvalInfo>();
		
		HashMap<String, Integer> countOccurenceCorrectMentions = new HashMap<String, Integer>();
		float manualkeys = 0, correctkey = 0, emptyChoice = 0, emptyManualAnnot = 0;
		try {
			String namefileA[] = namefile.split("/");
			
			PrintWriter writer = new PrintWriter(outDir+namefileA[namefileA.length-1].replace(".xml",
					"-resEvalV3.txt"), "UTF-8");
			
			PrintWriter writerCE = new PrintWriter(outDir+namefileA[namefileA.length-1].replace(".xml",
					"-resCorrectMentionsV3.txt"), "UTF-8");
						
			// output file
			DocumentBuilder b = DocumentBuilderFactory.newInstance()
					.newDocumentBuilder();
			org.w3c.dom.Document doc = b.parse(new FileInputStream(outDir+namefileA[namefileA.length-1]
					.replace(".xml", "-outV3.xml")));

			XPath xPath = XPathFactory.newInstance().newXPath();
			NodeList nodes = (NodeList) xPath.evaluate(
					xpathExpresion,
					doc.getDocumentElement(), XPathConstants.NODESET);

			// gold file
			String goldName = namefile.replace(".xml", "-gold.xml");
			DocumentBuilder bgold = DocumentBuilderFactory.newInstance()
					.newDocumentBuilder();
			org.w3c.dom.Document docgold = bgold.parse(new FileInputStream(
					goldName));
			XPath xPathGold = XPathFactory.newInstance().newXPath();
			NodeList nodesGold = (NodeList) xPathGold.evaluate(
					xpathExpresion,
					docgold.getDocumentElement(), XPathConstants.NODESET);
			
			Integer countParagraph = 0;
			for (int i = 0; i < nodes.getLength(); ++i) {
				
				Element e = (Element) nodes.item(i);
				String otherMentions = "";
				writer.println("Text portion# "+countParagraph);
				for (String annoTag : annotationTag.split(",")) {

					NodeList nodesChild = (NodeList) xPath.evaluate(".//"
							+ annoTag.trim(), e, XPathConstants.NODESET);
					
					// gold file
					Element eGold = (Element) nodesGold.item(i);
					NodeList nodesChildGold = (NodeList) xPathGold.evaluate(".//"
							+ annoTag.trim(), eGold, XPathConstants.NODESET);
					
					for (int k = 0; k < nodesChild.getLength(); ++k) {
						Element child = (Element) nodesChild.item(k);
						Element childGold = (Element) nodesChildGold.item(k);
						otherMentions += child.getTextContent() + ",";
						
						String ref = childGold.getAttribute("ref"); //single manual URI
						String ref_autoList = child.getAttribute(propertyTagRef); //chosen candidate URIs
						String mention = child.getTextContent().trim();
						
						EvalInfo evalInfo = new EvalInfo();	
						evalInfo.setMention(mention);
						evalInfo.setCandUris(allMentionsWithUrisPerContextinText.get(i).get(mention));
						
						writer.println("");
						writer.println("Mention: "
								+ child.getTextContent());
						
						writer.println("Manual was: "+ref);
						writer.println("REDEN chose: "+ref_autoList);
						
						if (ref != null && !ref.equals("")) { // gold has manual ref						
							evalInfo.setManualURI(ref);
							manualkeys++;
							//the good URI is in the candidate set (not necessarily the chosen one)
							if (evalInfo.getCandUris() != null) {
								for (List<String> cand : evalInfo.getCandUris()) {
									for (String uri : cand) {
										if (uri.toLowerCase().contains(ref.toLowerCase())) {
											evalInfo.setCorrectURIisInCandSet(true);
										}
									}								
								}
							}
								
							if (ref_autoList != null && !ref_autoList.equals("")) { // nel chose something
								evalInfo.setChosenUri(ref_autoList);								
								if (ref_autoList.contains(ref)) {
									writer.println("Good choice");
									correctkey++;
									evalInfo.setChoiceIsCorrect(true); //the good URI was chosen
									if (countOccurenceCorrectMentions.get(mention) == null) {
										countOccurenceCorrectMentions.put(mention, 1);
									} else {
										Integer count = countOccurenceCorrectMentions.get(mention);
										count++;
										countOccurenceCorrectMentions.put(mention, count);
									}
								} else {
									writer.println("Wrong choice");
								}
							} else {
								//writer.println("REDEN chose nothing");
								emptyChoice++;
								evalInfo.setChosenUri(null);
							}						
						} else {
							emptyManualAnnot++;
							evalInfo.setManualURI(null);
							writer.println("No manual annotation");
						}	
						collectedResults.add(evalInfo);
						if (evalInfo.getCorrectURIisInCandSet() && !evalInfo.getChoiceIsCorrect()) {
							writer.println("Right referent in candidate set but REDEN choose the wrong one");
						}
						//print candidate set
						writer.println("The candidate set is: ");						
						if (evalInfo.getCandUris() != null) {
							for (List<String> cand : evalInfo.getCandUris()) {
								writer.println(cand.toString());
							}
						}
					}
				}
				countParagraph++;				
				writer.println("Context was: "+otherMentions);
				writer.println("______________");
			}
			
			//print occurrences of correctly identified mentions
			Set<String> keys = countOccurenceCorrectMentions.keySet();
			for (String k : keys) {
				writerCE.println("mention: "+k + " count: "+countOccurenceCorrectMentions.get(k));
			}
			writer.println();
			/* Old evaluation procedure
			 * writer.println("Manually annotated keys:" + manualkeys);
			writer.println("Correctly annotated keys:" + correctkey);
			writer.println("NEL didn't make choice:" + emptyChoice);
			writer.println("Manual annotation was missing:" + emptyManualAnnot);
			writer.println("Evaluation: " + correctkey / manualkeys);*/
			writer.close();
			writerCE.close();
			logger.info("Check results in "
					+ outDir+namefileA[namefileA.length-1].replace(".xml", "-resEvalV3.txt"));			
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
		
		return collectedResults;

	}
	
	public static List<EvalInfo> compareResultsWithGold(String namefile, String annotationTag, String xpathExpresion, String outDir, String propertyTagRef, 
			Map<Integer, List<String>> allMentionsWithUrisPerContextinText) {
		
		List<EvalInfo> collectedResults = new ArrayList<EvalInfo>();
		
		HashMap<String, Integer> countOccurenceCorrectMentions = new HashMap<String, Integer>();
		float manualkeys = 0, correctkey = 0, emptyChoice = 0, emptyManualAnnot = 0;
		try {
			String namefileA[] = namefile.split("/");
			
			PrintWriter writer = new PrintWriter(outDir+namefileA[namefileA.length-1].replace(".xml",
					"-resEvalV3.txt"), "UTF-8");
			
			PrintWriter writerCE = new PrintWriter(outDir+namefileA[namefileA.length-1].replace(".xml",
					"-resCorrectMentionsV3.txt"), "UTF-8");
						
			// output file
			DocumentBuilder b = DocumentBuilderFactory.newInstance()
					.newDocumentBuilder();
			org.w3c.dom.Document doc = b.parse(new FileInputStream(outDir+namefileA[namefileA.length-1]
					.replace(".xml", "-outV3.xml")));

			XPath xPath = XPathFactory.newInstance().newXPath();
			NodeList nodes = (NodeList) xPath.evaluate(
					xpathExpresion,
					doc.getDocumentElement(), XPathConstants.NODESET);

			// gold file
			String goldName = namefile.replace(".xml", "-gold.xml");
			DocumentBuilder bgold = DocumentBuilderFactory.newInstance()
					.newDocumentBuilder();
			org.w3c.dom.Document docgold = bgold.parse(new FileInputStream(
					goldName));
			XPath xPathGold = XPathFactory.newInstance().newXPath();
			NodeList nodesGold = (NodeList) xPathGold.evaluate(
					xpathExpresion,
					docgold.getDocumentElement(), XPathConstants.NODESET);
			
			Integer countParagraph = 0;
			for (int i = 0; i < nodes.getLength(); ++i) {
				
				Element e = (Element) nodes.item(i);
				String otherMentions = "";
				writer.println("Text portion# "+countParagraph);
				for (String annoTag : annotationTag.split(",")) {

					NodeList nodesChild = (NodeList) xPath.evaluate(".//"
							+ annoTag.trim(), e, XPathConstants.NODESET);
					
					// gold file
					Element eGold = (Element) nodesGold.item(i);
					NodeList nodesChildGold = (NodeList) xPathGold.evaluate(".//"
							+ annoTag.trim(), eGold, XPathConstants.NODESET);
					
					for (int k = 0; k < nodesChild.getLength(); ++k) {
						Element child = (Element) nodesChild.item(k);
						Element childGold = (Element) nodesChildGold.item(k);
						otherMentions += child.getTextContent() + ",";
						
						String ref = childGold.getAttribute("ref"); //single manual URI
						String ref_autoList = child.getAttribute(propertyTagRef); //chosen candidate URIs
						String mention = "";
						int id = -1;
						NodeList childs = child.getChildNodes();
						for (int j = 0; j < childs.getLength(); j++) {
							Node n = childs.item(j);
							String nodeName = n.getNodeName();
							if ("name".equals(nodeName) && n.hasAttributes()) {
								NamedNodeMap attributes = n.getAttributes();
								for (int l = 0; l < attributes.getLength(); l++) {
									if ("xml:id".equals(attributes.item(l).getNodeName()))
									id = Integer.parseInt(attributes.item(l).getNodeValue());
								}
								NodeList childs2 = n.getChildNodes();
								for (int m = 0; m < childs2.getLength(); m++) {
									Node n2 = childs2.item(m);
									String nodeName2 = n2.getNodeName();
									if ("w".equals(nodeName2) && n2.getTextContent() != null) {
										mention += n2.getTextContent().trim() + " ";
									}
								}
								break;
							}
						}
						mention = mention.trim();
						EvalInfo evalInfo = new EvalInfo();	
						evalInfo.setMention(mention);
						List<String> candidates = allMentionsWithUrisPerContextinText.get(id);
						if (candidates != null && !candidates.isEmpty()) {
							List<List<String>> list = new ArrayList<>();
							list.add(candidates);
							evalInfo.setCandUris(list);
						}
						
						writer.println("");
						writer.println("Mention: "
								+ child.getTextContent());
						
						writer.println("Manual was: "+ref);
						writer.println("REDEN chose: "+ref_autoList);
						
						if (ref != null && !ref.equals("")) { // gold has manual ref						
							evalInfo.setManualURI(ref);
							manualkeys++;
							//the good URI is in the candidate set (not necessarily the chosen one)
							if (evalInfo.getCandUris() != null) {
								for (List<String> cand : evalInfo.getCandUris()) {
									for (String uri : cand) {
										if (uri.toLowerCase().contains(ref.toLowerCase())) {
											evalInfo.setCorrectURIisInCandSet(true);
										}
									}								
								}
							}
								
							if (ref_autoList != null && !ref_autoList.equals("")) { // nel chose something
								evalInfo.setChosenUri(ref_autoList);								
								if (ref_autoList.contains(ref)) {
									writer.println("Good choice");
									correctkey++;
									evalInfo.setChoiceIsCorrect(true); //the good URI was chosen
									if (countOccurenceCorrectMentions.get(mention) == null) {
										countOccurenceCorrectMentions.put(mention, 1);
									} else {
										Integer count = countOccurenceCorrectMentions.get(mention);
										count++;
										countOccurenceCorrectMentions.put(mention, count);
									}
								} else {
									writer.println("Wrong choice");
								}
							} else {
								//writer.println("REDEN chose nothing");
								emptyChoice++;
								evalInfo.setChosenUri(null);
							}						
						} else {
							emptyManualAnnot++;
							evalInfo.setManualURI(null);
							writer.println("No manual annotation");
						}	
						collectedResults.add(evalInfo);
						if (evalInfo.getCorrectURIisInCandSet() && !evalInfo.getChoiceIsCorrect()) {
							writer.println("Right referent in candidate set but REDEN choose the wrong one");
						}
						//print candidate set
						writer.println("The candidate set is: ");						
						if (evalInfo.getCandUris() != null) {
							for (List<String> cand : evalInfo.getCandUris()) {
								writer.println(cand.toString());
							}
						}
					}
				}
				countParagraph++;				
				writer.println("Context was: "+otherMentions);
				writer.println("______________");
			}
			
			//print occurrences of correctly identified mentions
			Set<String> keys = countOccurenceCorrectMentions.keySet();
			for (String k : keys) {
				writerCE.println("mention: "+k + " count: "+countOccurenceCorrectMentions.get(k));
			}
			writer.println();
			/* Old evaluation procedure
			 * writer.println("Manually annotated keys:" + manualkeys);
			writer.println("Correctly annotated keys:" + correctkey);
			writer.println("NEL didn't make choice:" + emptyChoice);
			writer.println("Manual annotation was missing:" + emptyManualAnnot);
			writer.println("Evaluation: " + correctkey / manualkeys);*/
			writer.close();
			writerCE.close();
			logger.info("Check results in "
					+ outDir+namefileA[namefileA.length-1].replace(".xml", "-resEvalV3.txt"));			
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
		
		return collectedResults;

	}
	
	
	/**
	 * Print the number of times a predicate (edge) appears in the graph.
	 * @param edgeFrequenceByLabel, container of the information
	 * @param fileName, name of the source XML-TEI file
	 * @param outDir, where to output the result
	 */
	public static void printRelationFrequency(Map<String, Double> edgeFrequenceByLabel, String fileName, String outDir) {
		try {
			PrintWriter writer = new PrintWriter(outDir+fileName.replace(".xml", "-relFrequency.txt"), "UTF-8");
			Map<String, Double> orderedMap = Util.sortByValue(edgeFrequenceByLabel);
			Set<String> keys = orderedMap.keySet();
			for (String key : keys) {
				writer.println("Rel: "+key+ " frequency: "+edgeFrequenceByLabel.get(key));
			}
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		
	}

	/**
	 * Implementation of NEL evaluation metrics according to Hachey et al (2011).
	 * @param collectedResults
	 */
	public static void computeFinalResults(List<EvalInfo> collectedResults) {
		
		//For lisibility purposes, we separate the computation of each measure 
		/**
		 * candidate cardinality mean: mean cardinality of the candidate sets. Fewer candidates mean reduced disambiguation workload
		 * (FR) la cardinalité des candidate sets divisé par le nombre de mentions dans le texte d’entrée 		 
		 */
		Double candSizes = 0.0;
		for (EvalInfo eval : collectedResults) {
			if (eval.getCandUris() != null)
				candSizes = candSizes + (double) eval.getCandUris().size();
		}
		System.out.println("candidate cardinality mean: "+ candSizes/collectedResults.size());
		System.out.println("");
		
		/**
		 * candidate precision: percentage of non-empty candidate sets containing the correct entity,
		 * (FR) nombre de candidate sets non-vides contenant l’URI correct par rapport au gold
		 * divisé par le nombre de candidate sets non-vides
		 */
		Double candidatePrecision = 0.0, nonEmptyCandSetsCorrectURIisThere = 0.0, nonEmptyCandSets = 0.0;
		for (EvalInfo eval : collectedResults) {
			if (eval.getCandUris() != null && !eval.getCandUris().isEmpty()) {
				nonEmptyCandSets++;
				if (eval.getCorrectURIisInCandSet()) { 
					nonEmptyCandSetsCorrectURIisThere++;
				}
			}
		}
		candidatePrecision = nonEmptyCandSetsCorrectURIisThere/nonEmptyCandSets;
		System.out.println("nonEmptyCandSetsCorrectURIisThere: "+nonEmptyCandSetsCorrectURIisThere);
		System.out.println("nonEmptyCandSets: "+nonEmptyCandSets);
		System.out.println("candidatePrecision = nonEmptyCandSetsCorrectURI/nonEmptyCandSets: "+candidatePrecision);
		System.out.println("");
		
		/**
		 * candidate recall: percentage of non-NIL queries where the candidate set includes the correct candidate
		 * (FR) le nombre de candidate sets contenant l’URI pour les mentions qui ont été annotées (!= NIL) divisé 
		 * par le nombre de candidate sets des mentions qui ont été annotées
		 */
		
		Double candidateRecall = 0.0, candSetsWitManualAnnot = 0.0, candSetWithManualAnnotAndURIisThere = 0.0;
		for (EvalInfo eval : collectedResults) {
			if (eval.getManualURI() != null) {
				candSetsWitManualAnnot++;
				if (eval.getCorrectURIisInCandSet()) {
					candSetWithManualAnnotAndURIisThere++;
				}
			}			
		}
		candidateRecall = candSetWithManualAnnotAndURIisThere/candSetsWitManualAnnot;
		System.out.println("candSetWithManualAnnotAndURIisThere: "+candSetWithManualAnnotAndURIisThere);
		System.out.println("candSetsWitManualAnnot: "+candSetsWitManualAnnot);
		System.out.println("candidateRecall = candSetWithManualAnnotAndURIisThere/candSetsWitManualAnnot: "+candidateRecall);
		System.out.println("");
		
		/**
		 * NIL precision: percentage of empty candidate sets that are correct (i.e. correspond to NIL queries)
		 * (FR) le nombre de candidate sets qui sont vides et la mention correspondante a été annoté comme NIL
		 * divisé par le nombre de candidate sets vides. Interpretation: si dans le gold quelqu’un dit que 
		 * “M. Barre” n’a pas d’URI (il n’existe pas dans la KB) alors on devrait aussi avoir des candidate sets vides		
		 */	
		Double nilPrecision = 0.0, emptyCandSets = 0.0, emptyCandSetsWithNILManualAnnot = 0.0;
		for (EvalInfo eval : collectedResults) {
			if (eval.getCandUris() == null || eval.getCandUris().isEmpty()) {
				emptyCandSets++;
				if (eval.getManualURI() == null) {
					emptyCandSetsWithNILManualAnnot++;
				}				
			}
		}
		nilPrecision = emptyCandSetsWithNILManualAnnot/emptyCandSets;
		System.out.println("emptyCandSetsWithNILManualAnnot: "+emptyCandSetsWithNILManualAnnot);
		System.out.println("emptyCandSets: "+emptyCandSets);
		System.out.println("nilPrecision = emptyCandSetsWithNILManualAnnot/emptyCandSets: "+nilPrecision);
		System.out.println("");
		
		/**
		 * NIL recall: percentage of NIL queries for which the candidate set is empty. 
		 * A high rate is valuable because it is difficult to disambiguators to determine whether 
		 * queries are NIL-linked when candidates are returned.
		 * (FR) le nombre de candidate sets qui sont vides et la mention correspondante a été annoté comme NIL divisé 
		 * par le nombre de mentions dans le gold qui ont une annotation NIL
		 */
		Double nilRecall = 0.0, mentionsWithNILManualAnnot = 0.0, emptyCandSetWithNILManualRef = 0.0; 
		for (EvalInfo eval : collectedResults) {
			if (eval.getManualURI() == null) {
				mentionsWithNILManualAnnot++;
				if (eval.getCandUris() == null || eval.getCandUris().isEmpty()) {
					emptyCandSetWithNILManualRef++;
				}
			}
		}
		nilRecall = emptyCandSetWithNILManualRef/mentionsWithNILManualAnnot;
		System.out.println("emptyCandSetWithNILManualRef: "+emptyCandSetWithNILManualRef);
		System.out.println("mentionsWithNILManualAnnot: "+mentionsWithNILManualAnnot);
		System.out.println("nilRecall = emptyCandSetWithNILManualRef/mentionsWithNILManualAnnot: "+nilRecall);
		System.out.println("");
		
		/**
		 * Disambiguation accuracy (mention level): percentage of correctly linked mentions 
		 * for non empty candidate sets with at least 2 candidates 
		 */
		Double disambiguationAccuracy = 0.0, correctMentionsNonEmptyAtleast2CandSet = 0.0, nonEmptyAtleast2CandSetwithCorrectUri = 0.0;
		for (EvalInfo eval : collectedResults) {
			if (eval.getCandUris() != null && !eval.getCandUris().isEmpty() 
					&& eval.getCandUris().size() > 1 && eval.getCorrectURIisInCandSet()) {
				nonEmptyAtleast2CandSetwithCorrectUri++;
				if (eval.getChoiceIsCorrect()) {
					correctMentionsNonEmptyAtleast2CandSet++;
				}
			}
		}
		disambiguationAccuracy = correctMentionsNonEmptyAtleast2CandSet/nonEmptyAtleast2CandSetwithCorrectUri;
		System.out.println("correctMentionsNonEmptyAtleast2CandSet: "+correctMentionsNonEmptyAtleast2CandSet);
		System.out.println("nonEmptyAtleast2CandSetwithCorrectUri: "+nonEmptyAtleast2CandSetwithCorrectUri);
		System.out.println("disambiguationAccuracy = correctMentionsNonEmptyAtleast2CandSet/nonEmptyAtleast2CandSetwithCorrectUri: "+disambiguationAccuracy);
		System.out.println("");
		
		/**
		 * Overall linking accuracy
		 */		
		Double overallLinkingAccuracy = 0.0, correctlyLinkedMentions1 = 0.0, correctlyLinkedMentions2 = 0.0, other = 0.0;
		for (EvalInfo eval : collectedResults) {
			if (eval.getManualURI() != null && eval.getChoiceIsCorrect()) {
				correctlyLinkedMentions1++;				
			} //even though the chosen URI and the manual URI are null, if there is a candidate, REDEN attributes an URI (the cand.) to the mention, and it should have been a NIL annotation
			else if ( (eval.getCandUris() == null || eval.getCandUris().isEmpty()) && eval.getManualURI() == null && eval.getChosenUri() == null) {
				correctlyLinkedMentions2++;
			} else {
				other++;
				/* TODO for correcting gold TEI
				 * if (eval.getCandUris() != null)
					System.out.println("HERE: ment:"+ eval.getMention()+" man:"+eval.getManualURI()+ " auto:"+eval.getChosenUri()+ " cands:"+eval.getCandUris().size() + " goodIsThere:"+eval.getCorrectURIisInCandSet());
				else 
					System.out.println("HERE: ment:"+ eval.getMention()+" man:"+eval.getManualURI()+ " auto:"+eval.getChosenUri()+ " cands:"+0 + " goodIsThere:"+eval.getCorrectURIisInCandSet());
				*/
				
			}
		}
		overallLinkingAccuracy = (correctlyLinkedMentions1 + correctlyLinkedMentions2)/collectedResults.size();
		System.out.println("correctlyLinkedMentions: (good choice)-> "+correctlyLinkedMentions1 + " and (good NIL)-> "+correctlyLinkedMentions2);
		System.out.println("nbMentions: "+collectedResults.size());
		System.out.println("other: "+other);
		System.out.println("overallLinkingAccuracy = correctlyLinkedMentions/nbMentions: "+overallLinkingAccuracy);
						
	}
}
