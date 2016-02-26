package fr.lip6.reden.ldextractor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.opencsv.CSVWriter;

/**
 * This class queries the person catalog in the Getty SPARQL end point.
 * 
 * @author Brando & Frontini
 */
public class QueryPersonalityGetty extends QuerySource implements QuerySourceInterface {

	private static Logger logger = Logger.getLogger(QueryPersonalityGetty.class);
	
	/**
	 * Mandatory fields
	 */
	public String SPARQL_END_POINT = "http://vocab.getty.edu/sparql";
	
	public Integer TIMEOUT = 200000;
	
	public Boolean LARGE_REPO = true;
	
	public String prefixDictionnaireFile = "personGetty";
		
	/**
	 * Default constructor.
	 */
	public QueryPersonalityGetty () {
		super();
	}
	
	/**
	 * Formulate a query which is decomposed in several sub-queries because of size of the data. 
	 *  
	 */
	@Override
	public Query formulateSPARQLQuery(List<TopicExtent> domainParams, 
			String firstLetter, String outDictionnaireDir) {
		
		File fexists = new File(outDictionnaireDir+"/"+prefixDictionnaireFile+firstLetter+".tsv");
		
		if (fexists.exists() && fexists.length() > 0) {
			System.out.println("entering Getty: skip, file exists - "+
					outDictionnaireDir+"/"+prefixDictionnaireFile+firstLetter+".tsv");
			return null; //skip processing
		} else {
			logger.info("entering Getty: formulateSPARQLQuery");
			//temporal information can be incorporated into the query in many ways
			String filterDate = "";
			for (TopicExtent d : domainParams) {
				if (d instanceof TemporalExtent) {
					TemporalExtent tem = (TemporalExtent) d;
					if (tem.getLesserThan() != null) {
						SimpleDateFormat df = new SimpleDateFormat("yyyy");
						String year = df.format(tem.getLesserThan());
						filterDate += "FILTER (?birthdate < '"+year+"'^^xsd:integer )";
					}
					if (tem.getGreaterThan() != null) {
						SimpleDateFormat df = new SimpleDateFormat("yyyy");
						String year = df.format(tem.getGreaterThan());
						filterDate += "FILTER (?birthdate > '"+year+"'^^xsd:integer )";
					}
				} else if (d instanceof SpatialExtent) {
					//TODO include query statement (geo vocabulary) to handle spatial delimitation
					//concerning placeOfBirth for instance
				}
			}
			String filterRegex = "";
			if (firstLetter.equalsIgnoreCase("other")) {
				filterRegex = "FILTER (!regex(STR(?nom), '^a|^b|^c|^d|^e|^f|^g|^h|^i|^j|^k|^l|^m|^n|^o|^p|^q|^r|^s|^t|^u|^v|^w|^x|^y|^z', 'i')) . ";			
			} else {
				filterRegex = "FILTER (regex(STR(?nom), '^"+firstLetter+"', 'i')) . ";			
			}
			
			String queryString = "PREFIX gvp: <http://vocab.getty.edu/ontology#> "
					+ "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
					+ "PREFIX skos:<http://www.w3.org/2004/02/skos/core#> "
					+ "PREFIX schema:<http://schema.org/> "
					+ "PREFIX foaf: <http://xmlns.com/foaf/0.1/> "
					+ "select ?person ?nom ?altname ?ref ?gender WHERE { "
					+ "?person rdf:type gvp:PersonConcept . "
					+ "?person skos:prefLabel ?nom . "
					+ filterRegex
					+ "OPTIONAL { ?person skos:altLabel ?altname } . "				
					+ "OPTIONAL { ?person skos:exactMatch ?ref . FILTER (!regex(STR(?ref), '^http://vocab.getty.edu', 'i')) } . "
					+ "OPTIONAL { ?person foaf:focus ?agent . "
					+ "?agent gvp:biography ?biopers . "
					//+ TODO (filter by date but there are several dates?) "?biopers gvp:estStart ?birthdate . " for obtaining the birthdate but there are two, which one to choose systematically?
					+ "?biopers schema:gender ?gender } }"; //gender uses codes from an ontology but these are inconsistent
			Query query = QueryFactory.create(queryString);
			logger.info("query: " + query.toString());
			logger.info("exiting Getty: formulateSPARQLQuery");
			return query;
		}		
	}
	
	@Override
	public ResultSet executeQuery(Query query, String timeout, String sparqlendpoint, 
			String outDictionnaireDir, String letter) {
		File fexists = new File(outDictionnaireDir+"/"+prefixDictionnaireFile+letter+".tsv");
		if (fexists.exists() && fexists.length() > 0) {
			System.out.println("entering Getty: skip, file exists - "+outDictionnaireDir+"/"+prefixDictionnaireFile+letter+".tsv");
			return null; //file exists, skip processing
		} else {
			logger.info("entering Getty: executeQuery");
			logger.info("exiting Getty: executeQuery");
			return super.executeQuery(query, SPARQL_END_POINT, timeout, 
					outDictionnaireDir, letter);
		}
	}
	
