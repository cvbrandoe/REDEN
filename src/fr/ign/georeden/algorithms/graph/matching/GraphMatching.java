package fr.ign.georeden.algorithms.graph.matching;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

/**
 * The Class GraphMatching.
 */
public class GraphMatching {

	private static Logger logger = Logger.getLogger(GraphMatching.class);
	
	private GraphMatching(){}

/**
 * Retourne un hashmap avec en clé la ressource du TEI et en valeur la liste des candidats de la KB
 * @return
 */
	public static Set<NodeMatching> nodeSelection() {
		float threshold = 0.7f;

		Set<NodeMatching> toponymsTEI = getToponymsFromTei();
		logger.info(toponymsTEI.size() + " toponymes dans le TEI");
		

		final List<Candidate> candidatesFromKB = getCandidatesFromKB();


		Set<NodeMatching> result = getCandidatesSelection(toponymsTEI, candidatesFromKB, threshold);
		logger.info(result.size() + " candidats");

//		// ancienne récusion pour trouver des candidats en faisant baisser le seuil (à 
//		// insérer dans la fonction getCandidatesSelection)
//		int i = 0;
//		while (result.entrySet().stream().anyMatch(entry -> entry.getValue().isEmpty())) {
//			i++;
//			logger.info(i + "eme itération");
//			threshold = threshold - 0.01f;
//			HashMap<String, String> toponymsTEITmp = new HashMap<>();
//			result.entrySet().stream().filter(entry -> entry.getValue().isEmpty()).forEach(entry -> 
//				toponymsTEITmp.put(entry.getKey(), toponymsTEI.get(entry.getKey()))
//			);
//			HashMap<String, Set<String>> resultTmp = getCandidatesSelection(
//					toponymsTEITmp, 
//					candidatesFromKB, 
//					threshold);
//			for (Iterator<Entry<String, Set<String>>> iterator = resultTmp.entrySet().iterator(); iterator.hasNext();) {
//				Entry<String, Set<String>> candidate = iterator.next();
//				result.put(candidate.getKey(), candidate.getValue());
//			}
//		}

//		logger.info("Affichage des résultats");
//		for (Iterator<String> iterator = result.keySet().iterator(); iterator.hasNext();) {
//			String key = iterator.next();
//			Set<String> candidates = result.get(key);
//			String s = key + " (" + candidates.size() + ") : ";
//			for (String string : candidates) {
//				s += string + ", ";
//			}
//			logger.info(s);
//		}
		if (result.stream().anyMatch(entry -> entry.getScores().isEmpty())) {
			for (Iterator<NodeMatching> iterator = result.iterator(); iterator.hasNext();) {
				NodeMatching key = iterator.next();
				List<Score> candidates = key.getScores();
				if (candidates.isEmpty())
					logger.info(key.getResource());
			}
		} else {
			logger.info("pas de topo sans candidat");
		}
		return result;
	}
	
