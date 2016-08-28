package fr.ign.georeden.nelinker;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.transform.TransformerException;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

import fr.ign.georeden.algorithms.graph.matching.GraphMatching;
//import fr.ign.georeden.graph.Toponym;
import fr.ign.georeden.algorithms.graph.matching.Toponym;
import fr.ign.georeden.utils.JSONUtil;
import fr.ign.georeden.utils.OptionManager;
import fr.ign.georeden.utils.XMLUtil;
import fr.lip6.reden.nelinker.EvalInfo;
import fr.lip6.reden.nelinker.ResultsAndEvaluationNEL;

public class MainNELApp {

	private static Logger logger = Logger.getLogger(MainNELApp.class);
	

	static final String workingDirectory = "C:\\";

	private static String teiSource;

	private MainNELApp() {
	}
	
	public static void main(String[] args) {
		
		//GraphMatching graphMatching2 = new GraphMatching(workingDirectory);
		
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

//		 GraphMatching g = new GraphMatching(document, workingDirectory + "dbpedia_fr_with_rlsp_V3.n3", workingDirectory);
//			if (true)
//				return;
		 double labelWeight = 0.3;//0.35;// 0.2; // 0.4
		 double rlspWeight = 0.4; // 0.4;  // 0.4
		 double linkWeight = 0.1;//0.05; //0.2;  // 0.1
		 double typeWeight = 0.2;  // 0.1
		 logger.info("weights : " + "label=" + labelWeight + " / rlsp=" + rlspWeight + " / link=" + linkWeight + " / type=" + typeWeight);
		GraphMatching graphMatching = new GraphMatching(document, workingDirectory + "dbpedia_fr_with_rlsp_V3.n3"
				, 15, 0.3f, workingDirectory + "serializations\\", labelWeight, rlspWeight, linkWeight, typeWeight, workingDirectory);
		if (optionManager.hasOption("shortestPaths")) {
			graphMatching.allPairShortestPathPreProcessing();
		}
		//graphMatching.test2();
		Set<Toponym> results = graphMatching.compute();
		
		evaluation(results);

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
	}
	
	/**
	 * Evaluation of the results.
	 *
	 * @param results the results
	 */
	private static void evaluation(Set<Toponym> results) {

		final Document teiSourceDocument = XMLUtil.createDocumentFromFile(teiSource);
		Document teiResults =  fillXmlWithResults(teiSourceDocument, results);
		try {
			XMLUtil.displayXml(teiResults, workingDirectory + "teiResult.xml", false);
			XMLUtil.displayXml(teiResults, workingDirectory + "goldRes\\" + "teiResult-outV3.xml", false);
		} catch (TransformerException e) {
			logger.info(e);
		}
		// namedEntityTag corresponds to the name of the tag identifying a named-entity in the TEI-XML
		String annotationTag = "placeName";
		// xpathExpresion is the XPATH expression which enables the customization of the size of the context 
		String xpathExpresion = "//p"; // //body/div
		String outDir = workingDirectory + "goldRes\\";
		// propertyTagRef is the property name of the TEI-XML named-entity tag where REDEN will store the URIs for each mention
		String propertyTagRef = "ref_auto";
		//List<Map<String, List<List<String>>>> allMentionsWithUrisPerContextinText = new ArrayList<>();
		//int max = results.stream().mapToInt(t -> t.getXmlId().intValue()).distinct().max().getAsInt();

		Map<Integer, List<String>> allMentionsWithUrisPerContextinText = new HashMap<>();
		for (Toponym toponym : results) {
			List<String> candidates = toponym.getScoreCriterionToponymCandidate().stream().map(c -> c.getCandidate().getResource().toString()).collect(Collectors.toList());
			allMentionsWithUrisPerContextinText.put(toponym.getXmlId(), candidates);
		}
//		for (int i = 0; i <= max; i++) {
//			final int index = i;
//			List<Toponym> currentToponyms = results.stream().filter(t -> t.getXmlId() == index).collect(Collectors.toList());
//			Optional<Toponym> topoOpt = currentToponyms.stream().filter(t -> t.getReferent() != null).findFirst();
//			if (!topoOpt.isPresent())
//				topoOpt = currentToponyms.stream().findFirst();
//			Toponym topo = topoOpt.isPresent() ? topoOpt.get() : null;
//			List<List<String>> listTmp = new ArrayList<>();
//			if (topo != null)
//				listTmp.add(topo.getScoreCriterionToponymCandidate().stream().map(s -> s.getCandidate().getResource().toString()).collect(Collectors.toList()));
//			allMentionsWithUrisPerContextinText.put(topo != null ? topo.getName() : "", listTmp);
//		}
		
		List<EvalInfo> collectedResults = ResultsAndEvaluationNEL.compareResultsWithGold("teiResult.xml", 
				annotationTag, xpathExpresion, outDir, propertyTagRef, allMentionsWithUrisPerContextinText);
		//compute final results
		ResultsAndEvaluationNEL.computeFinalResults(collectedResults);
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
								.filter(t -> t.getReferent() != null && t.getSubstitutionCostResult() != null)
								.sorted((t1, t2) -> Double.compare(t1.getSubstitutionCostResult().getTotalCost(), t2.getSubstitutionCostResult().getTotalCost()))
								.findFirst();
						String value = "nil";
						if (optTopo.isPresent()) {
							value = optTopo.get().getReferent().toString();
						}
						Element placeName = (Element)name.getParentNode();
						placeName.setAttribute("ref_auto", value);						
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
		//GraphMatching.saveModelToFile(workingDirectory + "dbpedia_fr_with_rlsp_V2.n3", kbSource, "N3");
	}

}
