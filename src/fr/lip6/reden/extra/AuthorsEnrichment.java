package fr.lip6.reden.extra;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.FileUtils;
import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.Point;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.FileManager;

/**
 * Auxiliary class to retrieve author's pics provided by LD sources 
 * from URIs specified in the input TEI file.
 * 
 * @author @author Brando & Frontini
 */
public class AuthorsEnrichment {

	/**
	 * Main program, TODO retravailler tous mes problemes avec RDF, faire du SPARL (mais quel end point, il me semble que je n'en avais pas trouvé)
	 * e.g: output\thibaudet_reflexions-outV3-enrichment.xml output\authorInformation.json bnf config\authors.properties ref_auto
	 * @param args
	 */
	public static void main(String [] args) {		
		if (args.length != 5) {
			System.out.println("The folder dataAuthor must exist. Program parameters are inputTeiFile, outputjsonFile, "
					+ "LD repo (only bnf), RDF properties file, "
					+ "name of XML-TEI attribute of identifier (e.g. ref_auto");
		} else {
			//reads TEI, gets authors and retrieve RDF
			Map<String, Map<String, String>> authors = readTEI(args[0], args[2], args[4]);
			//attribute pics
			authors = assignAuthorsPropValue(authors,  args[3]);
			//produces the GeoJson file
			toGeoJson(authors, args[1]);
		}
	}
	
