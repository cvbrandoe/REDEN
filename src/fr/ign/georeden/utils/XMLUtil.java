package fr.ign.georeden.utils;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.collections.IteratorUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import net.sf.saxon.TransformerFactoryImpl;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.SaxonApiUncheckedException;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmSequenceIterator;

/**
 * Contient des fonctions utilitaires pour gérer les fichiers XML.
 * 
 * @author PHParis
 *
 */
public final class XMLUtil {
	
	private static Logger logger = Logger.getLogger(XMLUtil.class);
	
	private XMLUtil() {

	}

	/**
	 * Retourn un document XML à partir de l'emplacement d'un fichier.
	 * 
	 * @param fullFilePath
	 * @return
	 * @throws Exception 
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public static Document createDocumentFromFile(String fullFilePath) {
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		Document document = null;
		try {
			final DocumentBuilder builder = factory.newDocumentBuilder();
			File file = new File(fullFilePath);
			if (!file.exists())
				throw new NullPointerException("file is null");
			document = builder.parse(file);
		} catch (SAXException | IOException | ParserConfigurationException e) {
			logger.error(e);
		}
		return document;
	}

	/**
	 * Affiche le fichier dans la console si inConsole vaut true. Enregistre le
	 * document à l'emplacement indiqué si fullFileName différent de null.
	 * 
	 * @param document
	 * @param fullFileName
	 * @param displayInConsole
	 * @throws TransformerException
	 */
	public static void displayXml(Document document, String fullFileName, Boolean displayInConsole)
			throws TransformerException {
		final TransformerFactory transformerFactory = TransformerFactory.newInstance();
		final Transformer transformer = transformerFactory.newTransformer();
		final DOMSource source = new DOMSource(document);

		// prologue
		transformer.setOutputProperty(OutputKeys.VERSION, "1.0");
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
//		transformer.setOutputProperty(OutputKeys.STANDALONE, "no");

		// formatage
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

		// output
		if (displayInConsole)
			transformer.transform(source, new StreamResult(System.out));
		if (fullFileName != null)
			transformer.transform(source, new StreamResult(fullFileName));
	}
	
	/**
	 * Return the content of a XML document
	 * @param doc
	 * @return
	 */
	public static String xmlDocumentContentToString(Document doc) {
		DOMSource domSource = new DOMSource(doc);
		StringWriter writer = new StringWriter();
		StreamResult result = new StreamResult(writer);
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer;
		try {
			transformer = tf.newTransformer();
			transformer.transform(domSource, result);
		} catch (TransformerException e) {
			logger.error(e);
		}
		return writer.toString();
	}
	
	/**
	 * Gets the xpath selector.
	 *
	 * @param document the document or XdmNode
	 * @param xpath the xpath
	 * @param nsPrefix the ns prefix
	 * @param nsUri the ns uri
	 * @return the x path selector
	 */
	private static XPathSelector getXPathSelector(Object document, String xpath, String nsPrefix, String nsUri) {
		Processor processor = new Processor(false);
		XPathCompiler xPathCompiler = processor.newXPathCompiler();
		if (nsPrefix != "" || nsUri != "")
			xPathCompiler.declareNamespace(nsPrefix, nsUri);
		XdmNode docw;
		if (document.getClass() == XdmNode.class) {
			docw = (XdmNode)document;
		} else {
			docw = processor.newDocumentBuilder().wrap(document);
		}
		XPathSelector selector = null;
		try {
			selector = xPathCompiler.compile(xpath).load();
			selector.setContextItem(docw);
		} catch (SaxonApiException e) {
			logger.error(e);
		}
		return selector;
	}
	
	/**
	 * Gets a list from the evalutation of a xpath.
	 *
	 * @param document the document
	 * @param xpath the xpath
	 * @param nsPrefix the ns prefix
	 * @param nsUri the ns uri
	 * @return the list from x path
	 */
	@SuppressWarnings("unchecked")
	public static List<XdmItem> getListFromXPath(Object document, String xpath, String nsPrefix, String nsUri) {
		List<XdmItem> myList = new ArrayList<>();
		try {
			myList.addAll(IteratorUtils.toList(getXPathSelector(document, xpath, nsPrefix, nsUri).evaluate().iterator()));
		} catch (SaxonApiUncheckedException | SaxonApiException e) {
			logger.error(e);
		}
		return myList;
	}
	
