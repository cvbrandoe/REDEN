package fr.lip6.ldcrawler.query;

import java.util.List;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFactory;

import fr.lip6.ldcrawler.domainparam.DomainExtent;

/**
 * Class for querying a particular source. New queries per source must extend this class.
 * @author @author Brando & Frontini - Labex OBVIL - Université Paris-Sorbonne - UPMC LIP6
 */
public class QuerySource implements QuerySourceInterface {

	/**
	 * Prepare the SPARQL statement.
	 * Children must implement their own queries.
	 * @param domain configuration
	 * @param firstleter, optional filtering for queries
	 * @return
	 */
	public Query formulateSPARQLQuery(List<DomainExtent> domainParams, String firstleter) {
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
			String timeout) {
		
		try {
			// wait 20 seconds for every query
			Thread.sleep(10000);
			try (QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlendpoint, query)) {
			      ResultSet results = qexec.execSelect() ;
			      results = ResultSetFactory.copyResults(results) ;
			      qexec.close();
			      return results ;    // Passes the result set out of the try-resources			     
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Process and write results.
	 * Children must implement their own result processing.
	 * @param res, query results
	 * @param outDictionnaireDir, name of the folder where to write the dictionary file
	 * @param prefixDictionnaireFile, prefix of the dico files
	 * @param letter, optional parameter for large repos
	 */
	
	public void processResults(ResultSet res, String outDictionnaireDir, String letter) {
	}

}
