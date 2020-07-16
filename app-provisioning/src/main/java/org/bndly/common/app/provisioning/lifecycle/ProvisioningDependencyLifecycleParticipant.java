package org.bndly.common.app.provisioning.lifecycle;

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

import org.bndly.common.app.provisioning.Constants;
import org.bndly.common.app.provisioning.model.ArtifactDefinition;
import org.bndly.common.app.provisioning.model.ProvisioningModel;
import org.bndly.common.app.provisioning.model.RunMode;
import org.bndly.common.app.provisioning.model.StartLevelBundle;
import org.bndly.common.app.provisioning.util.PlexusLoggerToMavenLoggerAdapter;
import org.bndly.common.app.provisioning.util.ProvisioningModelLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

@Component( role = AbstractMavenLifecycleParticipant.class, hint = "bndly-application")
public class ProvisioningDependencyLifecycleParticipant extends AbstractMavenLifecycleParticipant {

	@Requirement
	private org.codehaus.plexus.logging.Logger logger;
	
	@Override
	public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
		super.afterProjectsRead(session);
		Log log = new PlexusLoggerToMavenLoggerAdapter(logger);
		List<MavenProject> projects = session.getProjects();
		Map<ArtifactDefinition, MavenProject> reactorProjectsByArtifactDefinition = new HashMap<>();
		for (MavenProject project : projects) {
			String gid = project.getGroupId();
			String aid = project.getArtifactId();
			String version = project.getVersion();
			ArtifactDefinition artifactDefinition = new ArtifactDefinition(gid + ":" + aid + ":" + version);
			reactorProjectsByArtifactDefinition.put(artifactDefinition, project);
		}
		for (MavenProject project : projects) {
			if (Constants.PACKAGING_APP.equals(project.getPackaging())) {
				List<Dependency> dependencies = project.getDependencies();
				Map<ArtifactDefinition, Dependency> projectDependencies = new HashMap<>();
				for (Dependency dependency : dependencies) {
					projectDependencies.put(ArtifactDefinition.fromDependency(dependency).reduceToGroupdIdArtifactIdVersion(), dependency);
				}
				List<Dependency> dependenciesToAddFromReactor = new ArrayList<>();
				try {
					// read the projects provisioning data
					ProvisioningModel provisioningModel = new ProvisioningModelLoader(log).setProject(project).loadProvisioningModel();
					for (RunMode runMode : provisioningModel.getRunModes()) {
						for (StartLevelBundle bundle : runMode.getBundles()) {
							for (ArtifactDefinition artifact : bundle.getArtifacts()) {
								ArtifactDefinition reduced = artifact.reduceToGroupdIdArtifactIdVersion();
								MavenProject provisioningDependency = reactorProjectsByArtifactDefinition.get(reduced);
								if (provisioningDependency != null) {
									if (projectDependencies.containsKey(reduced)) {
										log.debug("skipping explicit project dependency: " + artifact);
										continue;
									}
									log.debug("adding project dependency: " + artifact);
									Dependency dep = new Dependency();
									dep.setGroupId(artifact.getGroupId());
									dep.setArtifactId(artifact.getArtifactId());
									dep.setVersion(artifact.getVersion());
									dep.setClassifier(artifact.getClassifier());
									dep.setScope(Artifact.SCOPE_PROVIDED);
									dependenciesToAddFromReactor.add(dep);
								}
							}
						}
					}
					dependencies.addAll(dependenciesToAddFromReactor);
				} catch (MojoExecutionException ex) {
					throw new MavenExecutionException(ex.getMessage(), ex);
				}
			}
		}
		
	}

	
}
