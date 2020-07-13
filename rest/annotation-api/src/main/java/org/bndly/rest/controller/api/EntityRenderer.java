package org.bndly.rest.controller.api;

/*-
 * #%L
 * REST Annotation API
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

import org.bndly.rest.api.ContentType;
import java.io.IOException;
import java.io.OutputStream;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public interface EntityRenderer {
	public ContentType getSupportedContentType();
	public String getSupportedEncoding();
	public void render(Object entity, OutputStream os) throws IOException;
}
