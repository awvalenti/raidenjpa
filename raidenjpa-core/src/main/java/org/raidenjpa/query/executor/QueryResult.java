package org.raidenjpa.query.executor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.raidenjpa.query.parser.GroupByClause;
import org.raidenjpa.query.parser.GroupByElements;
import org.raidenjpa.query.parser.JoinClause;
import org.raidenjpa.query.parser.OrderByClause;
import org.raidenjpa.query.parser.OrderByElement;
import org.raidenjpa.query.parser.SelectClause;
import org.raidenjpa.query.parser.SelectElement;
import org.raidenjpa.query.parser.WhereClause;
import org.raidenjpa.util.BadSmell;
import org.raidenjpa.util.FixMe;
import org.raidenjpa.util.ReflectionUtil;

public class QueryResult implements Iterable<QueryResultRow> {

	private List<QueryResultRow> rows = new ArrayList<QueryResultRow>();
	
	public QueryResult addFrom(String alias, List<?> newElements) {
		if (rows.isEmpty()) {
			firstFrom(alias, newElements);
		} else {
			cartesianProduct(alias, newElements);
		}
		
		return this;
	}

	public void cartesianProduct(String alias, List<?> newElements) {
		if (newElements.isEmpty()) {
			return;
		}
		
		for (QueryResultRow row : new ArrayList<QueryResultRow>(rows)) {
			row.column(alias, newElements.get(0));
			
			for (int i = 1; i < newElements.size(); i++) {
				QueryResultRow duplicatedRow = duplicate(row);
				duplicatedRow.column(alias, newElements.get(i));
				row = duplicatedRow;
			}
		}
	}

	private QueryResultRow duplicate(QueryResultRow row) {
		int index = rows.indexOf(row);
		QueryResultRow duplicatedRow = row.copy();
		rows.add(index + 1, duplicatedRow);
		return duplicatedRow;
	}

	private void firstFrom(String alias, List<?> objRows) {
		for (Object obj : objRows) {
			rows.add(new QueryResultRow(alias, obj));
		}
	}

	public Iterator<QueryResultRow> iterator() {
		return rows.iterator();
	}

	public void limit(Integer maxResult) {
		if (maxResult == null || maxResult >= rows.size()) {
			return;
		}
		
		rows = rows.subList(0, maxResult);
	}

	@BadSmell("This double verification in groupBy is only necessary because of bad design")
	public List<?> getList(SelectClause select, GroupByClause groupBy) {
		if (isThereAggregationFunction(select)) {
			return selectUsingAggregation(select, groupBy);
		} else {
			if (select.getElements().size() == 1) {
				return selectOneElement(select);
			} else {
				return selectMoreThanOneElement(select);
			}
		}
	}

	private boolean isThereAggregationFunction(SelectClause select) {
		for (SelectElement element : select.getElements()) {
			if ("count(*)".equalsIgnoreCase(element.getPath().get(0))) {
				return true;
			}
		}
		return false;
	}

	private List<?> selectUsingAggregation(SelectClause select, GroupByClause groupBy) {
		if (groupBy == null) {
			return Arrays.asList(new Long(rows.size()));
		}

		Map<String, List<QueryResultRow>> aggregateRows = aggregateRowsOld(groupBy);
		if (select.getElements().size() == 1) {
			List<Long> result = new ArrayList<Long>();
			for (Entry<String, List<QueryResultRow>> entry : aggregateRows.entrySet()) {
				result.add(new Long(entry.getValue().size()));
			}
			return result;
		}
		
		List<Object[]> result = new ArrayList<Object[]>();
		for (Entry<String, List<QueryResultRow>> entry : aggregateRows.entrySet()) {
			Object[] resultRow = new Object[select.getElements().size()];
			for (int i = 0; i < select.getElements().size(); i++) {
				SelectElement selectElement = select.getElements().get(i);
				
				if ("count(*)".equalsIgnoreCase(selectElement.getPath().get(0))) {
					resultRow[i] = new Long(entry.getValue().size()); 
				} else {
					resultRow[i] = entry.getValue().get(0).get(selectElement);
				}
			}
			result.add(resultRow);
		}
		
		return result;
	}

	@BadSmell("Primitive obsession")
	public Map<String, List<QueryResultRow>> aggregateRowsOld(GroupByClause groupBy) {
		Map<String, List<QueryResultRow>> map = new HashMap<String, List<QueryResultRow>>();
		
		for (QueryResultRow row : rows) {
			String key = "";
			for (GroupByElements element : groupBy.getElements()) {
				key += ";" + element.getPath() + "=" + row.getObject(element.getPath());
			}
			
			List<QueryResultRow> aggregatedRows = map.get(key);
			if (aggregatedRows == null) {
				aggregatedRows = new ArrayList<QueryResultRow>();
			}
			aggregatedRows.add(row);
			map.put(key, aggregatedRows);
		}
		return map;
	}
	
	public void aggregateRows(GroupByClause groupBy) {
		Map<String, GroupedRows> map = new HashMap<String, GroupedRows>();
		
		/*for (QueryResultRow row : rows) {
			String key = "";
			for (GroupByElements element : groupBy.getElements()) {
				key += ";" + element.getPath() + "=" + row.getObject(element.getPath());
			}
			
			List<QueryResultRow> aggregatedRows = map.get(key);
			if (aggregatedRows == null) {
				aggregatedRows = new ArrayList<QueryResultRow>();
				groupedRows.add()
			}
			aggregatedRows.add(row);
			map.put(key, aggregatedRows);
		}*/
	}

