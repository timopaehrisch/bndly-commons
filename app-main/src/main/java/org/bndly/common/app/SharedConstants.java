package org.bndly.common.app;

/*-
 * #%L
 * App Main
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

import org.apache.felix.framework.util.FelixConstants;
import org.osgi.framework.Constants;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public interface SharedConstants {
	public static final String AUTO_DEPLOY_PATH = "auto-deploy";
	public static final String RUN_MODE_PROPERTIES_FILE = "runmode.properties";
	public static final String APP_JAR_RESOURCES = "resources";
	public static final String APP_JAR_EXPLODED_FRAMEWORK = "framework";
	public static final String JAR_ENTRY_PATH_SEPARATOR = "/";
	
	/**
	 * This file contains information on the semantic of an embedded file in the final application artifact.
	 */
	public static final String FINAL_FILE_INFO_FILE = "file-info.properties";
	
	public static final String CONFIG_PROPERTY_DO_UNPACK_APP_JAR = "FelixMain.unpackAppJar";
	public static final String CONFIG_PROPERTY_HOME_FOLDER = "FelixMain.homeFolder";
	public static final String CONFIG_PROPERTY_CONFIG_PROPERTIES = "FelixMain.configProperties";
	
	public static final String FILE_SUFFIX_JAR = ".jar";
	
	public static final String ENV_PROPERTY_HOME_FOLDER = "BNDLY_APP_HOME";
	
	public static final String SYSTEM_PROPERTY_VALUE_SECURED_CONFIG_LOCATION = "conf/app";
	
	public static final String SYSTEM_PROPERTY_JETTY_HOME = "jetty.home";
	public static final String SYSTEM_PROPERTY_VALUE_JETTY_HOME = "jettyhome";
	
	public static final String SYSTEM_PROPERTY_JETTY_ETC_CONFIG_URLS = "jetty.etc.config.urls";
	public static final String SYSTEM_PROPERTY_VALUE_JETTY_ETC_CONFIG_URLS = "etc/jetty-0.xml,etc/jetty-1-selector.xml,etc/jetty-2-deployer.xml,etc/jetty-3-ssl.xml,etc/jetty-4-https.xml";
	
	public static final String SYSTEM_PROPERTY_SOLR_HOME = "solr.solr.home";
	public static final String SYSTEM_PROPERTY_VALUE_SOLR_HOME = "solr";
	
	public static final String SYSTEM_PROPERTY_JAVA_IO_TEMP = "java.io.tmpdir";
	public static final String SYSTEM_PROPERTY_VALUE_JAVA_IO_TEMP = "temp";
	
	public static final String SYSTEM_PROPERTY_LOGBACK_CONFIGURATION_FILE = "logback.configurationFile";
	public static final String SYSTEM_PROPERTY_VALUE_LOGBACK_CONFIGURATION_FILE = "conf/logback.xml";
	
	public static final String SYSTEM_PROPERTY_HOME_FOLDER = "bndly.application.home";
	public static final String SYSTEM_PROPERTY_HOME_FOLDER_EXTENDED = "bndly.application.home.extended";
	
	public static final String SYSTEM_PROPERTY_RUN_MODES = "bndly.application.runmodes";
	
	public static final String SYSTEM_PROPERTY_CONFIG_DIR = "bndly.application.config.dir";
	
	public static final String SYSTEM_PROPERTY_JAXB_IMPL_BUNDLE = "bndly.application.jaxb.bundle";

	/**
	 * This property can be configured as system or config file property. Its values might be {@code true} or {@code false}.
	 * If {@code true}, then the bndly application will install a fragment bundle, that will expose a SPI configuration
	 * for creating JAXBContext instances. If the property is not configured or {@code false}, then JAXBContext creation
	 * is not covered by the bndly application framework.
	 */
	public static final String SYSTEM_PROPERTY_JAXB_JAVA11_SUPPORT_ENABLED = "bndly.application.jaxbjava11support.enabled";

	public static final String[] SEMI_OFFICIAL_OSGI_FRAMEWORK_PROPERTIES = new String[]{
		"org.osgi.service.http.port",
		"org.osgi.service.http.port.secure"
	};
	static final String FELIX_FILEINSTALL_POLL = "felix.fileinstall.poll";
	static final String FELIX_FILEINSTALL_DIR = "felix.fileinstall.dir";
	static final String FELIX_FILEINSTALL_LOGLEVEL = "felix.fileinstall.log.level";
	static final String FELIX_FILEINSTALL_BUNDLESNEWSTART = "felix.fileinstall.bundles.new.start";
	static final String FELIX_FILEINSTALL_FILTER = "felix.fileinstall.filter";
	static final String FELIX_FILEINSTALL_TMPDIR = "felix.fileinstall.tmpdir";
	static final String FELIX_FILEINSTALL_NO_INITIAL_DELAY = "felix.fileinstall.noInitialDelay";
	static final String FELIX_FILEINSTALL_BUNDLESSTART_TRANSIENT = "felix.fileinstall.bundles.startTransient";
	static final String FELIX_FILEINSTALL_BUNDLESSTART_ACTIVATION_POL = "felix.fileinstall.bundles.startActivationPolicy";
	static final String FELIX_FILEINSTALL_STARTLEVEL = "felix.fileinstall.start.level";
	static final String FELIX_FILEINSTALL_ACTIVELEVEL = "felix.fileinstall.active.level";
	static final String FELIX_FILEINSTALL_ENABLE_CONFIG_SAVE = "felix.fileinstall.enableConfigSave";
	static final String FELIX_FILEINSTALL_BUNDLESUPDATE_WITH_LISTENER = "felix.fileinstall.bundles.updateWithListeners";
	
	public static final String[] BNDLY_APPLICATION_PROPERTIES = new String[]{
		SYSTEM_PROPERTY_RUN_MODES,
		SYSTEM_PROPERTY_CONFIG_DIR,
		SYSTEM_PROPERTY_JAXB_IMPL_BUNDLE,
		SYSTEM_PROPERTY_JAXB_JAVA11_SUPPORT_ENABLED
	};
			
	public static final String[] FELIX_FILEINSTALL_PROPERTIES = new String[]{
		FELIX_FILEINSTALL_POLL, 
		FELIX_FILEINSTALL_DIR, 
		FELIX_FILEINSTALL_LOGLEVEL, 
		FELIX_FILEINSTALL_BUNDLESNEWSTART, 
		FELIX_FILEINSTALL_FILTER, 
		FELIX_FILEINSTALL_TMPDIR, 
		FELIX_FILEINSTALL_NO_INITIAL_DELAY, 
		FELIX_FILEINSTALL_BUNDLESSTART_TRANSIENT, 
		FELIX_FILEINSTALL_BUNDLESSTART_ACTIVATION_POL, 
		FELIX_FILEINSTALL_STARTLEVEL, 
		FELIX_FILEINSTALL_ACTIVELEVEL, 
		FELIX_FILEINSTALL_ENABLE_CONFIG_SAVE, 
		FELIX_FILEINSTALL_BUNDLESUPDATE_WITH_LISTENER
	};
	
	public static final String[] OSGI_FRAMEWORK_PROPERTIES = new String[]{
				Constants.FRAMEWORK_VERSION,
				Constants.FRAMEWORK_VENDOR,
				Constants.FRAMEWORK_LANGUAGE,
				Constants.FRAMEWORK_OS_NAME,
				Constants.FRAMEWORK_OS_VERSION,
				Constants.FRAMEWORK_PROCESSOR,
				Constants.FRAMEWORK_BOOTDELEGATION,
				Constants.FRAMEWORK_SYSTEMPACKAGES,
				Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA,
				Constants.FRAMEWORK_SECURITY,
				Constants.FRAMEWORK_STORAGE,
				Constants.FRAMEWORK_STORAGE_CLEAN,
				Constants.FRAMEWORK_LIBRARY_EXTENSIONS,
				Constants.FRAMEWORK_EXECPERMISSION,
				Constants.FRAMEWORK_TRUST_REPOSITORIES,
				Constants.FRAMEWORK_WINDOWSYSTEM,
				Constants.FRAMEWORK_BEGINNING_STARTLEVEL,
				Constants.FRAMEWORK_BUNDLE_PARENT,
				Constants.FRAMEWORK_SYSTEMCAPABILITIES,
				Constants.FRAMEWORK_SYSTEMCAPABILITIES_EXTRA,
				Constants.FRAMEWORK_BSNVERSION
			};
	
	public static final String[] FELIX_FRAMEWORK_PROPERTIES = new String[]{
				FelixConstants.FRAMEWORK_BUNDLECACHE_IMPL,
				FelixConstants.LOG_LEVEL_PROP,
				FelixConstants.LOG_LOGGER_PROP,
				FelixConstants.SYSTEMBUNDLE_ACTIVATORS_PROP,
				FelixConstants.BUNDLE_STARTLEVEL_PROP,
				FelixConstants.SERVICE_URLHANDLERS_PROP,
				FelixConstants.IMPLICIT_BOOT_DELEGATION_PROP,
				FelixConstants.BOOT_CLASSLOADERS_PROP,
				FelixConstants.USE_LOCALURLS_PROP,
				FelixConstants.NATIVE_OS_NAME_ALIAS_PREFIX,
				FelixConstants.NATIVE_PROC_NAME_ALIAS_PREFIX
			};
}
