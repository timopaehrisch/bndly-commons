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
import org.bndly.common.app.provisioning.Constants;
import java.util.List;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Mojo(
		name = "stop",
		defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST,
		requiresDependencyResolution = ResolutionScope.NONE,
		threadSafe = false
)
public class StopMojo extends AbstractProvisioningMojo {

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		try {
			try {
				FelixMain main = (FelixMain) project.getContextValue(Constants.MAVEN_CONTEXT_APPLICATION_KEY);
				if (main == null) {
					throw new MojoExecutionException("could not get application instance to stop");
				}
				try {
					main.stop();
					main.destroy();
				} catch (Exception ex) {
					throw new MojoExecutionException("could not stop application", ex);
				}
			} finally {
				List<Runnable> systemPropertiesCleanUp = (List<Runnable>) project.getContextValue(Constants.MAVEN_CONTEXT_SYSTEM_PROPERTY_CLEANUP);
				if (systemPropertiesCleanUp != null) {
					for (Runnable runnable : systemPropertiesCleanUp) {
						runnable.run();
					}
				}
			}
		} finally {
			project.setContextValue(Constants.MAVEN_CONTEXT_APPLICATION_KEY, null);
		}
	}
	
}
