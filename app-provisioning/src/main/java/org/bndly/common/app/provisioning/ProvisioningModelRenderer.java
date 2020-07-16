package org.bndly.common.app.provisioning;

/*-
 * #%L
 * App Provisioning
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

import org.bndly.common.app.provisioning.model.ProvisioningModel;
import org.bndly.common.json.marshalling.Marshaller;
import org.bndly.common.json.model.JSValue;
import org.bndly.common.json.serializing.JSONSerializer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Provides methods to turn a {@link ProvisioningModel} instance into a {@link String} that can be consumed by
 * a {@link ProvisioningModelParser} again.
 */
public class ProvisioningModelRenderer {

	public String render(ProvisioningModel model) throws IOException {
		final StringWriter stringWriter = new StringWriter();
		render(model, stringWriter);
		return stringWriter.toString();
	}

	public void render(ProvisioningModel model, OutputStream outputStream) throws IOException {
		render(model, new OutputStreamWriter(outputStream, "UTF-8"));
	}

	public void render(ProvisioningModel model, Writer writer) throws IOException {
		new JSONSerializer().serialize(model.toJsValue(), writer);
		writer.flush();
	}
}
