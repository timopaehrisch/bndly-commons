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
import org.bndly.schema.api.ObjectReference;
import org.bndly.schema.api.Pagination;
import org.bndly.schema.api.Record;
import org.bndly.schema.api.RecordContext;
import org.bndly.schema.api.Transaction;
import org.bndly.schema.api.db.AttributeColumn;
import org.bndly.schema.api.db.JoinTable;
import org.bndly.schema.api.db.Table;
import org.bndly.schema.api.db.TypeTable;
import org.bndly.schema.api.exception.SchemaException;
import org.bndly.schema.api.listener.PreDeleteListener;
import org.bndly.schema.api.listener.PreMergeListener;
import org.bndly.schema.api.listener.PrePersistListener;
import org.bndly.schema.api.listener.QueryByExampleIteratorListener;
import org.bndly.schema.api.nquery.BooleanOperator;
import org.bndly.schema.api.nquery.BooleanStatement;
import org.bndly.schema.api.nquery.Count;
import org.bndly.schema.api.nquery.ExpressionStatementHandler;
import org.bndly.schema.api.nquery.IfClause;
import org.bndly.schema.api.nquery.Ordering;
import org.bndly.schema.api.nquery.Pick;
import org.bndly.schema.api.nquery.QueryParsingException;
import org.bndly.schema.api.nquery.WrapperBooleanStatement;
import org.bndly.schema.api.query.Expression;
import org.bndly.schema.api.query.Insert;
import org.bndly.schema.api.query.Join;
import org.bndly.schema.api.query.OrderBy;
import org.bndly.schema.api.query.PreparedStatementValueProvider;
import org.bndly.schema.api.query.Query;
import org.bndly.schema.api.query.QueryContext;
import org.bndly.schema.api.query.Select;
import org.bndly.schema.api.query.TableExpression;
import org.bndly.schema.api.query.Where;
import org.bndly.schema.api.query.WrapperExpression;
import org.bndly.schema.api.services.Accessor;
import org.bndly.schema.api.services.QueryByExample;
import org.bndly.schema.api.services.QueryContextFactory;
import org.bndly.schema.api.services.TableRegistry;
import org.bndly.schema.impl.events.PersistenceEventTransaction;
import org.bndly.schema.impl.nquery.BooleanStatementIterator;
import org.bndly.schema.impl.nquery.ParserImpl;
import org.bndly.schema.impl.nquery.sqlmapper.BooleanStatementSQLMapper;
import org.bndly.schema.impl.nquery.sqlmapper.ComparisonExpressionMapper;
import org.bndly.schema.impl.nquery.sqlmapper.InRangeExpressionMapper;
import org.bndly.schema.impl.nquery.sqlmapper.MediatorProvider;
import org.bndly.schema.impl.nquery.sqlmapper.RequiredAttribtuesInspector;
import org.bndly.schema.impl.nquery.sqlmapper.TypedExpressionMapper;
import org.bndly.schema.impl.nquery.sqlmapper.Util;
import org.bndly.schema.impl.nquery.sqlmapper.WrapperInspector;
import org.bndly.schema.impl.persistence.MarkAsNotDirty;
import org.bndly.schema.impl.persistence.PersistedEventForRecordContext;
import org.bndly.schema.impl.persistence.PersistenceManager;
import org.bndly.schema.impl.persistence.TypeJoinInsert;
import org.bndly.schema.impl.persistence.TypeTableDelete;
import org.bndly.schema.impl.persistence.TypeTableInsert;
import org.bndly.schema.impl.persistence.TypeTableUpdate;
import org.bndly.schema.impl.persistence.UniqueConstraintTableInsert;
import org.bndly.schema.impl.persistence.UniqueConstraintTableUpdate;
import org.bndly.schema.impl.persistence.UploadBlobs;
import org.bndly.schema.model.Attribute;
import org.bndly.schema.model.Mixin;
import org.bndly.schema.model.NamedAttributeHolder;
import org.bndly.schema.model.NamedAttributeHolderAttribute;
import org.bndly.schema.model.Type;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

public class AccessorImpl implements Accessor, Resetable {

	private MediatorRegistryImpl mediatorRegistry;
	private EngineImpl engine;
	private ExpressionStatementHandler expressionStatementHandler;

	private final Map<Class<? extends BooleanStatement>, BooleanStatementSQLMapper> sqlMappersByStatementType = new HashMap<>();
	private final Map<Class<? extends BooleanStatement>, RequiredAttribtuesInspector> requiredAttributesInspectorsByStatementType = new HashMap<>();

