package org.bndly.common.bpm.impl;

/*-
 * #%L
 * BPM Impl
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
import org.bndly.common.bpm.api.ProcessInstanceService;
import org.bndly.common.bpm.api.ProcessInvokerFactory;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = ProcessInvokerFactory.class)
public final class ProcessInvokerFactoryImpl implements ProcessInvokerFactory {
	private static final Logger LOG = LoggerFactory.getLogger(ProcessInvokerFactoryImpl.class);
	@Reference
	private ProcessInstanceServiceProvider processInstanceServiceProvider;
	@Reference
	private ContextResolver contextResolver;

	private final List<KnownProcessInvoker> invokersByEngineName = new ArrayList<>();
	
	private class KnownProcessInvoker {
		private final String engineName;
		private final ProcessInvoker processInvoker;

		public KnownProcessInvoker(String engineName, ProcessInvoker processInvoker) {
			this.engineName = engineName;
			this.processInvoker = processInvoker;
		}

		public String getEngineName() {
			return engineName;
		}

		public ProcessInvoker getProcessInvoker() {
			return processInvoker;
		}
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public <T> T create(Class<T> type, String engineName) {
		LOG.info("creating invoker for type {}", type.getName());
		Class<?>[] interfaces = new Class[]{type};
		ProcessInvoker invoker = new ProcessInvoker();
		invoker.setProcessInvokerFactoryImpl(this);
		invokersByEngineName.add(new KnownProcessInvoker(engineName, invoker));
		T instance = (T) Proxy.newProxyInstance(type.getClassLoader(), interfaces, invoker);
		LOG.info("created invoker for type {}", type.getName());
		return instance;
	}

	@Override
	public void destroy(Object processInvoker) {
		if (processInvoker == null) {
			LOG.warn("can not destroy null object");
			return;
		}
		if (!Proxy.isProxyClass(processInvoker.getClass())) {
			LOG.warn("can not destroy non-proxy object");
			return;
		}
		InvocationHandler invocationHandler = Proxy.getInvocationHandler(processInvoker);
		if (!KnownProcessInvoker.class.isInstance(invocationHandler)) {
			LOG.warn("unsupported invocation handler: {}", invocationHandler.getClass());
			return;
		}
		KnownProcessInvoker knownProcessInvoker = (KnownProcessInvoker) invocationHandler;
		Iterator<KnownProcessInvoker> iterator = invokersByEngineName.iterator();
		while (iterator.hasNext()) {
			if (iterator.next() == knownProcessInvoker) {
				iterator.remove();
			}
		}
	}

	public ContextResolver getContextResolver() {
		return contextResolver;
	}

	private KnownProcessInvoker getKnownProcessInvoker(ProcessInvoker invoker) {
		for (KnownProcessInvoker knownProcessInvoker : invokersByEngineName) {
			if (knownProcessInvoker.getProcessInvoker() == invoker) {
				return knownProcessInvoker;
			}
		}
		return null;
	}

	public ProcessInstanceService getProcessInstanceService(ProcessInvoker invoker) {
		KnownProcessInvoker knownProcessInvoker = getKnownProcessInvoker(invoker);
		if (knownProcessInvoker == null) {
			throw new IllegalStateException("could not find known process invoker for process invoker");
		}
		ProcessInstanceService instanceService = processInstanceServiceProvider.getInstanceServiceByEngineName(knownProcessInvoker.getEngineName());
		return instanceService;
	}

}
