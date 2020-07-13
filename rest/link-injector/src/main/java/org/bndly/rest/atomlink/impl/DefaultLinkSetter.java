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

import org.bndly.common.reflection.ReflectionUtil;
import org.bndly.rest.atomlink.api.AtomLinkBean;
import org.bndly.rest.atomlink.api.annotation.AtomLinkHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class DefaultLinkSetter implements LinkSetter<Object> {

	private static Logger log = LoggerFactory.getLogger(DefaultLinkSetter.class);

	@Override
	public Class<Object> getSupportedType() {
		return Object.class;
	}

	@Override
	public void setLinkInto(AtomLinkBean link, Object target) {
		if (target == null || link == null) {
			return;
		}

		Field injectionField = findInjectionField(target.getClass());
		if (injectionField == null) {
			log.error("Could not find field to inject AtomLinks");
			return;
		}
		Class<?> injectionFieldType = injectionField.getType();

		Collection<AtomLinkBean> links = new ArrayList<>();

		Class<? extends AtomLinkBean> atomLinkClass = injectionField.getAnnotation(AtomLinkHolder.class).value();
		AtomLinkBean linkCopy;
		try {
			linkCopy = atomLinkClass.newInstance();
		} catch (InstantiationException | IllegalAccessException ex) {
			log.error("Could not create new instance of link type.");
			return;
		}
		linkCopy.setHref(link.getHref());
		linkCopy.setRel(link.getRel());
		linkCopy.setMethod(link.getMethod());

		links.add(linkCopy);

		Object alreadyExistingLinks = ReflectionUtil.getFieldValue(injectionField, target);
		if (alreadyExistingLinks == null){
			if (List.class.isAssignableFrom(injectionFieldType)) {
				ReflectionUtil.setFieldValue(injectionField, links, target);
			} else {
				log.error("Link holder field is not of type List.");
				return;
			}
		} else {
			if (Collection.class.isAssignableFrom(alreadyExistingLinks.getClass())) {
				for (Object atomLinkBean : (Collection) alreadyExistingLinks) {
					if (((AtomLinkBean) atomLinkBean).getRel().equals(link.getRel())) {
						// if we already have a link for this rel, we stop.
						return;
					}
				}
				((Collection) alreadyExistingLinks).addAll(links);
			} else {
				// unsupported object in the link holder field
				log.error("Link holder field is not of type Collection.");
				return;
			}
		}

	}

	private Field findInjectionField(Class targetClass) {
		return ReflectionUtil.getFieldByAnnotation(AtomLinkHolder.class, targetClass);
	}

}