	public static Document applyXSLTTransformation(Document document, String xsltPath, String outputFilePath) throws TransformerException {
		final TransformerFactory transformerFactory = new TransformerFactoryImpl();
		final Transformer transformer = transformerFactory.newTransformer(new StreamSource(new File(xsltPath)));
		final DOMSource source = new DOMSource(document);
		Document result;

		// prologue
		transformer.setOutputProperty(OutputKeys.VERSION, "1.0");
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

		// formatage
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
		transformer.transform(source, new StreamResult(outputFilePath));

		result = createDocumentFromFile(outputFilePath);
		return result;
	}
	
	/**
	 * Gets a string from the evalutation of a xpath.
	 *
	 * @param document the document
	 * @param xpath the xpath
	 * @param nsPrefix the ns prefix
	 * @param nsUri the ns uri
	 * @return the string from x path
	 */
	public static String getStringFromXPath(Object document, String xpath, String nsPrefix, String nsUri) {
		String result = "";		
		try {
			XdmItem item = getXPathSelector(document, xpath, nsPrefix, nsUri).evaluateSingle();
			if (item != null)
				result = item.getStringValue();
		} catch (SaxonApiException e) {
			logger.error(e);
		}
		return result;
	}
	
	
	
	
	
	

	/**
	 * Retourne la liste des noeuds correspondant à l'évaluation du Xpath sur le
	 * document ou l'élément XML.
	 * 
	 * @param xmlDocumentOrElement
	 * @param xpathExpression
	 * @return
	 * @throws XPathExpressionException
	 */
	public static org.w3c.dom.NodeList getNodeList(Object xmlDocumentOrElement, String xpathExpression)
			throws XPathExpressionException {
		XPath xPath = XPathFactory.newInstance().newXPath();
		return (org.w3c.dom.NodeList) xPath.compile(xpathExpression).evaluate(xmlDocumentOrElement, XPathConstants.NODESET);
	}

	/**
	 * Retourne une chaîne de caractères correspondant à l'évaluation du Xpath
	 * sur le document ou l'élément XML.
	 * 
	 * @param xmlDocumentOrElement
	 * @param xpathExpression
	 * @return
	 * @throws XPathExpressionException
	 */
	public static String getString(Object xmlDocumentOrElement, String xpathExpression)
			throws XPathExpressionException {
		XPath xPath = XPathFactory.newInstance().newXPath();
		return xPath.compile(xpathExpression).evaluate(xmlDocumentOrElement);
	}

	/**
	 * Generate xml document from node list.
	 *
	 * @param nodes
	 *            the nodes
	 * @param rootName
	 *            the root name
	 * @param namespaceURI
	 *            the namespace uri
	 * @return the document
	 * @throws ParserConfigurationException
	 *             the parser configuration exception
	 */
	public static Document generateXmlDocumentFromNodeList(org.w3c.dom.NodeList nodes, String rootName, String namespaceURI)
			throws ParserConfigurationException {
		Document newXmlDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		org.w3c.dom.Element root = newXmlDocument.createElementNS(namespaceURI, rootName);
		newXmlDocument.appendChild(root);
		int length = nodes.getLength();
		for (int i = 0; i < length; i++) {			
			org.w3c.dom.Node node =  nodes.item(i);
			org.w3c.dom.Node copyNode = newXmlDocument.importNode(node, true);
			root.appendChild(copyNode);
		}
		return newXmlDocument;
	}
	
	public static XdmItem getPreviousSibling(XdmItem item) {
		XdmSequenceIterator iterator = ((XdmNode)item).axisIterator(Axis.PRECEDING_SIBLING);
		if (iterator.hasNext())
			return iterator.next();
		return null;
	}
	public static XdmItem getNextSibling(XdmItem item) {
		XdmSequenceIterator iterator = ((XdmNode)item).axisIterator(Axis.FOLLOWING_SIBLING);
		if (iterator.hasNext())
			return iterator.next();
		return null;
	}
	
	/**
	 * Checks if the value of attributeName value is equal to attributeValue.
	 *
	 * @param item the item
	 * @param attributeName the attribute name
	 * @param attributeValue the attribute value
	 * @return the boolean
	 */
	public static Boolean isAttributeValueEqualTo(XdmItem item, String attributeName, String attributeValue) {
		return attributeValue.equalsIgnoreCase(((XdmNode)item).getAttributeValue(new QName(attributeName)));
	}
	
	public static Boolean hasAttribute(XdmItem item, String attributeName) {
		return ((XdmNode)item).getAttributeValue(new QName(attributeName)) != null;
	}
	public static Boolean hasAttribute(XdmItem item, QName attributeName) {
		return ((XdmNode)item).getAttributeValue(attributeName) != null;
	}
}
