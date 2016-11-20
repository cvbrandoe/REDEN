package fr.lip6.reden.ldextractor.loc;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.opencsv.CSVWriter;

import fr.lip6.reden.ldextractor.QuerySource;
import fr.lip6.reden.ldextractor.QuerySourceInterface;
import fr.lip6.reden.ldextractor.TopicExtent;

/**
 * This class queries the authors catalog in the BnF SPARQL end point.
 * 
 * @author Brando & Frontini
 */
public class QueryPlaceBNF extends QuerySource implements QuerySourceInterface {

	private static Logger logger = Logger.getLogger(QueryPlaceBNF.class);
	
	/**
	 * Mandatory fields
	 */
	public String SPARQL_END_POINT = "http://data.bnf.fr/sparql";
	
	public Integer TIMEOUT = 200000;
	
	public Boolean LARGE_REPO = true;
	
	public String prefixDictionnaireFile = "placeBNF";
	
	/**
	 * Default constructor.
	 */
	public QueryPlaceBNF () {
		super();
	}
	
	/**
	 * Formulate a query which is decomposed in several sub-queries because of size of the BnF repo. 
	 *  
	 */
	@Override
	public Query formulateSPARQLQuery(List<TopicExtent> domainParams, 
			String firstLetter, String outDictionnaireDir) {
		
		File fexists = new File(outDictionnaireDir+"/"+prefixDictionnaireFile+firstLetter+".tsv");
		
		if (fexists.exists() && fexists.length() > 0) {
			System.out.println("entering BNF: skip, file exists - "+
					outDictionnaireDir+"/"+prefixDictionnaireFile+firstLetter+".tsv");
			return null; //skip processing
		} else {
			logger.info("entering BNF: formulateSPARQLQuery");
			String filterRegex = "";
			if (firstLetter.equalsIgnoreCase("other")) {
				filterRegex = "FILTER (!regex(STR(?pref), '^a|^b|^c|^d|^e|^f|^g|^h|^i|^j|^k|^l|^m|^n|^o|^p|^q|^r|^s|^t|^u|^v|^w|^x|^y|^z', 'i')) . ";			
			} else {
				filterRegex = "FILTER (regex(STR(?pref), '^"+firstLetter+"', 'i')) . ";			
			}
			
			String queryString = "PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>"
					+ " PREFIX foaf: <http://xmlns.com/foaf/0.1/>"
					+ " PREFIX skos: <http://www.w3.org/2004/02/skos/core#> "
					+ "SELECT ?alt ?pref ?spatial WHERE { "
					+ " ?concept foaf:focus ?spatial; skos:prefLabel ?pref . "
					+ filterRegex
					+ " OPTIONAL { ?concept skos:altLabel ?alt } . "
					+ " ?spatial a <http://www.w3.org/2003/01/geo/wgs84_pos#SpatialThing> . "
					+ " }";
			Query query = QueryFactory.create(queryString);
			logger.info("query: " + query.toString());
			logger.info("exiting BNF: formulateSPARQLQuery");
			return query;
			
		}		
	}
	
	@Override
	public ResultSet executeQuery(Query query, String timeout, String sparqlendpoint, 
			String outDictionnaireDir, String letter) {
		File fexists = new File(outDictionnaireDir+"/"+prefixDictionnaireFile+letter+".tsv");
		if (fexists.exists() && fexists.length() > 0) {
			System.out.println("entering BNF: skip, file exists - "+outDictionnaireDir+"/"+prefixDictionnaireFile+letter+".tsv");
			return null; //file exists, skip processing
		} else {
			logger.info("entering BNF: executeQuery");
			logger.info("exiting BNF: executeQuery");
			return super.executeQuery(query, SPARQL_END_POINT, timeout, 
					outDictionnaireDir, letter);
		}
	}
	
	/**
	 * Handling of the results specific to the BnF model.
	 */
	@Override
	public void processResults(ResultSet res, String outDictionnaireDir, String letter,  List<TopicExtent> domainParams) {
		
		File fexists = new File(outDictionnaireDir+"/"+prefixDictionnaireFile+letter+".tsv");
	
		if (fexists.exists() && fexists.length() > 0) {
			System.out.println("entering BNF: skip, file exists - "+outDictionnaireDir+"/"+prefixDictionnaireFile+letter+".tsv");
			return; //file exists, skip processing
		} else {
			logger.info("entering BNF: processResults");
			if (letter != null) {
				prefixDictionnaireFile += letter;
			}
			if (!new File(outDictionnaireDir).exists()) {
				new File(outDictionnaireDir).mkdir();			
			}
			if (!new File(outDictionnaireDir).exists()) {
				new File(outDictionnaireDir).mkdir();
			}
			
			List<PlaceDBpediaFr> places = new ArrayList<PlaceDBpediaFr>();
			
			while (res.hasNext()) {
				QuerySolution sol = res.next();
				//redirect pages
				PlaceDBpediaFr tri = new PlaceDBpediaFr();
					tri.setLabelstandard(sol.getLiteral("pref").getLexicalForm());
					tri.setUri(sol.getResource("spatial").getURI());
					if (sol.getLiteral("alt") != null ) {
						tri.setLabelalternative(sol.getLiteral("alt").getLexicalForm());
					} else {
						tri.setLabelalternative(sol.getLiteral("pref").getLexicalForm());	
					}
				places.add(tri);
				
			}
			if (places != null) {
				writeToFile(places, prefixDictionnaireFile, outDictionnaireDir);
			}
			logger.info("exiting BNF: processResults");
		}
	}

	/**
	 * It writes the ouput TSV file containing, for each individual:
	 * <alternative_name>	<nomalized_name>	<URI>
	 * @param results, the result of the sparql query
	 * @param filename, the name of the TSV file
	 * @param outDictionnaireDir, the name of the folder where the TSV file will be stored
	 */
	public static void writeToFile(List<PlaceDBpediaFr> results, String filename, String outDictionnaireDir) {
		try {
			FileWriter mFileWriter = new FileWriter(outDictionnaireDir+"/"+filename+".tsv", true); //append true
			CSVWriter writer = new CSVWriter(mFileWriter, '\t', 
					CSVWriter.NO_QUOTE_CHARACTER);
			int count = 0;
			for (int k = 0; k < results.size(); k++) {
				if (results.get(k).getLabelalternative().contains("(")) {
					String[] entry = { results.get(k).getLabelalternative().substring(0, results.get(k).getLabelalternative().indexOf("(")).trim(), results.get(k).getLabelstandard(), 
					results.get(k).getUri().replace("#spatialThing", "")};
					writer.writeNext(entry);
					count++;
				} else {
					String[] entry = { results.get(k).getLabelalternative(), results.get(k).getLabelstandard(),
							results.get(k).getUri().replace("#spatialThing", "")};
					writer.writeNext(entry);
					count++;
				}
			}			
			System.out.println("Total count places (entities and their alternative names): "+count);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}	
}
