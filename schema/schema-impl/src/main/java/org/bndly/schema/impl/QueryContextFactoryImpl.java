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

import org.bndly.schema.api.services.Accessor;
import org.bndly.schema.api.query.QueryContext;
import org.bndly.schema.api.services.QueryContextFactory;
import org.bndly.schema.api.services.TableRegistry;
import org.bndly.schema.vendor.VendorConfiguration;

public class QueryContextFactoryImpl implements QueryContextFactory {
    private Accessor accessor;
    private TableRegistry tableRegistry;
    private MediatorRegistryImpl mediatorRegistry;
	private VendorConfiguration vendorConfiguration;

    @Override
    public QueryContext buildQueryContext() {
        QueryContext qc = new QueryContextImpl(tableRegistry, mediatorRegistry, accessor, vendorConfiguration);
        return qc;
    }

    public void setAccessor(Accessor accessor) {
        this.accessor = accessor;
    }
    
    public void setTableRegistry(TableRegistry tableRegistry) {
        this.tableRegistry = tableRegistry;
    }

    public void setMediatorRegistry(MediatorRegistryImpl mediatorRegistry) {
        this.mediatorRegistry = mediatorRegistry;
    }

	public void setVendorConfiguration(VendorConfiguration vendorConfiguration) {
		this.vendorConfiguration = vendorConfiguration;
	}
    
}
