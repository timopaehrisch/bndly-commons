package org.bndly.schema.impl.nquery.sqlmapper;

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

import org.bndly.schema.api.AliasBinding;
import org.bndly.schema.api.MappingBinding;
import org.bndly.schema.api.nquery.BooleanStatement;
import org.bndly.schema.api.query.Expression;
import org.bndly.schema.impl.query.ExpressionProducer;
import java.util.List;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public abstract class AbstractFieldCentricMapper<E extends BooleanStatement> implements BooleanStatementSQLMapper<E> {

	private final Class<E> supportedInterface;

	public AbstractFieldCentricMapper(Class<E> supportedInterface) {
		if (supportedInterface == null) {
			throw new IllegalArgumentException("supportedInterface is not allowed to be null");
		}
		this.supportedInterface = supportedInterface;
	}
	
	@Override
	public final boolean supports(BooleanStatement booleanStatement) {
		return supportedInterface.isInstance(booleanStatement);
	}

	@Override
	public final void map(E booleanStatement, final Expression expression, String prefix, MappingBinding mappingBinding, Context context) {
		String attributePath = getAttributePath(booleanStatement);
		if (prefix != null && attributePath.startsWith(prefix)) {
			attributePath = attributePath.substring(prefix.length());
		}
		List<AliasBinding> aliasBindings = Util.resolveAliasBindingsFromMappingBinding(mappingBinding, attributePath, context.getTableRegistry());
		if (aliasBindings.size() == 1) {
			map(booleanStatement, new ExpressionProducer() {

				@Override
				public Expression produce() {
					return expression;
				}
			}, prefix, mappingBinding, context, aliasBindings.get(0));
		} else {
			ExpressionProducer expressionProducer = new ExpressionProducer() {
				Expression tmp = null;

				@Override
				public Expression produce() {
					if (tmp == null) {
						tmp = expression.wrap().wrapped();
					} else {
						tmp = tmp.or();
					}
					return tmp;
				}
			};
			for (AliasBinding aliasBinding : aliasBindings) {
				map(booleanStatement, expressionProducer, prefix, mappingBinding, context, aliasBinding);
			}
		}
	}
	
	protected abstract void map(E booleanStatement, ExpressionProducer expressionProducer, String prefix, MappingBinding mappingBinding, Context context, AliasBinding aliasBinding);
	
	protected abstract String getAttributePath(E booleanStatement);
	
}
