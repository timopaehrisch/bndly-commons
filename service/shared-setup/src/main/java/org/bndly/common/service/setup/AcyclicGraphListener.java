package org.bndly.common.service.setup;

/*-
 * #%L
 * Service Shared Client Setup
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

import org.bndly.common.graph.NoOpGraphListener;
import org.bndly.common.service.model.api.ReferableResource;
import org.bndly.common.service.model.api.ReferenceBuildingException;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Stack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class AcyclicGraphListener extends NoOpGraphListener<AcyclicGraphListener.Context> {

	private static final Logger LOG = LoggerFactory.getLogger(AcyclicGraphListener.class);

	public static final class Context {

		private final Stack<Object> stack = new Stack<>();
	}

	@Override
	public Class<Context> getIterationContextType() {
		return Context.class;
	}

	@Override
	public void onStart(Object bean, Context context) {
		context.stack.push(bean);
	}

	@Override
	public void onEnd(Object bean, Context context) {
		context.stack.pop();
	}

	@Override
	public void beforeVisitReference(Object value, Field field, Object bean, Context context) {
		if (value == null) {
			return;
		}
		if (context.stack.contains(value)) {
			injectReference(value, field, bean);
		}
		context.stack.push(value);
	}

	@Override
	public void afterVisitReference(Object value, Field field, Object bean, Context context) {
		if (value == null) {
			return;
		}
		context.stack.pop();
	}

	@Override
	public void beforeVisitReferenceInCollection(Object object, Collection c, Field field, Object bean, Context context) {
		if (object == null) {
			return;
		}
		if (context.stack.contains(object)) {
			injectReferenceToCollection(object, c);
		}
		context.stack.push(object);
	}

	@Override
	public void afterVisitReferenceInCollection(Object object, Collection c, Field field, Object bean, Context context) {
		if (object == null) {
			return;
		}
		context.stack.pop();
	}
	
	private void injectReference(Object value, Field field, Object bean) {
		try {
			Object ref = ((ReferableResource) value).buildReference();
			boolean ia = field.isAccessible();
			try {
				field.setAccessible(true);
				field.set(bean, ref);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				LOG.warn("could not inject reference, e");
			} finally {
				field.setAccessible(ia);
			}
		} catch (ReferenceBuildingException e) {
			LOG.warn("could not build reference", e);
		}
	}
	
	private void injectReferenceToCollection(Object object, Collection c) {
		try {
			Object ref = ((ReferableResource) object).buildReference();
			c.remove(object);
			c.add(ref);
		} catch (ReferenceBuildingException e) {
			LOG.warn("could not build reference", e);
		}
	}
}
