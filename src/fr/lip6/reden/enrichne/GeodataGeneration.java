package fr.lip6.reden.enrichne;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;

import fr.lip6.reden.nelinker.Util;

/**
 * Auxiliary class to retrieve geo-coordinates provided by LD sources 
 * from URIs specified in the input TEI file.
 * 
 * @author Brando & Frontini
 */
public class GeodataGeneration {
	
	/**
	 * Assigns geographic coordinates from the RDF data. 
	 * @param toponyms
	 * @param latLongPropertyFile
	 * @return
	 */
	public static Map<String, Map<String, String>> assignGeoCoordinates(Map<String, Map<String, String>> toponyms, 
			String latLongPropertyFile, String datadir) {
		try {
			//then, we search for Lat/Lon property values from the RDF data for these toponyms
			//read property file
			Properties prop = new Properties();
			InputStream input = new FileInputStream(latLongPropertyFile);
			prop.load(input);			
			String[] propLatNameList = prop.getProperty("LatProperties").replaceAll(" ", "").trim().split(","); 
			String[] propLonNameList = prop.getProperty("LongProperties").replaceAll(" ", "").trim().split(",");
			
			for (String placeOrigName : toponyms.keySet()) {
				
				//rdf data have already been downloaded
				if (new File(datadir + "/file" + Util.replaceNonAlphabeticCharacters(placeOrigName) + ".n3").exists()) {
					
					Model model = ModelFactory.createDefaultModel();
					model.read(datadir + "/file" + Util.replaceNonAlphabeticCharacters(placeOrigName) + ".n3");					
					Resource res = model.getResource(placeOrigName);
					RDFNode latOb = EnrichmentHandler.getValFromProperty(model, res, propLatNameList, null);
					RDFNode lonOb = EnrichmentHandler.getValFromProperty(model, res, propLonNameList, null);
					if (latOb != null && lonOb != null) {
						Double lat = latOb.asLiteral().getDouble();							
						Double lon = lonOb.asLiteral().getDouble();
						System.out.println("for "+placeOrigName+", Lat is "+lat+" and Long is "+lon);
						Map<String, String> m = toponyms.get(placeOrigName);
						m.put("lat", lat.toString());
						m.put("lon", lon.toString());
						toponyms.put(placeOrigName, m); //update
					}					
				} else {
					System.out.println("RDF file is missing: "+datadir + "/file" + placeOrigName.hashCode() + ".n3");
				}							
			}
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		return toponyms;
	}
	
}
