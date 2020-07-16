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
import org.bndly.common.json.model.JSString;
import org.bndly.common.json.model.JSValue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class StartLevelBundle {
	private final int startLevel;
	private final List<ArtifactDefinition> artifacts;

	StartLevelBundle(int startLevel, JSArray jsArray) {
		List<ArtifactDefinition> tmp = new ArrayList<>();
		// jsArray contains strings in the form of groupid:artifactid:version:type
		for (JSValue str : jsArray) {
			if (JSString.class.isInstance(str)) {
				tmp.add(new ArtifactDefinition(((JSString) str).getValue()));
			}
		}
		this.artifacts = Collections.unmodifiableList(tmp);
		this.startLevel = startLevel;
	}

	public int getStartLevel() {
		return startLevel;
	}

	public List<ArtifactDefinition> getArtifacts() {
		return artifacts;
	}
	
}
