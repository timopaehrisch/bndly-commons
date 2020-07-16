package org.bndly.rest.schema.resources;

/*-
 * #%L
 * REST Schema Resource
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

import org.bndly.common.data.io.IOUtils;
import org.bndly.rest.api.Context;
import org.bndly.rest.api.Resource;
import org.bndly.rest.api.ResourceRenderer;
import org.bndly.schema.model.Attribute;
import org.bndly.schema.model.BinaryAttribute;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class BinaryAttributeResourceRenderer implements ResourceRenderer {

	@Override
	public boolean supports(Resource resource, Context context) {
		boolean resourceTypeMatches = SchemaResourceAttributeInstance.class.isInstance(resource);
		if (!resourceTypeMatches) {
			return false;
		}
		SchemaResourceAttributeInstance r = (SchemaResourceAttributeInstance) resource;
		Attribute att = r.getAttribute();
		return BinaryAttribute.class.isInstance(att);
	}

	@Override
	public void render(Resource resource, Context context) throws IOException {
		SchemaResourceAttributeInstance r = (SchemaResourceAttributeInstance) resource;
		Object v = r.getRecord().getAttributeValue(r.getAttribute().getName());
		if (v == null) {
			return;
		}
		if (InputStream.class.isInstance(v)) {
			try (OutputStream os = context.getOutputStream()) {
				IOUtils.copy((InputStream) v, os);
				os.flush();
			}
		} else if (v.getClass().isArray()) {
			try (OutputStream os = context.getOutputStream()) {
				IOUtils.copy((byte[]) v, os);
				os.flush();
			}
		}
	}

}
