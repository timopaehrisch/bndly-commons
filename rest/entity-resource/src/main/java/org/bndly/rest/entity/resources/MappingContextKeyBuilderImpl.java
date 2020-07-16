package org.bndly.rest.entity.resources;

/*-
 * #%L
 * REST Entity Resource
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

import org.bndly.common.mapper.ComplexTypeDetector;
import org.bndly.common.mapper.MappingContextKey;
import org.bndly.common.mapper.MappingContextKeyBuilder;
import org.bndly.schema.beans.ActiveRecord;
import org.bndly.schema.beans.SchemaBeanInvocationHandler;
import org.bndly.schema.model.Type;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

public class MappingContextKeyBuilderImpl implements MappingContextKeyBuilder {

	private ComplexTypeDetector complexTypeDetector;

	@Override
	public MappingContextKey buildMappingContextKey(Object source) {
		if (source == null) {
			return null;
		}
		if (!complexTypeDetector.isComplexType(source.getClass(), source)) {
			return null;
		}
		Long id = null;
		String typeName = null;
		if (ActiveRecord.class.isInstance(source)) {
			ActiveRecord ar = ActiveRecord.class.cast(source);
			if (Proxy.isProxyClass(source.getClass())) {
				InvocationHandler ih = Proxy.getInvocationHandler(source);
				if (SchemaBeanInvocationHandler.class.isInstance(ih)) {
					Type t = SchemaBeanInvocationHandler.class.cast(ih).getRecord().getType();
					typeName = t.getName();
				}
			}
			id = ar.getId();
		}
		if (id == null || typeName == null) {
			return null;
		}
		MappingContextKeyImpl key = new MappingContextKeyImpl(id, typeName);
		return key;
	}

	public void setComplexTypeDetector(ComplexTypeDetector complexTypeDetector) {
		this.complexTypeDetector = complexTypeDetector;
	}

}
