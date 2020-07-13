package org.bndly.common.graph;

/*-
 * #%L
 * Graph Impl
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

import org.bndly.common.graph.DelegatingBeanGraphIteratorListener.DelegatingGraphListenerContext;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DelegatingBeanGraphIteratorListener implements BeanGraphIteratorListener<DelegatingGraphListenerContext> {

	private List<BeanGraphIteratorListener> listeners = new ArrayList<>();

	@Override
	public Class<DelegatingGraphListenerContext> getIterationContextType() {
		return DelegatingGraphListenerContext.class;
	}

	@Override
	public void onStart(Object bean, DelegatingGraphListenerContext context) {
		for (BeanGraphIteratorListener listener : listeners) {
			if (context.getContext(listener) == null) {
				context.createContext(listener);
			}
			listener.onStart(bean, context.getContext(listener));
		}
	}

	@Override
	public void onVisitReference(Object bean, DelegatingGraphListenerContext context) {
		for (BeanGraphIteratorListener listener : listeners) {
			listener.onVisitReference(bean, context.getContext(listener));
		}
	}

	@Override
	public void beforeVisitReference(Object value, Field field, Object bean, DelegatingGraphListenerContext context) {
		for (BeanGraphIteratorListener listener : listeners) {
			listener.beforeVisitReference(value, field, bean, context.getContext(listener));
		}
	}

	@Override
	public void beforeVisitReferenceCollection(Object value, Field field, Object bean, DelegatingGraphListenerContext context) {
		for (BeanGraphIteratorListener listener : listeners) {
			listener.beforeVisitReferenceCollection(value, field, bean, context.getContext(listener));
		}
	}

	@Override
	public void beforeVisitReferenceInCollection(Object object, Collection c, Field field, Object bean, DelegatingGraphListenerContext context) {
		for (BeanGraphIteratorListener listener : listeners) {
			listener.beforeVisitReferenceInCollection(object, c, field, bean, context.getContext(listener));
		}
	}

	@Override
	public void beforeVisitCollection(Object value, Field field, Object bean, DelegatingGraphListenerContext context) {
		for (BeanGraphIteratorListener listener : listeners) {
			listener.beforeVisitCollection(value, field, bean, context.getContext(listener));
		}
	}

	@Override
	public void beforeVisitValueInCollection(Object object, Collection c, Field field, Object bean, DelegatingGraphListenerContext context) {
		for (BeanGraphIteratorListener listener : listeners) {
			listener.beforeVisitValueInCollection(object, c, field, bean, context.getContext(listener));
		}
	}

	@Override
	public void onVisitValue(Object value, Field field, Object bean, DelegatingGraphListenerContext context) {
		for (BeanGraphIteratorListener listener : listeners) {
			listener.onVisitValue(value, field, bean, context.getContext(listener));
		}
	}

	@Override
	public void onRevisitReference(Object bean, Field field, Object owner, DelegatingGraphListenerContext context) {
		for (BeanGraphIteratorListener listener : listeners) {
			listener.onRevisitReference(bean, field, owner, context.getContext(listener));
		}
	}

	@Override
	public void onRevisitReferenceInCollection(Object bean, Collection c, DelegatingGraphListenerContext context) {
		for (BeanGraphIteratorListener listener : listeners) {
			listener.onRevisitReferenceInCollection(bean, c, context.getContext(listener));
		}
	}

	@Override
	public void onEnd(Object bean, DelegatingGraphListenerContext context) {
		for (BeanGraphIteratorListener listener : listeners) {
			listener.onEnd(bean, context.getContext(listener));
		}
	}

	public void addListener(BeanGraphIteratorListener listener) {
		listeners.add(listener);
	}

	public void setListeners(List<BeanGraphIteratorListener> listeners) {
		this.listeners = listeners;
	}

	@Override
	public void afterVisitReference(Object value, Field field, Object bean, DelegatingGraphListenerContext context) {
		for (BeanGraphIteratorListener listener : listeners) {
			listener.afterVisitReference(value, field, bean, context.getContext(listener));
		}
	}

	@Override
	public void afterVisitReferenceCollection(Object value, Field field, Object bean, DelegatingGraphListenerContext context) {
		for (BeanGraphIteratorListener listener : listeners) {
			listener.afterVisitReferenceCollection(value, field, bean, context.getContext(listener));
		}
	}

	@Override
	public void afterVisitReferenceInCollection(Object object, Collection c, Field field, Object bean, DelegatingGraphListenerContext context) {
		for (BeanGraphIteratorListener listener : listeners) {
			listener.afterVisitReferenceInCollection(object, c, field, bean, context.getContext(listener));
		}
	}

	@Override
	public void afterVisitCollection(Object value, Field field, Object bean, DelegatingGraphListenerContext context) {
		for (BeanGraphIteratorListener listener : listeners) {
			listener.afterVisitCollection(value, field, bean, context.getContext(listener));
		}
	}

	@Override
	public void afterVisitValueInCollection(Object object, Collection c, Field field, Object bean, DelegatingGraphListenerContext context) {
		for (BeanGraphIteratorListener listener : listeners) {
			listener.afterVisitValueInCollection(object, c, field, bean, context.getContext(listener));
		}
	}

	public static class DelegatingGraphListenerContext {

		Map<Class<? extends BeanGraphIteratorListener>, Object> contexts = new HashMap<>();

		public void createContext(BeanGraphIteratorListener listener) {
			Class contextType = listener.getIterationContextType();
			if (contextType == null) {
				return;
			}
			Object ctx = contexts.get(contextType);
			if (ctx == null) {
				try {
					ctx = contextType.newInstance();
					contexts.put(contextType, ctx);
				} catch (Exception e) {
					throw new IllegalStateException("could not instantiate context type " + contextType.getSimpleName(), e);
				}
			}
		}

		public Object getContext(BeanGraphIteratorListener listener) {
			return getContext(listener.getIterationContextType());
		}

		public Object getContext(Class<? extends BeanGraphIteratorListener> listenerType) {
			return contexts.get(listenerType);
		}
	}
}
