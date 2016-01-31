package logic;

import java.io.Serializable;

import xml.client.Query;

public class Report implements Serializable 
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private xml.client.Query query;
	private String name;
	
	public xml.client.Query getQuery() {
		return query;
	}

	public void setQuery(xml.client.Query query) {
		this.query = query;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Report(Query query, String name) {
		super();
		this.query = query;
		this.name = name;
	}
}
