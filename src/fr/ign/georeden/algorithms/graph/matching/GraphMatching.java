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
			List<Model> alts = explodeAlts(currentModel);
			if (alts.size() > 1) {
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
	static List<Model> explodeAlts(Model currentModel) {
		List<Model> results = new ArrayList<>();
		List<Resource> alts = currentModel.listStatements(null, null, currentModel.createResource("http://www.w3.org/1999/02/22-rdf-syntax-ns#Alt")).toList().stream()
			.map(s -> s.getSubject()).collect(Collectors.toList());
		results.add(currentModel);
		if (!alts.isEmpty()) {
			logger.info("Nb alts : " + alts.size());
			for (Resource resourceAlt : alts) {
				Alt alt = currentModel.getAlt(resourceAlt);
				List<Resource> places = new ArrayList<>();
				alt.iterator().toList().stream().forEach(m -> places.add((Resource)m));
				List<Statement> statements = results.get(0).listStatements().toList().stream()
						.filter(p -> (p.getSubject().getURI() == alt.getURI() 
							|| (p.getObject().isResource() && ((Resource)p.getObject()).getURI() == alt.getURI()))
							&& p.getPredicate().getNameSpace().equals(ignNS)).collect(Collectors.toList());
				for (Model model : results) {
					// remove statements where resource is subject
					model.removeAll(alt, null, (RDFNode) null);
				    // remove statements where resource is object
					model.removeAll(null, null, alt);					
				}
				List<Model> tmp = new ArrayList<>();
				for (Resource place : places) {
					List<Model> resultTmp = new ArrayList<>(results);
					for (Statement oldStatement : statements) {
						Statement newStatement;
						if (oldStatement.getSubject().getURI() == alt.getURI()) { // alt au début
							newStatement = currentModel.createStatement(place, oldStatement.getPredicate(), oldStatement.getObject());
						} else { // alt à la fin
							newStatement = currentModel.createStatement(oldStatement.getSubject(), oldStatement.getPredicate(), place);
						}
						resultTmp.forEach(m -> {
							Model newModel = cloneModel(m);
							newModel.add(newStatement);
							tmp.add(newModel);
						});
					}
				}
				results.clear();
				results.addAll(tmp);
				logger.info("Nb places : " + places.size());
			}
		}
		return results;
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
static String ignNS = "http://example.com/namespace/";
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
		saveModelToFile("testStatements" + counter + ".xml", initialModel);
		counter++;
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
	/**
	 * Generate the models (mini graphs) from a sequence.
	 *
	 * @param teiModel the tei model
	 * @param allQuerySolutionEntries the all query solution entries
	 * @param currentSequence the current sequence
	 * @return the models from sequence
	 */
	static List<Model> getModelsFromSequence(Model teiModel, List<QuerySolutionEntry> allQuerySolutionEntries, Resource currentSequence) {
		List<QuerySolutionEntry> querySolutionEntries = allQuerySolutionEntries.stream()
				.filter(q -> q.getSequence() == currentSequence)
				.sorted(comparatorQuerySolutionEntry)
				.collect(Collectors.toList());
		List<Model> results = new ArrayList<>();
		Model initialModel = ModelFactory.createDefaultModel();		
		initialModel.setNsPrefix("ign", "http://example.com/namespace/");
		results.add(initialModel);
		// liens dans les bags
		Map<Resource, List<QuerySolutionEntry>> byBag = querySolutionEntries.stream()
				.collect(Collectors.groupingBy(QuerySolutionEntry::getBag));
		byBag.keySet().stream().filter(key -> byBag.get(key).size() >= 2)
			.forEach(key -> {
				List<QuerySolutionEntry> currentBag = byBag.get(key).stream().sorted(comparatorQuerySolutionEntry).collect(Collectors.toList());
				QuerySolutionEntry bagElement = currentBag.get(0);
				if (bagElement.getSpatialReference() != null) { // 1er élément n'est pas une Alt
					for (int i = 1; i < currentBag.size(); i++) {
						QuerySolutionEntry nextBagElement = currentBag.get(i);
						if (nextBagElement.getSpatialReference() != null) {// 2e élément n'est pas une Alt
							Statement s = initialModel.createStatement(bagElement.getSpatialReference(), linkSameBag, nextBagElement.getSpatialReference());
							for (Model currentModel : results) {
								currentModel.add(s);
							}
						} else {// 2e élément est une Alt
							Statement s1 = initialModel.createStatement(bagElement.getSpatialReference(), linkSameBag, nextBagElement.getSpatialReferenceAlt());
							i++;
							QuerySolutionEntry nextNextBagElement = currentBag.get(i);
							Statement s2 = initialModel.createStatement(bagElement.getSpatialReference(), linkSameBag, nextNextBagElement.getSpatialReferenceAlt());
							List<Model> results1 = new ArrayList<>(results);
							List<Model> results2 = new ArrayList<>(results);
							results.clear();
							for (Model currentModel : results1) {
								Model newModel = cloneModel(currentModel);
								newModel.add(s1);
								results.add(newModel);									
							}
							for (Model currentModel : results2) {
								Model newModel = cloneModel(currentModel);
								newModel.add(s2);
								results.add(newModel);
							}
						}
					}
				} else if (currentBag.size() > 2) { // 1er élément est une Alt
					logger.info("alt");
					QuerySolutionEntry bagElementBis = currentBag.get(1);
					for (int i = 2; i < currentBag.size(); i++) {
						QuerySolutionEntry nextBagElement = currentBag.get(i);
						if (nextBagElement.getSpatialReference() != null) {// 2e élément n'est pas une Alt
							Statement s1 = initialModel.createStatement(bagElement.getSpatialReferenceAlt(), linkSameBag, nextBagElement.getSpatialReference());
							Statement s2 = initialModel.createStatement(bagElementBis.getSpatialReferenceAlt(), linkSameBag, nextBagElement.getSpatialReference());
							List<Model> results1 = new ArrayList<>(results);
							List<Model> results2 = new ArrayList<>(results);
							results.clear();
							for (Model currentModel : results1) {
								Model newModel = cloneModel(currentModel);
								newModel.add(s1);
								results.add(newModel);									
							}
							for (Model currentModel : results2) {
								Model newModel = cloneModel(currentModel);
								newModel.add(s2);
								results.add(newModel);
							}
						} else {// 2e élément est une Alt
							Statement s1 = initialModel.createStatement(bagElement.getSpatialReferenceAlt(), linkSameBag, nextBagElement.getSpatialReferenceAlt());
							Statement s2 = initialModel.createStatement(bagElementBis.getSpatialReferenceAlt(), linkSameBag, nextBagElement.getSpatialReferenceAlt());
							i++;
							QuerySolutionEntry nextNextBagElement = currentBag.get(i);
							Statement s3 = initialModel.createStatement(bagElement.getSpatialReferenceAlt(), linkSameBag, nextNextBagElement.getSpatialReferenceAlt());
							Statement s4 = initialModel.createStatement(bagElementBis.getSpatialReferenceAlt(), linkSameBag, nextNextBagElement.getSpatialReferenceAlt());
							List<Model> results1 = new ArrayList<>(results);
							List<Model> results2 = new ArrayList<>(results);
							List<Model> results3 = new ArrayList<>(results);
							List<Model> results4 = new ArrayList<>(results);
							logger.info("counter : " + counter);
							results.clear();
							for (Model currentModel : results1) {
								Model newModel = cloneModel(currentModel);
								newModel.add(s1);
								results.add(newModel);									
							}
							for (Model currentModel : results2) {
								Model newModel = cloneModel(currentModel);
								newModel.add(s2);
								results.add(newModel);
							}
							for (Model currentModel : results3) {
								Model newModel = cloneModel(currentModel);
								newModel.add(s3);
								results.add(newModel);									
							}
							for (Model currentModel : results4) {
								Model newModel = cloneModel(currentModel);
								newModel.add(s4);
								results.add(newModel);
							}
						}
					}
				}
			});
		
		// liens dans les séquences
		Map<Resource, List<QuerySolutionEntry>> byRoute = querySolutionEntries.stream()
				.collect(Collectors.groupingBy(QuerySolutionEntry::getRoute));
		List<Resource> routeKeys = querySolutionEntries.stream().map(q -> q.getRoute()).distinct().collect(Collectors.toList());
		for (int i = 0; i < routeKeys.size() - 1; i++) {
			List<QuerySolutionEntry> firstRoute = byRoute.get(routeKeys.get(i)).stream().sorted(comparatorQuerySolutionEntry.reversed()).collect(Collectors.toList());
			List<QuerySolutionEntry> nextRoute = byRoute.get(routeKeys.get(i + 1)).stream().sorted(comparatorQuerySolutionEntry).collect(Collectors.toList());
			Map<Resource, List<QuerySolutionEntry>> firstRouteBags = firstRoute.stream().collect(Collectors.groupingBy(QuerySolutionEntry::getBag));
			Map<Resource, List<QuerySolutionEntry>> nextRouteBags = nextRoute.stream().collect(Collectors.groupingBy(QuerySolutionEntry::getBag));
			List<QuerySolutionEntry> firstRouteLastBagContent = firstRouteBags.get(firstRoute.get(0).getBag()).stream().sorted(comparatorQuerySolutionEntry).collect(Collectors.toList());
			List<QuerySolutionEntry> nextRouteBagsFirstBagContent = nextRouteBags.get(nextRoute.get(0).getBag()).stream().sorted(comparatorQuerySolutionEntry).collect(Collectors.toList());
			for (int j = 0; j < firstRouteLastBagContent.size(); j++) {
				QuerySolutionEntry b1 = firstRouteLastBagContent.get(j);
				for (int k = 0; k < nextRouteBagsFirstBagContent.size(); k++) {
					QuerySolutionEntry b2 = nextRouteBagsFirstBagContent.get(k);
					if (b1.getSpatialReference() != null && b2.getSpatialReference() != null) { // pas d'Alt
						Statement s = initialModel.createStatement(b1.getSpatialReference(), linkSameSequence, b2.getSpatialReference());
						for (Model model : results) {
							model.add(s);
						}
					} else if (b1.getSpatialReference() != null) { // b2 est une Alt
						k++;
						QuerySolutionEntry b22 = nextRouteBagsFirstBagContent.get(k);
						Statement s1 = initialModel.createStatement(b1.getSpatialReference(), linkSameBag, b2.getSpatialReferenceAlt());
						Statement s2 = initialModel.createStatement(b1.getSpatialReference(), linkSameBag, b22.getSpatialReferenceAlt());
						if (results.stream().anyMatch(m -> m.containsResource(b2.getSpatialReferenceAlt()))) {
							// b2 a déjà été ajouté, on ajoute donc la relation que là où il apparait (pas besoin de dupliquer)
							for (Model currentModel : results) {
								if (currentModel.containsResource(b2.getSpatialReferenceAlt()))
									currentModel.add(s1);
								else if (currentModel.containsResource(b22.getSpatialReferenceAlt()))
									currentModel.add(s2);							
							}
						} else {
							List<Model> results1 = new ArrayList<>(results);
							List<Model> results2 = new ArrayList<>(results);
							results.clear();
							for (Model currentModel : results1) {
								Model newModel = cloneModel(currentModel);
								newModel.add(s1);
								results.add(newModel);									
							}
							for (Model currentModel : results2) {
								Model newModel = cloneModel(currentModel);
								newModel.add(s2);
								results.add(newModel);
							}
						}
					} else  if (b2.getSpatialReference() != null && j < firstRouteLastBagContent.size() - 1) { // b1 est une Alt
						j++;
						QuerySolutionEntry b12 = firstRouteLastBagContent.get(j);
						Statement s1 = initialModel.createStatement(b1.getSpatialReferenceAlt(), linkSameBag, b2.getSpatialReference());
						Statement s2 = initialModel.createStatement(b12.getSpatialReferenceAlt(), linkSameBag, b2.getSpatialReference());
						if (results.stream().anyMatch(m -> m.containsResource(b1.getSpatialReferenceAlt()))) {
							// b1 a déjà été ajouté, on ajoute donc la relation que là où il apparait (pas besoin de dupliquer)
							for (Model currentModel : results) {
								if (currentModel.containsResource(b1.getSpatialReferenceAlt()))
									currentModel.add(s1);
								else if (currentModel.containsResource(b12.getSpatialReferenceAlt()))
									currentModel.add(s2);								
							}
						} else {
							List<Model> results1 = new ArrayList<>(results);
							List<Model> results2 = new ArrayList<>(results);
							results.clear();
							for (Model currentModel : results1) {
								Model newModel = cloneModel(currentModel);
								newModel.add(s1);
								results.add(newModel);									
							}
							for (Model currentModel : results2) {
								Model newModel = cloneModel(currentModel);
								newModel.add(s2);
								results.add(newModel);
							}
						}
					} else if (j < firstRouteLastBagContent.size() - 1) { // les 2 sont des alt
						logger.info("double alt");
						j++;
						QuerySolutionEntry b12 = firstRouteLastBagContent.get(j);
						k++;
						QuerySolutionEntry b22 = nextRouteBagsFirstBagContent.get(k);
						Statement s1 = initialModel.createStatement(b1.getSpatialReferenceAlt(), linkSameBag, b2.getSpatialReferenceAlt());
						Statement s2 = initialModel.createStatement(b12.getSpatialReferenceAlt(), linkSameBag, b2.getSpatialReferenceAlt());
						Statement s3 = initialModel.createStatement(b1.getSpatialReferenceAlt(), linkSameBag, b22.getSpatialReferenceAlt());
						Statement s4 = initialModel.createStatement(b12.getSpatialReferenceAlt(), linkSameBag, b22.getSpatialReferenceAlt());
						if (results.stream().anyMatch(m -> m.containsResource(b1.getSpatialReferenceAlt()))) {
							// b1 a déjà été ajouté, on ajoute donc la relation que là où il apparait (pas besoin de dupliquer)
							if (results.stream().anyMatch(m -> m.containsResource(b2.getSpatialReferenceAlt()))) {
								// b2 a déjà été ajouté, on ajoute donc la relation que là où il apparait (pas besoin de dupliquer)
								for (Model currentModel : results) {
									if (currentModel.containsResource(b1.getSpatialReferenceAlt())) {
										if (currentModel.containsResource(b2.getSpatialReferenceAlt())) {
											currentModel.add(s1);
										}
										else {
											currentModel.add(s3);
										}
									}
									else if (currentModel.containsResource(b12.getSpatialReferenceAlt())) {
										if (currentModel.containsResource(b2.getSpatialReferenceAlt())) {
											currentModel.add(s2);
										}
										else {
											currentModel.add(s4);
										}								
									}
								}
							} else {
								// dupliquer uniquement b2
								List<Model> results1 = new ArrayList<>(results);
								List<Model> results2 = new ArrayList<>(results);
								results.clear();
								for (Model currentModel : results1) {
									Model newModel = cloneModel(currentModel);
									if (currentModel.containsResource(b1.getSpatialReferenceAlt()))
										newModel.add(s1);
									else {
										newModel.add(s2);
									}
									results.add(newModel);									
								}
								for (Model currentModel : results2) {
									Model newModel = cloneModel(currentModel);
									if (currentModel.containsResource(b1.getSpatialReferenceAlt()))
										newModel.add(s3);
									else {
										newModel.add(s4);
									}
									results.add(newModel);
								}
							}
						} else {
							// il faut dupliquer les 2
							List<Model> results1 = new ArrayList<>(results);
							List<Model> results2 = new ArrayList<>(results);
							List<Model> results3 = new ArrayList<>(results);
							List<Model> results4 = new ArrayList<>(results);
							results.clear();
							for (Model currentModel : results1) {
								Model newModel = cloneModel(currentModel);
								newModel.add(s1);
								results.add(newModel);									
							}
							for (Model currentModel : results2) {
								Model newModel = cloneModel(currentModel);
								newModel.add(s2);
								results.add(newModel);
							}
							for (Model currentModel : results3) {
								Model newModel = cloneModel(currentModel);
								newModel.add(s3);
								results.add(newModel);									
							}
							for (Model currentModel : results4) {
								Model newModel = cloneModel(currentModel);
								newModel.add(s4);
								results.add(newModel);
							}
						}
					}
				}
			}
		}
		
		// route
		byRoute.keySet().stream().forEach(keyRoute -> {
			List<QuerySolutionEntry> routeElements = byRoute.get(keyRoute).stream().sorted(comparatorQuerySolutionEntry).collect(Collectors.toList());
			List<Resource> orderedBags = routeElements.stream().map(m -> m.getBag()).distinct().collect(Collectors.toList());
			for (int i = 0; i < orderedBags.size() - 1; i++) {
				final int index = i;
				List<QuerySolutionEntry> firstBagContent = routeElements.stream().filter(p -> p.getBag() == orderedBags.get(index)).sorted(comparatorQuerySolutionEntry).collect(Collectors.toList());
				List<QuerySolutionEntry> nextBagContent = routeElements.stream().filter(p -> p.getBag() == orderedBags.get(index + 1)).sorted(comparatorQuerySolutionEntry).collect(Collectors.toList());
				for (int j = 0; j < firstBagContent.size(); j++) {
					QuerySolutionEntry bag1El = firstBagContent.get(j);
					for (int k = 0; k < nextBagContent.size(); k++) {
						QuerySolutionEntry bag2El = nextBagContent.get(k);
						if (bag1El.getSpatialReference() != null) { // bag1El n'est pas une Alt
							if (bag2El.getSpatialReference() != null) { // bag2El n'est pas une Alt
								Statement s = initialModel.createStatement(bag1El.getSpatialReference(), linkSameRoute,bag2El.getSpatialReference());
								for (Model model : results) {
									model.add(s);
								}
							} else { // bag2El est une Alt
								k++;
								QuerySolutionEntry bag2El2 = nextBagContent.get(k);
								Statement s1 = initialModel.createStatement(bag1El.getSpatialReference(), linkSameRoute,bag2El.getSpatialReferenceAlt());
								Statement s2 = initialModel.createStatement(bag1El.getSpatialReference(), linkSameRoute,bag2El2.getSpatialReferenceAlt());
								List<Model> results1 = new ArrayList<>(results);
								List<Model> results2 = new ArrayList<>(results);
								results.clear();
								for (Model currentModel : results1) {
									Model newModel = cloneModel(currentModel);
									newModel.add(s1);
									results.add(newModel);									
								}
								for (Model currentModel : results2) {
									Model newModel = cloneModel(currentModel);
									newModel.add(s2);
									results.add(newModel);
								}
							}
						} else if (j < firstBagContent.size() - 1) { // bag1El est une Alt
							j++;
							QuerySolutionEntry bag1El2 = firstBagContent.get(j);
							if (bag2El.getSpatialReference() != null) { // bag2El n'est pas une Alt
								Statement s1 = initialModel.createStatement(bag1El.getSpatialReferenceAlt(), linkSameRoute,bag2El.getSpatialReference());
								Statement s2 = initialModel.createStatement(bag1El2.getSpatialReferenceAlt(), linkSameRoute,bag2El.getSpatialReference());
								List<Model> results1 = new ArrayList<>(results);
								List<Model> results2 = new ArrayList<>(results);
								results.clear();
								for (Model currentModel : results1) {
									Model newModel = cloneModel(currentModel);
									newModel.add(s1);
									results.add(newModel);									
								}
								for (Model currentModel : results2) {
									Model newModel = cloneModel(currentModel);
									newModel.add(s2);
									results.add(newModel);
								}
							} else { // bag2El est une Alt
								logger.info("double alt encore");
//								k++;
//								QuerySolutionEntry bag2El2 = nextBagContent.get(k);
//								Statement s1 = initialModel.createStatement(bag1El.getSpatialReferenceAlt(), linkSameRoute,bag2El.getSpatialReferenceAlt());
//								Statement s3 = initialModel.createStatement(bag1El.getSpatialReferenceAlt(), linkSameRoute,bag2El2.getSpatialReferenceAlt());
//								Statement s2 = initialModel.createStatement(bag1El2.getSpatialReferenceAlt(), linkSameRoute,bag2El.getSpatialReferenceAlt());
//								Statement s4 = initialModel.createStatement(bag1El2.getSpatialReferenceAlt(), linkSameRoute,bag2El2.getSpatialReferenceAlt());
//								if (results.stream().anyMatch(m -> m.containsResource(bag1El.getSpatialReferenceAlt()))) {
//									// b1 a déjà été ajouté, on ajoute donc la relation que là où il apparait (pas besoin de dupliquer)
//									if (results.stream().anyMatch(m -> m.containsResource(bag2El.getSpatialReferenceAlt()))) {
//										// b2 a déjà été ajouté, on ajoute donc la relation que là où il apparait (pas besoin de dupliquer)
//										for (Model currentModel : results) {
//											if (currentModel.containsResource(bag1El.getSpatialReferenceAlt())) {
//												if (currentModel.containsResource(bag2El.getSpatialReferenceAlt())) {
//													currentModel.add(s1);
//												}
//												else {
//													currentModel.add(s3);
//												}
//											}
//											else if (currentModel.containsResource(bag1El2.getSpatialReferenceAlt())) {
//												if (currentModel.containsResource(bag2El.getSpatialReferenceAlt())) {
//													currentModel.add(s2);
//												}
//												else {
//													currentModel.add(s4);
//												}								
//											}
//										}
//									} else {
//										// dupliquer uniquement b2
//										List<Model> results1 = new ArrayList<>(results);
//										List<Model> results2 = new ArrayList<>(results);
//										results.clear();
//										for (Model currentModel : results1) {
//											Model newModel = cloneModel(currentModel);
//											if (currentModel.containsResource(bag1El.getSpatialReferenceAlt()))
//												newModel.add(s1);
//											else {
//												newModel.add(s2);
//											}
//											results.add(newModel);									
//										}
//										for (Model currentModel : results2) {
//											Model newModel = cloneModel(currentModel);
//											if (currentModel.containsResource(bag1El.getSpatialReferenceAlt()))
//												newModel.add(s3);
//											else {
//												newModel.add(s4);
//											}
//											results.add(newModel);
//										}
//									}
//								} else {
//									// il faut dupliquer les 2
//									List<Model> results1 = new ArrayList<>(results);
//									List<Model> results2 = new ArrayList<>(results);
//									List<Model> results3 = new ArrayList<>(results);
//									List<Model> results4 = new ArrayList<>(results);
//									results.clear();
//									for (Model currentModel : results1) {
//										Model newModel = cloneModel(currentModel);
//										newModel.add(s1);
//										results.add(newModel);									
//									}
//									for (Model currentModel : results2) {
//										Model newModel = cloneModel(currentModel);
//										newModel.add(s2);
//										results.add(newModel);
//									}
//									for (Model currentModel : results3) {
//										Model newModel = cloneModel(currentModel);
//										newModel.add(s3);
//										results.add(newModel);									
//									}
//									for (Model currentModel : results4) {
//										Model newModel = cloneModel(currentModel);
//										newModel.add(s4);
//										results.add(newModel);
//									}
//								}
////								List<Model> results1 = new ArrayList<>(results);
////								List<Model> results2 = new ArrayList<>(results);
////								List<Model> results3 = new ArrayList<>(results);
////								List<Model> results4 = new ArrayList<>(results);
////								results.clear();
////								for (Model currentModel : results1) {
////									Model newModel = cloneModel(currentModel);
////									newModel.add(s1);
////									results.add(newModel);									
////								}
////								for (Model currentModel : results2) {
////									Model newModel = cloneModel(currentModel);
////									newModel.add(s2);
////									results.add(newModel);
////								}
////								for (Model currentModel : results3) {
////									Model newModel = cloneModel(currentModel);
////									newModel.add(s3);
////									results.add(newModel);									
////								}
////								for (Model currentModel : results4) {
////									Model newModel = cloneModel(currentModel);
////									newModel.add(s4);
////									results.add(newModel);
////								}
							}
						}
					}
				}
			}
		});
		
		
//		Map<Resource, List<QuerySolutionEntry>> byRoute = querySolutionEntries.stream()
//				.collect(Collectors.groupingBy(QuerySolutionEntry::getRoute));
//		byRoute.keySet().stream().filter(key -> byRoute.get(key).stream().map(e -> e.getBag()).distinct().count() >= 2)
//		.forEach(key -> {
//			List<QuerySolutionEntry> entries = byRoute.get(key); // tous les éléments de entries appartiennent à la même route
//			Map<Resource, List<QuerySolutionEntry>> byRouteAndBag = entries.stream().collect(Collectors.groupingBy(QuerySolutionEntry::getBag));
//			for (int i = 0; i < byRouteAndBag.size() - 1; i++) {
//				List<QuerySolutionEntry> querySolutionEntry1 = byRouteAndBag.get(byRouteAndBag.keySet()
//						.stream().sorted(new CustomResourceComparator()).toArray()[i]);
//				List<QuerySolutionEntry> querySolutionEntry2 = byRouteAndBag.get(byRouteAndBag.keySet()
//						.stream().sorted(new CustomResourceComparator()).toArray()[i + 1]);
//				for (int k = 0; k < querySolutionEntry1.size(); k++) {
//					for (int j = 0; j < querySolutionEntry2.size(); j++) {
//						QuerySolutionEntry el1 = querySolutionEntry1.get(k);
//						QuerySolutionEntry el2 = querySolutionEntry2.get(j);
//						if (el1.getSpatialReference() != null && el2.getSpatialReference() != null) {
//							Statement s = currentModel.createStatement(el1.getSpatialReference(), linkSameRoute, el2.getSpatialReference());
//							currentModel.add(s);
//						}
//					}
//				}
//			}
//		});
//		// liens dans sequence
//		for (int i = 0; i < byRoute.size() - 1; i++) {
//			// on lie les éléments du dernier bag d'une route avec les éléments du 1er bag de la route suivante
//			List<QuerySolutionEntry> route1 = byRoute.get(byRoute.keySet().stream()
//					.sorted(new CustomResourceComparator()).toArray()[i]);
//			List<QuerySolutionEntry> route2 = byRoute.get(byRoute.keySet().stream()
//					.sorted(new CustomResourceComparator()).toArray()[i + 1]);
//			Map<Resource, List<QuerySolutionEntry>> byRouteAndBag1 = route1.stream()
//					.collect(Collectors.groupingBy(QuerySolutionEntry::getBag));
//			Map<Resource, List<QuerySolutionEntry>> byRouteAndBag2 = route2.stream()
//					.collect(Collectors.groupingBy(QuerySolutionEntry::getBag));
//			List<QuerySolutionEntry> elementsOfLastBagOfRoute1 = byRouteAndBag1.get(byRouteAndBag1.keySet().stream().sorted(new CustomResourceComparator()).toArray()[byRouteAndBag1.keySet().size() - 1]);
//			List<QuerySolutionEntry> elementsOfFirstBagOfRoute2 = byRouteAndBag2.get(byRouteAndBag2.keySet().stream().sorted(new CustomResourceComparator()).toArray()[0]);
//			for (int j = 0; j < elementsOfLastBagOfRoute1.size(); j++) {
//				for (int k = 0; k < elementsOfFirstBagOfRoute2.size(); k++) {
//					QuerySolutionEntry el1 = elementsOfLastBagOfRoute1.get(j);
//					QuerySolutionEntry el2 = elementsOfFirstBagOfRoute2.get(k);
//					if (el1.getSpatialReference() != null && el2.getSpatialReference() != null) {
//						Statement s = currentModel.createStatement(el1.getSpatialReference(), linkSameSequence, el2.getSpatialReference());
//						currentModel.add(s);
//					}
//				}
//			}
//		}
		int counterIntern = 0;
		for (Model model : results) {
			saveModelToFile("testStatements" + counter + "_" + counterIntern + ".xml", model);
			counterIntern++;
		}
		if (results.size() > 1) {
			logger.info("taille : " + counter);
		}
		counter++;
		return results.stream().filter(m -> !m.isEmpty()).collect(Collectors.toList());
	}
	static int counter = 0;
	static List<Model> createStatements(Resource lastUsedBag, Property p, Resource bag, List<Model> results) {
		
		return results;
	}
	
	static Model cloneModel(Model original) {
		Model newModel = ModelFactory.createDefaultModel();
		StmtIterator iterator = original.listStatements();
		while (iterator.hasNext()) {
			Statement statement = (Statement) iterator.next();
			newModel.add(statement);
		}
		return newModel;
	}
	
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
