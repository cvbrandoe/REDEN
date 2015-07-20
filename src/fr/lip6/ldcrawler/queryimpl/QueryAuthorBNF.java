package fr.lip6.ldcrawler.queryimpl;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.opencsv.CSVWriter;

import fr.lip6.ldcrawler.domainparam.DomainExtent;
import fr.lip6.ldcrawler.domainparam.SpatialExtent;
import fr.lip6.ldcrawler.domainparam.TemporalExtent;
import fr.lip6.ldcrawler.query.QuerySource;
import fr.lip6.ldcrawler.query.QuerySourceInterface;

/**
 * This class queries the authors catalog in the BnF SPARQL end point.
 * 
 * @author Brando & Frontini - Labex OBVIL - Université Paris-Sorbonne - UPMC LIP6
 */
public class QueryAuthorBNF extends QuerySource implements QuerySourceInterface {

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
	public Query formulateSPARQLQuery(List<DomainExtent> domainParams, 
			String firstLetter, String outDictionnaireDir) {
		
		File fexists = new File(outDictionnaireDir+"/PER/"+prefixDictionnaireFile+firstLetter+".tsv");
		
		if (fexists.exists() && fexists.length() > 0) {
			System.out.println("entering BNF: skip, file exists - "+
					outDictionnaireDir+"/PER/"+prefixDictionnaireFile+firstLetter+".tsv");
			return null; //skip processing
		} else {
			System.out.println("entering BNF: formulateSPARQLQuery");
			//temporal information can be incorporated into the query in many ways
			String filterDate = "";
			for (DomainExtent d : domainParams) {
				if (d instanceof TemporalExtent) {
					TemporalExtent tem = (TemporalExtent) d;
					if (tem.getLesserThan() != null) {
						SimpleDateFormat df = new SimpleDateFormat("yyyy");
						String year = df.format(tem.getLesserThan());
						filterDate += "OPTIONAL { FILTER (?birthdate < '"+year+"'^^xsd:integer ) } . ";
					}
					if (tem.getGreaterThan() != null) {
						SimpleDateFormat df = new SimpleDateFormat("yyyy");
						String year = df.format(tem.getGreaterThan());
						filterDate += "OPTIONAL { FILTER (?birthdate > '"+year+"'^^xsd:integer ) } . ";
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
						+ "SELECT ?auteur ?nom ?prenom ?gender ?birthdate ?deathdate ?ref WHERE { "
						+ "?auteur rdf:type foaf:Person . "
						+ "OPTIONAL {?auteur foaf:givenName ?prenom } . " //enables empty first names (eg. Voltaire)
						+ "?auteur foaf:familyName ?nom . "
						+ "OPTIONAL { ?auteur bnf-onto:firstYear ?birthdate } . "
						+ "OPTIONAL { ?auteur bnf-onto:lastYear ?deathdate } . "
						+ filterRegex
						+ filterDate
						+ "OPTIONAL { ?auteur foaf:gender ?gender } . "
						+ "OPTIONAL { ?auteur owl:sameAs ?ref . "
						+ "FILTER regex(STR(?ref), '^http://www.idref.fr|^http://dbpedia.org/resource', 'i') }}";
			Query query = QueryFactory.create(queryString);
			System.out.println("query: " + query.toString());
			System.out.println("exiting BNF: formulateSPARQLQuery");
			return query;
		}		
	}
	
	@Override
	public ResultSet executeQuery(Query query, String timeout, String sparqlendpoint, 
			String outDictionnaireDir, String letter) {
		File fexists = new File(outDictionnaireDir+"/PER/"+prefixDictionnaireFile+letter+".tsv");
		if (fexists.exists() && fexists.length() > 0) {
			System.out.println("entering BNF: skip, file exists - "+outDictionnaireDir+"/PER/"+prefixDictionnaireFile+letter+".tsv");
			return null; //file exists, skip processing
		} else {
			System.out.println("entering BNF: executeQuery");
			System.out.println("exiting BNF: executeQuery");
			return super.executeQuery(query, SPARQL_END_POINT, timeout, 
					outDictionnaireDir, letter);
		}
	}
	
	/**
	 * Handling of the results specific to the BnF model.
	 */
	@Override
	public void processResults(ResultSet res, String outDictionnaireDir, String letter) {
		
		//TODO ici: faire les authors aliasing (python) et repenser cette methode, trop de requetes à la bnf pour avoir les
		//noms alternatifs, pas possible
		
		File fexists = new File(outDictionnaireDir+"/PER/"+prefixDictionnaireFile+letter+".tsv");
	
		if (fexists.exists() && fexists.length() > 0) {
			System.out.println("entering BNF: skip, file exists - "+outDictionnaireDir+"/PER/"+prefixDictionnaireFile+letter+".tsv");
			return; //file exists, skip processing
		} else {
			System.out.println("entering BNF: processResults");
			try {
				if (letter != null) {
					prefixDictionnaireFile += letter;
				}
				if (!new File(outDictionnaireDir).exists()) {
					new File(outDictionnaireDir).mkdir();			
				}
				if (!new File(outDictionnaireDir+"/PER").exists()) {
					new File(outDictionnaireDir+"/PER").mkdir();
				}
				CSVWriter writer = new CSVWriter(new FileWriter(outDictionnaireDir+"/PER/"+prefixDictionnaireFile+".tsv"), 
						'\t', CSVWriter.NO_QUOTE_CHARACTER);
				Map<String, Author> authors = new HashMap<String, Author>();
				while (res.hasNext()) {
					QuerySolution sol = res.next();
					Author a = new Author();
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
					
					if (sol.get("birthdate") != null)
						a.setBirthdate(sol.get("birthdate").toString().replace("^^http://www.w3.org/2001/XMLSchema#integer", ""));
					else 
						a.setBirthdate("-");
					//System.out.println("birthdate: "+a.getBirthdate());
					
					if (sol.get("deathdate") != null)
						a.setDeathdate(sol.get("deathdate").toString().replace("^^http://www.w3.org/2001/XMLSchema#integer", ""));
					else 
						a.setDeathdate("-");
					//System.out.println("death: "+a.getDeathdate());
					
					if (sol.get("auteur") != null)
						a.setUri(sol.get("auteur").toString());
					else 
						a.setUri("-");
					
					if (sol.get("ref") != null)
						a.setRef(sol.get("ref").toString());
					else 
						a.setRef("");
					
					if (authors.containsKey(a.getUri())) {
						String oldref = authors.get(a.getUri()).getRef();
						authors.get(a.getUri()).setRef(oldref+"\t"+a.getRef());
					} else {
						boolean in = false;
						for (String uri : authors.keySet()) {
							if (authors.get(uri).getLastname().equals(a.getLastname()) && 
									(a.getFirstname().equals("") || a.getFirstname().equals(" ")) &&
									a.getGender().equals("") &&
									a.getRef().equals("") &&
									a.getRejectedForms().isEmpty() &&
									a.getDeathdate().equals("") &&
									a.getBirthdate().equals("")) {
								in = true;
							}
						}
						if (!in)
							authors.put(a.getUri(), a);
					}
					//In BnF, we need to make a separate query for each author entry in order to get their alternative names.
					//look for altNames
					String query2 = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> "
							+ "SELECT ?rejectedForm WHERE { "
							+ " <"+a.getUri().split("#")[0]+"> skos:altLabel ?rejectedForm . filter(langMatches(lang(?rejectedForm),'FR'))} ";
					System.out.println(query2);
					QueryExecution qexec2 = null;
					//default
					qexec2 = QueryExecutionFactory.sparqlService(SPARQL_END_POINT, query2);
					// Execute.
					ResultSet rs2 = qexec2.execSelect();
					if (rs2 != null) {
						while (rs2.hasNext()) {
							QuerySolution sol2 = rs2.next();
							String val = sol2.getLiteral("rejectedForm").getLexicalForm();
							if (val.contains("("))
								val = val.substring(0, val.indexOf("(")).trim();
							a.getRejectedForms().add(val);
						}	
					}
					qexec2.close();
				}
				
				System.out.println("count: "+authors.size());
				Iterator<String> it = authors.keySet().iterator();
				while (it.hasNext()) {
					String k = it.next();
					//adapt dates
					String bdate = "",ddate = "";
					if (authors.get(k).getBirthdate().matches("\\d{4}") 
							|| authors.get(k).getBirthdate().matches("\\d{2}"+"\\.\\.") 
							|| authors.get(k).getBirthdate().matches("\\d{1}"+"\\.\\."))
						bdate = authors.get(k).getBirthdate();
					else if (authors.get(k).getBirthdate().matches("\\d{4}"+"-"+"\\d{2}"+"-"+"\\d{2}"))
						bdate = authors.get(k).getBirthdate().substring(0, 4);
					
					
					if (authors.get(k).getDeathdate().matches("\\d{4}") 
							|| authors.get(k).getDeathdate().matches("\\d{2}"+"\\.\\.")
							|| authors.get(k).getBirthdate().matches("\\d{1}"+"\\.\\."))
						ddate = authors.get(k).getDeathdate();
					else if (authors.get(k).getDeathdate().matches("\\d{4}"+"-"+"\\d{2}"+"-"+"\\d{2}"))
						ddate = authors.get(k).getDeathdate().substring(0, 4);
					
					
					/*//TODO aliases handling
					
					String normalisedName = authors.get(k).makeNormalisedName();
					
					List<String> alternativeNameForms = authors.get(k).makeAliases();
					
					for (String alias : alternativeNameForms) {
						if (bdate.equals("") && ddate.equals("")) {
							String [] o = {alias, normalisedName,
									authors.get(k).getUri() + "\t" +authors.get(k).getRef()};
							writer.writeNext(o);
						} else {
							String [] o = { alias, normalisedName+ " ("+bdate+"-"+ddate+")", 
								authors.get(k).getUri() + "\t" +authors.get(k).getRef()};
							writer.writeNext(o);
						}
					}*/
					
					String normalisedName = "";
					
					if (authors.get(k).getFirstname().equals("") || authors.get(k).getFirstname().equals(" ")) {
						normalisedName = authors.get(k).getLastname();
					} else {
						normalisedName = authors.get(k).getLastname() + ", "+authors.get(k).getFirstname();
					}
					normalisedName = normalisedName.replaceAll("'", "' ").replaceAll("  ", " ");
					
					StringBuilder commaSepValueBuilder = new StringBuilder();
				 
				    for ( int i = 0; i< authors.get(k).getRejectedForms().size(); i++){
				      //append the value into the builder
				      commaSepValueBuilder.append(authors.get(k).getRejectedForms().get(i));
				       
				      //if the value is not the last element of the list
				      //then append the comma(,) as well
				      if ( i != authors.get(k).getRejectedForms().size()-1){
				        commaSepValueBuilder.append("| ");
				      }
				    }
				   
					if (bdate.equals("") && ddate.equals("")) {
						String [] o = { authors.get(k).getUri(), normalisedName,
								authors.get(k).getLastname(), authors.get(k).getFirstname(), commaSepValueBuilder.toString(),
								authors.get(k).getGender(), "-", authors.get(k).getRef()};
						writer.writeNext(o);
					} else {
						String [] o = { authors.get(k).getUri(), normalisedName+ " ("+bdate+"-"+ddate+")",
							authors.get(k).getLastname(), authors.get(k).getFirstname(), commaSepValueBuilder.toString(),
							authors.get(k).getGender(), bdate, authors.get(k).getRef()};
						writer.writeNext(o);
					}
				}
				writer.close();
				System.out.println("exiting BNF: processResults");
			} catch (IOException e) {
				e.printStackTrace();
			}
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
	private String ref;
	private List<String> rejectedForms;

	public Author() {
		this.rejectedForms = new ArrayList<String>();
	}

	public String getFirstname() {
		return firstname;
	}

	public void setFirstname(String firstname) {
		this.firstname = firstname;
	}

	public String getLastname() {
		return lastname;
	}

	public void setLastname(String lastname) {
		this.lastname = lastname;
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

	public String getRef() {
		return ref;
	}

	public void setRef(String ref) {
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
	
	public String makeNormalisedName() {
		String normalisedName = "";
		if (this.getFirstname().equals("") || this.getFirstname().equals(" ")) {
			normalisedName = this.getLastname();
		} else {
			normalisedName = this.getLastname() + ", "+this.getFirstname();
		}
		normalisedName = normalisedName.replaceAll("'", "' ").replaceAll("  ", " ");
		return normalisedName;		
	}
	
	public String getLastNameInitials() {
		String initials = "";
		for (int i = 0; i < this.getLastname().length(); i++) {
			if(Character.isUpperCase(this.getLastname().charAt(i))){
				initials +=  this.getLastname().charAt(i);
			}
		}
		return initials;
	}
	
	public String getFirstNameInitials() {
		String initials = "";
		for (int i = 0; i < this.getFirstname().length(); i++) {
			if(Character.isUpperCase(this.getFirstname().charAt(i))){
				initials +=  this.getFirstname().charAt(i);
			}
		}
		return initials;
	}
	
	public String getTitle() {
		if (this.getGender().equalsIgnoreCase("female")) {
			return "Mme";
		} else {
			return "M";
		}	
	}
	
	public List<String> makeAliases() {
		//TODO code in python
		/*List<String> aliases = new ArrayList<String>();
		authors.get(k).getGender()		
		authors.get(k).getRejectedForms()*/
		return null;
	}

}
