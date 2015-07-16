package fr.lip6.ldcrawler.query;

import java.util.List;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.ResultSet;

import fr.lip6.LDcrawler.domainParam.DomainExtent;

/**
 * Interface that must be implemented for querying a particular source.
 * @author Brando & Frontini - Labex OBVIL - Université Paris-Sorbonne - UPMC LIP6
 */
public interface QuerySourceInterface {
		
	/**
	 * Prepare the SPARQL query.
	 * @param domain configuration
	 * @param firstleter, optional filtering for queries
	 * @return the query
	 */
	Query formulateSPARQLQuery(List<DomainExtent> domainParams, String firstleter);
		
	/**
	 * Execute query in SPARQL endpoint.
	 * @param query, query to execute
	 * @param sparqlendpoint, URL of the SPARQL endpoint
	 * @param timeout, query timeout
	 * @return the result
	 */
	ResultSet executeQuery(Query query, String sparqlendpoint, String timeout);
	
	/**
	 * Process and write results.
	 * @param res, query results
	 * @param outDictionnaireDir, name of the folder where to write the dictionary file
	 * @param prefixDictionnaireFile, prefix of the dico files
	 * @param optional parameter in the presence of large repos
	 */
	void processResults(ResultSet res, String outDictionnaireDir, String letter);
	
}