	private final List<BooleanStatementSQLMapper> booleanStatementSQLMappers;
	private final List<RequiredAttribtuesInspector> requiredAttributesInspectors;

	public AccessorImpl() {
		booleanStatementSQLMappers = new ArrayList<>();
		requiredAttributesInspectors = new ArrayList<>();

		MediatorProvider mediatorProvider = new MediatorProvider() {

			@Override
			public AttributeMediator getMediatorForAttribute(Attribute attribute) {
				return mediatorRegistry.getMediatorForAttribute(attribute);
			}
		};
		ComparisonExpressionMapper eem = new ComparisonExpressionMapper(mediatorProvider);
		booleanStatementSQLMappers.add(eem);
		requiredAttributesInspectors.add(eem);

		InRangeExpressionMapper irm = new InRangeExpressionMapper(mediatorProvider);
		booleanStatementSQLMappers.add(irm);
		requiredAttributesInspectors.add(irm);

		TypedExpressionMapper tm = new TypedExpressionMapper();
		booleanStatementSQLMappers.add(tm);
		requiredAttributesInspectors.add(tm);

		requiredAttributesInspectors.add(new WrapperInspector());
	}
	
	public static void appendAttributesToSelectInTypeJoin(TableContextProvider tcp, Select select) {
		select.expression().table(tcp.getAlias()).field(tcp.getTable().getPrimaryKeyColumn().getColumnName()).as(tcp.getAlias() + "_id");
		List<AttributeColumn> cols = tcp.getTable().getColumns();
		for (AttributeColumn attributeColumn : cols) {
			String fieldName = attributeColumn.getColumnName();
			String alias = tcp.getAlias() + "_" + fieldName;
			select.expression().table(tcp.getAlias()).field(fieldName).as(alias);
		}
	}

	@Override
	public void reset() {
	}

	@Override
	public RecordContext buildRecordContext() {
		return new RecordContextImpl(engine);
	}

	@Override
	public Object createIdAsNamedAttributeHolderQuery(NamedAttributeHolder namedAttributeHolder, Type sourceType, long id, RecordContext context) {
		Record r = context.get(sourceType, id);
		if (r == null) {
			// lets make a defensive copy
			r = buildRecordContext().create(sourceType, id);
		}
		return createIdAsNamedAttributeHolderQuery(namedAttributeHolder, r);
	}

	@Override
	public Object createIdAsNamedAttributeHolderQuery(NamedAttributeHolder namedAttributeHolder, final Record record) {
		/*
		 in mysql the inner joins are not nested, but followed by each other
		 SELECT MIXIN_PURCHASABLE.ID FROM MIXIN_PURCHASABLE
		 INNER JOIN JOIN_PRODUCT
		 ON MIXIN_PURCHASABLE.PRODUCT_ID=JOIN_PRODUCT.ID
		 INNER JOIN VARIANT
		 ON JOIN_PRODUCT.VARIANT_ID=VARIANT.ID AND VARIANT.ID=1
		 */
		if (record.getType() == namedAttributeHolder && (record.getType().getSubTypes() == null || record.getType().getSubTypes().isEmpty())) {
			return record.getId();
		}

		JoinTable targetTable = engine.getTableRegistry().getJoinTableByNamedAttributeHolder(namedAttributeHolder);
		final String targetTableName = targetTable.getTableName();

		QueryContext qc = engine.getQueryContextFactory().buildQueryContext();
		Select select = qc.select();
		select.expression().table(targetTableName).field(targetTable.getPrimaryKeyColumn().getColumnName());
		TableExpression table = select.from().table(targetTableName);
		final TableExpression tableRoot = table;

		List<Table> joinChain = new ArrayList<>();
		buildRightJoinToNamedAttributeHolder(record.getType(), namedAttributeHolder, joinChain);
		Table leftTable = null;
		for (int i = joinChain.size() - 1; i >= 0; i--) {
			Table rightTable = joinChain.get(i);
			if (leftTable != null) {
				Join join = tableRoot.join(rightTable.getTableName()).inner();
				table = join;
				Expression onExp = null;

				List<AttributeColumn> cols = leftTable.getColumns();
				AttributeColumn joinColumn = null;
				for (AttributeColumn attributeColumn : cols) {
					Attribute att = attributeColumn.getAttribute();
					if (NamedAttributeHolderAttribute.class.isInstance(att)) {
						NamedAttributeHolderAttribute naha = (NamedAttributeHolderAttribute) att;
						NamedAttributeHolder holder = naha.getNamedAttributeHolder();
						if (JoinTable.class.isInstance(rightTable)) {
							JoinTable jtB = (JoinTable) rightTable;
							if (jtB.getNamedAttributeHolder() == holder) {
								joinColumn = attributeColumn;
								break;
							}
						} else if (TypeTable.class.isInstance(rightTable)) {
							TypeTable ttB = (TypeTable) rightTable;
							if (ttB.getType() == holder) {
								joinColumn = attributeColumn;
								break;
							}
						}
					}
				}
				if (joinColumn == null) {
					throw new IllegalStateException("could not find join column");
				}

				String left = leftTable.getTableName() + "." + joinColumn.getColumnName();
				String right = rightTable.getTableName() + "." + rightTable.getPrimaryKeyColumn().getColumnName();
				if (onExp == null) {
					onExp = join.on();
				} else {
					onExp = onExp.and();
				}
				onExp.criteria().field(left).equal().field(right);

				if (i == 0) {
					onExp.and().criteria().field(rightTable.getTableName() + "." + rightTable.getPrimaryKeyColumn().getColumnName()).equal().value(new ObjectReference<Long>() {

						@Override
						public Long get() {
							return record.getId();
						}

					});
				}
				leftTable = rightTable;
			} else {
				leftTable = rightTable;
				continue;
			}
		}
		Query q = qc.build(record.getContext());
		return q;
	}

