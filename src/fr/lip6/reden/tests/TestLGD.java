package fr.lip6.reden.tests;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.sparql.engine.http.QueryEngineHTTP;

class TestLGD {

     public static void main(String [] args) {

         String service = "http://linkedgeodata.org/sparql";
         /*String queryString = "Prefix lgdo: <http://linkedgeodata.org/ontology/> "
         		+ "select ?id ?d "
         		+ "From <http://linkedgeodata.org> "
         		+ "where { ?id rdfs:label ?d} limit 10"; OK
         */
         /*String queryString = " Prefix geom: <http://geovocab.org/geometry#>" 
        		 + " Prefix lgdo: <http://linkedgeodata.org/ontology/>"         		 
        		 + " select ?id ?geo "
        		 + "From <http://linkedgeodata.org> "
        		                  + "where { "
         		                  + "?id a lgdo:Place . "
        		                  + "?id geom:geometry ?geo. "        		             
        		                  + "} limit 20"; OK */
         String queryString = " Prefix geom: <http://geovocab.org/geometry#>" 
        		 + " Prefix lgdo: <http://linkedgeodata.org/ontology/>"       
        		 + " Prefix ogc: <http://www.opengis.net/ont/geosparql#>"
        		// + " PREFIX bif: <http://www.openlinksw.com/schemas/bif#>"
        		 + " select ?id ?geo "
        		 + "From <http://linkedgeodata.org> "
        		                  + "where { "
         		                  + "?id a lgdo:Place . "
        		                  + "?id geom:geometry ?node . "
         		                  + "?node ogc:asWKT ?geo ."
         		                  + "Filter(bif:st_intersects (?geo, bif:st_point (3.692764, 43.393794), 1)) . "
        		                  + "} limit 20"; //OK
          		                  
         System.out.println(queryString);
         QueryExecution vqe = new QueryEngineHTTP(service, queryString);

         ResultSet results = vqe.execSelect();
         while (results.hasNext()) {
             System.out.println(results.next());


         }
     }
}