package fr.ign.georeden.nelinker;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.json.JSONException;
import org.w3c.dom.Document;
import fr.ign.georeden.graph.LabeledEdge;
import fr.ign.georeden.graph.Toponym;
import fr.ign.georeden.kb.SpatialRelationship;
import fr.ign.georeden.nelinker.tei.TEIHandler;
import fr.ign.georeden.utils.GraphVisualisation;
import fr.ign.georeden.utils.OptionManager;
import fr.ign.georeden.utils.XMLUtil;

public class MainNELApp {
	
	private static Logger logger = Logger.getLogger(MainNELApp.class);
	
	private static String teiSource;
	
	private MainNELApp() {}

	public static void main(String[] args) {
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
		
		
		Document document;
		try {
			document = XMLUtil.createDocumentFromFile(teiSource);
		} catch (IOException e) {
			logger.error(e);
			optionManager.help();
			return;
		}
		
		TEIHandler teiHandler;
		SimpleDirectedGraph<Toponym, LabeledEdge<Toponym, SpatialRelationship>> graph = null;
		try {
			teiHandler = new TEIHandler(document);
			graph = teiHandler.createGraphFromTEI();
		} catch (XPathExpressionException | JSONException | ParserConfigurationException | IOException e) {
			logger.error(e);
		}

		if (graph != null) {
			GraphVisualisation<Toponym, LabeledEdge<Toponym, SpatialRelationship>> window = new GraphVisualisation<>(graph);
			window.init(1024, 768);
		}
	}

}