	@Override
	public long readIdAsNamedAttributeHolder(NamedAttributeHolder namedAttributeHolder, Type sourceType, long id, RecordContext context) {
		return readIdAsNamedAttributeHolder(namedAttributeHolder, sourceType, id, context, null);
	}

	private long readIdAsNamedAttributeHolder(NamedAttributeHolder namedAttributeHolder, Type sourceType, long id, RecordContext context, Transaction tx) {
		Object q = createIdAsNamedAttributeHolderQuery(namedAttributeHolder, sourceType, id, context);
		if (Query.class.isInstance(q)) {
			if (tx == null) {
				tx = engine.getQueryRunner().createTransaction();
			}
			ObjectReference<Long> ref = tx.getQueryRunner().number((Query) q);
			tx.commit();
			return ref.get();
		} else {
			return (long) q;
		}
	}

	private boolean buildRightJoinToNamedAttributeHolder(Type currentType, NamedAttributeHolder target, List<Table> tableJoinOrder) {
		if (Type.class.isInstance(target)) {
			Type targetType = (Type) target;
			if (targetType == currentType) {
				TypeTable tt = engine.getTableRegistry().getTypeTableByType(currentType);
				tableJoinOrder.add(tt);
				JoinTable jt = engine.getTableRegistry().getJoinTableByNamedAttributeHolder(currentType);
				if (jt != null) {
					tableJoinOrder.add(jt);
				}
				return true;
			} else {
				List<Type> tmp = targetType.getSubTypes();
				if (tmp == null || tmp.isEmpty()) {
					return false;
				} else {
					for (Type subType : tmp) {
						if (buildRightJoinToNamedAttributeHolder(currentType, subType, tableJoinOrder)) {
							tableJoinOrder.add(engine.getTableRegistry().getJoinTableByNamedAttributeHolder(targetType));
							return true;
						}
					}
					return false;
				}
			}
		} else if (Mixin.class.isInstance(target)) {
			Mixin mixin = (Mixin) target;
			List<Type> tmp = mixin.getMixedInto();
			if (tmp == null || tmp.isEmpty()) {
				return false;
			} else {
				for (Type type : tmp) {
					if (buildRightJoinToNamedAttributeHolder(currentType, type, tableJoinOrder)) {
						tableJoinOrder.add(engine.getTableRegistry().getJoinTableByNamedAttributeHolder(mixin));
						return true;
					}
				}
				return false;
			}

		} else {
			throw new IllegalArgumentException("unsupported named attribute holder");
		}
	}

	@Override
	public Record readById(String namedAttributeHolderName, final long id, RecordContext recordContext) {
		try {
			return queryByExample(namedAttributeHolderName, recordContext).attribute("id", id).single();
		} catch (UnknownNamedAttributeHolderException e) {
			return null;
		}
	}

