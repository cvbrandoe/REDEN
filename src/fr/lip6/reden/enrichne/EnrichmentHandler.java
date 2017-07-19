package fr.lip6.reden.enrichne;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.Point;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

import fr.lip6.reden.nelinker.GraphHandlerNEL;

/**
 * Reads URIs within the input TEI file and extracts intermediary information useful for visualization.
 * 
 * @author Brando & Frontini
 */
public class EnrichmentHandler {

	private static Logger logger = Logger.getLogger(EnrichmentHandler.class);
	
	/**
	 * Read toponyms URIs in TEI file and retrieve if necessary the associated RDF data.
	 * @return
	 */
	public static Map<String, Map<String, String>> readTEI(String teiAnnotatedFile,
			String xmlTeiIDAttr, String contextSize, String annotTag, String datadir) {
		
		Map<String, Map<String, String>> output = new HashMap<String, Map<String, String>>();
		
		try {				
			//firstly, we store RDF data associated to the URIs specified in the TEI file
			DocumentBuilder b = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			org.w3c.dom.Document doc = b.parse(new FileInputStream(teiAnnotatedFile));
			XPath xPath = XPathFactory.newInstance().newXPath();
			NodeList nodes = (NodeList) xPath.evaluate(
					contextSize,
					doc.getDocumentElement(), XPathConstants.NODESET);
			
			for (int i = 0; i < nodes.getLength(); ++i) {
				Element e = (Element) nodes.item(i);
				NodeList nodesChild = (NodeList) xPath.evaluate(".//"
						+ annotTag, e, XPathConstants.NODESET);
				
				for (int k = 0; k < nodesChild.getLength(); ++k) {
					Element child = (Element) nodesChild.item(k);
					String entityName = child.getTextContent();
					// TODO allow URIs with score information and more than one URI ? entityName = entityName.replaceAll("\\(\\d*\\.\\d+\\)", ""); //if there are scores in uris, remove them					
					logger.info("");
					logger.info("Entity name is " + entityName);
					String uri = child.getAttribute(xmlTeiIDAttr);					
					
					if (!uri.equals("")) { 
						
						if (!output.keySet().contains(uri)) { //firs time I see this URI, only get once the RDF
							logger.info("URI is "+uri);
							Map<String, String> m = new HashMap<String, String>();
							m.put("name", entityName);							
							m.put("occurrences", "1");							
							GraphHandlerNEL.retrieveRDF(uri, datadir);
							m.put("theuri", uri);
							output.put(uri, m);							
						} else { //already seen this uri, count it
							Map<String, String> m = output.get(uri);
							Integer count = Integer.parseInt(m.get("occurrences"));
							count++;
							m.put("occurrences", count.toString());
							output.put(uri, m); //update with count
						}						
					} else {
						logger.info("skip URI: " + uri);
					}
				}				
			}
			logger.info("Number of different entities: "+output.size());			
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
		return output;
	}
	
	
	
	/**
	 * Convert to GeoJson
	 * @param entities
	 * @param jsonFile
	 */
	public static void toJson(Map<String, Map<String, String>> entities, String jsonFile) {
		
		FeatureCollection featureCollection = new FeatureCollection();
		
		for (String entityOrigName : entities.keySet()) {
			Feature feat = new Feature();
			if (entities.get(entityOrigName).get("lon") != null && entities.get(entityOrigName).get("lat") != null) {
				Point p = new Point(Double.parseDouble(entities.get(entityOrigName)
					.get("lon")), Double.parseDouble(entities
					.get(entityOrigName).get("lat")));
				feat.setGeometry(p);
			}
			for (Entry<String, String> prop : entities.get(entityOrigName).entrySet()) {
				if (!prop.getKey().equalsIgnoreCase("lat")
						&& !prop.getKey().equalsIgnoreCase("lon")) {
					feat.setProperty(prop.getKey(), prop.getValue());
				}
			}
			featureCollection.add(feat);
		}
		try {
			PrintWriter out = new PrintWriter(jsonFile);
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
	 * Get value of at least a property for a given resource.
	 * @param model
	 * @param res
	 * @param propNameList
	 * @return
	 */
	public static RDFNode getValFromProperty(Model model, Resource res, String[] propNameList, String base) {
		
		int index = 0;
		RDFNode val = null;
		Property prop = null;
		while (val == null && index < propNameList.length ) {
			prop = model.getProperty(propNameList[index]);
			if (prop != null) {				
				Iterator<Statement> stmtIt = res.listProperties(prop);
				while (stmtIt.hasNext()) {
					Statement stmt = stmtIt.next();
					if (stmt != null) {
						val = stmt.getObject();
						if (base != null) {
							if (val.toString().startsWith(base))
								return val;
						} else {
							return val;
						}
					} 					
				}				
			}
			index++;			
		}
		return val;
	}	
	
}
