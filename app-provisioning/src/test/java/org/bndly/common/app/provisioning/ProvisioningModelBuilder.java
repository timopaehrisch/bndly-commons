package org.bndly.common.app.provisioning;

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

import org.bndly.common.json.model.JSArray;
import org.bndly.common.json.model.JSBoolean;
import org.bndly.common.json.model.JSNull;
import org.bndly.common.json.model.JSNumber;
import org.bndly.common.json.model.JSObject;
import org.bndly.common.json.model.JSString;

import java.math.BigDecimal;
import java.util.List;

/**
 * Utility class to facilitate the creation of {@link JSObject} hierarchies that can be used
 * to create {@link org.bndly.common.app.provisioning.model.ProvisioningModel} instances,
 * e.g. as test fixtures.
 */
final class ProvisioningModelBuilder {

	private final JSObject model;

	public ProvisioningModelBuilder() {
		model = new JSObject();
	}

	public RunModeBuilder runMode(String name) {
		return new RunModeBuilder(this, model, name);
	}

	public JSObject build() {
		return model;
	}

	public static final class RunModeBuilder {
		private final ProvisioningModelBuilder parent;
		private final JSObject runMode;

		private RunModeBuilder(ProvisioningModelBuilder parent, JSObject model, String name) {
			this.parent = parent;
			this.runMode = new JSObject();
			model.createMember(name).setValue(runMode);
		}

		public BundleBuilder bundles() {
			return new BundleBuilder(this, runMode);
		}

		public ConfigBuilder configs() {
			return new ConfigBuilder(this, runMode);
		}

		public ResourcesBuilder resources() {
			return new ResourcesBuilder(this, runMode);
		}

		public ProvisioningModelBuilder done() {
			return parent;
		}
	}

	public static final class BundleBuilder {

		private final RunModeBuilder parent;
		private final JSObject bundles;

		public BundleBuilder(RunModeBuilder parent, JSObject runMode) {
			this.parent = parent;
			this.bundles = new JSObject();
			runMode.createMember("bundles").setValue(bundles);
		}

		public BundleBuilder startLevel(String startLevel, String... artifactDefinitions) {
			JSArray artifacts = new JSArray();
			for (String artifactDefinition : artifactDefinitions) {
				artifacts.add(new JSString(artifactDefinition));
			}

			bundles.createMember(startLevel).setValue(artifacts);
			return this;
		}

		public RunModeBuilder done() {
			return parent;
		}
	}

	public static final class ConfigBuilder {

		private RunModeBuilder parent;
		private final JSObject configs;

		public ConfigBuilder(RunModeBuilder parent, JSObject runMode) {
			this.parent = parent;
			this.configs = new JSObject();
			runMode.createMember("configs").setValue(configs);
		}

		public ConfigEntryBuilder entry(String name) {
			return new ConfigEntryBuilder(this, configs, name);
		}

		public RunModeBuilder done() {
			return parent;
		}
	}

	public static final class ConfigEntryBuilder {

		private final ConfigBuilder parent;
		private final JSObject entry;

		public ConfigEntryBuilder(ConfigBuilder parent, JSObject configs, String name) {
			this.parent = parent;
			this.entry = new JSObject();
			configs.createMember(name).setValue(entry);
		}

		public ConfigEntryBuilder string(String value) {
			entry.createMember("string").setValue(new JSString(value));
			return this;
		}

		public ConfigEntryBuilder number(BigDecimal value) {
			entry.createMember("number").setValue(new JSNumber(value));
			return this;
		}

		public ConfigEntryBuilder bool(boolean value) {
			entry.createMember("boolean").setValue(new JSBoolean(value));
			return this;
		}

		public ConfigEntryBuilder nullValue() {
			entry.createMember("null").setValue(new JSNull());
			return this;
		}

		public ConfigEntryBuilder array(String... values) {
			JSArray array = new JSArray();
			for (String s : values) {
				array.add(new JSString(s));
			}
			entry.createMember("array").setValue(array);
			return this;
		}

		public ConfigBuilder done() {
			return parent;
		}
	}

	public static final class ResourcesBuilder{
		private RunModeBuilder parent;
		private final JSArray resources;

		public ResourcesBuilder(RunModeBuilder parent, JSObject model) {
			this.parent = parent;
			this.resources = new JSArray();
			model.createMember("resources").setValue(resources);
		}

		public ResourcesBuilder entry(String source, String target, List<String> includes, List<String> excludes) {
			JSObject resourceEntry = new JSObject();
			resourceEntry.createMember("source").setValue(new JSString(source));
			resourceEntry.createMember("target").setValue(new JSString(target));

			JSArray includesArray = new JSArray();
			for (String include : includes) {
				includesArray.add(new JSString(include));
			}
			resourceEntry.createMember("includes").setValue(includesArray);

			JSArray excludesArray = new JSArray();
			for (String exclude : excludes) {
				excludesArray.add(new JSString(exclude));
			}
			resourceEntry.createMember("excludes").setValue(excludesArray);

			resources.add(resourceEntry);
			return this;
		}

		public ResourcesBuilder entry(String target, String... artifactDefinitions) {
			JSObject resourceEntry = new JSObject();
			resourceEntry.createMember("target").setValue(new JSString(target));

			JSArray artifactsArray = new JSArray();
			for (String artifactDefinition : artifactDefinitions) {
				artifactsArray.add(new JSString(artifactDefinition));
			}
			resourceEntry.createMember("artifacts").setValue(artifactsArray);

			resources.add(resourceEntry);
			return this;
		}

		public RunModeBuilder done() {
			return parent;
		}
	}
}