	@Override
	public void buildInsertQuery(final Record record, Transaction transaction) {
		if (record.getId() != null) {
			throw new SchemaException("creating an insert query for a record that has already an id assigned. use an update query instead!");
		}
		for (PrePersistListener persistListener : engine.getListeners(PrePersistListener.class, record.getType().getName())) {
			persistListener.onBeforePersist(record, transaction);
		}
		TypeTableInsert.INSTANCE.append(transaction, record, engine);
		UploadBlobs.INSTANCE.append(transaction, record, engine);
		UniqueConstraintTableInsert.INSTANCE.append(transaction, record, engine);
		TypeJoinInsert.INSTANCE.append(transaction, record, engine);
		if (isPersistenceTransaction(transaction)) {
			((PersistenceEventTransaction) transaction).getPersistenceEventBuilder().schedulePersistEvent(record);
		}
		PersistedEventForRecordContext.INSTANCE.append(transaction, record, engine);
		MarkAsNotDirty.INSTANCE.append(transaction, record, engine);
	}
	
	public static void appendValueToInsert(
			Insert insert, 
			AttributeColumn attributeColumn, 
			Record record, 
			MediatorRegistryImpl mediatorRegistry
	) {
		Attribute attribute = attributeColumn.getAttribute();
		final AttributeMediator<Attribute> mediator = mediatorRegistry.getMediatorForAttribute(attribute);
		insert.value(attributeColumn.getColumnName(), mediator.createValueProviderFor(record, attribute), mediator.columnSqlType(attribute));
	}

	@Override
	public long insert(final Record record) {
		Transaction tx = engine.getQueryRunner().createTransaction();
		buildInsertQuery(record, tx);
		tx.commit();

		return record.getId();
	}

	@Override
	public void buildInsertCascadedQuery(final Record record, Transaction transaction) {
		if (record.getId() != null) {
			throw new SchemaException("creating an insert query for a record that has already an id assigned. use an update query instead!");
		}
		new PersistenceManager(engine, transaction).append(record).finalizeTransaction();
	}

	@Override
	public long insertCascaded(Record record) {
		Transaction tx = engine.getQueryRunner().createTransaction();
		buildInsertCascadedQuery(record, tx);
		tx.commit();
		return record.getId();
	}

	@Override
	public void buildUpdateQuery(final Record record, Transaction transaction) {
		buildUpdateQuery(record, transaction, true);
	}

	@Override
	public void buildUpdateQueryPostPersist(Record record, Transaction transaction) {
		buildUpdateQuery(record, transaction, false);
	}
	
	private void buildUpdateQuery(final Record record, Transaction transaction, boolean nullCheckId) {
		Long id = record.getId();
		if (nullCheckId && id == null) {
			throw new SchemaException("can not update a record without id");
		}
		for (PreMergeListener mergeListener : engine.getListeners(PreMergeListener.class, record.getType().getName())) {
			mergeListener.onBeforeMerge(record, transaction);
		}

		TypeTableUpdate.INSTANCE.append(transaction, record, engine);

		UniqueConstraintTableUpdate.INSTANCE.append(transaction, record, engine);
		
		UploadBlobs.INSTANCE.append(transaction, record, engine);
		if (isPersistenceTransaction(transaction)) {
			((PersistenceEventTransaction) transaction).getPersistenceEventBuilder().scheduleMergeEvent(record);
		}
		MarkAsNotDirty.INSTANCE.append(transaction, record, engine);
	}

	public static PreparedStatementValueProvider createRecordIdPreparedStatementValueProvider(AttributeColumn attributeColumn, final Record record, MediatorRegistryImpl mediatorRegistry) {
		final Attribute att = attributeColumn.getAttribute();
		final AttributeMediator<Attribute> mediator = mediatorRegistry.getMediatorForAttribute(att);
		return new PreparedStatementValueProvider() {

			@Override
			public Long get() {
				return record.getId();
			}

			@Override
			public void set(int index, PreparedStatement ps) throws SQLException {
				mediator.setRawParameterInPreparedStatement(index, ps, att, get());
			}
		};
	}

	private boolean isPersistenceTransaction(Transaction transaction) {
		if (PersistenceEventTransaction.class.isInstance(transaction)) {
			return true;
		}
		return false;
	}

	@Override
	public void update(final Record record) {
		Transaction tx = engine.getQueryRunner().createTransaction();
		buildUpdateQuery(record, tx);
		tx.commit();
	}

	@Override
	public void buildUpdateCascadedQuery(final Record record, Transaction transaction) {
		Long id = record.getId();
		if (id == null) {
			throw new SchemaException("can not update a record without id");
		}
		new PersistenceManager(engine, transaction).append(record).finalizeTransaction();
	}

	@Override
	public void updateCascaded(Record record) {
		Transaction tx = engine.getQueryRunner().createTransaction();
		buildUpdateCascadedQuery(record, tx);
		tx.commit();
	}

	@Override
	public void delete(final Record record, Transaction transaction) {
		buildDeleteQuery(record, transaction);
	}

