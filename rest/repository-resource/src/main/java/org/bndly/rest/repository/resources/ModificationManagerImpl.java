package org.bndly.rest.repository.resources;

/*-
 * #%L
 * REST Repository Resource
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

import org.bndly.schema.api.repository.Property;
import org.bndly.schema.api.repository.RepositoryException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class ModificationManagerImpl implements ModificationManager {

	private final Map<String, Modification> modificationsByPropertyPath = new HashMap<>();
	private final List<RepositoryRunnable> runnables = new ArrayList<>();

	private static interface RepositoryRunnable {

		void run() throws RepositoryException;
	}

	@Override
	public Modification createModification(Property property) {
		String path = new StringBuilder(property.getNode().getPath().toString()).append(".").append(property.getName()).toString();
		Modification modification = modificationsByPropertyPath.get(path);
		if (modification == null) {
			modification = new ModificationImpl(property);
			modificationsByPropertyPath.put(path, modification);
		}
		return modification;
	}

	public void flush() throws RepositoryException {
		for (RepositoryRunnable runnable : runnables) {
			runnable.run();
		}
	}

	private class ModificationImpl implements Modification, RepositoryRunnable {

		private final Property property;
		private boolean isScheduled = false;
		private boolean setValues = false;
		private Object[] rawValues;
		private Set<Integer> indicesToDrop;
		private List<Object> valuesToAdd;
		private Map<Integer, Object> valuesToSet;

		public ModificationImpl(Property property) {
			this.property = property;
		}

		@Override
		public void run() throws RepositoryException {
			if (setValues) {
				property.setValues(rawValues);
				return;
			} else {
				if (indicesToDrop == null && valuesToAdd == null && valuesToSet == null) {
					return;
				} else if (indicesToDrop == null && valuesToAdd == null && valuesToSet != null) {
					for (Map.Entry<Integer, Object> entry : valuesToSet.entrySet()) {
						property.setValue(entry.getKey(), entry.getValue());
					}
				} else if (indicesToDrop == null && valuesToAdd != null && valuesToSet == null) {
					for (Object value : valuesToAdd) {
						property.addValue(value);
					}
				} else if (indicesToDrop != null && valuesToAdd == null && valuesToSet == null) {
					Object[] values = property.getValues();
					ArrayList<Object> newValues = new ArrayList<>();
					for (int i = 0; i < values.length; i++) {
						if (!indicesToDrop.contains(i)) {
							newValues.add(values[i]);
						}
					}
					property.setValues(newValues.toArray());
				} else if (indicesToDrop == null && valuesToAdd != null && valuesToSet != null) {
					for (Map.Entry<Integer, Object> entry : valuesToSet.entrySet()) {
						property.setValue(entry.getKey(), entry.getValue());
					}
					for (Object value : valuesToAdd) {
						property.addValue(value);
					}
				} else if (indicesToDrop != null && valuesToAdd != null && valuesToSet == null) {
					Object[] values = property.getValues();
					ArrayList<Object> newValues = new ArrayList<>();
					for (int i = 0; i < values.length; i++) {
						if (!indicesToDrop.contains(i)) {
							newValues.add(values[i]);
						}
					}
					newValues.addAll(valuesToAdd);
					property.setValues(newValues.toArray());
				} else if (indicesToDrop != null && valuesToAdd == null && valuesToSet != null) {
					Object[] values = property.getValues();
					for (Map.Entry<Integer, Object> entry : valuesToSet.entrySet()) {
						if (values.length > entry.getKey()) {
							values[entry.getKey()] = entry.getValue();
						}
					}
					ArrayList<Object> newValues = new ArrayList<>();
					for (int i = 0; i < values.length; i++) {
						if (!indicesToDrop.contains(i)) {
							newValues.add(values[i]);
						}
					}
					property.setValues(newValues.toArray());
				} else if (indicesToDrop != null && valuesToAdd != null && valuesToSet != null) {
					Object[] values = property.getValues();
					for (Map.Entry<Integer, Object> entry : valuesToSet.entrySet()) {
						if (values.length > entry.getKey()) {
							values[entry.getKey()] = entry.getValue();
						}
					}
					ArrayList<Object> newValues = new ArrayList<>();
					for (int i = 0; i < values.length; i++) {
						if (!indicesToDrop.contains(i)) {
							newValues.add(values[i]);
						}
					}
					newValues.addAll(valuesToAdd);
					property.setValues(newValues.toArray());
				}
			}
		}

		private void assertIsScheduled() {
			if (!isScheduled) {
				runnables.add(this);
				isScheduled = true;
			}
		}

		@Override
		public Modification dropValue(int index) {
			assertIsScheduled();
			if (indicesToDrop == null) {
				indicesToDrop = new HashSet<>();
			}
			indicesToDrop.add(index);
			return this;
		}

		@Override
		public Modification addValue(Object rawValue) {
			assertIsScheduled();
			if (valuesToAdd == null) {
				valuesToAdd = new ArrayList<>();
			}
			valuesToAdd.add(rawValue);
			return this;
		}

		@Override
		public Modification setValue(int index, Object rawValue) {
			assertIsScheduled();
			if (valuesToSet == null) {
				valuesToSet = new LinkedHashMap<>();
			}
			valuesToSet.put(index, rawValue);
			return this;
		}

		@Override
		public Modification setValues(Object... rawValues) {
			assertIsScheduled();
			setValues = true;
			this.rawValues = rawValues;
			return this;
		}

	}

}
