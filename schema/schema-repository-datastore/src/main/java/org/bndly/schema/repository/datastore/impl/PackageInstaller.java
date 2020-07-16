package org.bndly.schema.repository.datastore.impl;

/*-
 * #%L
 * Schema Repository Datastore
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
import org.bndly.common.data.api.DataStoreListener;
import org.bndly.common.data.io.ReplayableInputStream;
import org.bndly.schema.api.repository.PackageImporter;
import org.bndly.schema.api.repository.Repository;
import org.bndly.schema.api.repository.RepositoryException;
import org.bndly.schema.api.repository.RepositorySession;
import java.io.IOException;
import java.util.regex.Pattern;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = {PackageInstaller.class, DataStoreListener.class}, immediate = true)
@Designate(ocd = PackageInstaller.Configuration.class)
public class PackageInstaller implements DataStoreListener {

	@ObjectClassDefinition(
			name = "Repository package installer"
	)
	public @interface Configuration {

		@AttributeDefinition(
				name = "Enabled",
				description = "Set this property to true, to automatically install packages from datastores."
		)
		boolean enabled() default true;

		@AttributeDefinition(
				name = "Regex to filter data packages",
				description = "A regular expression, that has to match to a dataname in order to install it as a package."
		)
		String dataNameRegex() default "[a-zA-Z0-9\\-\\_]+\\.zip";
	}

	private static final Logger LOG = LoggerFactory.getLogger(PackageInstaller.class);
	private Boolean enabled;
	private String dataNameRegex;
	private Pattern dataNamePattern;

	@Reference
	private PackageImporter packageImporter;
	@Reference
	private Repository repository;
	
	@Activate
	public void activate(Configuration configuration) {
		enabled = configuration.enabled();
		dataNameRegex = configuration.dataNameRegex();
		dataNamePattern = Pattern.compile(dataNameRegex);
	}

	@Override
	public void dataStoreIsReady(DataStore dataStore) {
		if (!enabled) {
			return;
		}
		for (Data data : dataStore.list()) {
			install(data);
		}
	}

	@Override
	public void dataStoreClosed(DataStore dataStore) {
	}

	@Override
	public Data dataCreated(DataStore dataStore, Data data) {
		install(data);
		return data;
	}

	@Override
	public Data dataUpdated(DataStore dataStore, Data data) {
		install(data);
		return data;
	}

	@Override
	public Data dataDeleted(DataStore dataStore, Data data) {
		return data;
	}

	private void install(Data data) {
		if (!enabled) {
			return;
		}
		String name = data.getName();
		if (dataNamePattern.matcher(name).matches()) {
			// install it
			ReplayableInputStream is = data.getInputStream();
			if (is != null) {
				LOG.info("installing package {}", name);
				try (RepositorySession session = repository.createAdminSession()) {
					packageImporter.importRepositoryData(is, session);
					session.flush();
					LOG.info("installed package {}", name);
				} catch (RepositoryException | IOException e) {
					LOG.error("failed to install package " + name + ": " + e.getMessage(), e);
				}
			}
		}
	}

}
