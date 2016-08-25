package fr.ign.georeden.algorithms.string;


public class Mapping {
	
	private String strSrc;
	private String strCbl;
	private double score;
	
	//scores intermediaires
	private double scoreNoms;
	private double scoreGeoloc;
	private double scoreImages;
	//noms des objets
	private String nom1;
	private String nom2;
	
	public Mapping(String strSrc, String strCbl) {
		// TODO Auto-generated constructor stub
		this.strSrc=strSrc;
		this.strCbl = strCbl;
		this.score=-1.0;
	}
	
	public Mapping(String strSrc, String strCbl, double score) {
		// TODO Auto-generated constructor stub
		this.strSrc=strSrc;
		this.strCbl = strCbl;
		this.score=score;
	}
	
	
	public String getStrSrc() {
		return strSrc;
	}

	public void setStrSrc(String strSrc) {
		this.strSrc = strSrc;
	}

	public String getStrCbl() {
		return strCbl;
	}

	public void setStrCbl(String strCbl) {
		this.strCbl = strCbl;
	}

	public Double getScore() {
		return score;
	}

	public void setScore(Double score) {
		this.score = score;
	}


	public double getScoreNoms() {
		return scoreNoms;
	}


	public void setScoreNoms(double scoreNoms) {
		this.scoreNoms = scoreNoms;
	}


	public double getScoreGeoloc() {
		return scoreGeoloc;
	}


	public void setScoreGeoloc(double scoreGeoloc) {
		this.scoreGeoloc = scoreGeoloc;
	}


	public double getScoreImages() {
		return scoreImages;
	}


	public void setScoreImages(double scoreImages) {
		this.scoreImages = scoreImages;
	}


	public String getNom1() {
		return nom1;
	}


	public void setNom1(String nom1) {
		this.nom1 = nom1;
	}


	public String getNom2() {
		return nom2;
	}


	public void setNom2(String nom2) {
		this.nom2 = nom2;
	}
	

}
