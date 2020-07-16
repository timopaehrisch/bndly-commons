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

import com.google.common.io.Files;
import org.bndly.common.app.provisioning.Constants;
import org.bndly.common.app.provisioning.ProvisioningModelParser;
import org.bndly.common.app.provisioning.model.ProvisioningModel;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.PropertiesBasedValueSource;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;

public final class ProvisioningModelLoader {
	
	private final Log logger;

	public ProvisioningModelLoader(Log logger) {
		if (logger == null) {
			throw new IllegalArgumentException("logger is not allowed to be null");
		}
		this.logger = logger;
	}
	
	private File provisioningDirectory;
	private StringSearchInterpolator stringSearchInterpolator;

	private Log getLog() {
		return logger;
	}

	private StringSearchInterpolator getStringSearchInterpolator() {
		return stringSearchInterpolator;
	}

	public final ProvisioningModelLoader setProject(MavenProject project) {
		setProvisioningDirectory(project.getBasedir().toPath().resolve(Constants.MVN_DEFAULT_PROVISIONING_FOLDER).toFile());
		stringSearchInterpolator = new StringSearchInterpolator();
		init(stringSearchInterpolator, project);
		return this;
	}
	
	private void init(StringSearchInterpolator stringSearchInterpolator, MavenProject project) {
		stringSearchInterpolator.addValueSource(new PropertiesBasedValueSource(project.getProjectBuildingRequest().getSystemProperties()));
		stringSearchInterpolator.addValueSource(new PropertiesBasedValueSource(project.getProjectBuildingRequest().getUserProperties()));
		stringSearchInterpolator.addValueSource(new PropertiesBasedValueSource(project.getProperties()));
	}
	
	public final ProvisioningModelLoader setProvisioningDirectory(File provisioningDirectory) {
		this.provisioningDirectory = provisioningDirectory;
		return this;
	}

	public final ProvisioningModelLoader setStringSearchInterpolator(StringSearchInterpolator stringSearchInterpolator) {
		this.stringSearchInterpolator = stringSearchInterpolator;
		return this;
	}
	
	public final ProvisioningModel loadProvisioningModel() throws MojoExecutionException {
		return loadProvisioningModel(true);
	}

	public final ProvisioningModel loadProvisioningModel(boolean interpolatePlaceholderStrings) throws MojoExecutionException {
		File[] provisioningFiles;
		if (provisioningDirectory.isDirectory()) {
			provisioningFiles = provisioningDirectory.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(".json");
				}
			});
		} else if (provisioningDirectory.isFile()) {
			if (!provisioningDirectory.getName().endsWith(".json")) {
				throw new MojoExecutionException("failed to get provisioning model from " + provisioningDirectory.toString() + ", because it is not a folder or json file.");
			}
			provisioningFiles = new File[]{provisioningDirectory};
		} else {
			throw new MojoExecutionException("failed to get provisioning model from " + provisioningDirectory.toString());
		}
		if (provisioningFiles == null || provisioningFiles.length == 0) {
			throw new MojoExecutionException("no provisioning file found");
		}
		getLog().debug("Found " + provisioningFiles.length + " provisioning files");
		ProvisioningModelParser provisioningModelParser = new ProvisioningModelParser();
		List<ProvisioningModel> models = new ArrayList<>();
		for (File provisioningFile : provisioningFiles) {
			getLog().debug("Processing " + provisioningFile);
			try {
				final ProvisioningModel model;
				if (interpolatePlaceholderStrings) {
					StringSearchInterpolator interp = getStringSearchInterpolator();
					if (interp == null) {
						throw new MojoExecutionException("no string search interpolator configured.");
					}
					String provisioningJson = Files.toString(provisioningFile, Charset.forName("UTF-8"));
					model = provisioningModelParser.parse(new StringReader(provisioningJson), interp);
				} else {
					try (Reader provisioningFileReader = Files.newReader(provisioningFile, Charset.forName("UTF-8"))) {
						model = provisioningModelParser.parse(provisioningFileReader, null);
					} catch (IOException e) {
						throw new MojoExecutionException("Failed to parse '" + provisioningFile + "'", e);
					}
				}
				models.add(model);
			} catch (IOException e) {
				throw new MojoExecutionException("could not parse provisioning file", e);
			} catch (InterpolationException ex) {
				throw new MojoExecutionException("could not interpolate provisioning file properties", ex);
			}
		}
		// aggregate models
		return new ProvisioningModel(models.toArray(new ProvisioningModel[models.size()]));
	}
}
