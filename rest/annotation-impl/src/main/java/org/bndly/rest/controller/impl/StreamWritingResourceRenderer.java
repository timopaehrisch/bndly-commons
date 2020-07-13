package org.bndly.rest.controller.impl;

/*-
 * #%L
 * REST Controller Impl
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
import org.bndly.common.data.io.ReplayableInputStream;
import org.bndly.rest.api.Context;
import org.bndly.rest.api.Resource;
import org.bndly.rest.api.ResourceRenderer;
import org.bndly.rest.api.StatusWriter.Code;
import org.bndly.rest.controller.api.InputStreamResource;
import org.bndly.rest.controller.api.Response;
import org.bndly.rest.controller.api.ResponseResource;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = ResourceRenderer.class, immediate = true)
public class StreamWritingResourceRenderer implements ResourceRenderer {
	private static final Logger LOG = LoggerFactory.getLogger(StreamWritingResourceRenderer.class);
	@Override
	public boolean supports(Resource resource, Context context) {
		return StreamWritingResource.class.isInstance(resource) || InputStreamResource.class.isInstance(resource);
	}

	@Override
	public void render(Resource resource, Context context) throws IOException {
		InputStreamResource swr = (InputStreamResource) resource;
		if (StreamWritingResource.class.isInstance(resource)) {
			Response resp = ((ResponseResource)resource).getResponse();
			if (resp.getContentType() != null) {
				context.setOutputContentType(resp.getContentType(), resp.getEncoding());
			}
			Map<String, String> headers = resp.getHeaders();
			if (headers != null) {
				for (Map.Entry<String, String> header : headers.entrySet()) {
					String key = header.getKey();
					String value = header.getValue();
					context.setOutputHeader(key, value);
				}
			}
			Code code = Code.fromHttpCode(resp.getStatus());
			if (code != null) {
				context.getStatusWriter().write(code);
			}
		}
		OutputStream os = context.getOutputStream();
		try (InputStream is = swr.getInputStream()) {
			if (!context.canBeCached() && ReplayableInputStream.class.isInstance(is)) {
				((ReplayableInputStream) is).noOp();
			}
			long start = System.currentTimeMillis();
			try {
				IOUtils.copyBuffered(is, os, 1024 * 1024);
				os.flush();
			} finally {
				long end = System.currentTimeMillis();
				LOG.trace("copying input stream to context outputstream took {}ms", (end - start));
			}
		}
	}
	
}
