package fr.ign.georeden.algorithms.graph.matching;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
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

import javax.naming.spi.DirStateFactory.Result;

import org.apache.http.conn.HttpHostConnectException;
import org.apache.jena.atlas.lib.NotImplemented;
import org.apache.jena.atlas.web.HttpException;
import org.apache.jena.riot.RiotException;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.ontology.OntTools;
import com.hp.hpl.jena.ontology.OntTools.PredicatesFilter;
import com.hp.hpl.jena.query.QueryParseException;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.sparql.function.library.e;
import com.hp.hpl.jena.util.iterator.Filter;

import fr.ign.georeden.algorithms.string.StringComparisonDamLev;
import fr.ign.georeden.graph.LabeledEdge;
//import fr.ign.georeden.graph.Toponym;
import fr.ign.georeden.kb.SpatialRelationship;
import fr.ign.georeden.kb.ToponymType;
import fr.ign.georeden.utils.RDFUtil;
import fr.ign.georeden.utils.XMLUtil;

import org.jgrapht.graph.SimpleDirectedGraph;

/**
 * The Class GraphMatching.
 */
public class GraphMatching {

	/** The logger. */
	private static Logger logger = Logger.getLogger(GraphMatching.class);

	public static String teiPath = "D:\\temp7.rdf";

	/**
	 * Instantiates a new graph matching.
	 */
	private GraphMatching() {
	}

	/**
	 * Retourne un hashmap avec en clé la ressource du TEI et en valeur la liste
	 * des candidats de la KB.
	 *
	 * @return the sets the
	 */
	public static Set<Toponym> nodeSelection() {
		Integer numberOfCandidate = 10;

		logger.info("Chargement du TEI");
		Document teiSource = XMLUtil.createDocumentFromFile(teiPath);
		Model teiRdf = RDFUtil.getModel(teiSource);
		logger.info("Model TEI vide : " + teiRdf.isEmpty());
		Set<Toponym> toponymsTEI = getToponymsFromTei(teiRdf);
		logger.info(toponymsTEI.size() + " toponymes dans le TEI");
		
		getRDFSequences(teiRdf);
		
		logger.info("Chargement de la KB");
		final Model kbSource = ModelFactory.createDefaultModel().read("D:\\\\dbpedia\\\\dbpedia_all.n3");

		Model sourceGraph = getSubGraphWithResources(kbSource);

		logger.info("Calcul du plus court chemin en cours... ");
		Resource start = sourceGraph.getResource("http://fr.dbpedia.org/resource/Chizé");// Saint-Jean-de-Luz
		Resource end = sourceGraph.getResource("http://fr.dbpedia.org/resource/Saint-Jean-d'Angély");
		// Ruffec_(Charente)
		// Aulnay_(Charente-Maritime)
		// Chizé
		// Surgères
		// Rochefort_(Charente-Maritime)
		// La_Rochelle
		// Saint-Jean-d'Angély
		List<Property> properties = new ArrayList<>();
		properties.add(sourceGraph.createProperty("http://fr.dbpedia.org/property/nord"));
		properties.add(sourceGraph.createProperty("http://fr.dbpedia.org/property/nordEst"));
		properties.add(sourceGraph.createProperty("http://fr.dbpedia.org/property/nordOuest"));
		properties.add(sourceGraph.createProperty("http://fr.dbpedia.org/property/sud"));
		properties.add(sourceGraph.createProperty("http://fr.dbpedia.org/property/sudEst"));
		properties.add(sourceGraph.createProperty("http://fr.dbpedia.org/property/sudOuest"));
		properties.add(sourceGraph.createProperty("http://fr.dbpedia.org/property/est"));
		properties.add(sourceGraph.createProperty("http://fr.dbpedia.org/property/ouest"));
		PredicatesFilter filter = new PredicatesFilter(properties);
		OntTools.Path path = OntTools.findShortestPath(sourceGraph, start, end, Filter.any);// Filter.any
		if (path != null && !path.isEmpty()) {
			logger.info("longeur du chemin le plus court : " + path.size());
			logger.info("chemin le plus court : " + path.toString());
		} else
			logger.info("longeur du chemin le plus court : KO");

		final List<Candidate> candidatesFromKB = getCandidatesFromKB(kbSource);

		Set<Toponym> result = getCandidatesSelection(toponymsTEI, candidatesFromKB, numberOfCandidate);
		logger.info(result.size() + " candidats");
		// il faut filter les toponyms de toponymsTEI en ne gardant que ceux qui
		// sont dans teiRDF
		// et donc il faut remplacer teiRDF par le graphe RDF d'une sequence
		
		//getRDFSequences(teiRdf);
		
		// AStarBeamSearch(sourceGraph, teiRdf, 10, numberOfCandidate,
		// toponymsTEI, candidatesFromKB);

		// logger.info("Vérification des résultats");
		// if (result.stream().anyMatch(entry ->
		// entry.getScoreCriterionToponymCandidate().isEmpty()
		// || entry.getScoreCriterionToponymCandidate().size() < 10
		// //|| entry.getName().equalsIgnoreCase("Ruffec")
		// || entry.getScoreCriterionToponymCandidate().stream().map(m ->
		// m.getValue()).max(Float::compare).get() <= 0f)) {
		// for (Iterator<Toponym> iterator = result.iterator();
		// iterator.hasNext();) {
		// Toponym key = iterator.next();
		// List<CriterionToponymCandidate> candidates =
		// key.getScoreCriterionToponymCandidate();
		// if (candidates.isEmpty() || candidates.size() < 10) {
		// logger.info("taille : " + key.getResource() + " : " +
		// candidates.size());
		// }
		//// else if (key.getName().equalsIgnoreCase("Ruffec")) {
		//// logger.info(key.getResource() + " (" + key.getName() +") : " +
		// candidates.get(0).getCandidate().getResource());
		//// }
		// else if (key.getScoreCriterionToponymCandidate().stream().map(m ->
		// m.getValue()).max(Float::compare).get() <= 0f) {
		// logger.info("scores : " + key.getResource() + " (" + key.getName()
		// +") : " + candidates.get(0).getCandidate().getResource());
		// }
		// }
		// } else {
		// logger.info("pas de topo sans candidat pour le score de label");
		// }

		return result;
	}
	
