package fr.ign.georeden.nelinker;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.cli.ParseException;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.jena.atlas.web.HttpException;
import org.apache.jena.riot.RiotException;
import org.apache.log4j.Logger;
import org.dom4j.Attribute;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.json.JSONException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

import fr.ign.georeden.algorithms.graph.matching.GraphMatchingOld;
import fr.ign.georeden.algorithms.graph.matching.GraphMatching;
import fr.ign.georeden.algorithms.string.DamerauLevenshteinAlgorithm;
import fr.ign.georeden.algorithms.string.IStringComparison;
import fr.ign.georeden.algorithms.string.StringComparisonDamLev;
import fr.ign.georeden.algorithms.string.StringComparisonMetaphone;
import fr.ign.georeden.graph.LabeledEdge;
//import fr.ign.georeden.graph.Toponym;
import fr.ign.georeden.algorithms.graph.matching.Toponym;
import fr.ign.georeden.kb.SpatialRelationship;
import fr.ign.georeden.nelinker.tei.ITEIHandler;
import fr.ign.georeden.nelinker.tei.TEIHandler;
import fr.ign.georeden.nelinker.tei.TEIHandlerV2;
import fr.ign.georeden.nelinker.tei.TEIUtil;
import fr.ign.georeden.talismane.TalismaneManager;
import fr.ign.georeden.utils.GraphVisualisation;
import fr.ign.georeden.utils.JSONUtil;
import fr.ign.georeden.utils.OptionManager;
import fr.ign.georeden.utils.RDFUtil;
import fr.ign.georeden.utils.XMLUtil;

public class MainNELApp {

	private static Logger logger = Logger.getLogger(MainNELApp.class);
	

	static final String workingDirectory = "C:\\";

	private static String teiSource;

	private MainNELApp() {
	}

	public static void main(String[] args) {

		// Xpath pour bag (différencier bag et séq)
		// //*[@xml:id and following-sibling::*[1][@lemma='et'] and
		// (following-sibling::*[2][@xml:id] or (
		// following-sibling::*[2][@lemma='de'] and
		// (following-sibling::*[3][@xml:id] or
		// following-sibling::*[4][@xml:id])))]

		// try {
		// TalismaneManager talismaneManager = new
		// TalismaneManager(workingDirectory + "PH/outputTalismane.txt");
		// talismaneManager.displayGraph();
		// } catch (IOException e) {
		// logger.error(e);
		// }

		OptionManager optionManager = new OptionManager();
		try {
			optionManager.parseArguments(args);
		} catch (ParseException e1) {
			logger.error(e1);
			optionManager.help();
			return;
		}
		if (optionManager.hasOption("help")) {
			optionManager.help();
			return;
		}

		teiSource = optionManager.getOptionValue("teiSource");
		

		 // TRANSFORMATION TEI VERS RDF
		 Document document = XMLUtil.createDocumentFromFile(teiSource);
		 if (document == null) {
			 optionManager.help();
			 return;
		 }
		 document = applyXSLTTransformations(document);
		
//		 StringComparisonDamLev sc = new StringComparisonDamLev();
//		 logger.info(sc.computeSimilarity("Gentioux", "Gentioux-Pigerolles"));
		
		GraphMatching graphMatching = new GraphMatching(document, workingDirectory + "dbpedia_fr_with_rlsp_V3.n3"
				, 10, 0.5f, workingDirectory + "serializations\\", 0.5f, 0.4f, 0.1f, workingDirectory);
		if (optionManager.hasOption("shortestPaths")) {
			graphMatching.allPairShortestPathPreProcessing();
		}
		//graphMatching.test2();
		Set<Toponym> results = graphMatching.compute();
		
		final Document teiSourceDocument = XMLUtil.createDocumentFromFile(teiSource);
		Document teiResults =  fillXmlWithResults(teiSourceDocument, results);
		try {
			XMLUtil.displayXml(teiResults, workingDirectory + "teiResult.xml", false);
		} catch (TransformerException e) {
			logger.info(e);
		}
		

		// String query =
		// "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
		// "PREFIX dbpedia-fr: <http://fr.dbpedia.org/resource/> " +
		// "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
		// "PREFIX dbpedia-owl: <http://dbpedia.org/ontology/> " +
		// "PREFIX foaf: <http://xmlns.com/foaf/0.1/> " +
		// "PREFIX prop-fr: <http://fr.dbpedia.org/property/> " +
		// "PREFIX georss: <http://www.georss.org/georss/> " +
		// "PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#> " +
		// "SELECT ?bagMember WHERE " +
		// "{" +
		// " ?seq a rdf:Seq . " +
		// " ?seq rdfs:member ?seqMember ." +
		// " ?seqMember rdf:rest*/rdf:first ?listMember ." +
		// " ?listMember rdfs:member ?bagMember ." +
		// " " +
		// "}";
		// try {
		//// Model test = RDFUtil.getQuerySelectResults(document, query);
		// for (QuerySolution solution : RDFUtil.getQuerySelectResults(document,
		// query)) {
		// String result = "";
		// for (String value : RDFUtil.getURIOrLexicalFormList(solution)) {
		// result += value + "\t";
		// }
		// System.out.println(result);
		// }
		// } catch (QueryParseException | HttpHostConnectException |
		// RiotException | MalformedURLException
		// | HttpException e) {
		// logger.error(e);
		// }

		// ANCIENNE VERSION
		// try {
		// XMLUtil.displayXml(document, null, true);
		// } catch (TransformerException e) {
		// logger.error(e);
		// }

		// TEIHandlerV2 teiHandler;
		// SimpleDirectedGraph<Toponym, LabeledEdge<Toponym,
		// SpatialRelationship>> graph = null;
		// try {
		// teiHandler = new TEIHandlerV2(document);
		//
		//// List<String> sentencesWithOrientation =
		// teiHandler.getSentencesWithOrientation();
		//// for (String string : sentencesWithOrientation) {
		//// System.out.println(string);
		//// }
		// graph = teiHandler.createGraphFromTEI();
		// } catch (XPathExpressionException | JSONException |
		// ParserConfigurationException | IOException e) {
		// logger.error(e);
		// }
		//
		// if (graph != null && !graph.vertexSet().isEmpty()) {
		// GraphVisualisation<Toponym, LabeledEdge<Toponym,
		// SpatialRelationship>> window = new GraphVisualisation<>(graph);
		// window.init(1024, 768);et
		// }
	}
	
