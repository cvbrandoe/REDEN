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
import org.apache.jena.rdf.model.Statement;

/**
 * Class for calculating population completeness in a given Linked data set
 * with respect to a literary text annotated by humans (gold standard)
 * QLD'16@EGC workshop
 * @author Brando & Frontini
 *
 */
public class CalculatePopulationCompletenessLD {

	public static void main(String [] args) {
		//bnf();
		dbpedia();
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
				+ "FILTER (?birthdate < 1900). "
				+ "FILTER regex (str(?langue), 'http://id.loc.gov/vocabulary/iso639-2/fre'). "
				+ "OPTIONAL { ?auteur owl:sameAs ?ref . "
				+ "FILTER regex(STR(?ref), '^http://www.idref.fr|^http://dbpedia.org/resource', 'i') }"
				+ "} ";
		
		try (QueryExecution qexec = QueryExecutionFactory.sparqlService("http://data.bnf.fr/sparql", domainScopeAuthorsQuery)) {
		      ResultSet results = qexec.execSelect() ;
		      results = ResultSetFactory.copyResults(results);
		      Integer count = 0;
		      Set<String> allAuthorsUris = new HashSet<String>(); 
		      //System.out.println(results.next().get("?total"));
		      while (results.hasNext()) {
		    	  count++;
		    	  QuerySolution q = results.next();
		    	  //System.out.println("ROW: "+q.get("?auteur")  + " " + q.get("?ref"));
		    	  allAuthorsUris.add(q.get("?auteur").toString());
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
		      org.w3c.dom.Document doc = b.parse(new FileInputStream(new File ("input/thibaudet_reflexions-gold.xml")));
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
	
	public static void dbpedia() {
		
		String domainScopeAuthorsQuery = "PREFIX prop-fr: <http://fr.dbpedia.org/property/>"
				+ " PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
				+ " PREFIX owl: <http://www.w3.org/2002/07/owl#> "
				+ "	PREFIX dcterms: <http://purl.org/dc/terms/> "
				+ "	SELECT distinct ?ecriv ?refsame WHERE {"
				+ "	?ecriv dcterms:subject ?c."
				+ "	?c rdfs:label ?lp."
				+ "	?ecriv rdfs:label ?l . "
				+ " ?ecriv owl:sameAs ?refsame . "
				+ "	FILTER regex(?lp, '.crivain fran.ais du XIXe si.cle')."
				+ "	OPTIONAL {?ecriv prop-fr:nomDeNaissance ?ndn}.	}";
		
		try (QueryExecution qexec = QueryExecutionFactory.sparqlService("http://fr.dbpedia.org/sparql", domainScopeAuthorsQuery)) {
		      ResultSet results = qexec.execSelect() ;
		      results = ResultSetFactory.copyResults(results);
		      Integer count = 0;
		      Set<String> allAuthorsUris = new HashSet<String>(); 
		      //System.out.println(results.next().get("?total"));
		      while (results.hasNext()) {
		    	  count++;
		    	  QuerySolution q = results.next();
		    	  System.out.println("ROW: "+q.get("?ecriv")  + " " + q.get("?refsame"));
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
		      System.out.println("Total entities in domain coverage "+count);
		      System.out.println("Total URIs literals (sameAs and main one) in domain coverage "+allAuthorsUris.size());
		      
		      //Checking with gold standard
		      Integer totalIn = 0;
		      DocumentBuilder b = DocumentBuilderFactory.newInstance()
						.newDocumentBuilder(); //
		      org.w3c.dom.Document doc = b.parse(new FileInputStream(new File ("input/thibaudet_reflexions-gold.xml")));
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
}