	@Override
	public void buildDeleteQuery(Record record, final Transaction transaction) {
		for (PreDeleteListener deleteListener : engine.getListeners(PreDeleteListener.class, record.getType().getName())) {
			deleteListener.onBeforeDelete(record, transaction);
		}
		
		TypeTableDelete.INSTANCE.append(transaction, record, engine);
		if (isPersistenceTransaction(transaction)) {
			((PersistenceEventTransaction) transaction).getPersistenceEventBuilder().scheduleDeleteEvent(record);
		}
		MarkAsNotDirty.INSTANCE.append(transaction, record, engine);
	}

	public void _delete(Record record) {
		Transaction tx = engine.getQueryRunner().createTransaction();
		delete(record, tx);
		tx.commit();
	}

	private void appendTableAsLeftJoinForSelect(JoinTable jt, TableExpression tableExpression, Select select) {
		List<AttributeColumn> cols = jt.getColumns();
		for (AttributeColumn attributeColumn : cols) {
			Attribute att = attributeColumn.getAttribute();
			if (NamedAttributeHolderAttribute.class.isInstance(att)) {
				NamedAttributeHolderAttribute naha = ((NamedAttributeHolderAttribute) att);
				NamedAttributeHolder holder = naha.getNamedAttributeHolder();
				Table t = engine.getTableRegistry().getJoinTableByNamedAttributeHolder(holder);
				if (t == null) {
					t = engine.getTableRegistry().getTypeTableByType(holder.getName());
				}
				if (t != null) {
					appendTableAsLeftJoinForSelect(tableExpression, t, jt, attributeColumn, select);
				}
			}
		}
	}

	private void appendTableAsLeftJoinForSelect(TableExpression tableExpression, Table rightTable, Table leftTable, AttributeColumn leftTableJoinColumn, Select select) {
		Join join = tableExpression.join(rightTable.getTableName()).left();
		Expression onExp = join.on();
		onExp.criteria()
				.field(leftTable.getTableName() + "." + leftTableJoinColumn.getColumnName())
				.equal()
				.field(rightTable.getTableName() + "." + rightTable.getPrimaryKeyColumn().getColumnName());

		if (TypeTable.class.isInstance(rightTable)) {
			TypeTable tt = TypeTable.class.cast(rightTable);
			List<AttributeColumn> columns = tt.getColumns();
			select.expression()
					.table(rightTable.getTableName())
					.field(rightTable.getPrimaryKeyColumn().getColumnName())
					.as(rightTable.getTableName() + "_id");

			for (AttributeColumn attributeColumn : columns) {
				String alias = rightTable.getTableName() + "_" + attributeColumn.getColumnName();
				select.expression()
						.table(rightTable.getTableName())
						.field(attributeColumn.getColumnName())
						.as(alias);
			}
		} else if (JoinTable.class.isInstance(rightTable)) {
			JoinTable jt = JoinTable.class.cast(rightTable);
			appendTableAsLeftJoinForSelect(jt, join, select);
		} else {
			// crap
			throw new IllegalStateException("unsupported joined table.");
		}
	}

	@Override
	public QueryByExample queryByExample(String namedAttributeHolderName, RecordContext context) {
		NamedAttributeHolder nah;
		JoinTable joinTable = engine.getTableRegistry().getJoinTableByNamedAttributeHolder(namedAttributeHolderName);
		if (joinTable != null) {
			nah = joinTable.getNamedAttributeHolder();
		} else {
			TypeTable typeTable = engine.getTableRegistry().getTypeTableByType(namedAttributeHolderName);
			if (typeTable != null) {
				nah = typeTable.getType();
			} else {
				throw new UnknownNamedAttributeHolderException(namedAttributeHolderName);
			}
		}
		return new QueryByExampleImpl(nah, engine, context);
	}

	@Override
	public void iterate(String typeName, QueryByExampleIteratorListener listener, final int batchSize, boolean eager, RecordContext recordContext) {
		QueryByExample qbe = engine.getAccessor().queryByExample(typeName, recordContext);
		long total = qbe.count();
		int start = 0;
		while (start < total) {
			final int s = start;
			QueryByExample q = engine.getAccessor().queryByExample(typeName, recordContext).pagination(new Pagination() {
				@Override
				public Long getOffset() {
					return new Long(s);
				}

				@Override
				public Long getSize() {
					return new Long(batchSize);
				}
			});
			if (eager) {
				q.eager(); // use eager loading carefully! you might end up with a lot of JOINs between tables.
			}
			List<Record> records = q.all();

			for (Record record : records) {
				listener.handleRecord(record);
			}
			start += batchSize;
			total = qbe.count();
		}
	}

