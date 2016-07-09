package fr.lip6.reden.extra;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Class to semi-automatically annotate a TEI file using the SameAs.org API.
 * 
 * @author Brando
 *
 */
public class SemiAutomaticTEIAnnotation {

	public static void main(String[] args) {
		String xpathExpresion = "//body//head|//body//item|//body//l|//body//p";
		String annotationTag = "persName";
		//annotate(args[0], xpathExpresion, annotationTag, "datos.bne.es");
		//annotate(args[0], xpathExpresion, annotationTag, "data.bnf.fr");
		annotate(args[0], xpathExpresion, annotationTag, "yago-knowledge.org");
	}

	public static void annotate(String goldfile, String xpathExpresion, String annotationTag, String base) {

		try {
			
			DocumentBuilder b = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			org.w3c.dom.Document doc = b.parse(new FileInputStream(goldfile));
			
			XPath xPath = XPathFactory.newInstance().newXPath();
			NodeList nodes = (NodeList) xPath.evaluate(xpathExpresion, doc.getDocumentElement(),
					XPathConstants.NODESET);

			for (int i = 0; i < nodes.getLength(); ++i) {
				Element e = (Element) nodes.item(i);

				for (String annoTag : annotationTag.split(",")) {
					NodeList nodesChild = (NodeList) xPath.evaluate(".//" + annoTag, e, XPathConstants.NODESET);

					for (int k = 0; k < nodesChild.getLength(); ++k) {
						Element child = (Element) nodesChild.item(k);
						System.out.println(child.getTextContent());
						String ref = child.getAttribute("ref");
						String urinew = equivalentURIS(base, ref).replaceAll(",", "").replaceAll("\"", "");
						if (!urinew.equals(""))
							child.setAttribute("ref_new", urinew);
					}
				}
			}
			
			// initialize StreamResult with File object to save to file
			Transformer transformer;
			transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			StreamResult result = new StreamResult(new StringWriter());
			DOMSource source = new DOMSource(doc);
			transformer.transform(source, result);
						
			String xmlString = result.getWriter().toString();
			PrintWriter writer = new PrintWriter(goldfile.replace(".xml", "-yago.xml"), "UTF-8");
			writer.println(xmlString);
			writer.close();
			
		} catch (ParserConfigurationException e1) {
			e1.printStackTrace();
		} catch (XPathExpressionException e1) {
			e1.printStackTrace();
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (SAXException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (TransformerConfigurationException e1) {
			e1.printStackTrace();
		} catch (TransformerFactoryConfigurationError e1) {
			e1.printStackTrace();
		} catch (TransformerException e1) {
			e1.printStackTrace();
		}

	}

	public static String equivalentURIS(String base, String uri) {
		try {

			String url = "http://sameas.org/json";
			String query = String.format("uri=%s", URLEncoder.encode(uri, "UTF-8"));
			java.net.URLConnection connection = new URL(url + "?" + query).openConnection();
			connection.setRequestProperty("Accept-Charset", "UTF-8");
			if (connection instanceof HttpURLConnection) {
				HttpURLConnection httpConnection = (HttpURLConnection) connection;
				BufferedReader br = new BufferedReader(new InputStreamReader((httpConnection.getInputStream())));
				String output;
				while ((output = br.readLine()) != null) {
					if (output.contains(base)) {
						//System.out.println(output.trim());
						return output.trim();
					}
				}
				return "";
			} else {
				System.err.println("error!");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

}
