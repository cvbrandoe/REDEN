package fr.ign.georeden.nelinker;

import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.json.JSONException;
import org.w3c.dom.Document;
import fr.ign.georeden.graph.LabeledEdge;
import fr.ign.georeden.graph.Toponym;
import fr.ign.georeden.kb.SpatialRelationship;
import fr.ign.georeden.nelinker.tei.ITEIHandler;
import fr.ign.georeden.nelinker.tei.TEIHandler;
import fr.ign.georeden.nelinker.tei.TEIHandlerV2;
import fr.ign.georeden.nelinker.tei.TEIUtil;
import fr.ign.georeden.talismane.TalismaneManager;
import fr.ign.georeden.utils.GraphVisualisation;
import fr.ign.georeden.utils.JSONUtil;
import fr.ign.georeden.utils.OptionManager;
import fr.ign.georeden.utils.XMLUtil;

public class MainNELApp {
	
	private static Logger logger = Logger.getLogger(MainNELApp.class);
	
	private static String teiSource;
	
	private MainNELApp() {}

	public static void main(String[] args) {
		
//		Xpath pour bag (différencier bag et séq)
//		//*[@xml:id and following-sibling::*[1][@lemma='et'] and
//		(following-sibling::*[2][@xml:id] or (
//		following-sibling::*[2][@lemma='de'] and (following-sibling::*[3][@xml:id] or following-sibling::*[4][@xml:id])))]
		
//		try {
//			TalismaneManager talismaneManager = new TalismaneManager("D:/PH/outputTalismane.txt");
//			talismaneManager.displayGraph();
//		} catch (IOException e) {
//			logger.error(e);
//		}
		
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
		
		
		Document document = XMLUtil.createDocumentFromFile(teiSource);
		if (document == null) {
			optionManager.help();
			return;
		}
		document = applyXSLTTransformations(document);
		System.out.println(document);
//		TEIHandlerV2 teiHandler;
//		SimpleDirectedGraph<Toponym, LabeledEdge<Toponym, SpatialRelationship>> graph = null;
//		try {
//			teiHandler = new TEIHandlerV2(document);
//
////			List<String> sentencesWithOrientation = teiHandler.getSentencesWithOrientation();
////			for (String string : sentencesWithOrientation) {
////				System.out.println(string);
////			}
//			graph = teiHandler.createGraphFromTEI();
//		} catch (XPathExpressionException | JSONException | ParserConfigurationException | IOException e) {
//			logger.error(e);
//		}
//
//		if (graph != null && !graph.vertexSet().isEmpty()) {
//			GraphVisualisation<Toponym, LabeledEdge<Toponym, SpatialRelationship>> window = new GraphVisualisation<>(graph);
//			window.init(1024, 768);
//		}
	}
	
	private static Document applyXSLTTransformations(Document source) {
		String[] files = null;
		Document result = source;
		try {
			files = JSONUtil.getStringArrayFromFile("transformations_to_apply", "config\\geoconfig.json");
		} catch (JSONException e) {
			logger.error(e);
		} catch (IOException e) {
			logger.error(e);
		}
		for (String file : files) {
			try {
				result = XMLUtil.applyXSLTTransformation(result, file, "temp.xml");
			} catch (TransformerException e) {
				logger.error(e);
			}
		}
		return result;
	}

}
