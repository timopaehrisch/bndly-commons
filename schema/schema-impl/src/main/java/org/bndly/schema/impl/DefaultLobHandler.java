package org.bndly.schema.impl;

/*-
 * #%L
 * Schema Impl
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

import org.bndly.schema.api.services.Engine;
import org.bndly.schema.api.mapper.LobHandler;
import org.bndly.schema.vendor.VendorConfiguration;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class DefaultLobHandler implements LobHandler {

	private final VendorConfiguration vendorConfiguration;
	private final Engine engine;

	public DefaultLobHandler(VendorConfiguration vendorConfiguration, Engine engine) {
		this.vendorConfiguration = vendorConfiguration;
		this.engine = engine;
	}

	@Override
	public byte[] getBlobAsBytes(ResultSet rs, String columnName) {
		try {
			Blob blob = rs.getBlob(columnName);
			return blob.getBytes(0, (int) blob.length());
		} catch (SQLException ex) {
			throw vendorConfiguration.getErrorCodeMapper().map("could not get byte[] for blob in column " + columnName, ex, engine);
		}
	}

	@Override
	public InputStream getBlobAsBinaryStream(ResultSet rs, String columnName) {
		try {
			return rs.getBinaryStream(columnName);
		} catch (SQLException ex) {
			throw vendorConfiguration.getErrorCodeMapper().map("could not get input stream for blob in column " + columnName, ex, engine);
		}
	}

}
