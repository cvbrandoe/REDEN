package fr.ign.georeden.nelinker.tei;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.w3c.dom.Document;

import fr.ign.georeden.kb.SpatialRelationship;
import fr.ign.georeden.utils.JSONUtil;
import fr.ign.georeden.utils.XMLUtil;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmSequenceIterator;

/**
 * The Class TEIUtil contains methods to manage the TEI file, that are
 * independent of any algorithm.
 */
public class TEIUtil {

	private static Logger logger = Logger.getLogger(TEIUtil.class);

	public static final String CONFIG_PATH = "./config/geoconfig.json";

	/** The Constant NORTH. */
	public static final String NORTH = "Nord";

	/** The Constant SOUTH. */
	public static final String SOUTH = "Sud";

	/** The Constant EAST. */
	public static final String EAST = "Est";

	/** The Constant WEST. */
	public static final String WEST = "Ouest";

	/** The Constant NEAR. */
	public static final String NEAR = "proche";

	/**
	 * Instantiates a new TEI util.
	 */
	private TEIUtil() {

	}

	 /**
	 * Gets the natural place list.
	 *
	 * @param xmlDocument the xml document
	 * @return the natural place list
	 * @throws XPathExpressionException the x path expression exception
	 * @throws IOException
	 * @throws JSONException
	 */
	public static List<XdmItem> getNaturalPlaceList(Document document) throws JSONException, IOException {
		return XMLUtil.getListFromXPath(document, JSONUtil.getStringFromFile("xpath_for_natural_places", CONFIG_PATH),
				"", "");//TEIConst.TEI_NS)
	}

	/**
	 * Gets the w values.
	 *
	 * @param item
	 *            the item
	 * @return the w values
	 */
	public static String getWValues(XdmItem item) {
		String xPathStatement = "string-join(.//w/text(), ' ')";
		String result = XMLUtil.getStringFromXPath(item, xPathStatement, "", "");//TEIConst.TEI_NS)
		return result;
	}

	/**
	 * Gets the place names.
	 *
	 * @param document
	 *            the document
	 * @return the place names
	 * @throws XPathExpressionException
	 *             the x path expression exception
	 */
	public static List<XdmItem> getToponymsNodeList(Document document) {
		return XMLUtil.getListFromXPath(document, "//*[@xml:id]",
				"", TEIConst.TEI_NS);
	}

	/**
	 * Gets all the orientation tags.
	 *
	 * @param xmlDocument
	 *            the xml document
	 * @return the all orientation tags
	 * @throws Exception
	 * @throws XPathExpressionException
	 *             the x path expression exception
	 * @throws IOException
	 * @throws JSONException
	 */
	public static List<XdmItem> getOrientationsNodeList(Document xmlDocument)
			throws JSONException, XPathExpressionException {
		String expression = "//*[@type='orientation' or @subtype='orientation']";
		try {
			expression = JSONUtil.getStringFromFile("xpath_for_orientations", CONFIG_PATH);
		} catch (IOException e) {
			logger.error(e);
		}
		return XMLUtil.getListFromXPath(xmlDocument, expression, "", TEIConst.TEI_NS);
	}

	/**
	 * Checks if the orientation is preceded by de.
	 *
	 * @param orientation
	 *            the orientation
	 * @return the boolean
	 */
	public static Boolean isOrientationPrecededByDe(XdmItem orientation) {
		Boolean isOrientationPrecededByDe = false;
		XdmNode previousElement = (XdmNode)XMLUtil.getPreviousSibling(orientation);
		if (previousElement != null && "de".equalsIgnoreCase(previousElement.getAttributeValue(new QName(TEIConst.LEMMA)))) {
			isOrientationPrecededByDe = true;
		}
		if (previousElement != null) {
			previousElement = (XdmNode)XMLUtil.getPreviousSibling(previousElement);
			if (!isOrientationPrecededByDe && previousElement != null && 
					"de".equalsIgnoreCase(previousElement.getAttributeValue(new QName(TEIConst.LEMMA)))) {
				isOrientationPrecededByDe = true;
			}
		}
		return isOrientationPrecededByDe;
	}

