package org.bndly.common.app.provisioning.util;

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

import org.bndly.common.app.provisioning.Constants;
import java.nio.file.Path;
import java.util.Properties;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class FileInfoTester {
	private final Properties fileInfo;
	private final Path targetFolder;

	public FileInfoTester(Properties fileInfo, Path targetFolder) {
		this.fileInfo = fileInfo;
		this.targetFolder = targetFolder;
	}
	
	public boolean isResource(Path path) {
		String v = fileInfo.getProperty(targetFolder.relativize(path).normalize().toString());
		return Constants.FILE_TYPE_RESOURCE.equals(v);
	}
	public boolean isResourceDirectory(Path path) {
		String v = fileInfo.getProperty(targetFolder.relativize(path).normalize().toString());
		return Constants.FILE_TYPE_RESOURCE_DIRECTORY.equals(v);
	}
	public boolean isBundle(Path path) {
		String v = fileInfo.getProperty(targetFolder.relativize(path).normalize().toString());
		return Constants.FILE_TYPE_BUNDLE.equals(v);
	}
	public boolean isConfig(Path path) {
		String v = fileInfo.getProperty(targetFolder.relativize(path).normalize().toString());
		return Constants.FILE_TYPE_CONFIG.equals(v);
	}
}
