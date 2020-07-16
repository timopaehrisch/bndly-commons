package org.bndly.rest.repository.resources.beans.api;

/*-
 * #%L
 * REST Repository Resource
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

import org.bndly.common.velocity.api.ContextData;
import org.bndly.common.velocity.api.Renderer;
import org.bndly.common.velocity.api.RenderingListener;
import org.bndly.common.velocity.api.VelocityTemplate;
import org.bndly.rest.api.Context;
import org.bndly.rest.api.Resource;
import org.bndly.rest.api.ResourceInterceptor;
import org.bndly.rest.repository.resources.beans.initializer.Initializeable;
import org.bndly.rest.repository.resources.beans.initializer.Initializer;
import org.bndly.rest.repository.resources.beans.initializer.BeanPojoFactoryAware;
import org.bndly.schema.api.repository.beans.Bean;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = { BeanPojoRegistry.class, BeanPojoFactory.class, ResourceInterceptor.class }, immediate = true)
public class BeanPojoRegistry implements BeanPojoFactory, Initializer, ResourceInterceptor, RenderingListener {

	private final ThreadLocal<BeanPojoFactory> localBeanPojoFactory = new ThreadLocal<>();

	@Reference
	private Renderer renderer;

	@Override
	public boolean canInitialize(Class beanType) {
		return BeanPojoFactoryAware.class.isAssignableFrom(beanType);
	}

	@Override
	public void initialize(Object bean) {
		if (BeanPojoFactoryAware.class.isInstance(bean)) {
			BeanPojoFactory beanPojoFactory = localBeanPojoFactory.get();
			if (beanPojoFactory == null) {
				beanPojoFactory = createCachingBeanPojoFactory();
				((BeanPojoFactoryAware) bean).setBeanPojoFactory(beanPojoFactory);
			} else {
				((BeanPojoFactoryAware) bean).setBeanPojoFactory(beanPojoFactory);
			}
		}
	}

	@Override
	public void beforeRendering(VelocityTemplate velocityTemplate, Writer writer) {
		BeanPojoFactory beanPojoFactory = localBeanPojoFactory.get();
		if (beanPojoFactory != null) {
			velocityTemplate
					.addContextData(ContextData.newInstance("beanRegistry", beanPojoFactory))
			;
		}
	}

	@Override
	public void afterRendering(VelocityTemplate velocityTemplate, Writer writer) {
	}

	public static interface Factory {

		Bean newInstance(Bean original);
	}

	private final Map<String, Factory> factories = new HashMap<>();
	private final List<Initializer> initializers = new ArrayList<>();
	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	
	@Activate
	public void activate() {
		registerInitializer(this);
		renderer.addRenderingListener(this);
	}
	@Deactivate
	public void deactivate() {
		renderer.removeRenderingListener(this);
		unregisterInitializer(this);
	}
	
	@Reference(
			bind = "addBeanFactoryRegistrar",
			unbind = "removeBeanFactoryRegistrar",
			cardinality = ReferenceCardinality.MULTIPLE,
			policy = ReferencePolicy.DYNAMIC,
			service = BeanFactoryRegistrar.class
	)
	public void addBeanFactoryRegistrar(BeanFactoryRegistrar beanFactoryRegistrar) {
		if (beanFactoryRegistrar != null) {
			lock.writeLock().lock();
			try {
				beanFactoryRegistrar.register(this);
			} finally {
				lock.writeLock().unlock();
			}
		}
	}

	public void removeBeanFactoryRegistrar(BeanFactoryRegistrar beanFactoryRegistrar) {
		if (beanFactoryRegistrar != null) {
			lock.writeLock().lock();
			try {
				beanFactoryRegistrar.unregister(this);
			} finally {
				lock.writeLock().unlock();
			}
		}
	}
	
	public void register(String beanType, Factory factory) {
		if (beanType != null && factory != null) {
			lock.writeLock().lock();
			try {
				factories.put(beanType, factory);
			} finally {
				lock.writeLock().unlock();
			}
		}
	}
	
	public void unregister(String beanType, Factory factory) {
		if (beanType != null && factory != null) {
			lock.writeLock().lock();
			try {
				Factory r = factories.get(beanType);
				if (r == factory) {
					factories.remove(beanType);
				}
			} finally {
				lock.writeLock().unlock();
			}
		}
	}
	
	@Reference(
			bind = "registerInitializer",
			unbind = "unregisterInitializer",
			cardinality = ReferenceCardinality.MULTIPLE,
			policy = ReferencePolicy.DYNAMIC,
			service = Initializer.class
	)
	public void registerInitializer(Initializer initializer) {
		if (initializer != null) {
			lock.writeLock().lock();
			try {
				initializers.add(initializer);
			} finally {
				lock.writeLock().unlock();
			}
		}
	}
	
	public void unregisterInitializer(Initializer initializer) {
		if (initializer != null) {
			lock.writeLock().lock();
			try {
				Iterator<Initializer> iterator = initializers.iterator();
				while (iterator.hasNext()) {
					if (iterator.next() == initializer) {
						iterator.remove();
					}
				}
			} finally {
				lock.writeLock().unlock();
			}
		}
	}
	
	@Override
	public Bean getBean(Bean original) {
		return createCachingBeanPojoFactory().getBean(original);
	}
	
	@Override
	public <E extends Bean> E getBean(Bean original, Class<E> requiredType) {
		return createCachingBeanPojoFactory().getBean(original, requiredType);
	}
	
	public BeanPojoFactory createCachingBeanPojoFactory() {
		return new BeanPojoFactory() {

			private final Map<String, Bean> cache = new HashMap<>();

			@Override
			public Bean getBean(Bean original) {
				return getBean(original, Bean.class);
			}

			@Override
			public <E extends Bean> E getBean(Bean original, Class<E> requiredType) {
				if (original == null) {
					return null;
				}
				Bean res = cache.get(original.getPath());
				if (res != null) {
					if (requiredType.isInstance(res)) {
						return requiredType.cast(res);
					}
					return null;
				}
				lock.readLock().lock();
				try {
					Factory factory = factories.get(original.getBeanType());
					if (factory == null) {
						if (requiredType.isInstance(original)) {
							return requiredType.cast(original);
						}
						return null;
					}
					res = factory.newInstance(original);
					if (!requiredType.isInstance(res)) {
						return null;
					}
					cache.put(res.getPath(), res);
					Class<? extends Bean> beanType = res.getClass();
					for (Initializer initializer : initializers) {
						if (initializer.canInitialize(beanType)) {
							initializer.initialize(res);
						}
					}
					if (Initializeable.class.isInstance(res)) {
						((Initializeable) res).init();
					}
					return requiredType.cast(res);
				} finally {
					lock.readLock().unlock();
				}
			}
		};
	}
	
	@Override
	public void beforeResourceResolving(Context context) {
		localBeanPojoFactory.set(createCachingBeanPojoFactory());
	}

	@Override
	public Resource intercept(Resource input) {
		return input;
	}

	@Override
	public void doFinally(Context context) {
		localBeanPojoFactory.remove();
	}
	
}
