package fr.lip6.reden.ldextractor;

import java.util.List;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;

/**
 * Class for querying a particular source. New queries per source must extend
 * this class.
 * 
 * @author @author Brando & Frontini
 */
public class QuerySource implements QuerySourceInterface {

	/**
	 * Prepare the SPARQL statement. Children must implement their own queries.
	 * 
	 * @param domain
	 *            configuration
	 * @param firstleter,
	 *            optional filtering for queries
	 * @return
	 */
	public Query formulateSPARQLQuery(List<TopicExtent> domainParams, String firstleter, String outDictionnaireDir) {
		return null;
	}

	/**
	 * Execute query in SPARQL endpoint. 
	 * Same methods for all children.
	 * @param query, query to execute
	 * @param sparqlendpoint, URL of the SPARQL endpoint
	 * @param timeout, query timeout
	 * @return the result
	 */
	public ResultSet executeQuery(Query query, String sparqlendpoint,
			String timeout, String outDictionnaireDir, String letter) {
		
		try {
			// wait 20 seconds for every query
			Thread.sleep(10000); 
			try {
				QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlendpoint, query);
				ResultSet results = qexec.execSelect() ;
			      results = ResultSetFactory.copyResults(results) ;
			      qexec.close();
			      return results ;    // Passes the result set out of the try-resources			     
			} catch (Exception e){
				System.err.println("error in sparql query execution");
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Process and write results. Children must implement their own result
	 * processing.
	 * 
	 * @param res,
	 *            query results
	 * @param outDictionnaireDir,
	 *            name of the folder where to write the dictionary file
	 * @param prefixDictionnaireFile,
	 *            prefix of the dico files
	 * @param letter,
	 *            optional parameter for large repos
	 */

	public void processResults(ResultSet res, String outDictionnaireDir, String letter,
			List<TopicExtent> domainParams) {
	}

}
