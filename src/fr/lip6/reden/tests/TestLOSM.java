package fr.lip6.reden.tests;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.sparql.engine.http.QueryEngineHTTP;

class TestLOSM {

     public static void main(String [] args) {

         String service = "http://sisinflab.poliba.it/semanticweb/lod/losm/sparql";
         String queryString = "PREFIX  rdfs: <http://www.w3.org/2000/01/rdf-schema#>"
         		//+ " PREFIX  lgdo: <http://linkedgeodata.org/ontology/>"
         		//+ " PREFIX  spatial: <http://jena.apache.org/spatial#> "
         		//+ " PREFIX  losm: <http://sisinflab.poliba.it/semanticweb/lod/losm/ontology/> "
         		+ " SELECT  ?spatial ?pref "
         		+ " WHERE "
         		+ "  { ?spatial rdfs:label ?pref } LIMIT   300";
          		                  
         System.out.println(queryString);
         QueryExecution vqe = new QueryEngineHTTP(service, queryString);

         ResultSet results = vqe.execSelect();
         while (results.hasNext()) {
             System.out.println(results.next());
         }
         vqe.close();
         //it does not work
     }
}