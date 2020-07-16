package org.bndly.common.app;

/*-
 * #%L
 * App Main
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

import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class MapBasedRunModeSettings implements RunModeSettings {

	private final Map<Integer, Properties> runModePropertiesByStartLevel;
	private final Set<String> activeRunModes;

	public MapBasedRunModeSettings(Map<Integer, Properties> runModePropertiesByStartLevel, Set<String> activeRunModes) {
		this.runModePropertiesByStartLevel = runModePropertiesByStartLevel;
		this.activeRunModes = activeRunModes;
	}

	@Override
	public boolean isActive(String bundleFileName) {
		for (Map.Entry<Integer, Properties> entry : runModePropertiesByStartLevel.entrySet()) {
			Properties value = entry.getValue();
			String runModes = value.getProperty(bundleFileName);
			if (runModes != null) {
				String[] split = runModes.split(",");
				for (String activeRunMode : activeRunModes) {
					for (String requiredRunMode : split) {
						if (requiredRunMode.equals(activeRunMode)) {
							return true;
						}
					}
				}
				return false;
			}
		}
		return true; // manually added bundles are always active by default.
	}

}
