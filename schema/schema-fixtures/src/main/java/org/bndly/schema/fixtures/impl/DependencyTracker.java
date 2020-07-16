package org.bndly.schema.fixtures.impl;

/*-
 * #%L
 * Schema Fixtures
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

import org.bndly.schema.fixtures.api.FixtureDeploymentException;
import org.bndly.schema.model.NamedAttributeHolderAttribute;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
class DependencyTracker {
	
	static class OneToOneBinding {

		private final NamedAttributeHolderAttribute attribute;
		private final PersistenceItem target;

		OneToOneBinding(NamedAttributeHolderAttribute attribute, PersistenceItem target) {
			this.attribute = attribute;
			this.target = target;
		}

		NamedAttributeHolderAttribute getAttribute() {
			return attribute;
		}

		PersistenceItem getTarget() {
			return target;
		}
		
	}
	private final List<PersistenceItem> items = new ArrayList<>();
	private final List<PersistenceItem> dependents = new ArrayList<>();
	private final List<OneToOneBinding> oneToOneBindings = new ArrayList<>();
	private boolean closed = false;

	void add(PersistenceItem item) throws FixtureDeploymentException {
		if (closed) {
			throw new FixtureDeploymentException("dependency tracker is already closed");
		}
		items.add(item);
	}

	void addDependent(PersistenceItem persistenceItem) throws FixtureDeploymentException {
		dependents.add(persistenceItem);
	}

	void appendDependentsOf(PersistenceItem persistenceItem) throws FixtureDeploymentException {
		for (PersistenceItem dependent : dependents) {
			dependent.dependsOn(persistenceItem);
		}
	}

	void skipPersistenceOfDependents() {
		for (PersistenceItem dependent : dependents) {
			dependent.skipPersistence();
		}
	}

	void appendDependenciesTo(PersistenceItem persistenceItem) throws FixtureDeploymentException {
		if (closed) {
			throw new FixtureDeploymentException("dependency tracker is already closed");
		}
		for (PersistenceItem item : items) {
			persistenceItem.dependsOn(item);
		}
		items.clear();
		persistenceItem.oneToOneBindings(Collections.unmodifiableList(new ArrayList<>(oneToOneBindings)));
		oneToOneBindings.clear();
		closed = true;
	}

	void addOneToOne(NamedAttributeHolderAttribute naha, PersistenceItem item) throws FixtureDeploymentException {
		add(item);
		oneToOneBindings.add(new OneToOneBinding(naha, item));
	}
	
}
