package fr.ign.georeden.nelinker.tei;

import net.sf.saxon.s9api.QName;

public class TEIConst {

	public static final String LEMMA = "lemma";
	public static final String XML_ID = "id";//"xml:id";
	public static final String XML_ID_WITH_COLON = "xml:id";
	public static final String FORCE = "force";
	public static final String ORIENTATION = "orientation";
	public static final String SUBTYPE = "subtype";
	public static final String TYPE = "type";
	public static final String STRONG = "strong";
	public static final String INTER = "inter";
	public static final String PC = "pc";
	
	public static final String TEI_NS = "http://www.tei-c.org/ns/1.0";
	public static final String TEI_ROOT = "TEI";
	
	public static final String XML_NS = "http://www.w3.org/XML/1998/namespace";
	
	public static final QName XML_ID_QNAME = new QName(TEIConst.XML_NS, TEIConst.XML_ID);

	private TEIConst() {
	}

}