	/**
	 * Gets the next toponym.
	 *
	 * @param element
	 *            the element
	 * @param isSearchInSentenceOnly
	 *            the is search in sentence only
	 * @return the next toponym
	 * @throws XPathExpressionException
	 *             the x path expression exception
	 */
	public static XdmItem getNextToponym(XdmItem element, Boolean isSearchInSentenceOnly)
			throws XPathExpressionException {
		XdmNode currentElement = (XdmNode) XMLUtil.getNextSibling(element);
		while (currentElement != null) {
			if (currentElement.getAttributeValue(TEIConst.XML_ID_QNAME) != null)
				return currentElement;
			if (isSearchInSentenceOnly 
					&& (XMLUtil.isAttributeValueEqualTo(currentElement, TEIConst.FORCE, TEIConst.STRONG) 
							|| XMLUtil.isAttributeValueEqualTo(currentElement, TEIConst.FORCE, TEIConst.INTER)))
				return null;
			if (XMLUtil.isAttributeValueEqualTo(currentElement, TEIConst.TYPE, TEIConst.ORIENTATION))
				return null;
			if (XMLUtil.isAttributeValueEqualTo(currentElement, TEIConst.SUBTYPE, TEIConst.ORIENTATION))
				return null;
			currentElement = (XdmNode)XMLUtil.getNextSibling(currentElement);
		}
		return null;
	}

	/**
	 * Checks if the orientation is at the beginning of the sentence.
	 *
	 * @param orientation
	 *            the orientation
	 * @return the boolean
	 * @throws XPathExpressionException
	 *             the x path expression exception
	 */
	public static Boolean isAtTheBeginningOfTheSentence(XdmItem orientation) throws XPathExpressionException {
		XdmNode previousElement = (XdmNode) XMLUtil.getPreviousSibling(orientation);
		int count = 1;
		while (previousElement != null && count <= 3) {
			if (".".equalsIgnoreCase(previousElement.getStringValue())) {
				// On a trouvé un ".", on arrête la boucle
				break;
			}
			count++;
			previousElement =  (XdmNode) XMLUtil.getPreviousSibling(previousElement);
		}
		if (count <= 3)
			return true;
		return false;
	}

	/**
	 * Gets the previous toponym.
	 *
	 * @param element
	 *            the element
	 * @param isSearchInSentenceOnly
	 *            the is search in sentence only
	 * @return the previous toponym
	 * @throws XPathExpressionException
	 *             the x path expression exception
	 */
	public static XdmItem getPreviousToponym(XdmItem element, Boolean isSearchInSentenceOnly)
			throws XPathExpressionException {
		XdmNode currentElement = (XdmNode) XMLUtil.getPreviousSibling(element);
		while (currentElement != null) {
			if (XMLUtil.hasAttribute(currentElement, TEIConst.XML_ID_QNAME))
				return currentElement;
			if (isSearchInSentenceOnly
					&& (XMLUtil.isAttributeValueEqualTo(currentElement, TEIConst.FORCE, TEIConst.STRONG)
							|| XMLUtil.isAttributeValueEqualTo(currentElement, TEIConst.FORCE, TEIConst.INTER)))
				return null;
			if (XMLUtil.isAttributeValueEqualTo(currentElement, TEIConst.TYPE, TEIConst.ORIENTATION))
				return null;
			if (XMLUtil.isAttributeValueEqualTo(currentElement, TEIConst.SUBTYPE, TEIConst.ORIENTATION))
				return null;
			currentElement = (XdmNode) XMLUtil.getPreviousSibling(currentElement);
		}
		return null;
	}

	/**
	 * Checks if the orientation is followed by de.
	 *
	 * @param orientation
	 *            the orientation
	 * @return the boolean
	 * @throws XPathExpressionException
	 *             the x path expression exception
	 */
	public static Boolean isFollowedByDe(XdmItem orientation) throws XPathExpressionException {
		if (TEIUtil.getWValues(orientation).endsWith(" de"))
			return true;
		XdmNode nextElement = (XdmNode)XMLUtil.getNextSibling(orientation);
		if (nextElement != null && "de".equalsIgnoreCase(nextElement.getAttributeValue(new QName(TEIConst.LEMMA))))
			return true;
		return false;
	}

