package fr.lip6.ldextractor;

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
 * This class queries the authors catalog in the BnF SPARQL end point.
 * 
 * @author Brando & Frontini
 */
public class QueryAuthorBNF extends QuerySource implements QuerySourceInterface {

	private static Logger logger = Logger.getLogger(QueryAuthorBNF.class);
	
	/**
	 * Mandatory fields
	 */
	public String SPARQL_END_POINT = "http://data.bnf.fr/sparql";
	
	public Integer TIMEOUT = 200000;
	
	public Boolean LARGE_REPO = true;
	
	public String prefixDictionnaireFile = "authorBNF";
	
	/**
	 * Default constructor.
	 */
	public QueryAuthorBNF () {
		super();
	}
	
	/**
	 * Formulate a query which is decomposed in several sub-queries because of size of the BnF repo. 
	 *  
	 */
	@Override
	public Query formulateSPARQLQuery(List<TopicExtent> domainParams, 
			String firstLetter, String outDictionnaireDir) {
		
		File fexists = new File(outDictionnaireDir+"/"+prefixDictionnaireFile+firstLetter+".tsv");
		
		if (fexists.exists() && fexists.length() > 0) {
			System.out.println("entering BNF: skip, file exists - "+
					outDictionnaireDir+"/"+prefixDictionnaireFile+firstLetter+".tsv");
			return null; //skip processing
		} else {
			logger.info("entering BNF: formulateSPARQLQuery");
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
			
			String queryString = "PREFIX dcterms: <http://purl.org/dc/terms/> "
						+ "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
						+ "PREFIX foaf: <http://xmlns.com/foaf/0.1/> "
						+ "PREFIX owl: <http://www.w3.org/2002/07/owl#> "
						+ "PREFIX bio: <http://vocab.org/bio/0.1/> "
						+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "
						+ "PREFIX bnf-onto: <http://data.bnf.fr/ontology/bnf-onto/> "
						+ "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> "
						+ "SELECT ?auteur ?nom ?prenom ?gender ?birthdate ?deathdate ?rejectedForm ?ref WHERE { "
						+ "?auteur rdf:type foaf:Person . "
						+ "OPTIONAL {?auteur foaf:givenName ?prenom } . " //enables empty first names (eg. Voltaire)
						+ "?auteur foaf:familyName ?nom . "
						+ "OPTIONAL { ?auteur bnf-onto:firstYear ?birthdate } . "+ filterDate + " . " //attention: filter cannot be inside an optional, obvious!
						+ "OPTIONAL { ?auteur bnf-onto:lastYear ?deathdate } . "
						+ "OPTIONAL { ?idArk foaf:focus ?auteur . ?idArk skos:altLabel ?rejectedForm . filter(langMatches(lang(?rejectedForm),'FR')) } . "
						+ filterRegex					
						+ "OPTIONAL { ?auteur foaf:gender ?gender } . "
						+ "OPTIONAL { ?auteur owl:sameAs ?ref . "
						+ "FILTER regex(STR(?ref), '^http://www.idref.fr|^http://dbpedia.org/resource', 'i') }}";
			Query query = QueryFactory.create(queryString);
			logger.info("query: " + query.toString());
			logger.info("exiting BNF: formulateSPARQLQuery");
			return query;
		}		
	}
	
