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
public final class RewiringGraphListener extends NoOpGraphListener<RewiringGraphListener.Context> {

	private static final Logger LOG = LoggerFactory.getLogger(RewiringGraphListener.class);

	public static final class Context {

		private final Stack<Object> visitedObjectStack = new Stack<>();
	}

	@Override
	public Class<Context> getIterationContextType() {
		return Context.class;
	}

	@Override
	public void onStart(Object bean, Context context) {
		context.visitedObjectStack.push(bean);
	}

	@Override
	public void onEnd(Object bean, Context context) {
		context.visitedObjectStack.pop();
	}
	
	private Object findCopyInStack(Object value, Context context) {
		Class<? extends Object> aClass = value.getClass();
		if (!ReferableResource.class.isAssignableFrom(aClass)) {
			return null;
		}
		for (Object object : context.visitedObjectStack) {
			if (aClass.isInstance(object)) {
				// check id's
				try {
					ReferableResource ref = (ReferableResource) ((ReferableResource) object).buildReference();
					if (ref.isReferenceFor(value)) {
						return object;
					}
				} catch (ReferenceBuildingException e) {
					LOG.warn("could not build reference", e);
				}
			}
		}
		return null;
	}

	@Override
	public void beforeVisitReference(Object value, Field field, Object bean, Context context) {
		if (value == null) {
			return;
		}
		Object copy = findCopyInStack(value, context);
		if (copy != null) {
			boolean ia = field.isAccessible();
			try {
				// set original in the field
				field.setAccessible(true);
				field.set(bean, copy);
			} catch (IllegalArgumentException | IllegalAccessException ex) {
				if (LOG.isWarnEnabled()) {
					LOG.warn("could not rewire field " + field.getName(), ex);
				}
			} finally {
				field.setAccessible(ia);
			}
		} else {
			context.visitedObjectStack.push(value);
		}
	}

	@Override
	public void afterVisitReference(Object value, Field field, Object bean, Context context) {
		if (value == null) {
			return;
		}
		if (context.visitedObjectStack.peek() == value) {
			context.visitedObjectStack.pop();
		}
	}

	@Override
	public void beforeVisitReferenceInCollection(Object object, Collection c, Field field, Object bean, Context context) {
		context.visitedObjectStack.push(object);
	}

	@Override
	public void afterVisitReferenceInCollection(Object object, Collection c, Field field, Object bean, Context context) {
		context.visitedObjectStack.pop();
	}
	

}
