package org.bndly.common.service.decorator.api;

/*-
 * #%L
 * Service Decorator API
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

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public interface DecoratedProxyFactory {
	void registerDecorator(ServiceDecorator serviceDecorator);
	void unregisterDecorator(ServiceDecorator serviceDecorator);
	
	/**
	 * Creates a decorated service instance, while all available decorators at invocation time are being compiled into the decorated instance. Subsequent unregistered or registered decorators will no
	 * appear in the decorated service instance. This method is a convenience method for {@link #decorateService(java.lang.ClassLoader, java.lang.Object, java.lang.Class)}.
	 * @param <E> Any java interface
	 * @param serviceInstance The actual implementation of the service to decorate
	 * @param serviceInterface The interface of the service implementation to use for the decorating wrapper
	 * @return a decorated instance of the provided serviceInstance, with the decorators, that where available at the time of invocation.
	 */
	<E> E decorateService(E serviceInstance, Class<E> serviceInterface);
	/**
	 * Creates a decorated service instance, while all available decorators at invocation time are being compiled into the decorated instance. Subsequent unregistered or registered decorators will no
	 * appear in the decorated service instance.
	 * @param <E> Any java interface
	 * @param customClassLoader The classloader to use for the decorating wrapper
	 * @param serviceInstance The actual implementation of the service to decorate
	 * @param serviceInterface The interface of the service implementation to use for the decorating wrapper
	 * @return a decorated instance of the provided serviceInstance, with the decorators, that where available at the time of invocation.
	 */
	<E> E decorateService(ClassLoader customClassLoader, E serviceInstance, Class<E> serviceInterface);
	
	/**
	 * Creates a decorated service instance, while all available decorators at invocation time are being compiled into the decorated instance. Subsequent unregistered or registered decorators will
	 * appear in the decorated service instance. This means, that the {@link DecoratedProxyFactory} keeps track of all created decorating wrappers. To avoid memory leaks, unused decorating wrappers
	 * should be untracked by calling {@link #untrackService(java.lang.Object)}. This method is a convenience method for
	 * {@link #decorateServiceWithDecoratorTracking(java.lang.ClassLoader, java.lang.Object, java.lang.Class) }.
	 *
	 * @param <E> Any java interface
	 * @param serviceInstance The actual implementation of the service to decorate
	 * @param serviceInterface The interface of the service implementation to use for the decorating wrapper
	 * @return a decorated instance of the provided serviceInstance, with the decorators, that where available at the time of invocation.
	 */
	<E> E decorateServiceWithDecoratorTracking(E serviceInstance, Class<E> serviceInterface);
	/**
	 * Creates a decorated service instance, while all available decorators at invocation time are being compiled into the decorated instance. Subsequent unregistered or registered decorators will
	 * appear in the decorated service instance. This means, that the {@link DecoratedProxyFactory} keeps track of all created decorating wrappers. To avoid memory leaks, unused decorating wrappers
	 * should be untracked by calling {@link #untrackService(java.lang.Object)}. This method is a convenience method for
	 * {@link #decorateServiceWithDecoratorTracking(java.lang.ClassLoader, java.lang.Object, java.lang.Class) }.
	 *
	 * @param <E> Any java interface
	 * @param customClassLoader The classloader to use for the decorating wrapper
	 * @param serviceInstance The actual implementation of the service to decorate
	 * @param serviceInterface The interface of the service implementation to use for the decorating wrapper
	 * @return a decorated instance of the provided serviceInstance, with the decorators, that where available at the time of invocation.
	 */
	<E> E decorateServiceWithDecoratorTracking(ClassLoader customClassLoader, E serviceInstance, Class<E> serviceInterface);
	
	/**
	 * Removes a decorating wrapper from the tracking mechanism. This means, that the provided decoratedServiceInstance will not received any further decorator add- or remove-events.
	 * @param decoratedServiceInstance The decorated service instance, that shall no longer receive change events of new or removed decorators
	 */
	void untrackService(Object decoratedServiceInstance);
	
	void addServiceProxyFactoryListener(ServiceProxyFactoryListener serviceProxyFactoryListener);
	void removeServiceProxyFactoryListener(ServiceProxyFactoryListener serviceProxyFactoryListener);
}
