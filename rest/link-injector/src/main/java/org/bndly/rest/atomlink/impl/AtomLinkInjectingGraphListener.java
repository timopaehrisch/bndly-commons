package org.bndly.rest.atomlink.impl;

/*-
 * #%L
 * REST Link Injector
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

import org.bndly.common.graph.BeanGraphIteratorListener;
import org.bndly.common.graph.NoOpGraphListener;
import org.bndly.rest.atomlink.api.AtomLinkInjector;
import org.bndly.rest.atomlink.impl.AtomLinkInjectingGraphListener.AtomLinkContext;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = BeanGraphIteratorListener.class)
public class AtomLinkInjectingGraphListener extends NoOpGraphListener<AtomLinkContext> {

	@Reference
	private AtomLinkInjector atomLinkInjector;

	public static class AtomLinkContext {

		private Object root;
		private final Set<Integer> handledBeans = new HashSet<>();
	}

	@Override
	public Class<AtomLinkContext> getIterationContextType() {
		return AtomLinkContext.class;
	}

	@Override
	public void onStart(Object bean, AtomLinkContext context) {
		context.root = bean;
		atomLinkInjector.addDiscovery(bean, true);
	}

	@Override
	public void onEnd(Object bean, AtomLinkContext context) {
		context.root = null;
	}

	@Override
	public void onVisitReference(Object bean, AtomLinkContext context) {
		if (context.root != bean) {
			if (bean != null) {
				int hc = bean.hashCode();
				if (context.handledBeans.contains(hc)) {
					return;
				}
				context.handledBeans.add(hc);
				atomLinkInjector.addDiscovery(bean, false);
			}
		}
	}

	@Override
	public void beforeVisitReferenceInCollection(Object object, Collection c, Field field, Object bean, AtomLinkContext context) {
		if (object != null) {
			int hc = object.hashCode();
			if (context.handledBeans.contains(hc)) {
				return;
			}
			context.handledBeans.add(hc);
			atomLinkInjector.addDiscovery(object, false);
		}
	}

	public void setAtomLinkInjector(AtomLinkInjector atomLinkInjector) {
		this.atomLinkInjector = atomLinkInjector;
	}

	public void setIteratorListeners(List<BeanGraphIteratorListener> l) {
		// this is a relict of the spring days. the setter was used to hook into a list of beans.
		l.add(this);
	}
}
