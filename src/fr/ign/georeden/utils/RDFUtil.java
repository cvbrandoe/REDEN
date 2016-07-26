package fr.ign.georeden.utils;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.http.conn.HttpHostConnectException;
import org.apache.jena.atlas.web.HttpException;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QueryParseException;
//import org.apache.jena.query.QuerySolution;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import org.apache.jena.riot.RiotException;
import org.apache.jena.riot.WebContent;
import org.w3c.dom.Document;

import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;
import com.hp.hpl.jena.sparql.engine.http.QueryExceptionHTTP;

public class RDFUtil {

	private RDFUtil() {
	}

	/**
	 * Gets the query select results.
	 *
	 * @param serviceURL
	 *            the service url
	 * @param queryString
	 *            the query string
	 * @return the query select results
	 * @throws QueryParseException
	 *             the query parse exception
	 * @throws MalformedURLException
	 *             the malformed url exception
	 * @throws HttpHostConnectException
	 *             the http host connect exception
	 */
	public static List<QuerySolution> getQuerySelectResults(String serviceURL, String queryString)
			throws QueryParseException, MalformedURLException, HttpHostConnectException, HttpException {
		List<QuerySolution> resultList = new ArrayList<>();
		if (serviceURL == null || queryString == null)
			return resultList;
		if (serviceURL.isEmpty() || queryString.isEmpty())
			return resultList;
		Query query = QueryFactory.create(queryString);
		try (QueryExecution qexec = QueryExecutionFactory.sparqlService(serviceURL, query)) {
			ResultSet results = qexec.execSelect();
			while (results.hasNext()) {
				QuerySolution querySolution = results.nextSolution();
				resultList.add(querySolution);
			}
		}
		return resultList;
	}

	/**
	 * Gets the query construct (in N3 format).
	 *
	 * @param serviceURL
	 *            the service url
	 * @param queryString
	 *            the query string
	 * @param outputStream
	 *            the output stream
	 * @return the query construct
	 * @throws QueryParseException
	 *             the query parse exception
	 * @throws MalformedURLException
	 *             the malformed url exception
	 * @throws HttpHostConnectException
	 *             the http host connect exception
	 */
	public static Model getQueryConstruct(String serviceURL, String queryString, OutputStream outputStream)
			throws QueryParseException, MalformedURLException, HttpHostConnectException, HttpException, RiotException {
		Model results = null;
		if (serviceURL == null || queryString == null)
			return results;
		if (serviceURL.isEmpty() || queryString.isEmpty())
			return results;
		Query query = QueryFactory.create(queryString);
		try (QueryEngineHTTP queryEngineHTTP = (QueryEngineHTTP) QueryExecutionFactory.sparqlService(serviceURL,
				query)) {
			queryEngineHTTP.setModelContentType(WebContent.contentTypeRDFXML); // cast
																				// effectué
																				// à
																				// cause
																				// d'un
																				// pb
																				// de
																				// validité
																				// d'une
																				// ressource
			// ERROR riot:84 - [line: 78437, col: 20] Failed to find a prefix
			// name or keyword: ’(8217;0x2019)
			results = queryEngineHTTP.execConstruct();
			return results.write(outputStream, WebContent.contentTypeN3); // TURTLE
																			// N3
		} catch (QueryExceptionHTTP e) {
			throw new HttpException(e.getMessage());
		}
	}
	
	public static Model getQueryConstruct(Model documentModel, String queryString, OutputStream outputStream)
			throws QueryParseException, MalformedURLException, HttpHostConnectException, HttpException, RiotException {
		if (queryString == null || queryString.isEmpty())
			return null;
		Model results = null;
		Query query = QueryFactory.create(queryString);
		try (QueryExecution queryExecution = QueryExecutionFactory.create(query, documentModel)) {
			results = queryExecution.execConstruct();
			if (outputStream == null)
				return results;
			return results.write(outputStream, "TURTLE");
		} catch (QueryExceptionHTTP e) {
			throw new HttpException(e.getMessage());
		}
	}

