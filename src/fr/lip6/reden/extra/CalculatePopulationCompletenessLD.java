package fr.lip6.reden.extra;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
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

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
//import org.apache.jena.rdf.model.Statement;

/**
 * Class for calculating population completeness in a given Linked data set with
 * respect to a literary text annotated by humans (gold standard) QLD'16@EGC
 * workshop and COLD@ISWC 2016
 * 
 * @author Brando & Abadie
 *
 */
public class CalculatePopulationCompletenessLD {

	public static void main(String[] args) {
		// bnfFiltered();
		// dbpedia();
		// bnf();
		// bne();
		// getty();
		dbpedia2();
	}

	public static void bnfFiltered() {

		String domainScopeAuthorsQuery = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
				+ "PREFIX foaf: <http://xmlns.com/foaf/0.1/> " + "PREFIX owl: <http://www.w3.org/2002/07/owl#> "
				+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "
				+ "PREFIX bnf-onto: <http://data.bnf.fr/ontology/bnf-onto/> "
				+ "PREFIX rdagroup2elements: <http://rdvocab.info/ElementsGr2/> "
				+ "SELECT distinct ?auteur ?ref  WHERE { " + "?auteur rdf:type foaf:Person . "
				+ "?auteur foaf:familyName ?nom . " + "?auteur rdagroup2elements:languageOfThePerson ?langue . "
				+ "?auteur bnf-onto:firstYear ?birthdate . " + "FILTER (?birthdate < 1900). "
				+ "FILTER regex (str(?langue), 'http://id.loc.gov/vocabulary/iso639-2/fre'). "
				+ "OPTIONAL { ?auteur owl:sameAs ?ref . "
				+ "FILTER regex(STR(?ref), '^http://www.idref.fr|^http://dbpedia.org/resource', 'i') }" + "} ";

		try {
			QueryExecution qexec = QueryExecutionFactory.sparqlService("http://data.bnf.fr/sparql",
					domainScopeAuthorsQuery);
			ResultSet results = qexec.execSelect();
			results = ResultSetFactory.copyResults(results);
			Integer count = 0;
			Set<String> allAuthorsUris = new HashSet<String>();
			// System.out.println(results.next().get("?total"));
			while (results.hasNext()) {
				count++;
				QuerySolution q = results.next();
				// System.out.println("ROW: "+q.get("?auteur") + " " + q.get("?ref"));
				allAuthorsUris.add(q.get("?auteur").toString());
				if (q.get("?ref") != null)
					allAuthorsUris.add(q.get("?ref").toString());
			}
			qexec.close();
			System.out.println("Total entities in domain coverage " + count);
			System.out.println("Total URIs literals (sameAs and main one) in domain coverage " + allAuthorsUris.size());

			// Checking with gold standard
			Integer totalIn = 0;
			DocumentBuilder b = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			org.w3c.dom.Document doc = b.parse(new FileInputStream(new File("input/thibaudet_reflexions-gold.xml")));
			XPath xPath = XPathFactory.newInstance().newXPath();
			NodeList nodes = (NodeList) xPath.evaluate("//body", doc.getDocumentElement(), XPathConstants.NODESET);
			for (int i = 0; i < nodes.getLength(); ++i) {
				Element e = (Element) nodes.item(i);
				NodeList nodesChild = (NodeList) xPath.evaluate(".//persName", e, XPathConstants.NODESET);
				for (int k = 0; k < nodesChild.getLength(); ++k) {
					Element child = (Element) nodesChild.item(k);
					String uri_auto = child.getAttribute("ref");
					if (!uri_auto.equals("") && allAuthorsUris.contains(uri_auto)) {
						System.out.println(uri_auto);
						totalIn++;
					}
				}
			}
			System.out.println(
					"Intersection BnF authors in domain scope AND annotated authors in gold standard " + totalIn);
		} catch (ParserConfigurationException e1) {
			e1.printStackTrace();
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (SAXException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (XPathExpressionException e1) {
			e1.printStackTrace();
		}
	}

	public static void bnf() {
		
		String domainScopeAuthorsQuery = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
				+ "PREFIX foaf: <http://xmlns.com/foaf/0.1/> "
				+ "PREFIX owl: <http://www.w3.org/2002/07/owl#> "
				+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "
				+ "PREFIX bnf-onto: <http://data.bnf.fr/ontology/bnf-onto/> "
				+ "PREFIX rdagroup2elements: <http://rdvocab.info/ElementsGr2/> "
				+ "SELECT distinct ?auteur ?ref  WHERE { "
				+ "?auteur rdf:type foaf:Person . "
				+ "?auteur foaf:familyName ?nom . "
				+ "?auteur rdagroup2elements:languageOfThePerson ?langue . "
				+ "?auteur bnf-onto:firstYear ?birthdate . "
				+ "OPTIONAL { ?auteur owl:sameAs ?ref . "
				+ "FILTER regex(STR(?ref), '^http://www.idref.fr|^http://dbpedia.org/resource', 'i') }"
				+ "} ";
		
		try {
			QueryExecution qexec = QueryExecutionFactory.sparqlService("http://data.bnf.fr/sparql", domainScopeAuthorsQuery);
			ResultSet results = qexec.execSelect() ;
		      results = ResultSetFactory.copyResults(results);
		      Integer count = 0;
		      Set<String> allAuthorsUris = new HashSet<String>(); 
		      //System.out.println(results.next().get("?total"));
		      while (results.hasNext()) {
		    	  count++;
		    	  QuerySolution q = results.next();
		    	  //System.out.println("ROW: "+q.get("?auteur")  + " " + q.get("?ref"));
		    	  allAuthorsUris.add(q.get("?auteur").toString().replaceAll("#foaf:Person", ""));
		    	  if (q.get("?ref") != null)
		    		  allAuthorsUris.add(q.get("?ref").toString());
		      }
		      qexec.close();		    				      
		      System.out.println("Total entities in domain coverage "+count);
		      System.out.println("Total URIs literals (sameAs and main one) in domain coverage "+allAuthorsUris.size());
		      
		      //Checking with gold standard
		      Integer totalIn = 0;
		      DocumentBuilder b = DocumentBuilderFactory.newInstance()
						.newDocumentBuilder();
		      org.w3c.dom.Document doc = b.parse(new FileInputStream(new File ("input/cold-iswc2016/thibaudet/thibaudet_reflexions-gold-bnf.xml")));
				XPath xPath = XPathFactory.newInstance().newXPath();
				NodeList nodes = (NodeList) xPath.evaluate(
						"//body",
						doc.getDocumentElement(), XPathConstants.NODESET);
				for (int i = 0; i < nodes.getLength(); ++i) {
					Element e = (Element) nodes.item(i);
					NodeList nodesChild = (NodeList) xPath.evaluate(".//persName",
							e, XPathConstants.NODESET);
					for (int k = 0; k < nodesChild.getLength(); ++k) {
						Element child = (Element) nodesChild.item(k);
						String uri_auto = child.getAttribute("ref");
						if (!uri_auto.equals("") && allAuthorsUris.contains(uri_auto)) {
							System.out.println(uri_auto);
							totalIn++;
						}
					}
				}
				System.out.println("Intersection BnF authors in domain scope AND annotated authors in gold standard "+totalIn);
		} catch (ParserConfigurationException e1) {
			e1.printStackTrace();
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (SAXException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (XPathExpressionException e1) {
			e1.printStackTrace();
		}	
	}

	public static void bne() {

		String domainScopeAuthorsQuery = "PREFIX ns2: <http://datos.bne.es/def/> "
				+ "PREFIX owl: <http://www.w3.org/2002/07/owl#> "
				+ "SELECT distinct ?author ?acceptedForm ?rejectedForm ?period ?ref " + "where { "
				+ "?author ns2:OP5001 ?work . " + "?author ns2:P5001 ?acceptedForm . "
				+ "OPTIONAL {?author ns2:P5012 ?rejectedForm } . " + "OPTIONAL {?author owl:sameAs ?ref } . "
				+ "OPTIONAL { ?author ns2:P5002 ?period } " + "} ";

		try {
			QueryExecution qexec = QueryExecutionFactory.sparqlService("http://datos.bne.es/sparql",
					domainScopeAuthorsQuery);
			ResultSet results = qexec.execSelect();
			results = ResultSetFactory.copyResults(results);
			Integer count = 0;
			Set<String> allAuthorsUris = new HashSet<String>();
			// System.out.println(results.next().get("?total"));
			while (results.hasNext()) {
				count++;
				QuerySolution q = results.next();
				// System.out.println("ROW: "+q.get("?auteur") + " " + q.get("?ref"));
				allAuthorsUris.add(q.get("?author").toString());
				if (q.get("?ref") != null)
					allAuthorsUris.add(q.get("?ref").toString());
			}
			qexec.close();
			System.out.println("Total entities in domain coverage " + count);
			System.out.println("Total URIs literals (sameAs and main one) in domain coverage " + allAuthorsUris.size());

			// Checking with gold standard
			Integer totalIn = 0;
			DocumentBuilder b = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			org.w3c.dom.Document doc = b.parse(
					new FileInputStream(new File("input/cold-iswc2016/thibaudet/thibaudet_reflexions-gold-bne.xml")));
			XPath xPath = XPathFactory.newInstance().newXPath();
			NodeList nodes = (NodeList) xPath.evaluate("//body", doc.getDocumentElement(), XPathConstants.NODESET);
			for (int i = 0; i < nodes.getLength(); ++i) {
				Element e = (Element) nodes.item(i);
				NodeList nodesChild = (NodeList) xPath.evaluate(".//persName", e, XPathConstants.NODESET);
				for (int k = 0; k < nodesChild.getLength(); ++k) {
					Element child = (Element) nodesChild.item(k);
					String uri_auto = child.getAttribute("ref");
					if (!uri_auto.equals("") && allAuthorsUris.contains(uri_auto)) {
						System.out.println(uri_auto);
						totalIn++;
					}
				}
			}
			System.out.println(
					"Intersection BNE authors in domain scope AND annotated authors in gold standard " + totalIn);
		} catch (ParserConfigurationException e1) {
			e1.printStackTrace();
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (SAXException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (XPathExpressionException e1) {
			e1.printStackTrace();
		}
	}

	public static void getty() {

		String domainScopeAuthorsQuery = "PREFIX gvp: <http://vocab.getty.edu/ontology#> "
				+ "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
				+ "PREFIX skos:<http://www.w3.org/2004/02/skos/core#> " + "PREFIX schema:<http://schema.org/> "
				+ "PREFIX foaf: <http://xmlns.com/foaf/0.1/> " + "select ?person ?nom ?altname ?ref ?gender WHERE { "
				+ "?person rdf:type gvp:PersonConcept . " + "?person skos:prefLabel ?nom . "
				+ "OPTIONAL { ?person skos:altLabel ?altname } . "
				+ "OPTIONAL { ?person skos:exactMatch ?ref . FILTER (!regex(STR(?ref), '^http://vocab.getty.edu', 'i')) } . "
				+ "OPTIONAL { ?person foaf:focus ?agent . " + "?agent gvp:biography ?biopers . "
				+ "?biopers schema:gender ?gender } }";

		try {
			QueryExecution qexec = QueryExecutionFactory.sparqlService("http://vocab.getty.edu/sparql",
					domainScopeAuthorsQuery);
			ResultSet results = qexec.execSelect();
			results = ResultSetFactory.copyResults(results);
			Integer count = 0;
			Set<String> allAuthorsUris = new HashSet<String>();
			// System.out.println(results.next().get("?total"));
			while (results.hasNext()) {
				count++;
				QuerySolution q = results.next();
				// System.out.println("ROW: "+q.get("?auteur") + " " + q.get("?ref"));
				allAuthorsUris.add(q.get("?person").toString());
				if (q.get("?ref") != null)
					allAuthorsUris.add(q.get("?ref").toString());
			}
			qexec.close();
			System.out.println("Total entities in domain coverage " + count);
			System.out.println("Total URIs literals (sameAs and main one) in domain coverage " + allAuthorsUris.size());

			// Checking with gold standard
			Integer totalIn = 0;
			DocumentBuilder b = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			org.w3c.dom.Document doc = b.parse(new FileInputStream(
					new File("input/cold-iswc2016/apollinaire/apollinaire-médtations-esthétiques-gold-getty.xml")));
			XPath xPath = XPathFactory.newInstance().newXPath();
			NodeList nodes = (NodeList) xPath.evaluate("//body", doc.getDocumentElement(), XPathConstants.NODESET);
			for (int i = 0; i < nodes.getLength(); ++i) {
				Element e = (Element) nodes.item(i);
				NodeList nodesChild = (NodeList) xPath.evaluate(".//persName", e, XPathConstants.NODESET);
				for (int k = 0; k < nodesChild.getLength(); ++k) {
					Element child = (Element) nodesChild.item(k);
					String uri_auto = child.getAttribute("ref");
					if (!uri_auto.equals("") && allAuthorsUris.contains(uri_auto)) {
						System.out.println(uri_auto);
						totalIn++;
					}
				}
			}
			System.out.println(
					"Intersection Getty personalities in domain scope AND annotated personalities in gold standard "
							+ totalIn);
		} catch (ParserConfigurationException e1) {
			e1.printStackTrace();
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (SAXException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (XPathExpressionException e1) {
			e1.printStackTrace();
		}
	}

	public static void dbpedia2() {

		String domainScopeAuthorsQuery = "PREFIX db-owl: <http://dbpedia.org/ontology/> "
				+ "PREFIX owl: <http://www.w3.org/2002/07/owl#> "
				+ "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
				+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
				+ "select distinct ?val ?labelfr ?labelred ?otherLinks where { " + " ?val rdf:type db-owl:Person ."
				+ " ?val rdfs:label ?labelfr . " + " filter(langMatches(lang(?labelfr),'FR')) ."
				+ " OPTIONAL {?red db-owl:wikiPageRedirects ?val ." + " ?red rdfs:label ?labelred ."
				+ " filter(langMatches(lang(?labelred),'FR')) } . " + " OPTIONAL { ?val owl:sameAs ?otherLinks ."
				+ " FILTER regex(str(?otherLinks), '^http://dbpedia.org/', 'i')} }";

		try {
			QueryExecution qexec = QueryExecutionFactory.sparqlService("http://fr.dbpedia.org/sparql",
					domainScopeAuthorsQuery);
			ResultSet results = qexec.execSelect();
			results = ResultSetFactory.copyResults(results);
			Integer count = 0;
			Set<String> allAuthorsUris = new HashSet<String>();
			// System.out.println(results.next().get("?total"));
			while (results.hasNext()) {
				count++;
				QuerySolution q = results.next();
				// System.out.println("ROW: "+q.get("?auteur") + " " + q.get("?ref"));
				allAuthorsUris.add(q.get("?val").toString());
				if (q.get("?ref") != null)
					allAuthorsUris.add(q.get("?otherLinks").toString());
			}
			qexec.close();
			System.out.println("Total entities in domain coverage " + count);
			System.out.println("Total URIs literals (sameAs and main one) in domain coverage " + allAuthorsUris.size());

			// Checking with gold standard
			Integer totalIn = 0;
			DocumentBuilder b = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			// org.w3c.dom.Document doc = b.parse(new FileInputStream(new File
			// ("input/cold-iswc2016/apollinaire/apollinaire-médtations-esthétiques-gold-dbpediafr.xml")));
			org.w3c.dom.Document doc = b.parse(new FileInputStream(
					new File("input/cold-iswc2016/thibaudet/thibaudet_reflexions-gold-dbpediafr.xml")));
			XPath xPath = XPathFactory.newInstance().newXPath();
			NodeList nodes = (NodeList) xPath.evaluate("//body", doc.getDocumentElement(), XPathConstants.NODESET);
			for (int i = 0; i < nodes.getLength(); ++i) {
				Element e = (Element) nodes.item(i);
				NodeList nodesChild = (NodeList) xPath.evaluate(".//persName", e, XPathConstants.NODESET);
				for (int k = 0; k < nodesChild.getLength(); ++k) {
					Element child = (Element) nodesChild.item(k);
					String uri_auto = child.getAttribute("ref");
					if (!uri_auto.equals("") && allAuthorsUris.contains(uri_auto)) {
						System.out.println(uri_auto);
						totalIn++;
					}
				}
			}
			System.out.println(
					"Intersection DBpediafr persons in domain scope AND annotated persons in gold standard " + totalIn);
		} catch (ParserConfigurationException e1) {
			e1.printStackTrace();
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (SAXException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (XPathExpressionException e1) {
			e1.printStackTrace();
		}
	}

	public static void dbpedia() {

		String domainScopeAuthorsQuery = "PREFIX prop-fr: <http://fr.dbpedia.org/property/>"
				+ " PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
				+ " PREFIX owl: <http://www.w3.org/2002/07/owl#> " + "	PREFIX dcterms: <http://purl.org/dc/terms/> "
				+ "	SELECT distinct ?ecriv ?refsame WHERE {" + "	?ecriv dcterms:subject ?c." + "	?c rdfs:label ?lp."
				+ "	?ecriv rdfs:label ?l . " + " ?ecriv owl:sameAs ?refsame . "
				+ "	FILTER regex(?lp, '.crivain fran.ais du XIXe si.cle')."
				+ "	OPTIONAL {?ecriv prop-fr:nomDeNaissance ?ndn}.	}";

		try {
			QueryExecution qexec = QueryExecutionFactory.sparqlService("http://fr.dbpedia.org/sparql",
					domainScopeAuthorsQuery);
			ResultSet results = qexec.execSelect();
			results = ResultSetFactory.copyResults(results);
			Integer count = 0;
			Set<String> allAuthorsUris = new HashSet<String>();
			// System.out.println(results.next().get("?total"));
			while (results.hasNext()) {
				count++;
				QuerySolution q = results.next();
				System.out.println("ROW: " + q.get("?ecriv") + " " + q.get("?refsame"));
				allAuthorsUris.add(q.get("?ecriv").toString());
				if (q.get("?refsame") != null) {
					if (q.get("?refsame").toString().contains("idref")) {
						allAuthorsUris.add(q.get("?refsame").toString().replaceAll("/id", ""));
					} else {
						allAuthorsUris.add(q.get("?refsame").toString());
					}
				}
			}
			qexec.close();
			System.out.println("Total entities in domain coverage " + count);
			System.out.println("Total URIs literals (sameAs and main one) in domain coverage " + allAuthorsUris.size());

			// Checking with gold standard
			Integer totalIn = 0;
			DocumentBuilder b = DocumentBuilderFactory.newInstance().newDocumentBuilder(); //
			org.w3c.dom.Document doc = b.parse(new FileInputStream(new File("input/thibaudet_reflexions-gold.xml")));
			XPath xPath = XPathFactory.newInstance().newXPath();
			NodeList nodes = (NodeList) xPath.evaluate("//body", doc.getDocumentElement(), XPathConstants.NODESET);
			for (int i = 0; i < nodes.getLength(); ++i) {
				Element e = (Element) nodes.item(i);
				NodeList nodesChild = (NodeList) xPath.evaluate(".//persName", e, XPathConstants.NODESET);
				for (int k = 0; k < nodesChild.getLength(); ++k) {
					Element child = (Element) nodesChild.item(k);
					String uri_auto = child.getAttribute("ref");
					if (!uri_auto.equals("") && allAuthorsUris.contains(uri_auto)) {
						System.out.println(uri_auto);
						totalIn++;
					}
				}
			}
			System.out.println(
					"Intersection BnF authors in domain scope AND annotated authors in gold standard " + totalIn);
		} catch (ParserConfigurationException e1) {
			e1.printStackTrace();
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (SAXException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (XPathExpressionException e1) {
			e1.printStackTrace();
		}
	}
}
