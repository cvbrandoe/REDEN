package fr.ign.georeden.nelinker.tei;

import java.io.IOException;

import javax.xml.xpath.XPathExpressionException;

import org.json.JSONException;

/**
 * The Interface ITEIHandler.
 *
 * @param <G>
 *            the type of the Graph
 */
@FunctionalInterface
public interface ITEIHandler<G> {

	/**
	 * Creates the graph from tei.
	 *
	 * @param xmlDocument
	 *            the xml document
	 * @return the simple directed graph
	 * @throws XPathExpressionException
	 */
	public G createGraphFromTEI() throws XPathExpressionException, JSONException, IOException;

}
