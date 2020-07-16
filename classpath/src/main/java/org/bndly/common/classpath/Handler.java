package org.bndly.common.classpath;

/*-
 * #%L
 * Classpath
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

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import org.osgi.service.component.annotations.Component;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = {Handler.class, URLStreamHandler.class}, immediate = true)
public class Handler extends URLStreamHandler {

	static {
		String key = "java.protocol.handler.pkgs";
		String handlerDef = System.getProperty(key);
		String thisPackage = "org.bndly.common";

		if (handlerDef != null && !"".equals(handlerDef)) {
			if (!handlerDef.contains(thisPackage)) {
				handlerDef = thisPackage + "|" + handlerDef;
			}
		} else {
			handlerDef = thisPackage;
		}

		System.setProperty(key, handlerDef);
	}

	static void init() {
		// just make sure this class is loaded
	}

	@Override
	protected URLConnection openConnection(URL url) throws IOException {
		String path = url.getPath();
		ClassLoader classloader = Thread.currentThread().getContextClassLoader();
		URL resourceUrl = classloader.getResource(path);
		if (resourceUrl == null) {
			throw new IOException("could not open classpath resource at '" + path + "' for the current threads classloader");
		}
		return resourceUrl.openConnection();
	}

}
