package fr.ign.georeden.nelinker.tei;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.log4j.Logger;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.json.JSONException;
import org.w3c.dom.Document;

import fr.ign.georeden.graph.LabeledEdge;
import fr.ign.georeden.graph.Toponym;
import fr.ign.georeden.kb.SpatialRelationship;
import fr.ign.georeden.utils.JSONUtil;
import fr.ign.georeden.utils.XMLUtil;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmSequenceIterator;

public class TEIHandlerV2 implements ITEIHandler<SimpleDirectedGraph<Toponym, LabeledEdge<Toponym, SpatialRelationship>>> {

	private static Logger logger = Logger.getLogger(TEIHandlerV2.class);
	private final Document teiAnnotadedFile;
	private Document teiAnnotadedFileWithBags;
	private List<Toponym> allToponyms;
	
	public TEIHandlerV2(Document originalTEIAnnotaded) throws XPathExpressionException, ParserConfigurationException, JSONException, IOException {
		String expression = JSONUtil.getStringFromFile("xpath_for_nodes_of_interest", TEIUtil.CONFIG_PATH);
		org.w3c.dom.NodeList nodes = XMLUtil.getNodeList(originalTEIAnnotaded, expression);
		teiAnnotadedFile = XMLUtil.generateXmlDocumentFromNodeList(nodes, TEIConst.TEI_ROOT,
				TEIConst.TEI_NS);
		try {
			teiAnnotadedFileWithBags = XMLUtil.applyXSLTTransformation(teiAnnotadedFile, "config/bag.xsl", "teiWithBags.xml");
		} catch (TransformerException e) {
			logger.error(e);
		}
		allToponyms = new ArrayList<>();
	}
	
	public void createRDFGraph() throws TransformerException {
		XMLUtil.applyXSLTTransformation(teiAnnotadedFileWithBags, "config/teiToRdf.xsl", "teiToRdf.xml");
	}
	
	@Override
	public SimpleDirectedGraph<Toponym, LabeledEdge<Toponym, SpatialRelationship>> createGraphFromTEI()
			throws XPathExpressionException, JSONException, IOException {
		@SuppressWarnings("unchecked")
		SimpleDirectedGraph<Toponym, LabeledEdge<Toponym, SpatialRelationship>> graph = new SimpleDirectedGraph<>(
				(Class<? extends LabeledEdge<Toponym, SpatialRelationship>>) LabeledEdge.class);
		int i = 0;
		int j = 0;
		List<XdmItem> childs = XMLUtil.getListFromXPath(teiAnnotadedFileWithBags, "/TEI/child::*", "", TEIConst.TEI_NS);
		XdmItem currentOrientation = null;
		for (XdmItem item : childs) {
			if (isOrientation(item)) {
				// On verifie ici que c'est une orientation utile, c'est à dire qu'elle n'est pas précédée d'un nom commun
				if (!isUsefullOrientation(item))
					continue;
				currentOrientation = item;
//				// affichage des orientations
//				XdmNode n = (XdmNode)item;
//				int count = 0;
//				XdmSequenceIterator iterator = n.axisIterator(Axis.PRECEDING_SIBLING);
//				String res = "";
//				while (count < 8) {
//					if (iterator.hasNext()) {
//						XdmItem next = iterator.next();
//						res = next.getStringValue().trim() + " " + res;
//					}
//					count++;
//				}
//				count = 0;
//				iterator = n.axisIterator(Axis.FOLLOWING_SIBLING);
//				res += n.getStringValue().trim() + " ";
//				while (count < 8) {
//					if (iterator.hasNext()) {
//						XdmItem next = iterator.next();
//						res += next.getStringValue().trim() + " ";
//					}
//					count++;
//				}
//				System.out.println(res.trim());
				i++;
			}
			if (isBag(item))
				j++;
		}
		System.out.println(i);
		System.out.println(j);
		System.out.println(childs.size());
		return graph;
	}
	
	
	/**
	 * Chaque élément de la liste retournée est un ensemble de phrases (1 ou plusieurs) qui contiennent au moins
	 * un toponyme ou une orientation utile.
	 *
	 * @param siblings the siblings
	 */
	@SuppressWarnings("unchecked")
	public List<List<XdmItem>> cutIntoGroups(List<XdmItem> siblings) {
		List<List<XdmItem>> results = new ArrayList<>();
		List<XdmItem> currentSentence = new ArrayList<>();
		Boolean sentenceOfInterest = false;
		for (XdmItem xdmItem : siblings) {
			XdmNode node = (XdmNode) xdmItem;
			if (TEIConst.STRONG.equalsIgnoreCase(node.getAttributeValue(new QName(TEIConst.FORCE)))) {
				if (sentenceOfInterest) {
					results.add((ArrayList<XdmItem>)((ArrayList<XdmItem>)currentSentence).clone());
					currentSentence.clear();
				}
				sentenceOfInterest = false;
			}
			else if (!sentenceOfInterest && (isBag(node) || (isOrientation(node) && isUsefullOrientation(node))))
				sentenceOfInterest = true;
			currentSentence.add(node);
		}
		return results;
	}
	
	public Boolean isUsefullOrientation(XdmItem orientation) {
		return !XMLUtil.getListFromXPath(orientation, 
				"preceding-sibling::*[1][(@type='PREPDET' or (@type='DET' and preceding-sibling::*[1][@type='PREP'])"
				+ "or @lemma='vers') and "
				+ "preceding-sibling::*[1][not(@type='N')] and preceding-sibling::*[2][not(@type='N')]]", "", 
				TEIConst.TEI_NS).isEmpty();
	}
	public Boolean isBag(XdmItem item) {
		XdmNode node = (XdmNode)item;
		if (node == null) 
			return false;
		QName nodeName = node.getNodeName();
		return "bag".equalsIgnoreCase(nodeName.getLocalName());
	}
	
	public Boolean isOrientation(XdmItem item) {
		XdmNode node = (XdmNode)item;
		if (node == null) 
			return false;
		String attribut = node.getAttributeValue(new QName("subtype"));
		if (attribut == null || !TEIConst.ORIENTATION.equalsIgnoreCase(attribut))
			attribut = node.getAttributeValue(new QName("type"));
		if (attribut == null)
			return false;
		return TEIConst.ORIENTATION.equalsIgnoreCase(attribut);
	}

	@Override
	public Document getTeiAnnotadedFile() {
		return teiAnnotadedFile;
	}

}
