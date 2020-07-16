package org.bndly.common.bpm.schema.impl;

/*-
 * #%L
 * BPM Schema
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

import org.bndly.common.bpm.api.ContextResolver;
import org.bndly.common.bpm.api.ProcessVariableType;
import org.bndly.schema.api.Record;
import org.bndly.schema.api.RecordContext;
import org.bndly.schema.beans.MissingTypeBindingException;
import org.bndly.schema.beans.SchemaBeanFactory;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.activiti.engine.impl.variable.ValueFields;
import org.activiti.engine.impl.variable.VariableType;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = { VariableType.class, ProcessVariableType.class }, immediate = true)
@Designate(ocd = SchemaBeanVariableType.Configuration.class)
public class SchemaBeanVariableType implements VariableType, ProcessVariableType {

	@ObjectClassDefinition
	public @interface Configuration {

		@AttributeDefinition(
				name = "Automatically create record context",
				description = "If this value is set to true, no RecordContext instances need to be provided when looking "
				+ "up a process variable. This means, that for each variable a separate RecordContext will be created."
		)
		boolean autoCreateRecordContext() default false;
	}
	
	@Reference
	private ContextResolver contextResolver;
	private static final Logger LOG = LoggerFactory.getLogger(SchemaBeanVariableType.class);

	private final List<SchemaBeanFactory> schemaBeanFactories = new ArrayList<>();
	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	private boolean autoCreateRecordContext;

	@Activate
	public void activate(Configuration configuration) {
		autoCreateRecordContext = configuration.autoCreateRecordContext();
	}
	
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
				schemaBeanFactories.add(schemaBeanFactory);
			} finally {
				lock.writeLock().unlock();
			}
		}
	}

	public void removeSchemaBeanFactory(SchemaBeanFactory schemaBeanFactory) {
		if (schemaBeanFactory != null) {
			lock.writeLock().lock();
			try {
				Iterator<SchemaBeanFactory> iterator = schemaBeanFactories.iterator();
				while (iterator.hasNext()) {
					SchemaBeanFactory next = iterator.next();
					if (next == schemaBeanFactory) {
						iterator.remove();
					}
				}
			} finally {
				lock.writeLock().unlock();
			}
		}
	}

	@Override
	public String getTypeName() {
		return "SchemaBeanType";
	}

	@Override
	public boolean isCachable() {
		return true;
	}

	@Override
	public boolean isAbleToStore(Object value) {
		lock.readLock().lock();
		try {
			for (SchemaBeanFactory schemaBeanFactory : schemaBeanFactories) {
				if (schemaBeanFactory.isSchemaBean(value)) {
					return true;
				}
			}
		} finally {
			lock.readLock().unlock();
		}
		return false;
	}

	@Override
	public void setValue(Object value, ValueFields valueFields) {
		lock.readLock().lock();
		try {
			for (SchemaBeanFactory schemaBeanFactory : schemaBeanFactories) {
				Record r = schemaBeanFactory.getRecordFromSchemaBean(value);
				if (r != null) {
					if (schemaBeanFactory.getEngine().getDeployer().getDeployedSchema() == r.getType().getSchema()) {
						valueFields.setTextValue(r.getType().getName());
						valueFields.setTextValue2(r.getType().getSchema().getName());
						valueFields.setLongValue(r.getId());
						LOG.trace("setting value for {} with id {} in schema {}", r.getType().getName(), r.getId(), r.getType().getSchema().getName());
					}
				}
			}
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public Object getValue(ValueFields valueFields) {
		String typeName = valueFields.getTextValue();
		String schemaName = valueFields.getTextValue2();
		Long id = valueFields.getLongValue();
		LOG.trace("getting value from {} with id {} in schema {}", typeName, id, schemaName);
		
		lock.readLock().lock();
		try {
			for (SchemaBeanFactory schemaBeanFactory : schemaBeanFactories) {
				if (schemaName.equals(schemaBeanFactory.getEngine().getDeployer().getDeployedSchema().getName())) {
					RecordContext context = contextResolver.getContext(RecordContext.class);
					if (context == null) {
						if (autoCreateRecordContext) {
							LOG.info("did not get record context for invocation for {} in schema {}", typeName, schemaName);
							context = schemaBeanFactory.getEngine().getAccessor().buildRecordContext();
						} else {
							LOG.error("did not get record context for invocation for {} in schema {}", typeName, schemaName);
						}
					}
					if (context != null) {
						try {
							Record rec = context.create(typeName, id);
							Object bean = schemaBeanFactory.getSchemaBean(rec);
							return bean;
						} catch (MissingTypeBindingException e) {
							LOG.error("missing type binding for {} in schema {}", typeName, schemaName);
						}
					}
				}

			}
		} finally {
			lock.readLock().unlock();
		}
		return null;
	}

}