	private List<?> selectMoreThanOneElement(SelectClause select) {
		List<Object[]> result = new ArrayList<Object[]>();
		for (QueryResultRow row : rows) {
			Object[] obj = new Object[select.getElements().size()];
			int index = 0;
			for (SelectElement element : select.getElements()) {
				obj[index++] = row.get(element);
			}
			
			result.add(obj);
		}
		
		applyDistinct(select, result);
		
		return result;
	}

	private void applyDistinct(SelectClause select, List<Object[]> result) {
		if (select.isDistinct()) {
			Set<String> distinct = new HashSet<String>();
			for (Object[] row : new ArrayList<Object[]>(result)) {
				String idf = "";
				for (Object column : row) {
					idf += column + "-";
				}
				if (!distinct.add(idf)) {
					result.remove(row);
				}
			}
		}
	}

	@BadSmell("Duplicated code? One element could be a especific case of more than one element")
	private List<?> selectOneElement(SelectClause select) {
		SelectElement selectElement = select.getElements().get(0);
		Collection<Object> result;
		
		if(select.isDistinct()) {
			result = new HashSet<Object>();
		} else {
			result = new ArrayList<Object>();
		}
		
		for (QueryResultRow row : rows) {
			Object element = row.get(selectElement);
			result.add(element);
		}
		
		return new ArrayList<Object>(result);
	}
	
	public int size() {
		return rows.size();
	}

	@BadSmell("It is weird to recive where")
	void join(JoinClause join, WhereClause where, Map<String, Object> parameters) {
		String leftAlias = join.getPath().get(0);
		String attribute = join.getPath().get(1);
		
		for (QueryResultRow row : new ArrayList<QueryResultRow>(rows)) {
			Object leftObject = row.get(leftAlias);
			
			Object obj = ReflectionUtil.getBeanField(leftObject, attribute);
			if (obj instanceof Collection) {
				joinCollection(join, where, row, (Collection<?>) obj, parameters);
			} else {
				joinObject(join, row, obj, parameters);
			}
		}
	}

	@FixMe("Receive where, like joinCollection")
	private void joinObject(JoinClause join, QueryResultRow row, Object obj, Map<String, Object> parameters) {
		if (obj == null) {
			rows.remove(row); // TODO: Beware about LEFT
		} else {
			row.column(join.getAlias(), obj);
			removeRowsNoMatchWith(join, parameters, Arrays.asList(row));
		}
	}

	@BadSmell("We could avoid some parameters making this attributes")
	private void joinCollection(JoinClause join, WhereClause where, QueryResultRow row, Collection<?> itensToAdd, Map<String, Object> parameters) {
		Iterator<?> it = itensToAdd.iterator();
		
		if (!it.hasNext()) {
			rows.remove(row); // TODO: Beware about LEFT
			return;
		}
		
		List<QueryResultRow> rowsInJoin = new ArrayList<QueryResultRow>();
		rowsInJoin.add(row);
		
		row.column(join.getAlias(), it.next());
		
		while(it.hasNext()) {
			Object itemToAdd = it.next();
			QueryResultRow newRow = duplicate(row);
			newRow.column(join.getAlias(), itemToAdd);
			rowsInJoin.add(newRow);
			row = newRow;
		}
		
		// @FixMe(We should not add)
		removeRowsNoMatchWith(join, parameters, rowsInJoin);
		removeRowsNoMatchWhere(where, parameters, rowsInJoin);
	}

	@BadSmell("Duplicate removeRowsNoMatchWith")
	private void removeRowsNoMatchWhere(WhereClause where, Map<String, Object> parameters, List<QueryResultRow> rowsInJoin) {
		if (where == null || where.getLogicExpression() == null) {
			return;
		}
		
		LogicExpressionExecutor executor = new LogicExpressionExecutor(where.getLogicExpression(), parameters);
		for (QueryResultRow rowInJoin : rowsInJoin) {
			if (executor.match(rowInJoin, false)) {
			} else {
				rows.remove(rowInJoin);
			}
		}
	}

	private void removeRowsNoMatchWith(JoinClause join, Map<String, Object> parameters, List<QueryResultRow> rowsInJoin) {
		if (join.getWith().getLogicExpression() == null) {
			return;
		}
		
		LogicExpressionExecutor executor = new LogicExpressionExecutor(join.getWith().getLogicExpression(), parameters);
		for (QueryResultRow rowInJoin : rowsInJoin) {
			if (executor.match(rowInJoin, false)) {
			} else {
				rows.remove(rowInJoin);
			}
		}
	}

	public void sort(final OrderByClause orderBy) {
		Collections.sort(rows, new Comparator<QueryResultRow>() {
			@SuppressWarnings({ "unchecked" })
			public int compare(QueryResultRow row1, QueryResultRow row2) {
				for (OrderByElement orderByElement : orderBy.getElements()) {
					Comparable<Object> value1 = (Comparable<Object>) row1.getObject(orderByElement.getPath());
					Comparable<Object> value2 = (Comparable<Object>) row2.getObject(orderByElement.getPath());
					
					if (value1 == null && value2 == null) {
						continue;
					}
					
					if (value1 == null && value2 != null) {
						return value2.compareTo(value1);
					}
					
					if (value1 != null && value2 == null) {
						return value1.compareTo(value2);
					}
					
					if (value1.equals(value2)) {
						continue;
					}
					
					if (orderByElement.getOrientation().equals("ASC")) {
						return value1.compareTo(value2);
					} else {
						return value2.compareTo(value1);
					}
				}
				
				return 0;
			}
		});
	}
}
