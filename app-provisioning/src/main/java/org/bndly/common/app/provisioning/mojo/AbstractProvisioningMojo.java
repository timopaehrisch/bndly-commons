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
import org.bndly.common.app.provisioning.ProvisioningModelRenderer;
import org.bndly.common.app.provisioning.model.ArtifactDefinition;
import org.bndly.common.app.provisioning.model.ProvisioningModel;
import org.bndly.common.app.provisioning.model.RunMode;
import org.bndly.common.app.provisioning.model.StartLevelBundle;
import org.bndly.common.app.provisioning.util.ProvisioningModelLoader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.interpolation.PropertiesBasedValueSource;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
import java.io.OutputStream;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public abstract class AbstractProvisioningMojo extends AbstractMojo {

	@Parameter(defaultValue = "${basedir}/" + Constants.MVN_DEFAULT_PROVISIONING_FOLDER)
	protected File provisioningDirectory;

	@Parameter(alias = "systemProperties", readonly = true)
	protected Properties systemPropertiesFromConfiguration;

	@Parameter(readonly = true, defaultValue = "false")
	protected boolean saveProvisioningModel;
	
	@Parameter(readonly = true, defaultValue = "true")
	protected boolean runModeConfigurations = true;

	@Deprecated
	@Parameter(defaultValue = "false")
	private boolean war = false;

	@Parameter(defaultValue = "classic")
	protected PackageLayout packageLayout = PackageLayout.classic;
	
	@Parameter
	protected List<String> additionalMainClasspathLibraries = new ArrayList<>();

	@Component
	protected MavenProjectHelper projectHelper;
	
	private StringSearchInterpolator stringSearchInterpolator;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (war && packageLayout != PackageLayout.war) {
			getLog().warn("overwriting package layout to war. please don't use <war>true</war> anymore in your pom.xml files. use <packageLayout>war</packageLayout> instead.");
			packageLayout = PackageLayout.war;
		}
	}
	
	protected final StringSearchInterpolator getStringSearchInterpolator() {
		if (stringSearchInterpolator == null) {
			stringSearchInterpolator = new StringSearchInterpolator();
			init(stringSearchInterpolator);
		}
		return stringSearchInterpolator;
	}
	
	protected void init(StringSearchInterpolator stringSearchInterpolator) {
		stringSearchInterpolator.addValueSource(new PropertiesBasedValueSource(mavenSession.getSystemProperties()));
		stringSearchInterpolator.addValueSource(new PropertiesBasedValueSource(mavenSession.getUserProperties()));
		stringSearchInterpolator.addValueSource(new PropertiesBasedValueSource(project.getProperties()));
	}
	
	protected final void writeProvisioningModel(ProvisioningModel model, String jsonFileName) throws MojoExecutionException {
		final Path outputPath = Paths.get(project.getBuild().getDirectory()).resolve(Constants.FINAL_PROVISION_FILE_PARENT_FOLDER).resolve(jsonFileName);
		getLog().info("Writing provisioning model to '" + outputPath + "'.");

		deleteIfExists(outputPath);
		createFileAndParentDirectories(outputPath);

		try (OutputStream out = java.nio.file.Files.newOutputStream(outputPath, StandardOpenOption.WRITE)) {
			new ProvisioningModelRenderer().render(model, out);
		} catch (IOException e) {
			throw new MojoExecutionException("Failed to render provisioning model to file '" + outputPath + "'", e);
		}
	}
	
	protected final void createFileAndParentDirectories(Path file) throws MojoExecutionException {
		try {
			final Path parentFolder = file.getParent();
			if (!java.nio.file.Files.isDirectory(parentFolder)) {
				java.nio.file.Files.createDirectories(parentFolder);
			}
		} catch (IOException e) {
			throw new MojoExecutionException("Failed to ensure the parent folder of '" + file + "' exists", e);
		}

		try {
			java.nio.file.Files.createFile(file);
		} catch (IOException e) {
			throw new MojoExecutionException("Failed to create file '" + file + "'", e);
		}
	}
	
	protected final void deleteIfExists(Path path) throws MojoExecutionException {
		try {
			java.nio.file.Files.deleteIfExists(path);
		} catch (IOException e) {
			throw new MojoExecutionException("Failed to delete '" + path + "'", e);
		}
	}

	protected final ProvisioningModel loadProvisioningModel() throws MojoExecutionException {
		return new ProvisioningModelLoader(getLog())
				.setProvisioningDirectory(provisioningDirectory)
				.setStringSearchInterpolator(getStringSearchInterpolator())
				.loadProvisioningModel()
				;
	}

	protected final ProvisioningModel loadProvisioningModel(boolean interpolatePlaceholderStrings) throws MojoExecutionException {
		return new ProvisioningModelLoader(getLog())
				.setProvisioningDirectory(provisioningDirectory)
				.setStringSearchInterpolator(getStringSearchInterpolator())
				.loadProvisioningModel(interpolatePlaceholderStrings)
				;
	}
	
	protected Map<Integer, Properties> extractRunModeProperties(ProvisioningModel model) throws MojoExecutionException {
		// there will be a runmode.properties file within each start level folder.
		// this means that per start level we can have multiple different run modes.
		Map<Integer, Properties> runModePropertiesByStartLevel = new HashMap<>();
		List<RunMode> runModes = model.getRunModes();
		if (runModes != null) {
			for (RunMode runMode : runModes) {
				List<StartLevelBundle> startLevel = runMode.getBundles();
				if (startLevel != null) {
					for (StartLevelBundle startLevelBundle : startLevel) {
						Properties properties = runModePropertiesByStartLevel.get(startLevelBundle.getStartLevel());
						if (properties == null) {
							properties = new Properties();
							runModePropertiesByStartLevel.put(startLevelBundle.getStartLevel(), properties);
						}
						List<ArtifactDefinition> artifacts = startLevelBundle.getArtifacts();
						if (artifacts != null) {
							for (ArtifactDefinition artifact : artifacts) {
								File file = resolveArtifactDefinitionToArtifactFile(artifact);
								if (file == null) {
									throw new MojoExecutionException("could not get artifact file " + artifact.toString());
								}
								String artifactFileName = file.toPath().getFileName().toString();
								String existing = properties.getProperty(artifactFileName);
								if (existing == null) {
									properties.setProperty(artifactFileName, runMode.getName());
								} else {
									properties.setProperty(artifactFileName, existing + "," + runMode.getName());
								}
							}
						}
					}
				}
			}
		}
		return runModePropertiesByStartLevel;
	}
	
	protected Path getTargetAppFolder() throws MojoExecutionException {
		Path target = Paths.get(project.getBuild().getDirectory());
		Path targetAppFolder;
		if (packageLayout == PackageLayout.springboot) {
			targetAppFolder = target.resolve(Constants.TARGET_APP_FOLDER_FOR_MAVEN_BUILD).resolve(Constants.SBO_INF_PREFIX);
		} else {
			targetAppFolder = target.resolve(Constants.TARGET_APP_FOLDER_FOR_MAVEN_BUILD);
		}
		try {
			if (java.nio.file.Files.notExists(targetAppFolder)) {
				java.nio.file.Files.createDirectories(targetAppFolder);
			}
		} catch (IOException ex) {
			throw new MojoExecutionException("could not create target folder", ex);
		}
		return targetAppFolder;
	}
	
	protected final Path getTargetJavaMainFolder() throws MojoExecutionException {
		Path target = Paths.get(project.getBuild().getDirectory());
		Path targetJavaMainFolder = target.resolve(Constants.TARGET_JAVA_MAIN_FOLDER);
		try {
			if (java.nio.file.Files.notExists(targetJavaMainFolder)) {
				java.nio.file.Files.createDirectories(targetJavaMainFolder);
			}
		} catch (IOException ex) {
			throw new MojoExecutionException("could not create target folder", ex);
		}
		return targetJavaMainFolder;
	}
	
	protected final Path getManifestPath() {
		Path target = Paths.get(project.getBuild().getDirectory());
		Path manifestPath = target.resolve("META-INF").resolve("MANIFEST.MF");
		return manifestPath;
	}
	
	protected final String getMainClassName() {
		return Constants.DEFAULT_MAIN_CLASS;
	}

	/**
	 * This setter exists for unit testing only.
	 * @param systemPropertiesFromConfiguration 
	 */
	public void setSystemProperties(Properties systemPropertiesFromConfiguration) {
		this.systemPropertiesFromConfiguration = systemPropertiesFromConfiguration;
	}

	/**
	 * This setter exists for unit testing only.
	 * @param provisioningDirectory 
	 */
	public void setProvisioningDirectory(File provisioningDirectory) {
		this.provisioningDirectory = provisioningDirectory;
	}

	/**
	 * This setter exists for unit testing only.
	 * @param mavenSession 
	 */
	public void setMavenSession(MavenSession mavenSession) {
		this.mavenSession = mavenSession;
	}

	/**
	 * This setter exists for unit testing only.
	 * @param project 
	 */
	public void setProject(MavenProject project) {
		this.project = project;
	}
	
}
