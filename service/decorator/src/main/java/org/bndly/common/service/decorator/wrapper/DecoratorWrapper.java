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

import org.bndly.common.service.decorator.api.ServiceDecorator;
import org.bndly.common.service.decorator.api.ServiceDecoratorChain;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;

public class DecoratorWrapper implements InvocationHandler {

	private final Map<Method, MethodInvoker> methodInvokers = new HashMap<>();
	private final ReadWriteLock decoratorsLock;
	private final List<ServiceDecorator> decorators;
	private final Object wrapped;


	private interface MethodInvoker {
		Object doInvoke(final Object o, final Object[] os) throws Throwable;
	}
	
	public DecoratorWrapper(Object wrapped, ReadWriteLock decoratorsLock, List<ServiceDecorator> decorators) {
		this.decoratorsLock = decoratorsLock;
		this.decorators = decorators;
		this.wrapped = wrapped;
	}

	public void reset() {
		// drop all method invokers for whatever reason. upon next method invocation the invokers will be re-created.
		methodInvokers.clear();
	}
	
	@Override
	public Object invoke(final Object o, Method m, final Object[] os) throws Throwable {
		MethodInvoker invoker = methodInvokers.get(m);
		if (invoker == null) {
			invoker = buildInvoker(m);
			methodInvokers.put(m, invoker);
		}
		return invoker.doInvoke(o, os);
	}
	
	private MethodInvoker buildInvoker(final Method methodToInvoke) {
		final Map<Method, List<ServiceDecorator>> decoratorsMappedByMethod = new HashMap<>();

		// find the decorators to apply onto the method only when the inoker is created.
		// this should not be re-evaluated per method invocation due to performance.
		List<ServiceDecorator> decoratorsToApply = decoratorsMappedByMethod.get(methodToInvoke);
		if (decoratorsToApply == null) {
			// add the decorators in correct order
			decoratorsToApply = buildOrderedDecoratorsListForMethod(methodToInvoke);
			decoratorsMappedByMethod.put(methodToInvoke, decoratorsToApply);
		}
		final List<ServiceDecorator> decoratorsToApplyFinal = decoratorsToApply;
		
		return new MethodInvoker() {
			
			@Override
			public Object doInvoke(Object o, final Object[] os) throws Throwable {
				// build a decoratorChain
				final Iterator<ServiceDecorator> iterator = decoratorsToApplyFinal.iterator();
				ServiceDecoratorChain chain = new ServiceDecoratorChain() {
					@Override
					public Object doContinue() throws Throwable {
						if (iterator.hasNext()) {
							ServiceDecorator currentDecorator = iterator.next();
							return currentDecorator.execute(this, wrapped, methodToInvoke, os);
						} else {
							throw new IllegalStateException("no more decorators in the chain. invocation of doContinue was not allowed.");
						}
					}
				};
				// trigger the chain
				Object result = chain.doContinue();
				// return chain result;
				return result;
			}
		};
	}

	private List<ServiceDecorator> buildOrderedDecoratorsListForMethod(Method method) {
		if (decorators != null) {
			List<ServiceDecorator> l = new ArrayList<>();
			decoratorsLock.readLock().lock();
			try {
				for (ServiceDecorator serviceDecorator : decorators) {
					if (serviceDecorator.appliesTo(method, wrapped)) {
						int i = 0;
						for (int j = 0; j < l.size(); j++) {
							ServiceDecorator dec = l.get(j);
							if (!dec.precedes(serviceDecorator)) {
								break;
							} else {
								i++;
							}
						}
						l.add(i, serviceDecorator);
					}
				}
			} finally {
				decoratorsLock.readLock().unlock();
			}
			return l;
		} else {
			return Collections.EMPTY_LIST;
		}
	}
}
