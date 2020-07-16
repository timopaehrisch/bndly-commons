package org.bndly.common.service.decorator.wrapper;

/*-
 * #%L
 * Service Decorator
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

import org.bndly.common.reflection.InstantiationUtil;
import org.bndly.common.reflection.InterfaceCollector;
import org.bndly.common.service.decorator.api.ServiceDecorator;
import org.bndly.common.service.decorator.api.DecoratedProxyFactory;
import org.bndly.common.service.decorator.api.ServiceProxyFactoryListener;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ServiceProxyFactoryImpl implements DecoratedProxyFactory {

	public static final String NAME = "serviceProxyFactory";
	private final ReadWriteLock decoratorsLock = new ReentrantReadWriteLock();
	private final List<ServiceDecorator> decorators = new ArrayList<>();
	private final ReadWriteLock listenersLock = new ReentrantReadWriteLock();
	private final List<ServiceProxyFactoryListener> listeners = new ArrayList<>();
	private final ReadWriteLock trackedDecoratorWrappersLock = new ReentrantReadWriteLock();
	private final List<DecoratorWrapper> trackedDecoratorWrappers = new ArrayList<>();

	@Override
	public <E> E decorateService(E serviceInstance, Class<E> serviceInterface) {
		return decorateService(this.getClass().getClassLoader(), serviceInstance, serviceInterface);
	}

	@Override
	public <E> E decorateService(ClassLoader customClassLoader, E serviceInstance, Class<E> serviceInterface) {
		return decorateServiceInternal(customClassLoader, serviceInstance, serviceInterface, false); // false to prevent decorator tracking
	}

	@Override
	public <E> E decorateServiceWithDecoratorTracking(E serviceInstance, Class<E> serviceInterface) {
		return decorateServiceWithDecoratorTracking(this.getClass().getClassLoader(), serviceInstance, serviceInterface);
	}

	@Override
	public <E> E decorateServiceWithDecoratorTracking(ClassLoader customClassLoader, E serviceInstance, Class<E> serviceInterface) {
		return decorateServiceInternal(customClassLoader, serviceInstance, serviceInterface, true); // true to enabled decorator tracking
	}

	private <E> E decorateServiceInternal(ClassLoader customClassLoader, E serviceInstance, Class<E> serviceInterface, final boolean trackDecorator) {
		if (trackDecorator) {
			trackedDecoratorWrappersLock.writeLock().lock();
		}
		DecoratorWrapper wrapper = createDecoratorWrapper(serviceInstance);
		E wrappedInstance = (E) Proxy.newProxyInstance(customClassLoader, new Class[]{serviceInterface}, wrapper);
		listenersLock.readLock().lock();
		try {
			for (ServiceProxyFactoryListener serviceProxyFactoryListener : listeners) {
				serviceProxyFactoryListener.createdProxy(serviceInterface, wrappedInstance, serviceInstance);
			}
			if (trackDecorator) {
				trackedDecoratorWrappers.add(wrapper);
			}
		} finally {
			listenersLock.readLock().unlock();
			if (trackDecorator) {
				trackedDecoratorWrappersLock.writeLock().unlock();
			}
		}
		return wrappedInstance;
	}

	@Override
	public void untrackService(Object decoratedServiceInstance) {
		if (decoratedServiceInstance == null) {
			return;
		}
		if (Proxy.isProxyClass(decoratedServiceInstance.getClass())) {
			InvocationHandler invocationHandler = Proxy.getInvocationHandler(decoratedServiceInstance);
			if (DecoratorWrapper.class.isInstance(invocationHandler)) {
				DecoratorWrapper decoratorWrapper = (DecoratorWrapper) invocationHandler;
				trackedDecoratorWrappersLock.writeLock().lock();
				try {
					Iterator<DecoratorWrapper> iterator = trackedDecoratorWrappers.iterator();
					while (iterator.hasNext()) {
						if (iterator.next() == decoratorWrapper) {
							iterator.remove();
						}
					}
				} finally {
					trackedDecoratorWrappersLock.writeLock().unlock();
				}
			}
		}
	}
	
	public Object createServiceInstance(String serviceImplClassName, String beanDefName) throws ClassNotFoundException {
		Class<?> serviceImplClass = getClass().getClassLoader().loadClass(serviceImplClassName);
		Object bean = InstantiationUtil.instantiateType(serviceImplClass);
		Object serviceInstance = bean;

		Class[] types = InterfaceCollector.collectInterfacesOfBeanAsArray(serviceInstance);
		if (types.length > 0) {
			DecoratorWrapper wrapper = createDecoratorWrapper(serviceInstance);
			serviceInstance = Proxy.newProxyInstance(getClass().getClassLoader(), types, wrapper);
		}
		
		return serviceInstance;
	}
	
	private DecoratorWrapper createDecoratorWrapper(Object serviceInstance) {
		return new DecoratorWrapper(serviceInstance, decoratorsLock, decorators);
	}
	
	private void resetTrackedDecoratorWrappers() {
		trackedDecoratorWrappersLock.writeLock().lock();
		try {
			for (DecoratorWrapper trackedDecoratorWrapper : trackedDecoratorWrappers) {
				trackedDecoratorWrapper.reset();
			}
		} finally {
			trackedDecoratorWrappersLock.writeLock().unlock();
		}
	}
	
	@Override
	public void registerDecorator(ServiceDecorator serviceDecorator) {
		if (serviceDecorator != null) {
			decoratorsLock.writeLock().lock();
			try {
				this.decorators.add(serviceDecorator);
			} finally {
				decoratorsLock.writeLock().unlock();
			}
			resetTrackedDecoratorWrappers();
		}
	}

	@Override
	public void unregisterDecorator(ServiceDecorator serviceDecorator) {
		if (serviceDecorator != null) {
			decoratorsLock.writeLock().lock();
			try {
				this.decorators.remove(serviceDecorator);
			} finally {
				decoratorsLock.writeLock().unlock();
			}
			resetTrackedDecoratorWrappers();
		}
	}

	@Override
	public void addServiceProxyFactoryListener(ServiceProxyFactoryListener serviceProxyFactoryListener) {
		if (serviceProxyFactoryListener != null) {
			listenersLock.writeLock().lock();
			try {
				listeners.add(serviceProxyFactoryListener);
			} finally {
				listenersLock.writeLock().unlock();
			}
		}
	}

	@Override
	public void removeServiceProxyFactoryListener(ServiceProxyFactoryListener serviceProxyFactoryListener) {
		if (serviceProxyFactoryListener != null) {
			listenersLock.writeLock().lock();
			try {
				listeners.remove(serviceProxyFactoryListener);
			} finally {
				listenersLock.writeLock().unlock();
			}
		}
	}

}
