package fr.lip6.reden.ldextractor.per;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
* Utility class for handling authors.
* @author Brando & Frontini
*
*/
public class AuthorBNF {
	private String firstname;
	private String lastname;
	private String gender;
	private String uri;
	private String birthdate;
	private String deathdate;
	private List<String> ref;
	private List<String> rejectedForms;
	private static String[] hons = {"de","d'","von","da"}; 

	public AuthorBNF() {
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
