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

import org.bndly.schema.api.tx.TransactionCallback;
import org.bndly.schema.api.exception.SchemaException;
import org.bndly.schema.api.services.Engine;
import org.bndly.schema.vendor.VendorConfiguration;
import java.sql.Connection;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class SingleConnectionTransactionTemplateImpl extends TransactionTemplateImpl {
	private Connection connection;

	public SingleConnectionTransactionTemplateImpl(VendorConfiguration vendorConfiguration, Engine engine) {
		super(vendorConfiguration, engine);
		setCloseConnectionAfterUsage(false);
	}

	@Override
	protected Connection getConnection() throws SchemaException {
		if (connection == null) {
			connection = super.getConnection();
		}
		return connection;
	}

	@Override
	public synchronized <E> E doInTransaction(TransactionCallback<E> transactionCallback) {
		return super.doInTransaction(transactionCallback);
	}

}