	public void setMediatorRegistry(MediatorRegistryImpl mediatorRegistry) {
		this.mediatorRegistry = mediatorRegistry;
	}

	public void setEngine(EngineImpl engine) {
		this.engine = engine;
	}

	public void setExpressionStatementHandler(ExpressionStatementHandler expressionStatementHandler) {
		this.expressionStatementHandler = expressionStatementHandler;
	}

	@Override
	public Iterator<Record> query(final String nQuery, final RecordContext recordContext, final LoadedAttributes userDefinedLoadedAttributes, final Object... queryArgs) {
		Pick pick = parseQuery(nQuery, Pick.class, queryArgs);
		assertTableExistsForTypeOrMixin(pick.getAttributeHolderName());

		String alias = pick.getAttributeHolderNameAlias();
		IfClause ifClause = pick.getIfClause();
		Ordering ordering = pick.getOrdering();

		// iterate over the ifclause and check which attributes are required to perform the query
		final Set<String> requiredAttributes = ifClause == null 
				? (ordering == null ? Collections.EMPTY_SET : new HashSet<String>()) 
				: _collectRequiredAttributesFromBooleanStatement(new HashSet<String>(), alias, ifClause.getNext());

		QueryContextFactory qcf = engine.getQueryContextFactory();
		QueryContext qc = qcf.buildQueryContext();
		Select select = qc.select();

		String orderByField = null;
		if (ordering != null) {
			String field = ordering.getField();
			orderByField = Util.stripEntityAliasPrefix(alias, field);
			addOrderByFieldToRequiredAttributes(requiredAttributes, orderByField);
		}

		final LoadedAttributes loadedAttributes = new LazyLoadedAttributes() {

			@Override
			public LoadedAttributes.Strategy isLoaded(Attribute attribute, String attributePath) {
				LoadedAttributes.Strategy strategy = super.isLoaded(attribute, attributePath);
				LoadedAttributes.Strategy userDefinedStrategy = userDefinedLoadedAttributes == null ? null : userDefinedLoadedAttributes.isLoaded(attribute, attributePath);
				if (userDefinedStrategy != null) {
					// TODO: let the used create customized queries
					userDefinedStrategy = userDefinedStrategy;
				}
				if ((strategy == LoadedAttributes.Strategy.LAZY_LOADED || strategy == LoadedAttributes.Strategy.NOT_LOADED) && requiredAttributes.contains(attributePath)) {
					strategy = LoadedAttributes.Strategy.LOADED;
				}
				return strategy;
			}

		};

		LoadingIterator iterator = new LoadingIterator(engine.getTableRegistry(), select, loadedAttributes, requiredAttributes);
		iterator.iterate(pick.getAttributeHolderName());

		if (ifClause != null) {
			// for each binding will be having its own OR condition in the where clause
			List<MappingBinding> bindings = iterator.getMappingBindings();
			createWhereClauseForBindings(ifClause, select, bindings, alias == null ? null : alias + ".");
		}

		if (ordering != null) {
			OrderBy ob = select.orderBy();
			createOrderBy(ob, iterator.getMappingBindings(), orderByField);
			ob.direction(ordering.isAscending() ? "ASC" : "DESC");
		} else {
			OrderBy ob = select.orderBy();
			// order by primary key of root table if nothing is defined
			ob.columnAlias(iterator.getRootMappingBinding().getTableAlias() + "." + iterator.getRootMappingBinding().getPrimaryKeyAlias().getAttributeColumn().getColumnName());
			ob.direction("ASC");
		}

		Long limit = pick.getLimit();
		if (limit != null) {
			select.limit(limit);
		}
		Long offset = pick.getOffset();
		if (offset != null) {
			select.offset(offset);
		}

		qc.setExternalMappingBindingsProvider(iterator);
		Query q = qc.build(recordContext);
		Transaction tx = engine.getQueryRunner().createTransaction();
		ObjectReference<List<Record>> items = tx.getQueryRunner().list(q);
		tx.commit();
		List<Record> itemsList = items.get();
		return itemsList == null ? null : itemsList.iterator();
	}

	@Override
	public Iterator<Record> query(String nQuery, Object... queryArgs) {
		return query(nQuery, buildRecordContext(), ((LoadedAttributes) null), queryArgs);
	}

