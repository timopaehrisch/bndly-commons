package org.bndly.schema.impl.factory;

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

import org.bndly.schema.api.SchemaBeanProvider;
import org.bndly.schema.api.services.Engine;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public abstract class SchemaBeanServicesListener {
	private static final Logger LOG = LoggerFactory.getLogger(SchemaBeanServicesListener.class);
	
	protected final BundleContext bundleContext;
	private final String schemaName;
	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	
	private ServiceTracker<Engine, Engine> engineTracker;
	private ServiceTracker<SchemaBeanProvider, SchemaBeanProvider> schemaBeanProviderTracker;
	
	private Engine engine;
	private SchemaBeanProvider schemaBeanProvider;
	private boolean ready;
	private boolean shutdown;

	public SchemaBeanServicesListener(BundleContext bundleContext, String schemaName) {
		this.bundleContext = bundleContext;
		this.schemaName = schemaName;
	}
	
	public void init() throws InvalidSyntaxException {
		// look for an instance of Engine
		String engineFilterString = "(&(objectClass=" + Engine.class.getName() + ")(" + Constants.SERVICE_PID + "=" + Engine.class.getName() + "." + schemaName + "))";
		engineTracker = new ServiceTracker<Engine, Engine>(bundleContext, bundleContext.createFilter(engineFilterString), new ServiceTrackerCustomizer<Engine, Engine>() {
			@Override
			public Engine addingService(ServiceReference<Engine> sr) {
				lock.writeLock().lock();
				try {
					engine = bundleContext.getService(sr);
					if (engine != null && schemaBeanProvider != null) {
						onReadyInternal(engine, schemaBeanProvider);
					}
					return engine;
				} finally {
					lock.writeLock().unlock();
				}
			}

			@Override
			public void modifiedService(ServiceReference<Engine> sr, Engine t) {
			}

			@Override
			public void removedService(ServiceReference<Engine> sr, Engine t) {
				lock.writeLock().lock();
				try {
					engine = null;
					onShutdownInternal(engine, schemaBeanProvider);
				} finally {
					lock.writeLock().unlock();
				}
			}
			
		});
		engineTracker.open();
		
		// look for an instance of SchemaBeanProvider
		String schemaBeanProviderFilterString = "(&(objectClass=" + SchemaBeanProvider.class.getName() + ")(" + Constants.SERVICE_PID + "=" + SchemaBeanProvider.class.getName() + "." + schemaName + "))";
		schemaBeanProviderTracker = new ServiceTracker<SchemaBeanProvider, SchemaBeanProvider>(bundleContext, bundleContext.createFilter(schemaBeanProviderFilterString), new ServiceTrackerCustomizer<SchemaBeanProvider, SchemaBeanProvider>() {
			@Override
			public SchemaBeanProvider addingService(ServiceReference<SchemaBeanProvider> sr) {
				lock.writeLock().lock();
				try {
					schemaBeanProvider = bundleContext.getService(sr);
					if (engine != null && schemaBeanProvider != null) {
						onReadyInternal(engine, schemaBeanProvider);
					}
					return schemaBeanProvider;
				} finally {
					lock.writeLock().unlock();
				}
			}

			@Override
			public void modifiedService(ServiceReference<SchemaBeanProvider> sr, SchemaBeanProvider t) {
			}

			@Override
			public void removedService(ServiceReference<SchemaBeanProvider> sr, SchemaBeanProvider t) {
				lock.writeLock().lock();
				try {
					schemaBeanProvider = null;
					onShutdownInternal(engine, schemaBeanProvider);
				} finally {
					lock.writeLock().unlock();
				}
			}
		});
		schemaBeanProviderTracker.open();
	}
	public void destroy() {
		
	}
	
	private void onReadyInternal(Engine engine, SchemaBeanProvider schemaBeanProvider) {
		if (!ready) {
			ready = true;
			shutdown = false;
			LOG.info("schema bean factory for schema {} should become ready now", schemaName);
			onReady(engine, schemaBeanProvider);
		}
	}
	
	private void onShutdownInternal(Engine engine, SchemaBeanProvider schemaBeanProvider) {
		if (!shutdown && ready) {
			shutdown = true;
			ready = false;
			LOG.info("schema bean factory for schema {} should shut down now", schemaName);
			onShutdown(engine, schemaBeanProvider);
		}
	}
	protected abstract void onReady(Engine engine, SchemaBeanProvider schemaBeanProvider);
	protected abstract void onShutdown(Engine engine, SchemaBeanProvider schemaBeanProvider);
}
