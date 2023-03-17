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

import org.bndly.common.app.provisioning.Constants;
import static org.bndly.common.app.provisioning.Constants.CONTEXT_PROVISIONING_MODEL;
import org.bndly.common.app.provisioning.model.ProvisioningModel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.jar.ManifestException;
import org.codehaus.plexus.util.FileUtils;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Mojo(
		name = "package",
		defaultPhase = LifecyclePhase.PREPARE_PACKAGE,
		requiresDependencyResolution = ResolutionScope.NONE,
		threadSafe = false
)
public class PackageMojo extends AbstractProvisioningMojo {

	@Component(role = org.codehaus.plexus.archiver.Archiver.class, hint = "jar")
	private JarArchiver jarArchiver;
	
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		super.execute();
		ProvisioningModel provisioningModel = (ProvisioningModel) project.getContextValue(CONTEXT_PROVISIONING_MODEL);
		if (provisioningModel == null) {
			throw new MojoExecutionException("missing the provisioning model in the project context");
		}
		getLog().info("got the loaded provisioning model");

		if(packageLayout == PackageLayout.springboot) {
			getLog().info("adding osgi bundles, configs and resources to spring boot application package");
			Artifact projectArtifact = project.getArtifact();

			if (projectArtifact == null || !"jar".equals(projectArtifact.getType())) {
				throw new MojoExecutionException("could not find jar artifact for project");
			}
			File artifactFile = projectArtifact.getFile();
			if (artifactFile == null) {
				throw new MojoExecutionException("no file attached to project artifact");
			}


			Manifest manifest;
			try (JarFile jf = new JarFile(artifactFile)) {
				manifest = jf.getManifest();
			} catch (IOException e) {
				throw new MojoExecutionException("failed to get manifest from original artifact", e);
			}
			try {
				jarArchiver.reset();
				// skip compression, because spring boot is a diva
				// otherwise you might be facing "Unable to open nested entry" exceptions
				jarArchiver.setCompress(false);
				File tempFileForExpandedData = FileUtils.createTempFile("sbo-zip", ".tmp", artifactFile.getParentFile());
				tempFileForExpandedData.deleteOnExit();

				jarArchiver.setDestFile(tempFileForExpandedData);

				// add original contents
				// create a defensive copy, because the file might be hold open by the archiver, so we will not be able
				// to replace the file later on.
				Path copyArtifact = artifactFile.toPath().getParent().resolve(artifactFile.getName() + ".copy");
				Files.deleteIfExists(copyArtifact);
				Files.copy(artifactFile.toPath(), copyArtifact);
				jarArchiver.addArchivedFileSet(copyArtifact.toFile());

				// add java main
				Path javaMain = getTargetJavaMainFolder();
				jarArchiver.addDirectory(javaMain.toFile());

				// the bundles, configs and all other files
				Path targetAppFolder = getTargetAppFolder();
				jarArchiver.addDirectory(targetAppFolder.toFile(), Constants.SBO_INF_PREFIX + File.separator);

				// and last but not least, the provisioning file and file info, if it exists
				Path provisioningFile = Paths.get(project.getBuild().getDirectory()).resolve(Constants.FINAL_PROVISION_FILE_PARENT_FOLDER).resolve(Constants.FINAL_PROVISION_FILE);
				if (Files.isRegularFile(provisioningFile)) {
					jarArchiver.addFile(provisioningFile.toFile(), Constants.SBO_INF_PREFIX + File.separator + Constants.FINAL_PROVISION_FILE);
				}

				Path fileInfoFile = Paths.get(project.getBuild().getDirectory()).resolve(Constants.FINAL_FILE_INFO_FILE);
				if (Files.isRegularFile(fileInfoFile)) {
					jarArchiver.addFile(fileInfoFile.toFile(), Constants.SBO_INF_PREFIX + File.separator + Constants.FINAL_FILE_INFO_FILE);
				}

				try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
					manifest.write(bos);
					bos.flush();
					org.codehaus.plexus.archiver.jar.Manifest plexusManifest = new org.codehaus.plexus.archiver.jar.Manifest(new ByteArrayInputStream(bos.toByteArray()));
					jarArchiver.addConfiguredManifest(plexusManifest);
				} catch (ManifestException e) {
					throw new MojoExecutionException("failed to copy manifest", e);
				}
				jarArchiver.createArchive();
				Files.deleteIfExists(artifactFile.toPath());
				Files.move(tempFileForExpandedData.toPath(), artifactFile.toPath());

			} catch (final IOException ioe) {
				throw new MojoExecutionException("Unable to create standalone jar", ioe);
			}

			return;
		}
		
		this.getLog().info("Packaging standalone jar...");

		Path jarPath = Paths.get(project.getBuild().getDirectory()).resolve(project.getBuild().getFinalName());
		if (!jarPath.getFileName().endsWith(".jar")) {
			jarPath = Paths.get(jarPath.toString() + ".jar");
		}
		if (Files.notExists(jarPath.getParent())) {
			try {
				Files.createDirectories(jarPath.getParent());
			} catch (IOException ex) {
				throw new MojoExecutionException("could not create output directory", ex);
			}
		}
		
		try {
			jarArchiver.reset();
			jarArchiver.setDestFile(jarPath.toFile());
			try (InputStream is = Files.newInputStream(getManifestPath(), StandardOpenOption.READ)) {
				org.codehaus.plexus.archiver.jar.Manifest manifest = new org.codehaus.plexus.archiver.jar.Manifest(is);
				jarArchiver.addConfiguredManifest(manifest);
				
				// add java main
				Path javaMain = getTargetJavaMainFolder();
				jarArchiver.addDirectory(javaMain.toFile());
				
				// the bundles, configs and all other files
				Path targetAppFolder = getTargetAppFolder();
				jarArchiver.addDirectory(targetAppFolder.toFile(), Constants.APP_JAR_RESOURCES + File.separator);
				
				// and last but not least, the provisioning file and file info, if it exists
				Path provisioningFile = Paths.get(project.getBuild().getDirectory()).resolve(Constants.FINAL_PROVISION_FILE_PARENT_FOLDER).resolve(Constants.FINAL_PROVISION_FILE);
				if (Files.isRegularFile(provisioningFile)) {
					jarArchiver.addFile(provisioningFile.toFile(), Constants.FINAL_PROVISION_FILE);
				}
				
				Path fileInfoFile = Paths.get(project.getBuild().getDirectory()).resolve(Constants.FINAL_FILE_INFO_FILE);
				if (Files.isRegularFile(fileInfoFile)) {
					jarArchiver.addFile(fileInfoFile.toFile(), Constants.FINAL_FILE_INFO_FILE);
				}
				
			} catch (ManifestException ex) {
				throw new MojoExecutionException("could not add manifest to jar", ex);
			}
			jarArchiver.createArchive();

			if (Constants.PACKAGING_APP.equals(project.getPackaging())) {
				project.getArtifact().setFile(jarPath.toFile());
			} else {
				String artifactType = "jar";
				String artifactClassifier = "app";
				projectHelper.attachArtifact(project, artifactType, artifactClassifier, jarPath.toFile());
			}
		} catch (final IOException ioe) {
			throw new MojoExecutionException("Unable to create standalone jar", ioe);
		}
	}

}