	@Override
	public Long count(String nQuery, Object... queryArgs) {
		Count count = parseQuery(nQuery, Count.class, queryArgs);
		assertTableExistsForTypeOrMixin(count.getAttributeHolderName());
		String alias = count.getAttributeHolderNameAlias();
		IfClause ifClause = count.getIfClause();

		// iterate over the ifclause and check which attributes are required to perform the query
		final Set<String> requiredAttributes = ifClause == null ? Collections.EMPTY_SET : _collectRequiredAttributesFromBooleanStatement(new HashSet<String>(), alias, ifClause.getNext());

		QueryContextFactory qcf = engine.getQueryContextFactory();
		QueryContext qc = qcf.buildQueryContext();
		Select select = qc.select().count();

		LoadedAttributes loadedAttributes = new LazyLoadedAttributes() {

			@Override
			public LoadedAttributes.Strategy isLoaded(Attribute attribute, String attributePath) {
				LoadedAttributes.Strategy strategy = super.isLoaded(attribute, attributePath);
				if ((strategy == LoadedAttributes.Strategy.LAZY_LOADED || strategy == LoadedAttributes.Strategy.NOT_LOADED) && requiredAttributes.contains(attributePath)) {
					strategy = LoadedAttributes.Strategy.LOADED;
				}
				return strategy;
			}

		};
		LoadingIterator iterator = new LoadingIterator(engine.getTableRegistry(), select, loadedAttributes, requiredAttributes);
		iterator.iterate(count.getAttributeHolderName(), true);

		if (ifClause != null) {
			// for each binding will be having its own OR condition in the where clause
			List<MappingBinding> bindings = iterator.getMappingBindings();
			createWhereClauseForBindings(ifClause, select, bindings, alias == null ? null : alias + ".");
		}

		RecordContext recordContext = buildRecordContext();
		Query q = qc.build(recordContext);
		Transaction tx = engine.getQueryRunner().createTransaction();
		ObjectReference<Long> countNumber = tx.getQueryRunner().number(q);
		tx.commit();
		return countNumber.get();
	}

	private <E extends org.bndly.schema.api.nquery.Query> E parseQuery(String nQuery, Class<E> queryType, Object... queryArgs) {
		org.bndly.schema.api.nquery.Query query;
		try {
			query = new ParserImpl(queryArgs).expressionStatementHandler(expressionStatementHandler).parse(nQuery).getQuery();
		} catch (QueryParsingException ex) {
			throw new SchemaException("failed to parse query string: " + ex.getMessage(), ex);
		}
		if (!queryType.isInstance(query)) {
			throw new SchemaException("provided query is not a " + queryType.getSimpleName());
		}
		return queryType.cast(query);
	}

	private void assertTableExistsForTypeOrMixin(String attributeHolderName) {
		TableRegistry tr = engine.getTableRegistry();
		Table table = tr.getJoinTableByNamedAttributeHolder(attributeHolderName);
		if (table == null) {
			table = tr.getTypeTableByType(attributeHolderName);
		}
		if (table == null) {
			throw new SchemaException("could not find table for type/mixin " + attributeHolderName);
		}
	}

	private Set<String> _collectRequiredAttributesFromBooleanStatement(final Set<String> set, final String alias, BooleanStatement booleanStatement) {
		if (booleanStatement != null) {
			RequiredAttribtuesInspector.Context ctx = new RequiredAttribtuesInspector.Context() {

				@Override
				public void collectRequiredAttributesFromBooleanStatement(BooleanStatement booleanStatement) {
					_collectRequiredAttributesFromBooleanStatement(set, alias, booleanStatement);
				}
			};
			BooleanStatement current = booleanStatement;
			while (current != null) {
				resolveInspector(current).collectRequiredAttributesFromBooleanStatement(set, alias, current, ctx);
				current = current.getNext();
			}
		}
		return set;
	}

