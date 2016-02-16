package fr.lip6.reden;

import java.util.ArrayList;
import java.util.List;

/**
 * Evaluation information per mention
 * 
 * @author @author Brando & Frontini - Labex OBVIL - Université Paris-Sorbonne - UPMC
 *         LIP6
 */
public class EvalInfo {
	
	private String mention;
	private String manualURI;
	private List<List<String>> candUris = new ArrayList<List<String>>(0);	
	private String chosenUri;
	private Boolean choiceIsCorrect = false; 
	
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