	/**
	 * Read toponyms URIs in TEI file and retrieve if necessary the associated RDF data.
	 * @return
	 */
	public static Map<String, Map<String, String>> readTEI(String teiAnnotatedFile,
			String ldRepo, String xmlTeiIDAttr) {
		
		Map<String, Map<String, String>> authors = new HashMap<String, Map<String, String>>();
		
		try {				
			//firstly, we store RDF data associated to the URIs specified in the TEI file
			DocumentBuilder b = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			org.w3c.dom.Document doc = b.parse(new FileInputStream(teiAnnotatedFile));
			XPath xPath = XPathFactory.newInstance().newXPath();
			NodeList nodes = (NodeList) xPath.evaluate(
					"//body//head|//body//item|//body//l|//body//p", //TODO this must be a parameter.
					doc.getDocumentElement(), XPathConstants.NODESET);
			
			for (int i = 0; i < nodes.getLength(); ++i) {
				Element e = (Element) nodes.item(i);
				NodeList nodesChild = (NodeList) xPath.evaluate(".//"
						+ "persName", e, XPathConstants.NODESET);
				
				for (int k = 0; k < nodesChild.getLength(); ++k) {
					Element child = (Element) nodesChild.item(k);
					String authorName = child.getTextContent();
					System.out.println("");
					System.out.println("Author name is " + authorName);
					String uri = child.getAttribute(xmlTeiIDAttr);					
					
					if (!uri.equals("")) { 
						
						//TEMP
						String [] temp = uri.split(" ");
						for (int l = 0; l < temp.length; l++) {
							if (temp[l].contains("data.bnf.fr"))
								uri = temp[l];
						}
						if (!authors.keySet().contains(uri)) { //first time I see this URI, only get once the RDF
							System.out.println("URI is "+uri);
							Map<String, String> m = new HashMap<String, String>();
							m.put("name", authorName);							
							m.put("occurrences", "1");							
							
							m.put("bnfUri", uri);
							authors.put(uri, m);
						
								// to go faster (remove f.exists if we want to update local triples)
								File f = new File("dataAuthor" + "/file" + uri.hashCode() + ".n3");
								
								if (!f.exists() || FileUtils.readFileToString(f).trim().isEmpty()) {
									
									// check rdf repos are available
									URL u = new URL(uri);
									HttpURLConnection huc = (HttpURLConnection) u
											.openConnection();
									huc.setRequestMethod("GET");
									huc.connect();
									int code = huc.getResponseCode();
									if (code != 503 && code != 404 
											&& !uri.equalsIgnoreCase("http://data.bnf.fr/ark:/12148/cb14641582c#foaf:Person")
											&& !uri.equalsIgnoreCase("http://data.bnf.fr/ark:/12148/cb10503577q#foaf:Person")
											&& !uri.equalsIgnoreCase("http://data.bnf.fr/ark:/12148/cb137533560#foaf:Person")
											&& !uri.equalsIgnoreCase("http://data.bnf.fr/ark:/12148/cb14461983w#foaf:Person")
											&& !uri.equalsIgnoreCase("http://data.bnf.fr/ark:/12148/cb15559816h#foaf:Person") //TEMP
											&& !uri.equalsIgnoreCase("http://data.bnf.fr/ark:/12148/cb12704079w#foaf:Person")
											&& !uri.equalsIgnoreCase("http://data.bnf.fr/ark:/12148/cb12231411d#foaf:Person")
											&& !uri.equalsIgnoreCase("http://data.bnf.fr/ark:/12148/cb123059537#foaf:Person")
											&& !uri.equalsIgnoreCase("http://data.bnf.fr/ark:/12148/cb11909393p#foaf:Person")
											&& !uri.equalsIgnoreCase("http://data.bnf.fr/ark:/12148/cb10329764f#foaf:Person")
											&& !uri.equalsIgnoreCase("http://data.bnf.fr/ark:/12148/cb138913346#foaf:Person")
											&& !uri.equalsIgnoreCase("http://data.bnf.fr/ark:/12148/cb12054596p#foaf:Person")
											&& !uri.equalsIgnoreCase("http://data.bnf.fr/ark:/12148/cb12443740v#foaf:Person")
											&& !uri.equalsIgnoreCase("http://data.bnf.fr/ark:/12148/cb12227128j#foaf:Person")
											){
										InputStream in = FileManager.get().open(uri);
										if (in != null) {
											Model model = ModelFactory.createDefaultModel();
											System.out.println("RDF data will be stored in "+"dataAuthor" + "/file" + uri.hashCode() + ".n3");
											model.read(in, null, "RDF/XML"); //sometimes, need to specify N3									
											OutputStream fileOutputStream = new FileOutputStream(f);
											OutputStreamWriter out = new OutputStreamWriter(fileOutputStream, "UTF-8");
											model.write(out, "N3");
										} else {
											System.out.println("skip URI: " + uri);
										}
										
									} else {
										System.out.println("RDF repo is not available "
												+ uri);
									}
								} else {
									System.out.println("RDF file is present: "+"dataAuthor" + "/file" + uri.hashCode() + ".n3");
								}
							
						} else { //already seen this uri, count it
							Map<String, String> m = authors.get(uri);
							Integer count = Integer.parseInt(m.get("occurrences"));
							count++;
							m.put("occurrences", count.toString());
							authors.put(uri, m); //update with count
						}						
					} else {
						System.out.println("skip URI: " + uri);
					}
				}				
			}
			System.out.println("Number of different authors: "+authors.size());			
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
		return authors;
	}
	
	/**
	 * Assigns author's pic from the RDF data. 
	 */
	public static Map<String, Map<String, String>> assignAuthorsPropValue(Map<String, Map<String, String>> authors, String rdfPropertiesFile) {
		try {
			//then, we search for foaf:depiction property values from the RDF data for these authors
			//read property file
			Properties prop = new Properties();
			InputStream input = new FileInputStream(rdfPropertiesFile);
			prop.load(input);			
			
			//pic
			String[] picNameList = prop.getProperty("picProperties").replaceAll(" ", "").trim().split(",");
			//domaine
			String[] domNameList = prop.getProperty("domaineProperties").replaceAll(" ", "").trim().split(","); 
			
			for (String author : authors.keySet()) {
				
				if (new File("dataAuthor" + "/file" + author.hashCode() + ".n3").exists()) {
					
					Model model = ModelFactory.createDefaultModel();
					model.read("dataAuthor" + "/file" + author.hashCode() + ".n3");					
					Resource res = model.getResource(author);
					String pic = getValFromProperty(model, res, picNameList, "http://commons.wikimedia.org"); 
					if (pic != null) {
						System.out.println("for "+author+", Depication is "+pic);
						Map<String, String> m = authors.get(author);
						m.put("depiction", pic);
						authors.put(author, m); //update
					}	
					String dom = getValFromProperty(model, res, domNameList, ""); 
					if (dom != null) {
						System.out.println("for "+author+", Domaine is "+dom);
						Map<String, String> m = authors.get(author);
						m.put("fieldOfActivityOfThePerson", dom);
						authors.put(author, m); //update
					}
				} else {
					System.out.println("RDF file is missing: "+"dataAuthor" + "/file" + author.hashCode() + ".n3");
				}							
			}
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		return authors;
	}
	
	/**
	 * Get value of at least a property for a given resource.
	 * @param model
	 * @param res
	 * @param propList
	 * @return
	 */
	public static String getValFromProperty(Model model, Resource res, String[] propList, String base) {
		
		int index = 0;
		String val = null;
		Property propPic = null;
		while (val == null && index < propList.length ) {
			propPic = model.getProperty(propList[index]);
			if (propPic != null) {
				Iterator<Statement> stmtIt = res.listProperties(propPic);
				while (stmtIt.hasNext()) {
					Statement stmt = stmtIt.next();
					if (stmt != null) {
						val = stmt.getObject().toString();
						if (val.startsWith(base))
							return val;
					} 					
				}			
			}
			index++;			
		}
		return val;
	}

	/**
	 * Convert to GeoJson
	 * @param authors
	 * @param geojsonFile
	 */
	public static void toGeoJson(Map<String, Map<String, String>> authors, String geojsonFile) {
		
		FeatureCollection featureCollection = new FeatureCollection();//attention ce n'est pas du geojson..
		
		for (String placeOrigName : authors.keySet()) {
			Feature feat = new Feature();
			Map<String, String> props = authors.get(placeOrigName);
			if (props.containsKey("depiction") && 
					props.containsKey("fieldOfActivityOfThePerson")) {				
				for (String keyProp : authors.get(placeOrigName).keySet()) {					
					feat.setProperty(keyProp, authors.get(placeOrigName).get(keyProp));
				}
				featureCollection.add(feat);
			}			
		}

		try {
			PrintWriter out = new PrintWriter(geojsonFile);
			String json = new ObjectMapper()
					.writeValueAsString(featureCollection); 
			out.println(json);
			out.close();
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

}
