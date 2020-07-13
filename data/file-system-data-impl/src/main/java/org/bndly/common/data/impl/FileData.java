package org.bndly.common.data.impl;

/*-
 * #%L
 * File System Data Impl
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

import org.bndly.common.data.io.ReplayableInputStream;
import org.bndly.common.data.api.SimpleData;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class FileData extends SimpleData {
	private final File file;
	private final String pathPrefix;

	public FileData(final File file, String pathPrefix) {
		super(new LazyLoader() {
			@Override
			public ReplayableInputStream getBytes() {
				try {
					return ReplayableInputStream.newInstance(new FileInputStream(file));
				} catch (IOException ex) {
					throw new IllegalStateException("could not read file data.", ex);
				}
			}
		});
		this.file = file;
		this.pathPrefix = pathPrefix;
	}

	@Override
	public String getName() {
		if (pathPrefix != null && !pathPrefix.isEmpty()) {
			return file.getAbsolutePath().substring(pathPrefix.length());
		} else {
			return file.getAbsolutePath();
		}
	}

	@Override
	public Date getCreatedOn() {
		return new Date(file.lastModified());
	}

	@Override
	public Date getUpdatedOn() {
		return new Date(file.lastModified());
	}
	
	public boolean delete() {
		return file.delete();
	}
	
}
