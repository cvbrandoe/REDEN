package fr.lip6.ldcrawler;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.ResultSet;

import fr.lip6.ldcrawler.queryimpl.QueryAuthorBNF;
import fr.lip6.ldcrawler.queryimpl.QueryPlaceDBpedia;

/**
 * Main class to launch the Linked Data crawler for handling domain-adaptation during named-entity linking.
 * @author Brando & Frontini - Labex OBVIL - Université Paris-Sorbonne - UPMC LIP6
 */
public class AppAdhoc 
{
	/**
	 * Main method.
	 * @param args
	 */
	public static void main(String [] args) {
		crawlsLinkedData("config/config.properties");		
	}
	
	/**
	 * Crawls Linked Data.
	 * @param propertiesFile, the configuration file
	 */
	public static void crawlsLinkedData(String propertiesFile) {
		
		// reading parameters
		try {
			System.out.println("entering: crawlsLinkedData");
			Properties prop = new Properties();
			InputStream input = new FileInputStream(propertiesFile);
			prop.load(input);
			//loading main config file
			String outDictionnaireDir = prop.getProperty("outDictionnaireDir");
			String greaterThan = prop.getProperty("greaterThan");
			String lesserThan = prop.getProperty("lesserThan");
			String spatialExtent = prop.getProperty("spatialExtent");
			
			//prepare domain-adaptation parameters from config file
			List<DomainExtent> domainParams = loadDomainParams(greaterThan, lesserThan, spatialExtent);
			
			//execute all queries defined in classes implementing the QuerySource interface -- adhoc way so far
			String[] let =  {"a","b","c","d","e","f","g","h","i","j","k","l","m","n",
					"o","p","q","r","s","t","u","v","w","x", "y","z", "other"};
			//String[] let =  {"x"}; //testing
			int counter = 0;
			Boolean out = false;
			while (counter < let.length && !out) {
				QueryAuthorBNF bnf = new QueryAuthorBNF();
				Boolean lr = bnf.LARGE_REPO;
				String letter = null;
				if (!lr) {
					out = true; //enters once
				} else {
					letter = let[counter];
					System.out.println("processing letter:" +letter);
				}
				
				//bnf				
				Query qbnf = bnf.formulateSPARQLQuery(domainParams, letter, "");
				ResultSet rsbnf = bnf.executeQuery(qbnf, bnf.TIMEOUT.toString(), bnf.SPARQL_END_POINT, "", "");
				bnf.processResults(rsbnf, outDictionnaireDir, letter);
				counter++;
			}
			
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
					System.out.println("processing letter:" +letter);
				}
				
				//dbpedia 
				Query qdb = dbp.formulateSPARQLQuery(domainParams, letter, "");
				ResultSet rsdbp = dbp.executeQuery(qdb, dbp.TIMEOUT.toString(), dbp.SPARQL_END_POINT, "", "");
				dbp.processResults(rsdbp, outDictionnaireDir, letter);
				
				counter++;
			}
			
			System.out.println("exiting: crawlsLinkedData");
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
	public static List<DomainExtent> loadDomainParams(String timePeriodBegin, String timePeriodEnd, String spatialExtent) {
		System.out.println("Entering: loadDomainParams");
		List<DomainExtent> domainParams = new ArrayList<DomainExtent>();
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
		System.out.println("exiting: loadDomainParams");
		return domainParams;
	}
}
