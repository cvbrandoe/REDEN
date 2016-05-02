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
 * Auxiliary class to retrieve author's pics provided by LD sources 
 * from URIs specified in the input TEI file.
 * 
 * @author @author Brando & Frontini
 */
public class AuthorsEnrichment {

	/**
	 * Assigns author's pic from the RDF data. 
	 */
	public static Map<String, Map<String, String>> assignAuthorsPropValue(Map<String, Map<String, String>> authors, String rdfPropertiesFile, String datadir) {
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
				
				if (new File(datadir + "/file" + Util.replaceNonAlphabeticCharacters(author) + ".n3").exists()) {
					
					Model model = ModelFactory.createDefaultModel();
					model.read(datadir + "/file" + Util.replaceNonAlphabeticCharacters(author) + ".n3");					
					Resource res = model.getResource(author);
					RDFNode picN = EnrichmentHandler.getValFromProperty(model, res, picNameList, "http://commons.wikimedia.org");
					if (picN != null) {
						String pic = picN.toString(); 
						System.out.println("for "+author+", Depication is "+pic);
						Map<String, String> m = authors.get(author);
						m.put("depiction", pic);
						authors.put(author, m); //update						
					}
					RDFNode domN = EnrichmentHandler.getValFromProperty(model, res, domNameList, "");
					if (domN != null) {
						String dom = domN.toString(); 
						System.out.println("for "+author+", Domaine is "+dom);
						Map<String, String> m = authors.get(author);
						m.put("fieldOfActivityOfThePerson", dom);
						authors.put(author, m); //update
					}
				} else {
					System.out.println("RDF file is missing: "+datadir + "/file" + Util.replaceNonAlphabeticCharacters(author) + ".n3");
				}							
			}
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		return authors;
	}

}
