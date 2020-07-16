package org.bndly.rest.repository.resources.json;

/*-
 * #%L
 * REST Repository Resource
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

import org.bndly.common.crypto.api.Base64Service;
import org.bndly.common.json.serializing.JSONWriter;
import org.bndly.schema.api.repository.Property;
import org.bndly.schema.api.repository.RepositoryException;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class BinaryJSONPropertyWriter implements JSONPropertyWriter {

	private final Base64Service base64Service;

	public BinaryJSONPropertyWriter(Base64Service base64Service) {
		if (base64Service == null) {
			throw new IllegalArgumentException("base64Service is not allowed to be null");
		}
		this.base64Service = base64Service;
	}

	@Override
	public void write(Property property, JSONWriter writer) throws RepositoryException, IOException {
		InputStream binary = property.getBinary();
		if (binary != null) {
			writer.writeString(base64Service.base64Encode(binary));
		} else {
			writer.writeNull();
		}
	}

}
