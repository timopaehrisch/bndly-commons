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

import org.bndly.schema.api.services.ConstraintRegistry;
import org.bndly.schema.api.services.Deployer;
import org.bndly.schema.api.services.Engine;
import org.bndly.schema.api.services.QueryContextFactory;
import org.bndly.schema.api.services.TransactionFactory;
import org.bndly.schema.api.services.TableRegistry;
import org.bndly.schema.api.services.VirtualAttributeAdapterRegistry;
import org.bndly.schema.vendor.VendorConfiguration;
import java.util.List;

public class EngineImpl implements Engine {

	private VendorConfiguration vendorConfiguration;
	private MediatorRegistryImpl mediatorRegistry;
	private Deployer deployer;
	private AccessorImpl accessor;
	private TransactionFactory queryRunner;
	private ConstraintRegistry constraintRegistry;
	private TableRegistry tableRegistry;
	private QueryContextFactory queryContextFactory;
	private VirtualAttributeAdapterRegistry virtualAttributeAdapterRegistry;
	private final ListenerRegistry listenerRegistry = new ListenerRegistry();

	@Override
	public void reset() {
		reset(deployer, accessor, queryRunner, tableRegistry, queryContextFactory, constraintRegistry);
	}

	private void reset(Object... o) {
		for (Object object : o) {
			reset(object);
		}
	}

	private void reset(Object o) {
		if (Resetable.class.isInstance(o)) {
			((Resetable) o).reset();
		}
	}

	@Override
	public void addListener(Object object) {
		listenerRegistry.addListener(object);
	}

	@Override
	public void addListenerForTypes(Object object, String... typeNames) {
		listenerRegistry.addListenerForTypes(object, typeNames);
	}
	

	@Override
	public void removeListener(Object object) {
		listenerRegistry.removeListener(object);
	}
	
	public <E> List<E> getListeners(Class<E> listenerType, String typeName) {
		return listenerRegistry.getListeners(listenerType, typeName);
	}

	@Override
	public Deployer getDeployer() {
		return deployer;
	}

	@Override
	public AccessorImpl getAccessor() {
		return accessor;
	}

	@Override
	public TransactionFactory getQueryRunner() {
		return queryRunner;
	}

	@Override
	public TableRegistry getTableRegistry() {
		return tableRegistry;
	}

	@Override
	public MediatorRegistryImpl getMediatorRegistry() {
		return mediatorRegistry;
	}

	@Override
	public QueryContextFactory getQueryContextFactory() {
		return queryContextFactory;
	}

	@Override
	public ConstraintRegistry getConstraintRegistry() {
		return constraintRegistry;
	}

	@Override
	public VirtualAttributeAdapterRegistry getVirtualAttributeAdapterRegistry() {
		return virtualAttributeAdapterRegistry;
	}

	public void setDeployer(Deployer deployer) {
		this.deployer = deployer;
	}

	public void setAccessor(AccessorImpl accessor) {
		this.accessor = accessor;
	}

	public void setQueryRunner(TransactionFactory queryRunner) {
		this.queryRunner = queryRunner;
	}

	public void setTableRegistry(TableRegistry tableRegistry) {
		this.tableRegistry = tableRegistry;
	}

	public void setQueryContextFactory(QueryContextFactory queryContextFactory) {
		this.queryContextFactory = queryContextFactory;
	}

	public void setConstraintRegistry(ConstraintRegistry constraintRegistry) {
		this.constraintRegistry = constraintRegistry;
	}

	public void setVirtualAttributeAdapterRegistry(VirtualAttributeAdapterRegistry virtualAttributeAdapterRegistry) {
		this.virtualAttributeAdapterRegistry = virtualAttributeAdapterRegistry;
	}

	public void setMediatorRegistry(MediatorRegistryImpl mediatorRegistry) {
		this.mediatorRegistry = mediatorRegistry;
	}

	public void setVendorConfiguration(VendorConfiguration vendorConfiguration) {
		this.vendorConfiguration = vendorConfiguration;
	}

}
