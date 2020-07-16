package org.eclipse.jetty.osgi.boot.utils;

/*-
 * #%L
 * REST Jetty Bundle Locator
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

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import org.osgi.framework.Bundle;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class FileLocatorHelperImpl implements BundleFileLocatorHelper {

	private static final String JARDIR_PROTOCOL = "jardir:";
	
	@Override
	public File getBundleInstallLocation(Bundle bundle) throws Exception {
		File location = BundleFileLocatorHelper.DEFAULT.getBundleInstallLocation(bundle);
		if (location == null) {
			// this might be a jardir
			String tmp = bundle.getLocation();
			if (tmp.startsWith(JARDIR_PROTOCOL)) {
				tmp = tmp.substring(JARDIR_PROTOCOL.length());
				Path path = Paths.get(tmp);
				if (Files.isDirectory(path)) {
					return path.toFile();
				}
			}
			return location;
		}
		return location;
	}

	@Override
	public File getFileInBundle(Bundle bundle, String path) throws Exception {
		File location = BundleFileLocatorHelper.DEFAULT.getFileInBundle(bundle, path);
		return location;
	}

	@Override
	public File[] locateJarsInsideBundle(Bundle bundle) throws Exception {
		File[] locations = BundleFileLocatorHelper.DEFAULT.locateJarsInsideBundle(bundle);
		return locations;
	}

	@Override
	public Enumeration<URL> findEntries(Bundle bundle, String entryPath) {
		Enumeration<URL> entries = BundleFileLocatorHelper.DEFAULT.findEntries(bundle, entryPath);
		return entries;
	}

	@Override
	public URL getLocalURL(URL url) throws Exception {
		URL result = BundleFileLocatorHelper.DEFAULT.getLocalURL(url);
		return result;
	}

	@Override
	public URL getFileURL(URL url) throws Exception {
		URL result = BundleFileLocatorHelper.DEFAULT.getFileURL(url);
		return result;
	}
	
}
