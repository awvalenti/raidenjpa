package org.raidenjpa.query.executor;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.raidenjpa.util.BadSmell;

public class PoolerRows {

	@BadSmell("This first if is really weird")
	public Collection<QueryResultRow> group(List<QueryResultRow> rows, List<List<String>> paths) {
		if (rows.isEmpty() && paths.size() > 0 && paths.get(0).get(0).equals("fake_aggregation_for_group_all_rows")) {
			return Arrays.asList(new QueryResultRow("", null));
		}
		
		Map<String, QueryResultRow> map = new HashMap<String, QueryResultRow>(); 
		
		for (QueryResultRow row : rows) {
			String key = toKey(row, paths);
			QueryResultRow groupedRow = map.get(key);
			if (groupedRow == null) {
				groupedRow = row;
				map.put(key, groupedRow);
			}
			groupedRow.addGroupedRow(row);
		}
		
		return map.values();
	}

	private String toKey(QueryResultRow row, List<List<String>> paths) {
		String key = "";
		for (List<String> path : paths) {
			key += ";" + toStringPath(path) + "=" + row.getObject(path);
		}
		return key;
	}

	@BadSmell("Probably it would be better have a Path class")
	private String toStringPath(List<String> path) {
		String result = "";
		for (String p : path) {
			result += p + ".";
		}
		return result;
	}
	
}
