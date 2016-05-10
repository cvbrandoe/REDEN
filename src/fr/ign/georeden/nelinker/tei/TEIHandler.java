package fr.ign.georeden.nelinker.tei;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.lang3.StringUtils;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.json.JSONException;
import org.w3c.dom.Document;

import fr.ign.georeden.graph.LabeledEdge;
import fr.ign.georeden.graph.Toponym;
import fr.ign.georeden.kb.SpatialRelationship;
import fr.ign.georeden.kb.ToponymType;
import fr.ign.georeden.utils.JSONUtil;
import fr.ign.georeden.utils.XMLUtil;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;

public class TEIHandler implements ITEIHandler<SimpleDirectedGraph<Toponym, LabeledEdge<Toponym, SpatialRelationship>>> {

	private final Document teiAnnotadedFile;
	private List<Toponym> allToponyms;
	
	public TEIHandler(Document originalTEIAnnotaded) throws XPathExpressionException, ParserConfigurationException, JSONException, IOException {
		String expression = JSONUtil.getStringFromFile("xpath_for_nodes_of_interest", TEIUtil.CONFIG_PATH);
		org.w3c.dom.NodeList nodes = XMLUtil.getNodeList(originalTEIAnnotaded, expression);
		teiAnnotadedFile = XMLUtil.generateXmlDocumentFromNodeList(nodes, TEIConst.TEI_ROOT,
				TEIConst.TEI_NS);
		allToponyms = new ArrayList<>();
	}

	@Override
	public Document getTeiAnnotadedFile() {
		return teiAnnotadedFile;
	}
	
	@Override
	public SimpleDirectedGraph<Toponym, LabeledEdge<Toponym, SpatialRelationship>> createGraphFromTEI()
			throws XPathExpressionException, JSONException, IOException {
		@SuppressWarnings("unchecked")
		SimpleDirectedGraph<Toponym, LabeledEdge<Toponym, SpatialRelationship>> graph = new SimpleDirectedGraph<>(
				(Class<? extends LabeledEdge<Toponym, SpatialRelationship>>) LabeledEdge.class);
		List<XdmItem> naturalPlaceList = TEIUtil.getNaturalPlaceList(teiAnnotadedFile);
		for (XdmItem element : naturalPlaceList) {
			Toponym toponym = new Toponym(TEIUtil.getWValues(element), 
					ToponymType.NATURAL_PLACE, ((XdmNode)element).getAttributeValue(TEIConst.XML_ID_QNAME));
			allToponyms.add(toponym);
			graph.addVertex(toponym);
		}
		List<XdmItem> toponymsNodeList = TEIUtil.getToponymsNodeList(teiAnnotadedFile);
		List<XdmItem> nonNaturalPlaceList = toponymsNodeList;
		nonNaturalPlaceList.removeAll(naturalPlaceList);
		for (XdmItem element : nonNaturalPlaceList) {
			Toponym toponym = new Toponym(TEIUtil.getWValues(element), 
					ToponymType.UNKNOWN, ((XdmNode)element).getAttributeValue(TEIConst.XML_ID_QNAME));			
			graph.addVertex(toponym);
			allToponyms.add(toponym);
		}
		// Pour créer les arcs, on commence par ceux qui auront une direction
		// (N, S, E, O)
		createOrientedEdges(teiAnnotadedFile, graph);

		// Ensuite on créé les arcs symétriques (relation "proche").
		createNonOrientedEdges(teiAnnotadedFile, graph);
		return graph;
	}
	
