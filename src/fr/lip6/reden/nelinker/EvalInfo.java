package fr.lip6.reden.nelinker;

import java.util.ArrayList;
import java.util.List;

/**
 * Evaluation information per mention
 * 
 * @author @author Brando & Frontini
 */
public class EvalInfo {
	
	private String mention;
	private String manualURI;
	private List<List<String>> candUris = new ArrayList<List<String>>(0);	
	private String chosenUri;
	private Boolean choiceIsCorrect = false; 
	private Boolean correctURIisInCandSet = false;
	
	public Boolean getCorrectURIisInCandSet() {
		return correctURIisInCandSet;
	}
	public void setCorrectURIisInCandSet(Boolean correctURIisInCandSet) {
		this.correctURIisInCandSet = correctURIisInCandSet;
	}
	public Boolean getChoiceIsCorrect() {
		return choiceIsCorrect;
	}
	public void setChoiceIsCorrect(Boolean choiceIsCorrect) {
		this.choiceIsCorrect = choiceIsCorrect;
	}
	public String getMention() {
		return mention;
	}
	public void setMention(String mention) {
		this.mention = mention;
	}
	public String getManualURI() {
		return manualURI;
	}
	public void setManualURI(String manualURI) {
		this.manualURI = manualURI;
	}
	public List<List<String>> getCandUris() {
		return candUris;
	}
	public void setCandUris(List<List<String>> candUris) {
		this.candUris = candUris;
	}
	public String getChosenUri() {
		return chosenUri;
	}
	public void setChosenUri(String chosenUri) {
		this.chosenUri = chosenUri;
	}
	
}
