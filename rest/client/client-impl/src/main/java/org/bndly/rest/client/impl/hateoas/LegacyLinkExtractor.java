package org.bndly.rest.client.impl.hateoas;

/*-
 * #%L
 * REST Client Impl
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
import org.bndly.rest.client.api.LinkExtractor;
import java.lang.reflect.Field;
import java.util.List;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class LegacyLinkExtractor implements LinkExtractor {
	public static final LegacyLinkExtractor INSTANCE = new LegacyLinkExtractor();

	private LegacyLinkExtractor() {
	}
	
	@Override
	public AtomLinkBean extractLink(String rel, Object source) {
		// extract the link from "resource"
		// set the url and the httpMethod
		Field field = ReflectionUtil.getFieldByAnnotation(AtomLinkHolder.class, source);
		if (field == null) {
			throw new IllegalArgumentException("could not find a field with the " + AtomLinkHolder.class.getSimpleName() + " in " + source.getClass().getSimpleName());
		}
		List<? extends AtomLinkBean> links = (List<? extends AtomLinkBean>) ReflectionUtil.getFieldValue(field, source);
		if (links != null) {
			for (AtomLinkBean atomLinkBean : links) {
				if (atomLinkBean.getRel().equals(rel)) {
					String method = atomLinkBean.getMethod();
					if (method == null) {
						method = "GET";
					}
					atomLinkBean.setMethod(method);
					return atomLinkBean;
				}
			}
		}
		return null;
	}
	
}
