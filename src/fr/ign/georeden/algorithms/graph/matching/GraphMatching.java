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
import java.util.Optional;
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
import com.hp.hpl.jena.rdf.model.Alt;
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
				
		List<QuerySolution> querySolutions = getGraphTuples(teiRdf);
		List<QuerySolutionEntry> querySolutionEntries = getQuerySolutionEntries(querySolutions).stream().sorted(comparatorQuerySolutionEntry).collect(Collectors.toList());
		List<Resource> sequences = querySolutionEntries.stream().map(q -> q.getSequence()).distinct()
				//.limit(1)
				.collect(Collectors.toList());
		int v = 0;
		for (Resource sequence : sequences) {
			Model currentModel = getModelsFromSequenceV2(querySolutionEntries, sequence);
			currentModel = oneSensePerDiscourseFusion(currentModel, teiRdf);// ajouter ici la fusion des noeuds identiques (one sence per discourse)
			List<Model> alts = explodeAlts(currentModel);
			if (alts.size() > 1) {
				saveModelToFile("t" + v + "_original.xml", currentModel);
				for (int j = 0; j < alts.size(); j++) {
					saveModelToFile("t" + v + "_" + j + ".xml", alts.get(j));
				}
			}
			v++;
		}
		
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
	
	static Model oneSensePerDiscourseFusion(Model model, Model teiRdf) {
		Model currentModel = cloneModel(model);
		Map<RDFNode, List<Statement>> typesByResources = teiRdf.listStatements(null, rdfsLabel, (RDFNode)null).toList()
			.stream().collect(Collectors.groupingBy((Statement s) -> s.getObject()));
		
		// on ne garde que les resources de la séquence actuelle
		List<Resource> resourcesFromCurrentModel = currentModel.listSubjects().toList();
		resourcesFromCurrentModel.addAll(currentModel.listObjects().toList().stream().map(n -> (Resource)n).collect(Collectors.toList()));
		resourcesFromCurrentModel = resourcesFromCurrentModel.stream().distinct().collect(Collectors.toList());
		final List<Resource> resourcesFromCurrentModel2 = new ArrayList<>(resourcesFromCurrentModel);
		for (RDFNode keyNode : typesByResources.keySet()) {
			List<Statement> oldStatements = typesByResources.get(keyNode);
			oldStatements = oldStatements.stream()
					.filter(s -> resourcesFromCurrentModel2.contains(s.getSubject())).collect(Collectors.toList());
			typesByResources.put(keyNode, oldStatements);
		}
		
		// on supprime toutes les clés avec seulement 1 élément (attention à la gestion des rdf:Alt)
		List<RDFNode> keysToRemove = typesByResources.keySet().stream().filter(k -> {
			List<Statement> statements = typesByResources.get(k);
			// prendre en compte les Alt			
			return statements.size() <= 1 || 
					(statements.size() == 2 
					&& statements.stream().anyMatch(s -> s.getSubject().toString().indexOf('_') > -1));
		}).collect(Collectors.toList());
		for (RDFNode keyToRemove : keysToRemove) {
			typesByResources.remove(keyToRemove);
		}
		if (!typesByResources.isEmpty()) {
			for (RDFNode keyNode : typesByResources.keySet()) {
				List<Resource> resources = typesByResources.get(keyNode).stream().map(m -> m.getSubject()).sorted(comparatorResourcePlace).collect(Collectors.toList());
				if (resources.stream().anyMatch(r -> r.toString().indexOf('_') > -1)) { // partie galère, car contient des Alts
					logger.info("Gérer la fusion avec les alts");
				} else {
					Resource firstPlace = resources.get(0);
					// Pour chaque place, on récupère les statements auxquels elle est liée (sujet ou objet)
					List<Statement> oldAllStatements = new ArrayList<>();
					resources.forEach(p -> oldAllStatements.addAll(getProperties(p, teiRdf)));
					List<Statement> oldStatements = new ArrayList<>(oldAllStatements.stream().filter(s -> s.getPredicate().getNameSpace().equals(ignNS)).collect(Collectors.toList()));					
					// Ensuite on adapte ces statements à la première place
					List<Statement> newStatements = new ArrayList<>();
					for (Statement statement : oldStatements) {
						if (resources.contains(statement.getSubject())) { // on remplace le sujet
							newStatements.add(teiRdf.createStatement(firstPlace, statement.getPredicate(), statement.getObject()));
						} else {
							newStatements.add(teiRdf.createStatement((Resource)statement.getObject(), statement.getPredicate(), firstPlace));
						}
					}
					// On suprime les autres places
					currentModel.remove(oldAllStatements);
					// on insert les statements (la 1ère place remplace les anciennes)
					List<Resource> types = resources.stream().map(place -> (Resource)teiRdf.getProperty(place, rdfType).getObject()).distinct().collect(Collectors.toList());
					types.forEach(logger::info);
				}
			}
			typesByResources.keySet().stream().forEach(rdfNode -> logger.info(typesByResources.get(rdfNode)));
		}
		return currentModel;
	}
	static List<Statement> getProperties(Resource place, Model teiRdf) {
		List<Statement> results = teiRdf.listStatements(place, null, (RDFNode)null).toList();
		results.addAll(teiRdf.listStatements(null, null, (RDFNode)place).toList());
		return results;
	}
	/**
	 * For each rdf:Alt, return the two corresponding models
	 *
	 * @param currentModel the current model
	 * @return the list
	 */
	static List<Model> explodeAlts(Model currentModel) {
		Model currentModelClone = cloneModel(currentModel);
		List<Model> results = new ArrayList<>();
		List<Resource> alts = currentModelClone.listStatements(null, null, currentModelClone.createResource("http://www.w3.org/1999/02/22-rdf-syntax-ns#Alt")).toList().stream()
			.map(s -> s.getSubject()).collect(Collectors.toList());
		results.add(currentModelClone);
		if (!alts.isEmpty()) {
			for (Resource resourceAlt : alts) {
				Alt alt = currentModelClone.getAlt(resourceAlt);
				// certaines rdf:Alt sont en fait identiques, il faut les fusionner. On vérifie si les places de cet Alt ont déjà été traités.
				// on vérifie uniquement ac la 1ère place
				Resource r_1 = (Resource)currentModelClone.getProperty(alt, currentModelClone.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#_1")).getObject();
				if (results.stream().anyMatch(m -> m.contains(r_1, linkSameBag) || m.contains(r_1, linkSameRoute) || m.contains(r_1, linkSameSequence)
						|| m.contains(null, linkSameBag, r_1) || m.contains(null, linkSameRoute, r_1) || m.contains(null, linkSameSequence, r_1))) {
					// cet rdf:Alt est un doublon
					List<Statement> statements = results.get(0).listStatements().toList().stream()
							.filter(p -> (p.getSubject().toString().equals(alt.toString()) 
								|| (p.getObject().isResource() && ((Resource)p.getObject()).toString().equals(alt.toString())))
								&& p.getPredicate().getNameSpace().equals(ignNS)).collect(Collectors.toList());
					List<Resource> places = new ArrayList<>();
					alt.iterator().toList().stream().forEach(m -> places.add((Resource)m));
					for (Resource place : places) {
						List<Statement> currentStatements = new ArrayList<>();
						for (Statement oldStatement : statements) {
							Statement newStatement;
							if (oldStatement.getSubject().getURI() == alt.getURI()) { // alt au début
								newStatement = currentModelClone.createStatement(place, oldStatement.getPredicate(), oldStatement.getObject());
							} else { // alt à la fin
								newStatement = currentModelClone.createStatement(oldStatement.getSubject(), oldStatement.getPredicate(), place);
							}
							currentStatements.add(newStatement);
						}
						for (Model m : results) {
							if (m.contains(place, linkSameBag) || m.contains(place, linkSameRoute) 
									|| m.contains(place, linkSameSequence) || m.contains(null, linkSameBag, place) 
									|| m.contains(null, linkSameRoute, place) || m.contains(null, linkSameSequence, place)) {
								for (Statement statement : currentStatements) {
									m.add(statement);
								}
							}
						}
					}
				} else { // cet rdf:Alt n'est pas un doublon
					List<Resource> places = new ArrayList<>();
					alt.iterator().toList().stream().forEach(m -> places.add((Resource)m));
					List<Statement> statements = results.get(0).listStatements().toList().stream()
							.filter(p -> (p.getSubject().toString().equals(alt.toString()) 
								|| (p.getObject().isResource() && ((Resource)p.getObject()).toString().equals(alt.toString())))
								&& p.getPredicate().getNameSpace().equals(ignNS)).collect(Collectors.toList());
					List<List<Statement>> statementLists = new ArrayList<>(); // on est censé avoir 2 list de statement (utant que de places)
					for (Resource place : places) {
						List<Statement> currentStatements = new ArrayList<>();
						for (Statement oldStatement : statements) {
							Statement newStatement;
							if (oldStatement.getSubject().getURI() == alt.getURI()) { // alt au début
								newStatement = currentModelClone.createStatement(place, oldStatement.getPredicate(), oldStatement.getObject());
							} else { // alt à la fin
								newStatement = currentModelClone.createStatement(oldStatement.getSubject(), oldStatement.getPredicate(), place);
							}
							currentStatements.add(newStatement);
						}
						statementLists.add(currentStatements);
					}
					
					List<List<Model>> resultsList = new ArrayList<>(); // autant de list de model que de places ou de list de statement
					for (int i = 0; i < places.size(); i++) {
						resultsList.add(new ArrayList<>(results));
					}
					results.clear();
					for (int i = 0; i < places.size(); i++) {
						List<Model> list = resultsList.get(i);
						List<Statement> currentStatements = statementLists.get(i);
						for (Model model : list) {
							Model newModel = cloneModel(model);
							for (Statement statement : currentStatements) {
								newModel.add(statement);
							}
							results.add(newModel);	
						}
					}
				}
				for (Model model : results) {
					deleteResource(model, alt);					
				}
			}			
		}
		return results;
	}

	public static void deleteResource(Model model, Resource resource) {
	    // remove statements where resource is subject
	    model.removeAll(resource, null, (RDFNode) null);
	    // remove statements where resource is object
	    model.removeAll(null, null, resource);
	}
	static Comparator<QuerySolutionEntry> comparatorQuerySolutionEntry = (a, b) -> {
		Resource r1 = null;
		if (a.getSpatialReference() != null) {
			r1 = a.getSpatialReference();
		} else {
			r1 = a.getSpatialReferenceAlt();
		}
		Resource r2 = null;
		if (b.getSpatialReference() != null) {
			r2 = b.getSpatialReference();
		} else {
			r2 = b.getSpatialReferenceAlt();
		}
		String subR1 = r1.toString().substring(r1.toString().lastIndexOf('/') + 1);
		String subR2 = r2.toString().substring(r2.toString().lastIndexOf('/') + 1);
		float fR1;
		if (subR1.indexOf('_') != -1) {
			fR1 = Float.parseFloat(subR1.substring(0, subR1.indexOf('_'))) + 0.1f;
		} else {
			fR1 = Float.parseFloat(subR1);
		}
		float fR2;
		if (subR2.indexOf('_') != -1) {
			fR2 = Float.parseFloat(subR2.substring(0, subR2.indexOf('_'))) + 0.1f;
		} else {
			fR2 = Float.parseFloat(subR2);
		}
		return Float.compare(fR1, fR2);
	};
	static Comparator<Resource> comparatorResourcePlace = (r1, r2) -> {
		String subR1 = r1.toString().substring(r1.toString().lastIndexOf('/') + 1);
		String subR2 = r2.toString().substring(r2.toString().lastIndexOf('/') + 1);
		float fR1;
		if (subR1.indexOf('_') != -1) {
			fR1 = Float.parseFloat(subR1.substring(0, subR1.indexOf('_'))) + 0.1f;
		} else {
			fR1 = Float.parseFloat(subR1);
		}
		float fR2;
		if (subR2.indexOf('_') != -1) {
			fR2 = Float.parseFloat(subR2.substring(0, subR2.indexOf('_'))) + 0.1f;
		} else {
			fR2 = Float.parseFloat(subR2);
		}
		return Float.compare(fR1, fR2);
	};
	static String ignNS = "http://example.com/namespace/";
	static String rdfsNS = "http://www.w3.org/2000/01/rdf-schema#";
	static String rdfNS = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	static Property rdfType = ModelFactory.createDefaultModel().createProperty(rdfNS + "type");
	static Property rdfsLabel = ModelFactory.createDefaultModel().createProperty(rdfsNS + "label");
	static Property linkSameRoute = ModelFactory.createDefaultModel().createProperty(ignNS + "linkSameRoute");
	static Property linkSameSequence = ModelFactory.createDefaultModel().createProperty(ignNS + "linkSameSequence");
	static Property linkSameBag = ModelFactory.createDefaultModel().createProperty(ignNS + "linkSameBag"); // SymmetricProperty et transitive
	static List<QuerySolution> getGraphTuples(Model teiModel) {
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
		return querySolutions;
	}
	static List<QuerySolutionEntry> getQuerySolutionEntries(List<QuerySolution> querySolutions) {
		List<QuerySolutionEntry> querySolutionEntries = new ArrayList<>();
		for (QuerySolution querySolution : querySolutions) {
			Resource sequence = (Resource) querySolution.get("sequence");
			Resource route = (Resource) querySolution.get("route");
			Resource bag = (Resource) querySolution.get("bag");
			Resource waypoint = (Resource) querySolution.get("waypoint");
			Resource spatialReference = (Resource) querySolution.get("spatialReference");
			Resource spatialReferenceAlt = (Resource) querySolution.get("spatialReferenceAlt");
			querySolutionEntries.add(new QuerySolutionEntry(sequence, route, bag, waypoint, spatialReference, spatialReferenceAlt));
		}
		return querySolutionEntries;
	}
	static Model getModelsFromSequenceV2(List<QuerySolutionEntry> allQuerySolutionEntries, Resource currentSequence) {
		List<QuerySolutionEntry> querySolutionEntries = allQuerySolutionEntries.stream()
				.filter(q -> q.getSequence() == currentSequence)
				.sorted(comparatorQuerySolutionEntry)
				.collect(Collectors.toList());
		Model initialModel = ModelFactory.createDefaultModel();		
		initialModel.setNsPrefix("ign", "http://example.com/namespace/");
		initialModel = managedBags(initialModel, querySolutionEntries);
		initialModel = managedRoutes(initialModel, querySolutionEntries);
		initialModel = managedSequences(initialModel, querySolutionEntries);
		return initialModel;
	}
	static Model managedBags(Model initialModel, List<QuerySolutionEntry> querySolutionEntries) {
		for (int i = 0; i < querySolutionEntries.size() - 1; i++) {
			QuerySolutionEntry previous = null;
			if (i > 0) {
				previous = querySolutionEntries.get(i - 1);
			}
			QuerySolutionEntry current = querySolutionEntries.get(i);
			QuerySolutionEntry currentAlt = null;
			if (current.getSpatialReferenceAlt() != null) {
				i++;
				currentAlt = querySolutionEntries.get(i);
			}
			int j = i + 1;
			if(i + 1 >= querySolutionEntries.size())
				break;
			QuerySolutionEntry next = querySolutionEntries.get(j);
			QuerySolutionEntry nextAlt = null;
			if (next.getSpatialReferenceAlt() != null) {
				j++;
				nextAlt = querySolutionEntries.get(j);
			}
			if ((previous == null || current.getBag() != previous.getBag()) && current.getBag() == next.getBag()) { 
				// current est le 1er élément d'un bag où il y a plusieurs éléments
				Resource r1;
				if (currentAlt != null) { // current est une Alt
					Alt alt = initialModel.createAlt();
					alt.add(current.getSpatialReferenceAlt());
					alt.add(currentAlt.getSpatialReferenceAlt());
					r1 = alt;
				} else { // current n'est pas une alt
					r1 = current.getSpatialReference();
				}
				while (current.getBag() == next.getBag()) {
					Resource r2;
					if (nextAlt != null) { // next est une Alt
						Alt alt = initialModel.createAlt();
						alt.add(next.getSpatialReferenceAlt());
						alt.add(nextAlt.getSpatialReferenceAlt());
						r2 = alt;
					} else { // next n'est pas une alt
						r2 = next.getSpatialReference();
					}
					initialModel.add(initialModel.createStatement(r1, linkSameBag, r2));
					if (j + 1 < querySolutionEntries.size()) {
						j++;
						next = querySolutionEntries.get(j);
						nextAlt = null;
						if (next.getSpatialReferenceAlt() != null) {
							nextAlt = querySolutionEntries.get(j + 1);
						}
					} else {
						break;
					}
				}
			}
		}
		return initialModel;
	}
	static Model managedRoutes(Model initialModel, List<QuerySolutionEntry> querySolutionEntries) {
		for (int i = 0; i < querySolutionEntries.size() - 1; i++) {
			QuerySolutionEntry current = querySolutionEntries.get(i);
			QuerySolutionEntry currentAlt = null;
			if (current.getSpatialReferenceAlt() != null) {
				i++;
				currentAlt = querySolutionEntries.get(i);
			}
			if (i + 1 >= querySolutionEntries.size())
				break;
			// on récupère les éléments du bag suivant sur la même route
			Optional<Resource> optionalBag = querySolutionEntries.subList(i + 1, querySolutionEntries.size()).stream()
				.filter(p -> p.getRoute() == current.getRoute() && p.getBag() != current.getBag())
				.map(p -> p.getBag()).distinct().findFirst();
			if (optionalBag.isPresent()) {
				List<QuerySolutionEntry> bagElements = querySolutionEntries.stream().filter(p -> p.getBag() == optionalBag.get())
						.sorted(comparatorQuerySolutionEntry).collect(Collectors.toList());
				Resource r1;
				if (currentAlt != null) { // current est une Alt
					Alt alt = initialModel.createAlt();
					alt.add(current.getSpatialReferenceAlt());
					alt.add(currentAlt.getSpatialReferenceAlt());
					r1 = alt;
				} else { // current n'est pas une alt
					r1 = current.getSpatialReference();
				}
				for (int j = 0; j < bagElements.size(); j++) {
					QuerySolutionEntry next = bagElements.get(j);
					QuerySolutionEntry nextAlt = null;
					if (next.getSpatialReferenceAlt() != null) {
						j++;
						nextAlt = bagElements.get(j);
					}
					Resource r2;
					if (nextAlt != null) { // next est une Alt
						Alt alt = initialModel.createAlt();
						alt.add(next.getSpatialReferenceAlt());
						alt.add(nextAlt.getSpatialReferenceAlt());
						r2 = alt;
					} else { // next n'est pas une alt
						r2 = next.getSpatialReference();
					}
					initialModel.add(initialModel.createStatement(r1, linkSameRoute, r2));
				}
			}			
		}
		return initialModel;
	}

	static Model managedSequences(Model initialModel, List<QuerySolutionEntry> querySolutionEntries) {
		for (int i = 0; i < querySolutionEntries.size() - 1; i++) {
			QuerySolutionEntry current = querySolutionEntries.get(i);
			if (current.getSpatialReferenceAlt() != null) {
				i++;
			}
			if (i + 1 >= querySolutionEntries.size())
				break;
			// on vérifie que le bag est le dernier de la route
			QuerySolutionEntry next = querySolutionEntries.get(i + 1);
			if (next.getRoute() != current.getRoute()) {
				Resource lastBagOfCurrentRoute = current.getBag();
				Resource firstBagOfLastRoute = next.getBag();
				List<QuerySolutionEntry> lastBagElements = querySolutionEntries.stream().filter(p -> p.getBag() == lastBagOfCurrentRoute).sorted(comparatorQuerySolutionEntry).collect(Collectors.toList());
				List<QuerySolutionEntry> firstBagElements = querySolutionEntries.stream().filter(p -> p.getBag() == firstBagOfLastRoute).sorted(comparatorQuerySolutionEntry).collect(Collectors.toList());
				for (int j = 0; j < lastBagElements.size(); j++) {
					current = lastBagElements.get(j);
					QuerySolutionEntry currentAlt = null;
					if (current.getSpatialReferenceAlt() != null) {
						j++;
						currentAlt = lastBagElements.get(j);
					}
					Resource r1;
					if (currentAlt != null) { // current est une Alt
						Alt alt = initialModel.createAlt();
						alt.add(current.getSpatialReferenceAlt());
						alt.add(currentAlt.getSpatialReferenceAlt());
						r1 = alt;
					} else { // current n'est pas une alt
						r1 = current.getSpatialReference();
					}
					for (int k = 0; k < firstBagElements.size(); k++) {
						next = firstBagElements.get(k);
						QuerySolutionEntry nextAlt = null;
						if (next.getSpatialReferenceAlt() != null && k < firstBagElements.size()) {
							k++;
							nextAlt = firstBagElements.get(k);
						}
						Resource r2;
						if (nextAlt != null) { // next est une Alt
							Alt alt = initialModel.createAlt();
							alt.add(next.getSpatialReferenceAlt());
							alt.add(nextAlt.getSpatialReferenceAlt());
							r2 = alt;
						} else { // next n'est pas une alt
							r2 = next.getSpatialReference();
						}
						initialModel.add(initialModel.createStatement(r1, linkSameSequence, r2));
					}
				}
			}	
		}
		return initialModel;
	}
	
	
	
	static Model cloneModel(Model original) {
		Model newModel = ModelFactory.createDefaultModel();
		StmtIterator iterator = original.listStatements();
		while (iterator.hasNext()) {
			Statement statement = (Statement) iterator.next();
			newModel.add(statement);
		}
		original.getNsPrefixMap().keySet().forEach(prefix -> newModel.setNsPrefix(prefix, original.getNsPrefixMap().get(prefix)));
		return newModel;
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
	static class CustomResourceComparator implements Comparator<Resource>{

	    @Override
	    public int compare(Resource p1, Resource p2) {
	    	String str1 = p1.toString();
	    	String str2 = p2.toString();

	       return str1.compareTo(str2);

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
