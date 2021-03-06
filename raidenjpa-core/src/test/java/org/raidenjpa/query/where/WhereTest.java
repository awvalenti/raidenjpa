package org.raidenjpa.query.where;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;
import org.raidenjpa.AbstractTestCase;
import org.raidenjpa.util.FixMe;
import org.raidenjpa.util.QueryHelper;

public class WhereTest extends AbstractTestCase {
	
	@Test
	public void testOneValue() {
		createABC();
		
		QueryHelper query;
		
		query = new QueryHelper("SELECT a FROM A a WHERE a.stringValue = :a");
		query.parameter("a", "a1");
		assertEquals(1, query.getResultList().size());
		
		query = new QueryHelper("SELECT a FROM A a WHERE a.stringValue = :a");
		query.parameter("a", "wrongValue");
		assertEquals(0, query.getResultList().size());
		
		query = new QueryHelper("SELECT a FROM A a WHERE a.intValue >= :intValue");
		query.parameter("intValue", 1);
		assertEquals(1, query.getResultList().size());
		
		query = new QueryHelper("SELECT a FROM A a WHERE a.intValue > :intValue");
		query.parameter("intValue", 1);
		assertEquals(0, query.getResultList().size());
		
		query = new QueryHelper("FROM A a WHERE a.intValue >= :um AND a.intValue <= :um");
		query.parameter("um", 1);
		assertEquals(1, query.getResultList().size());
	}

	@Test
	public void testIsNull() {
		createABC();
		
		QueryHelper query;
		query = new QueryHelper("SELECT a FROM A a WHERE a.stringValue is null");
		assertEquals(0, query.getResultList().size());
	}
	
	
	@Test
	public void testAnd() {
		createABC();
		
		QueryHelper query = new QueryHelper("SELECT a FROM A a WHERE a.stringValue = :stringValue AND a.intValue = :intValue");
		query.parameter("stringValue", "a1");
		query.parameter("intValue", 1);
		assertEquals(1, query.getResultList().size());
	}
	
	@Test
	public void testTwoFromComparingAttributes() {
		createABC();
		createB("b2");
	
		QueryHelper query = new QueryHelper("SELECT a, b FROM A a, B b WHERE a.stringValue = :valueA AND b.value = :valueB");
		query.parameter("valueA", "a1");
		query.parameter("valueB", "b2");
		assertEquals(1, query.getResultList().size());
	}
	
	@FixMe("Compare the entities. Make this test work with merge")
	@Test
	public void testTwoFromComparingObjects() {
		createABC();
		createB("b2");
	
		QueryHelper query = new QueryHelper("SELECT a, b FROM A a, B b WHERE a.b.id = b.id");
		assertEquals(1, query.getResultList().size());
	}

	@Test
	public void testInOperator() {
		createABC();
		createA("a2", 2);
		createA("a3", 3);
		createA("a4", 4);
		
		QueryHelper query = new QueryHelper("SELECT a FROM A a WHERE a.intValue IN (:values)");
		query.parameter("values", Arrays.asList(1, 3, 5));
		assertEquals(2, query.getResultList().size());
	}
	
	@FixMe("Check why in Hibernate the last jpql doesnt work")
	@Test
	public void testEntityComparation() {
		createABC();
		createA("a2");
		
		QueryHelper query = new QueryHelper("SELECT a1 FROM A a1, A a2 WHERE a1 = a2");
		assertEquals(2, query.getResultList().size());
		
		query = new QueryHelper("SELECT a1 FROM A a1, B b1 WHERE a1.b = b1");
		assertEquals(1, query.getResultList().size());
		
//		query = new QueryHelper("SELECT a1 FROM A a1, B b1 WHERE a1 = b1");
//		assertEquals(0, query.getResultList().size());
	}
	
	@Test
	public void testWhereLiteral() {
		createABC();
		String jpql;
		QueryHelper query;
		
		jpql = "SELECT a FROM A a";
		jpql += " WHERE a.intValue = 1";
		query = new QueryHelper(jpql);
		assertEquals(1, query.getResultList().size());
		
		jpql = "SELECT a FROM A a";
		jpql += " WHERE a.intValue = 0";
		query = new QueryHelper(jpql);
		assertEquals(0, query.getResultList().size());
	}
	
	@FixMe("Implement")
	@Test
	public void testEntityComparationByParameter() {
		
	}
	
	@FixMe("% in the begin")
	@Test
	public void testLike() {
		createA("a1");
		createA("a1");
		createA("a2");
		
		String jpql;
		QueryHelper query;
		
		jpql = "SELECT a FROM A a";
		jpql += " WHERE a.stringValue LIKE :a";
		query = new QueryHelper(jpql);
		query.parameter("a", "a1");
		assertEquals(2, query.getResultList().size());
		
		jpql = "SELECT a FROM A a";
		jpql += " WHERE a.stringValue LIKE :a";
		query = new QueryHelper(jpql);
		query.parameter("a", "a");
		assertEquals(0, query.getResultList().size());
		
		jpql = "SELECT a FROM A a";
		jpql += " WHERE a.stringValue LIKE :a";
		query = new QueryHelper(jpql);
		query.parameter("a", "a%");
		assertEquals(3, query.getResultList().size());
	}
}
