package org.bndly.schema.impl.nquery;

/*-
 * #%L
 * Schema Impl
 * %%
 * Copyright (C) 2013 - 2020 Cybercon GmbH
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import org.bndly.schema.api.nquery.QueryParsingException;
import org.bndly.schema.api.nquery.BooleanOperator;
import org.bndly.schema.api.nquery.Expression;
import org.bndly.schema.api.nquery.Query;
import org.bndly.schema.api.nquery.Pick;
import org.bndly.schema.api.nquery.BooleanStatement;
import org.bndly.schema.api.nquery.Count;
import org.bndly.schema.api.nquery.IfClause;
import org.bndly.schema.api.nquery.Ordering;
import org.bndly.schema.api.nquery.ReservedKeywords;
import org.bndly.schema.api.nquery.WrapperBooleanStatement;
import org.bndly.schema.impl.nquery.expression.DelegatingExpressionStatementHandler;
import org.bndly.schema.impl.nquery.expression.EqualExpressionStatementHandler;
import org.bndly.schema.impl.nquery.expression.GreaterEqualExpressionStatementHandler;
import org.bndly.schema.impl.nquery.expression.GreaterExpressionStatementHandler;
import org.bndly.schema.impl.nquery.expression.InRangeExpressionStatementHandler;
import org.bndly.schema.impl.nquery.expression.LowerEqualExpressionStatementHandler;
import org.bndly.schema.impl.nquery.expression.LowerExpressionStatementHandler;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class QueryParsingTest {
	
	@Test
	public void testSimplePick() throws QueryParsingException {
		String queryString = "PICK Product";
		Query query = createParser().parse(queryString).getQuery();
		Assert.assertNotNull(query);
		Assert.assertTrue(Pick.class.isInstance(query));
		Pick p = (Pick) query;
		Assert.assertEquals(p.getAttributeHolderName(), "Product");
	}
	
	@Test
	public void testSimplePickWithLimit() throws QueryParsingException {
		String queryString = "PICK PurchaseOrder LIMIT ?";
		Query query = createParser(1L).parse(queryString).getQuery();
		Assert.assertNotNull(query);
		Assert.assertTrue(Pick.class.isInstance(query));
		Pick p = (Pick) query;
		Assert.assertEquals(p.getAttributeHolderName(), "PurchaseOrder");
		Assert.assertEquals((long)p.getLimit(), 1L);
	}
	
	@Test
	public void testIfStatementWithNestedAttributeAndOrderByAndLimi() throws QueryParsingException {
		String queryString = "PICK Manufacturer IF address.firstName=? ORDERBY address.firstName LIMIT ?";
		Query query = createParser("Georg", 10L).parse(queryString).getQuery();
		Assert.assertNotNull(query);
		Assert.assertTrue(Pick.class.isInstance(query));
		Pick p = (Pick) query;
		Assert.assertEquals(p.getAttributeHolderName(), "Manufacturer");
		IfClause clause = p.getIfClause();
		Assert.assertNotNull(clause);
		BooleanStatement statement = clause.getNext();
		Assert.assertNotNull(statement);
		Assert.assertNull(statement.getNext());
		Ordering ordering = p.getOrdering();
		Assert.assertNotNull(ordering);
		Assert.assertEquals(ordering.getField(), "address.firstName");
		Long limit = p.getLimit();
		Assert.assertNotNull(limit);
	}
	
	@Test
	public void testOrderByDescWithLimit() throws QueryParsingException {
		String queryString = "PICK JobContextMap j ORDERBY j.createdOn DESC LIMIT ?";
		Query query = createParser(1).parse(queryString).getQuery();
		Assert.assertNotNull(query);
		Assert.assertTrue(Pick.class.isInstance(query));
		Pick p = (Pick) query;
		Assert.assertEquals(p.getAttributeHolderName(), "JobContextMap");
		Ordering ordering = p.getOrdering();
		Assert.assertEquals(ordering.getField(), "j.createdOn");
		Assert.assertEquals(p.getLimit(), Long.valueOf(1));
		
	}
	
	@Test
	public void testInRange() throws QueryParsingException {
		String queryString = "PICK Foo f IF f.bar INRANGE ?,?";
		Query query = createParser(0,1).parse(queryString).getQuery();
		Assert.assertNotNull(query);
		Assert.assertTrue(Pick.class.isInstance(query));
		Pick p = (Pick) query;
		Assert.assertEquals(p.getAttributeHolderName(), "Foo");
		IfClause ifClause = p.getIfClause();
		BooleanStatement tmp = ifClause.getNext();
		InRangeExpression inRange = (InRangeExpression) tmp;
		Assert.assertEquals(inRange.getLowerBorder().getArg(), 0);
		Assert.assertEquals(inRange.getUpperBorder().getArg(), 1);
	}
	
	@Test
	public void testOrderByAfterIfClause() throws QueryParsingException {
		String queryString = "PICK Node n IF n.parent.id=? ORDERBY n.parentIndex";
		Query query = createParser(1L).parse(queryString).getQuery();
		Assert.assertNotNull(query);
		Assert.assertTrue(Pick.class.isInstance(query));
		Pick p = (Pick) query;
		Assert.assertEquals(p.getAttributeHolderName(), "Node");
		Ordering ordering = p.getOrdering();
		Assert.assertEquals(ordering.getField(), "n.parentIndex");
	}

	@Test
	public void testSimplePickFail() {
		try {
			createParser().parse("PICK ");
			Assert.fail("expected a QueryParsingException");
		} catch(QueryParsingException e) {
			// expected
		}
		try {
			createParser().parse("PICK");
			Assert.fail("expected a QueryParsingException");
		} catch(QueryParsingException e) {
			// expected
		}
	}
	
	@Test
	public void testSimplePickAlias() throws QueryParsingException {
		String queryString = "PICK Product p";
		Query query = createParser().parse(queryString).getQuery();
		Assert.assertNotNull(query);
		Assert.assertTrue(Pick.class.isInstance(query));
		Pick p = (Pick) query;
		Assert.assertEquals(p.getAttributeHolderName(), "Product");
		Assert.assertEquals(p.getAttributeHolderNameAlias(), "p");
	}
	
	@Test
	public void testIncompleteIfClause() throws QueryParsingException {
		try {
			createParser().parse("PICK Product p IF");
			Assert.fail("expected a QueryParsingException");
		} catch(QueryParsingException e) {
			// expected
		}
	}
	
	@Test
	public void testIfClause() throws QueryParsingException {
		String queryString = "PICK Product p IF p.sku=?";
		Query query = createParser("4711").parse(queryString).getQuery();
		Assert.assertNotNull(query);
		Assert.assertTrue(Pick.class.isInstance(query));
		Pick p = (Pick) query;
		Assert.assertNotNull(p.getIfClause());
		BooleanStatement st = p.getIfClause().getNext();
		Assert.assertTrue(Expression.class.isInstance(st));
		Expression e = (Expression) st;
		Assert.assertTrue(ComparisonExpression.class.isInstance(e));
		ComparisonExpression ee = (ComparisonExpression) e;
		ContextVariable left = ee.getLeft();
		ContextVariable right = ee.getRight();
		Assert.assertFalse(ee.isNegated());
		
		Assert.assertFalse(left.isArg());
		Assert.assertEquals(left.getArg(), null);
		Assert.assertEquals(left.getName(), "p.sku");
		
		Assert.assertTrue(right.isArg());
		Assert.assertEquals(right.getArg(), "4711");
		Assert.assertEquals(right.getName(), ReservedKeywords.PARAM_WILDCARD);
	}
	
	@Test
	public void testIfClauseWithNegation() throws QueryParsingException {
		String queryString = "PICK Product p IF p.sku!=?";
		Query query = createParser("4711").parse(queryString).getQuery();
		Assert.assertNotNull(query);
		Assert.assertTrue(Pick.class.isInstance(query));
		Pick p = (Pick) query;
		Assert.assertNotNull(p.getIfClause());
		BooleanStatement st = p.getIfClause().getNext();
		Assert.assertTrue(Expression.class.isInstance(st));
		Expression e = (Expression) st;
		Assert.assertTrue(ComparisonExpression.class.isInstance(e));
		ComparisonExpression ee = (ComparisonExpression) e;
		ContextVariable left = ee.getLeft();
		ContextVariable right = ee.getRight();
		Assert.assertTrue(ee.isNegated());
		
		Assert.assertFalse(left.isArg());
		Assert.assertEquals(left.getArg(), null);
		Assert.assertEquals(left.getName(), "p.sku");
		
		Assert.assertTrue(right.isArg());
		Assert.assertEquals(right.getArg(), "4711");
		Assert.assertEquals(right.getName(), ReservedKeywords.PARAM_WILDCARD);
	}
	
	@Test
	public void testIfClauseWithNegation2() throws QueryParsingException {
		String queryString = "PICK Product p IF p.sku!>?";
		Query query = createParser("4711").parse(queryString).getQuery();
		Assert.assertNotNull(query);
		Assert.assertTrue(Pick.class.isInstance(query));
		Pick p = (Pick) query;
		Assert.assertNotNull(p.getIfClause());
		BooleanStatement st = p.getIfClause().getNext();
		Assert.assertTrue(Expression.class.isInstance(st));
		Expression e = (Expression) st;
		Assert.assertTrue(ComparisonExpression.class.isInstance(e));
		ComparisonExpression ee = (ComparisonExpression) e;
		ContextVariable left = ee.getLeft();
		ContextVariable right = ee.getRight();
		Assert.assertTrue(ee.isNegated());
		
		Assert.assertEquals(ee.getComparisonType(), ComparisonExpression.Type.GREATER);
		
		Assert.assertFalse(left.isArg());
		Assert.assertEquals(left.getArg(), null);
		Assert.assertEquals(left.getName(), "p.sku");
		
		Assert.assertTrue(right.isArg());
		Assert.assertEquals(right.getArg(), "4711");
		Assert.assertEquals(right.getName(), ReservedKeywords.PARAM_WILDCARD);
	}
	
	@Test
	public void testWrappedIfClause() throws QueryParsingException {
		String queryString = "PICK Product p IF (p.sku=?)";
		Query query = createParser("4711").parse(queryString).getQuery();
		Assert.assertNotNull(query);
		Assert.assertTrue(Pick.class.isInstance(query));
		Pick p = (Pick) query;
		Assert.assertNotNull(p.getIfClause());
	}
	
	@Test
	public void testIfClauseWithAND() throws QueryParsingException {
		String queryString = "PICK Product p IF p.sku=? AND p.currency=?";
		Query query = createParser("4711", "EUR").parse(queryString).getQuery();
		Assert.assertNotNull(query);
		Assert.assertTrue(Pick.class.isInstance(query));
		Pick p = (Pick) query;
		Assert.assertNotNull(p.getIfClause());
	}
	
	@Test
	public void testIfClauseWithORAndWrapper() throws QueryParsingException {
		String queryString = "PICK Product p IF p.sku=? AND (p.currency=? OR p.foo=?)";
		Query query = createParser("4711", "EUR", "bar").parse(queryString).getQuery();
		Assert.assertNotNull(query);
		Assert.assertTrue(Pick.class.isInstance(query));
		Pick p = (Pick) query;
		IfClause ifc = p.getIfClause();
		Assert.assertNotNull(ifc);
		BooleanStatement bs = ifc.getNext();
		Expression exp = assertIsExpression(bs);
		Assert.assertEquals(exp.getStatement(), "p.sku=?");
		Assert.assertEquals(exp.getNextOperator(), BooleanOperator.AND);
		WrapperBooleanStatement wrapper = assertIsWrapper(exp.getNext());
		Expression exp2 = assertIsExpression(wrapper.getWrapped());
		Assert.assertEquals(exp2.getNextOperator(), BooleanOperator.OR);
		Expression exp3 = assertIsExpression(exp2.getNext());
		Assert.assertNull(exp3.getNext());
		Assert.assertNull(exp3.getNextOperator());
		Assert.assertNull(wrapper.getNext());
		Assert.assertNull(wrapper.getNextOperator());
	}
	
	@Test
	public void testIfClauseWithORAndDoubleWrapper() throws QueryParsingException {
		String queryString = "PICK Product p IF p.sku=? AND ((p.currency=? OR (p.foo=?)))";
		Query query = createParser("4711", "EUR", "bar").parse(queryString).getQuery();
		Assert.assertNotNull(query);
		Assert.assertTrue(Pick.class.isInstance(query));
		Pick p = (Pick) query;
		IfClause ifc = p.getIfClause();
		Assert.assertNotNull(ifc);
		BooleanStatement bs = ifc.getNext();
		Expression exp = assertIsExpression(bs);
		Assert.assertEquals(exp.getStatement(), "p.sku=?");
		Assert.assertEquals(exp.getNextOperator(), BooleanOperator.AND);
		WrapperBooleanStatement wrapper = assertIsWrapper(exp.getNext());
		WrapperBooleanStatement wrapper2 = assertIsWrapper(wrapper.getWrapped());
		Expression exp2 = assertIsExpression(wrapper2.getWrapped());
		Assert.assertEquals(exp2.getStatement(), "p.currency=?");
		Assert.assertEquals(exp2.getNextOperator(), BooleanOperator.OR);
		WrapperBooleanStatement wrapper3 = assertIsWrapper(exp2.getNext());
		Expression exp3 = assertIsExpression(wrapper3.getWrapped());
		Assert.assertEquals(exp3.getStatement(), "p.foo=?");
		Assert.assertNull(exp3.getNext());
		Assert.assertNull(exp3.getNextOperator());
		Assert.assertNull(wrapper.getNext());
		Assert.assertNull(wrapper.getNextOperator());
	}
	
	@Test
	public void testIfWrapperWithFollowingOperator() throws QueryParsingException {
		String queryString = "PICK Product p IF (p.sku=?) AND p.currency=?";
		Query query = createParser("4711", "EUR").parse(queryString).getQuery();
		Assert.assertNotNull(query);
		Assert.assertTrue(Pick.class.isInstance(query));
		Pick p = (Pick) query;
		IfClause ifc = p.getIfClause();
		Assert.assertNotNull(ifc);
		BooleanStatement bs = ifc.getNext();
		WrapperBooleanStatement wrapper = assertIsWrapper(bs);
		Expression exp = assertIsExpression(wrapper.getWrapped());
		Assert.assertEquals(exp.getStatement(), "p.sku=?");
		Assert.assertEquals(wrapper.getNextOperator(), BooleanOperator.AND);
		Expression exp2 = assertIsExpression(wrapper.getNext());
		Assert.assertEquals(exp2.getStatement(), "p.currency=?");
	}
	
	@Test
	public void testLimitOffset() throws QueryParsingException {
		String queryString = "PICK Product p LIMIT ? OFFSET ?";
		Query query = createParser(47, 11).parse(queryString).getQuery();
		Assert.assertNotNull(query);
		Assert.assertTrue(Pick.class.isInstance(query));
		Pick p = (Pick) query;
		IfClause ifc = p.getIfClause();
		Assert.assertNull(ifc);
		Assert.assertNotNull(p.getLimit());
		Assert.assertNotNull(p.getOffset());
		Assert.assertEquals(p.getLimit().longValue(), 47);
		Assert.assertEquals(p.getOffset().longValue(), 11);
		
	}
	
	@Test
	public void testOrderBy() throws QueryParsingException {
		String queryString = "PICK Product p ORDERBY p.sku";
		Query query = createParser().parse(queryString).getQuery();
		Assert.assertNotNull(query);
		Assert.assertTrue(Pick.class.isInstance(query));
		Pick p = (Pick) query;
		IfClause ifc = p.getIfClause();
		Assert.assertNull(ifc);
		Ordering ordering = p.getOrdering();
		Assert.assertNotNull(ordering);
		Assert.assertEquals(ordering.getField(), "p.sku");
		Assert.assertTrue(ordering.isAscending());
	}
	
	@Test
	public void testOrderByASC() throws QueryParsingException {
		String queryString = "PICK Product p ORDERBY p.sku DESC";
		Query query = createParser().parse(queryString).getQuery();
		Assert.assertNotNull(query);
		Assert.assertTrue(Pick.class.isInstance(query));
		Pick p = (Pick) query;
		IfClause ifc = p.getIfClause();
		Assert.assertNull(ifc);
		Ordering ordering = p.getOrdering();
		Assert.assertNotNull(ordering);
		Assert.assertEquals(ordering.getField(), "p.sku");
		Assert.assertFalse(ordering.isAscending());
	}
	
	@Test
	public void testIfClauseAndLimit() throws QueryParsingException {
		String queryString = "PICK Product p IF p.sku=? LIMIT ? OFFSET ?";
		Query query = createParser("4711", 1, 0).parse(queryString).getQuery();
		Assert.assertNotNull(query);
		Assert.assertTrue(Pick.class.isInstance(query));
		Pick p = (Pick) query;
		IfClause ifc = p.getIfClause();
		Assert.assertNotNull(ifc);
		Long limit = p.getLimit();
		Assert.assertNotNull(limit);
		Assert.assertEquals(limit, Long.valueOf(1));
		Long offset = p.getOffset();
		Assert.assertNotNull(offset);
		Assert.assertEquals(offset, Long.valueOf(0));
	}
	
	@Test
	public void testSimpleCount() throws QueryParsingException {
		String queryString = "COUNT Product p";
		Query query = createParser().parse(queryString).getQuery();
		Assert.assertNotNull(query);
		Assert.assertTrue(Count.class.isInstance(query));
		Count count = (Count) query;
		IfClause ifc = count.getIfClause();
		Assert.assertNull(ifc);
	}
	
	@Test
	public void testSimpleCountWithIfClause() throws QueryParsingException {
		String queryString = "COUNT Product p IF p.sku=?";
		Query query = createParser("4711").parse(queryString).getQuery();
		Assert.assertNotNull(query);
		Assert.assertTrue(Count.class.isInstance(query));
		Count count = (Count) query;
		IfClause ifc = count.getIfClause();
		Assert.assertNotNull(ifc);
	}
	
	@Test
	public void testFailures() {
		String[] badQueries = new String[]{
			"PICK Product p IF (p.sku=? AND p.currency=?",
			"PICK Product p IF ((p.sku=?) AND p.currency=?",
			"PICK Product p IF p.sku=?) AND p.currency=?",
			"PICK Product p IF AND p.currency=?",
			"PICK Product p IF () AND p.currency=?"
		};
		for (String badQuery : badQueries) {
			try {
				createParser("a", "b").parse(badQuery);
				Assert.fail("'"+badQuery+"' did not throw a query parsing exception");
			} catch(QueryParsingException e) {
				// expected
			}
		}
	}
	
	@Test
	public void testGoodOnes() {
		String[] goodQueries = new String[]{
			"PICK Product p IF (p.sku=? AND p.currency=?)",
			"PICK Product p IF ((p.sku=?) AND p.currency=?)",
			"PICK Product p IF (p.sku=?) AND p.currency=?",
			"PICK Product p IF p.currency=?",
			"PICK Product p IF ()p.currency=?"
		};
		for (String goodQuery : goodQueries) {
			try {
				createParser("a", "b").parse(goodQuery);
			} catch(QueryParsingException e) {
				Assert.fail("'"+goodQuery+"' did throw a query parsing exception", e);
				// expected
			}
		}
	}
	
	private Expression assertIsExpression(BooleanStatement bs) {
		Assert.assertNotNull(bs, "provided boolean statement is null and therefore not expression");
		Assert.assertTrue(Expression.class.isInstance(bs), "provided boolean statement is not an expression");
		return (Expression) bs;
	}
	
	private WrapperBooleanStatement assertIsWrapper(BooleanStatement bs) {
		Assert.assertNotNull(bs, "provided boolean statement is null and therefore not wrapper");
		Assert.assertTrue(WrapperBooleanStatement.class.isInstance(bs), "provided boolean statement is not a wrapper");
		return (WrapperBooleanStatement) bs;
	}

	private ParserImpl createParser(Object... args) {
		DelegatingExpressionStatementHandler delegate = new DelegatingExpressionStatementHandler();
		delegate.addExpressionStatementHandler(new EqualExpressionStatementHandler());
		delegate.addExpressionStatementHandler(new GreaterEqualExpressionStatementHandler());
		delegate.addExpressionStatementHandler(new GreaterExpressionStatementHandler());
		delegate.addExpressionStatementHandler(new LowerEqualExpressionStatementHandler());
		delegate.addExpressionStatementHandler(new LowerExpressionStatementHandler());
		delegate.addExpressionStatementHandler(new InRangeExpressionStatementHandler());
		ParserImpl parser = new ParserImpl(args).expressionStatementHandler(delegate);
		return parser;
	}
}