	private void createWhereClauseForBindings(IfClause ifClause, Select select, List<MappingBinding> bindings, final String prefix) {
		Where where = select.where();
		Expression exp = null;
		for (final MappingBinding binding : bindings) {
			if (exp == null) {
				exp = where.expression();
			} else {
				exp = exp.or();
			}
			WrapperExpression wrap = exp.wrap();
			Expression wrapped = wrap.wrapped();
			final Stack<Expression> expressionStack = new Stack<>();
			expressionStack.push(wrap);
			expressionStack.push(wrapped);
			BooleanStatementIterator.iterate(ifClause.getNext(), new BooleanStatementIterator.NoOpCallback() {

				@Override
				public void onBooleanStatement(BooleanStatement booleanStatement, BooleanOperator operator) {
					applyBooleanStatement(booleanStatement, binding, prefix);
					Expression peek = expressionStack.pop();
					if (BooleanOperator.AND.equals(operator)) {
						expressionStack.push(peek.and());
					} else if (BooleanOperator.OR.equals(operator)) {
						expressionStack.push(peek.or());
					} else {
						throw new IllegalStateException("unsupported boolean operator");
					}
				}

				@Override
				public void onLastBooleanStatement(BooleanStatement booleanStatement) {
					applyBooleanStatement(booleanStatement, binding, prefix);
				}

				@Override
				public void onWrapperOpened(WrapperBooleanStatement wrapper) {
					Expression peek = expressionStack.pop();
					WrapperExpression wrp = peek.wrap();
					expressionStack.push(wrp);
					expressionStack.push(wrp.wrapped());
				}

				@Override
				public void onWrapperClosed(WrapperBooleanStatement wrapper) {
					expressionStack.pop();
				}

				void applyBooleanStatement(BooleanStatement booleanStatement, final MappingBinding mappingBinding, final String prefix) {
					if (WrapperBooleanStatement.class.isInstance(booleanStatement)) {
						// no-op
					} else {
						resolveSQLMapper(booleanStatement).map(booleanStatement, expressionStack.peek(), prefix, mappingBinding, new BooleanStatementSQLMapper.Context() {

							@Override
							public void map(BooleanStatement booleanStatement) {
								applyBooleanStatement(booleanStatement, mappingBinding, prefix);
							}

							@Override
							public TableRegistry getTableRegistry() {
								return engine.getTableRegistry();
							}

						});
					}
				}

			});
		}
	}

	private RequiredAttribtuesInspector resolveInspector(BooleanStatement booleanStatement) {
		Class<? extends BooleanStatement> key = booleanStatement.getClass();
		RequiredAttribtuesInspector insp = null;
		if (!requiredAttributesInspectorsByStatementType.containsKey(key)) {
			for (RequiredAttribtuesInspector inspector : requiredAttributesInspectors) {
				if (inspector.supports(booleanStatement)) {
					insp = inspector;
					break;
				}
			}
			requiredAttributesInspectorsByStatementType.put(key, insp);
		} else {
			insp = requiredAttributesInspectorsByStatementType.get(key);
		}
		if (insp == null) {
			throw new SchemaException("no required attributes inspector found for boolean statement");
		}
		return insp;
	}

	private BooleanStatementSQLMapper resolveSQLMapper(BooleanStatement booleanStatement) {
		Class<? extends BooleanStatement> key = booleanStatement.getClass();
		BooleanStatementSQLMapper mpr = null;
		if (!sqlMappersByStatementType.containsKey(key)) {
			for (BooleanStatementSQLMapper mapper : booleanStatementSQLMappers) {
				if (mapper.supports(booleanStatement)) {
					mpr = mapper;
					break;
				}
			}
			sqlMappersByStatementType.put(key, mpr);
		} else {
			mpr = sqlMappersByStatementType.get(key);
		}
		if (mpr == null) {
			throw new SchemaException("no required attributes inspector found for boolean statement");
		}
		return mpr;
	}

	private void createOrderBy(OrderBy orderBy, List<MappingBinding> mappingBindings, String orderByField) {
		int index = orderByField.indexOf(".");
		if (index > 0) {
			String currentElement = orderByField.substring(0, index);
			if (mappingBindings != null) {
				for (MappingBinding mappingBinding : mappingBindings) {
					Map<String, List<MappingBinding>> subs = mappingBinding.getSubBindings();
					if (subs != null) {
						String nextElement = orderByField.substring(index + 1);
						createOrderBy(orderBy, subs.get(currentElement), nextElement);
					}
				}
			}
		} else {
			for (MappingBinding mappingBinding : mappingBindings) {
				List<AliasBinding> aliases = mappingBinding.getAliases();
				if ("id".equals(orderByField)) {
					AliasBinding pkAlias = mappingBinding.getPrimaryKeyAlias();
					orderBy.columnAlias(pkAlias.getAlias());
				} else {
					if (aliases != null) {
						for (AliasBinding alias : aliases) {
							if (alias.getAttribute().getName().equals(orderByField)) {
								orderBy.columnAlias(alias.getAlias());
							}
						}
					}
				}
			}
		}
	}

	private void addOrderByFieldToRequiredAttributes(Set<String> requiredAttributes, String orderByField) {
		requiredAttributes.add(orderByField);
		int i = orderByField.lastIndexOf(".");
		if (i > 0) {
			addOrderByFieldToRequiredAttributes(requiredAttributes, orderByField.substring(0, i));
		}
	}
}