	private static Document fillXmlWithResults(Document teiSource, Set<Toponym> toponymsTEI) {
		NodeList names = teiSource.getElementsByTagName("name");
		Map<String, List<Toponym>> toponymsById = toponymsTEI.stream()
				.collect(Collectors.groupingBy((Toponym t) -> t.getXmlId().toString()));
		for (int i = 0; i < names.getLength(); i++) {
			Element name = (Element)names.item(i);
			NamedNodeMap attributes = name.getAttributes();
			for (int j = 0; j < attributes.getLength(); j++) {
				Node currentNode = attributes.item(j); 
				if ("xml:id".equals(currentNode.getNodeName())) {
					String xmlId = currentNode.getNodeValue();
					if (xmlId != null && toponymsById.containsKey(xmlId)) {
						Optional<fr.ign.georeden.algorithms.graph.matching.Toponym> optTopo = toponymsById.get(xmlId).stream()
								.filter(t -> t.getSubstitutionCostResult() != null)
								.sorted((t1, t2) -> Float.compare(t1.getSubstitutionCostResult().getTotalCost(), t2.getSubstitutionCostResult().getTotalCost()))
								.findFirst();
						String value = "nil";
						if (optTopo.isPresent()) {
							value = optTopo.get().getReferent().toString();
						}
						Element placeName = (Element)name.getParentNode();
						placeName.setAttribute("ref", value);						
					}
				}
			}
			
		}
		return teiSource;
	}

	/**
	 * Apply XSLT transformations to the document.
	 *
	 * @param source the source
	 * @return the document
	 */
	private static Document applyXSLTTransformations(Document source) {
		logger.info("XSLT transformations");
		String[] files = null;
		Document result = source;
		try {
			files = JSONUtil.getStringArrayFromFile("transformations_to_apply", "config\\geoconfig.json");
		} catch (JSONException | IOException e) {
			logger.error(e);
		}

		for (int i = 0; i < files.length; i++) {
			try {
				result = XMLUtil.applyXSLTTransformation(result, files[i], "temp" + i + ".xml");
			} catch (TransformerException e) {
				logger.error(e);
			}
		}
		return result;
	}

	static void completeDbpedia() {
		Model kbSource = ModelFactory.createDefaultModel().read(workingDirectory + "dbpedia_fr_with_rlsp.n3");
		Model kbSourceAll = ModelFactory.createDefaultModel().read(workingDirectory + "dbpedia\\dbpedia_all.n3");
		List<Resource> subjectsKBAll = kbSourceAll.listStatements(null, kbSourceAll.createProperty("http://dbpedia.org/ontology/wikiPageWikiLink"), (RDFNode)null).toList().stream().map(s -> s.getObject()).filter(o -> o.isResource()).distinct().map(o -> (Resource)o).collect(Collectors.toList());
		Set<Resource> subjects = new HashSet<>(kbSource.listSubjects().toList());
		for (Resource resource : subjectsKBAll) {
			if (!subjects.contains(resource)) {
				List<Statement> statementsToAdd = kbSourceAll.listStatements(resource, null, (RDFNode)null).toList();
				kbSource.add(statementsToAdd);
			}
		}
		GraphMatchingOld.saveModelToFile(workingDirectory + "dbpedia_fr_with_rlsp_V2.n3", kbSource, "N3");
	}

}
