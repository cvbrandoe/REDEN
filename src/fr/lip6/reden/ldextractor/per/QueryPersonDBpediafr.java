package fr.lip6.reden.ldextractor.per;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import com.opencsv.CSVWriter;

import fr.lip6.reden.ldextractor.QuerySource;
import fr.lip6.reden.ldextractor.QuerySourceInterface;
import fr.lip6.reden.ldextractor.TopicExtent;

/**
 * 
 * @author Brando
 * Query authors in DBpedia fr
 *
 */
public class QueryPersonDBpediafr extends QuerySource implements QuerySourceInterface {

private static Logger logger = Logger.getLogger(QueryPersonDBpediafr.class);
	
	/**
	 * Mandatory fields
	 */
	public String SPARQL_END_POINT = "http://fr.dbpedia.org/sparql";
	
	public Integer TIMEOUT = 200000;
	
	public Boolean LARGE_REPO = true;
	
	public String prefixDictionnaireFile = "authorDBpedia";
	
	/**
	 * Default constructor.
	 */
	public QueryPersonDBpediafr () {
		super();
	}
	
	/**
	 * Formulate a query which is decomposed in several sub-queries because of size of the DBpedia fr repo. 
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
			logger.info("entering DBpedia: formulateSPARQLQuery");
			
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
					+ "select distinct ?val ?labelfr ?labelred ?otherLinks where { "					
					+ " ?val rdf:type db-owl:Person ." //TODO ? Artist or Writer ?
					+ " ?val rdfs:label ?labelfr . "
					+ " filter(langMatches(lang(?labelfr),'FR')) ."
					+ filterRegex
					+ " OPTIONAL {?red db-owl:wikiPageRedirects ?val ."
					+ " ?red rdfs:label ?labelred ."
					+ " filter(langMatches(lang(?labelred),'FR')) } . "
					+ " OPTIONAL { ?val owl:sameAs ?otherLinks ."
					+ " FILTER regex(str(?otherLinks), '^http://dbpedia.org/', 'i')} }"
					/*+ " LIMIT 10000 OFFSET 20000" -- sometimes it's necessary */;
			// TODO eventually filter by date
			Query query = QueryFactory.create(queryString);
			logger.info("query: " + query.toString());
			logger.info("exiting DBpedia: formulateSPARQLQuery");
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
			logger.info("entering DBpedia: executeQuery");
			logger.info("exiting DBpedia: executeQuery");
			return super.executeQuery(query, SPARQL_END_POINT, timeout, 
					outDictionnaireDir, letter);
		}
	}
	
	/**
	 * Handling of the results specific to the AuthorDBpediaFr model.
	 */
	@Override
	public void processResults(ResultSet res, String outDictionnaireDir, String letter,  List<TopicExtent> domainParams) {
		
		File fexists = new File(outDictionnaireDir+"/"+prefixDictionnaireFile+letter+".tsv");
		
		if (fexists.exists() && fexists.length() > 0) {
			System.out.println("entering AuthorBDpediaFr: skip, file exists - "+outDictionnaireDir+"/"+prefixDictionnaireFile+letter+".tsv");
			return; //file exists, skip processing
		} else {
			logger.info("entering AuthorBDpediaFr: processResults");
			try {
				if (letter != null) {
					prefixDictionnaireFile += letter;
				}
				if (!new File(outDictionnaireDir).exists()) {
					new File(outDictionnaireDir).mkdir();			
				}
				if (!new File(outDictionnaireDir).exists()) {
					new File(outDictionnaireDir).mkdir();
				}
				CSVWriter writer = new CSVWriter(new FileWriter(outDictionnaireDir+"/"+prefixDictionnaireFile+".tsv"), 
						'\t', CSVWriter.NO_QUOTE_CHARACTER);
				
				Map<String, AuthorDBpediaFr> authors = new HashMap<String, AuthorDBpediaFr>();
				
				while (res.hasNext()) {
					QuerySolution sol = res.next();
					
					//update sameAs links and alternative names for this author
					if (authors.get(sol.get("val").toString()) != null) { 
						AuthorDBpediaFr a = authors.get(sol.get("val").toString());
						if (sol.get("otherLinks") != null) {
							if (!a.getRef().contains(sol.get("otherLinks").toString())) {
								a.getRef().add(sol.get("otherLinks").toString());
							}
						}
						 
						if (sol.get("labelred") != null) {
							String val = sol.getLiteral("labelred").getLexicalForm();
							if (val.contains("("))
								val = val.substring(0, val.indexOf("(")).trim();
							if (!a.getRejectedForms().contains(val))
								a.getRejectedForms().add(val);
						}
						
					} else {
						AuthorDBpediaFr a = new AuthorDBpediaFr();
						a.setUri(sol.get("val").toString());
						if (sol.get("labelfr") != null) {
							a.setLastname(sol.getLiteral("labelfr").getString());
							a.getRejectedForms().add(sol.getLiteral("labelfr").getString());
						} else
							a.setLastname("-");					
						
						if (sol.get("otherLinks") != null) {
							if (!a.getRef().contains(sol.get("otherLinks").toString())) {
								a.getRef().add(sol.get("otherLinks").toString());
							}
						}
						
						if (sol.get("labelred") != null) {
							String val = sol.getLiteral("labelred").getLexicalForm();
							if (val.contains("("))
								val = val.substring(0, val.indexOf("(")).trim();
							if (!a.getRejectedForms().contains(val))
								a.getRejectedForms().add(val);							
						}
						authors.put(a.getUri(), a);
					}							
				}
				System.out.println("count of authors: "+authors.size());
				
				for (String uri : authors.keySet()) {
					writeAuthorToFile(writer, authors.get(uri));
				}
				writer.close();
				logger.info("exiting AuthorDBpedia: processResults");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	private void writeAuthorToFile(CSVWriter writer, AuthorDBpediaFr author) {
		
		//build URIs
		StringBuilder commaSepValueBuilder = new StringBuilder();
		commaSepValueBuilder.append(author.getUri());
		for (String uris : author.getRef()){
			commaSepValueBuilder.append("\t"+uris);	     
		}
		for (String aliases : author.getRejectedForms()) {
			String[] entry = { aliases, author.getNormalisedName(), commaSepValueBuilder.toString()};
			writer.writeNext(entry);			
		}		
	}
}
