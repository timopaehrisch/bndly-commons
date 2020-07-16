package org.bndly.rest.repository.resources;

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

import org.bndly.rest.api.ContentType;
import org.bndly.rest.api.Context;
import org.bndly.schema.api.repository.Node;
import org.bndly.schema.api.repository.RepositoryException;
import java.io.IOException;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public interface PayloadToNodeConverter {

	public void convertPayload(Node node, Context context) throws RepositoryException, IOException;

	public ContentType getSupportedContentType();
	
}
