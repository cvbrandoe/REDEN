package fr.lip6.reden.ldextractor.loc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;

import com.opencsv.CSVWriter;

import fr.lip6.reden.ldextractor.TopicExtent;

/**
 * This class queries the places in the WikiData SPARQL end point.
 * 
 * @author Brando & Frontini
 */
public class QueryPlaceWikiData {

	private static Logger logger = Logger.getLogger(QueryPlaceWikiData.class);
	
	/**
	 * Mandatory fields
	 */
	public String SPARQL_END_POINT = "https://query.wikidata.org/sparql";
	
	public Integer TIMEOUT = 200000;
	
	public Boolean LARGE_REPO = true;
	
	public String prefixDictionnaireFile = "placeWikidata";
	
	/**
	 * Default constructor.
	 */
	public QueryPlaceWikiData () {
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
			System.out.println("entering Wikidata: skip, file exists - "+
					outDictionnaireDir+"/"+prefixDictionnaireFile+firstLetter+".tsv");
			return null; //skip processing
		} else {
			logger.info("entering WikiData: formulateSPARQLQuery");
			String filterRegex = "";
			if (firstLetter.equalsIgnoreCase("other")) {
				filterRegex = " FILTER (!regex(?itemLabel, '^a|^b|^c|^d|^e|^f|^g|^h|^i|^j|^k|^l|^m|^n|^o|^p|^q|^r|^s|^t|^u|^v|^w|^x|^y|^z', 'i')) . ";			
			} else {
				filterRegex = " FILTER regex(?itemLabel, '^"+firstLetter+"', 'i') . ";			
			}
			String queryString = "PREFIX wdt: <http://www.wikidata.org/prop/direct/>"+
					" PREFIX skos: <http://www.w3.org/2004/02/skos/core#>"
					+" PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"
					+ " SELECT ?item ?itemLabel ?altlabel WHERE {" +
	                "  ?item wdt:P625 ?coord . " +
	                "  ?item rdfs:label ?itemLabel ." +
	                "  filter(langMatches(lang(?itemLabel),'FR')) " +
	                filterRegex +
	                " OPTIONAL { ?item skos:altLabel ?altlabel . "
	                + "filter(langMatches(lang(?altlabel),'FR')) } . " +
	                "} ";

			logger.info(queryString);
			logger.info("exiting WikiData: formulateSPARQLQueryString");
			return queryString;			
		}		
	}
	
	public ResultSet executeQuery(String queryString, String timeout, String sparqlendpoint, 
			String outDictionnaireDir, String letter) {
		 
		try {
			Thread.sleep(20000); //20 seconds
			File fexists = new File(outDictionnaireDir+"/"+prefixDictionnaireFile+letter+".tsv");
			if (fexists.exists() && fexists.length() > 0) {
				System.out.println("entering WikiData: skip, file exists - "+outDictionnaireDir+"/"+prefixDictionnaireFile+letter+".tsv");
				return null; //file exists, skip processing
			} else {
				logger.info("entering WikiData: executeQuery");			
				QueryExecution qexec = QueryExecutionFactory.sparqlService(SPARQL_END_POINT, queryString);
		        try {
		        	ResultSet results = qexec.execSelect();
		            logger.info("exiting WikiData: executeQuery");
		            
		            logger.info("entering WikiData: processResults");
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
					
					while (results.hasNext()) {
						QuerySolution sol = results.next();
						PlaceEntry tri = new PlaceEntry();
						tri.setLabelstandard(sol.getLiteral("itemLabel").getLexicalForm());
						if (sol.get("altlabel") != null) {
							tri.setLabelalternative(sol.getLiteral("altlabel").getLexicalForm());
						} else {
							tri.setLabelalternative(sol.getLiteral("itemLabel").getLexicalForm());
						}
						tri.setUri(sol.getResource("item").getURI());
						places.add(tri);
						
					}
					if (places != null) {
						writeToFile(places, prefixDictionnaireFile, outDictionnaireDir);
					}
					logger.info("exiting WikiData: processResults");
					
		        } catch (Exception ex) {
		            System.out.println(ex.getMessage());
		        } finally {
		            qexec.close();
		        }
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} 
		return null;
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
