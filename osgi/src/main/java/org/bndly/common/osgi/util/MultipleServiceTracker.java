package org.bndly.common.osgi.util;

/*-
 * #%L
 * OSGI Utilities
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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class MultipleServiceTracker extends ServiceTracker {

	private final CompiledDependency[] dependencies;
	private final Map<CompiledDependency, Object> serviceByDependency = new HashMap<>();
	private final ReadWriteLock serviceByDependencyLock = new ReentrantReadWriteLock();
	private boolean ready;
	private final Callback callback;
	private final Wiring wiring;

	
	private MultipleServiceTracker(BundleContext context, Callback callback, Filter filter, CompiledDependency... dependencies) {
		super(context, filter, null);
		this.dependencies = dependencies;
		this.callback = callback;
		this.ready = false;
		wiring = new Wiring() {
			@Override
			public void wire(Object target) {
				for (Map.Entry<CompiledDependency, Object> entry : serviceByDependency.entrySet()) {
					CompiledDependency key = entry.getKey();
					Object value = entry.getValue();
					Dependency dep = key.getDependency();
					if (dep instanceof WiringDependency) {
						((WiringDependency) dep).wire(target, value);
					}
				}
			}
		};
	}

	@Override
	public void close() {
		serviceByDependencyLock.writeLock().lock();
		try {
			fireDestroy();
		} finally {
			serviceByDependencyLock.writeLock().unlock();
		}
		super.close();
	}
	
	public static MultipleServiceTracker newInstance(BundleContext bundleContext, Callback callback, Dependency... dependencies) throws InvalidSyntaxException {
		CompiledDependency[] compiledDependencies = new CompiledDependency[dependencies.length];
		StringBuilder sb = new StringBuilder();
		sb.append("(|");
		int i = 0;
		for (final Dependency dependency : dependencies) {
			String depLdap = dependency.toLdapFilter();
			final Filter depFilter = bundleContext.createFilter(depLdap);
			compiledDependencies[i] = new CompiledDependency() {
				@Override
				public Dependency getDependency() {
					return dependency;
				}

				@Override
				public Filter getFilter() {
					return depFilter;
				}
			};
			sb.append(depLdap);
			i++;
		}
		sb.append(")");
		return new MultipleServiceTracker(bundleContext, callback, bundleContext.createFilter(sb.toString()), compiledDependencies);
	}

	@Override
	public Object addingService(ServiceReference reference) {
		serviceByDependencyLock.writeLock().lock();
		try {
			Object service = super.addingService(reference);
			for (CompiledDependency dependency : dependencies) {
				if (dependency.getFilter().match(reference)) {
					serviceByDependency.put(dependency, service);
					fireReady();
					return service;
				}
			}
			return service;
		} finally {
			serviceByDependencyLock.writeLock().unlock();
		}
	}

	@Override
	public void removedService(ServiceReference reference, Object service) {
		serviceByDependencyLock.writeLock().lock();
		try {
			for (CompiledDependency dependency : dependencies) {
				if (dependency.getFilter().match(reference)) {
					Object trackedItem = serviceByDependency.get(dependency);
					if (trackedItem == service) {
						fireDestroy();
						serviceByDependency.remove(dependency);
					}
				}
			}
		} finally {
			serviceByDependencyLock.writeLock().unlock();
		}
	}

	private void fireDestroy() {
		if (serviceByDependency.size() != dependencies.length) {
			return;
		}
		if (ready) {
			ready = false;
			try {
				callback.onDestroy(context);
			} catch (Exception e) {
				throw new RuntimeException("failed to fire destroy event", e);
			}
		}
	}

	private void fireReady() {
		if (serviceByDependency.size() != dependencies.length) {
			return;
		}
		if (!ready) {
			ready = true;
			try {
				callback.onReady(context, wiring);
			} catch (Exception e) {
				throw new RuntimeException("failed to fire ready event", e);
			}
		}
	}

	private static interface CompiledDependency {
		Dependency getDependency();
		Filter getFilter();
	}
	
	public static interface Dependency {
		String toLdapFilter();
	}
	
	public static interface Setter<T,V> {
		void set(T target, V value);
	}
	
	public static interface WiringDependency<V> extends Dependency {
		void wire(Object target, V trackedService);
	}
	
	public static interface TypedDependency<V> extends Dependency {
		<T> WiringDependency<V> wire(Setter<T,V> setter);
	}
	
	public static <V> TypedDependency<V> serviceInterfaceDependency(final Class<V> serviceInterface) {
		return serviceInterfaceDependency(serviceInterface, null);
	}
	
	public static <V> TypedDependency<V> serviceInterfaceDependency(final Class<V> serviceInterface, final String furtherFilter) {
		return new TypedDependency<V>() {
			@Override
			public String toLdapFilter() {
				if (furtherFilter == null) {
					return "(" + Constants.OBJECTCLASS + "=" + serviceInterface.getName() + ")";
				} else {
					return "(&(" + Constants.OBJECTCLASS + "=" + serviceInterface.getName() + ")" + furtherFilter + ")";
				}
			}

			@Override
			public <T> WiringDependency wire(final Setter<T, V> setter) {
				final TypedDependency<V> that = this;
				return new WiringDependency<V>() {
					@Override
					public String toLdapFilter() {
						return that.toLdapFilter();
					}

					@Override
					public void wire(Object target, V trackedService) {
						setter.set((T) target, trackedService);
					}
					
				};
			}
			
		};
	}
	
	public static interface Wiring {
		void wire(Object target);
	}
	
	public static interface Callback {
		void onReady(BundleContext context, Wiring wiring) throws Exception;
		void onDestroy(BundleContext context) throws Exception;
	}
	
}
