package fr.lip6.ldcrawler;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.reflections.Reflections;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.ResultSet;

import fr.lip6.LDcrawler.domainParam.DomainExtent;
import fr.lip6.LDcrawler.domainParam.TemporalExtent;
import fr.lip6.LDcrawler.query.QuerySource;

/**
 * Main class to launch the Linked Data crawler for handling domain-adaptation during named-entity linking.
 * @author Brando & Frontini - Labex OBVIL - Université Paris-Sorbonne - UPMC LIP6
 */
public class App 
{
	/**
	 * Main method.
	 * @param args
	 */
	public static void main(String [] args) {
		crawlsLinkedData("config/config.properties");		
	}
	
	//TODO piste: http://fr.wikipedia.org/wiki/Adaptateur_%28patron_de_conception%29
	/**
	 * Crawls Linked Data.
	 * @param propertiesFile, the configuration file
	 */
	@SuppressWarnings("rawtypes")
	public static void crawlsLinkedData(String propertiesFile) {
		
		// reading parameters
		try {
			System.out.println("entering: crawlsLinkedData");
			Properties prop = new Properties();
			InputStream input = new FileInputStream(propertiesFile);
			prop.load(input);
			//loading main config file
			String timeout = prop.getProperty("timeout");
			String outDictionnaireDir = prop.getProperty("outDictionnaireDir");
			String greaterThan = prop.getProperty("greaterThan");
			String lesserThan = prop.getProperty("lesserThan");
			String spatialExtent = prop.getProperty("spatialExtent");
			
			//prepare domain-adaptation parameters from config file
			List<DomainExtent> domainParams = loadDomainParams(greaterThan, lesserThan, spatialExtent);
			
			//execute all queries defined in classes implementing the QuerySource interface
			Reflections reflections = new Reflections("fr.lip6.LDcrawler.queryImpl");  
			Set<Class<? extends QuerySource>> classes = reflections.getSubTypesOf(QuerySource.class);
			for (Class<? extends QuerySource> clazz : classes) {
				System.out.println("processing class: "+clazz.getName());
				Class<?> c = Class.forName(clazz.getName());
				Object t = c.newInstance();
				
				Field largeRepo = c.getDeclaredField("LARGE_REPO");
				Object lrO = largeRepo.get(t);
				Boolean lr = (Boolean) lrO;
				
				int counter = 0;
				Boolean out = false;
				//one solution to handle limitations of large repositories
				//String[] let =  {"a","b","c","d","e","f","g","h","i","j","k","l","m","n",
				//		"o","p","q","r","s","t","u","v","w","x", "y", "z", "other"};
				String[] let =  {"x"}; //testing
				while (counter < let.length && !out) {
					String letter = null;
					if (!lr) {
						out = true; //enters once
					} else {
						letter = let[counter];
						System.out.println("processing letter:" +letter);
					}
					//formulateSPARQLQuery
					Class[] cArg0 = new Class[2];
			        cArg0[0] = List.class;
			        cArg0[1] = String.class;
				    Method buildqueryMethod = c.getDeclaredMethod("formulateSPARQLQuery", cArg0);
				    Object o = buildqueryMethod.invoke(t, domainParams, letter);
					Query q = (Query) o;
				    
					//executeQuery
					Class[] cArg = new Class[3];
			        cArg[0] = Query.class;
			        cArg[1] = String.class;
			        cArg[2] = String.class;
			        Method lMethod = c.getDeclaredMethod("executeQuery", cArg);
			        Object o2 = lMethod.invoke(t, q, "", timeout); //get timout from class instead of config
			        ResultSet rs = (ResultSet) o2;
			        
			        //processResults
			        Class[] cArg2 = new Class[3];
			        cArg2[0] = ResultSet.class;
			        cArg2[1] = String.class;
			        cArg2[2] = String.class;
			        Method lMethod2 = c.getDeclaredMethod("processResults", cArg2);
			        lMethod2.invoke(t, rs, outDictionnaireDir, letter);	
			        
			        counter++;					
				}
			}
			System.out.println("exiting: crawlsLinkedData");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException x) {
			x.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
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