	/**
	 * Gets the bag backward.
	 *
	 * @param toponym
	 *            the toponym
	 * @return the bag backward
	 */
	public static List<XdmNode> getBagBackward(XdmItem toponym) {
		List<XdmNode> result = new ArrayList<>();
		result.add((XdmNode)toponym);
		XdmNode currentElement = (XdmNode) XMLUtil.getPreviousSibling(toponym);
		if (currentElement != null) {
			String currentElementValue = currentElement.getStringValue();
			if ("et".equalsIgnoreCase(currentElementValue) || ",".equalsIgnoreCase(currentElementValue)) {
				currentElement = (XdmNode) XMLUtil.getPreviousSibling(currentElement);
				if (currentElement != null && XMLUtil.hasAttribute(currentElement, TEIConst.XML_ID_QNAME)) {
					result.addAll(getBagBackward(currentElement));
				}
			} else if (XMLUtil.isAttributeValueEqualTo(currentElement, TEIConst.LEMMA, "de")) {
				currentElement = (XdmNode) XMLUtil.getPreviousSibling(currentElement);
				if (currentElement != null && XMLUtil.hasAttribute(currentElement, TEIConst.XML_ID_QNAME)) {
					result.addAll(getBagBackward(currentElement));
				}
			}
		}
		return result;
	}

	/**
	 * Gets the bag forward.
	 *
	 * @param toponym
	 *            the toponym
	 * @return the bag forward
	 */
	public static List<XdmNode> getBagForward(XdmItem toponym) {
		List<XdmNode> result = new ArrayList<>();
		result.add((XdmNode)toponym);
		XdmNode currentElement = (XdmNode) XMLUtil.getNextSibling(toponym);
		if (currentElement == null) {
			return result;
		}
		String currentElementValue = currentElement.getStringValue();
		if ("et".equalsIgnoreCase(currentElementValue) || ",".equalsIgnoreCase(currentElementValue)) {
			currentElement = (XdmNode) XMLUtil.getNextSibling(currentElement);
			if (currentElement != null && XMLUtil.hasAttribute(currentElement, TEIConst.XML_ID_QNAME)) {
				result.addAll(getBagForward(currentElement));
			} else if (currentElement != null && 
					XMLUtil.isAttributeValueEqualTo(currentElement, TEIConst.LEMMA, "de")) {
				currentElement = (XdmNode) XMLUtil.getNextSibling(currentElement);
				if (currentElement != null && XMLUtil.hasAttribute(currentElement, TEIConst.XML_ID_QNAME)) {
					result.addAll(getBagForward(currentElement));
				}
			}
		}
		return result;
	}

	/**
	 * Cut the element into sentences.
	 *
	 * @param root
	 *            the root
	 * @return the list
	 */
	@SuppressWarnings("unchecked")
	public static List<List<XdmNode>> cutIntoSentences(XdmItem root) {
		List<List<XdmNode>> phrases = new ArrayList<>();
		XdmSequenceIterator iterator = ((XdmNode)root).axisIterator(Axis.CHILD);
		XdmNode child = null;
		while (iterator.hasNext()) {
			child = (XdmNode)iterator.next();
		}
		if (child == null)
			return phrases;
		iterator = child.axisIterator(Axis.CHILD);
		if (iterator.hasNext()) {
			child = (XdmNode)iterator.next();
		}

		List<XdmNode> currentPhrase = new ArrayList<>();
		while (child != null) {
			XdmNode element = (XdmNode) child;
			
			if (TEIConst.PC.equalsIgnoreCase(element.getNodeName().getLocalName()) && ".".equalsIgnoreCase(element.getStringValue())) {
				// On est à la fin d'une phrase
				// on verrifie que la phrase nous intéresse
				// et si c'est le cas on l'ajoute à notre liste de phrase
				if (currentPhrase.stream().anyMatch(e -> XMLUtil.hasAttribute(e, TEIConst.XML_ID_QNAME))) {
					phrases.add((ArrayList<XdmNode>) ((ArrayList<XdmNode>) currentPhrase).clone());
				}
				// on réinitialise la phrase courante
				currentPhrase.clear();
			} else {
				currentPhrase.add(element);
			}
			child = (XdmNode)XMLUtil.getNextSibling(child);
		}
		return phrases;
	}

