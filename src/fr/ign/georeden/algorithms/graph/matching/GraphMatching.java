package fr.ign.georeden.algorithms.graph.matching;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.conn.HttpHostConnectException;
import org.apache.jena.atlas.web.HttpException;
import org.apache.jena.riot.RiotException;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import com.hp.hpl.jena.query.QueryParseException;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import fr.ign.georeden.algorithms.string.StringComparisonDamLev;
import fr.ign.georeden.utils.RDFUtil;
import fr.ign.georeden.utils.XMLUtil;

public class GraphMatching {

	private static Logger logger = Logger.getLogger(GraphMatching.class);

	public static void nodeSelection() {
		float threshold = 0.7f;
		logger.info("Chargement du TEI");
		Document teiSource = XMLUtil.createDocumentFromFile("temp6.xml");
		HashMap<String, String> toponymsTEI = new HashMap<>();
		logger.info("Récupération des toponymes du TEI");
		try {
			List<QuerySolution> qSolutionsTEI = RDFUtil.getQuerySelectResults(teiSource,
					"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
							+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"
							+ "PREFIX prop-fr: <http://fr.dbpedia.org/property/>"
							+ "PREFIX foaf: <http://xmlns.com/foaf/0.1/>"
							+ "PREFIX dbpedia-owl: <http://dbpedia.org/ontology/>" + "" + "SELECT DISTINCT * WHERE {"
							+ "  ?s rdfs:label ?label ." + "}");
			for (QuerySolution querySolution : qSolutionsTEI) {
				toponymsTEI.put(RDFUtil.getURIOrLexicalForm(querySolution, "s"), RDFUtil.getURIOrLexicalForm(querySolution, "label"));
			}
		} catch (QueryParseException | HttpHostConnectException | RiotException | MalformedURLException
				| HttpException e) {
			logger.error(e);
		}
		logger.info(toponymsTEI.size() + " résultats");
		Model test = ModelFactory.createDefaultModel();
		test = test.read("temp6.xml", "RDF/XML");
		

		final List<Candidate> qSolutionsKB = getCandidatesFromKB();

		logger.info(qSolutionsKB.size() + " résultats");

		logger.info("Sélection des candidats");
		HashMap<String, Set<String>> result = new HashMap<>();
		final AtomicInteger count = new AtomicInteger();
		final int total = toponymsTEI.size();
		StringComparisonDamLev sc = new StringComparisonDamLev();
		toponymsTEI.entrySet().parallelStream().forEach(set -> {
			Set<String> candidateResources = new HashSet<>();
			qSolutionsKB.parallelStream().forEach(querySolutionKB -> {
				float score = sc.computeSimilarity(set.getValue(), querySolutionKB.getName());
				if (score >= threshold) 
					candidateResources.add(querySolutionKB.getResource());
				else {
					score = sc.computeSimilarity(set.getValue(), querySolutionKB.getLabel());
					if (score >= threshold) 
						candidateResources.add(querySolutionKB.getResource());
				}
			});
			result.put(set.getKey(), candidateResources);
			logger.info(count.getAndIncrement() + " / " + total);
		});

		logger.info("Affichage des résultats");
		for (Iterator<String> iterator = result.keySet().iterator(); iterator.hasNext();) {
			String key = iterator.next();
			Set<String> candidates = result.get(key);
			String s = key + " (" + candidates.size() + ") : ";
			for (String string : candidates) {
				s += string + ", ";
			}
			logger.info(s);
		}
		for (Iterator<String> iterator = result.keySet().iterator(); iterator.hasNext();) {
			String key = iterator.next();
			Set<String> candidates = result.get(key);
			if (candidates.size() == 0)
				logger.info(key);
		}
	}

	static List<Candidate> getCandidatesFromKB() {
		List<QuerySolution> qSolutionsKB = new ArrayList<>();
		List<Candidate> result = new ArrayList<>();

		logger.info("Chargement du KB");
		Model kbSource = ModelFactory.createDefaultModel();
		kbSource = kbSource.read("D:\\\\dbpedia\\\\dbpedia_all.n3");
		logger.info("Récupérations des candidats de la KB");

		try {
			qSolutionsKB.addAll(RDFUtil.getQuerySelectResults(kbSource,
					"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" + 
					"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>" + 
					"PREFIX prop-fr: <http://fr.dbpedia.org/property/>" + 
					"PREFIX foaf: <http://xmlns.com/foaf/0.1/>" + 
					"PREFIX dbpedia-owl: <http://dbpedia.org/ontology/>" + 
					"" + 
					"SELECT DISTINCT * WHERE {" + 
					"  OPTIONAL { ?s foaf:name ?name . }" + 
					"  OPTIONAL { ?s rdfs:label ?label . }" + 
					"}"));
		} catch (QueryParseException | HttpHostConnectException | MalformedURLException | HttpException e) {
			logger.error(e);
		}
		for (QuerySolution querySolution : qSolutionsKB) {
			result.add(new Candidate(
					RDFUtil.getURIOrLexicalForm(querySolution, "s"), 
					RDFUtil.getURIOrLexicalForm(querySolution, "label"),
					RDFUtil.getURIOrLexicalForm(querySolution, "name")));
		}
		return result;
	}
}


