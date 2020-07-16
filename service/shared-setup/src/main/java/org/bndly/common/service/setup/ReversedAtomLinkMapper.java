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

import org.bndly.common.mapper.MappingContext;
import org.bndly.common.mapper.MappingState;
import org.bndly.common.mapper.TypeSpecificMapper;
import org.bndly.rest.atomlink.api.AtomLinkBean;
import org.bndly.rest.common.beans.AtomLink;
import org.bndly.common.service.model.api.Link;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class ReversedAtomLinkMapper implements TypeSpecificMapper {

	@Override
	public Class<?> getSupportedInput() {
		return Link.class;
	}

	@Override
	public Class<?> getSupportedOutput() {
		return AtomLink.class;
	}

	@Override
	public void map(Object source, Object target, MappingContext context, MappingState state) {
		AtomLinkBean t = (AtomLinkBean) target;
		Link s = (Link) source;
		t.setMethod(s.getMethod());
		t.setRel(s.getRel());
		t.setHref(s.getUrl());
	}
}
