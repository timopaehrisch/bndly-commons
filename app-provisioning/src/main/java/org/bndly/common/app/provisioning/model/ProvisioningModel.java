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

import org.bndly.common.json.model.JSMember;
import org.bndly.common.json.model.JSObject;
import org.bndly.common.json.model.JSValue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class ProvisioningModel {
	private final List<RunMode> runModes;

	public ProvisioningModel(JSObject object) {
		List<RunMode> tmp = new ArrayList<>();
		Set<JSMember> members = object.getMembers();
		if (members != null) {
			for (JSMember member : members) {
				String runModeName = member.getName().getValue();
				JSValue value = member.getValue();
				if (JSObject.class.isInstance(value)) {
					tmp.add(new RunMode(runModeName, (JSObject) value));
				}
			}
		}
		this.runModes = Collections.unmodifiableList(tmp);
	}
	
	public ProvisioningModel(ProvisioningModel... aggreagted) {
		final Map<String, List<RunMode>> runModesByName = new LinkedHashMap<>();
		for (ProvisioningModel item : aggreagted) {
			for (RunMode runMode : item.getRunModes()) {
				List<RunMode> runModesTmp = runModesByName.get(runMode.getName());
				if (runModesTmp == null) {
					runModesTmp = new ArrayList<>();
					runModesByName.put(runMode.getName(), runModesTmp);
				}
				runModesTmp.add(runMode);
			}
		}
		final List<RunMode> runModesTmp = new ArrayList<>();
		for (Map.Entry<String, List<RunMode>> entry : runModesByName.entrySet()) {
			runModesTmp.add(new RunMode(entry.getKey(), entry.getValue().toArray(new RunMode[entry.getValue().size()])));
		}
		runModes = Collections.unmodifiableList(runModesTmp);
	}

	public JSObject toJsValue() {
		JSObject jsObject = new JSObject();
		for(RunMode runMode : getRunModes()) {
			JSMember member = jsObject.createMember(runMode.getName());
			member.setValue(runMode.toJsValue());
		}

		return jsObject;
	}

	public List<RunMode> getRunModes() {
		return runModes;
	}
	
}
