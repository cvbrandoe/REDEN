package fr.lip6.reden.extra;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import fr.lip6.reden.nelinker.DicoProcessingNEL;

/**
 * Class for calculating population completeness in a given Linked data set
 * with respect to a literary text annotated by humans (gold standard)
 * QLD'16@EGC workshop and COLD@ISWC 2016
 * @author Brando & Abadie
 *
 */
public class CalculatePopulationCompletenessLDUsingDico {

	public static void main(String [] args) {
		//dbpedia();
		//bnf();
		//bne();
		//getty();
		dbpedia2();
	}
	
	public static void bnf() {
		
		try {
			Set<String> results = DicoProcessingNEL.searchIndexWithRegexp("dico/indexedDictionary/PER-24062016", "uris", "http.*");
				
			System.out.println("Total entities in domain coverage "+results.size());
			
			//Checking with gold standard
			Integer totalIn = 0;
			DocumentBuilder b = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			org.w3c.dom.Document doc = b.parse(new FileInputStream(new File ("input/cold-iswc2016/thibaudet/thibaudet_reflexions-gold-bnf.xml")));
			XPath xPath = XPathFactory.newInstance().newXPath();
			NodeList nodes = (NodeList) xPath.evaluate("//body", doc.getDocumentElement(), XPathConstants.NODESET);
			for (int i = 0; i < nodes.getLength(); ++i) {
				Element e = (Element) nodes.item(i);
				NodeList nodesChild = (NodeList) xPath.evaluate(".//persName", e, XPathConstants.NODESET);
				for (int k = 0; k < nodesChild.getLength(); ++k) {
					Element child = (Element) nodesChild.item(k);
					String uri_auto = child.getAttribute("ref").trim();
					if (!uri_auto.equals("") && results.contains(uri_auto)) {
						//System.out.println("both in gold and dico: "+ uri_auto);
						totalIn++;
					}
				}
			}
			System.out.println("Intersection BnF authors in domain scope AND annotated authors in gold standard "+totalIn);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (SAXException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (XPathExpressionException e1) {
			e1.printStackTrace();
		} catch (ParserConfigurationException e1) {
			e1.printStackTrace();
		}	
	}
	
	public static void bne() {
		
		try {
			Set<String> results = DicoProcessingNEL.searchIndexWithRegexp("dico/indexedDictionary/PER-20062016", "uris", "http.*");
				
			System.out.println("Total entities in domain coverage "+results.size());
			
			//Checking with gold standard
			Integer totalIn = 0;
			DocumentBuilder b = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			org.w3c.dom.Document doc = b.parse(new FileInputStream(new File ("input/cold-iswc2016/thibaudet/thibaudet_reflexions-gold-bne.xml")));
			XPath xPath = XPathFactory.newInstance().newXPath();
			NodeList nodes = (NodeList) xPath.evaluate("//body", doc.getDocumentElement(), XPathConstants.NODESET);
			for (int i = 0; i < nodes.getLength(); ++i) {
				Element e = (Element) nodes.item(i);
				NodeList nodesChild = (NodeList) xPath.evaluate(".//persName", e, XPathConstants.NODESET);
				for (int k = 0; k < nodesChild.getLength(); ++k) {
					Element child = (Element) nodesChild.item(k);
					String uri_auto = child.getAttribute("ref").trim();
					if (!uri_auto.equals("") && results.contains(uri_auto)) {
						//System.out.println("both in gold and dico: "+ uri_auto);
						totalIn++;
					}
				}
			}
			System.out.println("Intersection BNE authors in domain scope AND annotated authors in gold standard "+totalIn);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (SAXException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (XPathExpressionException e1) {
			e1.printStackTrace();
		} catch (ParserConfigurationException e1) {
			e1.printStackTrace();
		}	
				
	}
	
	public static void getty() {
		
		try {
			Set<String> results = DicoProcessingNEL.searchIndexWithRegexp("dico/indexedDictionary/PER-19062016", "uris", "http.*");
				
			System.out.println("Total entities in domain coverage "+results.size());
			
			//Checking with gold standard
			Integer totalIn = 0;
			DocumentBuilder b = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			org.w3c.dom.Document doc = b.parse(new FileInputStream(new File ("input/cold-iswc2016/apollinaire/apollinaire-médtations-esthétiques-gold-getty.xml")));
			XPath xPath = XPathFactory.newInstance().newXPath();
			NodeList nodes = (NodeList) xPath.evaluate("//body", doc.getDocumentElement(), XPathConstants.NODESET);
			for (int i = 0; i < nodes.getLength(); ++i) {
				Element e = (Element) nodes.item(i);
				NodeList nodesChild = (NodeList) xPath.evaluate(".//persName", e, XPathConstants.NODESET);
				for (int k = 0; k < nodesChild.getLength(); ++k) {
					Element child = (Element) nodesChild.item(k);
					String uri_auto = child.getAttribute("ref").trim();
					if (!uri_auto.equals("") && results.contains(uri_auto)) {
						//System.out.println("both in gold and dico: "+ uri_auto);
						totalIn++;
					}
				}
			}
			System.out.println("Intersection getty personalities in domain scope AND annotated authors in gold standard "+totalIn);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (SAXException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (XPathExpressionException e1) {
			e1.printStackTrace();
		} catch (ParserConfigurationException e1) {
			e1.printStackTrace();
		}	
	
	}

	public static void dbpedia2() {
		
		try {
			Set<String> results = DicoProcessingNEL.searchIndexWithRegexp("dico/indexedDictionary/PER-25062016", "uris", "http.*");
				
			System.out.println("Total entities in domain coverage "+results.size());
			
			//Checking with gold standard
			Integer totalIn = 0;
			DocumentBuilder b = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			//org.w3c.dom.Document doc = b.parse(new FileInputStream(new File ("input/cold-iswc2016/thibaudet/thibaudet_reflexions-gold-dbpediafr.xml")));
			org.w3c.dom.Document doc = b.parse(new FileInputStream(new File ("input/cold-iswc2016/apollinaire/apollinaire-médtations-esthétiques-gold-dbpediafr.xml")));
			XPath xPath = XPathFactory.newInstance().newXPath();
			NodeList nodes = (NodeList) xPath.evaluate("//body", doc.getDocumentElement(), XPathConstants.NODESET);
			for (int i = 0; i < nodes.getLength(); ++i) {
				Element e = (Element) nodes.item(i);
				NodeList nodesChild = (NodeList) xPath.evaluate(".//persName", e, XPathConstants.NODESET);
				for (int k = 0; k < nodesChild.getLength(); ++k) {
					Element child = (Element) nodesChild.item(k);
					String uri_auto = child.getAttribute("ref").trim();
					if (!uri_auto.equals("") && results.contains(uri_auto)) {
						//System.out.println("both in gold and dico: "+ uri_auto);
						totalIn++;
					}
				}
			}
			System.out.println("Intersection BNE authors in domain scope AND annotated authors in gold standard "+totalIn);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (SAXException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (XPathExpressionException e1) {
			e1.printStackTrace();
		} catch (ParserConfigurationException e1) {
			e1.printStackTrace();
		}
	}

}