	/**
	 * Gets the candidates selection.
	 *	@deprecated
	 * @param toponymsTEI the toponyms tei
	 * @param candidatesFromKB the candidates from kb
	 * @param threshold the threshold
	 * @return the candidates selection
	 */
	@Deprecated
	static HashMap<String, Set<String>> getCandidatesSelection(HashMap<String, String> toponymsTEI, List<Candidate> candidatesFromKB, float threshold) {
		logger.info("Sélection des candidats");
		HashMap<String, Set<String>> result = new HashMap<>();
		final AtomicInteger count = new AtomicInteger();
		final int total = toponymsTEI.size();
		StringComparisonDamLev sc = new StringComparisonDamLev();
		toponymsTEI.entrySet().parallelStream().forEach(set -> {
			Set<String> candidateResources = new HashSet<>();
			candidatesFromKB.parallelStream().forEach(querySolutionKB -> {
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
		return result;
	}
	
	static Set<NodeMatching> getCandidatesSelection(Set<NodeMatching> toponymsTEI, List<Candidate> candidatesFromKB, float threshold) {
		logger.info("Sélection des candidats (seuil : " + threshold + ")");
		Set<NodeMatching> result = new HashSet<>();
		Set<NodeMatching> noCandidateFounded = new HashSet<>();
		final AtomicInteger count = new AtomicInteger();
		final int total = toponymsTEI.size();
		StringComparisonDamLev sc = new StringComparisonDamLev();
		toponymsTEI.parallelStream().forEach(set -> {
			candidatesFromKB.parallelStream().forEach(querySolutionKB -> {
				if (set != null && querySolutionKB != null) {
					float score = sc.computeSimilarity(set.getLabel(), querySolutionKB.getName());
					if (score >= threshold) 
						set.addScore(new Score(querySolutionKB, score));
					else {
						score = sc.computeSimilarity(set.getLabel(), querySolutionKB.getLabel());
						if (score >= threshold) 
							set.addScore(new Score(querySolutionKB, score));
					}
				}
			});
			if (!set.getScores().isEmpty())
				result.add(set);
			else
				noCandidateFounded.add(set);
			logger.info(count.getAndIncrement() + " / " + total);
		});
		if (!noCandidateFounded.isEmpty())
			result.addAll(getCandidatesSelection(noCandidateFounded, candidatesFromKB, threshold - 0.01f ));
		return result;
	}

	static Set<NodeMatching> getToponymsFromTei() {
		logger.info("Chargement du TEI");
		Document teiSource = XMLUtil.createDocumentFromFile("d://temp7.rdf");
		Set<NodeMatching> results = new HashSet<>();
		logger.info("Récupération des toponymes du TEI");
		List<QuerySolution> qSolutionsTEI = new ArrayList<>();
		try {
			qSolutionsTEI.addAll(RDFUtil.getQuerySelectResults(teiSource,
					"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
							+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"
							+ "PREFIX prop-fr: <http://fr.dbpedia.org/property/>"
							+ "PREFIX foaf: <http://xmlns.com/foaf/0.1/>"
							+ "PREFIX ign: <http://example.com/namespace/>"
							+ "PREFIX dbo: <http://dbpedia.org/ontology/>" + "" + "SELECT DISTINCT * WHERE {"
							+ "  ?s rdfs:label ?label ." 
							+ "  ?s ign:id ?id ." 
							+ "  ?s rdf:type ?type ." 
							+ "}"));
		} catch (QueryParseException | HttpHostConnectException | RiotException | MalformedURLException
				| HttpException e) {
			logger.error(e);
		}
		for (QuerySolution querySolution : qSolutionsTEI) {
			String label = RDFUtil.getURIOrLexicalForm(querySolution, "label");
			String type = RDFUtil.getURIOrLexicalForm(querySolution, "type");
			String resource = RDFUtil.getURIOrLexicalForm(querySolution, "s");
			Integer id = Integer.parseInt(RDFUtil.getURIOrLexicalForm(querySolution, "id"));
			results.add(new NodeMatching(label, type, id, resource));
		}
		return results;
	}
	
	static List<Candidate> getCandidatesFromKB() {
		List<QuerySolution> qSolutionsKB = new ArrayList<>();
		List<Candidate> result = new ArrayList<>();

		logger.info("Chargement du KB");
		final Model kbSource = ModelFactory.createDefaultModel().read("D:\\\\dbpedia\\\\dbpedia_all.n3");
		//kbSource = kbSource.read("D:\\\\dbpedia\\\\dbpedia_all.n3");
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
//					"  ?s rdf:type ?type . " + 
//					"  FILTER (STRSTARTS(STR(?type), 'http://dbpedia.org/ontology/')) . " + 
					"}"));
		} catch (QueryParseException | HttpHostConnectException | MalformedURLException | HttpException e) {
			logger.error(e);
		}
		String[] query = new String[2];
		query[0] = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" + 
				"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>" + 
				"PREFIX prop-fr: <http://fr.dbpedia.org/property/>" + 
				"PREFIX foaf: <http://xmlns.com/foaf/0.1/>" + 
				"PREFIX dbpedia-owl: <http://dbpedia.org/ontology/>" + 
				"" + 
				"SELECT DISTINCT ?type WHERE {" + 
				"  <";
		query[1] = "> rdf:type ?type . " + 
				"  FILTER (STRSTARTS(STR(?type), 'http://dbpedia.org/ontology/')) . " + 
				"}";
		qSolutionsKB.parallelStream().forEach(querySolution -> {
			String candidateResource = RDFUtil.getURIOrLexicalForm(querySolution, "s");
			String candidateLabel = RDFUtil.getURIOrLexicalForm(querySolution, "label");
			String candidateName = RDFUtil.getURIOrLexicalForm(querySolution, "name");
			Set<String> types = new HashSet<>();
//			List<QuerySolution> qSolTypes = new ArrayList<>();
//			try {
//				qSolTypes.addAll(RDFUtil.getQuerySelectResults(kbSource, query[0] + candidateResource + query[1]));
//			} catch (QueryParseException | HttpHostConnectException | RiotException | MalformedURLException
//					| HttpException e) {
//				logger.error(e);
//			}
//			for (QuerySolution qSolType : qSolTypes) {
//				types.add(RDFUtil.getURIOrLexicalForm(qSolType, "type"));
//			}
			result.add(new Candidate(candidateResource, candidateLabel, candidateName, types));
		});
		logger.info(result.size() + " résultats");
		return result;
	}
}


