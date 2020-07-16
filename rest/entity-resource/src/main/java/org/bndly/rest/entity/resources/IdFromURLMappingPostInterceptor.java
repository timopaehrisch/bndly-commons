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

import org.bndly.common.json.model.JSObject;
import org.bndly.common.mapper.MappingPostInterceptor;
import org.bndly.common.mapper.MappingState;
import org.bndly.rest.api.DefaultCharacterEncodingProvider;
import org.bndly.rest.api.ResourceURI;
import org.bndly.rest.api.ResourceURIParser;
import org.bndly.rest.atomlink.api.annotation.Reference;
import org.bndly.rest.common.beans.AtomLink;
import org.bndly.rest.common.beans.RestBean;
import org.bndly.rest.descriptor.ParameterExtractor;
import org.bndly.rest.descriptor.ParameterValue;
import org.bndly.schema.api.Record;
import org.bndly.schema.beans.ActiveRecord;
import org.bndly.schema.json.beans.JSONUtil;
import org.bndly.schema.beans.SchemaBeanFactory;
import org.bndly.schema.json.beans.StreamingObject;

public class IdFromURLMappingPostInterceptor implements MappingPostInterceptor {

	private final SchemaBeanFactory schemaBeanFactory;
	private final ParameterExtractor parameterExtractor;
	private final Class<?> restBeanType;
	private final Class<?> restBeanReferenceType;
	private final DefaultCharacterEncodingProvider defaultCharacterEncodingProvider;

	public IdFromURLMappingPostInterceptor(
			SchemaBeanFactory schemaBeanFactory, 
			ParameterExtractor parameterExtractor, 
			Class<?> restBeanType, 
			DefaultCharacterEncodingProvider defaultCharacterEncodingProvider
	) {
		this.schemaBeanFactory = schemaBeanFactory;
		this.parameterExtractor = parameterExtractor;
		this.restBeanType = restBeanType;
		this.defaultCharacterEncodingProvider = defaultCharacterEncodingProvider;
		Class<?> superClass = restBeanType.getSuperclass();
		if (superClass != null && superClass.isAnnotationPresent(Reference.class)) {
			restBeanReferenceType = superClass;
		} else {
			restBeanReferenceType = null;
		}
	}

	@Override
	public void postIntercept(MappingState state) {
		Object source = state.getSource();
		Object target = state.getTarget();
		if (RestBean.class.isInstance(source)) {
			boolean isAr = ActiveRecord.class.isInstance(target);
			boolean isSo = StreamingObject.class.isInstance(target);
			if (!isAr && !isSo) {
				return;
			}
			RestBean rb = RestBean.class.cast(source);
			AtomLink selfLink = rb.follow("self");
			if (selfLink != null) {
				if (target != null) {
					if (restBeanType.isInstance(source) || source.getClass().isAssignableFrom(restBeanType)) {
						Long idFromSelfLink = getIdFromSelfLink(selfLink);
						if (ActiveRecord.class.isInstance(target)) {
							Record rec = schemaBeanFactory.getRecordFromSchemaBean(target);
							if (rec.getId() == null) {
								rec.setId(idFromSelfLink);
							}
						} else if (StreamingObject.class.isInstance(target)) {
							JSObject jsobject = schemaBeanFactory.getJsonSchemaBeanFactory().getJSObjectFromSchemaBean(target);
							if (idFromSelfLink != null) {
								if (JSONUtil.getMemberByName("id", jsobject) == null) {
									JSONUtil.createNumberMember(jsobject, "id", idFromSelfLink);
								}
							}
						}
					}
				}
			}
		}
	}

	public Class<?> getRestBeanType() {
		return restBeanType;
	}

	public Class<?> getRestBeanReferenceType() {
		return restBeanReferenceType;
	}

	public Long getIdFromSelfLink(AtomLink selfLink) {
		return getIdFromSelfLink(selfLink, restBeanType);
	}

	public Long getIdFromSelfLink(AtomLink selfLink, Class<?> restBeanType) {
		Long idFromSelfLink = null;
		String href = selfLink.getHref();
		if (href != null) {
			ParameterValue[] parameters = parameterExtractor.getParametersFromLink(restBeanType, selfLink.getRel(), href);
			if (parameters != null && parameters.length == 1) {
				ParameterValue p = parameters[0];
				String v = p.getParameterValue();
				ResourceURI uri = new ResourceURIParser(defaultCharacterEncodingProvider.createPathCoder(), href).parse().getResourceURI();
				ResourceURI.Extension ext = uri.getExtension();
				if (v != null) {
					if (ext != null) {
						int end = v.indexOf(".");
						if (end >= 0) {
							v = v.substring(0, end);
						}
					}
					try {
						idFromSelfLink = new Long(v);
					} catch (NullPointerException | NumberFormatException e) {
						// there is no id found
					}
				}
			}

		}
		return idFromSelfLink;
	}
}