	public static Model getQueryConstruct(Document rdfXml, String queryString, OutputStream outputStream)
			throws QueryParseException, MalformedURLException, HttpHostConnectException, HttpException, RiotException {
		Model results = null;
		if (rdfXml == null || queryString == null)
			return results;
		if (queryString.isEmpty())
			return results;
		Model documentModel = ModelFactory.createDefaultModel();
		String modelText = XMLUtil.xmlDocumentContentToString(rdfXml);
		documentModel.read(new ByteArrayInputStream(modelText.getBytes()), "RDF/XML");
		Query query = QueryFactory.create(queryString);
		try (QueryExecution queryExecution = QueryExecutionFactory.create(query, documentModel)) {
			results = queryExecution.execConstruct();
			if (outputStream == null)
				return results;
			return results.write(outputStream, "TURTLE");
		} catch (QueryExceptionHTTP e) {
			throw new HttpException(e.getMessage());
		}
	}
	
	public static Model getModel(Document rdfXml) {
		Model documentModel = ModelFactory.createDefaultModel();
		String modelText = XMLUtil.xmlDocumentContentToString(rdfXml);
		return documentModel.read(new ByteArrayInputStream(modelText.getBytes()), "RDF/XML");
	}

	public static List<QuerySolution> getQuerySelectResults(Document rdfXml, String queryString)
			throws QueryParseException, MalformedURLException, HttpHostConnectException, HttpException, RiotException {
		List<QuerySolution> resultList = new ArrayList<>();
		if (rdfXml == null || queryString == null)
			return resultList;
		if (queryString.isEmpty())
			return resultList;
		Model documentModel = ModelFactory.createDefaultModel();
		String modelText = XMLUtil.xmlDocumentContentToString(rdfXml);
		documentModel.read(new ByteArrayInputStream(modelText.getBytes()), "RDF/XML");
		Query query = QueryFactory.create(queryString);

		try (QueryExecution queryExecution = QueryExecutionFactory.create(query, documentModel)) {
			ResultSet results = queryExecution.execSelect();
			while (results.hasNext()) {
				QuerySolution querySolution = results.nextSolution();
				resultList.add(querySolution);
			}
		} catch (QueryExceptionHTTP e) {
			throw new HttpException(e.getMessage());
		}
		return resultList;
	}
	
	public static List<QuerySolution> getQuerySelectResults(Model documentModel, String queryString)
			throws QueryParseException, MalformedURLException, HttpHostConnectException, HttpException, RiotException {
		List<QuerySolution> resultList = new ArrayList<>();
		if (documentModel == null || queryString == null)
			return resultList;
		if (queryString.isEmpty())
			return resultList;
		Query query = QueryFactory.create(queryString);

		try (QueryExecution queryExecution = QueryExecutionFactory.create(query, documentModel)) {
			ResultSet results = queryExecution.execSelect();
			while (results.hasNext()) {
				QuerySolution querySolution = results.nextSolution();
				resultList.add(querySolution);
			}
		} catch (QueryExceptionHTTP e) {
			throw new HttpException(e.getMessage());
		}
		return resultList;
	}

	/**
	 * Gets the URI if the variable is a resource or the lexical form if it's a
	 * literal.
	 * 
	 * @param querySolution
	 * @param variableName
	 * @return
	 */
	public static String getURIOrLexicalForm(QuerySolution querySolution, String variableName) {
		String uriOrLexicalForm = null;
		RDFNode n = querySolution.get(variableName);
		if (n != null) {
			if (n.isLiteral())
				uriOrLexicalForm = ((Literal) n).getLexicalForm();
			if (n.isResource()) {
				Resource r = (Resource) n;
				if (!r.isAnon()) {
					String uri = r.getURI();
					uriOrLexicalForm = uri;
				}
			}
		}
		return uriOrLexicalForm;
	}

	/**
	 * Return the HashMap of the current solution.
	 * 
	 * @param querySolution
	 * @return
	 */
	public static HashMap<String, String> getURIOrLexicalFormList(QuerySolution querySolution) {
		Iterator<String> iterator = querySolution.varNames();
		HashMap<String, String> result = new HashMap<>();
		while (iterator.hasNext()) {
			String varName = iterator.next();
			RDFNode n = querySolution.get(varName);
			String uriOrLexicalForm = null;
			if (n.isLiteral())
				uriOrLexicalForm = ((Literal) n).getLexicalForm();
			if (n.isResource()) {
				Resource r = (Resource) n;
				if (!r.isAnon()) {
					String uri = r.getURI();
					uriOrLexicalForm = uri;
				}
			}
			result.put(varName, uriOrLexicalForm);
		}

		return result;
	}

}
