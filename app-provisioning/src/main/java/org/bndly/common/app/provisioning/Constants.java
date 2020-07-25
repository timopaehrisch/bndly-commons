package org.bndly.common.app.provisioning;

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

import org.bndly.common.app.SharedConstants;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public interface Constants extends SharedConstants {
	public static final String PACKAGING_JAR = "jar";
	public static final String PACKAGING_APP = "bndly-application";
	public static final String CONTEXT_PROVISIONING_MODEL = PACKAGING_APP + ":provisioning";
	public static final String TARGET_APP_FOLDER_FOR_MAVEN_BUILD = "app";
	public static final String TARGET_APP_FOLDER_FOR_MAVEN_START = "app-start";
	public static final String TARGET_JAVA_MAIN_FOLDER = "java-main";
	
	public static final String PATH_CONFIGS = "conf/app";
	public static final String PATH_WARS = "wars";

	public static final String MAVEN_CONTEXT_APPLICATION_KEY = "org.bndly.common.app.FelixMain.INSTANCE";
	public static final String MAVEN_CONTEXT_SYSTEM_PROPERTY_CLEANUP = "org.bndly.common.app.provisioning.mojo.StartMojo.SYSTEM_PROPERTY_CLEANUP";
	
	public static final String APP_PROVISIONING_PLUGIN_GROUP_ID = "org.bndly.common";
	public static final String APP_PROVISIONING_PLUGIN_ARTIFACT_ID = "bndly-maven-plugin";
	
	public static final String APP_MAIN_GROUP_ID = "org.bndly.common";
	public static final String APP_MAIN_ARTIFACT_ID = "org.bndly.common.app-main";
	
	public static final String FELIX_FRAMEWORK_GROUP_ID = "org.apache.felix";
	public static final String FELIX_FRAMEWORK_ARTIFACT_ID = "org.apache.felix.framework";
	
	public static final String DEFAULT_MAIN_CLASS = "org.bndly.common.app.Main";
	
	public static final String MANIFEST_VERSION = "Manifest-Version";
	public static final String MANIFEST_APPLICATION = "bndly-Application";
	public static final String MANIFEST_MAIN_CLASS = "Main-Class";
	public static final String MANIFEST_CLASSPATH = "Class-Path";
	
	public static final String FINAL_PROVISION_FILE_PARENT_FOLDER = "prov";
	public static final String FINAL_PROVISION_FILE = "provisioning-model.json";
	public static final String FINAL_PROVISION_FILE_APP_START = "provisioning-model-app-start.json";
	
	public static final String FILE_TYPE_BUNDLE = "b";
	public static final String FILE_TYPE_CONFIG = "c";
	public static final String FILE_TYPE_RESOURCE = "r";
	public static final String FILE_TYPE_RESOURCE_DIRECTORY = "d";
	public static final String MVN_DEFAULT_PROVISIONING_FOLDER = "src/main/prov";

	public static final String SBO_INF_PREFIX = "SBO-INF";
}
