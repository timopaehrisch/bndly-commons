package org.bndly.common.osgi.config.impl;

/*-
 * #%L
 * OSGI Config
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

import org.bndly.common.osgi.config.spi.PrefixHandler;
import org.bndly.common.osgi.util.ServiceRegistrationBuilder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class PrefixHandlerTracker {

	private final Set<String> requiredHandlerNames = new HashSet<>();
	private final Map<String, PrefixHandler> prefixHandlersByName = new HashMap<>();
	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	private final PrefixHandlerProvider prefixHandlerProvider = new PrefixHandlerProvider() {
		@Override
		public PrefixHandler get(String name) {
			lock.readLock().lock();
			try {
				return prefixHandlersByName.get(name);
			} finally {
				lock.readLock().unlock();
			}
		}
	};
	private ServiceRegistration<PrefixHandlerProvider> reg;
	private ServiceTracker<PrefixHandler, PrefixHandler> tracker;
	
//	@Activate
	public void activate(BundleContext context) {
		String requiredHandlerNamesProp = System.getProperty("org.bndly.common.osgi.config.impl.PrefixHandlerTracker.requiredHandlerNames");
		if (requiredHandlerNamesProp != null) {
			for (String handlerName : requiredHandlerNamesProp.split(",")) {
				requiredHandlerNames.add(handlerName.trim());
			}
		}
		
		tracker = new ServiceTracker<PrefixHandler, PrefixHandler>(context, PrefixHandler.class, null) {
			@Override
			public PrefixHandler addingService(ServiceReference<PrefixHandler> reference) {
				PrefixHandler handler = super.addingService(reference);
				lock.writeLock().lock();
				try {
					String prefix = handler.getPrefix();
					if (prefix != null) {
						prefixHandlersByName.put(prefix, handler);
					}
					updateProviderRegistration(context);
				} finally {
					lock.writeLock().unlock();
				}
				return handler;
			}

			@Override
			public void removedService(ServiceReference<PrefixHandler> reference, PrefixHandler handler) {
				lock.writeLock().lock();
				try {
					String prefix = handler.getPrefix();
					if (prefix != null) {
						PrefixHandler removed = prefixHandlersByName.remove(prefix);
						if (removed != handler) {
							prefixHandlersByName.put(prefix, removed);
						}
					}
					updateProviderRegistration(context);
				} finally {
					lock.writeLock().unlock();
				}
				super.removedService(reference, handler);
			}
		};
		tracker.open();
	}
	
//	@Deactivate
	public void deactivate(BundleContext context) {
		tracker.close();
	}
	
	private void updateProviderRegistration(BundleContext context) {
		boolean allHandlersAvailable = true;
		for (String requiredHandlerName : requiredHandlerNames) {
			if (!prefixHandlersByName.containsKey(requiredHandlerName)) {
				allHandlersAvailable = false;
				break;
			}
		}
		if (!allHandlersAvailable) {
			// remove registration if available
			if (reg != null) {
				reg.unregister();
				reg = null;
			}
		} else {
			// add registration if not available
			if (reg == null) {
				reg = ServiceRegistrationBuilder
						.newInstance(PrefixHandlerProvider.class, prefixHandlerProvider)
						.pid(PrefixHandlerProvider.class.getName())
						.register(context);
			}
		}
	}
	
}
