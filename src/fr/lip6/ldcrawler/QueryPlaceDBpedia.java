package fr.lip6.ldcrawler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.opencsv.CSVWriter;

/**
 * This class queries places in the DBoedia SPARQL end point.
 * 
 * @author Brando & Frontini - Labex OBVIL - Université Paris-Sorbonne - UPMC LIP6
 */
public class QueryPlaceDBpedia extends QuerySource implements QuerySourceInterface {

	/**
	 * Mandatory fields
	 */
	public String SPARQL_END_POINT = "http://fr.dbpedia.org/sparql";
	
	public Integer TIMEOUT = 200000;
	
	public Boolean LARGE_REPO = true;
	
	public String prefixDictionnaireFile = "placeDBpedia";
	
	/**
	 * Default constructor.
	 */
	public QueryPlaceDBpedia () {
		super();
	}
	
	/**
	 * Formulate a query which is decomposed in several sub-queries because of size of the BnF repo. 
	 *  
	 */
	@Override
	public Query formulateSPARQLQuery(List<TopicExtent> domainParams, String firstLetter, 
			String outDictionnaireDir) {
		
		File fexists = new File(outDictionnaireDir+prefixDictionnaireFile+firstLetter+".tsv");
		if (fexists.exists() && fexists.length() > 0) {
			System.out.println("entering DBpedia: formulateSPARQLQuery, skip, file exists");
			return null; //file exists, skip processing
		} else {
			System.out.println("entering DBpedia: formulateSPARQLQuery");
			//temporal information can be incorporated into the query in many ways
			for (TopicExtent d : domainParams) {
				if (d instanceof SpatialExtent) {
					//TODO include query statement (geo vocabulary) to handle spatial delimitation
					//concerning Lat,Lon of the place
				}
			}
			String filterRegex = "";
			if (firstLetter.equalsIgnoreCase("other")) {
				filterRegex = "FILTER (!regex(STR(?labelfr), '^a|^b|^c|^d|^e|^f|^g|^h|^i|^j|^k|^l|^m|^n|^o|^p|^q|^r|^s|^t|^u|^v|^w|^x|^y|^z', 'i')) . ";			
			} else {
				filterRegex = "FILTER (regex(STR(?labelfr), '^"+firstLetter+"', 'i')) . ";			
			}
			
			String queryString = "PREFIX db-owl: <http://dbpedia.org/ontology/> "
					+ "PREFIX owl: <http://www.w3.org/2002/07/owl#> "
					+ "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
					+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
					+ "PREFIX dbr: <http://dbpedia.org/resource/> "
					+ "PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#> "
					+ "PREFIX  prop-fr: <http://fr.dbpedia.org/property/> "
					+ "PREFIX dbpedia-owl: <http://dbpedia.org/ontology/> "
					+ "select distinct ?val ?labelfr ?labelred ?otherLinks where { "					
					/*
					 * Other predicates to filter places:
					 * prop-fr:longitude 
					 * prop-fr:latitude
					 * prop-fr:population
					 * rdf:type geo:SpatialThing
					 * dbpedia-owl:place
					 * prop-fr:lieu
					 * prop-fr:territoire
					 * prop-fr:région -- it doesn't work as we want
					 * prop-fr:régions -- idem
					 * dbpedia-owl:region -- idem
					*/
					+ " ?val rdf:type db-owl:Place ."
					+ " ?val prop-fr:régions ?reg ."
					+ " ?val rdfs:label ?labelfr . "
					+ " filter(langMatches(lang(?labelfr),'FR')) ."
					+ filterRegex
					+ " OPTIONAL {?red db-owl:wikiPageRedirects ?val ."
					+ " ?red rdfs:label ?labelred ."
					+ " filter(langMatches(lang(?labelred),'FR')) } . "
					+ " OPTIONAL { ?val owl:sameAs ?otherLinks ."
					+ " FILTER regex(str(?otherLinks), '^http://dbpedia.org/', 'i')} }"
					/*+ " LIMIT 10000 OFFSET 20000" -- sometimes it's necessary */;
			Query query = QueryFactory.create(queryString);
			System.out.println("query: " + query.toString());
			System.out.println("exiting DBpedia: formulateSPARQLQuery");
			return query;
		}
	}
	
	@Override
	public ResultSet executeQuery(Query query, String timeout, String sparqlendpoint, 
			String outDictionnaireDir, String letter) {
		File fexists = new File(outDictionnaireDir+prefixDictionnaireFile+letter+".tsv");
		if (fexists.exists() && fexists.length() > 0) {
			System.out.println("entering DBpedia: executeQuery, skip, file exists");
			return null; //file exists, skip processing
		} else {
			System.out.println("entering DBpedia: executeQuery");
			System.out.println("exiting DBpedia: executeQuery");
			return super.executeQuery(query, SPARQL_END_POINT, timeout, 
					outDictionnaireDir, letter);
		}
	}
	
