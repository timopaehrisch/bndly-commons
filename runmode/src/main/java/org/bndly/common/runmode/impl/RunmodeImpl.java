package org.bndly.common.runmode.impl;

/*-
 * #%L
 * Runmode
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

import org.bndly.common.runmode.Runmode;
import java.util.Arrays;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = Runmode.class)
public class RunmodeImpl implements Runmode {

	private String[] runModes;

	@Activate
	public void activate(BundleContext bundleContext) {
		String runmodesString = bundleContext.getProperty("bndly.application.runmodes");
		if (runmodesString == null) {
			runModes = new String[]{};
		} else {
			runModes = runmodesString.split(",");
		}
	}

	@Override
	public String[] getActiveRunmodes() {
		return Arrays.copyOf(runModes, runModes.length);
	}

}
