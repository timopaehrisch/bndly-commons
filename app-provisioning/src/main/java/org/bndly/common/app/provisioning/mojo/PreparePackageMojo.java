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

import org.bndly.common.app.provisioning.model.ArtifactDefinition;
import org.bndly.common.app.provisioning.model.Config;
import org.bndly.common.app.provisioning.model.ProvisioningModel;
import org.bndly.common.app.provisioning.model.ResourceDefinition;
import org.bndly.common.app.provisioning.model.RunMode;
import org.bndly.common.app.provisioning.model.StartLevelBundle;
import org.bndly.common.app.provisioning.util.ArtifactHelper;
import org.bndly.common.osgi.config.spi.PrefixHandler;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.components.io.fileselectors.FileSelector;
import org.codehaus.plexus.components.io.fileselectors.IncludeExcludeFileSelector;
import org.codehaus.plexus.util.DirectoryScanner;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Set;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.archiver.ArchiverException;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Mojo(
		name = "prepare-package",
		defaultPhase = LifecyclePhase.PREPARE_PACKAGE,
		requiresDependencyResolution = ResolutionScope.TEST,
		threadSafe = false
)
public class PreparePackageMojo extends AbstractProvisioningMojo {

	private static final String SECURE_CONFIG_HINT = "@SECURE";

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		super.execute();
		getLog().info("loading provisioning model");
		ProvisioningModel model = loadProvisioningModel();
		getLog().info("did load provisioning model");
		project.setContextValue(CONTEXT_PROVISIONING_MODEL, model);
		
		Path targetAppFolder = getTargetAppFolder();
		unpackAppMain(model);
		unpackFelixFramework();
		Properties fileInfoProperties = new Properties();
		copyBundles(targetAppFolder, model, fileInfoProperties);
		if (runModeConfigurations) {
			createRunModeProperties(targetAppFolder, model);
		}
		createConfigs(targetAppFolder, model, getLog(), fileInfoProperties, runModeConfigurations);
		copyResources(getArtifactHelper(), targetAppFolder, model, fileInfoProperties);
		createManifest();

		if (saveProvisioningModel) {
			writeProvisioningModel(loadProvisioningModel(false), Constants.FINAL_PROVISION_FILE);
		}
		