	@Override
	public ResultSet executeQuery(Query query, String timeout, String sparqlendpoint, 
			String outDictionnaireDir, String letter) {
		File fexists = new File(outDictionnaireDir+"/"+prefixDictionnaireFile+letter+".tsv");
		if (fexists.exists() && fexists.length() > 0) {
			System.out.println("entering BNF: skip, file exists - "+outDictionnaireDir+"/"+prefixDictionnaireFile+letter+".tsv");
			return null; //file exists, skip processing
		} else {
			logger.info("entering BNF: executeQuery");
			logger.info("exiting BNF: executeQuery");
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
			System.out.println("entering BNF: skip, file exists - "+outDictionnaireDir+"/"+prefixDictionnaireFile+letter+".tsv");
			return; //file exists, skip processing
		} else {
			logger.info("entering BNF: processResults");
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
				
				Map<String, Author> authors = new HashMap<String, Author>();
				
				while (res.hasNext()) {
					QuerySolution sol = res.next();
					
					//update sameAs links and alternative names for this author
					if (authors.get(sol.get("auteur").toString()) != null) { 
						Author a = authors.get(sol.get("auteur").toString());
						if (sol.get("ref") != null) {
							if (!a.getRef().contains(sol.get("ref").toString())) {
								a.getRef().add(sol.get("ref").toString());
							}
						}
						
						if (sol.get("rejectedForm") != null) {
							String val = sol.getLiteral("rejectedForm").getLexicalForm();
							if (val.contains("("))
								val = val.substring(0, val.indexOf("(")).trim();
							if (!a.getRejectedForms().contains(val))
								a.getRejectedForms().add(val);
						}
						
					} else {
						Author a = new Author();
						a.setUri(sol.get("auteur").toString());
						if (sol.get("nom") != null)
							a.setLastname(sol.get("nom").toString());
						else
							a.setLastname("-");					
						
						if (sol.get("prenom") != null)
							a.setFirstname(sol.get("prenom").toString());
						else 
							a.setFirstname("-");					
						
						if (sol.get("gender") != null)
							a.setGender(sol.get("gender").toString());
						else 
							a.setGender("-");					
						
						if (sol.get("birthdate") != null) {
							String bdate = sol.get("birthdate").toString().replace("^^http://www.w3.org/2001/XMLSchema#integer", "");
							if (bdate.matches("\\d{4}") 
									|| bdate.matches("\\d{2}"+"\\.\\.") 
									|| bdate.matches("\\d{1}"+"\\.\\."))
								a.setBirthdate(bdate);
							else if (bdate.matches("\\d{4}"+"-"+"\\d{2}"+"-"+"\\d{2}"))
								a.setBirthdate(bdate.substring(0, 4));							
						} else 
							a.setBirthdate("-");
						
						if (sol.get("deathdate") != null) {
							String ddate = sol.get("deathdate").toString().replace("^^http://www.w3.org/2001/XMLSchema#integer", "");
							if (ddate.matches("\\d{4}") 
									|| ddate.matches("\\d{2}"+"\\.\\.")
									|| ddate.matches("\\d{1}"+"\\.\\."))
								a.setDeathdate(ddate);
							else if (ddate.matches("\\d{4}"+"-"+"\\d{2}"+"-"+"\\d{2}"))
								a.setDeathdate(ddate.substring(0, 4));
						} else 
							a.setDeathdate("-");					
						
						if (sol.get("ref") != null) {
							if (!a.getRef().contains(sol.get("ref").toString())) {
								a.getRef().add(sol.get("ref").toString());
							}
						}
						
						if (sol.get("rejectedForm") != null) {
							String val = sol.getLiteral("rejectedForm").getLexicalForm();
							if (val.contains("("))
								val = val.substring(0, val.indexOf("(")).trim();
							if (!a.getRejectedForms().contains(val))
								a.getRejectedForms().add(val);							
						}
						//more rejected forms
						a.getRejectedForms().addAll(a.makeAliases());						
						authors.put(a.getUri(), a);
					}							
				}
				System.out.println("count of authors: "+authors.size());
				
				for (String uri : authors.keySet()) {
					writeAuthorToFile(writer, authors.get(uri));
				}
				writer.close();
				logger.info("exiting BNF: processResults");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void writeAuthorToFile(CSVWriter writer, Author author) {
		
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
 * Utility class for handling authors.
 *
 */
class Author {
	private String firstname;
	private String lastname;
	private String gender;
	private String uri;
	private String birthdate;
	private String deathdate;
	private List<String> ref;
	private List<String> rejectedForms;
	private static String[] hons = {"de","d'","von","da"}; 

	public Author() {
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

	public String getGender() {
		return gender;
	}

	public void setGender(String gender) {
		this.gender = gender;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getBirthdate() {
		return birthdate;
	}

	public void setBirthdate(String birthdate) {
		this.birthdate = birthdate;
	}

	public List<String> getRef() {
		return ref;
	}

	public void setRef(List<String> ref) {
		this.ref = ref;
	}

	public String getDeathdate() {
		return deathdate;
	}

	public void setDeathdate(String deathdate) {
		this.deathdate = deathdate;
	}

	public List<String> getRejectedForms() {
		return rejectedForms;
	}

	public void setRejectedForms(List<String> rejectedForms) {
		this.rejectedForms = rejectedForms;
	}
	
	public String getNormalisedName() {
		String normalisedName = "";
		if (this.getFirstname().equals("-") || this.getFirstname().equals("")) {
			normalisedName = this.getLastname();
		} else {
			normalisedName = this.getLastname() + ", "+this.getFirstname();
		}
		normalisedName = normalisedName.replaceAll("'", "' ");
		if (this.getBirthdate() != null && this.getDeathdate() != null) {
			normalisedName = normalisedName+ " ("+this.getBirthdate()+"-"+this.getDeathdate()+")";					
		}
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
	
	public String getTitle() {
		if (this.getGender().equalsIgnoreCase("female")) {
			return "Mme";
		} else {
			return "M";
		}	
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
		if (!this.getFirstname().equals("-") && !this.getFirstname().equals(""))
			aliases.add(this.getFirstname() + " " + this.getLastname());		
				
		//generate_family_name_only
		aliases.add(this.getLastname());
		
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