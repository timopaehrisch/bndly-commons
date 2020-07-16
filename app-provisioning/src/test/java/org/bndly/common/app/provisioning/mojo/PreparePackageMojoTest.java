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
import org.bndly.common.app.provisioning.model.ArtifactDefinition;
import org.bndly.common.app.provisioning.util.ArtifactHelper;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Properties;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class PreparePackageMojoTest extends AbstractMojoTestCase {

	@Rule
	private MojoRule mojoRule = new MojoRule(this);
	
	@Before
	@Override
	protected void setUp() throws Exception {
		// required
		super.setUp();
	}

	@After
	@Override
	protected void tearDown() throws Exception {
		// required
		super.tearDown();
	}

	@Override
	protected Mojo lookupMojo(String groupId, String artifactId, String version, String goal, PlexusConfiguration pluginConfiguration) throws Exception {
		if (version != null) {
			version = version.replaceAll("\\$\\{revision\\}", System.getProperty("revision")).replaceAll("\\$\\{changelist\\}", System.getProperty("changelist"));
		}
		return super.lookupMojo(groupId, artifactId, version, goal, pluginConfiguration);
	}

	@Test
	public void testPreparePackage() throws Exception {
		final Path testProjectPath = Paths.get(System.getProperty("test-project-path"));
		Path pomPath = testProjectPath.resolve("pom.xml");
		assertTrue(Files.isRegularFile(pomPath));

		final PreparePackageMojo myMojo = (PreparePackageMojo) lookupMojo("prepare-package", pomPath.toFile());
		assertNotNull(myMojo);
		MavenProject pseudoProject = new MavenProject();
		pseudoProject.getBuild().setDirectory(testProjectPath.resolve("target").toString());
		pseudoProject.getBuild().setFinalName("PreparePackageMojoTest.testPreparePackage");
		pseudoProject.getProperties().setProperty("home", testProjectPath.toString());
		pseudoProject.getProperties().setProperty("inlineJson", "{\"foo\":\"bar\"}");
		pseudoProject.getProperties().setProperty("windows", "C:\\Windows");
		pseudoProject.getProperties().setProperty("slf4j.version", "1.7.7");
		List<Plugin> plugins = pseudoProject.getBuild().getPlugins();
		Plugin plugin = new Plugin();
		plugin.setGroupId(Constants.APP_PROVISIONING_PLUGIN_GROUP_ID);
		plugin.setArtifactId(Constants.APP_PROVISIONING_PLUGIN_ARTIFACT_ID);
		plugin.setVersion("47.1.1");
		plugins.add(plugin);
				
		DefaultRepositorySystemSession repositorySession = new DefaultRepositorySystemSession();
		MavenExecutionRequest request = new DefaultMavenExecutionRequest();
		MavenExecutionResult result = new DefaultMavenExecutionResult();
		//PlexusContainer container, RepositorySystemSession repositorySession, MavenExecutionRequest request, MavenExecutionResult result
		MavenSession newMavenSession = new MavenSession(mojoRule.getContainer(), repositorySession, request, result);
		myMojo.setMavenSession(newMavenSession);
		myMojo.setProvisioningDirectory(testProjectPath.resolve("src/main/prov").toFile());
		myMojo.setProject(pseudoProject);
		myMojo.setArtifactHelper(new ArtifactHelper() {
			@Override
			public Path resolvePathToArtifact(ArtifactDefinition artifactDefinition) throws MojoExecutionException {
				org.apache.maven.artifact.Artifact loadArtifact = loadArtifact(artifactDefinition);
				return loadArtifact.getFile().toPath();
			}

			@Override
			public org.apache.maven.artifact.Artifact loadArtifact(ArtifactDefinition artifactDefinition) throws MojoExecutionException {
				ArtifactHandler artifactHandler = new DefaultArtifactHandler();
				DefaultArtifact defaultArtifact = new DefaultArtifact(
						artifactDefinition.getGroupId(),
						artifactDefinition.getArtifactId(),
						artifactDefinition.getVersion(),
						null,
						artifactDefinition.getPackaging(),
						artifactDefinition.getClassifier(), 
						artifactHandler);
				defaultArtifact.setFile(Paths.get("target", "test-m2", artifactDefinition.getArtifactId() + "-" + artifactDefinition.getVersion() + "." + artifactDefinition.getPackaging()).toFile());
				return defaultArtifact;
			}

			@Override
			public UnArchiver getUnArchiver(File file) throws NoSuchArchiverException {
				return myMojo.getArchiverManager().getUnArchiver(file);
			}

			@Override
			public Path getProjectBaseDirPath() {
				return testProjectPath;
			}

			@Override
			public org.apache.maven.artifact.Artifact resolveArtifact(org.apache.maven.artifact.Artifact mavenArtifact) throws MojoExecutionException {
				return mavenArtifact;
			}
			
		});
		myMojo.execute();
		// assert that there is a folder 'app' in the target folder, that contains the executable jar resources
		Path targetFolder = testProjectPath.resolve("target");
		Path createdFolder = targetFolder.resolve(Constants.TARGET_APP_FOLDER_FOR_MAVEN_BUILD);
		Path startLevelFolder = createdFolder.resolve("auto-deploy/1");
		Path bundleFile = startLevelFolder.resolve("slf4j-api-1.7.7.jar");
		
		Path javaMainFolder = testProjectPath.resolve("target").resolve(Constants.TARGET_JAVA_MAIN_FOLDER);
		Path fileFromFelix = javaMainFolder.resolve("file");
		
		Path configFolder = createdFolder.resolve(Constants.PATH_CONFIGS).resolve("runmode-test");
		Path configFile = configFolder.resolve("com.acme.Component.cfg");
		
		Assert.assertTrue(Files.isDirectory(createdFolder));
		Assert.assertTrue(Files.isDirectory(startLevelFolder));
		Assert.assertTrue(Files.isRegularFile(bundleFile));
		
		Assert.assertTrue(Files.isDirectory(javaMainFolder));
		Assert.assertTrue(Files.isRegularFile(fileFromFelix));
		
		Assert.assertTrue(Files.isDirectory(configFolder));
		Assert.assertTrue(Files.isRegularFile(configFile));
		
		try (InputStream is = Files.newInputStream(configFile, StandardOpenOption.READ)) {
			Properties properties = new Properties();
			properties.load(new InputStreamReader(is, "UTF-8"));
			
			Assert.assertEquals("bar", properties.get("foo"));
			Assert.assertEquals(testProjectPath.toString(), properties.get("replaced"));
			Assert.assertEquals("{\"foo\":\"bar\"}", properties.get("replaced2"));
			Assert.assertEquals("C:\\Windows", properties.get("replaced3"));
		}
		
	}
}
