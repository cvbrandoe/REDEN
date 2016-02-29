package fr.lip6.reden.extra;

import java.io.File;
import java.io.FileInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.output.FileWriterWithEncoding;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Significance testing by Friedman test, data preparation, test is performed using R.
 * Attention: pas possible de comparer les centrality measures avant normalisation!!!
 *
 */
public class SignificanceTesting {

	public static void main(String[] args) {
		//thibaudet chapter degree
		//thibaudet chapter eigenvector (justify, adgitis utilise celle là, nous on a testé, et c pareil que degree (l'hypothèse!)
		
		//thibaudet par degree
		//thibaudet par eigenvector (justify, adgitis utilise celle là, nous on a testé, et c pareil que degree (l'hypothèse!)
						
		//thibaudet whole degree
		//thibaudet whole eigenvector (justify, adgitis utilise celle là, nous on a testé, et c pareil que degree (l'hypothèse!)
				
		String[] fileThibaudet = {"output/thibaudet_reflexions-outV3-chapter-degree.xml", "output/thibaudet_reflexions-outV3-chapter-eigenvector.xml"};
		prepareData(fileThibaudet, 3000, "output/freidman-thibaudet-chapter.txt");
		//berson whole texte degree
		//bergson whole text eigenvector
		String[] fileBergson = {"output/bergson_evolutionV2-outV3-whole-eigenvector.xml", "output/bergson_evolutionV2-outV3-whole-degree.xml"};
		prepareData(fileBergson, 500, "output/freidman-bergson-whole.txt");	
		
		/**
		 * Code R
		 # Test entre deux groupes, eigenvector et degree, de données appariées 
		# Paramètre "chapter"
		data <- read.csv2("C:/Users/cbrando/Documents/REDEN/git/REDEN/output/freidman-thibaudet-chapter.csv", sep = "\t", row.names = NULL)
		data$eigenvector2 <- as.numeric(as.character(data$eigenvector))
		data$degree2 <- as.numeric(as.character(data$degree))
		data2 <- data[,c("eigenvector2","degree2")]
		t.test(data2[,1], data2[,2], paired=TRUE)
		summary(data2)
		 */
	}
	
	public static void prepareData(String[] files, int size, String outname) {
				
		Double[][] results = new Double[size][2];		
		try {
			int index = 0;
			for (String fileName : files) {
				int nmention = 0;
				DocumentBuilder b = DocumentBuilderFactory.newInstance()
						.newDocumentBuilder();
				org.w3c.dom.Document doc = b.parse(new FileInputStream(new File(fileName)));
				XPath xPath = XPathFactory.newInstance().newXPath();
				NodeList nodes = (NodeList) xPath.evaluate("//body/div", doc.getDocumentElement(), XPathConstants.NODESET);
				
				for (int i = 0; i < nodes.getLength(); ++i) {
					
					Element e = (Element) nodes.item(i);					
					NodeList nodesChild = (NodeList) xPath.evaluate(".//"
								+ "persName[not(@type='character')]", e, XPathConstants.NODESET);
					
					for (int k = 0; k < nodesChild.getLength(); ++k) {
						Element child = (Element) nodesChild.item(k);
						String ref = child.getAttribute("ref_auto");
						//System.out.println("ref: "+ ref);
						if (ref != null && !ref.contains("null")) {
							Pattern pattern = Pattern.compile(".*(\\(.+?\\)).*");
							Matcher matcher = pattern.matcher(ref);
							boolean found = matcher.matches();
						    if (found) {
						    	String v = matcher.group(1).replace("(", "").replace(")", "");
						    	//System.out.println("v: "+v);
						    	Double val = Double.parseDouble(v);
						    	results[nmention][index] = val;
						    } else {
						    	results[nmention][index] = 0.0;
						    }					
						} else {
							results[nmention][index] = 0.0;
						}
						nmention++;
					}					
				}
				index ++;
			}
			
			FileWriterWithEncoding fileT = new FileWriterWithEncoding(outname, "UTF-8");
			fileT.write("degree\teigenvector\n");
			for (Double[] x : results) {				
				for (Double y : x) {
					fileT.write(y + "\t"); //enlever tab à la fin de la ligne
				}
				fileT.write("\n");
			}
			fileT.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
}
