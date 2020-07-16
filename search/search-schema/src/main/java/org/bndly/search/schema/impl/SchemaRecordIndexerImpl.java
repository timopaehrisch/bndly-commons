package org.bndly.search.schema.impl;

/*-
 * #%L
 * Search Schema
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

import org.bndly.code.common.CodeGenerationContext;
import org.bndly.code.model.XMLElement;
import org.bndly.code.output.XMLWriter;
import org.bndly.common.data.api.ChangeableData;
import org.bndly.common.data.api.Data;
import org.bndly.common.data.api.DataStore;
import org.bndly.common.data.io.ReplayableInputStream;
import org.bndly.common.data.api.SimpleData;
import org.bndly.common.osgi.util.DictionaryAdapter;
import org.bndly.schema.api.Record;
import org.bndly.schema.api.listener.DeleteListener;
import org.bndly.schema.api.listener.MergeListener;
import org.bndly.schema.api.listener.PersistListener;
import org.bndly.schema.api.listener.QueryByExampleIteratorListener;
import org.bndly.schema.api.listener.SchemaDeploymentListener;
import org.bndly.schema.api.services.Engine;
import org.bndly.schema.model.Attribute;
import org.bndly.schema.model.BinaryAttribute;
import org.bndly.schema.model.BooleanAttribute;
import org.bndly.schema.model.DateAttribute;
import org.bndly.schema.model.DecimalAttribute;
import org.bndly.schema.model.InverseAttribute;
import org.bndly.schema.model.Mixin;
import org.bndly.schema.model.NamedAttributeHolder;
import org.bndly.schema.model.NamedAttributeHolderAttribute;
import org.bndly.schema.model.Schema;
import org.bndly.schema.model.StringAttribute;
import org.bndly.schema.model.Type;
import org.bndly.search.api.DocumentFieldValue;
import org.bndly.search.api.DocumentFieldValueProvider;
import org.bndly.search.api.ReindexService;
import org.bndly.search.api.SearchIndexService;
import org.bndly.search.api.SearchServiceListener;
import org.bndly.search.schema.api.SchemaRecordIndexer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@Component(
		service = {
			PersistListener.class,
			MergeListener.class,
			DeleteListener.class,
			SchemaDeploymentListener.class,
			SearchServiceListener.class,
			ReindexService.class,
			SchemaRecordIndexer.class
		},
		immediate = true,
		configurationPolicy = ConfigurationPolicy.REQUIRE
)
@Designate(ocd = SchemaRecordIndexerImpl.Configuration.class)
public class SchemaRecordIndexerImpl implements PersistListener, MergeListener, DeleteListener, SchemaDeploymentListener, SearchServiceListener, ReindexService, SchemaRecordIndexer {

	@ObjectClassDefinition(
			name = "Schema Record Search Indexer"
	)
	public @interface Configuration {
		@AttributeDefinition(
				name = "Excluded Types",
				description = "Names of types to be excluded from indexing. Names should follow the pattern of SCHEMANAME:TYPENAME. Example: ebx:BackendAccount"
		)
		String[] excludedTypes();
	}
	
	@Reference
	private DataStore dataStore;
	@Reference
	private SearchIndexService searchIndexService;
	private final Map<Integer, RecordSearchDocumentAdapter> globalDocumentAdapters = new HashMap<>();
	private boolean isReady;
	private final List<KnownEngine> knownEngines = new ArrayList<>();
	private Map<String,Set<String>> excludedTypesPerSchema = new HashMap<>();
	
	private class KnownEngine {
		private final Engine engine;
		private final Schema schema;
		private final Map<String, RecordSearchDocumentAdapter> documentAdapters = new HashMap<>();

		public KnownEngine(Engine engine, Schema schema) {
			this.engine = engine;
			this.schema = schema;
		}
		
	}

	@Activate
	public void activate(ComponentContext componentContext) {
		DictionaryAdapter adapter = new DictionaryAdapter(componentContext.getProperties());
		Collection<String> tmp = adapter.getStringCollection("excludedTypes");
		if (tmp != null) {
			for (String excludedType : tmp) {
				int i = excludedType.indexOf(":");
				if (i > -1) {
					String schema = excludedType.substring(0, i);
					String typeName = excludedType.substring(i + 1);
					Set<String> types = excludedTypesPerSchema.get(schema);
					if (types == null) {
						types = new HashSet<>();
						excludedTypesPerSchema.put(schema, types);
					}
					types.add(typeName);
				}
			}
		}
	}
	
	@Deactivate
	public void deactivate() {
		excludedTypesPerSchema.clear();
	}
	
	@Override
	public void onPersist(final Record record) {
		if (!isReady) {
			return;
		}
		if (isIndexedRecord(record)) {
			final RecordSearchDocumentAdapter adapter = getSearchDocumentAdapter(record);
			DocumentFieldValueProvider valueProvider = new DocumentFieldValueProvider() {
				@Override
				public List<DocumentFieldValue> getDocumentFieldValues() {
					List<DocumentFieldValue> v = adapter.buildDocumentFieldValues(record);
					return v;
				}
			};
			searchIndexService.addToIndex(valueProvider);
		}
	}

	@Override
	public void onMerge(Record record) {
		onPersist(record);
	}

	@Override
	public void onDelete(final Record record) {
		if (!isReady) {
			return;
		}
		if (isIndexedRecord(record)) {
			DocumentFieldValueProvider valueProvider = new DocumentFieldValueProvider() {
				@Override
				public List<DocumentFieldValue> getDocumentFieldValues() {
					List<DocumentFieldValue> l = new ArrayList<>();
					l.add(new RecordIdFieldValue(record));
					return l;
				}
			};
			searchIndexService.removeFromIndex(valueProvider);
		}
	}

	@Override
	public void reindex(final String typeName, Engine engine) {
		searchIndexService.removeFromIndex(new DocumentFieldValueProvider() {

			@Override
			public List<DocumentFieldValue> getDocumentFieldValues() {
				List<DocumentFieldValue> l = new ArrayList<>();
				l.add(new DocumentFieldValue() {

					@Override
					public String getFieldName() {
						return "_type";
					}

					@Override
					public Object getValue() {
						return typeName;
					}
				});
				return l;
			}
		});
		searchIndexService.flush();
		engine.getAccessor().iterate(typeName, new QueryByExampleIteratorListener() {
			@Override
			public void handleRecord(Record r) {
				onPersist(r);
			}
		}, 10, false, engine.getAccessor().buildRecordContext());
	}

	@Override
	public void schemaDeployed(Schema deployedSchema, Engine engine) {
		for (KnownEngine knownEngine : knownEngines) {
			if (knownEngine.engine == engine) {
				return;
			}
		}
		KnownEngine knownEngine = new KnownEngine(engine, deployedSchema);
		knownEngines.add(knownEngine);
		List<Type> types = deployedSchema.getTypes();
		if (types != null) {
			for (Type type : types) {
				registerDocumentAdapter(knownEngine, type, new DefaultRecordSearchDocumentAdapter(type));
			}
		}

		// generate a solr config and put it in a data store
		generateSolrConfigFromSchema(deployedSchema);

		engine.addListener(this);
	}

	@Override
	public void schemaUndeployed(Schema deployedSchema, Engine engine) {
		engine.removeListener(this);
		Iterator<KnownEngine> iterator = knownEngines.iterator();
		while (iterator.hasNext()) {
			KnownEngine next = iterator.next();
			if (next.engine == engine) {
				iterator.remove();
				List<Type> types = deployedSchema.getTypes();
				if (types != null) {
					List<Type> keysToRemove = new ArrayList<>();
					for (Type type : types) {
						for (Map.Entry<String, RecordSearchDocumentAdapter> entry : next.documentAdapters.entrySet()) {
							String key = entry.getKey();
							RecordSearchDocumentAdapter recordSearchDocumentAdapter = entry.getValue();
							if (recordSearchDocumentAdapter.getType() == type) {
								keysToRemove.add(type);
							}
						}
					}
					for (Type type : keysToRemove) {
						next.documentAdapters.remove(type.getName());
						globalDocumentAdapters.remove(System.identityHashCode(type));
					}
				}
			}
		}
	}

	private void generateSolrConfigFromSchema(Schema deployedSchema) {
		StringBuilder sb = new StringBuilder();
		CodeGenerationContext ctx = new CodeGenerationContext();
		final XMLElement el = ctx.create(XMLElement.class);
		el.setName("fields");
		XMLElement idFieldEl = el.createElement("field");
		idFieldEl.createAttribute("name", "id");
		idFieldEl.createAttribute("type", "long");
		idFieldEl.createAttribute("indexed", "true");
		idFieldEl.createAttribute("stored", "true");
		idFieldEl.createAttribute("required", "true");

		XMLElement typeFieldEl = el.createElement("field");
		typeFieldEl.createAttribute("name", "_type");
		typeFieldEl.createAttribute("type", "string");
		typeFieldEl.createAttribute("indexed", "true");
		typeFieldEl.createAttribute("stored", "true");
		typeFieldEl.createAttribute("required", "true");

		Iterable<Type> types = deployedSchema.getTypes();
		if (types != null) {
			for (Type type : types) {
				appendAttributesOfNamedAttributeHolder(el, type);
			}
		}

		Iterable<Mixin> mixins = deployedSchema.getMixins();
		if (mixins != null) {
			for (Mixin mixin : mixins) {
				appendAttributesOfNamedAttributeHolder(el, mixin);
			}
		}
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			new XMLWriter().write(el, bos);
			SimpleData d = new SimpleData(null);
			d.setName("solrSchema.xml");
			d.setContentType("application/xml");
			d.setCreatedOn(new Date());
			d.setInputStream(ReplayableInputStream.newInstance(new ByteArrayInputStream(bos.toByteArray())));
			Data found = dataStore.findByName(d.getName());
			if (found == null) {
				dataStore.create(d);
			} else {
				if (ChangeableData.class.isInstance(found)) {
					ChangeableData cd = (ChangeableData) found;
					cd.setUpdatedOn(d.getCreatedOn());
					cd.setInputStream(d.getInputStream());
					dataStore.update(found);
				}
			}

		} catch (IOException ex) {
			throw new IllegalStateException("could not write solrconfig.");
		}

	}

	private void appendAttributesOfNamedAttributeHolder(XMLElement el, NamedAttributeHolder nah) {
		List<Attribute> atts = nah.getAttributes();
		if (atts != null) {
			for (Attribute attribute : atts) {
				if (BinaryAttribute.class.isInstance(attribute)) {
					continue;
				} else if (InverseAttribute.class.isInstance(attribute)) {
					continue;
				}
				XMLElement fieldEl = el.createElement("field");
				fieldEl.createAttribute("name", nah.getName() + "_" + attribute.getName());
				fieldEl.createAttribute("indexed", "true");
				fieldEl.createAttribute("stored", "true");
				String solrTypeName = getSolrTypeNameForAttribute(attribute);
				if (attribute.isMandatory()) {
					fieldEl.createAttribute("required", "true");
				}
				fieldEl.createAttribute("type", solrTypeName);

			}
		}
	}

	private String getSolrTypeNameForAttribute(Attribute attribute) {
		if (BooleanAttribute.class.isInstance(attribute)) {
			return "boolean";
		} else if (BinaryAttribute.class.isInstance(attribute)) {
			return "binary";
		} else if (StringAttribute.class.isInstance(attribute)) {
			return "lc_text";
		} else if (NamedAttributeHolderAttribute.class.isInstance(attribute)) {
			return "long";
		} else if (DateAttribute.class.isInstance(attribute)) {
			return "tdate";
		} else if (DecimalAttribute.class.isInstance(attribute)) {
			DecimalAttribute da = DecimalAttribute.class.cast(attribute);
			Integer length = da.getLength();
			Integer dp = da.getDecimalPlaces();
			if (dp == null) {
				dp = 0;
			}
			if (length == null) {
				if (dp == 0) {
					return "long";
				} else {
					return "double";
				}
			} else {
				return "double";
			}
		} else {
			throw new IllegalStateException("unsupported attribute " + attribute.getName() + " " + attribute.getClass().getName());
		}
	}

	public void registerDocumentAdapter(String schemaName, Type type, RecordSearchDocumentAdapter adapter) {
		for (KnownEngine knownEngine : knownEngines) {
			if (schemaName.equals(knownEngine.schema.getName())) {
				registerDocumentAdapter(knownEngine, type, adapter);
			}
		}
	}
	
	private void registerDocumentAdapter(KnownEngine knownEngine, Type type, RecordSearchDocumentAdapter adapter) {
		if (type == null || adapter == null) {
			return;
		}
		String name = type.getName();
		knownEngine.documentAdapters.put(name, adapter);
		globalDocumentAdapters.put(System.identityHashCode(type), adapter);
	}

	private boolean isIndexedRecord(Record record) {
		Set<String> excludedTypes = excludedTypesPerSchema.get(record.getType().getSchema().getName());
		if (excludedTypes == null) {
			return (getSearchDocumentAdapter(record) != null);
		}
		return (!excludedTypes.contains(record.getType().getName())) && (getSearchDocumentAdapter(record) != null);
	}

	private RecordSearchDocumentAdapter getSearchDocumentAdapter(Record record) {
		RecordSearchDocumentAdapter adapter = globalDocumentAdapters.get(System.identityHashCode(record.getType()));
		return adapter;
	}

	public void setSchemaDeploymentListeners(List schemaDeploymentListeners) {
		schemaDeploymentListeners.add(this);
	}

	public void setDataStore(DataStore dataStore) {
		this.dataStore = dataStore;
	}

	@Override
	public void searchServiceIsReady(String searchServiceName) {
		this.isReady = true;
	}

	@Override
	public void reindex(Type type) {
		for (KnownEngine knownEngine : knownEngines) {
			Schema schema = knownEngine.engine.getDeployer().getDeployedSchema();
			if (schema.getTypes() != null && schema.getTypes().contains(type)) {
				reindex(type.getName(), knownEngine.engine);
				break;
			}
		}
	}

	@Override
	public void reindex() {
		for (KnownEngine knownEngine : knownEngines) {
			searchIndexService.removeFromIndex(new DocumentFieldValueProvider() {
				@Override
				public List<DocumentFieldValue> getDocumentFieldValues() {
					List<DocumentFieldValue> l = new ArrayList<>();
					l.add(new DocumentFieldValue() {

						@Override
						public String getFieldName() {
							return "id";
						}

						@Override
						public Object getValue() {
							return "*";
						}
					});
					return l;
				}
			});

			Schema s = knownEngine.engine.getDeployer().getDeployedSchema();
			for (Type type : s.getTypes()) {
				int batchSize = 10;
				if (!type.isVirtual() && !type.isAbstract()) {
					QueryByExampleIteratorListener listener = new QueryByExampleIteratorListener() {
						@Override
						public void handleRecord(Record r) {
							onPersist(r);
						}
					};
					knownEngine.engine.getAccessor().iterate(type.getName(), listener, batchSize, false, knownEngine.engine.getAccessor().buildRecordContext());
				}
			}
		}
	}

}
