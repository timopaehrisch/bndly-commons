package org.bndly.schema.beans;

/*-
 * #%L
 * Schema Beans
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

import org.bndly.schema.json.beans.StreamingObject;
import org.bndly.schema.json.beans.JSONSchemaBeanFactory;
import org.bndly.common.json.model.JSObject;
import org.bndly.common.lang.StringUtil;
import org.bndly.schema.api.Record;
import org.bndly.schema.api.RecordContext;
import org.bndly.schema.api.RecordList;
import org.bndly.schema.api.Transaction;
import org.bndly.schema.api.VirtualAttributeAdapter;
import org.bndly.schema.api.services.Engine;
import org.bndly.schema.beans.impl.Invoker;
import org.bndly.schema.model.Attribute;
import org.bndly.schema.model.BinaryAttribute;
import org.bndly.schema.model.BooleanAttribute;
import org.bndly.schema.model.CryptoAttribute;
import org.bndly.schema.model.DateAttribute;
import org.bndly.schema.model.DecimalAttribute;
import org.bndly.schema.model.InverseAttribute;
import org.bndly.schema.model.JSONAttribute;
import org.bndly.schema.model.NamedAttributeHolderAttribute;
import org.bndly.schema.model.StringAttribute;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SchemaBeanInvocationHandler implements InvocationHandler, ActiveRecord {

	private final Record record;
	private final SchemaBeanFactory schemaBeanFactory;
	private final JSONSchemaBeanFactory jsonSchemaBeanFactory;
	private final Engine engine;
	private final Map<Method, Invoker> invokersByMethod;

	public SchemaBeanInvocationHandler(Record record, SchemaBeanFactory schemaBeanFactory, JSONSchemaBeanFactory jsonSchemaBeanFactory, Engine engine, Map<Method, Invoker> invokersByMethod) {
		if (record == null) {
			throw new IllegalArgumentException("can not create a schema bean invocation handler without a record object");
		}
		if (invokersByMethod == null) {
			throw new IllegalArgumentException("can not create a schema bean invocation handler without a map of invokers");
		}
		this.record = record;
		this.schemaBeanFactory = schemaBeanFactory;
		this.jsonSchemaBeanFactory = jsonSchemaBeanFactory;
		this.engine = engine;
		this.invokersByMethod = invokersByMethod;
	}

	public SchemaBeanInvocationHandler(String typeName, Engine engine, SchemaBeanFactory schemaBeanFactory, JSONSchemaBeanFactory jsonSchemaBeanFactory, Map<Method, Invoker> invokersByMethod) {
		this(engine.getAccessor().buildRecordContext().create(typeName), schemaBeanFactory, jsonSchemaBeanFactory, engine, invokersByMethod);
	}

	public SchemaBeanInvocationHandler(
			String typeName, 
			Engine engine, 
			long id, 
			SchemaBeanFactory schemaBeanFactory, 
			JSONSchemaBeanFactory jsonSchemaBeanFactory, 
			Map<Method, Invoker> invokersByMethod
	) {
		this(engine.getAccessor().buildRecordContext().create(typeName, id), schemaBeanFactory, jsonSchemaBeanFactory, engine, invokersByMethod);
	}

	public Record getRecord() {
		return record;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		// precompile the methods into invokers!
		Invoker invoker = invokersByMethod.get(method);
		if (invoker == null) {
			invoker = compileInvoker(method);
			invokersByMethod.put(method, invoker);
		}
		return invoker.invoke(this, args);
	}

	public boolean isAttributePresent(String attributeName) {
		return record.isAttributePresent(attributeName);

	}

	protected static String getAttributeNameFromMethod(Method method, String prefix) {
		SchemaAttribute schemaAttribute = method.getAnnotation(SchemaAttribute.class);
		String attributeName = null;
		if (schemaAttribute != null) {
			attributeName = schemaAttribute.value();
		}
		if (attributeName == null || attributeName.isEmpty()) {
			attributeName = method.getName().substring(prefix.length());
			attributeName = StringUtil.lowerCaseFirstLetter(attributeName);
		}
		return attributeName;
	}

	private Record getRecordOfSchemaBean(Object schemaBean) {
		InvocationHandler ih = Proxy.getInvocationHandler(schemaBean);
		if (SchemaBeanInvocationHandler.class.isInstance(ih)) {
			SchemaBeanInvocationHandler sbih = SchemaBeanInvocationHandler.class.cast(ih);
			return sbih.record;
		}
		throw new IllegalArgumentException("provided object was not a schema bean.");
	}

	@Override
	public void persist(Transaction transaction) {
		engine.getAccessor().buildInsertQuery(record, transaction);
	}

	@Override
	public void persistCascaded(Transaction transaction) {
		engine.getAccessor().buildInsertCascadedQuery(record, transaction);
	}

	@Override
	public void update(Transaction transaction) {
		engine.getAccessor().buildUpdateQuery(record, transaction);
	}

	@Override
	public void updateCascaded(Transaction transaction) {
		engine.getAccessor().buildUpdateCascadedQuery(record, transaction);
	}

	@Override
	public void delete(Transaction transaction) {
		engine.getAccessor().buildDeleteQuery(record, transaction);
	}

	@Override
	public void persist() {
		engine.getAccessor().insert(record);
	}

	@Override
	public void persistCascaded() {
		engine.getAccessor().insertCascaded(record);
	}

	@Override
	public void update() {
		engine.getAccessor().update(record);
	}

	@Override
	public void updateCascaded() {
		engine.getAccessor().updateCascaded(record);
	}

	@Override
	public void updatePostPersist(Transaction transaction) {
		engine.getAccessor().buildUpdateQueryPostPersist(record, transaction);
	}

	@Override
	public void delete() {
		Transaction tx = engine.getQueryRunner().createTransaction();
		engine.getAccessor().delete(record, tx);
		tx.commit();
	}

	@Override
	public void reload() {
		engine.getAccessor().readById(record.getType().getName(), record.getId(), record.getContext());
	}

	@Override
	public Long getId() {
		return record.getId();
	}

	@Override
	public boolean isReference() {
		return record.isReference();
	}

	private Object getValueOfVirtualAttribute(String attributeName) {
		return getValueOfVirtualAttribute(record.getAttributeDefinition(attributeName));
	}
	
	private Object getValueOfVirtualAttribute(Attribute att) {
		VirtualAttributeAdapter adapter = engine.getVirtualAttributeAdapterRegistry().getAdapterForAttributeAndType(att, record.getType());
		if (adapter != null) {
			return adapter.read(att, record);
		} else {
			return record.getAttributeValue(att.getName());
		}
	}

	private void setValueOfVirtualAttribute(Attribute att, Object value) {
		VirtualAttributeAdapter adapter = engine.getVirtualAttributeAdapterRegistry().getAdapterForAttributeAndType(att, record.getType());
		if (adapter != null) {
			adapter.write(att, record, value);
		} else {
			record.setAttributeValue(att.getName(), value);
		}
	}

	private Invoker compileInvoker(final Method method) {
		if (method.getDeclaringClass().equals(ActiveRecord.class)) {
			return new Invoker() {

				@Override
				public Object invoke(SchemaBeanInvocationHandler schemaBeanInvocationHandler, Object[] args) throws Throwable {
					try {
						return method.invoke(schemaBeanInvocationHandler, args);
					} catch (InvocationTargetException e) {
						throw e.getTargetException();
					}
				}
			};
		} else if (method.getDeclaringClass().equals(Object.class)) {
			return new Invoker() {

				@Override
				public Object invoke(SchemaBeanInvocationHandler schemaBeanInvocationHandler, Object[] args) throws Throwable {
					try {
						return method.invoke(schemaBeanInvocationHandler, args);
					} catch (InvocationTargetException e) {
						throw e.getTargetException();
					}
				}
			};
		} else {
			final String attributeName;
			final boolean isGetter;
			if (method.getName().startsWith("get")) {
				isGetter = true;
				attributeName = getAttributeNameFromMethod(method, "get");
			} else if (method.getName().startsWith("set")) {
				isGetter = false;
				attributeName = getAttributeNameFromMethod(method, "set");
			} else {
				throw new IllegalStateException("could not create invoker for method: " + method);
			}
			final Class<?> returnType = method.getReturnType();
			final Attribute attributeDefinition = record.getAttributeDefinition(attributeName);
			final boolean isVirtual = record.isVirtualAttribute(attributeName);
			if (
					StringAttribute.class.isInstance(attributeDefinition) 
					|| DecimalAttribute.class.isInstance(attributeDefinition) 
					|| DateAttribute.class.isInstance(attributeDefinition) 
					|| BooleanAttribute.class.isInstance(attributeDefinition) 
					|| CryptoAttribute.class.isInstance(attributeDefinition)
			) {
				return compileSimpleAttributeInvoker(isGetter, isVirtual, attributeDefinition, returnType);
			} else if (NamedAttributeHolderAttribute.class.isInstance(attributeDefinition)) {
				return compileNamedAttributeHolderAttributeInvoker(isGetter, isVirtual, (NamedAttributeHolderAttribute) attributeDefinition, returnType);
			} else if (InverseAttribute.class.isInstance(attributeDefinition)) {
				return compileInverseAttributeInvoker(isGetter, isVirtual, (InverseAttribute) attributeDefinition, returnType);
			} else if (JSONAttribute.class.isInstance(attributeDefinition)) {
				return compileJSONAttributeInvoker(isGetter, isVirtual, (JSONAttribute) attributeDefinition, returnType);
			} else if (BinaryAttribute.class.isInstance(attributeDefinition)) {
				return compileBinaryAttributeInvoker(isGetter, isVirtual, (BinaryAttribute) attributeDefinition, returnType);
			} else {
				throw new IllegalStateException("unsupported attribute definition: " + attributeDefinition);
			}
		}
	}
	
	private void assertAttributeIsPresent(String attributeName) {
		// if the attribute is not present and the record has an id, then reload it, in order to fetch the missing attribute
		if (
				!getRecord().isAttributePresent(attributeName)
				&& getRecord().getId() != null
				) {
			reload();
		}
	}
	
	private RecordList wrapSchemaBeanListAsRecordList(InverseAttribute attributeDefinition, final List listOfSchemaBeans) {
		RecordList value = getRecord().getContext().createList(new RecordContext.RecordListInitializer() {

			@Override
			public Iterator<Record> initialize() {
				return new Iterator<Record>() {
					private Iterator iter;
					private Object current;

					private Iterator getIter() {
						if (iter == null) {
							iter = listOfSchemaBeans.iterator();
							if (iter == null) {
								throw new IllegalStateException("iterator could not be created");
							}
						}
						return iter;
					}

					@Override
					public boolean hasNext() {
						boolean r = getIter().hasNext();
						current = null;
						return r;
					}

					@Override
					public Record next() {
						current = getIter().next();
						Record currentRecord = getRecordOfSchemaBean(current);
						return currentRecord;
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException("Not supported.");
					}

				};
			}
		}, getRecord(), attributeDefinition);
		return value;
	}

	private Invoker compileSimpleAttributeInvoker(final boolean isGetter, final boolean isVirtual, final Attribute attributeDefinition, final Class<?> returnType) {
		final String attributeName = attributeDefinition.getName();
		// simple attributes are just plain get and set. there is no filtering or transformation involved
		if (isGetter) {
			// get
			if (isVirtual) {
				return new Invoker() {
					
					@Override
					public Object invoke(SchemaBeanInvocationHandler schemaBeanInvocationHandler, Object[] args) throws Throwable {
						return schemaBeanInvocationHandler.getValueOfVirtualAttribute(attributeDefinition);
					}
				};
			} else {
				return new Invoker() {
					
					@Override
					public Object invoke(SchemaBeanInvocationHandler schemaBeanInvocationHandler, Object[] args) throws Throwable {
						// if the attribute is not present and the record has an id, then reload it, in order to fetch the missing attribute
						schemaBeanInvocationHandler.assertAttributeIsPresent(attributeName);
						return schemaBeanInvocationHandler.getRecord().getAttributeValue(attributeName, returnType);
					}
				};
			}
		} else {
			//set
			if (isVirtual) {
				return new Invoker() {
					
					@Override
					public Object invoke(SchemaBeanInvocationHandler schemaBeanInvocationHandler, Object[] args) throws Throwable {
						schemaBeanInvocationHandler.setValueOfVirtualAttribute(attributeDefinition, args[0]);
						return null;
					}
				};
			} else {
				return new Invoker() {
					
					@Override
					public Object invoke(SchemaBeanInvocationHandler schemaBeanInvocationHandler, Object[] args) throws Throwable {
						Record record = schemaBeanInvocationHandler.getRecord();
						record.setAttributeValue(attributeName, args[0]);
						return null;
					}
				};
			}
		}
	}
	
	private Invoker compileNamedAttributeHolderAttributeInvoker(
			final boolean isGetter, 
			final boolean isVirtual, 
			final NamedAttributeHolderAttribute attributeDefinition, 
			final Class<?> returnType
	) {
		final String attributeName = attributeDefinition.getName();
		if (isGetter) {
			// get
			if (isVirtual) {
				return new Invoker() {

					@Override
					public Object invoke(SchemaBeanInvocationHandler schemaBeanInvocationHandler, Object[] args) throws Throwable {
						Object v = schemaBeanInvocationHandler.getValueOfVirtualAttribute(attributeDefinition);
						if (v == null) {
							return null;
						} else {
							// return a schema bean
							return schemaBeanFactory.getSchemaBean(returnType, (Record) v);
						}
					}
				};
			} else {
				return new Invoker() {

					@Override
					public Object invoke(SchemaBeanInvocationHandler schemaBeanInvocationHandler, Object[] args) throws Throwable {
						schemaBeanInvocationHandler.assertAttributeIsPresent(attributeName);
						Object v = schemaBeanInvocationHandler.getRecord().getAttributeValue(attributeName, Record.class);
						if (v == null) {
							return null;
						} else {
							// return a schema bean
							return schemaBeanFactory.getSchemaBean(returnType, (Record) v);
						}
					}
				};
			}
		} else {
			// set
			if (isVirtual) {
				return new Invoker() {

					@Override
					public Object invoke(SchemaBeanInvocationHandler schemaBeanInvocationHandler, Object[] args) throws Throwable {
						Object value = args[0];
						if (value == null) {
							// set null
							schemaBeanInvocationHandler.setValueOfVirtualAttribute(attributeDefinition, null);
						} else {
							Record recordValue = getRecordOfSchemaBean(value);
							schemaBeanInvocationHandler.setValueOfVirtualAttribute(attributeDefinition, recordValue);
						}
						return null;
					}
				};
			} else {
				return new Invoker() {

					@Override
					public Object invoke(SchemaBeanInvocationHandler schemaBeanInvocationHandler, Object[] args) throws Throwable {
						Record record = schemaBeanInvocationHandler.getRecord();
						Object value = args[0];
						if (value == null) {
							// set null
							record.setAttributeValue(attributeName, null);
						} else {
							Record recordValue = getRecordOfSchemaBean(value);
							record.setAttributeValue(attributeName, recordValue);
						}
						return null;
					}
				};
			}
		}
	}
	
	private Invoker compileInverseAttributeInvoker(final boolean isGetter, final boolean isVirtual, final InverseAttribute attributeDefinition, final Class<?> returnType) {
		final String attributeName = attributeDefinition.getName();
		if (isGetter) {
			// get
			if (isVirtual) {
				return new Invoker() {

					@Override
					public Object invoke(SchemaBeanInvocationHandler schemaBeanInvocationHandler, Object[] args) throws Throwable {
						Object v = schemaBeanInvocationHandler.getValueOfVirtualAttribute(attributeDefinition);
						if (v == null) {
							return null;
						} else {
							List schemaBeanList = new ArrayList();
							Iterable c = (Iterable) v;
							for (Object object : c) {
								Object bean = schemaBeanFactory.getSchemaBean((Record) object);
								schemaBeanList.add(bean);
							}
							return schemaBeanList;
						}
					}
				};
			} else {
				return new Invoker() {

					@Override
					public Object invoke(SchemaBeanInvocationHandler schemaBeanInvocationHandler, Object[] args) throws Throwable {
						schemaBeanInvocationHandler.assertAttributeIsPresent(attributeName);
						RecordList list = schemaBeanInvocationHandler.getRecord().getAttributeValue(attributeName, RecordList.class);
						if (list == null) {
							return null;
						} else {
							List schemaBeanList = new ArrayList();
							for (Record item : list) {
								Object bean = schemaBeanFactory.getSchemaBean(item);
								schemaBeanList.add(bean);
							}
							return schemaBeanList;
						}
					}
				};
			}
		} else {
			// set
			if (isVirtual) {
				return new Invoker() {

					@Override
					public Object invoke(SchemaBeanInvocationHandler schemaBeanInvocationHandler, Object[] args) throws Throwable {
						final List listOfSchemaBeans = (List) args[0];
						if (listOfSchemaBeans == null) {
							schemaBeanInvocationHandler.setValueOfVirtualAttribute(attributeDefinition, null);
						} else {
							RecordList value = schemaBeanInvocationHandler.wrapSchemaBeanListAsRecordList(attributeDefinition, listOfSchemaBeans);
							schemaBeanInvocationHandler.setValueOfVirtualAttribute(attributeDefinition, value);
						}
						return null;
					}
				};
			} else {
				return new Invoker() {

					@Override
					public Object invoke(SchemaBeanInvocationHandler schemaBeanInvocationHandler, Object[] args) throws Throwable {
						final List listOfSchemaBeans = (List) args[0];
						if (listOfSchemaBeans == null) {
							schemaBeanInvocationHandler.getRecord().setAttributeValue(attributeName, null);
						} else {
							RecordList value = schemaBeanInvocationHandler.wrapSchemaBeanListAsRecordList(attributeDefinition, listOfSchemaBeans);
							schemaBeanInvocationHandler.getRecord().setAttributeValue(attributeName, value);
						}
						return null;
					}
				};
			}
		}
	}
	
	private Invoker compileJSONAttributeInvoker(final boolean isGetter, final boolean isVirtual, final JSONAttribute attributeDefinition, final Class<?> returnType) {
		final String attributeName = attributeDefinition.getName();
		if (isGetter) {
			// get
			if (isVirtual) {
				return new Invoker() {

					@Override
					public Object invoke(SchemaBeanInvocationHandler schemaBeanInvocationHandler, Object[] args) throws Throwable {
						Object value = schemaBeanInvocationHandler.getValueOfVirtualAttribute(attributeDefinition);
						if (value == null) {
							return null;
						}
						
						if (JSObject.class.isInstance(value)) {
							Object bean = jsonSchemaBeanFactory.getSchemaBean((JSObject) value);
							return bean;
						} else if (Record.class.isInstance(value)) {
							return schemaBeanFactory.getSchemaBean(returnType, (Record) value);
						} else if (schemaBeanFactory.isSchemaBean(value)) {
							// NO-OP
							return value;
						} else {
							throw new IllegalStateException("json attributes should be stored as jsobject or record itself within the owner record.");
						}
					}
				};
			} else {
				return new Invoker() {

					@Override
					public Object invoke(SchemaBeanInvocationHandler schemaBeanInvocationHandler, Object[] args) throws Throwable {
						schemaBeanInvocationHandler.assertAttributeIsPresent(attributeName);
						Object value = schemaBeanInvocationHandler.getRecord().getAttributeValue(attributeName);
						if (value == null) {
							return null;
						}
						
						if (JSObject.class.isInstance(value)) {
							Object bean = jsonSchemaBeanFactory.getSchemaBean((JSObject) value);
							return bean;
						} else if (Record.class.isInstance(value)) {
							return schemaBeanFactory.getSchemaBean(returnType, (Record) value);
						} else if (schemaBeanFactory.isSchemaBean(value)) {
							// NO-OP
							return value;
						} else {
							throw new IllegalStateException("json attributes should be stored as jsobject or record itself within the owner record.");
						}
					}
				};
			}
		} else {
			// set
			if (isVirtual) {
				return new Invoker() {

					@Override
					public Object invoke(SchemaBeanInvocationHandler schemaBeanInvocationHandler, Object[] args) throws Throwable {
						Object value = args[0];
						if (schemaBeanFactory.isSchemaBean(value)) {
							value = getRecordOfSchemaBean(value);
						} else if (jsonSchemaBeanFactory.isSchemaBean(value)) {
							final StreamingObject jsonBean = (StreamingObject) value;
							value = jsonBean.getJSObject();
						}
						schemaBeanInvocationHandler.setValueOfVirtualAttribute(attributeDefinition, value);
						return null;
					}
				};
			} else {
				return new Invoker() {

					@Override
					public Object invoke(SchemaBeanInvocationHandler schemaBeanInvocationHandler, Object[] args) throws Throwable {
						Object value = args[0];
						if (value == null) {
							schemaBeanInvocationHandler.getRecord().setAttributeValue(attributeName, null);
						} else {
							if (schemaBeanFactory.isSchemaBean(value)) {
								value = getRecordOfSchemaBean(value);
							} else if (jsonSchemaBeanFactory.isSchemaBean(value)) {
								final StreamingObject jsonBean = (StreamingObject) value;
								value = jsonBean.getJSObject();
							}
							schemaBeanInvocationHandler.getRecord().setAttributeValue(attributeName, value);
						}
						return null;
					}
				};
			}
		}
	}
	
	private Invoker compileBinaryAttributeInvoker(final boolean isGetter, final boolean isVirtual, final BinaryAttribute attributeDefinition, final Class<?> returnType) {
		final String attributeName = attributeDefinition.getName();
		if (isGetter) {
			// get
			if (isVirtual) {
				return new Invoker() {

					@Override
					public Object invoke(SchemaBeanInvocationHandler schemaBeanInvocationHandler, Object[] args) throws Throwable {
						return schemaBeanInvocationHandler.getValueOfVirtualAttribute(attributeName);
					}
				};
			} else {
				return new Invoker() {

					@Override
					public Object invoke(SchemaBeanInvocationHandler schemaBeanInvocationHandler, Object[] args) throws Throwable {
						schemaBeanInvocationHandler.assertAttributeIsPresent(attributeName);
						return schemaBeanInvocationHandler.getRecord().getAttributeValue(attributeName, returnType);
					}
				};
			}
		} else {
			// set
			if (isVirtual) {
				return new Invoker() {

					@Override
					public Object invoke(SchemaBeanInvocationHandler schemaBeanInvocationHandler, Object[] args) throws Throwable {
						Object value = args[0];
						schemaBeanInvocationHandler.setValueOfVirtualAttribute(attributeDefinition, value);
						return null;
					}
				};
			} else {
				return new Invoker() {

					@Override
					public Object invoke(SchemaBeanInvocationHandler schemaBeanInvocationHandler, Object[] args) throws Throwable {
						Object value = args[0];
						schemaBeanInvocationHandler.getRecord().setAttributeValue(attributeName, value);
						return null;
					}
				};
			}
		}
	}

}