	static List<Model> getRDFSequences(Model teiModel) {
		String query = 
				"PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>" + 
				"PREFIX iti:<http://data.ign.fr/def/itineraires#>" + 
				"SELECT ?sequence ?route ?bag ?waypoint ?spatialReference ?spatialReferenceAlt WHERE {" + 
				"    ?sequence rdf:type rdf:Seq ." + 
				"    ?sequence ?pSeq ?route ." + 
				"    ?route iti:waypoints ?waypoints ." + 
				"    ?waypoints rdf:rest*/rdf:first ?bag ." + 
				"  	?bag ?pBag ?waypoint ." + 
				"  	OPTIONAL { ?waypoint iti:spatialReference ?spatialReference . }" + 
				"  	OPTIONAL {" + 
				"    	?waypoint rdf:type rdf:Alt ." + 
				"    	?waypoint ?pWaypoint ?waypointBis ." + 
				"    	?waypointBis iti:spatialReference ?spatialReferenceAlt ." + 
				"  	}" + 
				"    FILTER (?pSeq != rdf:type && ?pBag != rdf:type && ?pBag != rdf:first) ." + 
				"} ORDER BY ?sequence ?route ?bag ?waypoint";
		List<QuerySolution> querySolutions = new ArrayList<>();
		try {
			querySolutions.addAll(RDFUtil.getQuerySelectResults(teiModel, query));
		} catch (QueryParseException | HttpHostConnectException | RiotException | MalformedURLException
				| HttpException e) {
			logger.info(e);
		}
		List<Model> results = new ArrayList<>();
		Set<Resource> usedSequences = new HashSet<>();
		Set<Resource> usedRoutes = new HashSet<>();
		Resource lastUsedBag = null;
		Property linkSameRoute = teiModel.createProperty("ign:linkSameRoute");
		Property linkSameSequence = teiModel.createProperty("ign:linkSameSequence");
		Property linkSameBag = ModelFactory.createDefaultModel().createProperty("ign:linkSameBag");
		List<QuerySolutionEntry> querySolutionEntries = new ArrayList<>();
		for (QuerySolution querySolution : querySolutions) {
			Resource sequence = (Resource) querySolution.get("sequence");
			Resource route = (Resource) querySolution.get("route");
			Resource bag = (Resource) querySolution.get("bag");
			Resource waypoint = (Resource) querySolution.get("waypoint");
			Resource spatialReference = (Resource) querySolution.get("spatialReference");
			Resource spatialReferenceAlt = (Resource) querySolution.get("spatialReferenceAlt");
			querySolutionEntries.add(new QuerySolutionEntry(sequence, route, bag, waypoint, spatialReference, spatialReferenceAlt));
//			Model currentModel = ModelFactory.createDefaultModel();
//			if (usedSequences.contains(sequence)) {
//				currentModel = results.get(results.size() - 1);
//			} else {
//				// c'est une nouvelle séquence, on réinitialise le dernier bag pr ne pas utilisé celui de la dernière séquence
//				lastUsedBag = null;
//			}
//			
//			
//			if (lastUsedBag == null) {
//				lastUsedBag = bag; // !! si la séquence ne contient qu'un bag, celui ci ne sera jamais inséré dans le model
//			} else {
//				if (usedRoutes.contains(route)) {
//					results = createStatements(lastUsedBag, linkSameRoute, bag, results);
//				} else { // c'est une nouvelle route
//					usedRoutes.add(route);
//					results = createStatements(lastUsedBag, linkSameSequence, bag, results);
//				}
//			}
//			
//			if (!usedSequences.contains(sequence)) {
//				usedSequences.add(sequence);
//				results.add(currentModel);
//			}
		}
		Model currentModel = ModelFactory.createDefaultModel();
		Map<Resource, List<QuerySolutionEntry>> byBag = querySolutionEntries.stream()
				.collect(Collectors.groupingBy(QuerySolutionEntry::getBag));
		byBag.keySet().stream().filter(key -> byBag.get(key).stream().filter(e -> e.getSpatialReference() != null).count() >= 2)
			.forEach(key -> {
			List<QuerySolutionEntry> entries = byBag.get(key);
			List<Resource> bagElements = new ArrayList<>();
			entries.stream().filter(e -> e.getSpatialReference() != null).forEach(q -> bagElements.add(q.getSpatialReference()));
			for (int i = 0; i < bagElements.size() - 1; i++) {
				for (int j = 1; j < bagElements.size(); j++) {
					Resource el1 = bagElements.get(i);
					Resource el2 = bagElements.get(j);
					Statement s = currentModel.createStatement(el1, linkSameBag, el2);
					currentModel.add(s);
				}
//				if (i < bagElements.size() - 1) {
//					Resource el1 = bagElements.get(i);
//					Resource el2 = bagElements.get(i + 1);
//					Statement s = currentModel.createStatement(el1, linkSameBag, el2);
//					currentModel.add(s);
//				}
			}
		});
		results.add(currentModel);
		logger.info(currentModel);
		logger.info(currentModel.size());
		return results;
	}
	static List<Model> createStatements(Resource lastUsedBag, Property p, Resource bag, List<Model> results) {
		
		return results;
	}
//	static List<Model> getRDFSequences(Model teiModel) {
//		List<Model> results = new ArrayList<>();
//		
//		Resource seq = teiModel.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#Seq");
//		Property type = teiModel.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
//		ResIterator iter = teiModel.listSubjectsWithProperty(type, seq);
//		while (iter.hasNext()) {
//			Resource sequence = (Resource) iter.next();
//			results.add(generateFromSequence(sequence, teiModel));
//		}
//		
//		return results;
//	}
	
