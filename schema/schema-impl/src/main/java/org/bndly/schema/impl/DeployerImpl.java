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

import org.bndly.schema.api.AttributeMediator;
import org.bndly.schema.api.tx.Template;
import org.bndly.schema.api.tx.TransactionCallback;
import org.bndly.schema.api.tx.TransactionStatus;
import org.bndly.schema.api.tx.TransactionTemplate;
import org.bndly.schema.api.ForeignKeyConstraint;
import org.bndly.schema.api.db.AttributeColumn;
import org.bndly.schema.api.db.DeploymentState;
import org.bndly.schema.api.db.Index;
import org.bndly.schema.api.db.JoinTable;
import org.bndly.schema.api.db.Table;
import org.bndly.schema.api.db.TypeTable;
import org.bndly.schema.api.db.UniqueConstraintTable;
import org.bndly.schema.api.exception.SchemaException;
import org.bndly.schema.api.listener.SchemaDeploymentListener;
import org.bndly.schema.api.services.ConstraintRegistry;
import org.bndly.schema.api.services.Deployer;
import org.bndly.schema.impl.db.AttributeColumnImpl;
import org.bndly.schema.impl.db.IndexImpl;
import org.bndly.schema.impl.db.JoinTableImpl;
import org.bndly.schema.impl.db.TableImpl;
import org.bndly.schema.impl.db.TypeTableImpl;
import org.bndly.schema.impl.db.UniqueConstraintTableImpl;
import org.bndly.schema.vendor.VendorConfiguration;
import org.bndly.schema.model.Attribute;
import org.bndly.schema.model.InverseAttribute;
import org.bndly.schema.model.Mixin;
import org.bndly.schema.model.MixinAttribute;
import org.bndly.schema.model.NamedAttributeHolder;
import org.bndly.schema.model.NamedAttributeHolderAttribute;
import org.bndly.schema.model.Schema;
import org.bndly.schema.model.SchemaUtil;
import org.bndly.schema.model.Type;
import org.bndly.schema.model.TypeAttribute;
import org.bndly.schema.model.UniqueConstraint;
import org.bndly.schema.vendor.AntiSQLInject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeployerImpl implements Deployer, Resetable {

	private static final Logger LOG = LoggerFactory.getLogger(DeployerImpl.class);
	
	private TransactionTemplate transactionTemplate;
	private TableRegistryImpl tableRegistry;
	private MediatorRegistryImpl mediatorRegistry;
	private Schema schema;
	private Map<String, UniqueConstraint> uniqueConstraintsByName;
	private List<String> uniqueConstraintNames;
	private Set<String> tablesForNamedAttributeHolders;
	private List<ForeignKeyConstraint> fkConstraints;
	private List<Index> indices;
	private List<SchemaDeploymentListener> listeners;
	private ReadWriteLock deploymentListenersLock;
	private ConstraintRegistry constraintRegistry;
	private VendorConfiguration vendorConfiguration;
	private String internalDatabaseSchemaName;
	private StringBuffer deploymentSQL;
	private boolean validateOnly;
	private final EngineImpl engine;
	
	private static final String ALLOWED_IDENTIFIER_CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_";
	private boolean validationErrorIgnored;
	private static final AntiSQLInject ANTI_SQL_INJECT = new AntiSQLInject() {
		@Override
		public String filterCharactersForSQLIdentifier(String identifierToFilter) {
			StringBuffer sb = new StringBuffer();
			for (int index = 0; index < identifierToFilter.length(); index++) {
				char character = identifierToFilter.charAt(index);
				if (ALLOWED_IDENTIFIER_CHARACTERS.indexOf(character) > -1) {
					sb.append(character);
				} else {
					throw new IllegalArgumentException("found unallowed character for identifier name: " + character);
				}
			}
			String result = sb.toString();
			return result;
		}
	};

	public DeployerImpl(EngineImpl engine) {
		if (engine == null) {
			throw new IllegalArgumentException("deployer requires an engine");
		}
		this.engine = engine;
	}

	public void setValidateOnly(boolean validateOnly) {
		this.validateOnly = validateOnly;
	}

	public void setValidationErrorIgnored(boolean validationErrorIgnored) {
		this.validationErrorIgnored = validationErrorIgnored;
	}

	@Override
	public void reset() {
		tablesForNamedAttributeHolders = new HashSet<>();
		fkConstraints = new ArrayList<>();
		deploymentSQL = new StringBuffer();
		uniqueConstraintsByName = new HashMap<>();
		uniqueConstraintNames = new ArrayList<>();
		indices = new ArrayList<>();
	}

	@Override
	public Schema getDeployedSchema() {
		return schema;
	}

	@Override
	public void deploy(Schema schema) {
		try {
			this.schema = schema;
			reset();
			createTables();
			createMixinTables();
			createUniqueConstraintTables();
			addAttributesToTables();
			addForeignKeyConstraints();
			addIndicesOnColumns();
			
			uniqueConstraintsByName = Collections.unmodifiableMap(uniqueConstraintsByName);
			uniqueConstraintNames = Collections.unmodifiableList(new ArrayList<>(uniqueConstraintsByName.keySet()));
			
			if (listeners != null) {
				deploymentListenersLock.readLock().lock();
				try {
					for (SchemaDeploymentListener schemaDeploymentListener : listeners) {
						schemaDeploymentListener.schemaDeployed(schema, engine);
					}
				} finally {
					deploymentListenersLock.readLock().unlock();
				}
			}
			List<SchemaDeploymentListener> listenersTwo = engine.getListeners(SchemaDeploymentListener.class, null);
			for (SchemaDeploymentListener schemaDeploymentListener : listenersTwo) {
				schemaDeploymentListener.schemaDeployed(schema, engine);
			}
		} catch (RuntimeException e) {
			LOG.error("failed to deploy schema " + schema.getName(), e);
			throw e;
		}
	}

	@Override
	public List<String> getUniqueConstraintNames() {
		return uniqueConstraintNames;
	}

	@Override
	public UniqueConstraint getUniqueConstraintByName(String name) {
		return uniqueConstraintsByName.get(name);
	}

	@Override
	public String getDeploymentSQL() {
		return deploymentSQL == null ? null : deploymentSQL.toString();
	}

	private void createTables() {
		List<Type> types = schema.getTypes();
		if (types != null) {
			for (Type type : types) {
				createTable(type);
			}
		}
	}

	private String getTableNameTransformed(NamedAttributeHolder attributeHolder) {
		if (Type.class.isInstance(attributeHolder)) {
			return vendorConfiguration.getIdentifierAdapter().transformTableName(attributeHolder.getName());
		} else if (Mixin.class.isInstance(attributeHolder)) {
			return vendorConfiguration.getIdentifierAdapter().transformTableName(attributeHolder.getName());
		} else {
			throw new IllegalStateException("unsupported attribute holder");
		}
	}

	private String getTypeTableName(Type type) {
		return shortenIdentifier(getTableNameTransformed(type));
	}
	
	private String getJoinTableName(Type type) {
		return shortenIdentifier(vendorConfiguration.getIdentifierAdapter().transformTableName("JOIN_" + type.getName()));
	}

	private String getMixinTableName(Mixin mixin) {
		return shortenIdentifier(vendorConfiguration.getIdentifierAdapter().transformTableName("MIXIN_" + mixin.getName()));
	}
	
	public static String filterCharactersForSQLIdentifier(String input) {
		return ANTI_SQL_INJECT.filterCharactersForSQLIdentifier(input);
	}

	private void createTable(Type type) {
		if (type.isVirtual()) {
			return;
		}
		if (tablesForNamedAttributeHolderNotHandled(type)) {
			// go up the type hierarchy
			// this way we can assume,
			// that joinTables for super 
			// types already exist.
			Type superType = type.getSuperType();
			if (superType != null) {
				createTable(superType);
			}

			Table tableToInjectInParentJoinTable = null;

			// create the table for the actual attributes
			if (!type.isAbstract()) {
				final String tableName = getTypeTableName(type);
				TypeTableImpl table = tableRegistry.createTypeTable(type, tableName);
				DeploymentState state = assertTableExists(table);

				table.setState(state);
				if (superType != null) {
					tableToInjectInParentJoinTable = table;
				}
			}

			// go down the type hierarchy
			List<Type> subTypes = type.getSubTypes();
			if (subTypes != null && !subTypes.isEmpty()) {
				// create a join table
				final String tableName = getJoinTableName(type);
				JoinTableImpl joinTable = tableRegistry.createJoinTable(type, tableName);
				DeploymentState state = assertTableExists(joinTable);
				joinTable.setState(state);

				if (!type.isAbstract()) {
					joinTable.getJoinedTables().add(tableRegistry.getTypeTableByType(type));
				}

				for (Type subType : subTypes) {
					createTable(subType);
				}
				if (superType != null) {
					tableToInjectInParentJoinTable = joinTable;
				}
			}

			if (tableToInjectInParentJoinTable != null) {
				JoinTable parentJoinTable = tableRegistry.getJoinTableByNamedAttributeHolder(superType);
				parentJoinTable.getJoinedTables().add(tableToInjectInParentJoinTable);
			}
		}
	}

	private void createUniqueConstraintTables() {
		List<UniqueConstraint> uq = schema.getUniqueConstraints();
		if (uq != null) {
			for (UniqueConstraint uniqueConstraint : uq) {
				StringBuilder sb = new StringBuilder()
					.append("UQ_")
					.append(uniqueConstraint.getHolder().getName());
				for (Attribute attribute : uniqueConstraint.getAttributes()) {
					sb
						.append("_")
						.append(attribute.getName());
				}
				final String tableName = shortenIdentifier(vendorConfiguration.getIdentifierAdapter().transformTableName(sb.toString()));
				UniqueConstraintTableImpl table = tableRegistry.createUniqueConstraintTable(uniqueConstraint, tableName);
				createTableWithName(table, false);
			}
		}
	}

	private void addAttributesToTables() {
		for (TypeTable typeTable : tableRegistry.getAllTypeTables()) {
			Type type = typeTable.getType();
			List<Attribute> attributes = SchemaUtil.collectAttributes(type);
			for (Attribute attribute : attributes) {
				createAttributeForTable(attribute, typeTable);
			}
		}
		for (JoinTable joinTable : tableRegistry.getAllJoinTables()) {
			List<Table> joinedTables = joinTable.getJoinedTables();
			for (Table joinedTable : joinedTables) {
				if (TypeTable.class.isInstance(joinedTable)) {
					TypeTable tt = TypeTable.class.cast(joinedTable);
					TypeAttribute attribute = new TypeAttribute();
					attribute.setType(tt.getType());
					attribute.setName(tt.getType().getName());
					createAttributeForTable(attribute, joinTable);
				} else if (JoinTable.class.isInstance(joinedTable)) {
					JoinTable jt = JoinTable.class.cast(joinedTable);
					NamedAttributeHolder holder = jt.getNamedAttributeHolder();
					Attribute attribute;
					if (Type.class.isInstance(holder)) {
						Type type = Type.class.cast(holder);
						attribute = new TypeAttribute();
						((TypeAttribute) attribute).setType(type);
					} else if (Mixin.class.isInstance(holder)) {
						Mixin mixin = Mixin.class.cast(holder);
						attribute = new MixinAttribute();
						((MixinAttribute) attribute).setMixin(mixin);
					} else {
						throw new IllegalStateException("unsupported named attribute holder");
						// crap
					}
					attribute.setName(holder.getName());
					createAttributeForTable(attribute, joinTable);
				}
			}
		}
		for (UniqueConstraintTable uniqueConstraintTable : tableRegistry.getAllUniqueConstraintTables()) {
			UniqueConstraint uniqueConstraint = uniqueConstraintTable.getUniqueConstraint();
			NamedAttributeHolder holder = uniqueConstraint.getHolder();
			JoinTable joinTable = tableRegistry.getJoinTableByNamedAttributeHolder(holder);
			if (joinTable != null) {
				createPrimaryKeyAttributesForUniqueConstraintTable(joinTable, uniqueConstraintTable);
			} else {
				TypeTable typeTable = tableRegistry.getTypeTableByType(holder.getName());
				createPrimaryKeyAttributesForUniqueConstraintTable(typeTable, uniqueConstraintTable);
			}

			ArrayList<AttributeColumn> cols = new ArrayList<>();
			for (Attribute attribute : uniqueConstraint.getAttributes()) {
				AttributeColumn col = createAttributeForTable(attribute, uniqueConstraintTable);
				if (col != null) {
					cols.add(col);
				}
			}
			final String constraintName = "CONST_" + uniqueConstraintTable.getTableName();
			final String constraintNameTransformed = shortenIdentifier(vendorConfiguration.getIdentifierAdapter().transformConstraintName(constraintName));
			makeColumnsUnique(constraintNameTransformed, uniqueConstraintTable, cols.toArray(new AttributeColumn[cols.size()]));
			uniqueConstraintsByName.put(constraintNameTransformed, uniqueConstraint);
		}
	}

	private String shortenIdentifier(final String identifierName) {
		LOG.debug("testing identifier length: '{}'", identifierName);
		int hc = identifierName.hashCode();
		if (hc < 0) {
			hc *= -1;
		}
		String hcString = "" + hc;
		int hashLength = hcString.length();
		int maxLength = vendorConfiguration.getIdentifierAdapter().getIdentifierMaxLength();
		/*
		            01234567
		identifier: ABCDEFGH
		length:     8
		maxlength:  6
		hashlength: 4
		result:     AB####
		*/
		if (identifierName.length() > maxLength) {
			LOG.debug("identifier length {} > {} of exceeded by '{}' {}", identifierName.length(), maxLength, identifierName);
			String shortened = identifierName.substring(0, maxLength - hashLength) + hcString;
			LOG.debug("shortened identifier '{}' to '{}'", identifierName, shortened);
			return shortened;
		} else {
			LOG.debug("no shortening for identifier '{}'", identifierName);
			return identifierName;
		}
	}

	private void makeColumnsUnique(String constraintName, Table table, AttributeColumn... columns) {
		if (isConstraintDefinedOnTable(constraintName, table)) {
			return;
		}
		StringBuilder sb = new StringBuilder()
		.append("ALTER TABLE ")
		.append(filterCharactersForSQLIdentifier(table.getTableName()))
		.append(" ADD CONSTRAINT ")
		.append(filterCharactersForSQLIdentifier(constraintName))
		.append(" UNIQUE(");
		boolean first = true;
		for (AttributeColumn col : columns) {
			if (col != null) {
				if (!first) {
					sb.append(',');
				}
				sb.append(filterCharactersForSQLIdentifier(col.getColumnName()));
				first = false;
			}
		}
		sb.append(")");
		// ALTER TABLE TEST ADD CONSTRAINT NAME_UNIQUE UNIQUE(NAME)
		final String constraintSql = sb.toString();
		transactionTemplate.doInTransaction(new TransactionCallback() {

			@Override
			public Object doInTransaction(TransactionStatus transactionStatus, Template template) {
				template.execute(constraintSql);
				return null;
			}
		});
	}

	private void createPrimaryKeyAttributesForUniqueConstraintTable(Table table, UniqueConstraintTable uniqueConstraintTable) {
		if (JoinTable.class.isInstance(table)) {
			JoinTable jt = JoinTable.class.cast(table);
			for (Table table1 : jt.getJoinedTables()) {
				createPrimaryKeyAttributesForUniqueConstraintTable(table1, uniqueConstraintTable);
			}
		} else if (TypeTable.class.isInstance(table)) {
			TypeTable tt = TypeTable.class.cast(table);
			TypeAttribute ta = new TypeAttribute();
			ta.setType(tt.getType());
			ta.setName(tt.getType().getName());
			ta.setMandatory(false);
			AttributeColumn col = createAttributeForTable(ta, uniqueConstraintTable);
			uniqueConstraintTable.getHolderColumns().add(col);
			constraintRegistry.addUniqueConstraintsForType(uniqueConstraintTable.getUniqueConstraint(), tt.getType(), col);

			StringBuffer sb = new StringBuffer();
			sb
					.append("FKNQ_")
					.append(filterCharactersForSQLIdentifier(uniqueConstraintTable.getTableName()))
					.append("_")
					.append(filterCharactersForSQLIdentifier(col.getColumnName()))
					.append("_TO_")
					.append(filterCharactersForSQLIdentifier(tt.getTableName()))
					.append("_")
					.append(filterCharactersForSQLIdentifier(tt.getPrimaryKeyColumn().getColumnName()));
			String name = sb.toString();
			final String nameTransformed = shortenIdentifier(vendorConfiguration.getIdentifierAdapter().transformConstraintName(name));
			ForeignKeyConstraint fk = new ForeignKeyConstraint(nameTransformed);
			fk.setOnDelete(ForeignKeyConstraint.OnDelete.CASCADE);
			fk.setReferencingColumn(col);
			fk.setReferencingTable(uniqueConstraintTable);
			Table referencedTable = tt;
			fk.setReferencedTable(referencedTable);
			fk.setReferencedColumn(referencedTable.getPrimaryKeyColumn());
			fkConstraints.add(fk);

			String constraintName = 
					"CON_" 
					+ filterCharactersForSQLIdentifier(uniqueConstraintTable.getTableName())
					+ filterCharactersForSQLIdentifier(getTableNameTransformed(tt.getType()))
					;
			final String constraintNameTransformed = shortenIdentifier(vendorConfiguration.getIdentifierAdapter().transformConstraintName(constraintName));
			makeColumnsUnique(constraintNameTransformed, uniqueConstraintTable, col);
		}
	}
	
	private void createMixinTables() {
		List<Mixin> mixins = schema.getMixins();
		if (mixins != null) {
			for (Mixin mixin : mixins) {
				if (mixin.isVirtual()) {
					continue;
				}
				if (tablesForNamedAttributeHolderNotHandled(mixin)) {
					List<Type> joined = mixin.getMixedInto();
					if (joined != null && !joined.isEmpty()) {
						final String tableName = getMixinTableName(mixin);//"mixin_" + name;
						JoinTableImpl jt = tableRegistry.createJoinTable(mixin, tableName);
						DeploymentState state = assertTableExists(jt);
						jt.setState(state);
						for (Type joinedType : joined) {
							List<Type> joinedTypeSubTypes = joinedType.getSubTypes();
							Table table;
							if (joinedTypeSubTypes != null && !joinedTypeSubTypes.isEmpty()) {
								table = tableRegistry.getJoinTableByNamedAttributeHolder(joinedType);
							} else {
								table = tableRegistry.getTypeTableByType(joinedType);
							}
							jt.getJoinedTables().add(table);
						}
					}
				}
			}
		}
	}

	private void addForeignKeyConstraints() {
		for (JoinTable joinTable : tableRegistry.getAllJoinTables()) {
			addForeignKeyConstraintsForTable(joinTable, true);
		}
		for (TypeTable typeTable : tableRegistry.getAllTypeTables()) {
			addForeignKeyConstraintsForTable(typeTable, false);
		}
		for (UniqueConstraintTable uniqueConstraintTable : tableRegistry.getAllUniqueConstraintTables()) {
			addForeignKeyConstraintsForTable(uniqueConstraintTable, true);
		}

		for (ForeignKeyConstraint foreignKeyConstraint : fkConstraints) {
			String constraintName = foreignKeyConstraint.getName();
			Table table = foreignKeyConstraint.getReferencingTable();
			
			StringBuilder sb = new StringBuilder()
				.append("ALTER TABLE ")
				.append(filterCharactersForSQLIdentifier(foreignKeyConstraint.getReferencingTable().getTableName()))
				.append(" ADD CONSTRAINT ")
				.append(filterCharactersForSQLIdentifier(constraintName))
				.append(" FOREIGN KEY (")
				.append(filterCharactersForSQLIdentifier(foreignKeyConstraint.getReferencingColumn().getColumnName()))
				.append(") REFERENCES ")
				.append(filterCharactersForSQLIdentifier(foreignKeyConstraint.getReferencedTable().getTableName()))
				.append("(")
				.append(filterCharactersForSQLIdentifier(foreignKeyConstraint.getReferencedColumn().getColumnName()))
				.append(")");
			ForeignKeyConstraint.OnDelete onDelete = foreignKeyConstraint.getOnDelete();
			if (onDelete == ForeignKeyConstraint.OnDelete.CASCADE) {
				sb.append(" ON DELETE CASCADE");
			} else if (onDelete == ForeignKeyConstraint.OnDelete.SETNULL) {
				sb.append(" ON DELETE SET NULL");
			}
			final String sql = sb.toString();
			deploymentSQL.append(sql).append(";\n");
			
			if (isConstraintDefinedOnTable(constraintName, table)) {
				LOG.info("constraint {} is already defined", constraintName);
				continue;
			} else if (validateOnly) {
				if (validationErrorIgnored) {
					LOG.debug("constraint did not exist: {}", constraintName);
				} else {
					throw new SchemaException("constraint did not exist: " + constraintName);
				}
			}
			transactionTemplate.doInTransaction(new TransactionCallback() {

				@Override
				public Object doInTransaction(TransactionStatus transactionStatus, Template template) {
					template.execute(sql);
					return null;
				}
			});
		}
	}

	private void addForeignKeyConstraintsForTable(Table table, boolean cascadeDelete) {
		for (AttributeColumn attributeColumn : table.getColumns()) {
			Attribute att = attributeColumn.getAttribute();
			if (NamedAttributeHolderAttribute.class.isInstance(att)) {
				NamedAttributeHolderAttribute namedAttributeHolderAttribute = (NamedAttributeHolderAttribute) att;
				NamedAttributeHolder referenced = namedAttributeHolderAttribute.getNamedAttributeHolder();
				Table referencedTable = tableRegistry.getJoinTableByNamedAttributeHolder(referenced);
				if (referencedTable == null) {
					referencedTable = tableRegistry.getTypeTableByType(referenced.getName());
				}
				if (referencedTable != null) {
					StringBuffer sb = new StringBuffer();
					sb
							.append("FKTR_")
							.append(table.getTableName())
							.append("_")
							.append(attributeColumn.getColumnName())
							.append("_TO_")
							.append(referencedTable.getTableName())
							.append("_")
							.append(referencedTable.getPrimaryKeyColumn().getColumnName());
					String name = sb.toString();
					final String nameTransformed = shortenIdentifier(vendorConfiguration.getIdentifierAdapter().transformConstraintName(name));
					ForeignKeyConstraint fk = new ForeignKeyConstraint(nameTransformed);
					ForeignKeyConstraint.OnDelete onDelete = null;
					if (cascadeDelete) {
						onDelete = ForeignKeyConstraint.OnDelete.CASCADE;
					}
					if (namedAttributeHolderAttribute.getCascadeDelete() == null && namedAttributeHolderAttribute.getNullOnDelete() == null) {
						Type currentType = TypeTable.class.isInstance(table) ? ((TypeTable) table).getType() : null;
						// look for an inverse attribute on the current attribute
						List<Attribute> attributes = namedAttributeHolderAttribute.getNamedAttributeHolder().getAttributes();
						if (attributes != null) {
							for (Attribute attribute : attributes) {
								if (InverseAttribute.class.isInstance(attribute)) {
									InverseAttribute ia = (InverseAttribute) attribute;
									if (
										ia.getReferencedAttributeHolder() == currentType 
										&& ia.getReferencedAttributeName().equals(namedAttributeHolderAttribute.getName())
									) {
										if (ia.getDeleteOrphans() != null && ia.getDeleteOrphans()) {
											namedAttributeHolderAttribute.setCascadeDelete(true);
											onDelete = ForeignKeyConstraint.OnDelete.CASCADE;
										}
										break;
									}
								}
							}
						}
					} else if (namedAttributeHolderAttribute.getCascadeDelete() != null && namedAttributeHolderAttribute.getNullOnDelete() == null) {
						if (namedAttributeHolderAttribute.getCascadeDelete()) {
							onDelete = ForeignKeyConstraint.OnDelete.CASCADE;
						}
					} else if (namedAttributeHolderAttribute.getCascadeDelete() == null && namedAttributeHolderAttribute.getNullOnDelete() != null) {
						if (namedAttributeHolderAttribute.getNullOnDelete()) {
							onDelete = ForeignKeyConstraint.OnDelete.SETNULL;
						}
					} else {
						throw new IllegalStateException("either cascade delete or set null on delete has to be used");
					}
					fk.setOnDelete(onDelete);
					fk.setReferencedTable(referencedTable);
					fk.setReferencedColumn(referencedTable.getPrimaryKeyColumn());
					fk.setReferencingColumn(attributeColumn);
					fk.setReferencingTable(table);
					fkConstraints.add(fk);
				}
			}
		}
	}

	private void addIndicesOnColumns() {
		for (JoinTable joinTable : tableRegistry.getAllJoinTables()) {
			addIndicesForTable(joinTable);
		}
		for (TypeTable typeTable : tableRegistry.getAllTypeTables()) {
			addIndicesForTable(typeTable);
		}
		for (UniqueConstraintTable uniqueConstraintTable : tableRegistry.getAllUniqueConstraintTables()) {
			addIndicesForTable(uniqueConstraintTable);
		}
	}
	
	private void addIndicesForTable(Table table) {
		List<AttributeColumn> columns = table.getColumns();
		if (columns != null) {
			for (AttributeColumn column : columns) {
				if (column.requiresIndex()) {
					// define index name
					String indexName = shortenIdentifier(
							filterCharactersForSQLIdentifier(new StringBuffer("IDX_")
									.append(table.getTableName())
									.append("_")
									.append(column.getColumnName())
									.toString()
							)
					);
					final String finalIndexName = vendorConfiguration.getIdentifierAdapter().transformIndexName(indexName);
					StringBuilder sb = new StringBuilder();
					sb.append("CREATE INDEX ");
					sb.append(finalIndexName);
					sb.append(" ON ");
					if (internalDatabaseSchemaName != null) {
						sb.append(filterCharactersForSQLIdentifier(internalDatabaseSchemaName));
						sb.append(".");
					}
					sb.append(filterCharactersForSQLIdentifier(table.getTableName()));
					sb.append("(");
					sb.append(filterCharactersForSQLIdentifier(column.getColumnName()));
					sb.append(")");
					final String sql = sb.toString();
					deploymentSQL.append(sql).append(";\n");
					List<AttributeColumn> indexedColumns = Collections.unmodifiableList(Arrays.asList(column));
					final IndexImpl index = new IndexImpl(finalIndexName, indexedColumns, table);
					indices.add(index);
					// test is index exists
					boolean doesExist = vendorConfiguration.getIndexExistenceAdapter()
							.isIndexDefinedOnTableColumn(internalDatabaseSchemaName, finalIndexName, table, transactionTemplate);
					if (!doesExist) {
						if (validateOnly) {
							if (validationErrorIgnored) {
								LOG.debug("index did not exist: {}", finalIndexName);
							} else {
								throw new SchemaException("index did not exist: " + finalIndexName);
							}
						}
						LOG.debug("trying to create index {} on {}.{}", finalIndexName, table.getTableName(), column.getColumnName());
						try {
							// if index is missing, create it
							transactionTemplate.doInTransaction(new TransactionCallback() {

								@Override
								public Object doInTransaction(TransactionStatus transactionStatus, Template template) {
									template.execute(sql);
									return null;
								}
							});
						} catch (Exception ex) {
							throw new IllegalStateException("could not create index " + finalIndexName + ": " + ex.getMessage(), ex);
						}
						index.setState(DeploymentState.CREATED);
					} else {
						LOG.debug("skipping creation of index {} on {}.{}, because it does already exist", finalIndexName, table.getTableName(), column.getColumnName());
						index.setState(DeploymentState.FOUND);
					}
				}
			}
		}
	}
	
	private void createTableWithName(TableImpl table, boolean generatePrimaryKey) {
		StringBuilder sb = new StringBuilder();
		sb.append("CREATE TABLE ");
		sb.append(filterCharactersForSQLIdentifier(table.getTableName()));
		sb.append("(");
		sb.append(vendorConfiguration.getIdentifierAdapter().transformColumnName("ID"));
		String primaryKeyDefinition = vendorConfiguration.getPrimaryKeyAdapter().getTablePrimaryKeyAttributeDefinition();
		if (primaryKeyDefinition != null) {
			sb.append(primaryKeyDefinition);
		}
		sb.append(")");
		final String sql = sb.toString();
		deploymentSQL.append(sql).append(";\n");

		if (tableExists(table.getTableName())) {
			table.setState(DeploymentState.FOUND);
			return;
		} else {
			table.setState(DeploymentState.CREATED);
		}

		if (validateOnly) {
			if (validationErrorIgnored) {
				LOG.debug("required table did not exist: table={}", table.getTableName());
			} else {
				throw new SchemaException("could not deploy schema, because a required table did not exist: table=" + table.getTableName());
			}
		}
		try {
			transactionTemplate.doInTransaction(new TransactionCallback() {

				@Override
				public Object doInTransaction(TransactionStatus transactionStatus, Template template) {
					template.execute(sql);
					return null;
				}
			});
		} catch (Exception ex) {
			throw new IllegalStateException("could not create table: " + ex.getMessage(), ex);
		}
		if (!tableExists(table.getTableName())) {
			throw new IllegalStateException("created table, but can not query it.");
		}
		LOG.debug("CREATED TABLE: " + table.getTableName());
	}

	private void createTableWithNameAndPrimaryKey(TableImpl table) /*throws DataAccessException*/ {
		createTableWithName(table, true);
	}
	
	private boolean doesColumnExistInTable(String tableName, String columnName) {
		return vendorConfiguration.getColumnExistenceAdapter().isColumnDefinedOnTable(internalDatabaseSchemaName, columnName, tableName, transactionTemplate);
	}
	private boolean tableExists(final String name) {
		return vendorConfiguration.getTableExistenceAdapter().isTableDefined(internalDatabaseSchemaName, name, transactionTemplate, ANTI_SQL_INJECT);
	}
	private boolean isConstraintDefinedOnTable(String constraintName, Table table) {
		return vendorConfiguration.getConstraintExistenceAdapter().isConstraintDefinedOnTable(internalDatabaseSchemaName, constraintName, table, transactionTemplate);
	}

	private AttributeColumn createAttributeForTable(Attribute attribute, Table table) {
		if (attribute.isVirtual()) {
			return null;
		}
		String tableName = table.getTableName();
		AttributeMediator<Attribute> mediator = mediatorRegistry.getMediatorForAttribute(attribute);
		if (mediator.requiresColumnMapping(attribute)) {
			String columnNameTransformed = getAttributeColumnNameTransformed(attribute);
			String columnType = mediator.columnType(attribute);

			if (columnNameTransformed != null && columnType != null) {
				boolean requiresIndex = attribute.isIndexed() && !attribute.isVirtual();
				if (requiresIndex) {
					// if the attribute is part of a unique constraint or is a primary key,
					// the vendor configuration might tell us, that we do not need to create the index manually
					if (vendorConfiguration.getIndexExistenceAdapter().isUniqueColumnIndexedAutomatically()) {
						boolean hasUniqueConstraint = isSingleColumnUniqueConstraintDefinedForAttribute(attribute);
						if (hasUniqueConstraint) {
							// set requiresIndex to false, if we do not need to create the index manually, because it is created automatically by uniqueness
							requiresIndex = false;
						}
					}
				}
				// check if the column already exists
				AttributeColumn attributeColumn = new AttributeColumnImpl(attribute, columnNameTransformed, columnType, table, requiresIndex, false);
				
				StringBuilder sb = new StringBuilder();
				sb.append("ALTER TABLE ");
				sb.append(filterCharactersForSQLIdentifier(tableName));
				sb.append(" ADD COLUMN ");
				sb.append(filterCharactersForSQLIdentifier(columnNameTransformed));
				sb.append(" ");
				sb.append(columnType);
				if (attribute.isMandatory()) {
					sb.append(" NOT");
				}
				sb.append(" NULL");
				final String sql = sb.toString();
				deploymentSQL.append(sql).append(";\n");
				
				boolean columnExists = doesColumnExistInTable(tableName, columnNameTransformed);
				if (columnExists) {
					table.getColumns().add(attributeColumn);
					return attributeColumn;
				} else if (!columnExists && validateOnly) {
					if (validationErrorIgnored) {
						LOG.debug("could not find column table: {}.{}({})", tableName, columnNameTransformed, columnType);
					} else {
						throw new SchemaException("could not find column table: " + tableName + "." + columnNameTransformed + "(" + columnType + ")");
					}
				}

				try {
					transactionTemplate.doInTransaction(new TransactionCallback() {

						@Override
						public Object doInTransaction(TransactionStatus transactionStatus, Template template) {
							template.execute(sql);
							return null;
						}
					});
				} catch (Exception ex) {
					LOG.error("COULD NOT CREATE COLUMN FOR ATTRIBUTE: " + columnNameTransformed + " " + attribute.getName(), ex);
					throw new IllegalStateException("could not create column for attribute: " + ex.getMessage(), ex);
				}
				table.getColumns().add(attributeColumn);
				LOG.debug("CREATED COLUMN FOR ATTRIBUTE: " + columnNameTransformed + " " + attribute.getName());
				return attributeColumn;
			}
		}
		return null;
	}

	private boolean isSingleColumnUniqueConstraintDefinedForAttribute(Attribute attribute) {
		for (UniqueConstraint uniqueConstraint : uniqueConstraintsByName.values()) {
			List<Attribute> attributes = uniqueConstraint.getAttributes();
			if (attributes == null || attributes.size() != 1) {
				continue;
			}
			if (attribute == attributes.get(0)) {
				// there is a unique constraint for the column of the current attribute
				return true;
			}
		}
		return false;
	}

	private boolean tablesForNamedAttributeHolderNotHandled(NamedAttributeHolder nah) {
		String n = nah.getName();
		boolean r = !tablesForNamedAttributeHolders.contains(n);
		tablesForNamedAttributeHolders.add(n);
		return r;
	}

	public void setMediatorRegistry(MediatorRegistryImpl mediatorRegistry) {
		this.mediatorRegistry = mediatorRegistry;
	}

	public void setTableRegistry(TableRegistryImpl tableRegistry) {
		this.tableRegistry = tableRegistry;
	}

	private DeploymentState assertTableExists(TableImpl table) /*throws DataAccessException*/ {
		createTableWithNameAndPrimaryKey(table);
		return table.getState();
	}

	public void setDatabaseSchemaName(String schemaName) {
		this.internalDatabaseSchemaName = schemaName;
	}

	public void setListeners(List<SchemaDeploymentListener> listeners, ReadWriteLock deploymentListenersLock) {
		this.listeners = listeners;
		this.deploymentListenersLock = deploymentListenersLock;
	}

	public void setConstraintRegistry(ConstraintRegistry constraintRegistry) {
		this.constraintRegistry = constraintRegistry;
	}

	public void setTransactionTemplate(TransactionTemplate transactionTemplate) {
		this.transactionTemplate = transactionTemplate;
	}

	private String getAttributeColumnNameTransformed(Attribute attribute) {
		AttributeMediator<Attribute> med = mediatorRegistry.getMediatorForAttribute(attribute);
		if (med == null) {
			throw new IllegalStateException("could not find mediator for attribute " + attribute.getName());
		}
		String columnNameTransformed = vendorConfiguration.getIdentifierAdapter().transformColumnName(med.columnName(attribute));
		return columnNameTransformed;

	}

	public void setVendorConfiguration(VendorConfiguration vendorConfiguration) {
		this.vendorConfiguration = vendorConfiguration;
	}

}
