package fr.lip6.reden.ldextractor.per;

import java.util.ArrayList;
import java.util.List;

public class AuthorDBpediaFr {
	private String lastname;
	private String uri;
	private String birthdate;
	private String deathdate;
	private List<String> ref;
	private List<String> rejectedForms;
	
	public String getBirthdate() {
		return birthdate;
	}

	public void setBirthdate(String birthdate) {
		this.birthdate = birthdate;
	}

	public String getDeathdate() {
		return deathdate;
	}

	public void setDeathdate(String deathdate) {
		this.deathdate = deathdate;
	}
	
	public AuthorDBpediaFr() {
		this.rejectedForms = new ArrayList<String>();
		this.ref = new ArrayList<String>();
	}

	public String getLastname() {
		return lastname;
	}

	public void setLastname(String lastname) {
		this.lastname = lastname.replaceAll("-", " ").trim();
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
	
	public String getNormalisedName() {
		String normalisedName = this.getLastname().replaceAll("'", "' ");
		return normalisedName.replaceAll("  ", " ");		
	}
				
}
