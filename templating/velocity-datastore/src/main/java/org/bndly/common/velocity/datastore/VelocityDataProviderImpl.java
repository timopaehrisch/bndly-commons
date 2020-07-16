package org.bndly.common.velocity.datastore;

/*-
 * #%L
 * Velocity DataStore
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

import org.bndly.common.data.api.Data;
import org.bndly.common.data.api.DataStore;
import org.bndly.common.data.io.CloseOnLastByteOrExceptionInputStreamWrapper;
import org.bndly.common.data.io.IOUtils;
import org.bndly.common.data.io.ReplayableInputStream;
import org.bndly.common.velocity.api.VelocityDataProvider;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class VelocityDataProviderImpl implements VelocityDataProvider {
	
	private static final Logger LOG = LoggerFactory.getLogger(VelocityDataProviderImpl.class);
	
	private final DataStore dataStore;

	public VelocityDataProviderImpl(DataStore dataStore) {
		if (dataStore == null) {
			throw new IllegalArgumentException("dataStore is not allowed to be null");
		}
		this.dataStore = dataStore;
	}

	@Override
	public String getName() {
		return dataStore.getName();
	}

	@Override
	public boolean exists(String sourceName) {
		return dataStore.findByName(sourceName) != null;
	}
	
	@Override
	public byte[] getBytes(String sourceName) {
		InputStream is = getStream(sourceName);
		if (is != null) {
			try (InputStream dataIs = is) {
				return IOUtils.read(dataIs);
			} catch (IOException ex) {
				LOG.error("failed to read stream data from data store data: " + ex.getMessage(), ex);
				return null;
			}
		}
		return null;
	}

	@Override
	public InputStream getStream(String sourceName) {
		Data d = dataStore.findByName(sourceName);
		if (d == null) {
			return null;
		}
		ReplayableInputStream is = d.getInputStream();
		if (is != null) {
			return new CloseOnLastByteOrExceptionInputStreamWrapper(is);
		} else {
			return is;
		}
	}

	@Override
	public long getLastModified(String sourceName) {
		Data d = dataStore.findByName(sourceName);
		if (d == null) {
			return -1;
		}
		Date dt = d.getUpdatedOn();
		if (dt == null) {
			dt = d.getCreatedOn();
		}
		if (dt == null) {
			return 0;
		}
		return dt.getTime();
	}

	@Override
	public boolean isModified(String sourceName, long lastModified) {
		long l = getLastModified(sourceName);
		if (l == -1) {
			return false;
		}
		return lastModified > l;
	}
	
}
