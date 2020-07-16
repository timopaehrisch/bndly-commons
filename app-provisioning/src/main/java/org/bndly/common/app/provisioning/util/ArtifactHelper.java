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
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public interface ArtifactHelper {

	public Path resolvePathToArtifact(ArtifactDefinition artifactDefinition) throws MojoExecutionException;

	public Artifact loadArtifact(ArtifactDefinition appMain) throws MojoExecutionException;

	public UnArchiver getUnArchiver(File toFile) throws NoSuchArchiverException;
	
	public Path getProjectBaseDirPath();

	public Artifact resolveArtifact(Artifact mavenArtifact) throws MojoExecutionException;
	
}
