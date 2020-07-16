package org.bndly.schema.impl;

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
import org.bndly.schema.api.AttributeMediator;
import org.bndly.schema.api.LoadedAttributes;
import org.bndly.schema.api.MappingBinding;
import org.bndly.schema.api.MappingBindingsProvider;
import org.bndly.schema.api.ObjectReference;
import org.bndly.schema.api.Pagination;
import org.bndly.schema.api.Record;
import org.bndly.schema.api.RecordContext;
import org.bndly.schema.api.Transaction;
import org.bndly.schema.api.db.AttributeColumn;
import org.bndly.schema.api.db.Table;
import org.bndly.schema.api.db.TypeTable;
import org.bndly.schema.api.query.Expression;
import org.bndly.schema.api.query.NestedQueryAttribute;
import org.bndly.schema.api.query.OrderBy;
import org.bndly.schema.api.query.PreparedStatementValueProvider;
import org.bndly.schema.api.query.QueryAttribute;
import org.bndly.schema.api.query.QueryContext;
import org.bndly.schema.api.query.Select;
import org.bndly.schema.api.query.SimpleQueryAttribute;
import org.bndly.schema.api.query.Where;
import org.bndly.schema.api.query.WrapperExpression;
import org.bndly.schema.api.services.QueryByExample;
import org.bndly.schema.api.services.TableRegistry;
import org.bndly.schema.impl.query.ExpressionProducer;
import org.bndly.schema.model.Attribute;
import org.bndly.schema.model.BinaryAttribute;
import org.bndly.schema.model.InverseAttribute;
import org.bndly.schema.model.NamedAttributeHolder;
import org.bndly.schema.model.NamedAttributeHolderAttribute;
import org.bndly.schema.model.SchemaUtil;
import org.bndly.schema.model.Type;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class QueryByExampleImpl implements QueryByExample {

	private final EngineImpl engine;
	private final NamedAttributeHolder namedAttributeHolder;
	private final QueryContext queryContext;
	private final Map<String, Attribute> attributesByName = new HashMap<>();
	private final Select select;
	private Where where;
	private Table rootTable;
	private final List<QueryAttribute> queryAttributes = new ArrayList<>();
	private final List<QueryAttribute> rootQueryAttributes = new ArrayList<>();
	private LoadingIterator loadingIterator;
	private final RecordContext recordContext;
	private String sortDirection;
	private Attribute sortAttribute;

	public QueryByExampleImpl(NamedAttributeHolder namedAttributeHolder, EngineImpl engine, RecordContext recordContext) {
		this.namedAttributeHolder = namedAttributeHolder;
		this.engine = engine;
		this.queryContext = engine.getQueryContextFactory().buildQueryContext();
		this.select = queryContext.select();

		for (Attribute attribute : SchemaUtil.collectAttributes(namedAttributeHolder)) {
			attributesByName.put(attribute.getName(), attribute);
		}

		this.rootTable = engine.getTableRegistry().getJoinTableByNamedAttributeHolder(namedAttributeHolder);
		if (rootTable == null) {
			rootTable = engine.getTableRegistry().getTypeTableByType(namedAttributeHolder.getName());
		}
		this.recordContext = recordContext;
	}

	@Override
	public QueryByExample lazy() {
		return applyLoadingStrategy(false);
	}
	
	@Override
	public QueryByExample eager() {
		return applyLoadingStrategy(true);
	}

	private QueryByExample applyLoadingStrategy(final boolean eager) {
		if (loadingIterator != null) {
			throw new IllegalStateException("a loading strategy has already been defined.");
		}

		// when doing a lazy loading, then load those attributes/tables, that are used in the query attributes. otherwise we will get wrong results
		final Set<String> attributePaths = new HashSet<>();
		addQueriedAttributePaths(attributePaths, queryAttributes, "");
		
		TableRegistry tableRegistry = engine.getTableRegistry();
		// if we assumed a non-eager select until now...
		// recursively iterate through the queried type-attribute graph 
		// and append the required joins
		loadingIterator = new LoadingIterator(tableRegistry, select, new LoadedAttributes() {

			@Override
			public Strategy isLoaded(Attribute attribute, String attributePath) {
				Strategy strategy = Strategy.LOADED;
				if (!attributePaths.contains(attributePath)) {
					if (attributePath.contains(".")) {
							strategy = eager ? strategy : Strategy.NOT_LOADED;
					} else {
						if (NamedAttributeHolderAttribute.class.isInstance(attribute)) {
							strategy = eager ? strategy : Strategy.LAZY_LOADED;
						}
					}
				}
				return strategy;
			}

		}, Collections.EMPTY_SET);
		loadingIterator.iterate(this.namedAttributeHolder.getName());
		queryContext.setExternalMappingBindingsProvider(loadingIterator);
		return this;
	}

	@Override
	public QueryByExample attribute(String attributeName, Object value) {
		assertAttributeIsKnown(attributeName);

		Attribute attribute = attributesByName.get(attributeName);
		if (attribute != null) {
			QueryAttribute qa = buildQueryAttribute(attribute, value);
			queryAttributes.add(qa);
		} else {
			AttributeColumn pkColumn = rootTable.getPrimaryKeyColumn();
			Attribute att = pkColumn.getAttribute();
			if (attributeName.equals(att.getName())) {
				QueryAttribute qa = new SimpleQueryAttribute(value, att);
				rootQueryAttributes.add(qa);
			} else {
				throw new IllegalArgumentException("unknown attribute: " + attributeName);
			}
		}

		return this;
	}

	private Attribute assertAttributeIsKnown(String attributeName) throws IllegalArgumentException {
		if (!attributesByName.containsKey(attributeName) && !"id".equals(attributeName)) {
			throw new IllegalArgumentException("unknown attribute: " + attributeName + " in " + namedAttributeHolder.getName());
		}
		return attributesByName.get(attributeName);
	}

	private void appendNestedAttributesToQuery(Record r, NestedQueryAttribute nqa) {
		for (Attribute attribute : SchemaUtil.collectAttributes(r.getType())) {
			if (r.isAttributePresent(attribute.getName())) {
				Object v = r.getAttributeValue(attribute.getName());
				if (BinaryAttribute.class.isInstance(v)) {
					continue;
				} else if (InverseAttribute.class.isInstance(v)) {
					continue;
				} else {
					QueryAttribute qa = buildQueryAttribute(attribute, v);
					nqa.getNested().add(qa);
				}
			}
		}
	}

	private QueryAttribute buildQueryAttribute(Attribute attribute, Object value) {
		if (Record.class.isInstance(value)) {
			Record r = (Record) value;
			Type type = r.getType();
			Long id = r.getId();
			NestedQueryAttribute nqa;
			nqa = new NestedQueryAttribute(type, attribute);
			if (id != null) {
				TypeTable tt = engine.getTableRegistry().getTypeTableByType(type);
				Attribute primaryKeyAttribute = tt.getPrimaryKeyColumn().getAttribute();
				QueryAttribute sqa = new SimpleQueryAttribute(id, primaryKeyAttribute);
				nqa.getNested().add(sqa);
			}
			appendNestedAttributesToQuery(r, nqa);
			return nqa;
		} else {
			QueryAttribute qa = new SimpleQueryAttribute(value, attribute);
			return qa;
		}
	}

	@Override
	public QueryByExample pagination(Pagination p) {
		Long o = p.getOffset();
		if (o != null) {
			select.offset(o);
		}
		Long s = p.getSize();
		if (s != null) {
			select.limit(s);
		}
		return this;
	}

	@Override
	public List<QueryAttribute> getQueryAttributes() {
		List<QueryAttribute> l = new ArrayList<>();
		l.addAll(rootQueryAttributes);
		l.addAll(queryAttributes);
		return l;
	}

	@Override
	public Record single() {
		if (loadingIterator == null) {
			lazy();
		}
		appendQueryAttributes(loadingIterator);
		Transaction tx = engine.getQueryRunner().createTransaction();
		ObjectReference<Record> ref = tx.getQueryRunner().single(queryContext.build(recordContext));
		tx.commit();
		return ref.get();
	}

	@Override
	public List<Record> all() {
		if (loadingIterator == null) {
			lazy();
		}
		appendQueryAttributes(loadingIterator);
		appendSorting(loadingIterator);
		Transaction tx = engine.getQueryRunner().createTransaction();
		ObjectReference<List<Record>> ref = tx.getQueryRunner().list(queryContext.build(recordContext));
		tx.commit();
		return ref.get();
	}

	@Override
	public long count() {
		if (loadingIterator == null) {
			lazy();
		}
		select.count();
		appendQueryAttributes(loadingIterator);
		Transaction tx = engine.getQueryRunner().createTransaction();
		ObjectReference<Long> ref = tx.getQueryRunner().number(queryContext.build(recordContext));
		tx.commit();
		Long number = ref.get();
		if (number == null) {
			return 0;
		}
		return number;
	}

	private void appendQueryAttributes(MappingBindingsProvider mbp) {
		if (queryAttributes.isEmpty() && rootQueryAttributes.isEmpty()) {
			return;
		}
		if (where == null) {
			where = select.where();//.expression().and();
		}
		final ExpressionProducer producer = new ExpressionProducer() {
			private Expression exp = null;

			@Override
			public Expression produce() {
				if (exp == null) {
					exp = where.expression();
				} else {
					exp = exp.and();
				}
				return exp;
			}
		};
		if (!rootQueryAttributes.isEmpty()) {
			for (QueryAttribute queryAttribute : rootQueryAttributes) {
				appendQueryAttribute(queryAttribute, mbp.getRootMappingBinding(), producer);
			}
		}
		if (!queryAttributes.isEmpty()) {
			final ExpressionProducer bindingRootProducer = new ExpressionProducer() {
				private WrapperExpression wrap;

				@Override
				public Expression produce() {
					if (wrap == null) {
						wrap = producer.produce().wrap();
					} else {
						wrap = wrap.or().wrap();
					}
					return wrap.wrapped();
				}

			};
			for (MappingBinding mappingBinding : mbp.getMappingBindings()) {
				ExpressionProducer bindingBasedProducer = new ExpressionProducer() {
					private Expression exp = null;

					@Override
					public Expression produce() {
						if (exp == null) {
							exp = bindingRootProducer.produce();
						} else {
							exp = exp.and();
						}
						return exp;
					}
				};
				// append a check that the id is not null (we are making a type join!! -> if the type differs, the id might be null)
				AliasBinding pkAlias = mappingBinding.getPrimaryKeyAlias();
				String tbAlias = mappingBinding.getTableAlias();
				bindingBasedProducer.produce().criteria().field(tbAlias + "." + pkAlias.getAttributeColumn().getColumnName()).isNotNull();
				for (QueryAttribute queryAttribute : queryAttributes) {
					appendQueryAttribute(queryAttribute, mappingBinding, bindingBasedProducer);
				}
			}
		}
	}

	private void appendQueryAttribute(QueryAttribute queryAttribute, MappingBinding mappingBinding, ExpressionProducer expressionProducer) {
		if (SimpleQueryAttribute.class.isInstance(queryAttribute)) {
			appendSimpleQueryAttribute((SimpleQueryAttribute) queryAttribute, mappingBinding, expressionProducer);
		} else if (NestedQueryAttribute.class.isInstance(queryAttribute)) {
			NestedQueryAttribute nqa = (NestedQueryAttribute) queryAttribute;
			Attribute att = nqa.getAttribute();
			Map<String, List<MappingBinding>> sb = mappingBinding.getSubBindings();
			if (sb != null) {
				List<MappingBinding> sbList = sb.get(att.getName());
				if (sbList != null) {
					for (MappingBinding subBinding : sbList) {
						if (subBinding.getHolder() == nqa.getHolder()) {
							appendNestedQueryAttribute(nqa, subBinding, expressionProducer);
							return;
						}
					}
				}
			}
		}
	}

	private void appendSimpleQueryAttribute(SimpleQueryAttribute queryAttribute, MappingBinding mappingBinding, ExpressionProducer expressionProducer) {
		List<AliasBinding> aliases = mappingBinding.getAliases();
		if (aliases != null) {
			for (AliasBinding aliasBinding : aliases) {
				final Attribute att = queryAttribute.getAttribute();
				final Object val = queryAttribute.getValue();
				if (aliasBinding.getAttribute() == att) {
					String field = mappingBinding.getTableAlias() + "." + aliasBinding.getAttributeColumn().getColumnName();
					Expression exp = expressionProducer.produce();
					if (val != null) {
						exp.criteria().field(field).equal().value(createPreparedStatementValueProvider(aliasBinding.getAttribute(), val));
					} else {
						exp.criteria().field(field).isNull();
					}
					return;
				}
			}
		}
		AliasBinding aliasBinding = mappingBinding.getPrimaryKeyAlias();
		if (aliasBinding.getAttribute() == queryAttribute.getAttribute()) {
			String field = mappingBinding.getTableAlias() + "." + aliasBinding.getAttributeColumn().getColumnName();
			Expression exp = expressionProducer.produce();
			exp.criteria().field(field).equal().value(createPreparedStatementValueProvider(aliasBinding.getAttribute(), queryAttribute.getValue()));
		}
	}

	private PreparedStatementValueProvider createPreparedStatementValueProvider(final Attribute attribute, final Object rawValue) {
		return new PreparedStatementValueProvider() {

			@Override
			public Object get() {
				return rawValue;
			}

			@Override
			public void set(int index, PreparedStatement ps) throws SQLException {
				AttributeMediator<Attribute> mediator = engine.getMediatorRegistry().getMediatorForAttribute(attribute);
				mediator.setRawParameterInPreparedStatement(index, ps, attribute, get());
			}
		};
	}

	private void appendNestedQueryAttribute(NestedQueryAttribute queryAttribute, MappingBinding mappingBinding, ExpressionProducer expressionProducer) {
		List<QueryAttribute> nested = queryAttribute.getNested();
		for (QueryAttribute nestedQueryAttribute : nested) {
			appendQueryAttribute(nestedQueryAttribute, mappingBinding, expressionProducer);
		}
	}

	@Override
	public QueryByExample orderBy(String attributeName) {
		this.sortAttribute = assertAttributeIsKnown(attributeName);
		return this;
	}

	@Override
	public QueryByExample asc() {
		this.sortDirection = "ASC";
		return this;
	}

	@Override
	public QueryByExample desc() {
		this.sortDirection = "DESC";
		return this;
	}

	private void appendSorting(MappingBindingsProvider mbp) {
		if (this.sortDirection == null) {
			asc();
		}
		OrderBy orderBy = select.orderBy();
		if (this.sortAttribute != null) {
			// append the SQL fragments to the select object
			MappingBinding rootMappingBinding = mbp.getRootMappingBinding();
			List<AliasBinding> aliases = mbp.getRootMappingBinding().getAliases();
			if (aliases != null) {
				for (AliasBinding aliasBinding : aliases) {
					if (aliasBinding.getAttribute() == sortAttribute) {
						String alias = rootMappingBinding.getTableAlias() + "." + aliasBinding.getAttributeColumn().getColumnName();
						orderBy.columnAlias(alias);
					}
				}
			} else {
				List<MappingBinding> bindings = mbp.getMappingBindings();
				for (MappingBinding mappingBinding : bindings) {
					List<AliasBinding> localAliases = mappingBinding.getAliases();
					if (localAliases == null) {
						continue;
					}
					for (AliasBinding aliasBinding : localAliases) {
						if (aliasBinding.getAttribute() == sortAttribute) {
							String alias = mappingBinding.getTableAlias() + "." + aliasBinding.getAttributeColumn().getColumnName();
							orderBy.columnAlias(alias);
						}
					}
				}
			}
			orderBy.direction(sortDirection);
		} else {
			MappingBinding rootMappingBinding = mbp.getRootMappingBinding();
			String pkColumnName = rootMappingBinding.getPrimaryKeyAlias().getAttributeColumn().getColumnName();
			orderBy.columnAlias(rootMappingBinding.getTableAlias() + "." + pkColumnName);
		}
		orderBy.direction(sortDirection);
	}

	private void addQueriedAttributePaths(Set<String> attributePaths, List<QueryAttribute> queryAttributes, String prefix) {
		if (queryAttributes == null || queryAttributes.isEmpty()) {
			return;
		}
		for (QueryAttribute queryAttribute : queryAttributes) {
			if (NestedQueryAttribute.class.isInstance(queryAttribute)) {
				List<QueryAttribute> nested = ((NestedQueryAttribute) queryAttribute).getNested();
				String currentPath = prefix + queryAttribute.getAttribute().getName();
				attributePaths.add(currentPath);
				addQueriedAttributePaths(attributePaths, nested, currentPath + ".");
			} else if (SimpleQueryAttribute.class.isInstance(queryAttribute)) {
				attributePaths.add(prefix + queryAttribute.getAttribute().getName());
			}
		}
	}

}
