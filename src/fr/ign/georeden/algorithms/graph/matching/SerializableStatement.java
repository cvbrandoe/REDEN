package fr.ign.georeden.algorithms.graph.matching;

import java.io.Serializable;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;

public class SerializableStatement implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String subject;
	private String predicate;
	private String object;
	
	public SerializableStatement(Statement s) {
		this.subject = s.getSubject().toString();
		this.predicate = s.getPredicate().toString();
		this.object = s.getObject().toString();
	}
	
	/**
     * To statement.
     *
     * @param graph the graph
     * @return the statement
     */
    public Statement toStatement(Model graph) {
		return graph.createStatement(graph.getResource(subject), graph.getProperty(predicate), graph.getResource(object));
	}
	public String getSubject() {
		return this.subject;
	}
	public String getPredicate() {
		return this.predicate;
	}
	public String getObject() {
		return this.object;
	}
}
