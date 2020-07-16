package org.bndly.common.app.provisioning.model;

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
import org.bndly.common.json.model.JSMember;
import org.bndly.common.json.model.JSObject;
import org.bndly.common.json.model.JSString;
import org.bndly.common.json.model.JSValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class RunMode {
	private final String name;
	private final List<StartLevelBundle> bundles;
	private final List<Config> configs;
	private final List<ResourceDefinition> resources;

	RunMode(String runModeName, JSObject jsObject) {
		this.bundles = initBundles(jsObject.getMember("bundles"));
		this.configs = initConfigs(jsObject.getMember("configs"));
		this.resources = initResources(jsObject.getMember("resources"));
		this.name = runModeName;
	}
	
	RunMode(String runModeName, RunMode... aggregated) {
		final List<StartLevelBundle> bundlesTmp = new ArrayList<>();
		final List<Config> configsTmp = new ArrayList<>();
		final List<ResourceDefinition> resourcesTmp = new ArrayList<>();
		for (RunMode runMode : aggregated) {
			bundlesTmp.addAll(runMode.getBundles());
			configsTmp.addAll(runMode.getConfigs());
			resourcesTmp.addAll(runMode.getResources());
		}
		this.bundles = Collections.unmodifiableList(bundlesTmp);
		this.configs = Collections.unmodifiableList(configsTmp);
		this.resources = Collections.unmodifiableList(resourcesTmp);
		this.name = runModeName;
	}

	public JSObject toJsValue() {
		JSObject jsValue = new JSObject();

		if(!getBundles().isEmpty()) {
			JSObject bundles = new JSObject();
			for (StartLevelBundle startLevelBundle : getBundles()) {
				JSArray artifacts = new JSArray();
				for (ArtifactDefinition artifactDefinition : startLevelBundle.getArtifacts()) {
					artifacts.add(new JSString(artifactDefinition.toString()));
				}
				bundles.createMember(String.valueOf(startLevelBundle.getStartLevel())).setValue(artifacts);
			}
			jsValue.createMember("bundles").setValue(bundles);
		}

		if(!getConfigs().isEmpty()) {
			JSObject configs = new JSObject();
			for (Config config : getConfigs()) {
				configs.createMember(config.getName()).setValue(config.toJsValue());
			}
			jsValue.createMember("configs").setValue(configs);
		}

		if(!getResources().isEmpty()) {
			JSArray resources = new JSArray();
			for (ResourceDefinition resourceDefinition : getResources()) {
				resources.add(resourceDefinition.toJsValue());
			}
			jsValue.createMember("resources").setValue(resources);
		}

		return jsValue;
	}

	private List<StartLevelBundle> initBundles(JSMember member) {
		if (member == null) {
			return Collections.EMPTY_LIST;
		}
		JSValue val = member.getValue();
		if (!JSObject.class.isInstance(val)) {
			return Collections.EMPTY_LIST;
		}
		Set<JSMember> startLevelMembers = ((JSObject) val).getMembers();
		if (startLevelMembers == null || startLevelMembers.isEmpty()) {
			return Collections.EMPTY_LIST;
		}
		List<StartLevelBundle> tmp = new ArrayList<>();
		for (JSMember startLevelMember : startLevelMembers) {
			int startLevel = Integer.valueOf(startLevelMember.getName().getValue());
			if (!JSArray.class.isInstance(startLevelMember.getValue())) {
				continue;
			}
			JSArray arr = (JSArray) startLevelMember.getValue();
			if (arr.size() == 0) {
				continue;
			}
			StartLevelBundle startLevelBundle = new StartLevelBundle(startLevel, arr);
			tmp.add(startLevelBundle);
		}
		return Collections.unmodifiableList(tmp);
	}

	private List<Config> initConfigs(JSMember member) {
		if (member == null) {
			return Collections.EMPTY_LIST;
		}
		JSValue val = member.getValue();
		if (!JSObject.class.isInstance(val)) {
			return Collections.EMPTY_LIST;
		}
		Set<JSMember> configEntries = ((JSObject) val).getMembers();
		if (configEntries == null || configEntries.isEmpty()) {
			return Collections.EMPTY_LIST;
		}
		List<Config> tmp = new ArrayList<>();
		for (JSMember configEntry : configEntries) {
			String configName = configEntry.getName().getValue();
			if (!JSObject.class.isInstance(configEntry.getValue())) {
				continue;
			}
			JSObject properties = (JSObject) configEntry.getValue();
			Config config = new Config(configName, properties);
			tmp.add(config);
		}
		return Collections.unmodifiableList(tmp);
	}

	private List<ResourceDefinition> initResources(JSMember member) {
		if (member == null) {
			return Collections.EMPTY_LIST;
		}
		JSValue val = member.getValue();
		if (!JSArray.class.isInstance(val)) {
			return Collections.EMPTY_LIST;
		}
		JSArray defs = (JSArray) val;
		if (defs.size() == 0) {
			return Collections.EMPTY_LIST;
		}
		List<ResourceDefinition> tmp = new ArrayList<>();
		for (JSValue def : defs) {
			if (!JSObject.class.isInstance(def)) {
				continue;
			}
			JSObject resourceDef = (JSObject) def;
			tmp.add(new ResourceDefinition(resourceDef));
		}
		return Collections.unmodifiableList(tmp);
	}

	public String getName() {
		return name;
	}

	public List<StartLevelBundle> getBundles() {
		return bundles;
	}

	public List<Config> getConfigs() {
		return configs;
	}

	public List<ResourceDefinition> getResources() {
		return resources;
	}
	
}