	/**
	 * Gets the orientation.
	 *
	 * @param orientation
	 *            the orientation
	 * @return the orientation
	 * @throws XPathExpressionException
	 *             the x path expression exception
	 */
	public static String getOrientation(XdmItem orientation) throws XPathExpressionException {
		String orientationString = "";
		String wValue = TEIUtil.getWValues(orientation);

		if (StringUtils.containsIgnoreCase(wValue, NORTH) || StringUtils.containsIgnoreCase(wValue, "septentrional")) {
			orientationString += NORTH;
		} else if (StringUtils.containsIgnoreCase(wValue, SOUTH)
				|| StringUtils.containsIgnoreCase(wValue, "méridional")) {
			orientationString += SOUTH;
		}
		if (StringUtils.containsIgnoreCase(wValue, WEST) || StringUtils.containsIgnoreCase(wValue, "occident")) {
			orientationString += WEST;
		} else if (StringUtils.containsIgnoreCase(wValue, EAST) || StringUtils.containsIgnoreCase(wValue, "oriental")) {
			orientationString += EAST;
		}
		if (orientationString.isEmpty())
			orientationString = wValue;
		if (isOrientationReversed(orientation)) // sert à identifier quand on
												// inverse le sens de
												// l'orientation
			orientationString = reverseOrientation(orientationString);
		return orientationString;
	}

	public static SpatialRelationship getSpatialRelationship(XdmItem orientation) throws XPathExpressionException {
		String value = getOrientation(orientation);
		if (value.contains(NORTH)) {
			if (value.contains(WEST))
				return SpatialRelationship.SOUTH_EAST_OF;
			else if (value.contains(EAST))
				return SpatialRelationship.SOUTH_WEST_OF;
			return SpatialRelationship.SOUTH_OF;
		} else if (value.contains(SOUTH)) {
			if (value.contains(WEST))
				return SpatialRelationship.NORTH_EAST_OF;
			else if (value.contains(EAST))
				return SpatialRelationship.NORTH_WEST_OF;
			return SpatialRelationship.NORTH_OF;
		} else if (value.contains(WEST))
			return SpatialRelationship.EAST_OF;
		else if (value.contains(EAST))
			return SpatialRelationship.WEST_OF;
		logger.info(value + " == " + SpatialRelationship.NEAR);
		return SpatialRelationship.NEAR;
	}

	/**
	 * Reverse orientation.
	 *
	 * @param orientation
	 *            the orientation
	 * @return the string
	 */
	public static String reverseOrientation(String orientation) {
		String newOrientation = orientation;
		if (orientation.contains(NORTH))
			newOrientation = orientation.replace(NORTH, SOUTH);
		else if (orientation.contains(SOUTH))
			newOrientation = orientation.replace(SOUTH, NORTH);
		if (orientation.contains(WEST))
			newOrientation = orientation.replace(WEST, EAST);
		else if (orientation.contains(EAST))
			newOrientation = orientation.replace(EAST, WEST);
		return newOrientation;
	}

	/**
	 * Checks if the orientation is reversed.
	 *
	 * @param orientation
	 *            the orientation
	 * @return the boolean
	 * @throws XPathExpressionException
	 *             the x path expression exception
	 */
	public static Boolean isOrientationReversed(XdmItem orientation) throws XPathExpressionException {
		String wValue = TEIUtil.getWValues(orientation);
		XdmNode next = (XdmNode)XMLUtil.getNextSibling(orientation);
		if ("de".equalsIgnoreCase(next.getAttributeValue(new QName(TEIConst.LEMMA)))
				|| StringUtils.containsIgnoreCase(wValue, " d"))
			return true;
		return false;
	}
}
