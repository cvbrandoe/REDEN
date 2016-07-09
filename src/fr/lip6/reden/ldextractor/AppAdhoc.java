package fr.lip6.reden.ldextractor;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.ResultSet;

import fr.lip6.reden.ldextractor.loc.QueryPlaceDBpedia;
import fr.lip6.reden.ldextractor.per.QueryArtPersonalityGetty;
import fr.lip6.reden.ldextractor.per.QueryAuthorBNE;
import fr.lip6.reden.ldextractor.per.QueryAuthorBNEAll;
import fr.lip6.reden.ldextractor.per.QueryAuthorBNF;
import fr.lip6.reden.ldextractor.per.QueryAuthorBNFAll;
import fr.lip6.reden.ldextractor.per.QueryPersonDBpediafr;

/**
 * Main class to launch the Linked Data crawler for handling domain-adaptation during named-entity linking.
 * @author Brando & Frontini
 */
public class AppAdhoc 
{
	private static Logger logger = Logger.getLogger(AppAdhoc.class);
	
	/**
	 * Main method.
	 * @param args
	 */
	public static void main(String [] args) {
		crawlsLinkedData("config/config.properties", "all");		
	}
	
	/**
	 * Crawls Linked Data.
	 * @param propertiesFile, the configuration file
	 */
	public static void crawlsLinkedData(String propertiesFile, String dicLabel) {
		
		// reading parameters
		try {
			logger.info("entering: crawlsLinkedData");
			Properties prop = new Properties();
			InputStream input = new FileInputStream(propertiesFile);
			prop.load(input);
			//loading main config file
			String outDictionnaireDir = prop.getProperty("outDictionnaireDir");
			String greaterThan = prop.getProperty("greaterThan");
			String lesserThan = prop.getProperty("lesserThan");
			String spatialExtent = prop.getProperty("spatialExtent");
			
			//prepare domain-adaptation parameters from config file
			List<TopicExtent> domainParams = loadDomainParams(greaterThan, lesserThan, spatialExtent);
			
			//Adhoc way so far
			String[] let =  {"a","b","c","d","e","f","g","h","i","j","k","l","m","n",
					"o","p","q","r","s","t","u","v","w","x", "y","z", "other"};
			//String[] let =  {"bre"}; //testing
			int counter = 0;
			Boolean out = false;
			
			if (dicLabel.equalsIgnoreCase("bnf") || dicLabel.equalsIgnoreCase("all")) {
				// QUERY AUTHORS IN BNF (filter by date)
				while (counter < let.length && !out) {
					QueryAuthorBNF bnf = new QueryAuthorBNF();
					Boolean lr = bnf.LARGE_REPO;
					String letter = null;
					if (!lr) {
						out = true; //enters once
					} else {
						letter = let[counter];
						logger.info("processing letter:" +letter);
					}
					
					//bnf				
					Query qbnf = bnf.formulateSPARQLQuery(domainParams, letter, "");
					ResultSet rsbnf = bnf.executeQuery(qbnf, bnf.TIMEOUT.toString(), bnf.SPARQL_END_POINT, "", "");
					bnf.processResults(rsbnf, outDictionnaireDir, letter, domainParams);
					counter++;
				}
			}
			if (dicLabel.equalsIgnoreCase("bnf-all") || dicLabel.equalsIgnoreCase("all")) {
				// QUERY AUTHORS IN BNF
				while (counter < let.length && !out) {
					QueryAuthorBNFAll bnf = new QueryAuthorBNFAll();
					Boolean lr = bnf.LARGE_REPO;
					String letter = null;
					if (!lr) {
						out = true; //enters once
					} else {
						letter = let[counter];
						logger.info("processing letter:" +letter);
					}
					
					//bnf				
					Query qbnf = bnf.formulateSPARQLQuery(domainParams, letter, "");
					ResultSet rsbnf = bnf.executeQuery(qbnf, bnf.TIMEOUT.toString(), bnf.SPARQL_END_POINT, "", "");
					bnf.processResults(rsbnf, outDictionnaireDir, letter, domainParams);
					counter++;
				}
			}
			if (dicLabel.equalsIgnoreCase("dbpediafr") || dicLabel.equalsIgnoreCase("all")) {
				// QUERY PLACES IN DBPEDIA
				counter = 0;
				out = false;
				while (counter < let.length && !out) {
					QueryPlaceDBpedia dbp = new QueryPlaceDBpedia();
					Boolean lr = dbp.LARGE_REPO;
					
					String letter = null;
					if (!lr) {
						out = true; //enters once
					} else {
						letter = let[counter];
						logger.info("processing letter:" +letter);
					}
					
					//dbpedia 
					Query qdb = dbp.formulateSPARQLQuery(domainParams, letter, "");
					ResultSet rsdbp = dbp.executeQuery(qdb, dbp.TIMEOUT.toString(), dbp.SPARQL_END_POINT, "", "");
					dbp.processResults(rsdbp, outDictionnaireDir, letter, domainParams);
					
					counter++;
				}
			}
			if (dicLabel.equalsIgnoreCase("dbpediafr-author") || dicLabel.equalsIgnoreCase("all")) {
				// QUERY PLACES IN DBPEDIA
				counter = 0;
				out = false;
				while (counter < let.length && !out) {
					QueryPersonDBpediafr dbp = new QueryPersonDBpediafr();
					Boolean lr = dbp.LARGE_REPO;
					
					String letter = null;
					if (!lr) {
						out = true; //enters once
					} else {
						letter = let[counter];
						logger.info("processing letter:" +letter);
					}
					
					//dbpedia 
					Query qdb = dbp.formulateSPARQLQuery(domainParams, letter, "");
					ResultSet rsdbp = dbp.executeQuery(qdb, dbp.TIMEOUT.toString(), dbp.SPARQL_END_POINT, "", "");
					dbp.processResults(rsdbp, outDictionnaireDir, letter, domainParams);
					
					counter++;
				}
			}
			if (dicLabel.equalsIgnoreCase("getty-per") || dicLabel.equalsIgnoreCase("all")) {
				// QUERY PERSONALITIES IN GETTY
				counter = 0;
				out = false;
				while (counter < let.length && !out) {
					QueryArtPersonalityGetty dbp = new QueryArtPersonalityGetty();
					Boolean lr = dbp.LARGE_REPO;
					
					String letter = null;
					if (!lr) {
						out = true; //enters once
					} else {
						letter = let[counter];
						logger.info("processing letter:" +letter);
					}
					
					//getty 
					Query qdb = dbp.formulateSPARQLQuery(domainParams, letter, "");
					ResultSet rsdbp = dbp.executeQuery(qdb, dbp.TIMEOUT.toString(), dbp.SPARQL_END_POINT, "", "");
					dbp.processResults(rsdbp, outDictionnaireDir, letter, domainParams);
					
					counter++;
				}
			}
			if (dicLabel.equalsIgnoreCase("bne-all") || dicLabel.equalsIgnoreCase("all")) {
				// QUERY AUTHORS IN BNE
				counter = 0;
				out = false;
				while (counter < let.length && !out) {
					QueryAuthorBNEAll dbp = new QueryAuthorBNEAll();
					Boolean lr = dbp.LARGE_REPO;
					
					String letter = null;
					if (!lr) {
						out = true; //enters once
					} else {
						letter = let[counter];
						logger.info("processing letter:" +letter);
					}
					
					//bne 
					Query qdb = dbp.formulateSPARQLQuery(domainParams, letter, "");
					ResultSet rsdbp = dbp.executeQuery(qdb, dbp.TIMEOUT.toString(), dbp.SPARQL_END_POINT, "", "");
					dbp.processResults(rsdbp, outDictionnaireDir, letter, domainParams);
					
					counter++;
				}
			}
			if (dicLabel.equalsIgnoreCase("bne") || dicLabel.equalsIgnoreCase("all")) {
				// QUERY AUTHORS IN BNE (filter by date)
				counter = 0;
				out = false;
				while (counter < let.length && !out) {
					QueryAuthorBNE dbp = new QueryAuthorBNE();
					Boolean lr = dbp.LARGE_REPO;
					
					String letter = null;
					if (!lr) {
						out = true; //enters once
					} else {
						letter = let[counter];
						logger.info("processing letter:" +letter);
					}
					
					//bne 
					Query qdb = dbp.formulateSPARQLQuery(domainParams, letter, "");
					ResultSet rsdbp = dbp.executeQuery(qdb, dbp.TIMEOUT.toString(), dbp.SPARQL_END_POINT, "", "");
					dbp.processResults(rsdbp, outDictionnaireDir, letter, domainParams);
					
					counter++;
				}
			}
			logger.info("exiting: crawlsLinkedData");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Handles temporal and spatial parameters to add specific domain information to the crawler.
	 * @param timePeriodBegin
	 * @param timePeriodEnd
	 * @param spatialExtent
	 * @return
	 */
	public static List<TopicExtent> loadDomainParams(String timePeriodBegin, String timePeriodEnd, String spatialExtent) {
		logger.info("Entering: loadDomainParams");
		List<TopicExtent> domainParams = new ArrayList<TopicExtent>();
		try {
			//temporal information
			TemporalExtent temEx = new TemporalExtent();
			if (!timePeriodBegin.equals("-1")) {
				SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
				temEx.setGreaterThan(formatter.parse(timePeriodBegin.trim()));	
			}
			if (!timePeriodEnd.equals("-1")) {
				SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
				temEx.setLesserThan(formatter.parse(timePeriodEnd.trim()));	
			}
			if (temEx != null) {
				domainParams.add(temEx);
			}
			//spatial information TODO
		} catch (ParseException e) {
			e.printStackTrace();
		}
		logger.info("exiting: loadDomainParams");
		return domainParams;
	}
}