	static Model generateFromSequence(Resource sequence, Model teiModel) {
		throw new NotImplemented();
//		Property type = teiModel.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
//		// traitements ici 
//		StmtIterator iter2 = sequence.listProperties();
//		List<Property> sequenceProperties = new ArrayList<>();
//		while (iter2.hasNext()) {
//			Statement statement = (Statement) iter2.next();
//			if (!statement.getPredicate().getLocalName().equals(type.getLocalName()))
//				sequenceProperties.add(statement.getPredicate());
//		}
//		Collections.sort(sequenceProperties, new CustomPropertyComparator());
//		List<Resource> routes = new ArrayList<>();				
//		for (Property property : sequenceProperties) {
//			RDFNode node = sequence.getProperty(property).getObject();
//			if (node.isResource()) {
//				Resource route = (Resource)node;
//				routes.add(route);
//			}
//		}
//		return generateFromRoutes(routes, teiModel);
	}
	
	static List<Model> generateFromRoutes(List<Resource> routes, Model teiModel) {
		List<Model> results = new ArrayList<>();
		results.add(ModelFactory.createDefaultModel());
		Property waypoints = teiModel.getProperty("http://data.ign.fr/def/itineraires#waypoints");
		for (Resource route : routes) {
			Resource waypointsResource = (Resource) route.getProperty(waypoints).getObject();
			Resource first = (Resource)waypointsResource.getProperty(teiModel.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#first")).getObject();
			RDFNode restNode = waypointsResource.getProperty(teiModel.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#rest")).getObject();
			// first est un Bag
			//logger.info("first : " + getType(first, teiModel));	
			//List<List<Resource>> possibleBags = new ArrayList<>();
			//possibleBags.add(new ArrayList<>());	
			results = generateFromBag(first, teiModel, results);
//			for (List<Resource> listResource : possibleBags) {
//				//logger.info("Nouveau bag");
//				for (Resource resource : listResource) {
//					logger.info(resource);
//				}
//			}
			// S'occuper du rest
			if (!restNode.isLiteral()) {
				Resource rest = (Resource)restNode;
				if (rest.isAnon() || !rest.getLocalName().equals("nil")) {
					Resource firstOfRest = (Resource)rest.getProperty(teiModel.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#first")).getObject();
					results = generateFromBag(firstOfRest, teiModel, results);
					restNode = rest.getProperty(teiModel.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#rest")).getObject();
					while(!restNode.isLiteral() && (restNode.isAnon())) {					
						firstOfRest = (Resource)((Resource)restNode).getProperty(teiModel.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#first")).getObject();
						results = generateFromBag(firstOfRest, teiModel, results);
						if (!((Resource)restNode).hasProperty(teiModel.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#rest")))
							break;
						//rest.listProperties().toList().forEach(logger::info);
						restNode = ((Resource)restNode).getProperty(teiModel.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#rest")).getObject();
					}
				}
			}
			logger.info(results.size());
//			if (restNode.isLiteral()) {
//				logger.info("rest=literal :" + restNode.asLiteral().getValue()); // pb sur le reste de 13_1 (plaine poitevine)
//			} else {
//				Resource rest = (Resource)restNode;
//				if (rest.isAnon() || !rest.getLocalName().equals("nil")) {
//					Resource firstOfRest = (Resource)rest.getProperty(teiModel.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#first")).getObject();
//					logger.info("firstOfRest : " + getType(firstOfRest, teiModel)); // bag
//					rest.listProperties().toList().stream().forEach(logger::info);
//					// rest du rest à traiter
//				}
//			}
		}
		return results;
	}
	
//	static Model generateFromBag(Resource bag, Model teiModel, Model result) {
//		return ModelFactory.createDefaultModel();
//	}
	
	
	static List<Model> generateFromBag(Resource bag, Model teiModel, List<Model> results) {
		// On retourne une liste de liste, car pour chaque rdf:Alt, une nouvelle possibilité est créée
		List<Property> bagProperties = new ArrayList<>();
		Property type = teiModel.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		Property spatialReference = teiModel.createProperty("http://data.ign.fr/def/itineraires#spatialReference");
		bag.listProperties().toList().stream()
			.filter(s -> !s.getPredicate().getLocalName().equals(type.getLocalName()))
			.forEach(s -> bagProperties.add(s.getPredicate()));
		Collections.sort(bagProperties, new CustomPropertyComparator());
		List<List<Resource>> resourcesOfBag = new ArrayList<>();
		resourcesOfBag.add(new ArrayList<>());
		for (Property property : bagProperties) {
			RDFNode node = bag.getProperty(property).getObject();
			if (node.isResource()) {
				Resource waypoint = (Resource)node;
				Statement statement = waypoint.getProperty(spatialReference);
				if (statement != null) {
					Resource toponym = (Resource) statement.getObject();					
					for (List<Resource> list : resourcesOfBag) {
						list.add(toponym);
					}
				} else {
					// on est dans une rdf:Alt
					List<List<Resource>> listsToAdd = new ArrayList<>();
					for (List<Resource> list : resourcesOfBag) {
						for (Resource alt : getAlternatives(waypoint, teiModel)) {
							List<Resource> listTmp = new ArrayList<>(list);
							listTmp.add(alt);
							listsToAdd.add(listTmp);
						}
					}
					resourcesOfBag.clear();
					resourcesOfBag.addAll(listsToAdd);
				}
			}
		}
		for (List<Resource> list : resourcesOfBag) {
			if (list.size() == 1) {
				for (Model model : results) {

					throw new NotImplemented();
				}
			}
		}
		return results;
	}

	static List<Resource> getAlternatives(Resource waypoint, Model teiModel) {
		Property type = teiModel.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		Property spatialReference = teiModel.createProperty("http://data.ign.fr/def/itineraires#spatialReference");
		List<Property> waypointAlts = new ArrayList<>();
		List<Resource> alternatives = new ArrayList<>();
		waypoint.listProperties().toList().stream()
			.filter(s -> !s.getPredicate().getLocalName().equals(type.getLocalName()))
			.forEach(s -> waypointAlts.add(s.getPredicate()));
		Collections.sort(waypointAlts, new CustomPropertyComparator());	
		for (Property property2 : waypointAlts) {
			Resource waypointInAlt = (Resource)waypoint.getProperty(property2).getObject();
			alternatives.add((Resource)waypointInAlt.getProperty(spatialReference).getObject());
		}
		return alternatives;
	}
	
	static String getType(Resource r, Model model) {
		Property type = model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		Statement s = r.getProperty(type);
		if (s == null)
			return null;
		RDFNode n = s.getObject();
		if (n.isLiteral()) 
			return "literal";
		Resource res = (Resource)n;
		return res.getLocalName();
	}

	static void saveModelToFile(String fileName, Model model) {
		File file = new File(fileName);
		try {
			model.write(new java.io.FileOutputStream(file));
		} catch (FileNotFoundException e) {
			logger.error(e);
		}
	}

	// static void fuseRDFGraphsIntoJGTGraph(Model model) {
	// SimpleDirectedGraph<Toponym, LabeledEdge<Toponym, String>> graph = new
	// SimpleDirectedGraph<>((Class<? extends LabeledEdge<Toponym, String>>)
	// LabeledEdge.class);
	//
	// }

	/**
	 * A star beam search.
	 *
	 * @param source
	 *            the source (KB subgraph with only resources linked by prop-fr
	 *            properties)
	 * @param target
	 *            the target (graph from a TEI's sequence)
	 * @param maxNumberOfNodesToProcess
	 *            the max number of nodes to process
	 * @param numberOfCandidatByToponym
	 *            the number of candidat by toponym
	 * @param toponymsTEI
	 *            the toponyms TEI
	 * @param candidates
	 *            the candidates from the KB
	 */
	static void AStarBeamSearch(Model source, Model target, int maxNumberOfNodesToProcess,
			int numberOfCandidatByToponym, Set<Toponym> toponymsTEI) {
		CriterionToponymCandidate firstC = toponymsTEI.stream().sorted((t1, t2) -> Float.compare(
				t2.getScoreCriterionToponymCandidate().stream().map(m -> m.getValue()).max(Float::compare).get(),
				t1.getScoreCriterionToponymCandidate().stream().map(m -> m.getValue()).max(Float::compare).get()))
				.limit(1).collect(Collectors.toList()).get(0).getScoreCriterionToponymCandidate().stream()
				.sorted((c1, c2) -> Float.compare(c2.getValue(), c1.getValue())).limit(1).collect(Collectors.toList())
				.get(0);
		Resource firstR = source.getResource(firstC.getCandidate().getResource());
		logger.info("CriterionToponymCandidate : " + firstC.getValue() + " / " + firstC.getCandidate().getResource());
		logger.info("CriterionToponymCandidate est dans liste des candidats : "
				+ isResourceACandidate(firstR, toponymsTEI)); // doit être faux
																// pr massif sri
																// lanka
	}

	static boolean isResourceACandidate(Resource r, Set<Toponym> toponymsTEI) {
		return toponymsTEI.stream().anyMatch(t -> t.getScoreCriterionToponymCandidate().stream()
				.anyMatch(c -> c.getCandidate().getResource().equals(r.getURI())));
	}

	static double meanNumberOfLinksByNode(Model kbSource) {
		// calcul du nombre de lien moyen par noeud (entre dbo:place)
		double result = 0;
		try {
			List<QuerySolution> sol = RDFUtil.getQuerySelectResults(kbSource,
					"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
							+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"
							+ "PREFIX prop-fr: <http://fr.dbpedia.org/property/>"
							+ "PREFIX foaf: <http://xmlns.com/foaf/0.1/>"
							+ "PREFIX xml: <https://www.w3.org/XML/1998/namespace>"
							+ "PREFIX dbo: <http://dbpedia.org/ontology/>"
							+ "PREFIX ign: <http://example.com/namespace/>" + "SELECT (AVG(?count) as ?avg) WHERE {"
							+ "SELECT ?s (count(*) as ?count) WHERE {" + "    ?s ?p ?t ."
							+ "    ?t rdf:type dbo:Place ." + "  {" + "SELECT DISTINCT ?s WHERE {"
							+ "	?s rdf:type dbo:Place ." + "    }}" + "} GROUP BY ?s }");
			String avg = RDFUtil.getURIOrLexicalForm(sol.get(0), "avg");
			result = Double.parseDouble(avg);
		} catch (QueryParseException | HttpHostConnectException | RiotException | MalformedURLException
				| HttpException e) {
			logger.error(e);
		}
		return result;
	}

	/**
	 * Gets the sub graph with only the resources (no literals) linked by
	 * prop-fr.nord, prop-fr:sud, etc.
	 *
	 * @param kbSource
	 *            the kb source
	 * @return the sub graph with resources
	 */
	static Model getSubGraphWithResources(Model kbSource) {
		logger.info("Récupération du sous graphe de la base de connaissance.");
		Model model = null;
		try {
			model = RDFUtil.getQueryConstruct(kbSource, "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
					+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"
					+ "PREFIX prop-fr: <http://fr.dbpedia.org/property/>" + "PREFIX foaf: <http://xmlns.com/foaf/0.1/>"
					+ "PREFIX xml: <https://www.w3.org/XML/1998/namespace>"
					+ "PREFIX dbo: <http://dbpedia.org/ontology/>" + "PREFIX ign: <http://example.com/namespace/>"
					+ "CONSTRUCT {?s ?p ?o} WHERE {" + "  ?s ?p ?o."
					+ "  FILTER((?p=prop-fr:nord||?p=prop-fr:nordEst||?p=prop-fr:nordOuest||?p=prop-fr:sud"
					+ "  ||?p=prop-fr:sudEst||?p=prop-fr:sudOuest||?p=prop-fr:est||?p=prop-fr:ouest) && !isLiteral(?o))."
					+ "}", null);
			ResIterator iter = model.listSubjects();
			Set<Resource> resources = new HashSet<>();
			while (iter.hasNext()) {
				Resource r = iter.nextResource();
				resources.add(r);
			}
			NodeIterator nIter = model.listObjects();
			while (nIter.hasNext()) {
				RDFNode rdfNode = nIter.nextNode();
				if (rdfNode.isResource()) {
					Resource r = (Resource) rdfNode;
					if (!r.isAnon()) {
						resources.add(r);
					}
				}
			}
		} catch (QueryParseException | HttpHostConnectException | RiotException | MalformedURLException
				| HttpException e) {
			logger.error(e);
		}
		return model;
	}

	/**
	 * Gets the candidates selection.
	 *
	 * @param toponymsTEI
	 *            the toponyms tei
	 * @param candidatesFromKB
	 *            the candidates from kb
	 * @param threshold
	 *            the threshold
	 * @return the candidates selection
	 */
	static Set<Toponym> getCandidatesSelection(Set<Toponym> toponymsTEI, List<Candidate> candidatesFromKB,
			Integer numberOfCandidate) {
		logger.info("Sélection des candidats (nombre de candidats : " + numberOfCandidate + ")");
		Criterion criterion = Criterion.scoreText;
		Set<Toponym> result = new HashSet<>();
		final AtomicInteger count = new AtomicInteger();
		final int total = toponymsTEI.size();
		StringComparisonDamLev sc = new StringComparisonDamLev();
		toponymsTEI.parallelStream().filter(t -> t != null).forEach(toponym -> {
			candidatesFromKB.parallelStream().filter(c -> c != null && (toponym.getType() == ToponymType.PLACE
					|| typeContained(toponym.getType().toString(), c.getTypes()))).forEach(candidate -> {
						float score1 = sc.computeSimilarity(toponym.getName(), candidate.getName());
						float score2 = sc.computeSimilarity(toponym.getName(), candidate.getName());
						if (Math.max(score1, score2) > 0f)
							toponym.addScoreCriterionToponymCandidate(new CriterionToponymCandidate(toponym, candidate,
									Math.max(score1, score2), criterion));
					});
			if (toponym.getScoreCriterionToponymCandidate() != null
					&& !toponym.getScoreCriterionToponymCandidate().isEmpty()) {
				toponym.clearAndAddAllScoreCriterionToponymCandidate(
						toponym.getScoreCriterionToponymCandidate().stream().filter(s -> s != null)
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
	 * @param typeToCheck
	 *            the type to check
	 * @param types
	 *            the types
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
							+ "  ?s rdfs:label ?label ." + "  ?s ign:id ?id ." + "  ?s rdf:type ?type ." + "}"));
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
					"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
							+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"
							+ "PREFIX prop-fr: <http://fr.dbpedia.org/property/>"
							+ "PREFIX foaf: <http://xmlns.com/foaf/0.1/>"
							+ "PREFIX dbpedia-owl: <http://dbpedia.org/ontology/>" + "" + "SELECT DISTINCT * WHERE {"
							+ "  OPTIONAL { ?s foaf:name ?name . }" + "  OPTIONAL { ?s rdfs:label ?label . }" + "}"));
		} catch (QueryParseException | HttpHostConnectException | MalformedURLException | HttpException e) {
			logger.error(e);
		}
		logger.info("Récupérations des types des candidats de la KB");
		try {
			qSolutionsTypes.addAll(RDFUtil.getQuerySelectResults(kbSourceFinal,
					"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
							+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"
							+ "PREFIX prop-fr: <http://fr.dbpedia.org/property/>"
							+ "PREFIX foaf: <http://xmlns.com/foaf/0.1/>" + "PREFIX dbo: <http://dbpedia.org/ontology/>"
							+ "SELECT * WHERE {" + "  ?s rdf:type ?type . "
							+ "  FILTER (?type=dbo:Mountain || ?type=dbo:Volcano || ?type=dbo:BodyOfWater || "
							+ "?type=dbo:Settlement || ?type=dbo:Territory || ?type=dbo:NaturalPlace) . " + "}"));
		} catch (QueryParseException | HttpHostConnectException | MalformedURLException | HttpException e) {
			logger.error(e);
		}
		logger.info("Traitement des types des candidats de la KB");
		List<String[]> resourceTypeMap = new ArrayList<>();
		qSolutionsTypes.stream().forEach(qs -> {
			String candidateResource = RDFUtil.getURIOrLexicalForm(qs, "s");
			String candidateType = RDFUtil.getURIOrLexicalForm(qs, "type");
			resourceTypeMap.add(new String[] { candidateResource, candidateType });
		});
		Map<String, List<String[]>> typesByResources = resourceTypeMap.stream()
				.collect(Collectors.groupingBy((String[] s) -> s[0]));
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
	static class CustomPropertyComparator implements Comparator<Property>{

	    @Override
	    public int compare(Property p1, Property p2) {
	    	String str1 = p1.getLocalName();
	    	String str2 = p2.getLocalName();
	       // extract numeric portion out of the string and convert them to int
	       // and compare them, roughly something like this

	       int num1 = Integer.parseInt(str1.substring(1));
	       int num2 = Integer.parseInt(str2.substring(1));

	       return num1 - num2;

	    }
	}
	
	static class QuerySolutionEntry {
		private Resource sequence;
		private Resource route;
		private Resource bag;
		private Resource waypoint;
		private Resource spatialReference;
		private Resource spatialReferenceAlt;
		public QuerySolutionEntry(Resource sequence, Resource route, Resource bag, Resource waypoint, Resource spatialReference, Resource spatialReferenceAlt) {
			this.sequence = sequence;
			this.route = route;
			this.bag = bag;
			this.waypoint = waypoint;
			this.spatialReference = spatialReference;
			this.spatialReferenceAlt = spatialReferenceAlt;
		}
		
		public Resource getSequence() {
			return this.sequence;
		}
		public Resource getRoute() {
			return this.route;
		}
		public Resource getBag() {
			return this.bag;
		}
		public Resource getWaypoint() {
			return this.waypoint;
		}
		public Resource getSpatialReference() {
			return this.spatialReference;
		}
		public Resource getSpatialReferenceAlt() {
			return this.spatialReferenceAlt;
		}
	}
}
