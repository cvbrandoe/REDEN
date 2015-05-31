package fr.lip6.nel.V3;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

/**
 * This class implements methods related to the RDF data manipulation.
 * 
 * @author @author Brando & Frontini - Labex OBVIL - Université Paris-Sorbonne - UPMC
 *         LIP6
 */
public class RDFProcessingNEL {

	/**
	 * Decodes URI.
	 * 
	 * @param s, the URI
	 * @return the new URI
	 */
	public static String decompose(String s) {
		try {
			if (s.startsWith("http:")) {
				return URLDecoder.decode(s, "UTF-8");
			} else {
				return s;
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return "";
	}

	/**
	 * Downloads RDF data by URI from the Web of Data.
	 * @param uri, concerned URI
	 * @param baseURL
	 * @param model, RDF model where to store the data
	 * @param dir, physical location of the folder where to store RDF files
	 * (to avoid downloading files every time)
	 * @return the raw RDF graph
	 */
	public static Model retrieveRDF(String uri, String baseURL, Model model,
			String dir) {

		try {
			System.out.println("uri: " + uri + " and file " + dir + "/file"
					+ uri.hashCode() + ".n3");
			File f = new File(dir + "/file" + uri.hashCode() + ".n3");
			// to go faster (remove f.exists if we want
			// to update local triples)
			if (!f.exists() || FileUtils.readFileToString(f).trim().isEmpty()) {
				model = ModelFactory.createDefaultModel();
				if (uri.contains("dbpedia")) {
					uri = uri + ".rdf";
					InputStream in = FileManager.get().open(uri);
					if (in != null) {
						// check rdf repos are available
						URL u = new URL(baseURL);
						HttpURLConnection huc = (HttpURLConnection) u
								.openConnection();
						huc.setRequestMethod("GET");
						huc.connect();
						int code = huc.getResponseCode();
						if (code != 503 && code != 404) {
							model.read(in, null, "RDF/XML");
						} else {
							System.out.println("RDF repo is not available "
									+ baseURL);
							return null;
						}

					} else {
						System.out.println("skip URI: " + uri);
					}
				} else {
					// check rdf repos are available
					URL u = new URL(baseURL);
					HttpURLConnection huc = (HttpURLConnection) u
							.openConnection();
					huc.setRequestMethod("GET");
					huc.connect();
					int code = huc.getResponseCode();
					if (code != 503 && code != 404) {
						model.read(uri);
					} else {
						System.out.println("RDF repo is not available "
								+ baseURL);
						return null;
					}
				}

				OutputStream fileOutputStream = new FileOutputStream(f);
				OutputStreamWriter out = new OutputStreamWriter(
						fileOutputStream, "UTF-8");
				model.write(out, "N3");

			} else {
				// System.out.println("file exists: " + dir + "/file"
				// + uri.hashCode() + ".n3");
			}
		} catch (IOException ignore) {
			System.out.println("problem storing RDF subgraph of URI: " + uri);
		}
		return model;
	}

	/**
	 * inject all sameAs references.
	 * @param mentionsWithURIs, mentions and their candidates
	 * @param baseURIS
	 * @param model, raw RDF model
	 * @param dir, where to find the model
	 * @return the update raw RDF model
	 */
	public static Model injectSameAsInformation(
			Map<String, List<List<String>>> mentionsWithURIs,
			String[] baseURIS, Model model, String dir) {

		Model modelout = ModelFactory.createDefaultModel();
		Property prop = model
				.getProperty("http://www.w3.org/2002/07/owl#sameAs");

		for (List<List<String>> uriLists : mentionsWithURIs.values()) {
			for (List<String> uriList : uriLists) {
				for (String uri : uriList) {
					for (String baseURL2 : baseURIS) {
						String baseURL = baseURL2.trim();
						if (uri.contains(baseURL)) {
							Resource individualSameAs = model.getResource(uri);
							SimpleSelector ss = new SimpleSelector(
									individualSameAs, prop, (RDFNode) null);
							ExtendedIterator<Statement> iter = model
									.listStatements(ss);
							while (iter.hasNext()) {
								Statement stmt = iter.next();
								RDFNode object = stmt.getObject();
								if (object.toString().startsWith(
										"http://dbpedia.org")) {
									modelout = retrieveRDF(
											decompose(object.toString()),
											baseURL, modelout, dir);
								}
							}
						}
					}
				}
			}
		}
		return modelout;
	}

	/**
	 * For every paragraph, it builds the RDF sub-subgraph corresponding to the
	 * mentions thanks to URIs.
	 * 
	 * @param dir
	 *            , the name of folder where to store data
	 * @param mentionsWithURIs
	 *            , URIs information
	 * @param mentionsPerParagraph
	 *            , mentions found of paragraph
	 * @param baseURIS
	 *            , the base names of the URIs
	 */
	public static Model aggregateRDFSubGraphsFromURIs(String dir,
			Map<String, List<List<String>>> mentionsWithURIs,
			List<String> mentionsofParagraph, String[] baseURIS) {
		Date start = new Date();
		File dirF = new File(dir);
		if (!dirF.exists())
			dirF.mkdir();
		// store RDF files into a local folder
		List<String> alreadyProcessedURI = new ArrayList<String>();
		for (List<List<String>> uriLists : mentionsWithURIs.values()) {
			for (List<String> uriList : uriLists) {
				for (String uri : uriList) {
					for (String baseURL2 : baseURIS) {
						String baseURL = baseURL2.trim();
						if (uri.contains(baseURL)) {
							if (!alreadyProcessedURI.contains(uri)) {
								Model model = ModelFactory.createDefaultModel();
								model = retrieveRDF(uri, baseURL, model, dir);
								alreadyProcessedURI.add(uri);
							}
						}
					}
				}
			}
		}

		// This is the raw RDF graph
		// Load subgraphs for mentions in paragraph
		alreadyProcessedURI = new ArrayList<String>(0);
		Model model = ModelFactory.createDefaultModel();
		for (int l = 0; l < mentionsofParagraph.size(); l++) {
			String mention = mentionsofParagraph.get(l);
			List<List<String>> uriLists = mentionsWithURIs.get(mention);
			if (uriLists != null) {
				for (List<String> uriList : uriLists) {
					for (String uri : uriList) {
						for (String baseURL2 : baseURIS) {
							String baseURL = baseURL2.trim();
							if (uri.contains(baseURL)) { // avoiding reading
															// idref uri
								if (!alreadyProcessedURI.contains(uri)) {
									if (new File(dir + "/file" + uri.hashCode()+ ".n3").exists()) {
										model.read(dir + "/file"
												+ uri.hashCode() + ".n3");
										alreadyProcessedURI.add(uri);
									}
								}

							}
						}
					}
				}
			}
		}
		// add sameAs information into the model
		Model modelout = injectSameAsInformation(mentionsWithURIs, baseURIS,
				model, dir);
		model.add(modelout);
		Date end = new Date();
		System.out.println("Finished createRDFSubGraphsFromURIs in "
				+ (end.getTime() - start.getTime()) / 60 + "secs");
		return model;

	}
	
	// obtain potentially identical individuals thanks to the sameAs relation,
		// including itself
		public static List<String> obtainPotentiallyIdenticalIndividuals(
				String uri, Model model) {
			List<String> out = new ArrayList<String>();
			// out.add(uri);
			Property prop = model
					.getProperty("http://www.w3.org/2002/07/owl#sameAs");
			Resource person = model.getResource(uri);
			SimpleSelector ss = new SimpleSelector(person, prop, (RDFNode) null);
			ExtendedIterator<Statement> iter = model.listStatements(ss);
			while (iter.hasNext()) {
				Statement stmt = iter.next();
				if (out.size() == 0) {
					out.add(stmt.getObject().toString());
				}
				if (!out.contains(stmt.getObject().toString())) {
					out.add(stmt.getObject().toString());
				}
			}
			List<String> out2 = new ArrayList<String>();
			for (String uriA : out) {
				if (uriA.startsWith("http://dbpedia.org")) {
					Resource personA = model.getResource(uriA);
					SimpleSelector ssA = new SimpleSelector(personA, prop,
							(RDFNode) null);
					ExtendedIterator<Statement> iterA = model.listStatements(ssA);
					while (iterA.hasNext()) {
						Statement stmt = iterA.next();
						if (out2.size() == 0) {
							out2.add(stmt.getObject().toString());
						}
						if (!out2.contains(stmt.getObject().toString())) {
							out2.add(stmt.getObject().toString());
						}
					}
				}
			}
			out.addAll(out2);
			out.add(uri);
			return out;

		}

}