	private void createOrientedEdges(Document xmlDocument, SimpleDirectedGraph<Toponym, LabeledEdge<Toponym, SpatialRelationship>> graph)
			throws XPathExpressionException, JSONException, IOException {

		List<XdmItem> orientations = TEIUtil.getOrientationsNodeList(xmlDocument);
		for (int i = 0; i < orientations.size(); i++) {
			XdmNode orientation = (XdmNode) orientations.get(i);
			String orientationValue = TEIUtil.getWValues(orientation);
			if (!(StringUtils.containsIgnoreCase(orientationValue, TEIUtil.NORTH)
					|| StringUtils.containsIgnoreCase(orientationValue, TEIUtil.SOUTH)
					|| StringUtils.containsIgnoreCase(orientationValue, TEIUtil.WEST)
					|| StringUtils.containsIgnoreCase(orientationValue, TEIUtil.EAST))) {
				// L'orientation ne contient pas N, S, E, O -> on passe à la
				// suivante
				// On ne prend plus en compte les indications de type
				// "occidental", "oriental", etc.
				// car cela augmente trop les erreurs dans le graphe.
				continue;
			}
			// Orientation = Nord, Sud, Ouest ou Est
			// On vérifie si l'orientation est précédée de "de". Si c'est le
			// cas, on ne s'en sert pas, car elle n'indique pas en général la
			// position relative de deux toponymes.
			Boolean usefullOrientation = !TEIUtil.isOrientationPrecededByDe(orientation);
			if (!usefullOrientation) {
				continue;
			}
			if (TEIUtil.isAtTheBeginningOfTheSentence(orientation)) {
				// L'orientation est en début de phrase.
				XdmNode nextToponym = (XdmNode) TEIUtil.getNextToponym(orientation, true);
				XdmNode previousToponym = (XdmNode) TEIUtil.getPreviousToponym(orientation, false);
				if (nextToponym != null && previousToponym != null) {
					// on peut lier les deux toponymes
					// il faut vérifier s'ils font parti d'un groupe
					// de toponymes (isToponymPartOfBag)
					addEdgeToGraph(graph, previousToponym, nextToponym, orientation);
				}
			} else if (TEIUtil.isFollowedByDe(orientation)) {
				// L'orientation est en milieu ou fin de phrase.
				XdmNode nextToponym = (XdmNode) TEIUtil.getNextToponym(orientation, true);
				XdmNode previousToponym = (XdmNode) TEIUtil.getPreviousToponym(orientation, true);
				if (nextToponym != null && previousToponym != null) {
					// on peut lier les deux toponymes
					// il faut vérifier s'ils font parti d'un
					// groupe de toponymes (isToponymPartOfBag)
					// Ici, si on a par exemple "Save Nord de
					// Isle-Jourdain"
					// on aura "Isle-Jourdain |nord> Save"
					addEdgeToGraph(graph, previousToponym, nextToponym, orientation);
				}
			}
		}
	}
	
	private void addEdgeToGraph(SimpleDirectedGraph<Toponym, LabeledEdge<Toponym, SpatialRelationship>> graph, XdmItem previousToponymElement,
			XdmItem nextToponymElement, XdmItem orientation) throws XPathExpressionException {
		List<XdmNode> previousToponyms = TEIUtil.getBagBackward(previousToponymElement);
		List<XdmNode> nextToponyms = TEIUtil.getBagForward(nextToponymElement);
		for (XdmNode prev : previousToponyms) {
			TEIUtil.getWValues(prev);
			for (XdmNode next : nextToponyms) {
				TEIUtil.getWValues(next);
				Optional<Toponym> previousToponym = allToponyms.stream().filter(t -> t.getId() == prev.getAttributeValue(TEIConst.XML_ID_QNAME)).findFirst();
				Optional<Toponym> nextToponym = allToponyms.stream().filter(t -> t.getId() == next.getAttributeValue(TEIConst.XML_ID_QNAME)).findFirst();
				if (!(previousToponym.isPresent() && nextToponym.isPresent())) 
					continue;
				graph.addEdge(
						previousToponym.get(), 
						nextToponym.get(),
						new LabeledEdge<Toponym, SpatialRelationship>(previousToponym.get(), nextToponym.get(), TEIUtil.getSpatialRelationship(orientation)));
			}
		}
	}
	
	/**
	 * Gets the sentences with orientation in it.
	 *
	 * @return the sentences with orientation
	 */
	public List<String> getSentencesWithOrientation() {
		Processor processor = new Processor(false);
		XdmNode root = processor.newDocumentBuilder().wrap(teiAnnotadedFile);
		List<List<XdmNode>> phrases = TEIUtil.cutIntoSentences(root);
		List<List<XdmNode>> sentencesWithOrientation = new ArrayList<>();
		for (List<XdmNode> list : phrases) {
			if (list.stream().anyMatch(node -> 
			XMLUtil.isAttributeValueEqualTo(node, TEIConst.TYPE, TEIConst.ORIENTATION)
			|| XMLUtil.isAttributeValueEqualTo(node, TEIConst.SUBTYPE, TEIConst.ORIENTATION))
					&& list.stream().anyMatch(node -> XMLUtil.hasAttribute(node, TEIConst.XML_ID_QNAME)))
				sentencesWithOrientation.add(list);
		}
		List<String> result = new ArrayList<>();
		for (List<XdmNode> list : sentencesWithOrientation) {
			String sentence = "";
			for (XdmNode xdmNode : list) {
				sentence += TEIUtil.getWValues(xdmNode).trim() + " ";
			}
			result.add(sentence.trim());
		}		
		return result;
	}

