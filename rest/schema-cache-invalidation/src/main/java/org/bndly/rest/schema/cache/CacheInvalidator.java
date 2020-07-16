package org.bndly.rest.schema.cache;

/*-
 * #%L
 * REST Schema Cache Invalidation
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

import org.bndly.rest.cache.api.CacheLinkingService;
import org.bndly.rest.cache.api.CacheTransaction;
import org.bndly.rest.cache.api.CacheTransactionFactory;
import org.bndly.rest.cache.api.ContextCacheTransactionProvider;
import org.bndly.schema.api.Record;
import org.bndly.schema.api.RecordAttributeIterator;
import org.bndly.schema.api.listener.DeleteListener;
import org.bndly.schema.api.listener.MergeListener;
import org.bndly.schema.api.listener.PersistListener;
import org.bndly.schema.api.listener.SchemaDeploymentListener;
import org.bndly.schema.api.services.Engine;
import org.bndly.schema.model.Attribute;
import org.bndly.schema.model.InverseAttribute;
import org.bndly.schema.model.MixinAttribute;
import org.bndly.schema.model.Schema;
import org.bndly.schema.model.Type;
import org.bndly.schema.model.TypeAttribute;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = { SchemaDeploymentListener.class, PersistListener.class, MergeListener.class, DeleteListener.class }, immediate = true)
public class CacheInvalidator implements SchemaDeploymentListener, PersistListener, MergeListener, DeleteListener {
	private static final Logger LOG = LoggerFactory.getLogger(CacheInvalidator.class);
	
	@Reference
	private CacheTransactionFactory cacheTransactionFactory;
	@Reference
	private ContextCacheTransactionProvider contextCacheTransactionProvider;
	@Reference
	private CacheLinkingService cacheLinkingService;
	
	private final List<DeployedSchema> deployedSchemata = new ArrayList<>();

	private static class DeployedSchema {
		private final Schema schema;
		private final Engine engine;

		public DeployedSchema(Schema schema, Engine engine) {
			this.schema = schema;
			this.engine = engine;
		}

		public Engine getEngine() {
			return engine;
		}

		public Schema getSchema() {
			return schema;
		}
		
	}
	
	@Activate
	public void activate() {
	}
	
	@Deactivate
	public void deactivate() {
		for (DeployedSchema ds : deployedSchemata) {
			ds.getEngine().removeListener(this);
		}
		deployedSchemata.clear();
	}
	
	@Override
	public void schemaDeployed(Schema deployedSchema, Engine engine) {
		DeployedSchema ds = getDeployedSchema(deployedSchema);
		if (ds == null) {
			deployedSchemata.add(new DeployedSchema(deployedSchema, engine));
			engine.addListener(this);
		}
	}

	private DeployedSchema getDeployedSchema(Schema schema) {
		for (DeployedSchema ds : deployedSchemata) {
			if (ds.getSchema() == schema) {
				return ds;
			}
		}
		return null;
	}

	@Override
	public void schemaUndeployed(Schema deployedSchema, Engine engine) {
		DeployedSchema ds = getDeployedSchema(deployedSchema);
		if (ds != null) {
			ds.getEngine().removeListener(this);
			deployedSchemata.remove(ds);
		}
	}

	@Override
	public void onPersist(Record record) {
		invalidateRecord(record);
	}

	@Override
	public void onMerge(Record record) {
		invalidateRecord(record);
	}

	@Override
	public void onDelete(Record record) {
		invalidateRecord(record);
	}

	private void invalidateRecord(Record record) {
		DeployedSchema ds = getDeployedSchema(record.getType().getSchema());
		CacheTransaction existingCacheTransaction = contextCacheTransactionProvider.getCacheTransaction();
		if (existingCacheTransaction != null) {
			invalidateRecord(record, new HashSet<String>(), ds, existingCacheTransaction);
		} else {
			try (CacheTransaction cacheTransaction = cacheTransactionFactory.createCacheTransaction()) {
				invalidateRecord(record, new HashSet<String>(), ds, cacheTransaction);
			}
		}
	}
	
	private String buildCachePathToRecordType(Record record) {
		Type type = record.getType();
		Schema schema = type.getSchema();
		
		StringBuffer sb = new StringBuffer();
		String schemaName = schema.getName();
		String typeName = type.getName();
		sb.append(schemaName).append('/').append(typeName);
		String path = sb.toString();
		return path;
	}
	
	private String buildCachePathToRecord(Record record) {
		StringBuffer sb = new StringBuffer(buildCachePathToRecordType(record));
		Long id = record.getId();
		if (id != null) {
			sb.append('/').append(id.longValue());
		}
		String path = sb.toString();
		return path;
	}

	private void invalidateRecord(Record record, Set<String> invalidated, DeployedSchema ds, CacheTransaction cacheTransaction) {
		if (ds != null) {
			String typePath = buildCachePathToRecordType(record);
			String path = buildCachePathToRecord(record);
			if (invalidated.contains(path)) {
				return;
			}
			invalidated.add(path);
			// delete the cache entry for the directly created/modified instance
			LOG.debug("flushing {}", path);
			cacheTransaction.flushRecursive(path);
			cacheTransaction.flush(typePath);

			// TODO: care about references to the created/modified instance
			// example: created a cart item for a cart. cart item cache entry 
			// will be flushed, but what about the cart that contains the item?
			// since we hold the record, we also hold the objects that we refer to.
			// this means we could inspect the current record for attributes 
			// that refer to mixins or types. if there is a mixin or type, we can
			// invalidate the according record as well. ATTENTION: infinite recursion
			invalidateComplexAttributesOfRecord(record, invalidated, ds, path, cacheTransaction);

		}
	}

	private void invalidateComplexAttributesOfRecord(
			Record record, 
			final Set<String> invalidated, 
			final DeployedSchema ds, 
			final String containingRecordCachePath, 
			final CacheTransaction cacheTransaction
	) {
		record.iteratePresentValues(new RecordAttributeIterator() {

			@Override
			public void handleAttribute(Attribute attribute, Record record) {
				if (!attribute.isVirtual()) {
					boolean isMixin = MixinAttribute.class.isInstance(attribute);
					boolean isType = TypeAttribute.class.isInstance(attribute);
					boolean isInverse = InverseAttribute.class.isInstance(attribute);
					if (isMixin || isType) {
						Object attributeValue = record.getAttributeValue(attribute.getName());
						if (attributeValue != null) {
							if (Record.class.isInstance(attributeValue)) {
								Record nestedRecord = (Record) attributeValue;
								invalidateRecord(nestedRecord, invalidated, ds, cacheTransaction);
							}
						}
					} else if (isInverse) {
						Object attributeValue = record.getAttributeValue(attribute.getName());
						if (Collection.class.isInstance(attributeValue)) {
							Collection collection = (Collection) attributeValue;
							for (Object collectionItem : collection) {
								if (Record.class.isInstance(collectionItem)) {
									Record recordItem = (Record) collectionItem;
									// link the item to the containing record.
									// this allows to flush the containing record, if them item changes.
									String recordItemCachePath = buildCachePathToRecord(recordItem);
									try {
										LOG.debug("linking {} to {}", recordItemCachePath, containingRecordCachePath);
										cacheLinkingService.link(recordItemCachePath, containingRecordCachePath);
									} catch (IOException ex) {
										LOG.error("could not link '{}' with '{}' : {}", recordItemCachePath, containingRecordCachePath, ex.getMessage(), ex);
									}
								}
							}
						}
					}
				}
			}
		});
	}

}
