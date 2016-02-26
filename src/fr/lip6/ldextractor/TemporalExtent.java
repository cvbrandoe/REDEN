package fr.lip6.ldextractor;

import java.util.Date;

/**
 * Class for defining temporal parameters.
 * @author Brando & Frontini
 */
public class TemporalExtent extends TopicExtent {
	
	/**
	 * A date must be lesser than this date 
	 */
	private Date lesserThan;
	
	/**
	 * A date must be greater than this date
	 */
	private Date greaterThan;
	
	public Date getLesserThan() {
		return lesserThan;
	}
	public void setLesserThan(Date lesserThan) {
		this.lesserThan = lesserThan;
	}
	public Date getGreaterThan() {
		return greaterThan;
	}
	public void setGreaterThan(Date greaterThan) {
		this.greaterThan = greaterThan;
	}
	
}
