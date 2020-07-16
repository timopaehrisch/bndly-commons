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
import org.bndly.common.app.InstallableBundle;
import org.bndly.common.app.MapBasedRunModeSettings;
import org.bndly.common.app.PathInstallableBundle;
import org.bndly.common.app.RunModeSettings;
import org.bndly.common.app.SharedConstants;
import org.bndly.common.app.provisioning.Constants;
import org.bndly.common.app.provisioning.appmain.MavenEnvironment;
import org.bndly.common.app.provisioning.model.ArtifactDefinition;
import org.bndly.common.app.provisioning.model.ProvisioningModel;
import org.bndly.common.app.provisioning.model.RunMode;
import org.bndly.common.app.provisioning.model.StartLevelBundle;
import org.bndly.common.app.provisioning.util.LoggerAdapter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.interpolation.PropertiesBasedValueSource;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.bndly.common.app.provisioning.mojo.PreparePackageMojo.copyResources;
import static org.bndly.common.app.provisioning.mojo.PreparePackageMojo.createConfigs;
import org.bndly.common.app.provisioning.util.ArtifactHelperImpl;
import org.bndly.common.app.provisioning.util.FileInfoTester;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.apache.maven.model.Dependency;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public abstract class AbstractStartOrRunMojo extends AbstractProvisioningMojo {

	private Properties preparedSystemProperties;

	@Parameter( alias = "runModes", property = SharedConstants.SYSTEM_PROPERTY_RUN_MODES )
	private String runModes;
	
	@Parameter( alias = "artifact", property = "artifact" )
	private String appPackageMavenCoordinates;

	@Parameter(property = "resource.overlay.directory")
	private File resourceOverlayDirectory;
	private final RunModeSettings noModeRunModeSettings = new RunModeSettings() {
		@Override
		public boolean isActive(String bundleFileName) {
			return !runModeConfigurations;
		}
	};
	
	@Override
	public final void execute() throws MojoExecutionException, MojoFailureException {
		getLog().info("start application now...");

		boolean skipCopyingResource = false;
		final Path workingDir = getTargetAppFolder();
		if (appPackageMavenCoordinates == null) {
			// check the project for a bndly-application packaging dependency and use the first one
			List<Dependency> deps = project.getDependencies();
			if (deps != null) {
				for (Dependency dep : deps) {
					if (Constants.PACKAGING_APP.equals(dep.getType())) {
						appPackageMavenCoordinates = ArtifactHelperImpl.toString(dep);
						getLog().info("picking " + appPackageMavenCoordinates + " as the provided application to start");
						break;
					}
				}
			}
		}
		FileInfoTester fileInfoTester;
		if (appPackageMavenCoordinates != null) {
			getLog().debug("Found nonempty value in appPackageMavenCoordinates parameter - attempting to retrieve and launch '" + appPackageMavenCoordinates + "'");
			final Path pathToResolvedAppArchive = getArtifactHelper().resolvePathToArtifact(new ArtifactDefinition(appPackageMavenCoordinates));
			final Path unpackedAppFolder = Paths.get(project.getBuild().getDirectory()).resolve("app-unpacked");
			extractArchive(pathToResolvedAppArchive, unpackedAppFolder);

			fileInfoTester = new FileInfoTester(loadFileInfo(unpackedAppFolder), unpackedAppFolder.resolve(Constants.APP_JAR_RESOURCES));
			Path provisioningFile = unpackedAppFolder.resolve(Constants.FINAL_PROVISION_FILE);
			if (!Files.isRegularFile(provisioningFile)) {
				throw new MojoExecutionException("could not find provisioning file at " + provisioningFile.toString() + ". maybe you did not package it?");
			}
			getLog().info("Overriding provisioning directory to '" + provisioningDirectory );
			provisioningDirectory = provisioningFile.toFile();
			
			// do not attempt to copy resources defined in the provisioning model since the copy operation has
			// already been performed when the package was created and the original files are not included
			skipCopyingResource = true;
			// move the extractet files to the workingDir
			moveExtractedAppToWorkingDir(workingDir, unpackedAppFolder, fileInfoTester);
		} else {
			if (!Constants.PACKAGING_APP.equals(project.getPackaging())) {
				throw new MojoExecutionException("can not start bndly-application, because the project is not of the right packaging or no dependency to run is defined.");
			}
			getLog().info("Using application home '" + workingDir + "'");
		}

		copyOverlayResources(workingDir);

		List<Runnable> systemPropertiesCleanUp = setPropertiesAndPrepareCleanup(workingDir);

		// prepare the config
		Map<String, Object> config = new HashMap<>();
		config.put(Constants.CONFIG_PROPERTY_DO_UNPACK_APP_JAR, Boolean.FALSE);
		config.put(Constants.CONFIG_PROPERTY_HOME_FOLDER, workingDir.toAbsolutePath().toString());
		
		ProvisioningModel provisioningModel = loadProvisioningModel();
		
		Set<String> activeRunModes;
		RunModeSettings runModeSettings;
		if (runModes != null && !runModes.isEmpty()) {
			getLog().info("using run modes: " + runModes);
			String[] split = runModes.split(",");
			activeRunModes = new HashSet<>();
			for (String runMode : split) {
				activeRunModes.add(runMode);
			}
			config.put(SharedConstants.SYSTEM_PROPERTY_RUN_MODES, runModes);
			final Map<Integer, Properties> runModePropertiesByStartLevel = extractRunModeProperties(provisioningModel);
			runModeSettings = new MapBasedRunModeSettings(runModePropertiesByStartLevel, activeRunModes);
		} else {
			getLog().info("no active run modes");
			activeRunModes = Collections.EMPTY_SET;
			runModeSettings = noModeRunModeSettings;
		}
		FelixMain main = new FelixMain(new MavenEnvironment(project), new LoggerAdapter(getLog()), config);
		
		if (saveProvisioningModel) {
			writeProvisioningModel(provisioningModel, Constants.FINAL_PROVISION_FILE_APP_START);
		}
		Map<Integer, List<InstallableBundle>> bundlesByStartLevel = buildBundlePathsForStartLevels(provisioningModel);
		main.setBundlesByStartLevel(bundlesByStartLevel);
		main.setActiveRunModes(activeRunModes);
		main.setRunModeSettings(runModeSettings);

		createConfigs(workingDir, provisioningModel, getLog(), null, runModeConfigurations);

		if (!skipCopyingResource) {
			copyResources(getArtifactHelper(), workingDir, provisioningModel, null);
		}
		
		project.setContextValue(Constants.MAVEN_CONTEXT_APPLICATION_KEY, main);
		project.setContextValue(Constants.MAVEN_CONTEXT_SYSTEM_PROPERTY_CLEANUP, systemPropertiesCleanUp);
		doStartOrRun(main);
	}

	/**
	 * Prepares the system properties and returns a list of runnables to clean up the system properties after execution.
	 * @param workingDir the current directory of application resources
	 * @return a list of runnables to clean up the system properties in order to revert the changes
	 */
	private List<Runnable> setPropertiesAndPrepareCleanup(final Path workingDir) {
		List<Runnable> systemPropertiesCleanUp = new ArrayList<>();
		final Properties originalSystemProperties = System.getProperties();
		// Thomas: systemPropertiesFromConfiguration should be expanded for
		// "home" "felix.fileinstall.dir" and "pathToLicenseFile" if we just run the application and if these values are not yet explicitly defined
		// "home" should become "FelixMain.homeFolder"
		preparedSystemProperties = new Properties(systemPropertiesFromConfiguration);
		if (!preparedSystemProperties.stringPropertyNames().contains(SharedConstants.CONFIG_PROPERTY_HOME_FOLDER)) {
			preparedSystemProperties.setProperty(SharedConstants.CONFIG_PROPERTY_HOME_FOLDER, workingDir.toString() + File.separator);
		}
		if (!preparedSystemProperties.stringPropertyNames().contains(SharedConstants.FELIX_FILEINSTALL_DIR)) {
			Path wars = Paths.get(preparedSystemProperties.getProperty(SharedConstants.CONFIG_PROPERTY_HOME_FOLDER)).resolve(Paths.get(Constants.PATH_WARS));
			preparedSystemProperties.setProperty(SharedConstants.FELIX_FILEINSTALL_DIR, wars.toString() + File.separator);
		}
		if (!preparedSystemProperties.stringPropertyNames().contains(SharedConstants.SYSTEM_PROPERTY_CONFIG_DIR)) {
			Path confApp = Paths.get(preparedSystemProperties.getProperty(SharedConstants.CONFIG_PROPERTY_HOME_FOLDER)).resolve(Paths.get(Constants.PATH_CONFIGS));
			preparedSystemProperties.setProperty(SharedConstants.SYSTEM_PROPERTY_CONFIG_DIR, confApp.toString() + File.separator);
		}
		if (!preparedSystemProperties.stringPropertyNames().contains(SharedConstants.SYSTEM_PROPERTY_RUN_MODES) && runModes != null && !runModes.isEmpty()) {
			preparedSystemProperties.setProperty(SharedConstants.SYSTEM_PROPERTY_RUN_MODES, runModes);
		}
		for (final String key : preparedSystemProperties.stringPropertyNames()) {
			final String value = preparedSystemProperties.getProperty(key);
			final boolean hasOriginal = originalSystemProperties.containsKey(key);
			final String originalValue = (String) originalSystemProperties.get(key);
			originalSystemProperties.setProperty(key, value);
			getLog().info("setting system property " + key + "=" + value);
			systemPropertiesCleanUp.add(new Runnable() {
				@Override
				public void run() {
					if (hasOriginal) {
						getLog().info("restoring original system property " + key + "=" + originalValue);
						originalSystemProperties.setProperty(key, originalValue);
					} else {
						getLog().info("removing system property " + key);
						originalSystemProperties.remove(key);
						System.clearProperty(key); // .remove will remove it only from the "originalSystemProperties" object. System.getProperty(key) will still return the removed value.
					}
				}
			});
		}
		return systemPropertiesCleanUp;
	}

	private void copyOverlayResources(final Path workingDir) throws MojoExecutionException {
		if (resourceOverlayDirectory != null) {
			if (resourceOverlayDirectory.isDirectory()) {
				getLog().info("Copy resource overlay from '" + resourceOverlayDirectory + "'");
				try {
					// Copy contents of the overlay dir (not the source dir itself) to the working dir, preserving subdirectories
					FileUtils.copyDirectoryStructure(resourceOverlayDirectory, workingDir.toFile());
				} catch (IOException e) {
					throw new MojoExecutionException("Failed to copy contents of '" + resourceOverlayDirectory + "' to '" + workingDir + "'", e);
				}
			} else {
				throw new MojoExecutionException("Configured resource overlay directory is not a valid folder: '" + resourceOverlayDirectory + "'");
			}
		}
	}

	private Properties loadFileInfo(final Path unpackedAppFolder) throws MojoExecutionException {
		Properties fileInfo;
		Path fileInfoFile = unpackedAppFolder.resolve(Constants.FINAL_FILE_INFO_FILE);
		if (!Files.isRegularFile(fileInfoFile)) {
			throw new MojoExecutionException("could not find file info file at " + fileInfoFile.toString() + ". maybe you did not package it?");
		}
		fileInfo = new Properties();
		try (InputStream is = Files.newInputStream(fileInfoFile, StandardOpenOption.READ)) {
			try (Reader r = new InputStreamReader(is, "UTF-8")) {
				fileInfo.load(r);
			}
		} catch (IOException e) {
			throw new MojoExecutionException("could not read file info", e);
		}
		return fileInfo;
	}

	private void extractArchive(Path archivePath, Path outputDirectory) throws MojoExecutionException {
		final UnArchiver unArchiver;
		try {
			unArchiver = getArtifactHelper().getUnArchiver(archivePath.toFile());
		} catch (NoSuchArchiverException e) {
			throw new MojoExecutionException("Failed to obtain unarchiver for '" + archivePath + "'", e);
		}
		try {
			Files.createDirectories(outputDirectory);
		} catch (IOException e) {
			throw new MojoExecutionException("could not create output directory while extracting archive", e);
		}

		unArchiver.setSourceFile(archivePath.toFile());
		unArchiver.setDestDirectory(outputDirectory.toFile());
		unArchiver.extract();
	}

	@Override
	protected Path getTargetAppFolder() throws MojoExecutionException {
		Path target = Paths.get(project.getBuild().getDirectory());
		Path targetAppFolder = target.resolve(Constants.TARGET_APP_FOLDER_FOR_MAVEN_START);
		try {
			if (java.nio.file.Files.notExists(targetAppFolder)) {
				java.nio.file.Files.createDirectories(targetAppFolder);
			}
		} catch (IOException ex) {
			throw new MojoExecutionException("could not create target folder", ex);
		}
		return targetAppFolder;
	}

	@Override
	protected void init(StringSearchInterpolator stringSearchInterpolator) {
		if (preparedSystemProperties != null) {
			stringSearchInterpolator.addValueSource(new PropertiesBasedValueSource(preparedSystemProperties));
		}
		super.init(stringSearchInterpolator);
	}

	private Map<Integer, List<InstallableBundle>> buildBundlePathsForStartLevels(ProvisioningModel provisioningModel) throws MojoExecutionException {
		Map<Integer, List<InstallableBundle>> bundlesByStartLevel = new HashMap<>();
		for (RunMode runMode : provisioningModel.getRunModes()) {
			for (StartLevelBundle startLevel : runMode.getBundles()) {
				for (ArtifactDefinition artifact : startLevel.getArtifacts()) {
					List<InstallableBundle> list = bundlesByStartLevel.get(startLevel.getStartLevel());
					if (list == null) {
						list = new ArrayList<>();
						bundlesByStartLevel.put(startLevel.getStartLevel(), list);
					}
					Path path = getArtifactHelper().resolvePathToArtifact(artifact);
					list.add(new PathInstallableBundle(path));
				}
			}
		}
		return bundlesByStartLevel;
	}

	protected abstract void doStartOrRun(FelixMain main) throws MojoExecutionException;

	private void moveExtractedAppToWorkingDir(final Path workingDir, Path unpackedJarFolder, final FileInfoTester fileInfoTester) throws MojoExecutionException {
		final Path extractedAppFolder = unpackedJarFolder.resolve(Constants.APP_JAR_RESOURCES);
		try {
			Files.walkFileTree(extractedAppFolder, new FileVisitor<Path>() {
				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					if (fileInfoTester.isResourceDirectory(dir)) {
						Path folder = workingDir.resolve(extractedAppFolder.relativize(dir));
						Files.createDirectories(folder);
					}
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					if (fileInfoTester.isBundle(file) || fileInfoTester.isResource(file)) {
						Path f = workingDir.resolve(extractedAppFolder.relativize(file));
						Path parentFolder = f.getParent();
						Files.createDirectories(parentFolder);
						Files.deleteIfExists(f);
						Files.copy(file, f);
					} else if (fileInfoTester.isConfig(file)) {
						// skip
					}
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
					return FileVisitResult.TERMINATE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			throw new MojoExecutionException("could not move extracted app to working directory", e);
		}
	}
}
