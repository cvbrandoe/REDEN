package fr.ign.georeden.algorithms.graph.matching;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.http.conn.HttpHostConnectException;
import org.apache.jena.atlas.web.HttpException;
import org.apache.jena.riot.RiotException;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.query.QueryParseException;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

import fr.ign.georeden.algorithms.string.StringComparisonDamLev;
import fr.ign.georeden.kb.ToponymType;
import fr.ign.georeden.utils.RDFUtil;
import fr.ign.georeden.utils.XMLUtil;

/**
 * The Class GraphMatching.
 */
public class GraphMatching {
	
	/** The logger. */
	private static Logger logger = Logger.getLogger(GraphMatching.class);
	
	public static String teiPath = "d://temp7.rdf";
	
	/**
	 * Instantiates a new graph matching.
	 */
	private GraphMatching(){}

/**
 * Retourne un hashmap avec en clé la ressource du TEI et en valeur la liste des candidats de la KB.
 *
 * @return the sets the
 */
	public static Set<Toponym> nodeSelection() {
		Integer numberOfCandidate = 10;

		logger.info("Chargement du TEI");
		Document teiSource = XMLUtil.createDocumentFromFile(teiPath);
		Model teiRdf = RDFUtil.getModel(teiSource);
		Set<Toponym> toponymsTEI = getToponymsFromTei(teiRdf);
		logger.info(toponymsTEI.size() + " toponymes dans le TEI");
		

		final Model kbSource = ModelFactory.createDefaultModel().read("D:\\\\dbpedia\\\\dbpedia_all.n3");
		final List<Candidate> candidatesFromKB = getCandidatesFromKB(kbSource);


		Set<Toponym> result = getCandidatesSelection(toponymsTEI, candidatesFromKB, numberOfCandidate);
		logger.info(result.size() + " candidats");


		logger.info("Vérification des résultats");
		if (result.stream().anyMatch(entry -> entry.getScoreCriterionToponymCandidate().isEmpty() 
				|| entry.getScoreCriterionToponymCandidate().size() < 10 
				|| entry.getName().equalsIgnoreCase("Ruffec"))) {
			for (Iterator<Toponym> iterator = result.iterator(); iterator.hasNext();) {
				Toponym key = iterator.next();
				List<CriterionToponymCandidate> candidates = key.getScoreCriterionToponymCandidate();
				if (candidates.isEmpty() || candidates.size() < 10) {
					logger.info(key.getResource() + " : " + candidates.size());
				} else if (key.getName().equalsIgnoreCase("Ruffec")) {
					logger.info(key.getResource() + " (" + key.getName() +") : " + candidates.get(0).getCandidate().getResource());
				}
			}
		} else {
			logger.info("pas de topo sans candidat pour le score de label");
		}
		
		Resource ruffec = teiRdf.getResource("http://data.ign.fr/id/propagation/Place/0");
		Resource chize = teiRdf.getResource("http://data.ign.fr/id/propagation/Place/1");
		StmtIterator iter = chize.listProperties();
		while (iter.hasNext()) {
		    Statement stmt      = iter.nextStatement();  // get next statement
		    Resource  subject   = stmt.getSubject();     // get the subject
		    Property  predicate = stmt.getPredicate();   // get the predicate
		    RDFNode   object    = stmt.getObject();      // get the object

		    System.out.print(subject.toString());
		    System.out.print(" " + predicate.toString() + " ");
		    if (object instanceof Resource) {
		       System.out.print(object.toString());
		    } else {
		        // object is a literal
		        System.out.print(" \"" + object.toString() + "\"");
		    }

		    System.out.println(" .");
		} 
		Resource ruffecCharente = kbSource.getResource("http://fr.dbpedia.org/resource/Ruffec_(Charente)");
//		Toponym ruffecToponym = result.stream().filter(t -> t.getResource().equalsIgnoreCase("http://data.ign.fr/id/propagation/Place/0")).findFirst().get();
//		List<String> ruffecCandidates = ruffecToponym.getScoreCriterionToponymCandidate().stream().map(s -> s.getCandidate().getResource()).collect(Collectors.toList());
//		for (String string : ruffecCandidates) {
//			System.out.println(string);
//		}
		return result;
	}	
	
	
	