	/**
	 * Handling of the results specific to the BnF model.
	 */
	@Override
	public void processResults(ResultSet res, String outDictionnaireDir, String letter) {
		
		File fexists = new File(outDictionnaireDir+"/"+prefixDictionnaireFile+letter+".tsv");
	
		if (fexists.exists() && fexists.length() > 0) {
			System.out.println("entering Getty: skip, file exists - "+outDictionnaireDir+"/"+prefixDictionnaireFile+letter+".tsv");
			return; //file exists, skip processing
		} else {
			logger.info("entering Getty: processResults");
			try {
				if (letter != null) {
					prefixDictionnaireFile += letter;
				}
				if (!new File(outDictionnaireDir).exists()) {
					new File(outDictionnaireDir).mkdir();			
				}
				if (!new File(outDictionnaireDir).exists()) {
					new File(outDictionnaireDir).mkdir();
				}
				CSVWriter writer = new CSVWriter(new FileWriter(outDictionnaireDir+"/"+prefixDictionnaireFile+".tsv"), 
						'\t', CSVWriter.NO_QUOTE_CHARACTER);
				
				Map<String, Personality> authors = new HashMap<String, Personality>();
				
				while (res.hasNext()) {
					QuerySolution sol = res.next();
					
					//update sameAs links and alternative names for this author
					if (authors.get(sol.get("person").toString()) != null) { 
						
						Personality a = authors.get(sol.get("person").toString());
						if (sol.get("ref") != null) {
							if (!a.getRef().contains(sol.get("ref").toString())) {
								a.getRef().add(sol.get("ref").toString());
							}
						}
						
					} else {
						Personality a = new Personality();
						a.setUri(sol.get("person").toString());
						if (sol.get("nom") != null) {
							if (sol.get("nom").toString().contains(",")) {
								String[] val = sol.get("nom").toString().split(",");
								a.setLastname(val[0]);
								if (val.length > 1)
									a.setFirstname(sol.get("nom").toString().split(",")[1]);
								else 
									a.setFirstname("-");
							} else {
								a.setLastname(sol.get("nom").toString());
								a.setFirstname("-");
							}
						} else {
							a.setLastname("-");
							a.setFirstname("-");
						}
						
						if (sol.get("gender") != null)
							a.setGender(sol.get("gender").toString());
						else 
							a.setGender("-");
						
						if (sol.get("ref") != null) {
							if (!a.getRef().contains(sol.get("ref").toString())) {
								a.getRef().add(sol.get("ref").toString());
							}
						}
						
						if (sol.get("altname") != null) {
							String val = sol.getLiteral("altname").getLexicalForm();
							if (!a.getRejectedForms().contains(val)) {
								a.getRejectedForms().add(val);			
							}
						}
						//more rejected forms
						a.getRejectedForms().addAll(a.makeAliases());						
						authors.put(a.getUri(), a);
					}							
				}
				System.out.println("count of persons: "+authors.size());
				
				for (String uri : authors.keySet()) {
					writePersonalityToFile(writer, authors.get(uri));
				}
				writer.close();
				logger.info("exiting Getty: processResults");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void writePersonalityToFile(CSVWriter writer, Personality author) {
		
		//build URIs
		StringBuilder commaSepValueBuilder = new StringBuilder();
		commaSepValueBuilder.append(author.getUri());
		for (String uris : author.getRef()){
			commaSepValueBuilder.append("\t"+uris);	     
		}
		for (String aliases : author.getRejectedForms()) {
			String[] entry = { aliases, author.getNormalisedName(), commaSepValueBuilder.toString()};
			writer.writeNext(entry);			
		}		
	}	
}

/**
 * Utility class for handling personalities in Getty.
 *
 */
class Personality {
	private String firstname;
	private String lastname;
	private String uri;
	private List<String> ref;
	private List<String> rejectedForms;
	private String gender;
	private static String[] hons = {"de","d'","von","da"};
	private static String codeGettyFemale = "http://vocab.getty.edu/aat/300189557";	
	private static String codeGettyMale = "http://vocab.getty.edu/aat/300189559";
	

	public Personality() {
		this.rejectedForms = new ArrayList<String>();
		this.ref = new ArrayList<String>();
	}

	public String getFirstname() {
		return firstname;
	}

	public void setFirstname(String firstname) {
		this.firstname = firstname.replaceAll("-", " ").trim();
	}

	public String getLastname() {
		return lastname;
	}

	public void setLastname(String lastname) {
		this.lastname = lastname.replaceAll("-", " ").trim();
	}
	
	public String getTitle() {
		if (this.getGender().equalsIgnoreCase(codeGettyFemale)) {
			return "Mme";
		} else {
			return "M";
		}	
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public List<String> getRef() {
		return ref;
	}

	public void setRef(List<String> ref) {
		this.ref = ref;
	}

	public List<String> getRejectedForms() {
		return rejectedForms;
	}

	public void setRejectedForms(List<String> rejectedForms) {
		this.rejectedForms = rejectedForms;
	}
	
	public String getGender() {
		return gender;
	}

	public void setGender(String gender) {
		this.gender = gender;
	}
	
	public String getNormalisedName() {
		String normalisedName = "";
		if (this.getFirstname().equals("-") || this.getFirstname().equals("")) {
			normalisedName = this.getLastname();
		} else {
			normalisedName = this.getLastname() + ", "+this.getFirstname();
		}
		normalisedName = normalisedName.replaceAll("'", "' ");
		return normalisedName.replaceAll("  ", " ");		
	}
	
	public String makeFirstNameInitials() {
		String initials = "";
		for (int i = 0; i < this.getFirstname().length(); i++) {
			if(Character.isUpperCase(this.getFirstname().charAt(i))){
				initials +=  " " + this.getFirstname().charAt(i);
			}
		}
		return initials.trim();
	}
	
	public String getHonorific() {
		for(String hon : hons) {
			String honSpace = " " + hon;
			if (this.firstname.endsWith(honSpace))
				return hon;				
		}
		return null;
	}
	
	/**
	 * Rules to generate alternative names for authors (Francesca)
	 * @return
	 */
	public Set<String> makeAliases() {
		Set<String> aliases = new HashSet<String>();
		
		//generate_full_name
		if (!this.getFirstname().equals("-") && !this.getFirstname().equals("")) {
			String val = this.getFirstname() + " " + this.getLastname();
			if (!this.getRejectedForms().contains(val)) {
				aliases.add(val);
			}
		}
				
		//generate_family_name_only
		String val = this.getLastname();
		if (!this.getRejectedForms().contains(val)) {
			aliases.add(val);
		}
		
		//generate_titles
		aliases.add(this.getTitle() + " " + this.getLastname());
		if (!this.getFirstname().equals("-") && !this.getFirstname().equals(""))
			aliases.add(this.getTitle() + " " + this.getFirstname() + " " + this.getLastname());
		
		//generate_titles_with dots
		aliases.add(this.getTitle() + ". " + this.getLastname());
		if (!this.getFirstname().equals("-") && !this.getFirstname().equals(""))
			aliases.add(this.getTitle() + ". " + this.getFirstname() + " " + this.getLastname());		
				
		//if there is a honorific it generates the version with the honorific as well
		if (this.getHonorific() != null) {						
			aliases.add(this.getHonorific() + " " + this.getLastname());
			aliases.add(Character.toUpperCase(this.getHonorific().charAt(0)) + this.getHonorific().substring(1) + " " + this.getLastname());
		}			
		
		//generate_initials_with_family_name
		String initials = "", initials_dot = "", honorific = "";			
		if (this.makeFirstNameInitials() != null && !this.makeFirstNameInitials().equals("")) {
			initials = this.makeFirstNameInitials();
			initials_dot = this.makeFirstNameInitials().replaceAll(" ", ". ");
			initials_dot += ".";
			
			aliases.add(initials + " " + this.getLastname());			
			aliases.add(initials_dot + " " + this.getLastname());			
		}
		
		if (this.getHonorific() != null) {
			honorific = this.getHonorific() + " ";			
			aliases.add(this.getTitle() + " " + honorific + this.getLastname());
			aliases.add(this.getTitle() + ". " + honorific + this.getLastname());
			aliases.add(this.getTitle() + " " + Character.toUpperCase(honorific.charAt(0)) + honorific.substring(1) + this.getLastname());
			aliases.add(this.getTitle() + ". " + Character.toUpperCase(honorific.charAt(0)) + honorific.substring(1) + this.getLastname());
		
			if (!initials.equals("") && !initials_dot.equals("")) {
				aliases.add(initials + " " + honorific + this.getLastname());
				aliases.add(initials + " " + Character.toUpperCase(honorific.charAt(0)) + honorific.substring(1) + this.getLastname());
				aliases.add(initials_dot + " " + Character.toUpperCase(honorific.charAt(0)) + honorific.substring(1) + this.getLastname());
				aliases.add(initials_dot + " " + honorific + this.getLastname());
			}
			
		}
		
		return aliases;
	}

}