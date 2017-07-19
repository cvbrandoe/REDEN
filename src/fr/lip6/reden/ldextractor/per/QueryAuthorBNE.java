package fr.lip6.reden.ldextractor.per;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.Logger;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import com.opencsv.CSVWriter;

import fr.lip6.reden.ldextractor.QuerySource;
import fr.lip6.reden.ldextractor.QuerySourceInterface;
import fr.lip6.reden.ldextractor.TemporalExtent;
import fr.lip6.reden.ldextractor.TopicExtent;

/**
 * This class queries the authors catalog in the BNE SPARQL end point.
 * 
 * @author Brando & Frontini
 */
public class QueryAuthorBNE extends QuerySource implements QuerySourceInterface {

private static Logger logger = Logger.getLogger(QueryAuthorBNE.class);
	
	/**
	 * Mandatory fields
	 */
	public String SPARQL_END_POINT = "http://datos.bne.es/sparql";
	
	public Integer TIMEOUT = 200000;
	
	public Boolean LARGE_REPO = true;
	
	public String prefixDictionnaireFile = "authorBNE";
	
	/**
	 * Default constructor.
	 */
	public QueryAuthorBNE () {
		super();
	}
	
	/**
	 * Formulate a query which is decomposed in several sub-queries because of size of the BnF repo. 
	 *  
	 */
	public Query formulateSPARQLQuery(List<TopicExtent> domainParams, 
			String firstLetter, String outDictionnaireDir) {
		
		File fexists = new File(outDictionnaireDir+"/"+prefixDictionnaireFile+firstLetter+".tsv");
		
		if (fexists.exists() && fexists.length() > 0) {
			System.out.println("entering BNE: skip, file exists - "+
					outDictionnaireDir+"/"+prefixDictionnaireFile+firstLetter+".tsv");
			return null; //skip processing
		} else {
			logger.info("entering BNE: formulateSPARQLQuery");			
			String filterRegex = "";
			if (firstLetter.equalsIgnoreCase("other")) {
				filterRegex = "FILTER (!regex(STR(?acceptedForm), '^a|^b|^c|^d|^e|^f|^g|^h|^i|^j|^k|^l|^m|^n|^o|^p|^q|^r|^s|^t|^u|^v|^w|^x|^y|^z', 'i')) . ";			
			} else {
				filterRegex = "FILTER (regex(STR(?acceptedForm), '^"+firstLetter+"', 'i')) . ";			
			}
									
			String queryString = "PREFIX ns2: <http://datos.bne.es/def/> "
					+ "PREFIX owl: <http://www.w3.org/2002/07/owl#> "
					+ "SELECT distinct ?author ?acceptedForm ?rejectedForm ?period ?ref "
					+ "where { "
					+ "?author ns2:OP5001 ?work . "					
					+ "?author ns2:P5001 ?acceptedForm . "
					+ filterRegex
					+ "OPTIONAL {?author ns2:P5012 ?rejectedForm } . "
					+ "OPTIONAL {?author owl:sameAs ?ref } . "					
					+ "OPTIONAL { ?author ns2:P5002 ?period } "
					+ "} ";
			
			Query query = QueryFactory.create(queryString);
			logger.info("query: " + query.toString());
			logger.info("exiting BNF: formulateSPARQLQuery");
			return query;
		}		
	}
	
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
			System.out.println("entering BNE: skip, file exists - "+outDictionnaireDir+"/"+prefixDictionnaireFile+letter+".tsv");
			return; //file exists, skip processing
		} else {
			logger.info("entering BNE: processResults");
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
				
				Map<String, AuthorBNE> authors = new HashMap<String, AuthorBNE>();
				
				//get date filter specified by user
				int lesser = 0, greater = 0;
				for (TopicExtent d : domainParams) {
					if (d instanceof TemporalExtent) {
						TemporalExtent tem = (TemporalExtent) d;
						if (tem.getLesserThan() != null) {
							SimpleDateFormat df = new SimpleDateFormat("yyyy");
							String year = df.format(tem.getLesserThan());
							lesser = Integer.parseInt(year);
						}
						if (tem.getGreaterThan() != null) {
							SimpleDateFormat df = new SimpleDateFormat("yyyy");
							String year = df.format(tem.getGreaterThan());
							greater = Integer.parseInt(year);
						}
					} 
				}
				
				while (res.hasNext()) {
					QuerySolution sol = res.next();
					
					//filtering birth date ?period
					/*Boolean ignoreL = false, ignoreG = false; //TODO FAIRE CECI EN SPARQL!!!
					if (sol.get("period") != null) {
						if (sol.getLiteral("period").getString().contains("-")) {
							String birth = sol.getLiteral("period").getString().split("-")[0].trim();
							String death = sol.getLiteral("period").getString().split("-")[1].trim();
							if (NumberUtils.isNumber(birth) && NumberUtils.isNumber(death)) {
								if (Integer.parseInt(birth) < 0 && Integer.parseInt(birth) < lesser) {
										ignoreL = true;
								}
								if (Integer.parseInt(death) < 0 && Integer.parseInt(death) > greater) {
										ignoreG = true;
								}
							}							
						}
					}	*/				
					
					//if (ignoreL) {
						//update sameAs links and alternative names for this author
						if (authors.get(sol.get("author").toString()) != null) { 
							AuthorBNE a = authors.get(sol.get("author").toString());
							if (sol.get("ref") != null) {
								if (!a.getRef().contains(sol.get("ref").toString())) {
									a.getRef().add(sol.get("ref").toString());
								}
							}
							
							if (sol.get("rejectedForm") != null) {
								String val = sol.getLiteral("rejectedForm").getLexicalForm();
								if (val.contains("("))
									val = val.substring(0, val.indexOf("(")).trim();
								if (!a.getRejectedForms().contains(val))
									a.getRejectedForms().add(val);
							}
							
						} else {
							AuthorBNE a = new AuthorBNE();
							a.setUri(sol.get("author").toString());
							if (sol.get("acceptedForm") != null)
								a.setLastname(sol.get("acceptedForm").toString());
							else
								a.setLastname("-");					
							
							if (sol.get("ref") != null) {
								if (!a.getRef().contains(sol.get("ref").toString())) {
									a.getRef().add(sol.get("ref").toString());
								}
							}
							
							if (sol.get("rejectedForm") != null) {
								String val = sol.getLiteral("rejectedForm").getLexicalForm();
								if (val.contains("("))
									val = val.substring(0, val.indexOf("(")).trim();
								if (!a.getRejectedForms().contains(val))
									a.getRejectedForms().add(val);							
							}
							//more rejected forms
							//a.getRejectedForms().addAll(a.makeAliases());						
							authors.put(a.getUri(), a);
						}
					//}
				}				
				System.out.println("count of authors: "+authors.size());
				
				for (String uri : authors.keySet()) {
					writeAuthorToFile(writer, authors.get(uri));
				}
				writer.close();
				logger.info("exiting BNE: processResults");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void writeAuthorToFile(CSVWriter writer, AuthorBNE author) {
		
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