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
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.util.FileManager;

/**
 * Auxiliary class to retrieve geo-coordinates provided by LD sources 
 * from URIs specified in the input TEI file.
 * 
 * @author @author Brando & Frontini
 */
public class GeodataGeneration {
	
	/**
	 * Main program, e.g: output\dbpedia-results-gir2015\apollinaire_heresiarque-et-cie-outV3.xml output\localisationInformation.json dbpedia config\latlong.properties ref_auto
	 * @param args
	 * Param1: inputTeiFile
	 * Param2: outputGeojsonFile
	 * Param3: LD repo - "dbpedia" or "geonames (so far, the only ones supported)
	 * Param4: property file listing properties lat and long using different vocabularies
	 * Param5: name of XML-TEI attribute of identifier (e.g. "ref_auto", "ref") 
	 */
	public static void main(String [] args) {
		
		if (args.length != 5) {
			System.out.println("The folder dataGeo must exist. Program parameters are inputTeiFile, outputGeojsonFile, "
					+ "LD repo (dbpedia or geonames), lat/lon property file, "
					+ "name of XML-TEI attribute of identifier (e.g. ref_auto");
		} else {
			//reads TEI, gets toponyms and retrieve RDF
			Map<String, Map<String, String>> toponyms = readTEI(args[0], args[2], args[4]);
			//attribute geo-coordinates
			toponyms = assignGeoCoordinates(toponyms,  args[3]);
			//produces the GeoJson file
			toGeoJson(toponyms, args[1]);
		}
	}
	
	/**
	 * Read toponyms URIs in TEI file and retrieve if necessary the associated RDF data.
	 * @return
	 */
	public static Map<String, Map<String, String>> readTEI(String teiAnnotatedFile,
			String ldRepo, String xmlTeiIDAttr) {
		
		Map<String, Map<String, String>> toponyms = new HashMap<String, Map<String, String>>();
		
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
						+ "placeName", e, XPathConstants.NODESET);
				
