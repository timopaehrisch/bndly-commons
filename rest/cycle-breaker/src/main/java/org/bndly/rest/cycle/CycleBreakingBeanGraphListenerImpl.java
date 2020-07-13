package org.bndly.rest.cycle;

/*-
 * #%L
 * REST Cycle Breaker
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
import org.bndly.common.reflection.InstantiationUtil;
import org.bndly.common.reflection.ReflectionUtil;
import org.bndly.rest.atomlink.api.annotation.Reference;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import org.osgi.service.component.annotations.Component;

@Component(service = BeanGraphIteratorListener.class)
public class CycleBreakingBeanGraphListenerImpl extends NoOpGraphListener<CycleBreakingContext> implements BeanGraphIteratorListener<CycleBreakingContext> {

	@Override
	public Class<CycleBreakingContext> getIterationContextType() {
		return CycleBreakingContext.class;
	}

	@Override
	public void onRevisitReference(Object bean, Field field, Object owner, CycleBreakingContext context) {
		Object ref = buildReference(bean);
		ReflectionUtil.setFieldValue(field, ref, owner);
	}

	@Override
	public void onRevisitReferenceInCollection(Object bean, Collection collection, CycleBreakingContext context) {
		collection.remove(bean);
		Object ref = buildReference(bean);
		if (ref != null) {
			collection.add(ref);
		}
	}

	private Object buildReference(Object bean) {
		Class<?> referenceType = findReferenceType(bean.getClass());
		if (referenceType == null) {
			return bean;
		}
		Object ref = InstantiationUtil.instantiateType(referenceType);
		List<Field> fields = ReflectionUtil.collectAllFieldsFromClass(bean.getClass());
		if (fields != null) {
			for (Field field : fields) {
				Object value = ReflectionUtil.getFieldValue(field, bean);
				ReflectionUtil.setFieldValue(field, value, ref);
			}
		}
		return ref;
	}

	public void setIteratorListeners(List<BeanGraphIteratorListener> listOfListeners) {
		listOfListeners.add(this);
	}

	private Class<?> findReferenceType(Class<?> referenceTypeOrSubTypeOfReferenceType) {
		if (referenceTypeOrSubTypeOfReferenceType == null) {
			return null;
		}
		if (referenceTypeOrSubTypeOfReferenceType.isAnnotationPresent(Reference.class)) {
			return referenceTypeOrSubTypeOfReferenceType;
		}
		return findReferenceType(referenceTypeOrSubTypeOfReferenceType.getSuperclass());
	}

}
