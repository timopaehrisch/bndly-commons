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
import org.bndly.common.app.provisioning.util.ExportPackageUtil;
import org.bndly.common.app.provisioning.util.Version;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilderException;

@Mojo(name = "derivePackages", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class DeriveDependencyPackagesMojo extends AbstractMojo {

	public static final String NO_VERSION_PREFIX = "**";
	public static final String ADD_VERSION_PREFIX = "++";

	@Parameter
	protected String dependencyOverrides;

	@Parameter
	protected String excludes;
	
	@Parameter
	protected boolean excludeOsgiBundles;
	
	@Parameter(defaultValue = "META-INF.*,WEB-INF.*")
	protected String packageExcludes = "META-INF.*,WEB-INF.*";

	@Parameter(defaultValue = "dependencyPackages")
	protected String projectProperty = "dependencyPackages";

	@Component
	private DependencyTreeBuilder dependencyTreeBuilder;

	private static enum VersionType {
		/**
		 * The version is exported via the Export-Package header from the source bundle artifact.
		 */
		EXPORT_PACKAGE,

		/**
		 * The version is taken from the Bundle-Version header from the source bundle artifact.
		 */
		BUNDLE_VERSION,

		/**
		 * The version is taken from the artifact itself (GAV).
		 */
		ARTIFACT_VERSION,

		/**
		 * The version information shall not be stored in the derived package properties.
		 */
		SKIP
	}
	
	private static class ScannedPackages {
		final List<ScannedPackage> scannedPackages;
		final Set<ScannedPackage> packagesForMerge;
		final String javaPackage;

		public ScannedPackages(String javaPackage) {
			if (javaPackage == null) {
				throw new IllegalArgumentException("java package is not allowed to be null: " + javaPackage);
			}
			this.javaPackage = javaPackage;
			scannedPackages = new ArrayList<>();
			packagesForMerge = new HashSet<>();
		}

		private ScannedPackages add(ScannedPackage scannedPackage) {
			scannedPackages.add(scannedPackage);
			return this;
		}

		private ScannedPackages addMerged(ScannedPackage scannedPackage) {
			scannedPackages.add(scannedPackage);
			packagesForMerge.add(scannedPackage);
			return this;
		}

	}
	
	private static class ScannedPackage {
		final ArtifactDefinition sourceArtifact;
		final VersionType versionType;
		final Version version;
		final Version[] versions;
		final String javaPackage;

		public ScannedPackage(ArtifactDefinition sourceArtifact, VersionType versionType, String javaPackage, String version) {
			this(sourceArtifact, versionType, javaPackage, version == null ? null : Version.from(version));
		}

		private ScannedPackage(ArtifactDefinition sourceArtifact, VersionType versionType, String javaPackage, Version... versions) {
			if (sourceArtifact == null) {
				throw new IllegalArgumentException("source artifact is not allowed to be null");
			}
			this.sourceArtifact = sourceArtifact;
			if (javaPackage == null) {
				throw new IllegalArgumentException("java package is not allowed to be null: " + javaPackage);
			}
			this.javaPackage = javaPackage;
			if (versionType == null) {
				throw new IllegalArgumentException("version type is not allowed to be null: " + sourceArtifact + "->" + javaPackage);
			}
			this.versionType = versionType;
			if (versions == null) {
				throw new IllegalArgumentException("versions is not allowed to be null: " + sourceArtifact + "->" + javaPackage + "[" + versionType + "]");
			}
			this.versions = versions;
			this.version = versions[0];
		}


		@Override
		public int hashCode() {
			int hash = 5;
			hash = 17 * hash + Objects.hashCode(this.sourceArtifact);
			hash = 17 * hash + Objects.hashCode(this.versionType);
			hash = 17 * hash + Objects.hashCode(this.javaPackage);
			hash = 17 * hash + Objects.hashCode(this.version);
			return hash;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			final ScannedPackage other = (ScannedPackage) obj;
			if (!Objects.equals(this.javaPackage, other.javaPackage)) {
				return false;
			}
			if (!Objects.equals(this.version, other.version)) {
				return false;
			}
			if (!Objects.equals(this.sourceArtifact, other.sourceArtifact)) {
				return false;
			}
			if (this.versionType != other.versionType) {
				return false;
			}
			return true;
		}


		@Override
		public String toString() {
			return sourceArtifact.toString() + "->" + javaPackage + "=" + version + "[" + versionType + "]";
		}

		public ScannedPackage withFurtherVersion(Version version) {
			ArrayList<Version> versions = new ArrayList<>(Arrays.asList(this.versions));
			versions.add(version);
			return new ScannedPackage(sourceArtifact, versionType, javaPackage, versions.toArray(new Version[versions.size()]));
		}
	}
	
	private static interface Config {

		boolean isExcluded(Dependency dependency);

		boolean isExcluded(Artifact artifact);
		
		boolean isExcluded(ArtifactDefinition artifactDefinition);
		
		boolean isExcludedPackage(String packageName);

		Iterable<ArtifactDefinition> getOverrideDefinitions();

		boolean isVersionInfoSkipped(ArtifactDefinition artifactDefinition);

		boolean isAdditionalVersionInfoAvailable(ArtifactDefinition artifactDefinition);
	}

	private Config createConfig() {

		final Set<ArtifactDefinition> skipVersionInfoForArtifactDefinitions = new HashSet<>();
		final Set<ArtifactDefinition> additionalVersionInfoForArtifactDefinitions = new HashSet<>();
		final Set<ArtifactDefinition> excludedDefinitions = new HashSet<>();
		if (excludes != null) {
			for (String string : excludes.split(",")) {
				String trimmedEntry = string.trim();
				ArtifactDefinition artifactDefinition = new ArtifactDefinition(trimmedEntry);
				excludedDefinitions.add(artifactDefinition);
			}
		}
		final Set<ArtifactDefinition> overrideDefinitions = new HashSet<>();
		final Map<String, ArtifactDefinition> overrideDefinitionsByGroupdIdArtifactId = new HashMap<>();
		if (dependencyOverrides != null) {
			for (String string : dependencyOverrides.split(",")) {
				String trimmedEntry = string.trim();
				boolean skipVersionInfo = trimmedEntry.startsWith(NO_VERSION_PREFIX);
				boolean additionalVersionInfo = trimmedEntry.startsWith(ADD_VERSION_PREFIX);
				if (skipVersionInfo) {
					trimmedEntry = trimmedEntry.substring(NO_VERSION_PREFIX.length());
				} else if (additionalVersionInfo) {
					trimmedEntry = trimmedEntry.substring(ADD_VERSION_PREFIX.length());
				}
				ArtifactDefinition artifactDefinition = new ArtifactDefinition(trimmedEntry);
				if(skipVersionInfo) {
					skipVersionInfoForArtifactDefinitions.add(artifactDefinition);
				} else if(additionalVersionInfo) {
					additionalVersionInfoForArtifactDefinitions.add(artifactDefinition);
				}
				overrideDefinitionsByGroupdIdArtifactId.put(artifactDefinition.getGroupId() + ":" + artifactDefinition.getArtifactId(), artifactDefinition);
				overrideDefinitions.add(artifactDefinition);
			}
		}
		// exclude the current project artifact by default
		excludedDefinitions.add(ArtifactDefinition.fromArtifact(project.getArtifact()));
		String[] packageExcludesSplit = packageExcludes.split(",");
		final Set<String> wildcardExcludes = new HashSet<>();
		final Set<String> explicitExcludes = new HashSet<>();
		for (String packageExclude : packageExcludesSplit) {
			if (packageExclude.endsWith(".*")) {
				wildcardExcludes.add(packageExclude.substring(0, packageExclude.length() - ".*".length()));
			} else {
				explicitExcludes.add(packageExclude);
			}
		}
		return new Config() {
			@Override
			public boolean isExcluded(Dependency dependency) {
				return isExcluded(ArtifactDefinition.fromDependency(dependency));
			}

			@Override
			public boolean isExcluded(Artifact artifact) {
				return isExcluded(ArtifactDefinition.fromArtifact(artifact));
			}

			@Override
			public boolean isExcluded(ArtifactDefinition artifactDefinition) {
				String packaging = artifactDefinition.getPackaging();
				if ("jar".equals(packaging) || "war".equals(packaging) || "bundle".equals(packaging)) {
					return excludedDefinitions.contains(artifactDefinition) || overrideDefinitionsByGroupdIdArtifactId.containsKey(artifactDefinition.getGroupId() + ":" + artifactDefinition.getArtifactId());
				} else {
					return true;
				}
			}

			@Override
			public Iterable<ArtifactDefinition> getOverrideDefinitions() {
				return overrideDefinitions;
			}

			@Override
			public boolean isVersionInfoSkipped(ArtifactDefinition artifactDefinition) {
				return skipVersionInfoForArtifactDefinitions.contains(artifactDefinition);
			}

			@Override
			public boolean isAdditionalVersionInfoAvailable(ArtifactDefinition artifactDefinition) {
				return additionalVersionInfoForArtifactDefinitions.contains(artifactDefinition);
			}

			@Override
			public boolean isExcludedPackage(String packageName) {
				if (explicitExcludes.contains(packageName)) {
					return true;
				} else if (wildcardExcludes.contains(packageName)) {
					return true;
				} else {
					String[] packageElements = packageName.split("\\.");
					for (int i = 1; i < packageElements.length; i++) {
						StringBuilder sb = new StringBuilder();
						boolean first = true;
						for (int j = 0; j < packageElements.length - i; j++) {
							if (first) {
								first = false;
							} else {
								sb.append(".");
							}
							sb.append(packageElements[j]);
						}
						if (wildcardExcludes.contains(sb.toString())) {
							return true;
						}
					}
				}
				return false;
			}

		};
	}

	private interface Conflict {
		ScannedPackage getOne();
		ScannedPackage getTwo();
	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		final Config config = createConfig();
		final Set<ArtifactDefinition> defs = collectArtifactsFromDependencies(config);
		addArtifactsFromConfigOverrides(config, defs);
		Map<String, ScannedPackages> scannedPackagesByPackageName = scanJavaPackagesInArtifacts(config, defs);

		// merge the packages
		List<Conflict> conflicts = new ArrayList<>();
		List<ScannedPackage> scannedPackagesList = mergeJavaPackageAndResolveConflicts(scannedPackagesByPackageName, new Consumer<Conflict>(){
			@Override
			public void accept(Conflict conflict) {
				conflicts.add(conflict);
			}
		});
		if(!conflicts.isEmpty()) {
			 // list all conflicts to make it easier to resolve the issues in one step
			getLog().error("conflicts:");
			for (Conflict conflict : conflicts) {
				getLog().error("" + conflict.getOne() + " VS " + conflict.getTwo());
			}
			throw new MojoExecutionException("version conflicts found. please use either override mechanism using '++' prefix or exclude conflicting artifacts manually.");
		}

		// serialize all scanned packages into a comma separated value
		StringBuilder sb = createPropertyStringWithJavaPackages(scannedPackagesList);
		String packageString = sb.toString();
		getLog().debug(packageString);
		project.getProperties().setProperty(projectProperty, packageString);
		System.setProperty(projectProperty, packageString); // test for stupid spring boot maven wrapper...
	}

	private StringBuilder createPropertyStringWithJavaPackages(List<ScannedPackage> scannedPackagesList) {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		Collections.sort(scannedPackagesList, new Comparator<ScannedPackage>() {
			@Override
			public int compare(ScannedPackage o1, ScannedPackage o2) {
				return o1.javaPackage.compareTo(o2.javaPackage);
			}
		});
		for (ScannedPackage value : scannedPackagesList) {
			for (Version version : value.versions) {
				if (first) {
					first = false;
				} else {
					sb.append(", \\\n");
				}
				sb.append(value.javaPackage);
				if(value.versionType != VersionType.SKIP) {
					if (value.versionType != VersionType.ARTIFACT_VERSION && value.versionType != VersionType.BUNDLE_VERSION) {
						String v = version.toOsgiString();
						if (!v.isEmpty()) {
							sb.append(";version=\"").append(v).append("\"");
						}
					}
				}
			}
		}
		return sb;
	}

	private List<ScannedPackage> mergeJavaPackageAndResolveConflicts(Map<String, ScannedPackages> scannedPackagesByPackageName, Consumer<Conflict> conflictConsumer) throws MojoExecutionException {
		List<ScannedPackage> scannedPackagesList = new ArrayList<>();
		for (ScannedPackages scannedPackages : scannedPackagesByPackageName.values()) {
			Iterator<ScannedPackage> iterator = scannedPackages.scannedPackages.iterator();
			ScannedPackage existingPackage = null;
			while (iterator.hasNext()) {
				ScannedPackage scannedPackage = iterator.next();
				if (scannedPackages.packagesForMerge.contains(scannedPackage)) {
					// skip conflict resolution for packages, that will be merged
					continue;
				} else if (existingPackage == null) {
					existingPackage = scannedPackage;
					continue;
				}
				// only if the versions differ, we do some further investigation
				if (existingPackage.version.compareTo(scannedPackage.version) != 0) {

					if (existingPackage.versionType == VersionType.ARTIFACT_VERSION && scannedPackage.versionType == VersionType.ARTIFACT_VERSION) {
						// this is ok, because split packages may exist in apps <= Java 9
						continue;
					} else if (existingPackage.versionType == VersionType.ARTIFACT_VERSION && scannedPackage.versionType != VersionType.ARTIFACT_VERSION) {
						// we overwrite an artifact version with something explicitly defined. so this is ok.
						existingPackage = scannedPackage;
						continue;
					} else if (existingPackage.versionType != VersionType.ARTIFACT_VERSION && scannedPackage.versionType == VersionType.ARTIFACT_VERSION) {
						// this is ok, because at least one dependency has a proper version declaration
						continue;
					}
					if (existingPackage.sourceArtifact.equals(scannedPackage.sourceArtifact)) {
						getLog().warn("artifact internal version conflict: " + existingPackage + " VS " + scannedPackage);
						continue;
					}

					final ScannedPackage one = existingPackage;
					final ScannedPackage two = scannedPackage;
					conflictConsumer.accept(new Conflict() {
						@Override
						public ScannedPackage getOne() {
							return one;
						}

						@Override
						public ScannedPackage getTwo() {
							return two;
						}
					});
				}
			}
			// now we know the desired version. but there might be a further version, that needs to be merged into the generation output.
			for (ScannedPackage scannedPackage : scannedPackages.packagesForMerge) {
				existingPackage = existingPackage.withFurtherVersion(scannedPackage.version);
			}
			scannedPackagesList.add(existingPackage);
		}
		return scannedPackagesList;
	}

	private Map<String, ScannedPackages> scanJavaPackagesInArtifacts(Config config, Iterable<ArtifactDefinition> defs) throws MojoExecutionException {
		Map<String, ScannedPackages> scannedPackagesByPackageName = new HashMap<>();
		for (final ArtifactDefinition artifactDefinition : defs) {
			// load the artifact and then check the contents for exported packages
			File artifactFile = resolveArtifactDefinitionToArtifactFile(artifactDefinition);
			if (artifactFile == null) {
				throw new MojoExecutionException("no file for artifact " + artifactDefinition);
			}
			try {
				JarFile jarFile = new JarFile(artifactFile);
				Manifest manifest = jarFile.getManifest();
				final String exportPackages;
				final String bundleVersion;
				if (manifest != null) {
					Attributes mainAttributes = manifest.getMainAttributes();
					if (mainAttributes != null) {
						exportPackages = mainAttributes.getValue("Export-Package");
						bundleVersion = mainAttributes.getValue("Bundle-Version");
					} else {
						exportPackages = null;
						bundleVersion = null;
					}
				} else {
					exportPackages = null;
					bundleVersion = null;
				}
				if (exportPackages != null) {
					if (excludeOsgiBundles) {
						continue;
					}
					getLog().debug("scanned " + artifactDefinition);
					getLog().debug("Export-Packages: " + exportPackages);
					final List<ScannedPackage> scannedPackages = new ArrayList<>();
					ExportPackageUtil.parse(exportPackages, new ExportPackageUtil.PackageConsumer() {
						@Override
						public void consumePackage(String packageName, List<String> metaData) {
							if (config.isExcludedPackage(packageName)) {
								return;
							}
							String version = ExportPackageUtil.getVersionFromMetaData(metaData);
							ScannedPackage sp;
							if (version != null) {
								sp = new ScannedPackage(artifactDefinition, config.isVersionInfoSkipped(artifactDefinition) ? VersionType.SKIP : VersionType.EXPORT_PACKAGE, packageName, version);

							} else {
								String message = "package " + packageName + " from dependency " + artifactDefinition + " has no version: " + exportPackages;
								if (bundleVersion == null) {
									getLog().warn(message);
									sp = new ScannedPackage(artifactDefinition, config.isVersionInfoSkipped(artifactDefinition) ? VersionType.SKIP : VersionType.ARTIFACT_VERSION, packageName, artifactDefinition.getVersion());
								} else {
									// the artifact seems to be a proper osgi bundle. therefore we will trust, that the export package header tells us!
									getLog().debug(message);
									sp = new ScannedPackage(artifactDefinition, config.isVersionInfoSkipped(artifactDefinition) ? VersionType.SKIP : VersionType.BUNDLE_VERSION, packageName, bundleVersion);
								}
							}
							scannedPackages.add(sp);

						}
					});
					for (ScannedPackage scannedPackage : scannedPackages) {
						put(scannedPackagesByPackageName, scannedPackage.sourceArtifact, scannedPackage.versionType, scannedPackage.javaPackage, scannedPackage.version.toString(), config);
					}
				} else {
					// look at the contents
					Set<String> packages = new HashSet<>();
					Enumeration<JarEntry> entries = jarFile.entries();
					while (entries.hasMoreElements()) {
						JarEntry jarEntry = entries.nextElement();
						String entryName = jarEntry.getName();
						if (entryName.endsWith(".class")) {
							String className = entryName.substring(0, entryName.length() - ".class".length());
							int pckEnd = className.lastIndexOf("/");
							if (pckEnd > -1) {
								String packageName = className.substring(0, pckEnd).replaceAll("/", ".");
								if (config.isExcludedPackage(packageName)) {
									continue;
								}
								packages.add(packageName.trim());
							}
						}

					}
					getLog().debug("scanned " + artifactDefinition);
					for (String packageName : packages) {
						put(scannedPackagesByPackageName, artifactDefinition, VersionType.ARTIFACT_VERSION, packageName, artifactDefinition.getVersion(), config);
					}
				}
			} catch (IOException ex) {
				throw new MojoExecutionException("could not unpack " + artifactDefinition, ex);
			}
		}
		return scannedPackagesByPackageName;
	}

	private void addArtifactsFromConfigOverrides(Config config, Set<ArtifactDefinition> defs) {
		for (ArtifactDefinition overrideDefinition : config.getOverrideDefinitions()) {
			getLog().info("adding override dependency: " + overrideDefinition);
			defs.add(overrideDefinition);
		}
	}

	private Set<ArtifactDefinition> collectArtifactsFromDependencies(Config config) throws MojoExecutionException {
		ArtifactFilter af = new ArtifactFilter() {
			@Override
			public boolean include(Artifact artifact) {
				return !config.isExcluded(artifact);
			}
		};
		final Set<ArtifactDefinition> defs = new HashSet<>();
		ArtifactRepository ar;
		try {
			dependencyTreeBuilder.buildDependencyTree(project, mavenSession.getLocalRepository(), af).accept(new org.apache.maven.shared.dependency.tree.traversal.DependencyNodeVisitor() {
				@Override
				public boolean visit(org.apache.maven.shared.dependency.tree.DependencyNode dn) {
					Artifact artifact = dn.getArtifact();
					ArtifactDefinition artifactDefinition = ArtifactDefinition.fromArtifact(artifact);
					if (!config.isExcluded(artifactDefinition)) {
						defs.add(artifactDefinition);
					}
					return true;
				}

				@Override
				public boolean endVisit(org.apache.maven.shared.dependency.tree.DependencyNode dn) {
					return true;
				}
			});
		} catch (DependencyTreeBuilderException e) {
			throw new MojoExecutionException("could not build dependency tree", e);
		}
		return defs;
	}

	private void put(Map<String, ScannedPackages> scannedPackagesByPackageName, ArtifactDefinition sourceArtifact, VersionType versionType, String packageName, String version, Config config) throws MojoExecutionException {
		ScannedPackage scannedPackage = new ScannedPackage(sourceArtifact, versionType, packageName, version);
		ScannedPackages existingPackages = scannedPackagesByPackageName.get(packageName);
		if (existingPackages == null) {
			existingPackages = new ScannedPackages(packageName);
			scannedPackagesByPackageName.put(packageName, existingPackages);
		}

		// track packages first. then merge them together later!
		if (config.isAdditionalVersionInfoAvailable(sourceArtifact)) {
			existingPackages.addMerged(scannedPackage);
		} else {
			existingPackages.add(scannedPackage);
		}
	}
}
