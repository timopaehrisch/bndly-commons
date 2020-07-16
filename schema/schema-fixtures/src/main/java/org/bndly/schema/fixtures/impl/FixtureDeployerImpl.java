package org.bndly.schema.fixtures.impl;

/*-
 * #%L
 * Schema Fixtures
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

import org.bndly.common.crypto.api.Base64Service;
import org.bndly.common.json.model.JSArray;
import org.bndly.common.json.model.JSBoolean;
import org.bndly.common.json.model.JSMember;
import org.bndly.common.json.model.JSNull;
import org.bndly.common.json.model.JSNumber;
import org.bndly.common.json.model.JSObject;
import org.bndly.common.json.model.JSString;
import org.bndly.common.json.model.JSValue;
import org.bndly.common.json.parsing.JSONParser;
import org.bndly.common.json.serializing.JSONSerializer;
import org.bndly.common.reflection.GetterBeanPropertyAccessor;
import org.bndly.common.reflection.SetterBeanPropertyWriter;
import org.bndly.schema.api.ObjectReference;
import org.bndly.schema.api.Record;
import org.bndly.schema.api.RecordAttributeIterator;
import org.bndly.schema.api.RecordContext;
import org.bndly.schema.api.Transaction;
import org.bndly.schema.api.listener.QueryByExampleIteratorListener;
import org.bndly.schema.api.services.Engine;
import org.bndly.schema.beans.ActiveRecord;
import org.bndly.schema.beans.SchemaBeanFactory;
import org.bndly.schema.fixtures.api.FixtureDeployer;
import org.bndly.schema.fixtures.api.FixtureDeploymentException;
import org.bndly.schema.model.Attribute;
import org.bndly.schema.model.BinaryAttribute;
import org.bndly.schema.model.BooleanAttribute;
import org.bndly.schema.model.DateAttribute;
import org.bndly.schema.model.DecimalAttribute;
import org.bndly.schema.model.InverseAttribute;
import org.bndly.schema.model.NamedAttributeHolderAttribute;
import org.bndly.schema.model.Schema;
import org.bndly.schema.model.StringAttribute;
import org.bndly.schema.model.Type;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bndly.schema.api.db.TypeTable;
import org.bndly.schema.api.exception.SchemaException;
import org.bndly.schema.api.query.Query;
import org.bndly.schema.api.query.QueryContext;
import org.bndly.schema.api.services.TableRegistry;
import org.bndly.schema.json.RecordJsonConverter;
import org.bndly.schema.model.JSONAttribute;
import org.bndly.schema.model.Mixin;
import org.bndly.schema.model.NamedAttributeHolder;
import org.bndly.schema.model.SchemaUtil;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = FixtureDeployer.class, immediate = true)
public class FixtureDeployerImpl implements FixtureDeployer {

	@Reference
	private RecordJsonConverter recordJsonConverter;
	private static final Logger LOG = LoggerFactory.getLogger(FixtureDeployerImpl.class);
	private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
	private static final NotExistsConditionEvaluator NOT_EXISTS_CONDITION_EVALUATOR = new NotExistsConditionEvaluator();
	private static final MissingReferencedItemHandler MISSING_REFERENCE_ON_TYPE_INSTANCE_LEVEL = new MissingReferencedItemHandler() {

		@Override
		public PersistenceItem onMissingReferencedItem(String referenceKey) throws FixtureDeploymentException {
			throw new FixtureDeploymentException("reference '" + referenceKey + "' is not allowed on type instance level");
		}
	};
	private static final MissingReferencedItemHandler REQUIRED_REFERENCE = new MissingReferencedItemHandler() {

		@Override
		public PersistenceItem onMissingReferencedItem(String referenceKey) throws FixtureDeploymentException {
			throw new FixtureDeploymentException("reference '" + referenceKey + "' is required but not yet found");
		}
	};
	private static final AttributeValueHandler SET_VALUE_ATTRIBUTE_VALUE_HANDLER = new AttributeValueHandler() {
		
		final SetterBeanPropertyWriter writer = new SetterBeanPropertyWriter();
		
		@Override
		public void onAttributeValue(Object value, Attribute attribute, ActiveRecord targetBean, FixtureConversionContext fixtureConversionContext) throws FixtureDeploymentException {
			writer.set(attribute.getName(), value, targetBean);
			if (NamedAttributeHolderAttribute.class.isInstance(attribute)) {
				NamedAttributeHolderAttribute naha = (NamedAttributeHolderAttribute) attribute;
				if (naha.getToOneAttribute() != null) {
					// close the cycle
					writer.set(naha.getToOneAttribute(), targetBean, value);
				}
			}
		}
	};
	private ComponentContext componentContext;
	private boolean active;

	private Set<NamedAttributeHolderAttribute> findAttributesWithBacklinks(Schema schema) {
		final Map<String, NamedAttributeHolder> attributeHoldersByName = new HashMap<>();
		final List<NamedAttributeHolderAttribute> attributesWithOneToOneRelation = new ArrayList<>();
		if (schema.getTypes() != null) {
			for (Type type : schema.getTypes()) {
				attributeHoldersByName.put(type.getName(), type);
				findAttributesWithToOneAttribute(type, attributesWithOneToOneRelation);
			}
		}
		if (schema.getMixins() != null) {
			for (Mixin mixin : schema.getMixins()) {
				attributeHoldersByName.put(mixin.getName(), mixin);
				findAttributesWithToOneAttribute(mixin, attributesWithOneToOneRelation);
			}
		}
		Set<NamedAttributeHolderAttribute> result = new HashSet<>();
		for (NamedAttributeHolderAttribute oneToOneAttribute : attributesWithOneToOneRelation) {
			NamedAttributeHolder referrsTo = oneToOneAttribute.getNamedAttributeHolder();
			Map<String, Attribute> attributeMap = SchemaUtil.collectAttributesAsMap(referrsTo);
			Attribute backlinkAttribute = attributeMap.get(oneToOneAttribute.getToOneAttribute());
			if (NamedAttributeHolderAttribute.class.isInstance(backlinkAttribute)) {
				result.add((NamedAttributeHolderAttribute) backlinkAttribute);
			}
		}
		return result;
	}

	private void findAttributesWithToOneAttribute(NamedAttributeHolder attributeHolder, final List<NamedAttributeHolderAttribute> attributesWithOneToOneRelation) {
		List<Attribute> atts = attributeHolder.getAttributes();
		if (atts != null) {
			for (Attribute att : atts) {
				if (att.isVirtual()) {
					continue;
				}
				if (NamedAttributeHolderAttribute.class.isInstance(att)) {
					NamedAttributeHolderAttribute naha = (NamedAttributeHolderAttribute) att;
					if (naha.getToOneAttribute() != null) {
						attributesWithOneToOneRelation.add(naha);
					}
				}
			}
		}
	}

	private static interface FixtureConversionContext {
		SchemaBeanFactory getSchemaBeanFactory();
		RecordContext getRecordContext();
		FixtureConversionContext putInitialTransactionAppender(TransactionAppender transactionAppender);
		PersistenceItem createPersistenceItemForRecord(Record record);
		PersistenceItem createPersistenceItemForRecord(String key, Record record);
		PersistenceItem getPersistable(String key);
		PersistenceItem putPersistable(PersistenceItem persistable);
		JSObject getEntry(String key);
		void putEntry(JSObject jsObject);
	}
	
	public static interface TransactionAppender {
		void appendToTransaction(Transaction transaction) throws FixtureDeploymentException;
	}
	
	private static interface MissingReferencedItemHandler {
		PersistenceItem onMissingReferencedItem(String referenceKey) throws FixtureDeploymentException;
	}
	
	private static interface AttributeValueHandler {
		void onAttributeValue(Object value, Attribute attribute, ActiveRecord targetBean, FixtureConversionContext fixtureConversionContext) throws FixtureDeploymentException;
	}
	
	private final List<SchemaBeanFactory> schemaBeanFactorys = new ArrayList<>();
	private final List<BoundFixtureDeployerImpl> boundFixtureDeployer = new ArrayList<>();
	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	@Reference
	private Base64Service base64Service;
	
	@Reference(
			bind = "addSchemaBeanFactory",
			unbind = "removeSchemaBeanFactory",
			cardinality = ReferenceCardinality.MULTIPLE,
			policy = ReferencePolicy.DYNAMIC,
			service = SchemaBeanFactory.class
	)
	public void addSchemaBeanFactory(SchemaBeanFactory schemaBeanFactory) {
		if (schemaBeanFactory != null) {
			lock.writeLock().lock();
			try {
				schemaBeanFactorys.add(schemaBeanFactory);
				BoundFixtureDeployerImpl tmp = new BoundFixtureDeployerImpl(schemaBeanFactory) {
					@Override
					public void deploy(Reader reader) throws FixtureDeploymentException {
						deployInternal(getSchemaName(), getSchemaBeanFactory(), reader);
					}
					
					@Override
					public void dumpFixture(Writer writer) throws FixtureDeploymentException {
						dumpFixtureInternal(getSchemaName(), getSchemaBeanFactory(), writer);
					}
				};
				boundFixtureDeployer.add(tmp);
				if (active) {
					tmp.register(componentContext);
				}
			} finally {
				lock.writeLock().unlock();
			}
		}
	}

	public void removeSchemaBeanFactory(SchemaBeanFactory schemaBeanFactory) {
		if (schemaBeanFactory != null) {
			lock.writeLock().lock();
			try {
				schemaBeanFactorys.remove(schemaBeanFactory);
				Iterator<BoundFixtureDeployerImpl> iter = boundFixtureDeployer.iterator();
				while (iter.hasNext()) {
					BoundFixtureDeployerImpl tmp = iter.next();
					if (tmp.getSchemaBeanFactory() == schemaBeanFactory) {
						iter.remove();
						if (active) {
							tmp.unregister();
						}
					}
				}
			} finally {
				lock.writeLock().unlock();
			}
		}
	}
	
	@Activate
	public void activate(ComponentContext componentContext) {
		lock.writeLock().lock();
		try {
			this.componentContext = componentContext;
			active = true;
			for (BoundFixtureDeployerImpl boundFixtureDeployerImpl : boundFixtureDeployer) {
				boundFixtureDeployerImpl.register(componentContext);
			}
		} finally {
			lock.writeLock().unlock();
		}
	}
	
	@Deactivate
	public void deactivate(ComponentContext componentContext) {
		lock.writeLock().lock();
		try {
			for (BoundFixtureDeployerImpl boundFixtureDeployerImpl : boundFixtureDeployer) {
				boundFixtureDeployerImpl.unregister();
			}
			this.componentContext = null;
			active = false;
		} finally {
			lock.writeLock().unlock();
		}
	}
	
	private SchemaBeanFactory getSchemaBeanFactoryBySchemaName(String schemaName) {
		lock.readLock().lock();
		try {
			for (SchemaBeanFactory schemaBeanFactory : schemaBeanFactorys) {
				Engine engine = schemaBeanFactory.getEngine();
				if (schemaName.equals(engine.getDeployer().getDeployedSchema().getName())) {
					return schemaBeanFactory;
				}
			}
			return null;
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public void dumpFixture(final String schemaName, final Writer writer) throws FixtureDeploymentException {
		SchemaBeanFactory schemaBeanFactory = getSchemaBeanFactoryBySchemaName(schemaName);
		if (schemaBeanFactory == null) {
			return;
		}
		dumpFixtureInternal(schemaName, schemaBeanFactory, writer);
	}

	private void dumpFixtureInternal(final String schemaName, SchemaBeanFactory schemaBeanFactory, final Writer writer) throws FixtureDeploymentException {
		final Engine engine = schemaBeanFactory.getEngine();
		// iterate all types and dump them as JSON objects to the writer
		Schema deployedSchema = engine.getDeployer().getDeployedSchema();
		// find the attributes, that have one-to-one backlinks. these should be skipped.
		final Set<NamedAttributeHolderAttribute> attributesWithBacklink = findAttributesWithBacklinks(deployedSchema);
		List<Type> types = deployedSchema.getTypes();
		final JSONSerializer serializer = new JSONSerializer();
		try {
			writer.write("{");
			serializer.serialize(new JSString("items"), writer);
			writer.write(":[");
			if (types != null) {
				int batchSize = 10;
				boolean eager = false;
				boolean firstType = true;
				for (Type type : types) {
					if (type.isVirtual() || type.isAbstract()) {
						continue;
					}
					if (!firstType) {
						writer.write(",");
					}
					writer.write("{");
					serializer.serialize(new JSString("type"), writer);
					writer.write(":");
					serializer.serialize(new JSString(type.getName()), writer);
					writer.write(",");
					serializer.serialize(new JSString("entries"), writer);
					writer.write(":[");
					
					// create a new record context for each iteration, because otherwise the context might get too big
					final RecordContext context = engine.getAccessor().buildRecordContext();
					
					final RecordAttributeIterator recordAttributeIterator = new RecordAttributeIterator() {

						private void appendMember(String memberName, JSValue value) throws IOException {
							writer.write(",");
							serializer.serialize(new JSString(memberName), writer);
							writer.write(":");
							serializer.serialize(value, writer);
						}

						@Override
						public void handleAttribute(Attribute attribute, Record record) {
							try {
								if (StringAttribute.class.isInstance(attribute)) {
									String stringValue = record.getAttributeValue(attribute.getName(), String.class);
									if (stringValue == null) {
										appendMember(attribute.getName(), JSNull.INSTANCE);
									} else {
										appendMember(attribute.getName(), new JSString(stringValue));
									}
								} else if (DecimalAttribute.class.isInstance(attribute)) {
									Number numberValue = (Number) record.getAttributeValue(attribute.getName());
									if (numberValue == null) {
										appendMember(attribute.getName(), JSNull.INSTANCE);
									} else {
										JSNumber num;
										if (Byte.class.isInstance(numberValue)) {
											num = new JSNumber(numberValue.byteValue());
										} else if (Short.class.isInstance(numberValue)) {
											num = new JSNumber(numberValue.shortValue());
										} else if (Integer.class.isInstance(numberValue)) {
											num = new JSNumber(numberValue.intValue());
										} else if (Long.class.isInstance(numberValue)) {
											num = new JSNumber(numberValue.longValue());
										} else if (Float.class.isInstance(numberValue)) {
											num = new JSNumber(numberValue.floatValue());
										} else if (Double.class.isInstance(numberValue)) {
											num = new JSNumber(numberValue.doubleValue());
										} else if (BigDecimal.class.isInstance(numberValue)) {
											num = new JSNumber((BigDecimal) numberValue);
										} else {
											throw new IllegalStateException("unsupported number");
										}
										appendMember(attribute.getName(), num);
									}
								} else if (DateAttribute.class.isInstance(attribute)) {
									Date dateValue = record.getAttributeValue(attribute.getName(), Date.class);
									if (dateValue == null) {
										appendMember(attribute.getName(), JSNull.INSTANCE);
									} else {
										appendMember(attribute.getName(), new JSNumber(dateValue.getTime()));
									}
								} else if (BooleanAttribute.class.isInstance(attribute)) {
									Boolean boolValue = record.getAttributeValue(attribute.getName(), Boolean.class);
									if (boolValue == null) {
										appendMember(attribute.getName(), JSNull.INSTANCE);
									} else {
										appendMember(attribute.getName(), new JSBoolean(boolValue));
									}
								} else if (BinaryAttribute.class.isInstance(attribute) && !JSONAttribute.class.isInstance(attribute)) {
									Object binaryValue = record.getAttributeValue(attribute.getName());
									if (binaryValue == null) {
										appendMember(attribute.getName(), JSNull.INSTANCE);
									} else {
										InputStream is;
										if (InputStream.class.isInstance(binaryValue)) {
											is = (InputStream) binaryValue;
										} else {
											is = new ByteArrayInputStream((byte[]) binaryValue);
										}
										writer.write(",");
										serializer.serialize(new JSString(attribute.getName()), writer);
										writer.write(":");
										writer.write("\"");
										// base64 does not contain characters, that need string escaping for json
										base64Service.base64EncodeStream(is, writer);
										writer.write("\"");
									}
								} else if (JSONAttribute.class.isInstance(attribute)) {
									Record val = record.getAttributeValue(attribute.getName(), Record.class);
									if (val != null) {
										JSObject json = recordJsonConverter.convertRecordToJson(new JSObject(), val);
										// any jsobject with a _type and _id may be replaced with a _ref, if the according entity still exists.
										insertRefsIntoJson(json);
										appendMember(attribute.getName(), json);
									}
								} else if (NamedAttributeHolderAttribute.class.isInstance(attribute)) {
									NamedAttributeHolderAttribute naha = (NamedAttributeHolderAttribute) attribute;
									if (attributesWithBacklink.contains(naha)) {
										// we ignore this value, because it will be set by the referenced object. 
										// the reason behind this is the fact, that we are dealing with a one-to-one relation.
										return;
									}
									Record val = record.getAttributeValue(attribute.getName(), Record.class);
									if (val != null) {
										String key = buildKeyForRecord(val);
										if (key != null) {
											JSObject ref = new JSObject();
											ref.createMember("_ref").setValue(new JSString(key));
											appendMember(attribute.getName(), ref);
										} else {
											LOG.warn(
													"unable to create reference to record of type {} in {}.{} "
													+ "while dumping fixture from schema {}",
													new Object[]{
														val.getType().getName(),
														record.getType().getName(),
														attribute.getName(),
														schemaName
													}
											);
										}
									}
								} else {
									// not yet supported
									LOG.warn(
											"unsupported attribute type {} in {}.{} while dumping fixture from schema {}",
											new Object[]{
												attribute.getClass().getName(),
												record.getType().getName(),
												attribute.getName(),
												schemaName
											}
									);
								}
							} catch (IOException ex) {
								throw new SchemaException("failed to write attribute " + attribute.getName() + ": " + ex.getMessage(), ex);
							}
						}

						private void insertRefsIntoJson(JSObject json) {
							String typeName = json.getMemberStringValue("_type");
							BigDecimal idValue = json.getMemberNumberValue("_id");
							if (typeName != null && idValue != null) {
								// could be replaced as _ref
								Record foundRecord = engine.getAccessor().readById(typeName, idValue.longValue(), context);
								if (foundRecord != null) {
									json.getMembers().clear();
									json.createMember("_ref").setValue(new JSString(typeName + "_" + idValue.longValue()));
									return;
								} else {
									// the record does not exist anymore. therefore we better remove the _id member
									json.getMembers().remove(json.getMember("_id"));
								}
							}
							for (JSMember member : json.getMembers()) {
								JSValue memValue = member.getValue();
								if (JSArray.class.isInstance(memValue)) {
									for (JSValue arrayItem : (JSArray) memValue) {
										if (JSObject.class.isInstance(arrayItem)) {
											insertRefsIntoJson((JSObject) arrayItem);
										}
									}
								} else if (JSObject.class.isInstance(memValue)) {
									insertRefsIntoJson((JSObject) memValue);
								}
							}
						}
					};
					engine.getAccessor().iterate(type.getName(), new QueryByExampleIteratorListener() {

						private boolean isFirstRecord = true;
						
						@Override
						public void handleRecord(Record r) {
							try {
								if (!isFirstRecord) {
									writer.write(",");
								}
								writer.write("{");
								serializer.serialize(new JSString("_key"), writer);
								writer.write(":");
								serializer.serialize(new JSString(r.getType().getName() + "_" + r.getId().toString()), writer);

								r.iteratePresentValues(recordAttributeIterator);
								writer.write("}");
								isFirstRecord = false;
							} catch (IOException ex) {
								throw new SchemaException("failed to write record " + r.getType().getName() + " id=" + r.getId() + ": " + ex.getMessage(), ex);
							}
						}
					}, batchSize, eager, context);

					writer.write("]");
					writer.write("}");
					firstType = false;
				}
			}
			writer.write("]");
			writer.write("}");
		} catch (SchemaException | IOException ex) {
			throw new FixtureDeploymentException("could not serialize fixture dump: " + ex.getMessage(), ex);
		}
	}
	
	private String buildKeyForRecord(Record record) {
		Long id = record.getId();
		if (id == null) {
			return null;
		}
		Type type = record.getType();
		String name = type.getName();
		String key = name + "_" + id;
		return key;
	}
	
	@Override
	public void deploy(String schemaName, Reader reader) throws FixtureDeploymentException {
		final SchemaBeanFactory schemaBeanFactory = getSchemaBeanFactoryBySchemaName(schemaName);
		if (schemaBeanFactory == null) {
			LOG.warn("could not find schema bean factory for schema {}. therefore a fixture could not be deployed.", schemaName);
			return;
		}
		deployInternal(schemaName, schemaBeanFactory, reader);
	}
	
	private void deployInternal(final String schemaName, final SchemaBeanFactory schemaBeanFactory, Reader reader) throws FixtureDeploymentException {
		Engine engine = schemaBeanFactory.getEngine();
		try {
			JSObject fixture = (JSObject) new JSONParser().parse(reader);
			final Map<String, PersistenceItem> persistenceItems = new LinkedHashMap<>();
			final Map<String, JSObject> keyedEntries = new HashMap<>();
			final List<TransactionAppender> initialTransactionAppenders = new ArrayList<>();
			final RecordContext recordContext = engine.getAccessor().buildRecordContext();
			FixtureConversionContext fixtureConversionContext = new FixtureConversionContext() {

				@Override
				public SchemaBeanFactory getSchemaBeanFactory() {
					return schemaBeanFactory;
				}

				@Override
				public RecordContext getRecordContext() {
					return recordContext;
				}

				@Override
				public PersistenceItem createPersistenceItemForRecord(Record record) {
					return createPersistenceItemForRecord(buildKeyForRecord(record), record);
				}

				@Override
				public PersistenceItem createPersistenceItemForRecord(String key, Record record) {
					PersistenceItem persistenceItem = new PersistenceItem(key, (ActiveRecord) schemaBeanFactory.getSchemaBean(record), record);
					if (!record.getType().isVirtual()) {
						putPersistable(persistenceItem);
					}
					return persistenceItem;
				}

				@Override
				public FixtureConversionContext putInitialTransactionAppender(TransactionAppender transactionAppender) {
					if (transactionAppender != null) {
						initialTransactionAppenders.add(transactionAppender);
					}
					return this;
				}

				@Override
				public PersistenceItem getPersistable(String key) {
					return persistenceItems.get(key);
				}

				@Override
				public PersistenceItem putPersistable(PersistenceItem persistable) {
					persistenceItems.put(persistable.getKey(), persistable);
					return persistable;
				}

				@Override
				public JSObject getEntry(String key) {
					return keyedEntries.get(key);
				}

				@Override
				public void putEntry(JSObject jsObject) {
					String key = jsObject.getMemberStringValue("_key");
					if (key != null) {
						keyedEntries.put(key, jsObject);
					}
				}
				
			};
			deployTypes(fixture, fixtureConversionContext);

			Transaction tx = engine.getQueryRunner().createTransaction();
			for (TransactionAppender transactionAppender : initialTransactionAppenders) {
				transactionAppender.appendToTransaction(tx);
			}
			for (PersistenceItem value : persistenceItems.values()) {
				value.appendToTransaction(tx);
			}
			tx.commit();
		} catch (FixtureDeploymentException e) {
			LOG.error("failed to deploy fixture to schema {}", schemaName, e);
			throw e;
		} catch (Exception e) {
			LOG.error("failed to deploy fixture to schema {}", schemaName, e);
			throw new FixtureDeploymentException("could not deploy fixture to schema " + schemaName, e);
		}
	}

	private JSMember getMemberByName(String name, JSObject object) {
		return object.getMember(name);
	}

	private void deployTypes(JSObject fixture, final FixtureConversionContext fixtureConversionContext) throws FixtureDeploymentException {
		Set<JSMember> members = fixture.getMembers();
		if (members != null) {
			for (JSMember jSMember : members) {
				if (jSMember.getName().getValue().equals("items")) {
					List<JSValue> items = ((JSArray) jSMember.getValue()).getItems();
					if(items == null) {
						continue;
					}
					for (JSValue jSValue : items) {
						JSMember typeMember = getMemberByName("type", (JSObject) jSValue);
						JSMember entriesMember = getMemberByName("entries", (JSObject) jSValue);
						if (typeMember == null) {
							throw new FixtureDeploymentException("missing a type member in fixture");
						}
						final String typeName = ((JSString) typeMember.getValue()).getValue();
						Boolean dropBefore = ((JSObject) jSValue).getMemberBooleanValue("drop");
						if (dropBefore != null && dropBefore) {
							fixtureConversionContext.putInitialTransactionAppender(new TransactionAppender() {

								@Override
								public void appendToTransaction(Transaction transaction) {
									Engine engine = fixtureConversionContext.getSchemaBeanFactory().getEngine();
									TableRegistry tr = engine.getTableRegistry();
									TypeTable tt = tr.getTypeTableByType(typeName);
									QueryContext qc = engine.getQueryContextFactory().buildQueryContext();
									qc.delete().from(tt.getTableName());
									Query q = qc.build(fixtureConversionContext.getRecordContext());
									transaction.getQueryRunner().run(q);
								}
							});
						}
						if (entriesMember == null) {
							continue;
						}
						JSValue instances = entriesMember.getValue();
						if (!JSArray.class.isInstance(instances)) {
							throw new FixtureDeploymentException("instances of " + typeName + " were not provided as an array");
						}
						JSArray instancesArray = (JSArray) instances;
						List<JSValue> instanceDescriptions = instancesArray.getItems();
						if (instanceDescriptions != null) {
							for (JSValue instanceDescription : instanceDescriptions) {
								if (!JSObject.class.isInstance(instanceDescription)) {
									throw new FixtureDeploymentException("instance of " + typeName + " was not provided as an object");
								}
								fixtureConversionContext.putEntry((JSObject) instanceDescription);
								deployTypeInstance(typeName, (JSObject) instanceDescription, fixtureConversionContext);
							}
						}
					}
				}
			}
		}
	}

	private void deployTypeInstance(String typeName, JSObject jsObject, FixtureConversionContext fixtureConversionContext) throws FixtureDeploymentException {
		createTypeInstanceFromJSObjectAsDependency(jsObject, typeName, fixtureConversionContext, MISSING_REFERENCE_ON_TYPE_INSTANCE_LEVEL);
	}

	private PersistenceItem createTypeInstanceFromJSObjectAsDependency(
			JSObject jsObject, String instanceType, FixtureConversionContext fixtureConversionContext, MissingReferencedItemHandler missingReferencedItemHandler
	) throws FixtureDeploymentException {
		return createTypeInstanceFromJSObject(jsObject, instanceType, false, false, fixtureConversionContext, missingReferencedItemHandler);
	}

	private PersistenceItem createTypeInstanceFromJSObject(
			JSObject jsObject, String instanceType, boolean withinJsonAttributeScope, boolean asChild, FixtureConversionContext fixtureConversionContext, MissingReferencedItemHandler missingReferencedItemHandler
	) throws FixtureDeploymentException {
		PersistenceItem par = __convertJSObjectToPersistableActiveRecord(jsObject, instanceType, withinJsonAttributeScope, asChild, fixtureConversionContext, missingReferencedItemHandler);
		return par;
	}

	private PersistenceItem __convertJSObjectToPersistableActiveRecord(
			final JSObject jsObject,
			String defaultTypeName,
			boolean withinJsonAttributeScope,
			boolean asChild,
			final FixtureConversionContext fixtureConversionContext,
			MissingReferencedItemHandler missingReferencedItemHandler
	) throws FixtureDeploymentException {
		String ref = null;
		String storeAsKey = null;
		List<JSMember> realMembers = new ArrayList<>();
		Set<JSMember> members = jsObject.getMembers();
		final ObjectReference<Record> foundRecordReference = new ObjectReference<>();
		if (members != null) {
			for (JSMember jSMember : members) {
				String attributeName = jSMember.getName().getValue();
				final JSValue attributeValueAsJSON = jSMember.getValue();
				if ("_ref".equals(attributeName)) {
					ref = ((JSString) attributeValueAsJSON).getValue();
					// get the property value from the deployed instances
				} else if ("_type".equals(attributeName)) {
					defaultTypeName = ((JSString) attributeValueAsJSON).getValue();
				} else if ("_key".equals(attributeName)) {
					storeAsKey = ((JSString) attributeValueAsJSON).getValue();
				} else if ("_condition".equals(attributeName)) {
					final String conditionDefaultTypeName = defaultTypeName;
					final JSValue conditionJSON = attributeValueAsJSON;
					ConditionEvaluator.ConditionContext conditionContext = new ConditionEvaluator.ConditionContext() {
						@Override
						public SchemaBeanFactory getSchemaBeanFactory() {
							return fixtureConversionContext.getSchemaBeanFactory();
						}

						@Override
						public String getInstanceType() {
							return conditionDefaultTypeName;
						}

						@Override
						public JSValue getDefinition() {
							return conditionJSON;
						}

						@Override
						public JSObject getEntry() {
							return jsObject;
						}

						@Override
						public JSObject getEntry(String key) throws FixtureDeploymentException {
							JSObject entry = fixtureConversionContext.getEntry(key);
							if (entry == null) {
								throw new FixtureDeploymentException("failed to get fixture entry by key " + key);
							}
							return entry;
						}

						@Override
						public void onFoundRecord(Record foundRecord) throws FixtureDeploymentException {
							foundRecordReference.set(foundRecord);
						}
					};
					String type;
					if (JSObject.class.isInstance(conditionJSON)) {
						type = ((JSObject) conditionJSON).getMemberStringValue("type");
					} else if (JSString.class.isInstance(conditionJSON)) {
						type = ((JSString) conditionJSON).getValue();
					} else {
						LOG.warn("unsupported condition definition");
						continue;
					}
					if ("notExists".equals(type)) {
						if (NOT_EXISTS_CONDITION_EVALUATOR.shouldBePersisted(conditionContext)) {
							LOG.info("condition did not match. persisting instance of type {}", defaultTypeName);
						} else {
							LOG.info("condition did match. reusing record instance {}", foundRecordReference.get());
						}
					} else {
						LOG.warn("unsupported condition type {}", type);
					}
				} else {
					realMembers.add(jSMember);
				}
			}
		}

		// if something is referenced, look it up.
		if (ref != null) {
			PersistenceItem existingItem = fixtureConversionContext.getPersistable(ref);
			if (existingItem == null) {
				// if the look up fails, throw an exception
				return missingReferencedItemHandler.onMissingReferencedItem(ref);
			} else {
				// otherwise we can directly return the looked up item
				return existingItem;
			}
		}
		
		if (foundRecordReference.get() != null) {
			PersistenceItem item;
			if (storeAsKey != null) {
				item = fixtureConversionContext.createPersistenceItemForRecord(storeAsKey, foundRecordReference.get());
			} else {
				item = fixtureConversionContext.createPersistenceItemForRecord(foundRecordReference.get());
			}
			item.skipPersistence();
			return item;
		}

		final Record record = fixtureConversionContext.getRecordContext().create(defaultTypeName);
		ActiveRecord activeRecord = (ActiveRecord) fixtureConversionContext.getSchemaBeanFactory().getSchemaBean(record);
		
		final DependencyTracker dependencyTracker = new DependencyTracker();
		for (JSMember jSMember : realMembers) {
			String attributeName = jSMember.getName().getValue();
			Attribute attributeDef = record.getAttributeDefinition(attributeName);
			if (attributeDef.isVirtual() && !record.getType().isVirtual()) {
				continue;
			} else if (BinaryAttribute.class.isInstance(attributeDef) && !JSONAttribute.class.isInstance(attributeDef)) {
				JSValue val = jSMember.getValue();
				if (val == null || !JSString.class.isInstance(val)) {
					continue;
				}
			}
			__convertJSMemberToActiveRecordMember(jSMember, attributeDef, activeRecord, fixtureConversionContext, SET_VALUE_ATTRIBUTE_VALUE_HANDLER, dependencyTracker, withinJsonAttributeScope);
		}

		final PersistenceItem result;
		if (storeAsKey != null) {
			result = fixtureConversionContext.createPersistenceItemForRecord(storeAsKey, record);
		} else {
			if (record.getId() != null) {
				result = fixtureConversionContext.createPersistenceItemForRecord(record);
			} else {
				result = fixtureConversionContext.createPersistenceItemForRecord(Integer.toString(System.identityHashCode(record)), record);
				if (withinJsonAttributeScope) {
					result.skipPersistence();
				}
			}
		}
		fixtureConversionContext.putInitialTransactionAppender(new TransactionAppender() {

			@Override
			public void appendToTransaction(Transaction transaction) throws FixtureDeploymentException {
				dependencyTracker.appendDependentsOf(result);
				dependencyTracker.appendDependenciesTo(result);
			}
		});
		return result;
	}

	private void __convertJSMemberToActiveRecordMember(
			final JSMember jSMember,
			final Attribute attributeDefinition,
			final ActiveRecord targetBean,
			final FixtureConversionContext fixtureConversionContext,
			final AttributeValueHandler attributeValueHandler,
			final DependencyTracker dependencyTracker,
			final boolean withinJsonAttributeScope
	) throws FixtureDeploymentException {
		String attributeName = jSMember.getName().getValue();
		JSValue attributeValueAsJSON = jSMember.getValue();
		if (JSNull.class.isInstance(attributeValueAsJSON)) {
			// null has to be set explicitly
			attributeValueHandler.onAttributeValue(null, attributeDefinition, targetBean, fixtureConversionContext);
			return;
		}

		if (NamedAttributeHolderAttribute.class.isInstance(attributeDefinition)) {
			// complex object
			NamedAttributeHolderAttribute naha = (NamedAttributeHolderAttribute) attributeDefinition;
			String defaultAttributeHolderName = naha.getNamedAttributeHolder().getName();
			PersistenceItem nestedObject = __convertJSObjectToPersistableActiveRecord(
					(JSObject) attributeValueAsJSON, 
					defaultAttributeHolderName, 
					withinJsonAttributeScope,
					false, 
					fixtureConversionContext, 
					createLazyReferenceHandler(fixtureConversionContext, attributeValueHandler, attributeDefinition, targetBean, dependencyTracker)
			);
			if (nestedObject != null) {
				if (naha.getToOneAttribute() != null) {
					dependencyTracker.addOneToOne(naha, nestedObject);
				} else {
					dependencyTracker.add(nestedObject);
				}
				attributeValueHandler.onAttributeValue(nestedObject.getActiveRecord(), attributeDefinition, targetBean, fixtureConversionContext);
			}
			return;
		} else if (InverseAttribute.class.isInstance(attributeDefinition)) {
			// collection of complex objects
			final SetterBeanPropertyWriter setterBeanPropertyWriter = new SetterBeanPropertyWriter();
			JSArray entries = (JSArray) attributeValueAsJSON;
			List<JSValue> items = entries.getItems();
			if (items != null) {
				final InverseAttribute ia = (InverseAttribute) attributeDefinition;
				String defaultAttributeHolderName = ia.getReferencedAttributeHolder().getName();
				List<ActiveRecord> nestedObjects = null;
				for (JSValue jSValue : items) {
					PersistenceItem nestedObject = __convertJSObjectToPersistableActiveRecord(
							(JSObject) jSValue, defaultAttributeHolderName, withinJsonAttributeScope, true, fixtureConversionContext, new MissingReferencedItemHandler() {

						@Override
						public PersistenceItem onMissingReferencedItem(final String referenceKey) throws FixtureDeploymentException {
							fixtureConversionContext.putInitialTransactionAppender(new TransactionAppender() {

								@Override
								public void appendToTransaction(Transaction transaction) throws FixtureDeploymentException {
									PersistenceItem referencedListItem = fixtureConversionContext.getPersistable(referenceKey);
									if (referencedListItem == null) {
										throw new FixtureDeploymentException(
												"referenced list item '" 
												+ referenceKey 
												+ "' for attribute " 
												+ attributeDefinition.getName() 
												+ " was not found"
										);
									}
									String prop = ia.getReferencedAttributeName();
									if (prop != null) {
										setterBeanPropertyWriter.set(prop, targetBean, referencedListItem.getActiveRecord());
									}

								}
							});
							return null;
						}
					});
					// set parent in the nested object
					if (nestedObject != null) {
						String prop = ia.getReferencedAttributeName();
						if (prop != null) {
							setterBeanPropertyWriter.set(prop, targetBean, nestedObject.getActiveRecord());
						} else {
							if (ia.isVirtual()) {
								// append nested items to a list
								if (nestedObjects == null) {
									nestedObjects = new ArrayList<>();
								}
								nestedObjects.add(nestedObject.getActiveRecord());
							}
						}
						
						// since we are in a list, we depend on the list owner
						dependencyTracker.addDependent(nestedObject);
					}
				}
				if (nestedObjects != null) {
					attributeValueHandler.onAttributeValue(nestedObjects, attributeDefinition, targetBean, fixtureConversionContext);
				}
			}
		} else if (StringAttribute.class.isInstance(attributeDefinition)) {
			String value = ((JSString) attributeValueAsJSON).getValue();
			attributeValueHandler.onAttributeValue(value, attributeDefinition, targetBean, fixtureConversionContext);
			return;
		} else if (DecimalAttribute.class.isInstance(attributeDefinition)) {
			BigDecimal value = ((JSNumber) attributeValueAsJSON).getValue();
			Class<?> type = new GetterBeanPropertyAccessor().typeOf(attributeName, targetBean);
			if (Long.class.equals(type)) {
				attributeValueHandler.onAttributeValue(value.longValue(), attributeDefinition, targetBean, fixtureConversionContext);
				return ;
			} else if (Double.class.equals(type)) {
				attributeValueHandler.onAttributeValue(value.doubleValue(), attributeDefinition, targetBean, fixtureConversionContext);
				return ;
			} else {
				attributeValueHandler.onAttributeValue(value, attributeDefinition, targetBean, fixtureConversionContext);
				return ;
			}
		} else if (BooleanAttribute.class.isInstance(attributeDefinition)) {
			boolean value = ((JSBoolean) attributeValueAsJSON).isValue();
			attributeValueHandler.onAttributeValue(value, attributeDefinition, targetBean, fixtureConversionContext);
			return;
		} else if (DateAttribute.class.isInstance(attributeDefinition)) {
			BigDecimal dateAsNumber = ((JSNumber) attributeValueAsJSON).getValue();
			Date date = new Date(dateAsNumber.longValue());
			attributeValueHandler.onAttributeValue(date, attributeDefinition, targetBean, fixtureConversionContext);
			return ;
		} else if (JSONAttribute.class.isInstance(attributeDefinition)) {
 			String defaultAttributeHolderName = ((JSONAttribute) attributeDefinition).getNamedAttributeHolder().getName();
			PersistenceItem nestedObject = __convertJSObjectToPersistableActiveRecord(
					(JSObject) attributeValueAsJSON, 
					defaultAttributeHolderName, 
					true,
					false, 
					fixtureConversionContext,
					createLazyReferenceHandler(fixtureConversionContext, attributeValueHandler, attributeDefinition, targetBean, dependencyTracker)
			);
			if (nestedObject != null) {
				attributeValueHandler.onAttributeValue(nestedObject.getActiveRecord(), attributeDefinition, targetBean, fixtureConversionContext);
			}
			return ;
		} else if (BinaryAttribute.class.isInstance(attributeDefinition)) {
			BinaryAttribute ba = (BinaryAttribute) attributeDefinition;
			String base64StringData = ((JSString) jSMember.getValue()).getValue();
			if (ba.getAsByteArray() != null && ba.getAsByteArray()) {
				if (base64StringData == null || base64StringData.isEmpty()) {
					attributeValueHandler.onAttributeValue(EMPTY_BYTE_ARRAY, attributeDefinition, targetBean, fixtureConversionContext);
					return ;
				} else {
					attributeValueHandler.onAttributeValue(base64Service.base64Decode(base64StringData), attributeDefinition, targetBean, fixtureConversionContext);
					return ;
				}
			} else {
				if (base64StringData == null || base64StringData.isEmpty()) {
					attributeValueHandler.onAttributeValue(new ByteArrayInputStream(EMPTY_BYTE_ARRAY), attributeDefinition, targetBean, fixtureConversionContext);
					return ;
				} else {
					InputStream inputStream = new ByteArrayInputStream(base64Service.base64Decode(base64StringData));
					attributeValueHandler.onAttributeValue(inputStream, attributeDefinition, targetBean, fixtureConversionContext);
					return ;
				}
			}
		} else {
			throw new FixtureDeploymentException(
					"unsupported attribute type: " 
					+ attributeDefinition.getClass().getSimpleName() 
					+ " in attribute " 
					+ attributeDefinition.getName()
			);
		}
	}

	private MissingReferencedItemHandler createLazyReferenceHandler(
			final FixtureConversionContext fixtureConversionContext, 
			final AttributeValueHandler attributeValueHandler, 
			final Attribute attributeDefinition, 
			final ActiveRecord targetBean,
			final DependencyTracker dependencyTracker
	) {
		return new MissingReferencedItemHandler() {
			
			@Override
			public PersistenceItem onMissingReferencedItem(final String referenceKey) throws FixtureDeploymentException {
				// if the referenced item is not found here, then retry setting the reference later
				fixtureConversionContext.putInitialTransactionAppender(new TransactionAppender() {
					
					@Override
					public void appendToTransaction(Transaction transaction) throws FixtureDeploymentException {
						PersistenceItem referencedItem = fixtureConversionContext.getPersistable(referenceKey);
						if (referencedItem == null) {
							throw new FixtureDeploymentException("could not resolve deferred reference '" + referenceKey + "'");
						}
						dependencyTracker.add(referencedItem);
						attributeValueHandler.onAttributeValue(referencedItem.getActiveRecord(), attributeDefinition, targetBean, fixtureConversionContext);
					}
				});
				return null;
			}
		};
	}

	/**
	 * This method only exists for writing tests.
	 * @param recordJsonConverter 
	 */
	public void setRecordJsonConverter(RecordJsonConverter recordJsonConverter) {
		this.recordJsonConverter = recordJsonConverter;
	}

	/**
	 * This method only exists for writing tests.
	 * @param base64Service 
	 */
	public void setBase64Service(Base64Service base64Service) {
		this.base64Service = base64Service;
	}
}
