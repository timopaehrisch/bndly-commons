package org.bndly.common.app.provisioning.util;

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
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class ArtifactHelperImpl implements ArtifactHelper {
	
	private final ArtifactResolver resolver;
	private final MavenSession mavenSession;
	private final MavenProject project;
	private final ArtifactHandlerManager artifactHandlerManager;
	private final ArchiverManager archiverManager;

	public ArtifactHelperImpl(ArtifactResolver resolver, MavenSession mavenSession, MavenProject project, ArtifactHandlerManager artifactHandlerManager, ArchiverManager archiverManager) {
		if (resolver == null) {
			throw new IllegalArgumentException("resolver is not allowed to be null");
		}
		this.resolver = resolver;
		this.mavenSession = mavenSession;
		this.project = project;
		this.artifactHandlerManager = artifactHandlerManager;
		if (archiverManager == null) {
			throw new IllegalArgumentException("archiverManager is not allowed to be null");
		}
		this.archiverManager = archiverManager;
	}
	
	public ArtifactResolver getResolver() {
		return resolver;
	}

	public MavenSession getMavenSession() {
		return mavenSession;
	}

	public MavenProject getProject() {
		return project;
	}

	public ArtifactHandlerManager getArtifactHandlerManager() {
		return artifactHandlerManager;
	}

	public ArchiverManager getArchiverManager() {
		return archiverManager;
	}
	
	@Override
	public final Path resolvePathToArtifact(ArtifactDefinition artifact) throws MojoExecutionException {
		Artifact mavenArtifact = loadArtifact(artifact);
		File file = mavenArtifact.getFile();
		if (file == null) {
			throw new MojoExecutionException("could not resolve path to artifact " + artifact);
		}
		return file.toPath();
	}
	
	@Override
	public final Artifact resolveArtifact(final Artifact artifact) throws MojoExecutionException {
		// artifacts from the maven project object may have been resolved, but the file of the artifact is not yet attached.
		if (artifact.getFile() == null && artifact.isResolved()) {
			Artifact found = mavenSession.getLocalRepository().find(artifact);
			if (found != null && found.getFile() != null) {
				return found;
			}
		}
		ArtifactResolutionResult result = resolver.resolve(new ArtifactResolutionRequest()
				.setArtifact(artifact)
				.setRemoteRepositories(project.getRemoteArtifactRepositories())
				.setLocalRepository(mavenSession.getLocalRepository())
		);
		List<Exception> exceptions = result.getExceptions();
		if (exceptions != null && !exceptions.isEmpty()) {
			throw new MojoExecutionException("could not resolve artifact", exceptions.get(0));
		}
		Set<Artifact> resultArtifacts = result.getArtifacts();
		if (resultArtifacts.isEmpty()) {
			throw new MojoExecutionException("could not find artifact: " + artifactToString(artifact));
		}
		return resultArtifacts.iterator().next();
	}

	private String artifactToString(Artifact artifact) {
		return String.format("%s:%s:%s:%s:%s", artifact.getGroupId(), artifact.getArtifactId(), artifact.getType(), artifact.getClassifier(), artifact.getVersion());
	}
	
	@Override
	public final Artifact loadArtifact(ArtifactDefinition artifact) throws MojoExecutionException {
		Set<Artifact> dependencies = project.getDependencyArtifacts();
		if (dependencies != null) {
			for (Artifact dependency : dependencies) {
				if (dependencyMatchesArtifactDefinition(dependency, artifact)) {
					return resolveArtifact(dependency);
				}
			}
		}
		String groupId = artifact.getGroupId();
		String artifactId = artifact.getArtifactId();
		String version = artifact.getVersion();
		String type = artifact.getPackaging();
		String classifier = artifact.getClassifier();
		final Artifact prjArtifact = new DefaultArtifact(
				groupId,
				artifactId,
				VersionRange.createFromVersion(version),
				Artifact.SCOPE_PROVIDED,
				type,
				classifier,
				artifactHandlerManager.getArtifactHandler(type)
		);
		return resolveArtifact(prjArtifact);
	}
	
	public final boolean dependencyMatchesArtifactDefinition(Artifact dependency, ArtifactDefinition artifact) {
		return dependency.getGroupId().equals(artifact.getGroupId())
				&& dependency.getArtifactId().equals(artifact.getArtifactId())
				&& dependency.getVersion().equals(artifact.getVersion())
				&& 
				(
					(dependency.getType() == null && artifact.getPackaging() == null) 
					|| (dependency.getType() != null && dependency.getType().equals(artifact.getPackaging()))
				)
				&& 
				(
					(dependency.getClassifier() == null && artifact.getClassifier() == null)
					|| (dependency.getClassifier() != null && dependency.getClassifier().equals(artifact.getClassifier()))
				)
		;
	}
	
	public static String toString(Dependency dependency) {
		StringBuilder sb = new StringBuilder();
		sb
			.append(dependency.getGroupId())
			.append(":").append(dependency.getArtifactId())
			.append(":").append(dependency.getType());
		if (dependency.getClassifier() != null) {
			sb.append(":").append(dependency.getClassifier());
		}
		sb.append(":").append(dependency.getVersion());
		return sb.toString();
	}

	@Override
	public Path getProjectBaseDirPath() {
		return project.getBasedir().toPath();
	}

	@Override
	public UnArchiver getUnArchiver(File file) throws NoSuchArchiverException {
		return getArchiverManager().getUnArchiver(file);
	}
	
	
}