	private void createNonOrientedEdges(Document xmlDocument, SimpleDirectedGraph<Toponym, LabeledEdge<Toponym, SpatialRelationship>> graph)
			throws XPathExpressionException {
		Processor processor = new Processor(false);
		XdmNode root = processor.newDocumentBuilder().wrap(xmlDocument);
		List<List<XdmNode>> phrases = TEIUtil.cutIntoSentences(root);
		// on lie les toponymes d'une même phrase les uns à la suite des autres,
		// et quand il y a un bag, tous ceux du bag entre eux.
		for (List<XdmNode> phrase : phrases) {
			for (int i = 0; i < phrase.size(); i++) {
				XdmNode word = phrase.get(i);
				if (!XMLUtil.hasAttribute(word, TEIConst.XML_ID_QNAME)) {
					continue;
				}
				// l'élément courant est un toponyme
				List<XdmNode> currentBag = TEIUtil.getBagForward(word);
				if (currentBag.size() > 1) {
					// on doit lier les éléments entre eux
					for (int j = 0; j < currentBag.size(); j++) {
						for (int j2 = j + 1; j2 < currentBag.size(); j2++) {
							XdmNode e1 = currentBag.get(j);
							XdmNode e2 = currentBag.get(j2);
							String v1 = TEIUtil.getWValues(e1);
							String v2 = TEIUtil.getWValues(e2);
							Optional<Toponym> t1 = allToponyms.stream().filter(t -> t.getId() == e1.getAttributeValue(TEIConst.XML_ID_QNAME)).findFirst();
							Optional<Toponym> t2 = allToponyms.stream().filter(t -> t.getId() == e2.getAttributeValue(TEIConst.XML_ID_QNAME)).findFirst();
							if (!(t1.isPresent() && t2.isPresent())) 
								continue;
							if (!v1.equalsIgnoreCase(v2) && !graph.containsEdge(t2.get(), t1.get())) {
								graph.addEdge(t1.get(), t2.get(), 
										new LabeledEdge<Toponym, SpatialRelationship>(t1.get(), t2.get(), SpatialRelationship.NEAR));
							}
						}
					}
					// puis on met le mot avant le dernier élément du bag en
					// élément courant (pas le dernier topo du bag car il
					// faut le lier au prochain topo de la phrase.
					i = phrase.indexOf(currentBag.get(currentBag.size() - 1)) - 1;
				} else {
					// cet élément ne fait pas parti d'un bag, ou est le
					// dernier d'un bag
					// on le lie au prochain toponyme qu'il faut d'abord
					// trouvé
					for (int j = i + 1; j < phrase.size(); j++) {
						XdmNode secondWord = phrase.get(j);
						if (XMLUtil.hasAttribute(secondWord, TEIConst.XML_ID)) {
							String v1 = TEIUtil.getWValues(word);
							String v2 = TEIUtil.getWValues(secondWord);
							Optional<Toponym> t1 = allToponyms.stream().filter(t -> t.getId() == word.getAttributeValue(TEIConst.XML_ID_QNAME)).findFirst();
							Optional<Toponym> t2 = allToponyms.stream().filter(t -> t.getId() == secondWord.getAttributeValue(TEIConst.XML_ID_QNAME)).findFirst();
							if (!(t1.isPresent() && t2.isPresent())) 
								continue;
							if (!v1.equalsIgnoreCase(v2) && !graph.containsEdge(t2.get(), t1.get())) {
								graph.addEdge(t1.get(), t2.get(), new LabeledEdge<Toponym, SpatialRelationship>(t1.get(), t2.get(), SpatialRelationship.NEAR));
							}
							i = j - 1;
							break;
						}
					}
					// ce toponyme devient l'élément suivant
				}
			}
		}
	}
}
