package org.bndly.common.app.provisioning.mojo;

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

import org.bndly.common.app.FelixMain;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Mojo(
		name = "start",
		defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST,
		requiresDependencyResolution = ResolutionScope.NONE,
		threadSafe = false
)
public class StartMojo extends AbstractStartOrRunMojo {

	@Override
	protected void doStartOrRun(FelixMain main) throws MojoExecutionException {
		try {
			main.setSkipShutdownHook(true);
			main.start();
		} catch (Exception ex) {
			throw new MojoExecutionException("could not start application", ex);
		}
	}

}
