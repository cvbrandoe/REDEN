package fr.lip6.reden;

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

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This class implements the methods for writing TEI output and 
 * to evaluate results in the presence of the corresponding gold file
 * 
 * @author @author Brando & Frontini - Labex OBVIL - Université Paris-Sorbonne - UPMC
 *         LIP6
 */
public class ResultsAndEvaluationNEL {

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
			System.out.println("NB: " + nb);
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
			System.out.println("Finished produceResults in "
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
	public static void evaluation(String namefile, String annotationTag, String xpathExpresion, String outDir, String propertyTagRef) {
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
				NodeList nodesChild = (NodeList) xPath.evaluate(".//"
						+ annotationTag, e, XPathConstants.NODESET);
				
				// gold file
				Element eGold = (Element) nodesGold.item(i);
				NodeList nodesChildGold = (NodeList) xPathGold.evaluate(".//"
						+ annotationTag, eGold, XPathConstants.NODESET);
				String otherMentions = "";
				writer.println("Paragraph# "+countParagraph);
				for (int k = 0; k < nodesChild.getLength(); ++k) {
					Element child = (Element) nodesChild.item(k);
					Element childGold = (Element) nodesChildGold.item(k);
					otherMentions += child.getTextContent() + ",";
					
					String ref = childGold.getAttribute("ref");
					String ref_autoList = child.getAttribute(propertyTagRef);
					String mention = child.getTextContent();
					if (ref != null && !ref.equals("")) { // gold has manual ref
						manualkeys++;
						if (ref_autoList != null && !ref_autoList.equals("")) { // nel chose something
							if (ref_autoList.contains(ref)) {
								// writer.println("Mention: "+child.getTextContent());
								// writer.println("Ok");
								correctkey++;
								if (countOccurenceCorrectMentions.get(mention) == null) {
									countOccurenceCorrectMentions.put(mention, 1);
								} else {
									Integer count = countOccurenceCorrectMentions.get(mention);
									count++;
									countOccurenceCorrectMentions.put(mention, count);
								}
							} else {
								writer.println("Mention: "
										+ child.getTextContent());
								writer.println("Manual was: ");
								writer.println(ref);
								writer.println("Algorithm choice was: ");
								writer.println(ref_autoList);																
							}
						} else {
							writer.println("Mention: " + child.getTextContent());
							writer.println("Manual was: ");
							writer.println(ref);
							writer.println("Algorithm choice was EMPTY");
							emptyChoice++;
						}						
					} else {
						emptyManualAnnot++;
						// writer.println("Mention "+child.getTextContent()+" has no manual annotation");
					}					
				}
				countParagraph++;
				writer.println("");
				writer.println("Context was: "+otherMentions);
				writer.println("______________");
			}
			
			//print occurrences of correctly identified mentions
			Set<String> keys = countOccurenceCorrectMentions.keySet();
			for (String k : keys) {
				writerCE.println("mention: "+k + " count: "+countOccurenceCorrectMentions.get(k));
			}
			writer.println();
			writer.println("Manually annotated keys:" + manualkeys);
			writer.println("Correctly annotated keys:" + correctkey);
			writer.println("NEL didn't make choice:" + emptyChoice);
			writer.println("Manual annotation was missing:" + emptyManualAnnot);
			writer.println("Evaluation: " + correctkey / manualkeys);
			writer.close();
			writerCE.close();
			System.out.println("Check evaluation results in "
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
			Map<String, Double> orderedMap = GraphProcessingNEL.sortByValue(edgeFrequenceByLabel);
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
}
