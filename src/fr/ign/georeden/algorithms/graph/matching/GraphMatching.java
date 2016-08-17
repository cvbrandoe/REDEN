package fr.ign.georeden.algorithms.graph.matching;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.naming.spi.DirStateFactory.Result;

import org.apache.http.conn.HttpHostConnectException;
import org.apache.jena.atlas.lib.NotImplemented;
import org.apache.jena.atlas.web.HttpException;
import org.apache.jena.base.Sys;
import org.apache.jena.riot.RiotException;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import org.apache.jena.graph.Graph;
import org.apache.jena.ontology.OntTools;
import org.apache.jena.ontology.OntTools.Path;
import org.apache.jena.ontology.OntTools.PredicatesFilter;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Alt;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.sparql.function.library.e;
import org.apache.jena.util.iterator.Filter;

import fr.ign.georeden.algorithms.string.DamerauLevenshteinAlgorithm;
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

	public static final String TEI_PATH = "D:\\temp7.rdf";
	public static final String dbPediaRdfFilePath = "D:\\dbpedia_fr_with_rlsp.n3";
	public static final String serializePath = "D:\\serializations\\";

	static Comparator<QuerySolutionEntry> comparatorQuerySolutionEntry = (a, b) -> {
		// return Integer.compare(a.getId(), b.getId());
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
	static Integer timeout = 3; // secondes
	static String ignNS = "http://example.com/namespace/";
	static String rdfsNS = "http://www.w3.org/2000/01/rdf-schema#";
	static String rdfNS = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	static String propFrNS = "http://fr.dbpedia.org/property/";
	static String rlspNS = "http://data.ign.fr/def/relationsspatiales#";
	static String dboNS = "http://dbpedia.org/ontology/";
	static String geoNS = "http://www.w3.org/2003/01/geo/wgs84_pos#";
	static Property rdfType = ModelFactory.createDefaultModel().createProperty(rdfNS + "type");
	static Property rdfsLabel = ModelFactory.createDefaultModel().createProperty(rdfsNS + "label");
	static Property linkSameRoute = ModelFactory.createDefaultModel().createProperty(ignNS + "linkSameRoute");
	static Property linkSameSequence = ModelFactory.createDefaultModel().createProperty(ignNS + "linkSameSequence");
	static Property linkSameBag = ModelFactory.createDefaultModel().createProperty(ignNS + "linkSameBag"); // SymmetricProperty
																											// et
																											// transitive
	static Property propNord = ModelFactory.createDefaultModel().createProperty(propFrNS + "nord");
	static Property propNordEst = ModelFactory.createDefaultModel().createProperty(propFrNS + "nordEst");
	static Property propNordOuest = ModelFactory.createDefaultModel().createProperty(propFrNS + "nordOuest");
	static Property propSud = ModelFactory.createDefaultModel().createProperty(propFrNS + "sud");
	static Property propSudEst = ModelFactory.createDefaultModel().createProperty(propFrNS + "sudEst");
	static Property propSudOuest = ModelFactory.createDefaultModel().createProperty(propFrNS + "sudOuest");
	static Property propEst = ModelFactory.createDefaultModel().createProperty(propFrNS + "est");
	static Property propOuest = ModelFactory.createDefaultModel().createProperty(propFrNS + "ouest");
	static Property rlspNorthOf = ModelFactory.createDefaultModel().createProperty(rlspNS + "northOf");
	static Property rlspNorthEastOf = ModelFactory.createDefaultModel().createProperty(rlspNS + "northEastOf");
	static Property rlspNorthWestOf = ModelFactory.createDefaultModel().createProperty(rlspNS + "northWestOf");
	static Property rlspSouthOf = ModelFactory.createDefaultModel().createProperty(rlspNS + "southOf");
	static Property rlspSouthEastOf = ModelFactory.createDefaultModel().createProperty(rlspNS + "southEastOf");
	static Property rlspSouthWestOf = ModelFactory.createDefaultModel().createProperty(rlspNS + "southWestOf");
	static Property rlspEastOf = ModelFactory.createDefaultModel().createProperty(rlspNS + "eastOf");
	static Property rlspWestOf = ModelFactory.createDefaultModel().createProperty(rlspNS + "westOf");
	static Property propLat = ModelFactory.createDefaultModel().createProperty(propFrNS + "latitude");
	static Property propLong = ModelFactory.createDefaultModel().createProperty(propFrNS + "longitude");
	static Property geoLat = ModelFactory.createDefaultModel().createProperty(geoNS + "lat");
	static Property geoLong = ModelFactory.createDefaultModel().createProperty(geoNS + "long");
	static Property spatialReference = ModelFactory.createDefaultModel().createProperty("http://data.ign.fr/def/itineraires#spatialReference");

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
		float threshold = 0.5f;
		// final Model kbSource2 =
		// ModelFactory.createDefaultModel().read(dbPediaRdfFilePath);
		// float test =
		// getLatitude(kbSource2.createResource("http://fr.dbpedia.org/resource/Aulnay-sous-Bois"),
		// kbSource2);
		// float test2 =
		// getLongitude(kbSource2.createResource("http://fr.dbpedia.org/resource/Aulnay-sous-Bois"),
		// kbSource2);
		// float test3 =
		// getLatitude(kbSource2.createResource("http://fr.dbpedia.org/resource/Paris"),
		// kbSource2);
		// float test4 =
		// getLongitude(kbSource2.createResource("http://fr.dbpedia.org/resource/Paris"),
		// kbSource2);
		// logger.info(test);
		// logger.info(test2);
		// logger.info(test3);
		// logger.info(test4);
		// logger.info(distance(test, test2, test3, test4));
		// saveModelToFile("D:\\dbpedia_fr_with_rlsp.ttl", kbSource2, "TURTLE");
		// Model test = ModelFactory.createDefaultModel();
		// Statement s1 =
		// test.createStatement(test.createResource("http://fr.dbpedia.org/page/Surg%C3%A8res"),
		// rlspEastOf,
		// test.createResource("http://data.ign.fr/id/propagation/Place/3"));
		// Statement s2 =
		// test.createStatement(test.createResource("http://fr.dbpedia.org/page/Surg%C3%A8res"),
		// rlspEastOf,
		// test.createResource("http://data.ign.fr/id/propagation/Place/3"));
		// test.add(s1);
		// test.add(s2);
		// saveModelToFile("t.n3", test, "N3");
//		DijkstraSP te = DijkstraSP.deserialize(serializePath + 3);

		logger.info("Chargement du TEI : " + TEI_PATH);
		Document teiSource = XMLUtil.createDocumentFromFile(TEI_PATH);
		Model teiRdf = RDFUtil.getModel(teiSource);//ModelFactory.createDefaultModel().read("D:\\temp7.n3");// RDFUtil.getModel(teiSource)
		logger.info("Model TEI vide : " + teiRdf.isEmpty());
//		Map<Resource, String> listS = new HashMap<>();
//		for (Statement s : teiRdf.listStatements().toList()) {
//			if (s.getPredicate().getURI().equals(rdfsLabel.getURI())) {
//				listS.put(s.getSubject(), s.getSubject() + " -> "+ s.getObject());
//			}
//		}
//		listS.keySet().stream().sorted(comparatorResourcePlace).forEach(s -> System.out.println(listS.get(s)));
		Set<Toponym> toponymsTEI = getToponymsFromTei(teiRdf);
		logger.info(toponymsTEI.size() + " toponymes dans le TEI");

		logger.info("Chargement de la KB : " + dbPediaRdfFilePath);
		final Model kbSource = ModelFactory.createDefaultModel().read(dbPediaRdfFilePath);
		logger.info("Création du sous graphe de la KB contenant uniquement les relations spatiales");
		Model kbSubgraph = getSubGraphWithResources(kbSource);
//		Set<Resource> subjects = new HashSet<>(kbSubgraph.listSubjects().toList());
//		int c = 0;
//		for (RDFNode o : kbSubgraph.listObjects().toList()) {
//			if (!subjects.contains(o)) {
//				logger.info(o);
//				c++;
//			}
//		}
//		logger.info(c);
		// // Inversion des statements
//		for (Statement s : kbSubgraph.listStatements().toList()) {
//			Property p = s.getPredicate();
//			Property newProperty = p;
//			if (p.getURI().equalsIgnoreCase(propNord.getURI())) {
//				newProperty = propSud;
//			} else if (p.getURI().equalsIgnoreCase(propNordEst.getURI())) {
//				newProperty = propSudOuest;
//			} else if (p.getURI().equalsIgnoreCase(propNordOuest.getURI())) {
//				newProperty = propSudEst;
//			} else if (p.getURI().equalsIgnoreCase(propSud.getURI())) {
//				newProperty = propNord;
//			} else if (p.getURI().equalsIgnoreCase(propSudEst.getURI())) {
//				newProperty = propNordOuest;
//			} else if (p.getURI().equalsIgnoreCase(propSudOuest.getURI())) {
//				newProperty = propNordEst;
//			} else if (p.getURI().equalsIgnoreCase(propEst.getURI())) {
//				newProperty = propOuest;
//			} else if (p.getURI().equalsIgnoreCase(propOuest.getURI())) {
//				newProperty = propEst;
//			}
//			kbSubgraph.add(kbSubgraph.createStatement((Resource) s.getObject(), newProperty, s.getSubject()));
//		}

		//short[][] test = floydWarshallAPSP(kbSubgraph); // trop long
		List<Resource> subjects = kbSubgraph.listSubjects().toList().stream().sorted((a, b) -> b.toString().compareTo(a.toString())).collect(Collectors.toList());
		File folder = new File(serializePath);
		File[] listOfFiles = folder.listFiles();
		List<Integer> resourcesProcessed = new ArrayList<>();
		for (File file : listOfFiles) {
			resourcesProcessed.add(Integer.parseInt(file.getName()));
		}
		logger.info("Resources déjà traitées : " + resourcesProcessed.size());
		final AtomicInteger countSP = new AtomicInteger(subjects.size());
		subjects.parallelStream().forEachOrdered(subject -> {
		//for (Resource subject : subjects) {			
			int counter = countSP.getAndDecrement();
			if (!resourcesProcessed.contains(counter)) {
				DijkstraSP dspTest = new DijkstraSP(kbSubgraph, subject);
				dspTest.serialize(serializePath + counter);
			}
			logger.info(counter + " / " + subjects.size());
		});
		Resource lille = kbSubgraph.getResource("http://fr.dbpedia.org/resource/Lille");
		Resource marseille = kbSubgraph.getResource("http://fr.dbpedia.org/resource/Marseille");
//		logger.info(kbSubgraph.listStatements().toList().size());
		logger.info("DijkstraSP");
		DijkstraSP dspTest = new DijkstraSP(kbSubgraph, lille);
		logger.info(dspTest.hasPathTo(marseille));
//		Map<String, DijkstraSP> apsp = new ConcurrentHashMap<>();
//		final AtomicInteger countSP = new AtomicInteger();
//		List<Resource> nodes = kbSubgraph.listSubjects().toList().stream().distinct().collect(Collectors.toList());
//		nodes.parallelStream().forEach(n -> {
//			if (!apsp.containsKey(n.toString())) {
//				DijkstraSP dsp = new DijkstraSP(kbSubgraph, n);
//				apsp.put(n.toString(), dsp);
//			}
//			logger.info((countSP.getAndIncrement() + 1) + " / " + nodes.size());
//		});
		FloydWarshallAPSPV2 floydW = new FloydWarshallAPSPV2(kbSubgraph);
//		floydW.serialize("D:\\testSerialization.txt");
//		FloydWarshallAPSP floydW2 = FloydWarshallAPSP.deserialize("D:\\testSerialization.txt");
		floydW.compute(); // sérialiser une fois le contenu calculer. Ajouter une methode pr sérialiser dans floyW class et une méthode statique pour désérialiser (tjs dans la classe)
		floydW.serialize("C:\\FloydWarshallAPSP.txt");
		logger.info(floydW.hasPath(lille, marseille));
		logger.info(floydW.hasPath(marseille, lille));
		//floydWarshallAPSPV2(kbSubgraph); 
//		logger.info("Dijkstra");
//		List<Resource> nodesForSP = kbSubgraph.listStatements().toList().stream().map(s -> s.getSubject()).distinct()
//				.limit(1)
//				.collect(Collectors.toList());
//		int count = 1;
//		DijkstraSP dijkstraSerialized = null;
//		for (Resource r : nodesForSP) {
//			logger.info(count + " / " + nodesForSP.size());
//			DijkstraSP dijkstra = new DijkstraSP(kbSubgraph, r);
//			dijkstras.put(r, dijkstra);
//			dijkstraSerialized = dijkstra;
//			try (FileOutputStream fout = new FileOutputStream("D:\\dijkstraSerialisationTest.txt")) {
//
//				try (ObjectOutputStream oos = new ObjectOutputStream(fout)) {
//					oos.writeObject(dijkstra);
//				}
//				logger.info("Done");
//
//			} catch (Exception e) {
//				logger.error(e);
//			}
//			count++;
//		}
//		DijkstraSP dijkstraDeserialized = null;
//		try (FileInputStream fis = new FileInputStream("D:\\dijkstraSerialisationTest.txt")) {
//			try (ObjectInputStream objectInputStream = new ObjectInputStream(fis)) {
//				dijkstraDeserialized = (DijkstraSP) objectInputStream.readObject();
//			} catch (ClassNotFoundException e) {
//				logger.error(e);
//			}
//		} catch (IOException e) {
//			logger.error(e);
//		}
//		if (dijkstraSerialized != null && dijkstraDeserialized != null) {
//			logger.info(dijkstraSerialized.getNode().toString());
//			if (dijkstraDeserialized.getNode().toString().equals(dijkstraSerialized.getNode().toString())) {
//				logger.info("noeuds identiques");
//			}
//			Resource Lille = kbSubgraph.getResource("http://fr.dbpedia.org/resource/Lille");
//			Resource Marseille = kbSubgraph.getResource("http://fr.dbpedia.org/resource/Marseille");
//			logger.info(dijkstraDeserialized.hasPathTo(Marseille));
//			logger.info(dijkstraSerialized.hasPathTo(Marseille));
//			logger.info(dijkstraDeserialized.hasPathTo(Marseille) ? dijkstraDeserialized.pathTo(Marseille, kbSubgraph) : "KO");
//			logger.info(dijkstraSerialized.hasPathTo(Marseille) ? dijkstraSerialized.pathTo(Marseille, kbSubgraph) : "KO");
//			logger.info(dijkstraDeserialized.hasPathTo(Lille));
//			logger.info(dijkstraSerialized.hasPathTo(Lille));
//			logger.info(dijkstraDeserialized.hasPathTo(Lille) ? dijkstraDeserialized.pathTo(Lille, kbSubgraph) : "KO");
//			logger.info(dijkstraSerialized.hasPathTo(Lille) ? dijkstraSerialized.pathTo(Lille, kbSubgraph) : "KO");
//		} else {
//			if (dijkstraSerialized == null)
//				logger.info("dijkstraSerialized null");
//			if (dijkstraDeserialized == null)
//				logger.info("dijkstraDeserialized null");
//		}
		////FIN DIJKSTRA
		//saveModelToFile("subgraphWithRLSP.n3", kbSubgraph, "N3")
		logger.info("Récupérations des candidats de la KB");
		final List<Candidate> candidatesFromKB = getCandidatesFromKB(kbSource);

		Set<Toponym> result = getCandidatesSelection(toponymsTEI, candidatesFromKB, numberOfCandidate, threshold);
		
		// test algo SP
//		Resource Ruffec = kbSubgraph.getResource("http://fr.dbpedia.org/resource/Lille");
//		Resource Pamiers = kbSubgraph.getResource("http://fr.dbpedia.org/resource/Marseille");
//		Path path = findShortestPathV2(kbSubgraph, Ruffec, Pamiers, kbSource);
//		logger.info(path.size());
		
		deleteUselessAlts(result, teiRdf);
		
		logger.info("Préparation de la création des mini graphes pour chaques séquences");
		List<QuerySolution> querySolutions = getGraphTuples(teiRdf);
		List<QuerySolutionEntry> querySolutionEntries = getQuerySolutionEntries(querySolutions).stream()
				.sorted(comparatorQuerySolutionEntry).collect(Collectors.toList());
		List<Resource> sequences = querySolutionEntries.stream().map(q -> q.getSequence()).distinct()
				.collect(Collectors.toList());

		//V1(sequences, querySolutionEntries, teiRdf, kbSubgraph, kbSource, toponymsTEI);
		//V2(sequences, querySolutionEntries, teiRdf, kbSubgraph, kbSource, toponymsTEI);
		V3(sequences, querySolutionEntries, teiRdf, kbSubgraph, kbSource, toponymsTEI);

		return result;
	}
	static void V1(List<Resource> sequences, List<QuerySolutionEntry> querySolutionEntries, Model teiRdf, Model kbSubgraph, Model kbSource, Set<Toponym> toponymsTEI) {
		logger.info("Traitement des mini graphes des séquences");
		int seqCount = 1;
		List<List<Model>> altsBySeq = new ArrayList<>();
		for (Resource sequence : sequences.stream().limit(1).collect(Collectors.toList())) {
			logger.info("Traitement de la séquence " + seqCount + "/" + sequences.size());
			seqCount++;
			Model currentModel = getModelsFromSequenceV2(querySolutionEntries, sequence);
			currentModel = addRlsp(currentModel, teiRdf);
			// currentModel = oneSensePerDiscourseFusion(currentModel,
			// teiRdf);// ajouter ici la fusion des noeuds identiques (one sence
			// per discourse)
			// Il faudrait en fait réaliser la fusion des noeuds identiques
			// avant (au moment des transformations XSLT), amsi attention car la
			// création
			// des mini graphes devra être adaptée (l'ordonancement des
			// QuerySolutionEntry devra être géré d'une manière différente)
			List<Model> alts = explodeAlts(currentModel);
			for (Model miniGraph : alts) {
				fillNodesToVisitForCurrentSeq(kbSubgraph, miniGraph, toponymsTEI);
			}
			altsBySeq.add(alts);
		}
		shortestPath(kbSubgraph);
		for (List<Model> alts : altsBySeq) {
			Map<Float, List<IPathMatching>> resultsForCurrentSeq = new HashMap<>();
			logger.info(alts.size() + " mini graphes à traiter pour cette séquence.");
			for (Model miniGraph : alts) {
				// saveModelToFile("test2.n3", currentModel, "N3")
				List<IPathMatching> path = graphMatchingV2(kbSubgraph, miniGraph, toponymsTEI, 0.4f, 0.4f, 0.2f,
						kbSource);
				resultsForCurrentSeq.put(totalCostPath(path), path);
			}
			if (!resultsForCurrentSeq.isEmpty()) {
				rlspCalculous.stream().sorted().forEach(logger::info);
				rlspCalculous.clear();
				Entry<Float, List<IPathMatching>> bestPath = getBestPath(resultsForCurrentSeq);
				logger.info(bestPath.getKey());
				bestPath.getValue().forEach(logger::info);
			}
		}
	}
	static void V2(List<Resource> sequences, List<QuerySolutionEntry> querySolutionEntries, Model teiRdf, Model kbSubgraph, Model kbSource, Set<Toponym> toponymsTEI) {
		logger.info("Traitement des mini graphes des séquences");
		int seqCount = 1;
		for (Resource sequence : sequences
				//.stream().limit(1).collect(Collectors.toList())
				) {
			logger.info("Traitement de la séquence " + seqCount + "/" + sequences.size());
			seqCount++;
			Model currentModel = getModelsFromSequenceV2(querySolutionEntries, sequence);
			currentModel = addRlsp(currentModel, teiRdf);
			// currentModel = oneSensePerDiscourseFusion(currentModel,
			// teiRdf);// ajouter ici la fusion des noeuds identiques (one sence
			// per discourse)
			// Il faudrait en fait réaliser la fusion des noeuds identiques
			// avant (au moment des transformations XSLT), amsi attention car la
			// création
			// des mini graphes devra être adaptée (l'ordonancement des
			// QuerySolutionEntry devra être géré d'une manière différente)
			List<Model> alts = explodeAlts(currentModel);
			Map<Float, List<IPathMatching>> resultsForCurrentSeq = new HashMap<>();
			logger.info(alts.size() + " mini graphes à traiter pour cette séquence.");
			for (Model miniGraph : alts
					.stream().sorted((a, b) -> Long.compare(a.size(), b.size())).collect(Collectors.toList())) {
				List<IPathMatching> path = graphMatchingV2(kbSubgraph, miniGraph, toponymsTEI, 0.4f, 0.4f, 0.2f,
						kbSource);
				resultsForCurrentSeq.put(totalCostPath(path), path);
			}
			if (!resultsForCurrentSeq.isEmpty()) {
				rlspCalculous.stream().sorted().forEach(logger::info);
				rlspCalculous.clear();
				Entry<Float, List<IPathMatching>> bestPath = getBestPath(resultsForCurrentSeq);
				logger.info(bestPath.getKey());
				bestPath.getValue().forEach(logger::info);
			}
		}
	}
	static void V3(List<Resource> sequences, List<QuerySolutionEntry> querySolutionEntries, Model teiRdf, Model kbSubgraph, Model kbSource, Set<Toponym> toponymsTEI) {
		logger.info("V3");
		logger.info("Traitement des mini graphes des séquences");
		int seqCount = 1;
		List<List<Model>> altsBySeq = new ArrayList<>();
		for (Resource sequence : sequences
				//.stream().limit(1).collect(Collectors.toList())
				) {
			logger.info("Traitement de la séquence " + seqCount + "/" + sequences.size());
			seqCount++;
			Model currentModel = getModelsFromSequenceV2(querySolutionEntries, sequence);
			currentModel = addRlsp(currentModel, teiRdf);
			// currentModel = oneSensePerDiscourseFusion(currentModel,
			// teiRdf);// ajouter ici la fusion des noeuds identiques (one sence
			// per discourse)
			// Il faudrait en fait réaliser la fusion des noeuds identiques
			// avant (au moment des transformations XSLT), amsi attention car la
			// création
			// des mini graphes devra être adaptée (l'ordonancement des
			// QuerySolutionEntry devra être géré d'une manière différente)
			List<Model> alts = explodeAlts(currentModel);
			altsBySeq.add(alts);			
		}
		seqCount = 1;
		for (List<Model> alts : altsBySeq.stream()
				.sorted((l1, l2) -> Integer.compare(l1.get(0).listStatements().toList().size(), l2.get(0).listStatements().toList().size())).collect(Collectors.toList())) {
			logger.info("Traitement de la séquence " + seqCount + "/" + altsBySeq.size());
			seqCount++;
			Map<Float, List<IPathMatching>> resultsForCurrentSeq = new HashMap<>();
			logger.info(alts.size() + " mini graphes à traiter pour cette séquence.");
			for (Model miniGraph : alts) {
					//.stream().sorted((a, b) -> Long.compare(a.size(), b.size())).collect(Collectors.toList())) {
				List<IPathMatching> path = graphMatchingV2(kbSubgraph, miniGraph, toponymsTEI, 0.4f, 0.4f, 0.2f,
						kbSource);
				resultsForCurrentSeq.put(totalCostPath(path), path);
			}
			if (!resultsForCurrentSeq.isEmpty()) {
				rlspCalculous.stream().sorted().forEach(logger::info);
				rlspCalculous.clear();
				Entry<Float, List<IPathMatching>> bestPath = getBestPath(resultsForCurrentSeq);
				logger.info(bestPath.getKey());
				bestPath.getValue().forEach(logger::info);
			}
		}
	}
	static void deleteUselessAlts(Set<Toponym> result, Model teiRdf) {
		// certaines Alt on une de leur possibilité qui n'a pas de candidat. Elle seront tjrs préférées aux possibilités avec candidats.
				// Il faut donc les supprimer de Set<Toponym> result et de Model teiRdf
				for (Entry<Integer, List<Toponym>> toponymEntry : result.stream().collect(Collectors.groupingBy((Toponym t) -> t.getXmlId())).entrySet().stream()
					.filter(e -> e.getValue().size() > 1 && e.getValue().stream().anyMatch(p -> p.getScoreCriterionToponymCandidate().isEmpty())
							 && e.getValue().stream().anyMatch(p -> !p.getScoreCriterionToponymCandidate().isEmpty())).collect(Collectors.toList())) {
					Toponym toponymToRemove = toponymEntry.getValue().stream().filter(f -> f.getScoreCriterionToponymCandidate().isEmpty()).findFirst().get();
					Toponym toponymToKeep = toponymEntry.getValue().stream().filter(f -> !f.getScoreCriterionToponymCandidate().isEmpty()).findFirst().get();
					List<Statement> statementsFromNodeToKeep = teiRdf.listStatements(null, spatialReference, toponymToKeep.getResource()).toList();
					Resource blankNodeOfNodeToKeep = statementsFromNodeToKeep.get(0).getSubject();
					if (toponymToRemove.getScoreCriterionToponymCandidate().isEmpty() && result.remove(toponymToRemove)) {
						Optional<Statement> sOpt = teiRdf.listStatements(null, null, toponymToRemove.getResource()).toList().stream().findFirst();//.get();
						if (sOpt.isPresent()) {
							Statement s = sOpt.get();
							Optional<Statement> altStatement = teiRdf.listStatements(null, null, s.getSubject()).toList().stream().findFirst();
							if (altStatement.isPresent()) {
								Alt alt = teiRdf.getAlt(altStatement.get().getSubject());
								List<Statement> statementsToRemove = teiRdf.listStatements(null, null, alt).toList();
								List<Statement> statementsToAdd = new ArrayList<>();
								for (Statement statement : statementsToRemove) {
									statementsToAdd.add(teiRdf.createStatement(statement.getSubject(), statement.getPredicate(), blankNodeOfNodeToKeep));
								}
								deleteResource(teiRdf, toponymToRemove.getResource());
								deleteResource(teiRdf, alt);
								teiRdf.add(statementsToAdd);
								logger.info("1 : " + toponymToRemove.getResource() + " / " + toponymToKeep.getResource());
							} else {
								logger.info("2 : " + toponymToRemove.getResource() + " / " + toponymToKeep.getResource());
							}
						}
					}
				}
	}
	static Map<Resource, DijkstraSP> dijkstras = new HashMap<>();
	static Set<String> rlspCalculous = new HashSet<>();
	/**
	 * Adds the rlsp to the model of the sequence.
	 *
	 * @param sequenceModel
	 *            the sequence model
	 * @param teiModel
	 *            the tei model
	 * @return the model
	 */
	static Model addRlsp(Model sequenceModel, Model teiModel) {
		List<Resource> resourcesFromCurrentSeq = sequenceModel.listSubjects().toList();
		resourcesFromCurrentSeq.addAll(sequenceModel.listObjects().toList().stream().filter(o -> o.isResource())
				.map(p -> (Resource) p).collect(Collectors.toList()));
		List<Statement> statements = new ArrayList<>();
		statements.addAll(teiModel.listStatements(null, rlspEastOf, (RDFNode) null).toList());
		statements.addAll(teiModel.listStatements(null, rlspNorthEastOf, (RDFNode) null).toList());
		statements.addAll(teiModel.listStatements(null, rlspNorthOf, (RDFNode) null).toList());
		statements.addAll(teiModel.listStatements(null, rlspNorthWestOf, (RDFNode) null).toList());
		statements.addAll(teiModel.listStatements(null, rlspSouthEastOf, (RDFNode) null).toList());
		statements.addAll(teiModel.listStatements(null, rlspSouthOf, (RDFNode) null).toList());
		statements.addAll(teiModel.listStatements(null, rlspSouthWestOf, (RDFNode) null).toList());
		statements.addAll(teiModel.listStatements(null, rlspWestOf, (RDFNode) null).toList());
		statements = statements.stream()
				.filter(p -> resourcesFromCurrentSeq.stream().anyMatch(rseq -> areResourcesEqual(p.getSubject(), rseq))
						&& resourcesFromCurrentSeq.stream()
								.anyMatch(rseq -> areResourcesEqual((Resource) p.getObject(), rseq)))
				.collect(Collectors.toList());
		if (!statements.isEmpty()) {
			sequenceModel.add(statements);
		}
		return sequenceModel;
	}

	static Entry<Float, List<IPathMatching>> getBestPath(Map<Float, List<IPathMatching>> resultsForCurrentSeq) {
		Float min = resultsForCurrentSeq.keySet().stream().min(Float::compare).get();
		return resultsForCurrentSeq.entrySet().stream().filter(e -> e.getKey() == min).findFirst().get();
	}

	/**
	 * Dbpedia alltodbpedia fr. Transforme le model contenant toutes les places
	 * en ne gardant que celles en France
	 */
	static void dbpediaAlltodbpediaFr() {
		String dbPediaRdfFilePath = "D:\\dbpedia_all_with_rlsp.n3";
		String queryString = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>   "
				+ "PREFIX dbpedia-fr: <http://fr.dbpedia.org/resource/>   "
				+ "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>   "
				+ "PREFIX dbpedia-owl: <http://dbpedia.org/ontology/>   "
				+ "PREFIX foaf: <http://xmlns.com/foaf/0.1/>   "
				+ "PREFIX prop-fr: <http://fr.dbpedia.org/property/>   "
				+ "PREFIX georss: <http://www.georss.org/georss/>   "
				+ "PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#> " + "CONSTRUCT {?s ?p ?o} WHERE {  "
				+ "    ?s ?p ?o ." + "    {    " + "        ?s prop-fr:longitude ?long.     "
				+ "        ?s prop-fr:latitude ?lat.    " + "    }     " + "    UNION     " + "    {    "
				+ "        ?s geo:long ?long.     " + "        ?s geo:lat ?lat.    " + "    }     "
				+ "    FILTER ((?long > -5.25 && ?long <8.25) &&(?lat > 42.3 && ?lat < 51.15)) .    " + "} ";
		final Model kbSource = ModelFactory.createDefaultModel().read(dbPediaRdfFilePath);
		Model newModel;
		try {
			newModel = RDFUtil.getQueryConstruct(kbSource, queryString, null);
			saveModelToFile("D:\\dbpedia_fr_with_rlsp.n3", newModel, "N3");
		} catch (QueryParseException | HttpHostConnectException | RiotException | MalformedURLException
				| HttpException e) {
			logger.error(e);
		}
	}

	static Model oneSensePerDiscourseFusion(Model modelCurrentSeq, Model teiRdf) {
		Model currentModel = cloneModel(modelCurrentSeq);
		Map<RDFNode, List<Statement>> typesByResources = teiRdf.listStatements(null, rdfsLabel, (RDFNode) null).toList()
				.stream().collect(Collectors.groupingBy((Statement s) -> s.getObject()));

		// on ne garde que les resources de la séquence actuelle
		List<Resource> resourcesFromCurrentModel = currentModel.listSubjects().toList();
		resourcesFromCurrentModel.addAll(
				currentModel.listObjects().toList().stream().map(n -> (Resource) n).collect(Collectors.toList()));
		resourcesFromCurrentModel = resourcesFromCurrentModel.stream().distinct().collect(Collectors.toList());
		final List<Resource> resourcesFromCurrentModel2 = new ArrayList<>(resourcesFromCurrentModel);
		for (RDFNode keyNode : typesByResources.keySet()) {
			List<Statement> oldStatements = typesByResources.get(keyNode);
			oldStatements = oldStatements.stream().filter(s -> resourcesFromCurrentModel2.contains(s.getSubject()))
					.collect(Collectors.toList());
			typesByResources.put(keyNode, oldStatements);
		}

		// on supprime toutes les clés avec seulement 1 élément (attention à la
		// gestion des rdf:Alt)
		List<RDFNode> keysToRemove = typesByResources.keySet().stream().filter(k -> {
			List<Statement> statements = typesByResources.get(k);
			// prendre en compte les Alt
			return statements.size() <= 1 || (statements.size() == 2
					&& statements.stream().anyMatch(s -> s.getSubject().toString().indexOf('_') > -1));
		}).collect(Collectors.toList());
		for (RDFNode keyToRemove : keysToRemove) {
			typesByResources.remove(keyToRemove);
		}
		if (!typesByResources.isEmpty()) {
			for (RDFNode keyNode : typesByResources.keySet()) {
				List<Resource> resources = typesByResources.get(keyNode).stream().map(m -> m.getSubject())
						.sorted(comparatorResourcePlace).collect(Collectors.toList());
				if (resources.stream().anyMatch(r -> r.toString().indexOf('_') > -1)) { // partie
																						// galère,
																						// car
																						// contient
																						// des
																						// Alts
					logger.info("Gérer la fusion avec les alts");
				} else {
					// firstPlace est la resource qui remplacera les autres
					Resource firstPlace = resources.get(0);
					resources.stream().filter(r -> r != firstPlace).forEach(place -> {
						List<Statement> statementsToReplace = getProperties(place, currentModel);
						List<Statement> newStatements = new ArrayList<>();
						for (Statement statement : statementsToReplace) {
							if (areResourcesEqual(place, statement.getSubject())) { // on
																					// remplace
																					// le
																					// sujet
								newStatements.add(teiRdf.createStatement(firstPlace, statement.getPredicate(),
										statement.getObject()));
							} else {
								newStatements.add(teiRdf.createStatement((Resource) statement.getObject(),
										statement.getPredicate(), firstPlace));
							}
						}
						currentModel.remove(statementsToReplace);
						currentModel.add(newStatements);
					});
				}
			}
		}
		return currentModel;
	}

	static List<Statement> getProperties(Resource place, Model teiRdf) {
		List<Statement> results = teiRdf.listStatements(place, null, (RDFNode) null).toList();
		results.addAll(teiRdf.listStatements(null, null, (RDFNode) place).toList());
		return results;
	}

	/**
	 * For each rdf:Alt, return the two corresponding models
	 *
	 * @param currentModel
	 *            the current model
	 * @return the list
	 */
	static List<Model> explodeAlts(Model currentModel) {
		Model currentModelClone = cloneModel(currentModel);
		List<Model> results = new ArrayList<>();
		List<Resource> alts = currentModelClone
				.listStatements(null, null,
						currentModelClone.createResource("http://www.w3.org/1999/02/22-rdf-syntax-ns#Alt"))
				.toList().stream().map(s -> s.getSubject()).collect(Collectors.toList());
		results.add(currentModelClone);
		if (!alts.isEmpty()) {
			for (Resource resourceAlt : alts) {
				Alt alt = currentModelClone.getAlt(resourceAlt);
				// certaines rdf:Alt sont en fait identiques, il faut les
				// fusionner. On vérifie si les places de cet Alt ont déjà été
				// traités.
				// on vérifie uniquement ac la 1ère place
				Resource r_1 = (Resource) currentModelClone
						.getProperty(alt,
								currentModelClone.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#_1"))
						.getObject();
				if (results.stream()
						.anyMatch(m -> m.contains(r_1, linkSameBag) || m.contains(r_1, linkSameRoute)
								|| m.contains(r_1, linkSameSequence) || m.contains(null, linkSameBag, r_1)
								|| m.contains(null, linkSameRoute, r_1) || m.contains(null, linkSameSequence, r_1))) {
					// cet rdf:Alt est un doublon
					List<Statement> statements = results.get(0).listStatements().toList().stream()
							.filter(p -> (p.getSubject().toString().equals(alt.toString())
									|| (p.getObject().isResource()
											&& ((Resource) p.getObject()).toString().equals(alt.toString())))
									&& p.getPredicate().getNameSpace().equals(ignNS))
							.collect(Collectors.toList());
					List<Resource> places = new ArrayList<>();
					alt.iterator().toList().stream().forEach(m -> places.add((Resource) m));
					for (Resource place : places) {
						List<Statement> currentStatements = new ArrayList<>();
						for (Statement oldStatement : statements) {
							Statement newStatement;
							if (oldStatement.getSubject().getURI() == alt.getURI()) { // alt
																						// au
																						// début
								newStatement = currentModelClone.createStatement(place, oldStatement.getPredicate(),
										oldStatement.getObject());
							} else { // alt à la fin
								newStatement = currentModelClone.createStatement(oldStatement.getSubject(),
										oldStatement.getPredicate(), place);
							}
							currentStatements.add(newStatement);
						}
						for (Model m : results) {
							if (m.contains(place, linkSameBag) || m.contains(place, linkSameRoute)
									|| m.contains(place, linkSameSequence) || m.contains(null, linkSameBag, place)
									|| m.contains(null, linkSameRoute, place)
									|| m.contains(null, linkSameSequence, place)) {
								for (Statement statement : currentStatements) {
									m.add(statement);
								}
							}
						}
					}
				} else { // cet rdf:Alt n'est pas un doublon
					List<Resource> places = new ArrayList<>();
					alt.iterator().toList().stream().forEach(m -> places.add((Resource) m));
					List<Statement> statements = results.get(0).listStatements().toList().stream()
							.filter(p -> (p.getSubject().toString().equals(alt.toString())
									|| (p.getObject().isResource()
											&& ((Resource) p.getObject()).toString().equals(alt.toString())))
									&& p.getPredicate().getNameSpace().equals(ignNS))
							.collect(Collectors.toList());
					List<List<Statement>> statementLists = new ArrayList<>(); // on
																				// est
																				// censé
																				// avoir
																				// 2
																				// list
																				// de
																				// statement
																				// (utant
																				// que
																				// de
																				// places)
					for (Resource place : places) {
						List<Statement> currentStatements = new ArrayList<>();
						for (Statement oldStatement : statements) {
							Statement newStatement;
							if (oldStatement.getSubject().getURI() == alt.getURI()) { // alt
																						// au
																						// début
								newStatement = currentModelClone.createStatement(place, oldStatement.getPredicate(),
										oldStatement.getObject());
							} else { // alt à la fin
								newStatement = currentModelClone.createStatement(oldStatement.getSubject(),
										oldStatement.getPredicate(), place);
							}
							currentStatements.add(newStatement);
						}
						statementLists.add(currentStatements);
					}

					List<List<Model>> resultsList = new ArrayList<>(); // autant
																		// de
																		// list
																		// de
																		// model
																		// que
																		// de
																		// places
																		// ou de
																		// list
																		// de
																		// statement
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

	static List<QuerySolution> getGraphTuples(Model teiModel) {
		String query = "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
				+ "PREFIX iti:<http://data.ign.fr/def/itineraires#> "
				+ "SELECT ?sequence ?route ?bag ?waypoint ?spatialReference ?spatialReferenceAlt ?id WHERE { "
				+ "     ?sequence rdf:type rdf:Seq . " + "     ?sequence ?pSeq ?route . "
				+ "     ?route iti:waypoints ?waypoints . " + "     ?waypoints rdf:rest*/rdf:first ?bag . "
				+ "    ?bag ?pBag ?waypoint . " + "    OPTIONAL { "
				+ "        ?waypoint iti:spatialReference ?spatialReference . "
				+ "        ?spatialReference <http://example.com/namespace/id> ?id . " + "    }    " + "    OPTIONAL { "
				+ "        ?waypoint rdf:type rdf:Alt .    " + "        ?waypoint ?pWaypoint ?waypointBis .    "
				+ "        ?waypointBis iti:spatialReference ?spatialReferenceAlt .  "
				+ "        ?spatialReferenceAlt <http://example.com/namespace/id> ?id . " + "    }    "
				+ "     FILTER (?pSeq != rdf:type && ?pBag != rdf:type && ?pBag != rdf:first) . "
				+ "} ORDER BY ?sequence ?route ?bag ?waypoint ?id";
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
			RDFNode nodeId = querySolution.get("id");
			Integer id = -1;
			if (nodeId.isLiteral()) {
				Literal literalId = nodeId.asLiteral();
				String stringId = literalId.getLexicalForm();
				id = Integer.parseInt(stringId);
			}
			querySolutionEntries.add(
					new QuerySolutionEntry(sequence, route, bag, waypoint, spatialReference, spatialReferenceAlt, id));
		}
		return querySolutionEntries;
	}

	static Model getModelsFromSequenceV2(List<QuerySolutionEntry> allQuerySolutionEntries, Resource currentSequence) {
		List<QuerySolutionEntry> querySolutionEntries = allQuerySolutionEntries.stream()
				.filter(q -> areResourcesEqual(q.getSequence(), currentSequence)).sorted(comparatorQuerySolutionEntry)
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
			if (i + 1 >= querySolutionEntries.size())
				break;
			QuerySolutionEntry next = querySolutionEntries.get(j);
			QuerySolutionEntry nextAlt = null;
			if (next.getSpatialReferenceAlt() != null && j < querySolutionEntries.size() - 1) {
				j++;
				nextAlt = querySolutionEntries.get(j);
			}
			if ((previous == null || current.getBag() != previous.getBag()) && current.getBag() == next.getBag()) {
				// current est le 1er élément d'un bag où il y a plusieurs
				// éléments
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
				List<QuerySolutionEntry> bagElements = querySolutionEntries.stream()
						.filter(p -> p.getBag() == optionalBag.get()).sorted(comparatorQuerySolutionEntry)
						.collect(Collectors.toList());
				Resource r1;
				if (currentAlt != null && currentAlt.getSpatialReferenceAlt() != null && current.getSpatialReferenceAlt() != null) { // current est une Alt
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
					if (next.getSpatialReferenceAlt() != null && j < bagElements.size() - 1) {
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
					if (r1 != null && r2 != null)
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
				List<QuerySolutionEntry> lastBagElements = querySolutionEntries.stream()
						.filter(p -> p.getBag() == lastBagOfCurrentRoute).sorted(comparatorQuerySolutionEntry)
						.collect(Collectors.toList());
				List<QuerySolutionEntry> firstBagElements = querySolutionEntries.stream()
						.filter(p -> p.getBag() == firstBagOfLastRoute).sorted(comparatorQuerySolutionEntry)
						.collect(Collectors.toList());
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
						if (next.getSpatialReferenceAlt() != null && k < firstBagElements.size() - 1) {
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
						if (r1 != null && r2 != null)
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
		original.getNsPrefixMap().keySet()
				.forEach(prefix -> newModel.setNsPrefix(prefix, original.getNsPrefixMap().get(prefix)));
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
		Resource res = (Resource) n;
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

	static void saveModelToFile(String fileName, Model model, String lang) {
		File file = new File(fileName);
		try {
			model.write(new java.io.FileOutputStream(file), lang);
		} catch (FileNotFoundException e) {
			logger.error(e);
		}
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
			Integer numberOfCandidate, float threshold) {
		logger.info("Sélection des candidats (nombre de candidats : " + numberOfCandidate + ")");
		List<Candidate> candidatesFromKBCleared = candidatesFromKB.stream()
				.filter(c -> c != null && c.getTypes() != null && (c.getName() != null || c.getLabel() != null))
				.collect(Collectors.toList());
		

		Map<String, List<Candidate>> candidatesByType = new HashMap<>();
		for (Candidate candidate2 : candidatesFromKBCleared) {
			for (String type : candidate2.getTypes()) {
				List<Candidate> candidates;
				if (candidatesByType.containsKey(type)) {
					candidates = candidatesByType.get(type);

				} else {
					candidates = new ArrayList<>();
				}
				candidates.add(candidate2);
				candidatesByType.put(type, candidates);
			}
		}
		Set<Toponym> result = Collections.synchronizedSet(new HashSet<>());
		final AtomicInteger count = new AtomicInteger();
		// calculs des scores pour chaque candidat de chaque toponyme
		// aggrégation des toponymes sur leur labal
		Map<String, List<Toponym>> toponymsByLabel = toponymsTEI.stream()
				.collect(Collectors.groupingBy((Toponym s) -> s.getName()));
		final int total = toponymsByLabel.size();
		toponymsByLabel.entrySet().parallelStream().forEach(toponymsWithLabel -> {
			// toponymes de l'entry aggrégés par type
			Map<ToponymType, List<Toponym>> toponymsByType = toponymsWithLabel.getValue().stream()
					.collect(Collectors.groupingBy((Toponym s) -> s.getType()));
			computeCandidates(toponymsWithLabel, toponymsByType.entrySet(), candidatesByType);
			toponymsWithLabel.getValue().stream().forEach(toponym -> {
				toponym.clearAndAddAllScoreCriterionToponymCandidate(
						toponym.getScoreCriterionToponymCandidate().stream().filter(s -> s != null)
								.sorted(Comparator.comparing(CriterionToponymCandidate::getValue).reversed())
								.filter(t -> t.getValue() >= threshold)
								.limit(Math.min(numberOfCandidate, toponym.getScoreCriterionToponymCandidate().size()))
								.collect(Collectors.toList()));
				result.add(toponym);
			});
			logger.info((count.getAndIncrement() + 1) + " / " + total);
		});

		// toponymsTEI.parallelStream().filter(t -> t != null).forEach(toponym
		// -> {
		// candidatesFromKB.parallelStream().filter(c -> c != null &&
		// (toponym.getType() == ToponymType.PLACE
		// || typeContained(toponym.getType().toString(),
		// c.getTypes()))).forEach(candidate -> {
		// float score1 = sc.computeSimilarity(toponym.getName(),
		// candidate.getName());
		// float score2 = sc.computeSimilarity(toponym.getName(),
		// candidate.getName());
		// if (Math.max(score1, score2) > 0f)
		// toponym.addScoreCriterionToponymCandidate(new
		// CriterionToponymCandidate(toponym, candidate, Math.max(score1,
		// score2), criterion));
		// });
		// if (toponym.getScoreCriterionToponymCandidate() != null &&
		// !toponym.getScoreCriterionToponymCandidate().isEmpty()) {
		// toponym.clearAndAddAllScoreCriterionToponymCandidate(toponym.getScoreCriterionToponymCandidate().stream()
		// .filter(s -> s !=
		// null).sorted(Comparator.comparing(CriterionToponymCandidate::getValue).reversed())
		// .filter(t -> t.getValue() >= threshold)
		// .limit(Math.min(numberOfCandidate,
		// toponym.getScoreCriterionToponymCandidate().size())).collect(Collectors.toList()));
		// }
		// result.add(toponym);
		//
		// });
		return result;
	}

	static void computeCandidates(Entry<String, List<Toponym>> toponymsWithLabel,
			Set<Entry<ToponymType, List<Toponym>>> entries, Map<String, List<Candidate>> candidatesByType) {
		Criterion criterion = Criterion.scoreText;
		StringComparisonDamLev sc = new StringComparisonDamLev();
		Map<String, Float> scoreByLabel = new ConcurrentHashMap<>(); // utilisés
																		// pour
																		// stocker
																		// les
																		// scores
																		// déjà
																		// calculés
		for (Entry<ToponymType, List<Toponym>> toponymsTyped : entries) {
			String keyType = getTEITypeToKBType(toponymsTyped.getKey().toString());
			List<Candidate> candidatesToCheck = candidatesByType.get(keyType);
			if (candidatesToCheck != null && !candidatesToCheck.isEmpty()) {
				candidatesToCheck.parallelStream().forEach(candidate -> {
					float score = 0f;
					if (candidate.getName() != null && scoreByLabel.containsKey(candidate.getName())) {
						score = scoreByLabel.get(candidate.getName());
					} else if (candidate.getLabel() != null && scoreByLabel.containsKey(candidate.getLabel())) {
						score = scoreByLabel.get(candidate.getLabel());
					} else {
						float score1 = sc.computeSimilarity(toponymsWithLabel.getKey(), candidate.getName());
						float score2 = sc.computeSimilarity(toponymsWithLabel.getKey(), candidate.getLabel());
						if (score1 > score2 && candidate.getName() != null) {
							score = score1;
							scoreByLabel.put(candidate.getName(), score1);
						} else if (candidate.getLabel() != null) {
							score = score2;
							scoreByLabel.put(candidate.getLabel(), score2);
						}
					}
					for (Toponym toponym : toponymsTyped.getValue()) {
						toponym.addScoreCriterionToponymCandidate(
								new CriterionToponymCandidate(toponym, candidate, score, criterion));
					}
				});
			}
		}
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

	static String getTEITypeToKBType(String type) {
		String typeToponym = type.substring(type.lastIndexOf(':') + 1);
		return dboNS + typeToponym;
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
			Resource resource = teiRdf.createResource(RDFUtil.getURIOrLexicalForm(querySolution, "s"));
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
		List<Candidate> result = new ArrayList<>();

		final Model kbSourceFinal = kbSource;
		try {
			qSolutionsKB.addAll(RDFUtil.getQuerySelectResults(kbSourceFinal,
					"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
							+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"
							+ "PREFIX prop-fr: <http://fr.dbpedia.org/property/>"
							+ "PREFIX foaf: <http://xmlns.com/foaf/0.1/>"
							+ "PREFIX dbpedia-owl: <http://dbpedia.org/ontology/>" + "" + "SELECT DISTINCT * WHERE {"
							+ "  OPTIONAL { ?s a ?type . } "
							+ "  OPTIONAL { ?s foaf:name ?name . }" + "  OPTIONAL { ?s rdfs:label ?label . }" 
							+ " FILTER (STRSTARTS(STR(?type), 'http://dbpedia.org/ontology/')) . "
							+ "}"));
		} catch (QueryParseException | HttpHostConnectException | MalformedURLException | HttpException e) {
			logger.error(e);
		}
		qSolutionsKB.parallelStream().forEach(querySolution -> {
			Resource candidateResource = kbSource.createResource(RDFUtil.getURIOrLexicalForm(querySolution, "s"));
			String candidateLabel = RDFUtil.getURIOrLexicalForm(querySolution, "label");
			String candidateName = RDFUtil.getURIOrLexicalForm(querySolution, "name");
			if (candidateResource != null && (candidateLabel != null || candidateName != null)) {
				String candidateType = RDFUtil.getURIOrLexicalForm(querySolution, "type");
				Set<String> types = new HashSet<>();
				if (candidateType == null || candidateType.isEmpty()) {
					candidateType = "http://dbpedia.org/ontology/Place";
				}
				types.add(candidateType);
				result.add(new Candidate(candidateResource, candidateLabel, candidateName, types));
			}
		});
		Map<Resource, List<Candidate>> candidatesByResource = result.stream().filter(p -> p != null && p.getResource() != null)
				.collect(Collectors.groupingBy((Candidate c) -> c.getResource()));
		result.clear();
		for (Entry<Resource, List<Candidate>> entry : candidatesByResource.entrySet()) {
			Set<String> types = new HashSet<>();
			Candidate oldC = null;
			for (Candidate c : entry.getValue()) {
				types.addAll(c.getTypes());
				oldC = c;
			}
			if (oldC != null) {
				Candidate newC = new Candidate(oldC.getResource(), oldC.getLabel(), oldC.getName(), types);
				result.add(newC);
			}
		}
		//
//		try {
//			qSolutionsKB.addAll(RDFUtil.getQuerySelectResults(kbSourceFinal,
//					"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
//							+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"
//							+ "PREFIX prop-fr: <http://fr.dbpedia.org/property/>"
//							+ "PREFIX foaf: <http://xmlns.com/foaf/0.1/>"
//							+ "PREFIX dbpedia-owl: <http://dbpedia.org/ontology/>" + "" + "SELECT DISTINCT * WHERE {"
//							+ "  OPTIONAL { ?s foaf:name ?name . }" + "  OPTIONAL { ?s rdfs:label ?label . }" + "}"));
//		} catch (QueryParseException | HttpHostConnectException | MalformedURLException | HttpException e) {
//			logger.error(e);
//		}
//		logger.info("Récupérations des types des candidats de la KB");
//		try {
//			qSolutionsTypes.addAll(RDFUtil.getQuerySelectResults(kbSourceFinal,
//					"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
//							+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"
//							+ "PREFIX prop-fr: <http://fr.dbpedia.org/property/>"
//							+ "PREFIX foaf: <http://xmlns.com/foaf/0.1/>" + "PREFIX dbo: <http://dbpedia.org/ontology/>"
//							+ "SELECT * WHERE {" + "  ?s rdf:type ?type . "
//							+ "  FILTER (?type=dbo:Mountain || ?type=dbo:Volcano || ?type=dbo:BodyOfWater || "
//							+ "?type=dbo:Settlement || ?type=dbo:Territory || ?type=dbo:NaturalPlace) . " + "}"));
//		} catch (QueryParseException | HttpHostConnectException | MalformedURLException | HttpException e) {
//			logger.error(e);
//		}
//		logger.info("Traitement des types des candidats de la KB");
//		List<String[]> resourceTypeMap = new ArrayList<>();
//		qSolutionsTypes.stream().forEach(qs -> {
//			String candidateResource = RDFUtil.getURIOrLexicalForm(qs, "s");
//			String candidateType = RDFUtil.getURIOrLexicalForm(qs, "type");
//			resourceTypeMap.add(new String[] { candidateResource, candidateType });
//		});
//		Map<String, List<String[]>> typesByResources = resourceTypeMap.stream()
//				.collect(Collectors.groupingBy((String[] s) -> s[0]));
//		logger.info("Traitement des candidats de la KB");
//		qSolutionsKB.parallelStream().forEach(querySolution -> {
//			Resource candidateResource = kbSource.createResource(RDFUtil.getURIOrLexicalForm(querySolution, "s"));
//			String candidateLabel = RDFUtil.getURIOrLexicalForm(querySolution, "label");
//			String candidateName = RDFUtil.getURIOrLexicalForm(querySolution, "name");
//			Set<String> types = new HashSet<>();
//			if (typesByResources.containsKey(candidateResource)) {
//				List<String[]> list = typesByResources.get(candidateResource);
//				for (String[] strings : list) {
//					types.add(strings[1]);
//				}
//			}
//			types.add("http://dbpedia.org/ontology/Place");
//			result.add(new Candidate(candidateResource, candidateLabel, candidateName, types));
//		});
		logger.info(result.size() + " résultats");
		return result;
	}

	/*
	 * Fonctions de coût
	 */
	static float getDeletionCost(Resource nodeToDelete, Set<Resource> candidateResources) {
		float cost = 0f;
		if (candidateResources.contains(nodeToDelete)) {
			cost = 1f;
		}
		return cost;
	}

	static float getInsertionCost(Resource nodeToInsert) {
		return 1f;
	}

	/**
	 * Return the cost (between 0 and 1) of the substitution of the
	 * "nodeToRemove" by "nodeToInsert"
	 *
	 * @param nodeToRemove
	 * @param nodeToInsert
	 * @param labelWeight
	 * @param rlspWeight
	 * @param linkWeight
	 * @param toponymsTEI
	 * @param teiRdf
	 * @param kbWithInterestingProperties
	 *            Subgraph of the kb with only "prop-fr:nord" likes nodes
	 * @return
	 */
	static float getSubstitutionCost(Resource nodeToRemove, Resource nodeToInsert, float labelWeight, float rlspWeight,
			float linkWeight, Set<Toponym> toponymsTEI, Model teiRdf, Model kbWithInterestingProperties) {
		// sub(n,m)=a.(1 - scoreLabel(n,m))+b.rlsp(n,m)+c.link(n,m)
		return labelWeight * (1 - scoreLabel(nodeToRemove, nodeToInsert, toponymsTEI))
				+ rlspWeight * scoreRlsp(nodeToRemove, nodeToInsert, teiRdf, toponymsTEI, kbWithInterestingProperties)
				+ linkWeight * scoreLink(nodeToRemove, nodeToInsert, teiRdf, toponymsTEI, kbWithInterestingProperties);
	}

	static float getSubstitutionCostV2(Toponym nodeToRemove, CriterionToponymCandidate candidateCriterion,
			float labelWeight, float rlspWeight, float linkWeight, Set<Toponym> toponymsTEI, Model teiRdf,
			Model kbWithInterestingProperties, Model completeKB) {
		float scoreLabel = (1 - candidateCriterion.getValue());
		float scoreLink = scoreLinkV2(nodeToRemove, candidateCriterion, teiRdf, toponymsTEI,
				kbWithInterestingProperties, completeKB);
		// rlsp : on utilise le fait que le chemin a déjà potentiellement été étudié, on vérifie que toutes les propriétés
		// des statements sont biens compatibles avec la direction de la rlsp
		float scoreRlsp = scoreRlspV2(nodeToRemove, candidateCriterion, teiRdf, toponymsTEI, kbWithInterestingProperties, completeKB);
		rlspCalculous.add(nodeToRemove.getResource() + " (" + nodeToRemove.getName() + ")" + " -> " + candidateCriterion.getCandidate().getResource() + " ("
				+ scoreLabel + "/" + scoreLink + "/" + scoreRlsp + ")");
		return labelWeight * scoreLabel + rlspWeight * scoreRlsp + linkWeight * scoreLink;
	}

	static float scoreLabel(Resource nodeToRemove, Resource nodeToInsert, Set<Toponym> toponymsTEI) {
		List<CriterionToponymCandidate> candidates = getCandidates(nodeToInsert, toponymsTEI);
		if (!candidates.isEmpty()) {
			Optional<CriterionToponymCandidate> candidate = candidates.stream()
					.filter(t -> areResourcesEqual(t.getCandidate().getResource(), nodeToRemove)).findFirst();
			if (candidate.isPresent()) {
				return candidate.get().getValue();
			}
		}
		return 0f;
	}

	static float scoreRlsp(Resource nodeToRemove, Resource nodeToInsert, Model teiRdf, Set<Toponym> toponymsTEI,
			Model kbWithInterestingProperties) {
		List<Statement> statements = getProperties(nodeToInsert, teiRdf);
		statements = keepOnlyRLSP(statements);
		if (statements.isEmpty())
			return 1f;
		float result = 0f;
		for (Statement statement : statements) {
			Resource m;
			if (statement.getSubject() == nodeToInsert) {
				m = (Resource) statement.getObject();
			} else {
				m = statement.getSubject();
			}
			Property statementProperty = statement.getPredicate();
			List<Property> properties = getCorrespondingProperties(statementProperty);
			PredicatesFilter filter = new PredicatesFilter(properties);
			Map<Resource, Integer> pathLength = new HashMap<>();
			List<CriterionToponymCandidate> candidates = getCandidates(nodeToInsert, toponymsTEI);
			for (CriterionToponymCandidate criterionToponymCandidate : candidates) {
				Resource end = criterionToponymCandidate.getCandidate().getResource();
				OntTools.Path path = OntTools.findShortestPath(kbWithInterestingProperties, nodeToRemove, end, filter);
				if (path != null) { // à revoir
					pathLength.put(end, path.size());
				}
			}
			Optional<Integer> maxPathLength = pathLength.values().stream().max(Integer::compare);
			if (maxPathLength.isPresent()) {
				result += pathLength.keySet().stream().map(key -> {
					Integer sp = pathLength.get(key);
					return (1 - scoreLabel(key, m, toponymsTEI)) * ((float) sp) / ((float) maxPathLength.get());
				}).min(Float::compareTo).get();
			}

		}

		return result / ((float) statements.size());
	}

	static float scoreRlspV2(Toponym nodeToRemove, CriterionToponymCandidate nodeToInsert, Model teiRdf,
			Set<Toponym> toponymsTEI, Model kbWithInterestingProperties, Model completeKB) {
		List<Statement> statements = getProperties(nodeToRemove.getResource(), teiRdf);
		statements = keepOnlyRLSP(statements);
		if (statements.isEmpty())
			return 1f;
		float result = 0f;
		for (Statement statement : statements) {
			Resource m;
			if (areResourcesEqual(statement.getSubject(), nodeToRemove.getResource())) {
				m = (Resource) statement.getObject();
			} else {
				m = statement.getSubject();
			}
			Property statementProperty = statement.getPredicate();
			List<Property> properties = getCorrespondingProperties(statementProperty);
			PredicatesFilter filter = new PredicatesFilter(properties);
			Map<CriterionToponymCandidate, Integer> pathLength = new HashMap<>();
			List<CriterionToponymCandidate> candidates = getReferent(m, toponymsTEI);
			if (candidates.isEmpty())
				candidates.addAll(getCandidates(m, toponymsTEI)); // ici il faut prendre les candidats seulement si le topo n'a pas été désambiguisé, sinon on utilise son référent
			candidates.parallelStream().forEach(criterionToponymCandidate -> {
			//for (CriterionToponymCandidate criterionToponymCandidate : candidates) {
				Resource end = criterionToponymCandidate.getCandidate().getResource();
				if (scoresRlspTmp.containsKey(statementProperty.toString())
						&& scoresRlspTmp.get(statementProperty.toString()).containsKey(end)
						&& scoresRlspTmp.get(statementProperty.toString()).get(end)
								.containsKey(nodeToInsert.getCandidate().getResource())) {
					pathLength.put(criterionToponymCandidate, scoresRlspTmp.get(statementProperty.toString()).get(end)
							.get(nodeToInsert.getCandidate().getResource()));
				} else {
//				if (scoresLinkTmp.containsKey(end) && scoresLinkTmp.get(end).containsKey(nodeToInsertCopy)) {
//					pathLength.put(criterionToponymCandidate, scoresLinkTmp.get(end).get(nodeToInsertCopy)); // pb ici, il faut tenir compte du filtre
//				} else {
					final Resource nodeToInsertCopy = nodeToInsert.getCandidate().getResource();
					OntTools.Path path = null;
					path = findShortestPathV2(kbWithInterestingProperties, nodeToInsertCopy, end, completeKB);
					boolean erasePath = false;
					if (path != null) {
						for (Statement statement2 : path) {
							Property p = statement2.getPredicate();
							if (!properties.stream().anyMatch(p2 -> p2.getURI().equals(p.getURI()))) {
								erasePath = true;
								break;
							}
						}
					}
//					if (dijkstras.containsKey(nodeToInsert.getCandidate().getResource())) {
//						DijkstraSP sp = dijkstras.get(nodeToInsert.getCandidate().getResource());
//						if (sp.hasPathTo(end)) {
//							final Path path2 = new Path();
//							sp.pathTo(end).forEach(s -> path2.add(s));
//							path = path2;
//						}
//					} else {
//						DijkstraSP sp = dijkstras.get(end);
//						if (sp.hasPathTo(nodeToInsert.getCandidate().getResource())) {
//							final Path path2 = new Path();
//							sp.pathTo(nodeToInsert.getCandidate().getResource()).forEach(s -> path2.add(s));
//							path = path2;
//						}
//					}
//					if (!path.stream().allMatch(s -> properties.contains(s.getPredicate()))) {
//						path = null;
//					}
//					ExecutorService service = Executors.newSingleThreadExecutor();
//					SearchPathThread spt = new SearchPathThread(kbWithInterestingProperties, nodeToInsert.getCandidate().getResource(), end, filter, completeKB);
//					try {
//						path = service.submit(spt).get(timeout, TimeUnit.SECONDS);
//					} catch (InterruptedException | ExecutionException | TimeoutException e) {
//						logger.error(e + " : " + nodeToInsert.getCandidate().getResource() + " -> " + end);
//					}
////					OntTools.Path path = findShortestPathWithFilter(kbWithInterestingProperties,
////							nodeToInsert.getCandidate().getResource(), end, filter, completeKB);
//					if (path != null) {
//						recordLinkPath(scoresLinkTmp, nodeToInsertCopy, end, path.size());
//						if (!erasePath) {
//							pathLength.put(criterionToponymCandidate, path.size());
//						} else pathLength.put(criterionToponymCandidate, -1);
//					} else {
//						pathLength.put(criterionToponymCandidate, -1);
//						recordLinkPath(scoresLinkTmp, nodeToInsertCopy, end, 10000); // valeur
//																					// arbitraire
//																					// pr
//																					// signifier
//																					// que
//																					// le
//																					// chemin
//																					// est
//																					// trop
//																					// long
//					}
					if (path != null) {
						if (!erasePath) {
							pathLength.put(criterionToponymCandidate, path.size());
						} else pathLength.put(criterionToponymCandidate, -1);
						recordRlspPath(scoresRlspTmp, nodeToInsertCopy, end, path.size(),
								statementProperty.toString());
					} else {
						pathLength.put(criterionToponymCandidate, -1);
						recordRlspPath(scoresRlspTmp, nodeToInsertCopy, end, -1,
								statementProperty.toString());
					}
				}
			});
			Integer maxPathLength = getMaxValue(pathLength);
			float min = 1f;
			for (Entry<CriterionToponymCandidate, Integer> entry : pathLength.entrySet()) {
				if (entry.getValue() > 0) {
					float scoreTmp = ((float) entry.getValue()) / ((float) maxPathLength); // (1
																							// -
																							// entry.getKey().getValue())
																							// *
					if (scoreTmp < min) {
						min = scoreTmp;
					}
				}
			}
			result += min;

		}

		return result / ((float) statements.size());
	}

	static void recordRlspPath(Map<String, Map<Resource, Map<Resource, Integer>>> scoresRlspTmp, Resource r1,
			Resource r2, Integer value, String p) {
		Map<Resource, Map<Resource, Integer>> scoresRlspTmp2;
		if (scoresRlspTmp.containsKey(p)) {
			scoresRlspTmp2 = scoresRlspTmp.get(p);
		} else {
			scoresRlspTmp2 = new HashMap<>();
			scoresRlspTmp.put(p, scoresRlspTmp2);
		}
		recordLinkPath(scoresRlspTmp2, r1, r2, value);
		// if (scoresLinkTmp.containsKey(r1)) { // r1 a déjà été traité
		// Map<Resource, Integer> scoreR1 = scoresLinkTmp.get(r1);
		// if (!scoreR1.containsKey(r2)) { // r1 et r2 n'ont jamais été traités
		// ensemble
		// scoreR1.put(r2, value);
		// }
		// } else { // r1 n'a jamais été traité
		// Map<Resource, Integer> scoreR1 = new HashMap<>();
		// scoreR1.put(r2, value);
		// scoresLinkTmp.put(r1, scoreR1);
		// }
		// if (scoresLinkTmp.containsKey(r2)) { // r2 a déjà été traité
		// Map<Resource, Integer> scoreR2 = scoresLinkTmp.get(r2);
		// if (!scoreR2.containsKey(r1)) { // r1 et r2 n'ont jamais été traités
		// ensemble
		// scoreR2.put(r1, value);
		// }
		// } else {// r2 n'a jamais été traité
		// Map<Resource, Integer> scoreR2 = new HashMap<>();
		// scoreR2.put(r1, value);
		// scoresLinkTmp.put(r2, scoreR2);
		// }
	}

	static Map<String, Map<Resource, Map<Resource, Integer>>> scoresRlspTmp = new HashMap<>();

	/**
	 * Gets the corresponding DBpedia properties from the TEI's one.
	 *
	 * @param teiProperty
	 *            the tei property
	 * @return the corresponding properties
	 */
	static List<Property> getCorrespondingProperties(Property teiProperty) {
		List<Property> properties = new ArrayList<>(); // propriétés autorisées
		if (teiProperty.getURI().equalsIgnoreCase(rlspNorthOf.getURI())) {
			properties.add(propSud);
			properties.add(propSudEst);
			properties.add(propSudOuest);
			properties.add(propEst);
			properties.add(propOuest);
		} else if (teiProperty.getURI().equalsIgnoreCase(rlspNorthEastOf.getURI())) {
			properties.add(propSud);
			properties.add(propSudOuest);
			properties.add(propOuest);
		} else if (teiProperty.getURI().equalsIgnoreCase(rlspNorthWestOf.getURI())) {
			properties.add(propSud);
			properties.add(propSudEst);
			properties.add(propEst);
		} else if (teiProperty.getURI().equalsIgnoreCase(rlspSouthOf.getURI())) {
			properties.add(propNord);
			properties.add(propNordEst);
			properties.add(propNordOuest);
			properties.add(propEst);
			properties.add(propOuest);
		} else if (teiProperty.getURI().equalsIgnoreCase(rlspSouthEastOf.getURI())) {
			properties.add(propNord);
			properties.add(propNordOuest);
			properties.add(propOuest);
		} else if (teiProperty.getURI().equalsIgnoreCase(rlspSouthWestOf.getURI())) {
			properties.add(propNord);
			properties.add(propNordEst);
			properties.add(propEst);
		} else if (teiProperty.getURI().equalsIgnoreCase(rlspEastOf.getURI())) {
			properties.add(propNord);
			properties.add(propNordOuest);
			properties.add(propSudOuest);
			properties.add(propOuest);
			properties.add(propSud);
		} else if (teiProperty.getURI().equalsIgnoreCase(rlspWestOf.getURI())) {
			properties.add(propNord);
			properties.add(propNordEst);
			properties.add(propSudEst);
			properties.add(propEst);
			properties.add(propSud);
		}
		return properties;
	}

	static float scoreLink(Resource nodeToRemove, Resource nodeToInsert, Model teiRdf, Set<Toponym> toponymsTEI,
			Model kbWithInterestingProperties) {
		float result = 0f;
		List<Statement> statements = getProperties(nodeToInsert, teiRdf);
		statements = removeRLSP(statements);
		if (statements.isEmpty())
			return result;

		List<Property> properties = new ArrayList<>();
		properties.add(propNord);
		properties.add(propNordEst);
		properties.add(propNordOuest);
		properties.add(propSud);
		properties.add(propSudEst);
		properties.add(propSudOuest);
		properties.add(propEst);
		properties.add(propOuest);
		PredicatesFilter filter = new PredicatesFilter(properties);
		for (Statement statement : statements) {
			Resource m;
			if (areResourcesEqual(statement.getSubject(), nodeToInsert)) {
				m = (Resource) statement.getObject();
			} else {
				m = statement.getSubject();
			}
			List<CriterionToponymCandidate> candidates = getCandidates(nodeToInsert, toponymsTEI);
			Map<Resource, Integer> pathLength = new HashMap<>();
			for (CriterionToponymCandidate criterionToponymCandidate : candidates) {
				Resource end = criterionToponymCandidate.getCandidate().getResource();
				OntTools.Path path = OntTools.findShortestPath(kbWithInterestingProperties, nodeToRemove, end, filter);
				pathLength.put(end, path.size());
			}
			Optional<Integer> maxPathLength = pathLength.values().stream().max(Integer::compare);
			if (maxPathLength.isPresent()) {
				result += pathLength.keySet().stream().map(key -> {
					Integer sp = pathLength.get(key);
					return (1 - scoreLabel(key, m, toponymsTEI)) * ((float) sp) / ((float) maxPathLength.get());
				}).min(Float::compareTo).get();
			}
		}

		return result / ((float) statements.size());
	}

	static float scoreLinkV2(Toponym toponym, CriterionToponymCandidate criterion, Model teiRdf,
			Set<Toponym> toponymsTEI, Model kbWithInterestingProperties, Model completeKB) {
		Resource nodeToRemove = toponym.getResource();
		Resource nodeToInsert = criterion.getCandidate().getResource();
		float result = 0f;
		List<Statement> statements = getProperties(nodeToRemove, teiRdf);
		statements = removeRLSP(statements);
		if (statements.isEmpty())
			return result;
		// pour chaque statement (non RLSP) du noeud du TEI
		for (Statement s : statements) {
			final Statement statement = s;
			Resource m; // resource liée au noeud à supprimer
			if (areResourcesEqual(statement.getSubject(), nodeToRemove)) {
				m = (Resource) statement.getObject();
			} else {
				m = statement.getSubject();
			}
			List<CriterionToponymCandidate> candidates = getReferent(m, toponymsTEI);
			if (candidates.isEmpty())
				candidates.addAll(getCandidates(m, toponymsTEI)); // ici il faut prendre les candidats seulement si le topo n'a pas été désambiguisé, sinon on utilise son référent
			Map<CriterionToponymCandidate, Integer> pathLength = new HashMap<>();
			candidates.parallelStream().forEach(criterionToponymCandidate -> {
			//for (CriterionToponymCandidate criterionToponymCandidate : candidates) {
				Resource end = criterionToponymCandidate.getCandidate().getResource();
				final Resource nodeToInsertCopy = nodeToInsert;
				if (scoresLinkTmp.containsKey(end) && scoresLinkTmp.get(end).containsKey(nodeToInsertCopy)) {
					pathLength.put(criterionToponymCandidate, scoresLinkTmp.get(end).get(nodeToInsertCopy));
				} else {
					OntTools.Path path = null;
//					if (dijkstras.containsKey(nodeToInsert)) {
//						DijkstraSP sp = dijkstras.get(nodeToInsert);
//						if (sp.hasPathTo(end)) {
//							final Path path2 = new Path();
//							sp.pathTo(end).forEach(s -> path2.add(s));
//							path = path2;
//						}
//					} else {
//						DijkstraSP sp = dijkstras.get(end);
//						if (sp.hasPathTo(nodeToInsert)) {
//							final Path path2 = new Path();
//							sp.pathTo(nodeToInsert).forEach(s -> path2.add(s));
//							path = path2;
//						}
//					}
//					ExecutorService service = Executors.newSingleThreadExecutor();
//					SearchPathThread spt = new SearchPathThread(kbWithInterestingProperties, nodeToInsert, end, null, completeKB);
//					try {
//						path = service.submit(spt).get(timeout, TimeUnit.SECONDS);
//					} catch (InterruptedException | ExecutionException | TimeoutException e) {
//						logger.error(e + " : " + nodeToInsert + " -> " + end);
//					}
					path = findShortestPathV2(kbWithInterestingProperties, nodeToInsertCopy, end, completeKB);
//					//findShortestPath(kbWithInterestingProperties, nodeToInsert, end, completeKB)
					if (path != null) {
						pathLength.put(criterionToponymCandidate, path.size());
						recordLinkPath(scoresLinkTmp, nodeToInsertCopy, end, path.size());
					} else {
						pathLength.put(criterionToponymCandidate, -1);
						recordLinkPath(scoresLinkTmp, nodeToInsertCopy, end, 10000); // valeur
																					// arbitraire
																					// pr
																					// signifier
																					// que
																					// le
																					// chemin
																					// est
																					// trop
																					// long
					}
				}
			});
			Integer maxPathLength = getMaxValue(pathLength);
			float min = 1f;
			for (Entry<CriterionToponymCandidate, Integer> entry : pathLength.entrySet()) {
				if (entry.getValue() > 0) {
					float scoreTmp = ((float) entry.getValue()) / ((float) maxPathLength); // (1
																							// -
																							// entry.getKey().getValue())
																							// *
					if (scoreTmp < min) {
						min = scoreTmp;
					}
				}
			}
			result += min;
		}

		return result / ((float) statements.size());
	}

	static Integer getMaxValue(Map<CriterionToponymCandidate, Integer> pathLength) {
		Integer result = 0;
		for (Entry<CriterionToponymCandidate, Integer> entry : pathLength.entrySet()) {
			if (entry.getValue() > result) {
				result = entry.getValue();
			}
		}
		return result;
	}

	static void recordLinkPath(Map<Resource, Map<Resource, Integer>> scoresLinkTmp, Resource r1, Resource r2,
			Integer value) {
		if (scoresLinkTmp.containsKey(r1)) { // r1 a déjà été traité
			Map<Resource, Integer> scoreR1 = scoresLinkTmp.get(r1);
			if (!scoreR1.containsKey(r2)) { // r1 et r2 n'ont jamais été traités
											// ensemble
				scoreR1.put(r2, value);
			}
		} else { // r1 n'a jamais été traité
			Map<Resource, Integer> scoreR1 = new HashMap<>();
			scoreR1.put(r2, value);
			scoresLinkTmp.put(r1, scoreR1);
		}
		if (scoresLinkTmp.containsKey(r2)) { // r2 a déjà été traité
			Map<Resource, Integer> scoreR2 = scoresLinkTmp.get(r2);
			if (!scoreR2.containsKey(r1)) { // r1 et r2 n'ont jamais été traités
											// ensemble
				scoreR2.put(r1, value);
			}
		} else {// r2 n'a jamais été traité
			Map<Resource, Integer> scoreR2 = new HashMap<>();
			scoreR2.put(r1, value);
			scoresLinkTmp.put(r2, scoreR2);
		}
	}

	static Map<Resource, Map<Resource, Integer>> scoresLinkTmp = new HashMap<>();

	static List<CriterionToponymCandidate> getCandidates(Resource r, Set<Toponym> toponymsTEI) {
		List<CriterionToponymCandidate> results = new ArrayList<>();
		Optional<Toponym> toponym = toponymsTEI.stream().filter(t -> areResourcesEqual(t.getResource(), r)).findFirst();
		if (toponym.isPresent()) {
//			if (toponym.get().getReferent() != null) {
//				return toponym.get().getScoreCriterionToponymCandidate().stream()
//						.filter(p -> areResourcesEqual(r, p.getCandidate().getResource())).collect(Collectors.toList());
//			}
//			else 
				return toponym.get().getScoreCriterionToponymCandidate();

		}
		return results;
	}
	static List<CriterionToponymCandidate> getReferent(Resource r, Set<Toponym> toponymsTEI) {
		List<CriterionToponymCandidate> results = new ArrayList<>();
		Optional<Toponym> toponym = toponymsTEI.stream().filter(t -> areResourcesEqual(t.getResource(), r)).findFirst();
		if (toponym.isPresent()) {
			if (toponym.get().getReferent() != null) {
				Resource ref = toponym.get().getReferent();
				results.addAll(toponym.get().getScoreCriterionToponymCandidate().stream().
						filter(p -> areResourcesEqual(ref, p.getCandidate().getResource())).collect(Collectors.toList()));
			}

		}
		return results;
	}

	/**
	 * Removes the RLSP from the statements list.
	 *
	 * @param statements
	 *            the statements
	 * @return the list
	 */
	static List<Statement> removeRLSP(List<Statement> statements) {
		if (statements == null || statements.isEmpty())
			return new ArrayList<>();
		return statements.stream().filter(s -> !s.getPredicate().getNameSpace().equals(rlspNS)).collect(Collectors.toList());
	}

	static List<Statement> keepOnlyRLSP(List<Statement> statements) {
		if (statements == null || statements.isEmpty())
			return new ArrayList<>();
		return statements.stream().filter(s -> s.getPredicate().getNameSpace().equals(rlspNS))
				.collect(Collectors.toList());
	}

	/*
	 * Algo de matching
	 */
	static List<IPathMatching> graphMatching(Model kbSubgraph, Model miniGraph, Set<Toponym> toponymsTEI,
			float labelWeight, float rlspWeight, float linkWeight) {
		// on récupère les noeuds du mini graphe
		Set<Resource> targetNodes = new HashSet<>(miniGraph.listSubjects().toList());
		targetNodes.addAll(miniGraph.listObjects().toList().stream().filter(o -> o.isResource()).map(m -> (Resource) m)
				.collect(Collectors.toList()));
		// On commence par ne garder que les toponyms présents dans ce mini
		// graphe
		Set<Toponym> toponymsSeq = new HashSet<>(toponymsTEI.stream()
				.filter(p -> targetNodes.stream().anyMatch(res -> areResourcesEqual(p.getResource(), res)))
				.collect(Collectors.toList()));

		// on récupère les noeuds de dbpedia à traiter
		Set<Resource> sourceNodes = new HashSet<>(toponymsSeq.stream()
				.map(m -> m.getScoreCriterionToponymCandidate().stream().map(l -> l.getCandidate().getResource())
						.collect(Collectors.toList()))
				.flatMap(l -> l.stream()).distinct().collect(Collectors.toList()));
		List<List<IPathMatching>> open = new ArrayList<>();
		Resource firstSourceNode = kbSubgraph.createResource(toponymsSeq.stream()
				.sorted((a, b) -> Integer.compare(a.getScoreCriterionToponymCandidate().size(),
						b.getScoreCriterionToponymCandidate().size()))
				.findFirst().get().getScoreCriterionToponymCandidate().get(0).getCandidate().getResource());// null;
																											// //
																											// Définir
																											// comment
																											// on
																											// choisit
																											// ce
																											// noeud
		for (Resource targetNode : targetNodes) {
			float cost = getSubstitutionCost(firstSourceNode, targetNode, labelWeight, rlspWeight, linkWeight,
					toponymsSeq, miniGraph, kbSubgraph);
			List<IPathMatching> path = new ArrayList<>();
			path.add(new Substitution(firstSourceNode, targetNode, cost));
			open.add(path);
		}
		List<IPathMatching> pathDeletion = new ArrayList<>();
		pathDeletion.add(new Deletion(firstSourceNode, 1));
		open.add(pathDeletion);
		List<IPathMatching> pMin = null;
		while (true) {
			pMin = open.stream()
					.min((a, b) -> Float.compare(
							totalCostPath(a) + heuristicCostPath(a, kbSubgraph, miniGraph, toponymsSeq, labelWeight,
									rlspWeight, linkWeight, getSourceUnusedResources(a, sourceNodes),
									getTargetUnusedResources(a, targetNodes)),
							totalCostPath(b) + heuristicCostPath(b, kbSubgraph, miniGraph, toponymsSeq, labelWeight,
									rlspWeight, linkWeight, getSourceUnusedResources(b, sourceNodes),
									getTargetUnusedResources(b, targetNodes))))
					.get(); // définir pmin g + h
			open.remove(pMin);
			if (isCompletePath(pMin, sourceNodes, targetNodes)) {
				break;
			} else {
				// Set<Resource> usedResourcesFromTarget =
				// getTargetUsedResources(pMin); // resources du graphe cible
				// utilisées dans ce chemin
				Set<Resource> unusedResourcesFromTarget = getTargetUnusedResources(pMin, targetNodes);
				if (pMin.size() < sourceNodes.size()) {
					final List<IPathMatching> pMinFinal = new ArrayList<>(pMin);
					Resource currentSourceNode = kbSubgraph.createResource(toponymsSeq.stream()
							.filter(p -> getTargetUnusedResources(pMinFinal, targetNodes).stream()
									.anyMatch(t -> t.toString().equals(p)))
							.sorted((a, b) -> Integer.compare(a.getScoreCriterionToponymCandidate().size(),
									b.getScoreCriterionToponymCandidate().size()))
							.findFirst().get().getScoreCriterionToponymCandidate().get(0).getCandidate().getResource());// null;
																														// //
																														// Définir
																														// comment
																														// on
																														// choisit
																														// ce
																														// noeud
					for (Resource resource : unusedResourcesFromTarget) {
						List<IPathMatching> newPath = new ArrayList<>(pMin);
						float cost = getSubstitutionCost(currentSourceNode, resource, labelWeight, rlspWeight,
								linkWeight, toponymsSeq, miniGraph, kbSubgraph);
						newPath.add(new Substitution(currentSourceNode, resource, cost));
						open.add(newPath);
					}
					List<IPathMatching> newPathDeletion = new ArrayList<>(pMin);
					newPathDeletion.add(new Deletion(currentSourceNode, 1));
					open.add(newPathDeletion);
				} else {
					for (Resource resource : unusedResourcesFromTarget) {
						List<IPathMatching> newPath = new ArrayList<>(pMin);
						newPath.add(new Insertion(resource, getInsertionCost(resource)));
						open.add(newPath);
					}
				}
			}
		}

		return pMin;
	}

	static boolean areResourcesEqual(Resource r1, Resource r2) {
		if (r1.isAnon() && r2.isAnon()) {
			return r1 == r2;
		}
		String uri1 = r1.getURI();
		String uri2 = r2.getURI();
		return uri1.equalsIgnoreCase(uri2);
	}
	static void fillNodesToVisitForCurrentSeq(Model kbSubgraph, Model miniGraph, Set<Toponym> toponymsTEI) {
		// on récupère les noeuds du mini graphe
		Set<Resource> sourceNodes = new HashSet<>(miniGraph.listSubjects().toList());
		sourceNodes.addAll(miniGraph.listObjects().toList().stream().filter(o -> o.isResource()).map(m -> (Resource) m)
				.collect(Collectors.toList()));
		// On ne garde que les toponyms présents dans ce mini graphe
		Set<Toponym> toponymsSeq = new HashSet<>(toponymsTEI.stream()
				.filter(p -> sourceNodes.stream().anyMatch(res -> areResourcesEqual(p.getResource(), res)))
				.collect(Collectors.toList()));

		fillNodesToVisit(miniGraph, toponymsSeq, kbSubgraph);
	}
	static List<IPathMatching> graphMatchingV2(Model kbSubgraph, Model miniGraph, Set<Toponym> toponymsTEI,
			float labelWeight, float rlspWeight, float linkWeight, Model completeKB) {
		// on récupère les noeuds du mini graphe
		Set<Resource> sourceNodes = new HashSet<>(miniGraph.listSubjects().toList());
		sourceNodes.addAll(miniGraph.listObjects().toList().stream().filter(o -> o.isResource()).map(m -> (Resource) m)
				.collect(Collectors.toList()));
		// On ne garde que les toponyms présents dans ce mini graphe
		Set<Toponym> toponymsSeq = new HashSet<>(toponymsTEI.stream()
				.filter(p -> sourceNodes.stream().anyMatch(res -> areResourcesEqual(p.getResource(), res)))
				.collect(Collectors.toList()));
		List<Resource> usedSourceNodes = new ArrayList<>();
		// on récupère les noeuds de dbpedia à traiter
		Set<Resource> targetNodes = new HashSet<>(toponymsSeq.stream()
				.map(m -> m.getScoreCriterionToponymCandidate().stream().map(l -> l.getCandidate().getResource())
						.collect(Collectors.toList()))
				.flatMap(l -> l.stream()).distinct().collect(Collectors.toList()));
		List<List<IPathMatching>> open = new ArrayList<>(); // liste des chemins
															// à traiter
				
		
		
		logger.info("Sélection du premier noeud à traiter");
		Toponym firstToponym = getNextNodeToProcess(usedSourceNodes, toponymsSeq, miniGraph); // sélection
																					// du
																					// premier
																					// noeud
																					// à
																					// traiter
		Resource firstSourceNode = firstToponym.getResource();
		List<IPathMatching> pathDeletion = new ArrayList<>();
		float deletionCostFirstToponym = 1f; // si le topo n'a paas de candidats, il faut lui mettre 1 en cout de suppression
		if (firstToponym.getScoreCriterionToponymCandidate().isEmpty())
			deletionCostFirstToponym = 1f;
		else { // ce toponym n'a pas de candidat, on va le supprimer, il ne sera
				// pas désambiguisé
			for (CriterionToponymCandidate candidateCriterion : firstToponym.getScoreCriterionToponymCandidate()) {
				Resource targetNode = candidateCriterion.getCandidate().getResource();
				float cost = getSubstitutionCostV2(firstToponym, candidateCriterion, labelWeight, rlspWeight,
						linkWeight, toponymsSeq, miniGraph, kbSubgraph, completeKB);
				List<IPathMatching> path = new ArrayList<>();
				path.add(new Substitution(firstSourceNode, targetNode, cost));
				open.add(path);
			}
		}
		pathDeletion.add(new Deletion(firstSourceNode, deletionCostFirstToponym));
		open.add(pathDeletion);
		List<IPathMatching> pMin = null;
		logger.info("Noeud sélectionné : " + firstSourceNode);
		while (true) {
			// FONCTIOn getMinCostPath à revoir
			pMin = getMinCostPath(open, kbSubgraph, miniGraph, toponymsSeq, labelWeight, rlspWeight, linkWeight,
					sourceNodes, targetNodes, completeKB);
			updateToponymsV2(pMin, toponymsSeq);
			if (isCompletePath(pMin, sourceNodes, targetNodes)) {
				break;
			} else {
				open.clear(); // on vide la liste, car le chemin pMin est
								// forcément le meilleur
				// resources du graphe cible non utilisées dans ce chemin
				Set<Resource> unusedResourcesFromTarget = getTargetUnusedResources(pMin, targetNodes);
				if (pMin.size() < sourceNodes.size()) {
					Toponym currentToponym = getNextNodeToProcess(usedSourceNodes, toponymsSeq, miniGraph);
					Resource currentSourceNode = currentToponym.getResource();
					float deletionCost = 1f;
					if (currentToponym.getScoreCriterionToponymCandidate().isEmpty())
						deletionCost = 1f;
					else {
						for (CriterionToponymCandidate candidateCriterion : currentToponym
								.getScoreCriterionToponymCandidate()) {
							Resource resourceFromTarget = candidateCriterion.getCandidate().getResource();
							List<IPathMatching> newPath = new ArrayList<>(pMin);
							float cost = getSubstitutionCostV2(currentToponym, candidateCriterion, labelWeight,
									rlspWeight, linkWeight, toponymsSeq, miniGraph, kbSubgraph, completeKB);
							newPath.add(new Substitution(currentSourceNode, resourceFromTarget, cost));
							open.add(newPath);
						}
					}
					List<IPathMatching> newPathDeletion = new ArrayList<>(pMin);
					newPathDeletion.add(new Deletion(currentSourceNode, deletionCost));
					open.add(newPathDeletion);
				} else {
					for (Resource resource : unusedResourcesFromTarget) {
						List<IPathMatching> newPath = new ArrayList<>(pMin);
						newPath.add(new Insertion(resource, getInsertionCost(resource)));
						open.add(newPath);
					}
				}
			}
		}

		return pMin;
	}

	/**
	 * Update the toponym (in the last substitution of the path) in the sequence
	 * by keeping only the used candidate.
	 *
	 * @param pMin
	 *            the min
	 * @param toponymsSeq
	 *            the toponyms seq
	 */
	static void updateToponyms(List<IPathMatching> pMin, Set<Toponym> toponymsSeq) {
		IPathMatching lastOperation = pMin.get(pMin.size() - 1);
		if (lastOperation.getClass() == Substitution.class) {
			Substitution s = (Substitution) lastOperation;
			Resource deletedNode = s.getDeletedNode();
			Toponym topo = toponymsSeq.stream().filter(p -> areResourcesEqual(deletedNode, p.getResource())).findFirst()
					.get();
			if (areResourcesEqual(deletedNode, topo.getResource())) {
				Resource insertedNode = s.getInsertedNode();
				Optional<CriterionToponymCandidate> c = topo.getScoreCriterionToponymCandidate().stream()
						.filter(p -> areResourcesEqual(p.getCandidate().getResource(), insertedNode)).findFirst();
				if (c.isPresent()) {
					List<CriterionToponymCandidate> newList = new ArrayList<>();
					newList.add(c.get());
					topo.clearAndAddAllScoreCriterionToponymCandidate(newList);
				}
			}
		}
	}

	static void updateToponymsV2(List<IPathMatching> pMin, Set<Toponym> toponymsSeq) {
		IPathMatching lastOperation = pMin.get(pMin.size() - 1);
		if (lastOperation.getClass() == Substitution.class) {
			Substitution s = (Substitution) lastOperation;
			Resource deletedNode = s.getDeletedNode();
			Toponym topo = toponymsSeq.stream().filter(p -> areResourcesEqual(deletedNode, p.getResource())).findFirst()
					.get();
			if (areResourcesEqual(deletedNode, topo.getResource())) {
				Resource insertedNode = s.getInsertedNode();
				topo.setReferent(insertedNode); // on ajoute le referent pour
												// limiter les calculs de
												// chemins qui implique ce noeud
												// par la suite
			}
		}
	}

	/**
	 * Gets the path that costs the less and remove it from OPEN.
	 *
	 * @param open
	 *            the open
	 * @return the min cost path
	 */
	static List<IPathMatching> getMinCostPath(List<List<IPathMatching>> open, Model kbSubgraph, Model miniGraph,
			Set<Toponym> toponymsSeq, float labelWeight, float rlspWeight, float linkWeight, Set<Resource> sourceNodes,
			Set<Resource> targetNodes, Model completeKB) {
		float min = Float.MAX_VALUE;
		List<IPathMatching> pMin = null;
		for (List<IPathMatching> path : open) {
			Set<Resource> unusedSourceNodes = getSourceUnusedResources(path, sourceNodes);
			Set<Resource> unusedTargetNodes = getTargetUnusedResources(path, targetNodes);
			float g = totalCostPath(path);
			float h = heuristicCostPathV2(path, kbSubgraph, miniGraph, toponymsSeq, labelWeight, rlspWeight, linkWeight,
					unusedSourceNodes, unusedTargetNodes, completeKB);
			if (g + h < min) {
				min = g + h;
				pMin = path;
			}
		}
		if (pMin != null)
			open.remove(pMin);
		return pMin;
	}

	/**
	 * Gets the next node of the source graph to process.
	 *
	 * @param open
	 *            the open
	 * @param toponymsSeq
	 *            the toponyms seq
	 * @return the next node to process
	 */
	static Toponym getNextNodeToProcess(List<Resource> usedSourceNodes, Set<Toponym> toponymsSeq, Model miniGraph) {
		List<Statement> statements = miniGraph.listStatements().toList();
		// on veut choisir le noeud qui a le moins de liens car il y aura moins de calculs RLSP et Link à effectuer
		Map<RDFNode, Integer> nbLinks = new HashMap<>();
		for (Statement statement : statements) {
			Resource s = statement.getSubject();
			RDFNode o = statement.getObject();
			if (nbLinks.containsKey(s)) {
				Integer i = nbLinks.get(s);
				i++;
				nbLinks.put(s, i);
			} else nbLinks.put(s, 1);
			if (nbLinks.containsKey(o)) {
				Integer i = nbLinks.get(o);
				i++;
				nbLinks.put(o, i);
			} else nbLinks.put(o, 1);
		}
		RDFNode n = nbLinks.entrySet().stream().filter(p -> !usedSourceNodes.contains(p.getKey())).sorted((a, b) ->
				Integer.compare(a.getValue(), b.getValue())).limit(1).findFirst().get().getKey();
//		List<Toponym> sortedToponyms = toponymsSeq.stream().sorted((t1, t2) -> Integer
//				.compare(t1.getScoreCriterionToponymCandidate().size(), t2.getScoreCriterionToponymCandidate().size()))
//				.collect(Collectors.toList());
//		for (Toponym toponym : sortedToponyms) {
//			Resource resourceToCheck = toponym.getResource();
//			if (!usedSourceNodes.contains(resourceToCheck)) {
//				usedSourceNodes.add(resourceToCheck);
//				return toponym;
//			}
//		}
		for (Toponym toponym : toponymsSeq) {
			Resource resourceToCheck = toponym.getResource();
			if (areResourcesEqual(resourceToCheck, (Resource)n)) {
				usedSourceNodes.add(resourceToCheck);
				return toponym;
			}
		}
		return null;
	}

	/**
	 * Gets the used resources of the target graph in the current path.
	 *
	 * @param path
	 *            the path
	 * @return the target resources used
	 */
	static Set<Resource> getTargetUsedResources(List<IPathMatching> path) {
		Set<Resource> usedResources = new HashSet<>();
		for (IPathMatching pathElement : path) {
			if (pathElement.getClass() == Substitution.class) {
				Substitution sub = (Substitution) pathElement;
				usedResources.add(sub.getInsertedNode());
			} else if (pathElement.getClass() == Insertion.class) {
				Insertion sub = (Insertion) pathElement;
				usedResources.add(sub.getInsertedNode());
			}
		}
		return usedResources;
	}

	static Set<Resource> getTargetUnusedResources(List<IPathMatching> path, Set<Resource> targetResources) {
		Set<Resource> unusedResources = new HashSet<>(targetResources);
		unusedResources.removeAll(getTargetUsedResources(path));
		return unusedResources;
	}

	/**
	 * Gets the used resources of the source graph in the current path.
	 *
	 * @param path
	 *            the path
	 * @return the source used resources
	 */
	static Set<Resource> getSourceUsedResources(List<IPathMatching> path) {
		Set<Resource> usedResources = new HashSet<>();
		for (IPathMatching pathElement : path) {
			if (pathElement.getClass() == Substitution.class) {
				Substitution sub = (Substitution) pathElement;
				usedResources.add(sub.getDeletedNode());
			} else if (pathElement.getClass() == Deletion.class) {
				Deletion sub = (Deletion) pathElement;
				usedResources.add(sub.getDeletedNode());
			}
		}
		return usedResources;
	}

	static Set<Resource> getSourceUnusedResources(List<IPathMatching> path, Set<Resource> sourceResources) {
		Set<Resource> unusedResources = new HashSet<>(sourceResources);
		unusedResources.removeAll(getTargetUsedResources(path));
		return unusedResources;
	}

	static boolean isCompletePath(List<IPathMatching> path, Set<Resource> sourceNodes, Set<Resource> targetNodes) {
		Set<Resource> usedNodesFromTarget = getTargetUsedResources(path);
		Set<Resource> usedNodesFromSource = getSourceUsedResources(path);
		return sourceNodes.stream().allMatch(usedNodesFromSource::contains)
				&& targetNodes.stream().allMatch(usedNodesFromTarget::contains);
	}

	static interface IPathMatching {

		/**
		 * Gets the cost of the path.
		 *
		 * @return the cost
		 */
		float getCost();
	}

	static class Insertion implements IPathMatching {
		private Resource insertedNode;
		private float cost;

		public Insertion(Resource insertedNode, float cost) {
			this.insertedNode = insertedNode;
			this.cost = cost;
		}

		public Resource getInsertedNode() {
			return this.insertedNode;
		}

		@Override
		public float getCost() {
			return this.cost;
		}

		@Override
		public String toString() {
			return "Insertion : " + this.insertedNode + " (" + this.cost + ")";
		}
	}

	static class Deletion implements IPathMatching {
		private Resource deletedNode;
		private float cost;

		public Deletion(Resource deletedNode, float cost) {
			this.deletedNode = deletedNode;
			this.cost = cost;
		}

		public Resource getDeletedNode() {
			return this.deletedNode;
		}

		@Override
		public float getCost() {
			return this.cost;
		}

		@Override
		public String toString() {
			return "Deletion : " + this.deletedNode + " (" + this.cost + ")";
		}
	}

	static class Substitution implements IPathMatching {
		private Resource deletedNode;
		private Resource insertedNode;
		private float cost;

		public Substitution(Resource deletedNode, Resource insertedNode, float cost) {
			this.deletedNode = deletedNode;
			this.insertedNode = insertedNode;
			this.cost = cost;
		}

		public Resource getDeletedNode() {
			return this.deletedNode;
		}

		public Resource getInsertedNode() {
			return this.insertedNode;
		}

		@Override
		public float getCost() {
			return this.cost;
		}

		@Override
		public String toString() {
			return "Substitution : " + this.deletedNode + " -> " + this.insertedNode + " (" + this.cost + ")";
		}
	}

	/**
	 * Total cost path. (g)
	 *
	 * @param path
	 *            the path
	 * @return the float
	 */
	static float totalCostPath(List<IPathMatching> path) {
		float result = 0f;
		if (path == null)
			return result;
		for (IPathMatching iPathMatching : path) {
			result += iPathMatching.getCost();
		}
		return result;
	}

	/**
	 * Heuristic cost of the path. (h)
	 * 
	 * @param path
	 * @return
	 */
	static float heuristicCostPath(List<IPathMatching> path, Model kbSubgraph, Model miniGraph,
			Set<Toponym> toponymsTEI, float labelWeight, float rlspWeight, float linkWeight,
			Set<Resource> unusedSourceNodes, Set<Resource> unusedTargetNodes) {
		float result = 0f;
		/*
		 * h(o)= somme des min{n1, n2} substitutions les moins chères + max{0,
		 * n1 − n2} suppression + max{0, n2 − n1} insertions (si l’on pose
		 * n1=nombre de noeuds non traités du graphe source et n2=nombre de
		 * noeuds non traités du graphe cible).
		 */
		Integer n1 = unusedSourceNodes.size();
		Integer n2 = unusedTargetNodes.size();
		List<Float> substitutionCosts = new ArrayList<>();
		for (Resource unusedSourceNode : unusedSourceNodes) {
			for (Resource unusedTargetNode : unusedTargetNodes) {
				substitutionCosts.add(getSubstitutionCost(unusedSourceNode, unusedTargetNode, labelWeight, rlspWeight,
						linkWeight, toponymsTEI, miniGraph, kbSubgraph));
			}
		}
		result += substitutionCosts.stream().sorted((a, b) -> Float.compare(b, a)).limit(Integer.min(n1, n2))
				.mapToDouble(i -> i).sum();
		result += (float) Integer.max(0, n1 - 2) + (float) Integer.max(0, n2 - n1);
		return result;
	}

	static float heuristicCostPathV2(List<IPathMatching> path, Model kbSubgraph, Model miniGraph,
			Set<Toponym> toponymsTEI, float labelWeight, float rlspWeight, float linkWeight,
			Set<Resource> unusedSourceNodes, Set<Resource> unusedTargetNodes, Model completeKB) {
		float result = 0f;
		/*
		 * h(o)= somme des min{n1, n2} substitutions les moins chères + max{0,
		 * n1 − n2} suppression + max{0, n2 − n1} insertions (si l’on pose
		 * n1=nombre de noeuds non traités du graphe source et n2=nombre de
		 * noeuds non traités du graphe cible).
		 */
		Integer n1 = unusedSourceNodes.size();
		Integer n2 = unusedTargetNodes.size();
		List<Float> substitutionCosts = new ArrayList<>();
		for (Resource unusedSourceNode : unusedSourceNodes) {
			List<Float> substitutionCostsCurrentToponym = new ArrayList<>();
			Toponym unusedToponym = toponymsTEI.stream()
					.filter(t -> areResourcesEqual(t.getResource(), unusedSourceNode)).findFirst().get();
			for (CriterionToponymCandidate criterion : unusedToponym.getScoreCriterionToponymCandidate()) {
				substitutionCostsCurrentToponym.add(getSubstitutionCostV2(unusedToponym, criterion, labelWeight,
						rlspWeight, linkWeight, toponymsTEI, miniGraph, kbSubgraph, completeKB));
			}
			if (!substitutionCostsCurrentToponym.isEmpty())
				substitutionCosts.add(substitutionCostsCurrentToponym.stream().sorted((a, b) -> Float.compare(b, a))
						.findFirst().get());
		}
		result += substitutionCosts.stream().sorted((a, b) -> Float.compare(b, a)).limit(Integer.min(n1, n2))
				.mapToDouble(i -> i).sum();
		result += (float) Integer.max(0, n1 - 2) + (float) Integer.max(0, n2 - n1);
		return result;
	}

	static class CustomPropertyComparator implements Comparator<Property> {

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

	static class CustomResourceComparator implements Comparator<Resource> {

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
		private Integer id;

		public QuerySolutionEntry(Resource sequence, Resource route, Resource bag, Resource waypoint,
				Resource spatialReference, Resource spatialReferenceAlt, Integer id) {
			this.sequence = sequence;
			this.route = route;
			this.bag = bag;
			this.waypoint = waypoint;
			this.spatialReference = spatialReference;
			this.spatialReferenceAlt = spatialReferenceAlt;
			this.id = id;
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

		public Integer getId() {
			return this.id;
		}
	}

	static class SearchPathThread implements Callable<OntTools.Path> {
		private Model kbWithInterestingProperties;
		private Model comleteModel;
		private Resource nodeToInsert;
		private Resource end;
		private Predicate<Statement> filter;
		private OntTools.Path path;

		public SearchPathThread(Model kbWithInterestingProperties, Resource nodeToInsert, Resource end,
				Predicate<Statement> filter, Model comleteModel) {
			this.kbWithInterestingProperties = kbWithInterestingProperties;
			this.nodeToInsert = nodeToInsert;
			this.end = end;
			this.filter = filter;
			this.path = null;
			this.comleteModel = comleteModel;
		}

		@Override
		public OntTools.Path call() {
			if (this.filter == null) {
				this.path = findShortestPathV2(kbWithInterestingProperties, nodeToInsert, end, comleteModel);
			} else {
				this.path = findShortestPathWithFilter(kbWithInterestingProperties, nodeToInsert, end, filter, comleteModel);
			}
			return this.path;
		}
	}

	static float getLatitude(Resource r, Model m) {
		float result = 0f;
		Statement s = m.getProperty(r, propLat);

		if (s == null) {
			s = m.getProperty(r, geoLat);
		}
		if (s != null) {
			RDFNode latNode = s.getObject();
			if (latNode.isLiteral()) {
				Literal latLiteral = (Literal) latNode;
				Object latObject = latLiteral.getValue();
				if (latObject != null) {
					String latString = latObject.toString();
					if (latString != null) {
						try {
							result = Float.parseFloat(latString);
						} catch (NumberFormatException e) {
							logger.error(e);
						}
					}
				}
			}
		}
		return result;
	}
	
	static boolean hasLatAndLong(Resource r, Model m) {
		Statement sLat = m.getProperty(r, propLat);
		if (sLat == null) {
			sLat = m.getProperty(r, geoLat);
		}
		Statement sLong = m.getProperty(r, propLong);
		if (sLong == null) {
			sLong = m.getProperty(r, geoLong);
		}
		return sLat != null && sLong != null;
	}

	static float getLongitude(Resource r, Model m) {
		float result = 0f;
		Statement s = m.getProperty(r, propLong);

		if (s == null) {
			s = m.getProperty(r, geoLong);
		}
		if (s != null) {
			RDFNode latNode = s.getObject();
			if (latNode.isLiteral()) {
				Literal latLiteral = (Literal) latNode;
				Object latObject = latLiteral.getValue();
				if (latObject != null) {
					String latString = latObject.toString();
					if (latString != null) {
						try {
							result = Float.parseFloat(latString);
						} catch (NumberFormatException e) {
							logger.error(e);
						}
					}
				}
			}
		}
		return result;
	}

	static float distance(float latX, float longX, float latY, float longY) {
		float latDist = Math.abs(latX - latY);
		float longDist = Math.abs(longX - longY);
		double distance = Math.sqrt(longDist * longDist + latDist * latDist);
		return (float) distance;
	}

	/**
	 * Find shortest path.
	 *
	 * @param m
	 *            the m sub graph of mComplete
	 * @param start
	 *            the start
	 * @param end
	 *            the end
	 * @param onPath
	 *            the on path
	 * @param mComplete
	 *            the m complete
	 * @return the path
	 */
	public static Path findShortestPath(Model m, Resource start, Resource end, Model mComplete) {
		// On récupère les latitudes et longitudes des points d'arrivées
		float endLat = getLatitude(end, mComplete);
		float endLong = getLongitude(end, mComplete);
//		float startLat = getLatitude(start, m);
//		float startLong = getLongitude(start, m);
//		float epsilon = 0.00000001f;
//
//		float minDistance = distance(endLat, endLong, startLat, startLong);
		
		List<Path> bfs = new LinkedList<>();
		Set<Resource> seen = new HashSet<>();

		// initialise the paths {
		for (Iterator<Statement> i = m.listStatements(start, null, (RDFNode) null); i.hasNext();) {
			bfs.add(new Path().append(i.next()));
		}

		// search
		Path solution = null;
		
		while (solution == null && !bfs.isEmpty()) {
			Path candidate = selectMostPromisingPath(bfs, m, endLat, endLong, seen, mComplete);
			if (candidate.hasTerminus(end)) {
				solution = candidate;
			} else {
				Resource terminus = candidate.getTerminalResource();
				if (terminus != null) {
					seen.add(terminus);
					
//					float terminusLat = getLatitude(end, mComplete);
//					float terminusLong = getLongitude(end, mComplete);
//					float distance = distance(endLat, endLong, terminusLat, terminusLong);
//					if (minDistance > distance || (Math.abs(terminusLat - 0f) < epsilon && Math.abs(terminusLong - 0f) < epsilon)) {
//						minDistance = distance;
					// breadth-first expansion
						for (Iterator<Statement> i = terminus.listProperties(); i.hasNext();) {
							Statement link = i.next();
	
							// no looping allowed, so we skip this link if it takes
							// us to a node we've seen
							if (!seen.contains(link.getObject())) {
								bfs.add(candidate.append(link));
							}
						}
					//}
				}
			}
		}

		return solution;
	}
	public static Path findShortestPathV2(Model m, Resource start, Resource end, Model mComplete) {
		// search
		Path solution = null;
		// On récupère les latitudes et longitudes des points d'arrivées
		Resource startResource = start;
		Resource endResource = end;
		boolean endHasCoords = hasLatAndLong(endResource, mComplete);
		boolean startHasCoords = hasLatAndLong(startResource, mComplete);
		float endLat = 0.0f;
		float endLong = 0.0f;
		float startLat = 0.0f;
		float startLong = 0.0f;
		float distanceBetweendStartAndEnd = -1.0f; // distance between start and end points
		if (endHasCoords && startHasCoords) {
			//logger.info("les 2 ont des coordonnées")
			endLat = getLatitude(endResource, mComplete);
			endLong = getLongitude(endResource, mComplete);
			startLat = getLatitude(startResource, mComplete);
			startLong = getLongitude(startResource, mComplete);
			distanceBetweendStartAndEnd = distance(endLat, endLong, startLat, startLong);
		} else  if (startHasCoords) {
			logger.info("start a des coordonnées"); // OBSOLETE -> mais pas end, donc on inverse la recherche de chemin pour pouvoir utiliser l'heuristique malgré tout
//			endLat = getLatitude(startResource, mComplete);
//			endLong = getLongitude(startResource, mComplete);
//			startResource = end;
//			endResource = start;
			startLat = getLatitude(startResource, mComplete);
			startLong = getLongitude(startResource, mComplete);
			// il faut s'occuper de end
			Resource closestEnd = getClosestResourceWithCoordinates(m, endResource, mComplete);
			if (closestEnd != null) {
				endLat = getLatitude(closestEnd, mComplete);
				endLong = getLongitude(closestEnd, mComplete);
				distanceBetweendStartAndEnd = distance(endLat, endLong, startLat, startLong);
			}
			
		} else  if (endHasCoords) {
			logger.info("end a des coordonnées");
			endLat = getLatitude(endResource, mComplete);
			endLong = getLongitude(endResource, mComplete);
			// il faut s'occuper de start
			Resource closestStart = getClosestResourceWithCoordinates(m, startResource, mComplete);
			if (closestStart != null) {
				startLat = getLatitude(closestStart, mComplete);
				startLong = getLongitude(closestStart, mComplete);
				distanceBetweendStartAndEnd = distance(endLat, endLong, startLat, startLong);
			}
			
		} else {
			// aucun n'a de topo, il faut s'occuper start et end
			logger.info("aucun n'a de coordonnées");
			Resource closestEnd = getClosestResourceWithCoordinates(m, endResource, mComplete);
			if (closestEnd != null) {
				endLat = getLatitude(closestEnd, mComplete);
				endLong = getLongitude(closestEnd, mComplete);
			}
			Resource closestStart = getClosestResourceWithCoordinates(m, startResource, mComplete);
			if (closestStart != null) {
				startLat = getLatitude(closestStart, mComplete);
				startLong = getLongitude(closestStart, mComplete);
			}
			if (closestEnd != null && closestStart != null) {
				distanceBetweendStartAndEnd = distance(endLat, endLong, startLat, startLong);
			}
		}
		
//		float epsilon = 0.00000001f;
//
//		float minDistance = distance(endLat, endLong, startLat, startLong);
		
		List<Path> forwardStatements = new LinkedList<>();
		List<Path> backwardStatements = new LinkedList<>();
		Set<Resource> forwardSeen = new HashSet<>();
		Set<Resource> backwardSeen = new HashSet<>();

		// initialise the paths
		for (Iterator<Statement> i = m.listStatements(startResource, null, (RDFNode) null); i.hasNext();) {
			forwardStatements.add(new Path().append(i.next()));
		}
		for (Iterator<Statement> i = m.listStatements(null, null, (RDFNode) endResource); i.hasNext();) {
			backwardStatements.add(new Path().append(i.next()));
		}

		
		while (solution == null && !forwardStatements.isEmpty() && !backwardStatements.isEmpty()) {
			Path forwardCandidate = selectMostPromisingPath(forwardStatements, m, endLat, endLong, forwardSeen, mComplete);
			Path backwardCandidate = selectMostPromisingPathBackward(backwardStatements, m, startLat, startLong, backwardSeen, mComplete);
			if (forwardCandidate == null || backwardCandidate == null) {
				break;
			} else if (forwardCandidate != null && endResource != null && forwardCandidate.hasTerminus(endResource)) {
				solution = forwardCandidate;
			} else if(backwardCandidate != null && ! backwardCandidate.isEmpty() && backwardCandidate.get(0) != null && backwardCandidate.get(0).getSubject() != null  
					&& areResourcesEqual(backwardCandidate.get(0).getSubject(), startResource)) {
				solution = backwardCandidate;
			} else if (forwardSeen.stream().anyMatch(p -> backwardSeen.contains(p))) { 
				// les frontières se touchent
				// il faut trouver le bon chemin et le renvoyer
				//logger.info("Fusion de frontières")
				forwardSeen.retainAll(backwardSeen);
				Optional<Resource> optRes = forwardSeen.stream().findFirst();
				if (optRes.isPresent()) {
					Resource r = optRes.get();
					Optional<Path> pF = forwardStatements.stream().filter(l -> l.stream().anyMatch(p -> areResourcesEqual(p.getSubject(), r ) || (p.getObject().isResource() && areResourcesEqual((Resource)p.getObject(), r )))).limit(1).findFirst();
					Optional<Path> pB = backwardStatements.stream().filter(l -> l.stream().anyMatch(p -> areResourcesEqual(p.getSubject(), r ) || (p.getObject().isResource() && areResourcesEqual((Resource)p.getObject(), r )))).limit(1).findFirst();
					if (pF.isPresent() && pB.isPresent()) {
						Path pathForward = pF.get();
						Path pathBackward = pB.get();
						Path newPath = new Path();
						for (Statement statement : pathForward) {
							newPath.add(statement);
							if (areResourcesEqual(statement.getSubject(), r))
								break;
						}
						boolean seen = false; // tant que l'élément commun r n'a pas été vu dans pathBackward, on n'insere pas les statements
						for (Statement statement : pathBackward) {
							if (seen)
								newPath.add(statement);
							if (statement.getObject().isResource() && areResourcesEqual((Resource)statement.getObject(), r))
								seen = true;
						}
						solution = newPath;
					}
				}
				break;
			}
			else {
				Resource terminus = forwardCandidate.getTerminalResource();
				if (terminus != null) {					
					if (wrongDistance(distanceBetweendStartAndEnd, startLat, startLong, terminus, mComplete)) {
						break;
					}
					forwardSeen.add(terminus);
					
					// breadth-first expansion
					for (Iterator<Statement> i = terminus.listProperties(); i.hasNext();) {
						Statement link = i.next();

						// no looping allowed, so we skip this link if it takes
						// us to a node we've seen
						if (!forwardSeen.contains(link.getObject()) && forwardCandidate.size() < maxLengthForSP) {
							forwardStatements.add(forwardCandidate.append(link));
						}
					}
				}
				Resource subject = backwardCandidate.get(0).getSubject();
				if (subject != null) {
					if (wrongDistance(distanceBetweendStartAndEnd, endLat, endLong, subject, mComplete)) {
						break;
					}
					backwardSeen.add(subject);
					
					// breadth-first expansion
					for (Iterator<Statement> i = m.listStatements(null, null, (RDFNode)subject); i.hasNext();) {
						Statement link = i.next();

						// no looping allowed, so we skip this link if it takes
						// us to a node we've seen
						if (!backwardSeen.contains(link.getSubject()) && backwardCandidate.size() < maxLengthForSP) {
							Path newPath = new Path();
							newPath.add(link);
							backwardCandidate.forEach(p -> newPath.add(p));
							backwardStatements.add(newPath);
						}
					}
				}
			}
		}
		return solution;
	}
	static boolean wrongDistance(float distanceBetweendStartAndEnd, float aLat, float aLong, Resource r, Model mComplete) {
		if (distanceBetweendStartAndEnd > 0.0f && hasLatAndLong(r, mComplete)) { // si distanceBetweendStartAndEnd < 0.0f, on ne peut pas s'en servir. Cela veut qu'il nous a manqué une distance d'un des points de départ ou d'arrivée
			float bLat = getLatitude(r, mComplete);
			float bLong = getLongitude(r, mComplete);
			float distanceToCompare = distance(aLat, aLong, bLat, bLong);
			if (distanceBetweendStartAndEnd * 1.5 < distanceToCompare) 
				return true;
		}
		return false;
	}
	static boolean wrongDistance(float distanceBetweendStartAndEnd, float aLat, float aLong, float bLat, float bLong) {
		if (distanceBetweendStartAndEnd > 0.0f) { // si distanceBetweendStartAndEnd < 0.0f, on ne peut pas s'en servir. Cela veut qu'il nous a manqué une distance d'un des points de départ ou d'arrivée
			float distanceToCompare = distance(aLat, aLong, bLat, bLong);
			if (distanceBetweendStartAndEnd * 1.5 < distanceToCompare) 
				return true;
		}
		return false;
	}
	static Resource getClosestResourceWithCoordinates(Model m, Resource r, Model mComplete) {
		List<Statement> properties = m.listStatements(r, null, (RDFNode)null).toList();
		for (Statement statement : properties) {
			RDFNode n = statement.getObject();
			if (n.isResource() && hasLatAndLong((Resource)n, mComplete))
				return (Resource)n;
		}
		properties = m.listStatements(null, null, (RDFNode)r).toList();
		for (Statement statement : properties) {
			Resource n = statement.getSubject();
			if (hasLatAndLong(n, mComplete))
				return n;
		}
		return null;
	}
	static int maxLengthForSP = 100; // longueur maximale qu'un chemin peut atteindre. S'il dépasse ce nombre, on s'en désintéresse
	public static Path findShortestPathWithFilter(Model m, Resource start, Resource end, Predicate<Statement> onPath,
			Model mComplete) {
		// On récupère les latitudes et longitudes des points de départ et
		// d'arrivées
		// float startLat = getLatitude(start, m);
		// float startLong = getLongitude(start, m);
		float endLat = getLatitude(end, mComplete);
		float endLong = getLongitude(end, mComplete);

		List<Path> bfs = new LinkedList<>();
		Set<Resource> seen = new HashSet<>();

		// initialise the paths
		for (Iterator<Statement> i = m.listStatements(start, null, (RDFNode) null).filterKeep(onPath); i.hasNext();) {
			bfs.add(new Path().append(i.next()));
		}
		// for (Iterator<Statement> i = m.listStatements(null, null, (RDFNode)
		// start).filterKeep(onPath); i.hasNext();) {
		// bfs.add(new Path().append(i.next()));
		// }

		// search
		Path solution = null;
		while (solution == null && !bfs.isEmpty()) {
			Path candidate = selectMostPromisingPath(bfs, m, endLat, endLong, seen, mComplete);
			if (candidate.hasTerminus(end)) {
				solution = candidate;
			} else {
				Resource terminus = candidate.getTerminalResource();
				if (terminus != null) {
					seen.add(terminus);

					// breadth-first expansion
					for (Iterator<Statement> i = terminus.listProperties().filterKeep(onPath); i.hasNext();) {
						Statement link = i.next();

						// no looping allowed, so we skip this link if it takes
						// us to a node we've seen
						if (!seen.contains(link.getObject())) {
							bfs.add(candidate.append(link));
						}
					}
				}
			}
		}

		return solution;
	}

	/**
	 * Select most promising path by using lat and long.
	 *
	 * @param bfs
	 *            the bfs
	 * @param m
	 *            the m
	 * @param endLat
	 *            the end lat
	 * @param endLong
	 *            the end long
	 * @param seen
	 *            the seen
	 * @param mComplete
	 *            the m complete
	 * @return the path
	 */
	static Path selectMostPromisingPath(List<Path> bfs, Model m, float endLat, float endLong, Set<Resource> seen, Model mComplete) {
		Path result = null;
		float shortestDistance = Float.MAX_VALUE;
		for (Path path : bfs) {
			Statement lastStatement = path.get(path.size() - 1);
			RDFNode object = lastStatement.getObject();
			if (!seen.contains(object) && object.isResource()) {
				Resource r = (Resource) object;
				if (hasLatAndLong(r, mComplete)) {
					float rLat = getLatitude(r, mComplete);
					float rLong = getLongitude(r, mComplete);
					float distance = distance(endLat, endLong, rLat, rLong);
					if (shortestDistance > distance) {
						shortestDistance = distance;
						result = path;
					}
				} else if (result == null) {
					// r n'a pas de l'attititude. On sélectionne son chemin seulement si result est null
					result = path;
				}
			}
		}
		if (result != null) {
			bfs.remove(result);
		}
		return result;
	}
	static Path selectMostPromisingPathBackward(List<Path> bfs, Model m, float startLat, float startLong, Set<Resource> seen, Model mComplete) {
		Path result = null;
		float shortestDistance = Float.MAX_VALUE;
		for (Path path : bfs) {
			Statement firstStatement = path.get(0);
			Resource r = firstStatement.getSubject();
			if (!seen.contains(r)) {
				if (hasLatAndLong(r, mComplete)) {
					float rLat = getLatitude(r, mComplete);
					float rLong = getLongitude(r, mComplete);
					float distance = distance(startLat, startLong, rLat, rLong);
					if (shortestDistance > distance) {
						shortestDistance = distance;
						result = path;
					}
				} else if (result == null) {
					// r n'a pas de l'attititude. On sélectionne son chemin seulement si result est null
					result = path;
				}
			}
		}
		if (result != null) {
			bfs.remove(result);
		}
		return result;
	}

	static class LinkResult {
		private Resource a;
		private Resource b;
		private float cost;

		public LinkResult(Resource a, Resource b, float cost) {
			this.a = a;
			this.b = b;
			this.cost = cost;
		}

		public float getCost() {
			return this.cost;
		}

		public Resource getA() {
			return this.a;
		}

		public Resource getB() {
			return this.b;
		}

		@Override
		public int hashCode() {
			return a.hashCode() + b.hashCode();
		}

		@Override
		public boolean equals(Object other) {
			if (other == null)
				return false;
			if (other == this)
				return true;
			if (!(other instanceof LinkResult))
				return false;
			LinkResult otherTyped = (LinkResult) other;
			return (this.getA().toString().equalsIgnoreCase(otherTyped.getB().toString())
					&& this.getB().toString().equalsIgnoreCase(otherTyped.getA().toString()))
					|| (this.getB().toString().equalsIgnoreCase(otherTyped.getB().toString())
							&& this.getA().toString().equalsIgnoreCase(otherTyped.getA().toString()));
		}
	}
	
	static short[][] floydWarshallAPSP(Model model) {
		/*
		  	1 let dist be a |V| × |V| array of minimum distances initialized to ∞ (infinity)
			2 for each vertex v
			3    dist[v][v] ← 0
			4 for each edge (u,v)
			5    dist[u][v] ← w(u,v)  // the weight of the edge (u,v)
			6 for k from 1 to |V|
			7    for i from 1 to |V|
			8       for j from 1 to |V|
			9          if dist[i][j] > dist[i][k] + dist[k][j] 
			10             dist[i][j] ← dist[i][k] + dist[k][j]
			11         end if
		*/
		logger.info("Algo Floyd-Warshall");
		List<Statement> statements = model.listStatements().toList();// statements = tous les statements du graph dans lequel on veut les chemins
		Set<RDFNode> nodes = new HashSet<>(model.listSubjects().toList());
		nodes.addAll(model.listObjects().toList());
		int nbVertex = nodes.size(); 
		int index = 0;
		
		for (RDFNode rdfNode : nodes) {
			indexesOfResource.put(rdfNode, index);
			resourcesOfIndex.put(index, rdfNode);
			index++;
		}
		short[][] dist = new short[nbVertex][nbVertex];
		for (int i = 0; i < nbVertex; i++) {
			for (int j = 0; j < nbVertex; j++) {
				if (i == j) {
					dist[i][j] = 0; // un noeud a un parcours de 0 à faire avec lui même
				} else {
					dist[i][j] = 127; // Integer.MAX_VALUE équivalant ici à plus l'infini
				}
			}
		}
		for (Statement s : statements) {
			int index1 = indexesOfResource.get(s.getSubject());
			int index2 = indexesOfResource.get(s.getObject());
			dist[index1][index2] = 1; // on initialise les voisins à 1
		}
		for (int k = 0; k < nbVertex; k++) {
			logger.info("étape " + k + " sur " + nbVertex);
			for (int i = 0; i < nbVertex; i++) {
				for (int j = 0; j < nbVertex; j++) {
					if (dist[i][j] > dist[i][k] + dist[k][j]) {
						dist[i][j] = (byte) (dist[i][k] + dist[k][j]);
					} 
				}
			}
		}
		return dist;
	}
	static void floydWarshallAPSPV2(Model model) {
		/*
		  	1 let dist be a |V| × |V| array of minimum distances initialized to ∞ (infinity)
			2 for each vertex v
			3    dist[v][v] ← 0
			4 for each edge (u,v)
			5    dist[u][v] ← w(u,v)  // the weight of the edge (u,v)
			6 for k from 1 to |V|
			7    for i from 1 to |V|
			8       for j from 1 to |V|
			9          if dist[i][j] > dist[i][k] + dist[k][j] 
			10             dist[i][j] ← dist[i][k] + dist[k][j]
			11         end if
		*/
		logger.info("Algo Floyd-Warshall V2");
		logger.info("Récupération des statements");
		List<Statement> statements = model.listStatements().toList();// statements = tous les statements du graph dans lequel on veut les chemins
		Set<String> nodes = new HashSet<>(model.listSubjects().toList().stream().map(m -> m.toString()).collect(Collectors.toList()));
		nodes.addAll(model.listObjects().toList().stream().map(m -> m.toString()).collect(Collectors.toList()));
		int nbVertex = nodes.size(); 
		logger.info("Création de la structure");
		Map<String, Map<String, Float>> dist2 = new ConcurrentHashMap<>(nbVertex);
//		nodes.parallelStream().forEach(n1 -> {
//			Map<String, Float> mapSecondNode = new ConcurrentHashMap<>(nbVertex);
//			nodes.parallelStream().forEach(n2 -> mapSecondNode.put(n2, n1.equals(n2) ? 0.0f : Float.POSITIVE_INFINITY));
//			dist2.put(n1, mapSecondNode);
//		});
		Map<String, Float> mapSecondNode = new ConcurrentHashMap<>(nbVertex);
		logger.info("Création de la structure 1");
		nodes.parallelStream().forEach(n2 -> mapSecondNode.put(n2, Float.POSITIVE_INFINITY));
		logger.info("Création de la structure 2");
		nodes.parallelStream().forEach(n1 -> {
			Map<String, Float> mapToAdd = new HashMap<>(mapSecondNode);
			mapToAdd.put(n1, 0.0f);
			dist2.put(n1, mapToAdd);
		});

		logger.info("Initialisation des voisins");
		// on initialise les voisins à 1
		statements.parallelStream().forEach(s -> {
			String n1 = s.getSubject().toString();
			String n2 = s.getObject().toString();
			Map<String, Float> mapToChange = dist2.get(n1);
			mapToChange.put(n2, 1.0f);
		});
		float v = dist2.get(statements.get(0).getSubject().toString()).get(statements.get(0).getObject().toString());
		logger.info(v);
		
		int k = 1;
		for (String nodeK : dist2.keySet()) {
			logger.info("étape " + k + " sur " + nbVertex);
			for (String nodeI : dist2.keySet()) {
				for (String nodeJ : dist2.keySet()) {
					float value = dist2.get(nodeI).get(nodeK) + dist2.get(nodeK).get(nodeJ);
					if (dist2.get(nodeI).get(nodeJ) > value) {
						Map<String, Float> mapToChange = dist2.get(nodeI);
						mapToChange.put(nodeJ, value);
					}
				}
			}
			k++;
		}
		
//		for (int k = 0; k < nbVertex; k++) {
//			logger.info("étape " + k + " sur " + nbVertex);
//			for (int i = 0; i < nbVertex; i++) {
//				for (int j = 0; j < nbVertex; j++) {
//					if (dist[i][j] > dist[i][k] + dist[k][j]) {
//						dist[i][j] = (byte) (dist[i][k] + dist[k][j]);
//					} 
//				}
//			}
//		}
//		
//		short[][] dist = new short[nbVertex][nbVertex];
//		for (int i = 0; i < nbVertex; i++) {
//			for (int j = 0; j < nbVertex; j++) {
//				if (i == j) {
//					dist[i][j] = 0; // un noeud a un parcours de 0 à faire avec lui même
//				} else {
//					dist[i][j] = 127; // Integer.MAX_VALUE équivalant ici à plus l'infini
//				}
//			}
//		}
//		for (Statement s : statements) {
//			int index1 = indexesOfResource.get(s.getSubject());
//			int index2 = indexesOfResource.get(s.getObject());
//			dist[index1][index2] = 1; // on initialise les voisins à 1
//		}
//		for (int k = 0; k < nbVertex; k++) {
//			logger.info("étape " + k + " sur " + nbVertex);
//			for (int i = 0; i < nbVertex; i++) {
//				for (int j = 0; j < nbVertex; j++) {
//					if (dist[i][j] > dist[i][k] + dist[k][j]) {
//						dist[i][j] = (byte) (dist[i][k] + dist[k][j]);
//					} 
//				}
//			}
//		}
	}
	
	static Map<RDFNode, Integer> indexesOfResource = new HashMap<>(); // Pour chaque resource du graphe, contient son index
	static Map<Integer, RDFNode> resourcesOfIndex = new HashMap<>(); // Pour chaque index , contient la resource correspondante
	
	static void fillNodesToVisit(Model miniGraph, Set<Toponym> toponymsSeq, Model kbSubgraph) {
		// miniGraph est un modèle d'une séquence (mini graph)
		List<Statement> statements = miniGraph.listStatements().toList();
		
		for (Statement statement : statements) {
			Optional<Toponym> subject = toponymsSeq.stream().filter(t -> areResourcesEqual(t.getResource(), statement.getSubject())).findFirst();
			Optional<Toponym> object = toponymsSeq.stream().filter(t -> areResourcesEqual(t.getResource(), (Resource)statement.getObject())).findFirst();
			if(subject.isPresent() && object.isPresent()) {
				// on utilisera celui qui a le moins de candidats, donc on vérifie ce nombre
				if (object.get().getScoreCriterionToponymCandidate().size() < subject.get().getScoreCriterionToponymCandidate().size()) {
					object = toponymsSeq.stream().filter(t -> areResourcesEqual(t.getResource(), statement.getSubject())).findFirst();
					subject = toponymsSeq.stream().filter(t -> areResourcesEqual(t.getResource(), (Resource)statement.getObject())).findFirst();
				}
				for (CriterionToponymCandidate candidateSubject : subject.get().getScoreCriterionToponymCandidate()) {
					for (CriterionToponymCandidate candidateObject : object.get().getScoreCriterionToponymCandidate()) {
						RDFNode key = candidateSubject.getCandidate().getResource();
						Set<RDFNode> set;
						if (nodesToVisit.containsKey(key)) { // si le sujet a déjà été traité on ajoute l'objet
							set = nodesToVisit.get(key);
							set.add(candidateObject.getCandidate().getResource());
						} else {
							key = candidateObject.getCandidate().getResource();
							if (nodesToVisit.containsKey(key)) { // si l'objet a déjà eté traité, on ajoute le sujet
								set = nodesToVisit.get(key);
								set.add(candidateSubject.getCandidate().getResource());
							} else { // ni le sujet ni l'objet n'ont été traités
								set = new HashSet<>();
								set.add(candidateObject.getCandidate().getResource());
								nodesToVisit.put(candidateSubject.getCandidate().getResource(), set);
							}
						}
					}
				}			
			}
		}
		
	}
	static void shortestPath(Model kbSubgraph) {
		final AtomicInteger count = new AtomicInteger();
		nodesToVisit.entrySet().parallelStream().forEach(e -> {
			RDFNode rdfNode = e.getKey();
			logger.info((count.getAndIncrement() + 1) + "/" + nodesToVisit.size());
			if (rdfNode.isResource() && !dijkstras.containsKey((Resource)rdfNode)) {
				DijkstraSP dij = new DijkstraSP(kbSubgraph, rdfNode, e.getValue());
				dijkstras.put((Resource)rdfNode, dij);			
			}
		});
	}
	static Map<RDFNode, Set<RDFNode>> nodesToVisit = new HashMap<>();
}