	/**
	 * Handling of the results specific to the BnF model.
	 */
	@Override
	public void processResults(ResultSet res, String outDictionnaireDir, String letter) {
		File fexists = new File(outDictionnaireDir+prefixDictionnaireFile+letter+".tsv");
		if (fexists.exists() && fexists.length() > 0) {
			System.out.println("entering DBpedia: processResults, skip, file exists");
			return; //file exists, skip processing
		} else {
			System.out.println("entering DBpedia: processResults");
			List<Place> results = new ArrayList<Place>();
			if (letter != null) {
				prefixDictionnaireFile += letter;
			}
			if (!new File(outDictionnaireDir).exists()) {
				new File(outDictionnaireDir).mkdir();			
			}
			if (!new File(outDictionnaireDir+"/LOC").exists()) {
				new File(outDictionnaireDir+"/LOC").mkdir();
			}
			while (res.hasNext()) {
				QuerySolution sol = res.next();
				//redirect pages
				Boolean contains = false;
				for (int k = 0; k < results.size(); k++) {
					if (sol.getLiteral("labelfr").getLexicalForm() == results.get(k).getLabelstandard() 
							&& sol.getResource("val").getURI() == results.get(k).getUri()
							&& (sol.getLiteral("labelred") != null && sol.getLiteral("labelred").getLexicalForm().equals(results.get(k).getLabelalternative())
							|| (sol.getLiteral("labelred") == null && results.get(k).getLabelalternative().equals(" "))) )
						contains = true;
				}
				if (!contains) {
					Place tri = new Place();
					tri.setLabelstandard(sol.getLiteral("labelfr").getLexicalForm());
					tri.setUri(sol.getResource("val").getURI());
					if (sol.getLiteral("labelred") != null ) {
						tri.setLabelalternative(sol.getLiteral("labelred").getLexicalForm());
					} else {
						tri.setLabelalternative(" ");	
					}
					results.add(tri);
				}
				
				// main page
				contains = false;
				for (int k = 0; k < results.size(); k++) {
					if (sol.getLiteral("labelfr").getLexicalForm() == results.get(k).getLabelstandard() 
							&& sol.getResource("val").getURI() == results.get(k).getUri()
							&& sol.getLiteral("labelfr").getLexicalForm() == results.get(k).getLabelalternative())
						contains = true;
				}
				if (!contains) {
					Place tri2 = new Place();
					tri2.setLabelstandard(sol.getLiteral("labelfr").getLexicalForm());
					tri2.setUri(sol.getResource("val").getURI());
					tri2.setLabelalternative(sol.getLiteral("labelfr").getLexicalForm());
					results.add(tri2);
				}			
			}
			if (results != null) {
				writeToFile(results, prefixDictionnaireFile, outDictionnaireDir+"/LOC");
			}
			System.out.println("exiting DBpedia: processResults");
		}
	}
	
	/**
	 * It writes the ouput TSV file containing, for each individual:
	 * <alternative_name>	<nomalized_name>	<URI>
	 * @param results, the result of the sparql query
	 * @param filename, the name of the TSV file
	 * @param outDictionnaireDir, the name of the folder where the TSV file will be stored
	 */
	public static void writeToFile(List<Place> results, String filename, String outDictionnaireDir) {
		try {
			FileWriter mFileWriter = new FileWriter(outDictionnaireDir+"/"+filename+".tsv", true); //append true
			CSVWriter writer = new CSVWriter(mFileWriter, '\t', 
					CSVWriter.NO_QUOTE_CHARACTER);
			int count = 0;
			for (int k = 0; k < results.size(); k++) {
				if (results.get(k).getLabelalternative() != " ") {
					String[] entry = { results.get(k).getLabelalternative(), results.get(k).getLabelstandard(), 
							results.get(k).getUri()};
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

class Place {
	private String labelstandard;
	private String labelalternative;
	private String uri;
	public String getLabelstandard() {
		return labelstandard;
	}
	public void setLabelstandard(String labelstandard) {
		this.labelstandard = labelstandard;
	}
	public String getLabelalternative() {
		return labelalternative;
	}
	public void setLabelalternative(String labelalternative) {
		this.labelalternative = labelalternative;
	}
	public String getUri() {
		return uri;
	}
	public void setUri(String uri) {
		this.uri = uri;
	}
}
