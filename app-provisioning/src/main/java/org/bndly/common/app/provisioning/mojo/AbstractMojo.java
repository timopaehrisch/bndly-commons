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

import org.bndly.common.app.provisioning.model.ArtifactDefinition;
import org.bndly.common.app.provisioning.util.ArtifactHelper;
import org.bndly.common.app.provisioning.util.ArtifactHelperImpl;
import java.io.File;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.manager.ArchiverManager;

public abstract class AbstractMojo extends org.apache.maven.plugin.AbstractMojo {

	@Parameter(property = "project", readonly = true, required = true)
	protected MavenProject project;

	@Component
	private ArtifactResolver resolver;
	
	@Parameter(property = "session", readonly = true, required = true)
	protected MavenSession mavenSession;

	@Component
	private ArtifactHandlerManager artifactHandlerManager;

	@Component
	private ArchiverManager archiverManager;

	private ArtifactHelper artifactHelper;

	protected final ArtifactHelper getArtifactHelper() {
		if (artifactHelper == null) {
			artifactHelper = new ArtifactHelperImpl(resolver, mavenSession, project, artifactHandlerManager, archiverManager);
		}
		return artifactHelper;
	}
	
	
	protected final File resolveArtifactDefinitionToArtifactFile(ArtifactDefinition artifact) throws MojoExecutionException {
		Artifact mavenArtifact = getArtifactHelper().loadArtifact(artifact);
		// copy artifact file to target folder
		if (mavenArtifact == null) {
			throw new MojoExecutionException("could not load artifact " + artifact.toString());
		}
		File file = mavenArtifact.getFile();
		if (file == null) {
			mavenArtifact = getArtifactHelper().resolveArtifact(mavenArtifact);
			if (mavenArtifact != null) {
				file = mavenArtifact.getFile();
			}
		}
		return file;
	}

	/**
	 * This setter exists for unit testing only.
	 * @param artifactHelper 
	 */
	public void setArtifactHelper(ArtifactHelper artifactHelper) {
		this.artifactHelper = artifactHelper;
	}

	/**
	 * This setter exists for unit testing only.
	 * @param resolver 
	 */
	public void setResolver(ArtifactResolver resolver) {
		this.resolver = resolver;
	}

	/**
	 * This setter exists for unit testing only.
	 * @return 
	 */
	public ArchiverManager getArchiverManager() {
		return archiverManager;
	}
}
