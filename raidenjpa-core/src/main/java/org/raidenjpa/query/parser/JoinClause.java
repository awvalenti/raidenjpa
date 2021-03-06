package org.raidenjpa.query.parser;

import java.util.ArrayList;
import java.util.List;

public class JoinClause {

	private List<String> path = new ArrayList<String>();
	
	private String alias;

	private WithClause with;
	
	private boolean isFetch;

	public void parse(QueryWords words) {
		if ("INNER".equalsIgnoreCase(words.current())
				|| "LEFT".equalsIgnoreCase(words.current())
				|| "RIGHT".equalsIgnoreCase(words.current())) {
			words.next();
		}
		
		words.require("JOIN");
		words.next();
		
		if ("FETCH".equalsIgnoreCase(words.current())) {
			isFetch = true;
			words.next();
		}
		
		path = words.getAsPath();
		
		if (!isFetch) {
			alias = words.next();
			with = new WithClause();
			with.parse(words);
		}
	}

	public List<String> getPath() {
		return path;
	}

	public String getAlias() {
		return alias;
	}

	public WithClause getWith() {
		return with;
	}
	
	public boolean isFetch() {
		return isFetch;
	}

	public void setPath(List<String> path) {
		this.path = path;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public void setWith(WithClause with) {
		this.with = with;
	}

}