		writeFileInfoProperties(fileInfoProperties);
	}

	private void createManifest() throws MojoExecutionException {
		Manifest manifest = new Manifest();
		Attributes attributes = manifest.getMainAttributes();
		attributes.putValue(Constants.MANIFEST_VERSION, "1.0");
		attributes.putValue(Constants.MANIFEST_APPLICATION, project.getArtifactId());
		attributes.putValue(Constants.MANIFEST_MAIN_CLASS, getMainClassName());

		Path manifestPath = getManifestPath();
		deleteIfExists(manifestPath);
		createFileAndParentDirectories(manifestPath);

		try (OutputStream out = Files.newOutputStream(manifestPath, StandardOpenOption.WRITE)) {
			manifest.write(out);
			out.flush();
		} catch (IOException ex) {
			throw new MojoExecutionException("could not write manifest", ex);
		}
	}
	
	static void copyResources(ArtifactHelper artifactHelper, final Path targetAppFolder, ProvisioningModel model, final Properties fileInfoProperties) throws MojoExecutionException {
		for (RunMode runMode : model.getRunModes()) {
			for (ResourceDefinition resourceDefinition : runMode.getResources()) {
				Path targetPath;
				String target = resourceDefinition.getTarget(); // defaults to targetAppFolder
				if (target == null) {
					targetPath = targetAppFolder;
				} else {
					if (!Paths.get(target).isAbsolute()) {
						target = targetAppFolder.resolve(Paths.get(target)).toString();
					}
					targetPath = Paths.get(target);
				}
				if (Files.notExists(targetPath)) {
					try {
						Files.createDirectories(targetPath);
						if (fileInfoProperties != null) {
							String key = targetAppFolder.relativize(targetPath).normalize().toString();
							fileInfoProperties.setProperty(key, Constants.FILE_TYPE_RESOURCE_DIRECTORY);
						}
					} catch (IOException ex) {
						throw new MojoExecutionException("could not create folder " + targetPath);
					}
				}
				List<ArtifactDefinition> artifacts = resourceDefinition.getArtifacts();
				if (!artifacts.isEmpty()) {
					// copy artifacts to targetPath
					for (ArtifactDefinition artifact : artifacts) {
						Path pathToArtifact = artifactHelper.resolvePathToArtifact(artifact);
						if (resourceDefinition.isUnpack()) {
							try {
								if (Files.notExists(targetPath)) {
									Files.createDirectory(targetPath);
								}
							} catch (IOException ex) {
								throw new MojoExecutionException("could not create directory for unpacked artifact " + artifact.toString(), ex);
							}
							try {
								UnArchiver unarchiver = artifactHelper.getUnArchiver(pathToArtifact.toFile());
								unarchiver.setDestDirectory(targetPath.toFile());
								unarchiver.setSourceFile(pathToArtifact.toFile());
								IncludeExcludeFileSelector selector = new IncludeExcludeFileSelector();
								List<String> includes = resourceDefinition.getIncludes();
								boolean requiresSelector = false;
								if (includes != null && !includes.isEmpty()) {
									selector.setIncludes(includes.toArray(new String[includes.size()]));
									requiresSelector = true;
								}
								List<String> excludes = resourceDefinition.getExcludes();
								if (excludes != null && !excludes.isEmpty()) {
									selector.setExcludes(excludes.toArray(new String[excludes.size()]));
									requiresSelector = true;
								}
								if (requiresSelector) {
									unarchiver.setFileSelectors(new FileSelector[]{selector});
								}
								unarchiver.extract();
								// how to keep track of extracted files? walk over the file tree and everything, that is not in the properties yet
								if (fileInfoProperties != null) {
									Files.walkFileTree(unarchiver.getDestDirectory().toPath(), new FileVisitor<Path>() {
										@Override
										public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
											return FileVisitResult.CONTINUE;
										}

										@Override
										public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
											String key = targetAppFolder.relativize(file.normalize()).toString();
											if (fileInfoProperties.getProperty(key) == null) {
												fileInfoProperties.setProperty(key, Constants.FILE_TYPE_RESOURCE);
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
								}
							} catch (IOException | NoSuchArchiverException ex) {
								throw new MojoExecutionException("no unarchiver for " + artifact.toString(), ex);
							}
						} else {
							Path targetFilePath = targetPath.resolve(pathToArtifact.getFileName());
							try {
								Files.deleteIfExists(targetFilePath);
							} catch (IOException ex) {
								throw new MojoExecutionException("could not delete existing resource file at " + targetFilePath, ex);
							}
							try {
								Files.copy(pathToArtifact, targetFilePath);
								if (fileInfoProperties != null) {
									fileInfoProperties.setProperty(targetAppFolder.relativize(targetFilePath).toString(), Constants.FILE_TYPE_RESOURCE);
								}
							} catch (IOException ex) {
								throw new MojoExecutionException("could not copy resource to " + targetFilePath, ex);
							}
						}
					}
				} else {
					String source = resourceDefinition.getSource(); // default to src/main/resources
					if (source == null) {
						source = artifactHelper.getProjectBaseDirPath().resolve("src").resolve("main").resolve("resources").toString();
					} else {
						if (!Paths.get(source).isAbsolute()) {
							source = artifactHelper.getProjectBaseDirPath().resolve(Paths.get(source)).toString();
						}
					}
					Path sourcePath = Paths.get(source);
					DirectoryScanner directoryScanner = new DirectoryScanner();
					directoryScanner.setBasedir(source);
					List<String> includes = resourceDefinition.getIncludes();
					if (includes != null && !includes.isEmpty()) {
						directoryScanner.setIncludes(includes.toArray(new String[includes.size()]));
					}
					List<String> excludes = resourceDefinition.getExcludes();
					if (excludes != null && !excludes.isEmpty()) {
						directoryScanner.setExcludes(excludes.toArray(new String[excludes.size()]));
					}
					directoryScanner.addDefaultExcludes();
					directoryScanner.scan();
					for (String includedFile : directoryScanner.getIncludedFiles()) {
						Path tmp = Paths.get(includedFile);
						Path includedFilePath = sourcePath.resolve(tmp);
						Path targetFilePath = targetPath.resolve(tmp);
						try {
							Files.deleteIfExists(targetFilePath);
						} catch (IOException ex) {
							throw new MojoExecutionException("could not delete existing resource file at " + targetFilePath, ex);
						}
						Path parent = targetFilePath.getParent();
						if (Files.notExists(parent)) {
							try {
								Files.createDirectories(parent);
							} catch (IOException ex) {
								throw new MojoExecutionException("could not create parent directory " + parent, ex);
							}
						}
						try {
							Files.copy(includedFilePath, targetFilePath);
							if (fileInfoProperties != null) {
								fileInfoProperties.setProperty(targetAppFolder.relativize(targetFilePath).toString(), Constants.FILE_TYPE_RESOURCE);
							}
						} catch (IOException ex) {
							throw new MojoExecutionException("could not copy resource to " + targetFilePath, ex);
						}
					}
				}
			}
		}
	}
	
	static void createConfigs(final Path targetAppFolder, ProvisioningModel model, Log log, Properties fileInfoProperties, boolean runModeConfigurations) throws MojoExecutionException {
		ServiceLoader<PrefixHandler> loader = ServiceLoader.load(PrefixHandler.class);
		loader.reload();
		Iterator<PrefixHandler> handlers = loader.iterator();
		Map<String, PrefixHandler> handlersByPrefix = new HashMap<>();
		while (handlers.hasNext()) {
			PrefixHandler next = handlers.next();
			handlersByPrefix.put(next.getPrefix(), next);
		}
		Set<String> configNames = new HashSet<>();
		Path configFolderTmp = targetAppFolder.resolve("conf").resolve("app");
		for (RunMode runMode : model.getRunModes()) {
			Path configFolder = !runModeConfigurations ? configFolderTmp : configFolderTmp.resolve("runmode-" + runMode.getName());
			for (Config config : runMode.getConfigs()) {
				String configName = config.getName();
				if (configNames.contains(configName)) {
					log.warn("config " + configName + " appears multiple times");
				}
				configNames.add(configName);
				// create a config file
				// integrate with the config providers
				String configHint = (String) config.getProperties().get(SECURE_CONFIG_HINT);
				if (configHint == null) {
					configHint = "";
				}
				PrefixHandler prefixHandler = handlersByPrefix.get(configHint);
				if (prefixHandler == null && !configHint.isEmpty()) {
					throw new MojoExecutionException("could not find config prefix handler " + configHint);
				}
				Properties properties = new Properties();
				boolean isSecured = false;
				for (Map.Entry<String, Object> entry : config.getProperties().entrySet()) {
					String key = entry.getKey();
					if (SECURE_CONFIG_HINT.equals(key)) {
						isSecured = true;
						continue;
					}
					Object value = entry.getValue();
					// serialize properties
					String valueString = convertConfigValueToString(value);
					// manual no-op implementation
					String valuePlainOrEncrypted = prefixHandler == null ? valueString : "@{" + configHint + ":" + prefixHandler.set(valueString) + "}";
					properties.setProperty(key, valuePlainOrEncrypted);
				}
				if (Files.notExists(configFolder)) {
					try {
						Files.createDirectories(configFolder);
					} catch (IOException ex) {
						throw new MojoExecutionException("could not create config folder", ex);
					}
				}
				final String extension = isSecured ? ".scfg" : ".cfg";
				Path configFile = configFolder.resolve(configName + extension);
				try {
					Files.deleteIfExists(configFile);
				} catch (IOException ex) {
					throw new MojoExecutionException("could not delete existing config file", ex);
				}
				try {
					Files.createFile(configFile);
				} catch (IOException ex) {
					throw new MojoExecutionException("could not create empty config file", ex);
				}
				try (OutputStream out = Files.newOutputStream(configFile, StandardOpenOption.WRITE)) {
					properties.store(out, null);
					out.flush();
					if (fileInfoProperties != null) {
						fileInfoProperties.setProperty(targetAppFolder.relativize(configFile).toString(), Constants.FILE_TYPE_CONFIG);
					}
				} catch (IOException ex) {
					throw new MojoExecutionException("could not write config file", ex);
				}
			}
		}
	}
	
	private void copyBundles(final Path targetAppFolder, ProvisioningModel model, Properties fileInfoProperties) throws MojoExecutionException {
		Path bundleFolder = targetAppFolder.resolve(Constants.AUTO_DEPLOY_PATH);
		List<RunMode> runModes = model.getRunModes();
		for (RunMode runMode : runModes) {
			List<StartLevelBundle> bundles = runMode.getBundles();
			for (StartLevelBundle bundle : bundles) {
				Path startLevelTargetFolder = bundleFolder.resolve(Integer.toString(bundle.getStartLevel()));
				if (Files.notExists(startLevelTargetFolder)) {
					try {
						Files.createDirectories(startLevelTargetFolder);
					} catch (IOException ex) {
						throw new MojoExecutionException("could not create target folder for startlevel", ex);
					}
				}
				List<ArtifactDefinition> artifacts = bundle.getArtifacts();
				for (ArtifactDefinition artifact : artifacts) {
					if (isAppMainArtifact(artifact)) {
						continue;
					}
					File file = resolveArtifactDefinitionToArtifactFile(artifact);
					if (file == null) {
						throw new MojoExecutionException("could not get artifact file " + artifact.toString());
					}
					Path ap = file.toPath();
					try {
						Path targetPathForArtifact = startLevelTargetFolder.resolve(ap.getFileName());
						Files.deleteIfExists(targetPathForArtifact);
						Files.copy(ap, targetPathForArtifact);
						if (fileInfoProperties != null) {
							fileInfoProperties.setProperty(targetAppFolder.relativize(targetPathForArtifact).toString(), Constants.FILE_TYPE_BUNDLE);
						}
					} catch (IOException ex) {
						throw new MojoExecutionException("could not copy artifact " + artifact.toString(), ex);
					}
				}
			}
		}
	}

	static String convertConfigValueToString(Object value) throws MojoExecutionException {
		if (value == null) {
			return "";
		} else if (BigDecimal.class.isInstance(value)) {
			return value.toString();
		} else if (String.class.isInstance(value)) {
			return (String) value;
		} else if (Boolean.class.isInstance(value)) {
			return Boolean.toString((boolean) value);
		} else if (value.getClass().isArray()) {
			Object[] arr = (Object[]) value;
			StringBuilder sb = null;
			for (Object object : arr) {
				if (sb == null) {
					sb = new StringBuilder();
				} else {
					sb.append(",");
				}
				sb.append(convertConfigValueToString(object));
			}
			return sb == null ? "" : sb.toString();
		} else {
			throw new MojoExecutionException("could not convert value to config string value: " + value);
		}
	}
	
	private void createRunModeProperties(Path targetAppFolder, ProvisioningModel model) throws MojoExecutionException {
		Map<Integer, Properties> runModePropertiesByStartLevel = extractRunModeProperties(model);
		
		Path bundleFolder = targetAppFolder.resolve(Constants.AUTO_DEPLOY_PATH);
		for (Map.Entry<Integer, Properties> entry : runModePropertiesByStartLevel.entrySet()) {
			Integer startLevel = entry.getKey();
			Properties runmodeProperties = entry.getValue();
			Path startLevelTargetFolder = bundleFolder.resolve(Integer.toString(startLevel));
			if (Files.notExists(startLevelTargetFolder)) {
				try {
					Files.createDirectories(startLevelTargetFolder);
				} catch (IOException ex) {
					throw new MojoExecutionException("could not create target folder for startlevel", ex);
				}
			}
			Path runModePropertiesFilePath = startLevelTargetFolder.resolve(Constants.RUN_MODE_PROPERTIES_FILE);
			try {
				Files.deleteIfExists(runModePropertiesFilePath);
			} catch (IOException ex) {
				throw new MojoExecutionException("could not delete existing run mode properties file", ex);
			}
			try {
				Files.createFile(runModePropertiesFilePath);
			} catch (IOException ex) {
				throw new MojoExecutionException("could not create empty run mode properties file", ex);
			}
			try (Writer writer = Files.newBufferedWriter(runModePropertiesFilePath, Charset.forName("UTF-8"), StandardOpenOption.WRITE)) {
				runmodeProperties.store(writer, "Generated run mode properties file.");
				writer.flush();
			} catch (IOException ex) {
				throw new MojoExecutionException("could not write to run mode properties file", ex);
			}
		}
	}

	private void unpackFelixFramework() throws MojoExecutionException {
		Path destination;
		if (packageLayout == PackageLayout.war) {
			destination = getTargetJavaMainFolder().resolve("WEB-INF").resolve("classes");
		} else if (packageLayout == PackageLayout.classic) {
			destination = getTargetJavaMainFolder();
		} else {
			getLog().info("skipping unpacking of Apache Felix for spring boot application layout");
			return;
		}
		getLog().info("unpacking felix to " + destination);

		Artifact artifact = getFelixFrameworkArtifact();
		if (artifact == null) {
			throw new MojoExecutionException("could not load felix framework artifact");
		}
		File file = artifact.getFile();
		if (file == null) {
			throw new MojoExecutionException("artifact " + artifact.getArtifactId() + " has no file attached");
		}
		try {
			final UnArchiver unArchiver = getArtifactHelper().getUnArchiver(file);

			assertUnpackDestinationFolderExists(destination);
			unArchiver.setSourceFile(file);
			unArchiver.setDestDirectory(destination.toFile());
			IncludeExcludeFileSelector includeExcludeFileSelector = new IncludeExcludeFileSelector();
			includeExcludeFileSelector.setIncludes(new String[]{
				"**/*"
			});
			includeExcludeFileSelector.setExcludes(new String[]{
				"META-INF/MANIFEST.MF",
				"META-INF/maven",
				"META-INF/maven/*",
				"META-INF/maven/**/*"
			});
			unArchiver.setFileSelectors(new FileSelector[]{
				includeExcludeFileSelector
			});
			unArchiver.extract();
		} catch (NoSuchArchiverException e) {
			throw new MojoExecutionException("failed to unpack app main");
		}
	}

	private void unpackAppMain(ProvisioningModel model) throws MojoExecutionException {
		ArtifactDefinition appMain = getAppMainArtifact(model);
		if (appMain == null) {
			throw new MojoExecutionException("could not find app main artifact");
		}
		getLog().debug("app main artifact: " + appMain.toString());
		Artifact appMainMavenArtifact = getArtifactHelper().loadArtifact(appMain);
		// unpack to app folder
		if (appMainMavenArtifact == null) {
			throw new MojoExecutionException("could not load artifact of " + appMain.toString());
		}
		extractMainClasspathLibrary(appMain);
		
		for (String additionalMainClasspathLibrary : additionalMainClasspathLibraries) {
			ArtifactDefinition additionalLibrary = new ArtifactDefinition(additionalMainClasspathLibrary);
			getLog().info("extracting additional main classpath library " + additionalMainClasspathLibrary);
			extractMainClasspathLibrary(additionalLibrary);
		}
		
	}

	private void extractMainClasspathLibrary(ArtifactDefinition artifactDefinition) throws MojoExecutionException {
		Path destination;
		if (packageLayout == PackageLayout.war) {
			destination = getTargetJavaMainFolder().resolve("WEB-INF").resolve("classes");
		} else if (packageLayout == PackageLayout.classic) {
			destination = getTargetJavaMainFolder();
		} else {
			getLog().info("skipping unpacking of FelixMain for spring boot application layout");
			return;
		}

		Artifact additionalArtifact = getArtifactHelper().loadArtifact(artifactDefinition);
		if (additionalArtifact == null) {
			throw new MojoExecutionException("could not find additional artifact " + artifactDefinition);
		}
		File file = additionalArtifact.getFile();
		if (file == null) {
			throw new MojoExecutionException("artifact " + artifactDefinition.toString() + " has no file attached");
		}
		try {
			final UnArchiver unArchiver = getArtifactHelper().getUnArchiver(file);

			getLog().info("unpacking artifact to " + destination);
			assertUnpackDestinationFolderExists(destination);
			unArchiver.setSourceFile(file);
			unArchiver.setDestDirectory(destination.toFile());
			IncludeExcludeFileSelector includeExcludeFileSelector = new IncludeExcludeFileSelector();
			includeExcludeFileSelector.setIncludes(new String[]{
				"**/*"
			});
			includeExcludeFileSelector.setExcludes(new String[]{
				"**/*.class",
				"META-INF/MANIFEST.MF",
				"META-INF/maven",
				"META-INF/maven/*",
				"META-INF/maven/**/*"
			});
			unArchiver.setFileSelectors(new FileSelector[]{
				includeExcludeFileSelector
			});
			unArchiver.extract();
			
			if (packageLayout == PackageLayout.war) {
				unArchiver.setDestDirectory(destination.toFile());
				assertUnpackDestinationFolderExists(destination);
			}
			includeExcludeFileSelector.setIncludes(new String[]{
				"**/*.class"
			});
			includeExcludeFileSelector.setExcludes(new String[]{});
			unArchiver.setFileSelectors(new FileSelector[]{
				includeExcludeFileSelector
			});
			unArchiver.extract();
		} catch (NoSuchArchiverException e) {
			throw new MojoExecutionException("failed to unpack app main", e);
		}
	}

	private void assertUnpackDestinationFolderExists(Path destination) throws MojoExecutionException {
		if (Files.notExists(destination)) {
			try {
				Files.createDirectories(destination);
			} catch (IOException e) {
				throw new MojoExecutionException("could not create output folder " + destination, e);
			}
		}
	}
	
	private ArtifactDefinition dependencyToArtifactDefinition(Dependency dependency) {
		return ArtifactDefinition.fromDependency(dependency);
	}
	
	private Artifact getFelixFrameworkArtifact() throws MojoExecutionException {
		List<Dependency> dependencies = project.getDependencies();
		for (Dependency dependency : dependencies) {
			if (Constants.FELIX_FRAMEWORK_GROUP_ID.equals(dependency.getGroupId()) && Constants.FELIX_FRAMEWORK_ARTIFACT_ID.equals(dependency.getArtifactId())) {
				return getArtifactHelper().loadArtifact(dependencyToArtifactDefinition(dependency));
			}
		}
		return getArtifactHelper().loadArtifact(new ArtifactDefinition(Constants.FELIX_FRAMEWORK_GROUP_ID + ":" + Constants.FELIX_FRAMEWORK_ARTIFACT_ID + ":" + Constants.PACKAGING_JAR + ":5.6.12"));
	}

	private boolean isAppMainArtifact(ArtifactDefinition artifact) {
		return Constants.APP_MAIN_GROUP_ID.equals(artifact.getGroupId()) && Constants.APP_MAIN_ARTIFACT_ID.equals(artifact.getArtifactId());
	}
	
	private ArtifactDefinition getAppMainArtifact(ProvisioningModel model) {
		for (RunMode runMode : model.getRunModes()) {
			for (StartLevelBundle bundle : runMode.getBundles()) {
				for (ArtifactDefinition artifact : bundle.getArtifacts()) {
					if (isAppMainArtifact(artifact)) {
						return artifact;
					}
				}
			}
		}
		List<Plugin> plugins = project.getBuild().getPlugins();
		for (Plugin plugin : plugins) {
			if (
					Constants.APP_PROVISIONING_PLUGIN_GROUP_ID.equals(plugin.getGroupId()) 
					&& Constants.APP_PROVISIONING_PLUGIN_ARTIFACT_ID.equals(plugin.getArtifactId())
			) {
				// check if there is a dependency configured for the plugin in the project
				List<Dependency> pluginDependencies = plugin.getDependencies();
				if (pluginDependencies != null) {
					for (Dependency pluginDependency : pluginDependencies) {
						ArtifactDefinition artifact = dependencyToArtifactDefinition(pluginDependency);
						if (isAppMainArtifact(artifact)) {
							return artifact;
						}
					}
				}
				
				// if not, use the same version as of this plugin
				return new ArtifactDefinition(Constants.APP_MAIN_GROUP_ID + ":" + Constants.APP_MAIN_ARTIFACT_ID + ":" + Constants.PACKAGING_JAR + ":" + plugin.getVersion());
			}
		}
		return null;
	}

	static void recursiveDelete(Path targetFilePath) throws IOException {
		Files.walkFileTree(targetFilePath, new FileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
				return FileVisitResult.TERMINATE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	private void writeFileInfoProperties(Properties fileInfoProperties) throws MojoExecutionException {
		final Path outputPath;
		if (packageLayout == PackageLayout.springboot) {
			outputPath = Paths.get(project.getBuild().getDirectory()).resolve(Constants.TARGET_APP_FOLDER_FOR_MAVEN_BUILD).resolve("SBO-INF").resolve(Constants.FINAL_FILE_INFO_FILE);
		} else {
			outputPath = Paths.get(project.getBuild().getDirectory()).resolve(Constants.FINAL_FILE_INFO_FILE);
		}
		deleteIfExists(outputPath);
		createFileAndParentDirectories(outputPath);

		try (OutputStream out = Files.newOutputStream(outputPath, StandardOpenOption.WRITE)) {
			try (OutputStreamWriter w = new OutputStreamWriter(out, "UTF-8")) {
				fileInfoProperties.store(w, null);
				w.flush();
			}
			out.flush();
		} catch (IOException e) {
			throw new MojoExecutionException("Failed to render provisioning model to file '" + outputPath + "'", e);
		}
	}


}