				for (int k = 0; k < nodesChild.getLength(); ++k) {
					Element child = (Element) nodesChild.item(k);
					String placeName = child.getTextContent();
					System.out.println("");
					System.out.println("Place name is " + placeName);
					String uri = child.getAttribute(xmlTeiIDAttr);					
					
					if (!uri.equals("")) { 
						
						if (!toponyms.keySet().contains(uri)) { //firs time I see this URI, only get once the RDF
							System.out.println("URI is "+uri);
							Map<String, String> m = new HashMap<String, String>();
							m.put("name", placeName);							
							m.put("occurrences", "1");							
							InputStream in = null;
							if(ldRepo.equalsIgnoreCase("dbpedia")) {
								in = FileManager.get().open(uri.replace("/page/", "/data/")+".ntriples");
								m.put("dbpediaUri", uri);
							} else { //geonmaes
								in = FileManager.get().open(uri);
								m.put("geoNamesUri", uri);
							}
							toponyms.put(uri, m);
							if (in != null) {
								// to go faster (remove f.exists if we want to update local triples)
								File f = new File("dataGeo" + "/file" + uri.hashCode() + ".n3");
								
								if (!f.exists() || FileUtils.readFileToString(f).trim().isEmpty()) {
									Model model = ModelFactory.createDefaultModel();
									System.out.println("RDF data will be stored in "+"dataGeo" + "/file" + uri.hashCode() + ".n3");
									model.read(in, null, "N3");									
									OutputStream fileOutputStream = new FileOutputStream(f);
									OutputStreamWriter out = new OutputStreamWriter(fileOutputStream, "UTF-8");
									model.write(out, "N3");
								} else {
									System.out.println("RDF file is present: "+"dataGeo" + "/file" + uri.hashCode() + ".n3");
								}
							} else {
								System.out.println("skip URI: " + uri);
							}
						} else { //already seen this uri, count it
							Map<String, String> m = toponyms.get(uri);
							Integer count = Integer.parseInt(m.get("occurrences"));
							count++;
							m.put("occurrences", count.toString());
							toponyms.put(uri, m); //update with count
						}						
					} else {
						System.out.println("skip URI: " + uri);
					}
				}				
			}
			System.out.println("Number of different toponyms: "+toponyms.size());			
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
		return toponyms;
	}
	
	/**
	 * Assigns geographic coordinates from the RDF data. 
	 * @param toponyms
	 * @param latLongPropertyFile
	 * @return
	 */
	public static Map<String, Map<String, String>> assignGeoCoordinates(Map<String, Map<String, String>> toponyms, String latLongPropertyFile) {
		try {
			//then, we search for Lat/Lon property values from the RDF data for these toponyms
			//read property file
			Properties prop = new Properties();
			InputStream input = new FileInputStream(latLongPropertyFile);
			prop.load(input);			
			String[] propLatNameList = prop.getProperty("LatProperties").replaceAll(" ", "").trim().split(","); 
			String[] propLonNameList = prop.getProperty("LongProperties").replaceAll(" ", "").trim().split(",");
			
			for (String placeOrigName : toponyms.keySet()) {
				
				if (new File("dataGeo" + "/file" + placeOrigName.hashCode() + ".n3").exists()) {
					
					Model model = ModelFactory.createDefaultModel();
					model.read("dataGeo" + "/file" + placeOrigName.hashCode() + ".n3");					
					Resource res = model.getResource(placeOrigName);
					Double lat = getValFromProperty(model, res, propLatNameList);
					Double lon = getValFromProperty(model, res, propLonNameList);
					System.out.println("for "+placeOrigName+", Lat is "+lat+" and Long is "+lon);
					Map<String, String> m = toponyms.get(placeOrigName);
					if (lat != null && lon != null) {
						m.put("lat", lat.toString());
						m.put("lon", lon.toString());
						toponyms.put(placeOrigName, m); //update
					}					
				} else {
					System.out.println("RDF file is missing: "+"dataGeo" + "/file" + placeOrigName.hashCode() + ".n3");
				}							
			}
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		return toponyms;
	}
	
	/**
	 * Get value of at least a property for a given resource.
	 * @param model
	 * @param res
	 * @param propNameList
	 * @return
	 */
	public static Double getValFromProperty(Model model, Resource res, String[] propNameList) {
		
		int index = 0;
		Double val = null;
		Property propLat = null;
		while (val == null && index < propNameList.length ) {
			propLat = model.getProperty(propNameList[index]);
			if (propLat != null) {
				Statement stmt = res.getProperty(propLat);
				if (stmt != null) {
					val = stmt.getDouble();
					return val;
				} else {
					index++;
				}
			} else {
				index++;
			}
		}
		return val;
	}
	
	/**
	 * Convert to GeoJson
	 * @param toponyms
	 * @param geojsonFile
	 */
	public static void toGeoJson(Map<String, Map<String, String>> toponyms, String geojsonFile) {
		
		FeatureCollection featureCollection = new FeatureCollection();
		
		for (String placeOrigName : toponyms.keySet()) {
			Feature feat = new Feature();
			if (toponyms.get(placeOrigName).get("lon") != null && toponyms.get(placeOrigName).get("lat") != null) {
				Point p = new Point(Double.parseDouble(toponyms.get(placeOrigName)
					.get("lon")), Double.parseDouble(toponyms
					.get(placeOrigName).get("lat")));
				feat.setGeometry(p);
			}
			for (Entry<String, String> prop : toponyms.get(placeOrigName).entrySet()) {
				if (!prop.getKey().equalsIgnoreCase("lat")
						&& !prop.getKey().equalsIgnoreCase("lon")) {
					feat.setProperty(prop.getKey(), prop.getValue());
				}
			}
			featureCollection.add(feat);			
			
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
	
	/**
	 * Check LD repo is on.
	 * @param baseURL
	 * @return
	 */
	public static Boolean checkConnectionToLDRepo(String baseURL) {
		// check rdf repos are available
		try {
			URL u = new URL(baseURL);
			HttpURLConnection huc = (HttpURLConnection) u
					.openConnection();
			huc.setRequestMethod("GET");
			huc.connect();
			int code = huc.getResponseCode();
			if (code != 503 && code != 404) {
				return true;
			}
		} catch (IOException e) {			
			e.printStackTrace();
		}
		return false;
	}
}