	/**
	 * Gets the candidates selection.
	 *
	 * @param toponymsTEI the toponyms tei
	 * @param candidatesFromKB the candidates from kb
	 * @param threshold the threshold
	 * @return the candidates selection
	 */
	static Set<Toponym> getCandidatesSelection(Set<Toponym> toponymsTEI, List<Candidate> candidatesFromKB, Integer numberOfCandidate) {
		logger.info("Sélection des candidats (nombre de candidats : " + numberOfCandidate + ")");
		Criterion criterion = Criterion.scoreText;
		Set<Toponym> result = new HashSet<>();
		final AtomicInteger count = new AtomicInteger();
		final int total = toponymsTEI.size();
		StringComparisonDamLev sc = new StringComparisonDamLev();
		toponymsTEI.parallelStream().filter(t -> t != null).forEach(toponym -> {
			candidatesFromKB.parallelStream().filter(c -> c != null && (toponym.getType() == ToponymType.PLACE || typeContained(toponym.getType().toString(), c.getTypes()))).forEach(candidate -> {
				float score1 = sc.computeSimilarity(toponym.getName(), candidate.getName());
				float score2 = sc.computeSimilarity(toponym.getName(), candidate.getName());
				if (Math.max(score1, score2) > 0f)
					toponym.addScoreCriterionToponymCandidate(new CriterionToponymCandidate(toponym, candidate, Math.max(score1, score2), criterion));
			});
			if (toponym.getScoreCriterionToponymCandidate() != null && !toponym.getScoreCriterionToponymCandidate().isEmpty()) {
				toponym.clearAndAddAllScoreCriterionToponymCandidate(toponym.getScoreCriterionToponymCandidate().stream().filter(s -> s != null)
						.sorted(Comparator.comparing(CriterionToponymCandidate::getValue).reversed())
						.limit(Math.min(numberOfCandidate, toponym.getScoreCriterionToponymCandidate().size()))
						.collect(Collectors.toList()));
			}
			result.add(toponym);
			logger.info((count.getAndIncrement() + 1) + " / " + total);
		});
		return result;
	}

	/**
	 * Return true if the type to check is contained in the set of types.
	 *
	 * @param typeToCheck the type to check
	 * @param types the types
	 * @return true, if successful
	 */
	static boolean typeContained(String typeToCheck, Set<String> types) {
		String typeToponym = typeToCheck.substring(typeToCheck.lastIndexOf(':') + 1);
		for (String t : types) {
			String typeCandidate = t.substring(t.lastIndexOf('/') + 1);
			if (typeToponym.equalsIgnoreCase(typeCandidate))
				return true;
		}
		return false;
	}
	
	/**
	 * Gets the toponyms from tei.
	 *
	 * @return the toponyms from tei
	 */
	static Set<Toponym> getToponymsFromTei(Model teiRdf) {
		Set<Toponym> results = new HashSet<>();
		logger.info("Récupération des toponymes du TEI");
		List<QuerySolution> qSolutionsTEI = new ArrayList<>();
		try {
			qSolutionsTEI.addAll(RDFUtil.getQuerySelectResults(teiRdf,
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
			results.add(new Toponym(resource, id, label, ToponymType.where(type)));
		}
		return results;
	}
	
	/**
	 * Gets the candidates from kb.
	 *
	 * @return the candidates from kb
	 */
	static List<Candidate> getCandidatesFromKB(Model kbSource) {
		List<QuerySolution> qSolutionsKB = new ArrayList<>();
		List<QuerySolution> qSolutionsTypes = new ArrayList<>();
		List<Candidate> result = new ArrayList<>();

		logger.info("Chargement de la KB");
		final Model kbSourceFinal = kbSource;
		logger.info("Récupérations des candidats de la KB");

		try {
			qSolutionsKB.addAll(RDFUtil.getQuerySelectResults(kbSourceFinal,
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
		logger.info("Récupérations des types des candidats de la KB");
		try {
			qSolutionsTypes.addAll(RDFUtil.getQuerySelectResults(kbSourceFinal, "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" + 
					"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>" + 
					"PREFIX prop-fr: <http://fr.dbpedia.org/property/>" + 
					"PREFIX foaf: <http://xmlns.com/foaf/0.1/>" + 
					"PREFIX dbo: <http://dbpedia.org/ontology/>" + 
					"SELECT * WHERE {" + 
					"  ?s rdf:type ?type . " + 
					"  FILTER (?type=dbo:Mountain || ?type=dbo:Volcano || ?type=dbo:BodyOfWater || "
					+ "?type=dbo:Settlement || ?type=dbo:Territory || ?type=dbo:NaturalPlace) . " + 
					"}"));
		} catch (QueryParseException | HttpHostConnectException | MalformedURLException | HttpException e) {
			logger.error(e);
		}
		logger.info("Traitement des types des candidats de la KB");
		List<String[]> resourceTypeMap = new ArrayList<>();
		qSolutionsTypes.stream().forEach(qs -> {
			String candidateResource = RDFUtil.getURIOrLexicalForm(qs, "s");
			String candidateType = RDFUtil.getURIOrLexicalForm(qs, "type");
			resourceTypeMap.add(new String[]{candidateResource, candidateType});
		});
		Map<String, List<String[]>>  typesByResources = resourceTypeMap.stream()
	            .collect(Collectors.groupingBy((String [] s) -> s[0]));
		logger.info("Traitement des candidats de la KB");
		qSolutionsKB.parallelStream().forEach(querySolution -> {
			String candidateResource = RDFUtil.getURIOrLexicalForm(querySolution, "s");
			String candidateLabel = RDFUtil.getURIOrLexicalForm(querySolution, "label");
			String candidateName = RDFUtil.getURIOrLexicalForm(querySolution, "name");
			Set<String> types = new HashSet<>();
			if (typesByResources.containsKey(candidateResource)) {
				List<String[]> list = typesByResources.get(candidateResource);
				for (String[] strings : list) {
					types.add(strings[1]);
				}
			}
			types.add("http://dbpedia.org/ontology/Place");
			result.add(new Candidate(candidateResource, candidateLabel, candidateName, types));
		});
		logger.info(result.size() + " résultats");
		return result;
	}
}


