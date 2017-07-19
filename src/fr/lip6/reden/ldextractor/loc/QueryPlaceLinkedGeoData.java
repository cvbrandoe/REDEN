package fr.lip6.reden.ldextractor.loc;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.sparql.engine.http.QueryEngineHTTP;

import com.opencsv.CSVWriter;

import fr.lip6.reden.ldextractor.TopicExtent;

/**
 * This class queries the places in the LinkedGeoData SPARQL end point.
 * 
 * @author Brando & Frontini
 */
public class QueryPlaceLinkedGeoData {

	private static Logger logger = Logger.getLogger(QueryPlaceLinkedGeoData.class);
	
	/**
	 * Mandatory fields
	 */
	public String SPARQL_END_POINT = "http://linkedgeodata.org/sparql";
	
	public Integer TIMEOUT = 200000;
	
	public Boolean LARGE_REPO = true;
	
	public String prefixDictionnaireFile = "placeLGD";
	
	/**
	 * Default constructor.
	 */
	public QueryPlaceLinkedGeoData () {
		super();
	}
	
	/**
	 * Formulate a query which is decomposed in several sub-queries because of size of the LGD repo. 
	 *  
	 */	
	public String formulateSPARQLQueryString(List<TopicExtent> domainParams, 
			String firstLetter, String outDictionnaireDir) {
		
		File fexists = new File(outDictionnaireDir+"/"+prefixDictionnaireFile+firstLetter+".tsv");
		
		if (fexists.exists() && fexists.length() > 0) {
			System.out.println("entering LinkedGeoData: skip, file exists - "+
					outDictionnaireDir+"/"+prefixDictionnaireFile+firstLetter+".tsv");
			return null; //skip processing
		} else {
			logger.info("entering LinkedGeoData: formulateSPARQLQuery");
			String filterRegex = "";
			if (firstLetter.equalsIgnoreCase("other")) {
				filterRegex = "FILTER (!regex(STR(?pref), '^a|^b|^c|^d|^e|^f|^g|^h|^i|^j|^k|^l|^m|^n|^o|^p|^q|^r|^s|^t|^u|^v|^w|^x|^y|^z', 'i')) . ";			
			} else {
				filterRegex = "FILTER (regex(STR(?pref), '^"+firstLetter+"', 'i')) . ";			
			}
			String queryString = " Prefix geom: <http://geovocab.org/geometry#>" 
	        		 + " Prefix lgdo: <http://linkedgeodata.org/ontology/>"       
	        		 + " Prefix ogc: <http://www.opengis.net/ont/geosparql#>"
	        		 + " Prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>"
	        		 + " select ?spatial ?pref "
	        		 + "From <http://linkedgeodata.org> "
	        		                  + "where { "
	         		                  + "?spatial rdfs:label ?pref . "	         		                  
	         		                  + filterRegex
	         		                  + " filter(langMatches(lang(?pref),'EN')) . "
	        		                  + "?spatial geom:geometry ?node . "
	         		                  + "?node ogc:asWKT ?geo ."
	         		                //  + " Filter(bif:st_intersects (?geo, bif:st_point (41.836944, -87.684722), 0.25)) . " //	         		                 
	        		                  + "} OFFSET 50000 limit 50000 ";     
			logger.info(queryString);
			logger.info("exiting LinkedGeoData: formulateSPARQLQueryString");
			return queryString;			
		}		
	}
	
	public ResultSet executeQuery(String queryString, String timeout, String sparqlendpoint, 
			String outDictionnaireDir, String letter) {
		File fexists = new File(outDictionnaireDir+"/"+prefixDictionnaireFile+letter+".tsv");
		if (fexists.exists() && fexists.length() > 0) {
			System.out.println("entering LinkedGeoData: skip, file exists - "+outDictionnaireDir+"/"+prefixDictionnaireFile+letter+".tsv");
			return null; //file exists, skip processing
		} else {
			logger.info("entering LinkedGeoData: executeQuery");			
			QueryExecution vqe = new QueryEngineHTTP(SPARQL_END_POINT, queryString);
			ResultSet results = vqe.execSelect();
			results = ResultSetFactory.copyResults(results) ;		      
			vqe.close();
			logger.info("exiting LinkedGeoData: executeQuery");
			return results;			
		}
	}
	
	/**
	 * Handling of the results specific to the LinkedGeoData model.
	 */
	public void processResults(ResultSet res, String outDictionnaireDir, String letter,  List<TopicExtent> domainParams) {
		
		File fexists = new File(outDictionnaireDir+"/"+prefixDictionnaireFile+letter+".tsv");
	
		if (fexists.exists() && fexists.length() > 0) {
			System.out.println("entering LinkedGeoData: skip, file exists - "+outDictionnaireDir+"/"+prefixDictionnaireFile+letter+".tsv");
			return; //file exists, skip processing
		} else {
			logger.info("entering LinkedGeoData: processResults");
			if (letter != null) {
				prefixDictionnaireFile += letter;
			}
			if (!new File(outDictionnaireDir).exists()) {
				new File(outDictionnaireDir).mkdir();			
			}
			if (!new File(outDictionnaireDir).exists()) {
				new File(outDictionnaireDir).mkdir();
			}
			
			List<PlaceEntry> places = new ArrayList<PlaceEntry>();
			
			while (res.hasNext()) {
				QuerySolution sol = res.next();
				PlaceEntry tri = new PlaceEntry();
				tri.setLabelstandard(sol.getLiteral("pref").getLexicalForm());
				tri.setUri(sol.getResource("spatial").getURI());
				tri.setLabelalternative(sol.getLiteral("pref").getLexicalForm());
				places.add(tri);
				
			}
			if (places != null) {
				writeToFile(places, prefixDictionnaireFile, outDictionnaireDir);
			}
			logger.info("exiting LinkedGeoData: processResults");
		}
	}

	/**
	 * It writes the ouput TSV file containing, for each individual:
	 * <alternative_name>	<nomalized_name>	<URI>
	 * @param results, the result of the sparql query
	 * @param filename, the name of the TSV file
	 * @param outDictionnaireDir, the name of the folder where the TSV file will be stored
	 */
	public static void writeToFile(List<PlaceEntry> results, String filename, String outDictionnaireDir) {
		try {
			FileWriter mFileWriter = new FileWriter(outDictionnaireDir+"/"+filename+".tsv", true); //append true
			CSVWriter writer = new CSVWriter(mFileWriter, '\t', 
					CSVWriter.NO_QUOTE_CHARACTER);
			int count = 0;
			for (int k = 0; k < results.size(); k++) {
				String[] entry = {results.get(k).getLabelalternative(), results.get(k).getLabelstandard(),
							results.get(k).getUri()};
				writer.writeNext(entry);
				count++;				
			}			
			System.out.println("Total count places: "+count);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}	
}
