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
import org.bndly.common.json.model.JSObject;
import org.bndly.common.json.model.JSString;
import org.bndly.common.json.model.JSValue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class ResourceDefinition {
	private final String source;
	private final String target;
	private final boolean unpack;
	private final List<String> includes;
	private final List<String> excludes;
	private final List<ArtifactDefinition> artifacts;

	ResourceDefinition(JSObject resourceDef) {
		source = resourceDef.getMemberStringValue("source");
		target = resourceDef.getMemberStringValue("target");
		JSArray artifactsTmp = resourceDef.getMemberValue("artifacts", JSArray.class);
		if (artifactsTmp != null) {
			Boolean unpackTmp = resourceDef.getMemberBooleanValue("unpack");
			unpack = unpackTmp == null ? false : unpackTmp;
			List<ArtifactDefinition> tmp = new ArrayList<>();
			for (JSValue artifactDefString : artifactsTmp) {
				if (JSString.class.isInstance(artifactDefString)) {
					tmp.add(new ArtifactDefinition(((JSString) artifactDefString).getValue()));
				}
			}
			artifacts = Collections.unmodifiableList(tmp);
		} else {
			artifacts = Collections.EMPTY_LIST;
			unpack = false;
		}
		JSArray includesTmp = resourceDef.getMemberValue("includes", JSArray.class);
		if (includesTmp != null) {
			List<String> tmp = new ArrayList<>();
			for (JSValue include : includesTmp) {
				if (JSString.class.isInstance(include)) {
					tmp.add(((JSString) include).getValue());
				}
			}
			includes = Collections.unmodifiableList(tmp);
		} else {
			includes = Collections.EMPTY_LIST;
		}
		JSArray excludesTmp = resourceDef.getMemberValue("excludes", JSArray.class);
		if (excludesTmp != null) {
			List<String> tmp = new ArrayList<>();
			for (JSValue exclude : excludesTmp) {
				if (JSString.class.isInstance(exclude)) {
					tmp.add(((JSString) exclude).getValue());
				}
			}
			excludes = Collections.unmodifiableList(tmp);
		} else {
			excludes = Collections.EMPTY_LIST;
		}
	}

	public JSValue toJsValue() {
		JSObject jsValue = new JSObject();

		if(source != null) {
			jsValue.createMember("source").setValue(new JSString(source));
		}

		jsValue.createMember("target").setValue(new JSString(target));

		if(!artifacts.isEmpty()) {
			JSArray artifactDefinitions = new JSArray();
			for (ArtifactDefinition artifact : artifacts) {
				artifactDefinitions.add(new JSString(artifact.toString()));
			}
			jsValue.createMember("artifacts").setValue(artifactDefinitions);
		}

		if(!includes.isEmpty()) {
			JSArray includeDefinitions = new JSArray();
			for (String include : includes) {
				includeDefinitions.add(new JSString(include));
			}
			jsValue.createMember("includes").setValue(includeDefinitions);
		}

		if(!excludes.isEmpty()) {
			JSArray excludeDefinitions = new JSArray();
			for (String exclude : excludes) {
				excludeDefinitions.add(new JSString(exclude));
			}
			jsValue.createMember("excludes").setValue(excludeDefinitions);
		}

		return jsValue;
	}

	public boolean isUnpack() {
		return unpack;
	}

	public String getSource() {
		return source;
	}

	public String getTarget() {
		return target;
	}

	public List<String> getIncludes() {
		return includes;
	}

	public List<String> getExcludes() {
		return excludes;
	}

	public List<ArtifactDefinition> getArtifacts() {
		return artifacts;
	}
	
}
