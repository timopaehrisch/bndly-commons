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

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class ComparisonExpressionImpl extends ExpressionImpl implements ComparisonExpression {
	private ContextVariable left;
	private ContextVariable right;
	private boolean negated;
	private final Type type;

	public ComparisonExpressionImpl(String statement, Type type) {
		super(statement);
		this.type = type;
	}

	@Override
	public Type getComparisonType() {
		return type;
	}

	@Override
	public ContextVariable getLeft() {
		return left;
	}

	@Override
	public ContextVariable getRight() {
		return right;
	}

	@Override
	public boolean isNegated() {
		return negated;
	}

	public void setLeft(ContextVariable left) {
		this.left = left;
	}

	public void setRight(ContextVariable right) {
		this.right = right;
	}

	public void setNegated(boolean negated) {
		this.negated = negated;
	}
	
}
