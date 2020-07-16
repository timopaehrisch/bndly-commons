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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;
import org.apache.felix.framework.Felix;
import org.apache.felix.framework.util.FelixConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class FelixMain implements AppLauncher, FrameworkListener, Logger {

	private final Map<String, Object> config;
	private final Logger log;
	private final SimpleEnvironment environment;
	private final List<Runnable> onDestroy;
	
	private boolean started;
	private Felix felix;
	private FrameworkStartLevel startLevel;
	private BundleContext framworkBundleContext;
	private int targetStartLevel;
	private List<Integer> startLevels;
	private Map<Integer, List<InstallableBundle>> _bundlesByStartLevel;
	private Properties configProperties;
	private boolean skipShutdownHook;
	private Set<String> activeRunModes = Collections.EMPTY_SET;
	private RunModeSettings runModeSettings = new RunModeSettings() {
		@Override
		public boolean isActive(String bundleFileName) {
			return true;
		}
	};
	private final List<FelixMainListener> listeners = new ArrayList<>();

	public void destroy() {
		// remove the service loader based listeners
		for (Runnable destruction : onDestroy) {
			destruction.run();
		}
		onDestroy.clear();
	}

	public Properties getConfigProperties() {
		return configProperties;
	}

	private static final class StartLevelComparator implements Comparator<Integer> {

		@Override
		public int compare(Integer o1, Integer o2) {
			long r = o1.longValue() - o2.longValue();
			if (r < 0) {
				return -1;
			} else if (r > 0) {
				return 1;
			} else {
				return 0;
			}
		}

	}
	
	private final class ShutdownHook extends Thread {

		private final FelixMain main;

		public ShutdownHook(FelixMain main) {
			super("FelixMain Shutdown Hook");
			this.main = main;
		}
		
		@Override
		public void run() {
			try {
				main.stop();
			} catch (Exception ex) {
				log.info("could not stop framework: " + ex);
			}
		}
		
	}

	public FelixMain(SimpleEnvironment environment, Logger log, Map<String, Object> config) {
		this.log = log == null ? this : log;
		this.config = config == null ? Collections.EMPTY_MAP : Collections.unmodifiableMap(config);
		this.environment = environment;
		onDestroy = new ArrayList<>();
		ServiceLoader<FelixMainListener> listenerServiceLoader = ServiceLoader.load(FelixMainListener.class);
		Iterator<FelixMainListener> iterator = listenerServiceLoader.iterator();
		while(iterator.hasNext()) {
			final FelixMainListener listener = iterator.next();
			addListener(listener);
			onDestroy.add(new Runnable(){
				@Override
				public void run() {
					removeListener(listener);
				}

			});
		}
	}
	
	public FelixMain(Environment environment, Logger log, Map<String, Object> config) {
		this(new EnvironmentAsSimpleEnvironment(new EnvironmentOverlay(Arrays.asList(new ConfigBasedEnvironment((config == null ? Collections.EMPTY_MAP : Collections.unmodifiableMap(config))), environment))), log, config);
	}

	@Override
	public void start() throws Exception {
		synchronized (this) {
			if (started) {
				return;
			}
			if (!skipShutdownHook) {
				Runtime.getRuntime().addShutdownHook(new ShutdownHook(this));
			}
			environment.logConfig(log);
			environment.prepareForStart(log);

			// config properties can only be loaded after the unpack, because they might be contained in the jar
			configProperties = environment.initConfigProperties(log);

			environment.init(log, this);

			// start the felix framework
			if (!configProperties.containsKey(Constants.FRAMEWORK_BEGINNING_STARTLEVEL)) {
				configProperties.put(Constants.FRAMEWORK_BEGINNING_STARTLEVEL, Integer.toString(targetStartLevel));
			}
			if (!configProperties.containsKey(Constants.FRAMEWORK_STORAGE)) {
				configProperties.put(Constants.FRAMEWORK_STORAGE, environment.resolveFelixCachePath().toString());
			}
			
			log.info("framework is about to start");
			Map felixConfig = new HashMap();
			// put the properties from the config.properties here
			// also keep in mind, that this class might be called from a maven plugin
			applyConfigProperties("OSGI", SharedConstants.OSGI_FRAMEWORK_PROPERTIES, felixConfig);
			applyConfigProperties("OSGI", SharedConstants.SEMI_OFFICIAL_OSGI_FRAMEWORK_PROPERTIES, felixConfig);
			applyConfigProperties("FELIX", SharedConstants.FELIX_FRAMEWORK_PROPERTIES, felixConfig);
			applyConfigProperties("FILEINSTALL", SharedConstants.FELIX_FILEINSTALL_PROPERTIES, felixConfig);
			applyConfigProperties("BNDLY", SharedConstants.BNDLY_APPLICATION_PROPERTIES, felixConfig);
			
			for (FelixMainListener listener : listeners) {
				listener.beforeConfig(felixConfig);
			}
			
			felix = new Felix(felixConfig);
			felix.init(this);
			startLevel = felix.adapt(FrameworkStartLevel.class);
			framworkBundleContext = felix.adapt(BundleContext.class);
			framworkBundleContext.addFrameworkListener(this);
			for (FelixMainListener listener : listeners) {
				listener.beforeBundleInstallation(felix);
			}
			installBundles();
			for (FelixMainListener listener : listeners) {
				listener.beforeStart(felix);
			}
			felix.start();
			log.info("framework is started");
			started = true;
		}
	}

	public final FelixMain addListener(FelixMainListener listener) {
		listeners.add(listener);
		return this;
	}

	public final FelixMain removeListener(FelixMainListener listener) {
		Iterator<FelixMainListener> iterator = listeners.iterator();
		while (iterator.hasNext()) {
			if (iterator.next() == listener) {
				iterator.remove();
			}
		}
		return this;
	}
	
	public BundleContext getFramworkBundleContext() {
		return framworkBundleContext;
	}

	public void setRunModeSettings(RunModeSettings runModeSettings) {
		this.runModeSettings = runModeSettings;
	}

	public void setActiveRunModes(Set<String> activeRunModes) {
		this.activeRunModes = activeRunModes;
	}
	
	public void setBundlesByStartLevel(Map<Integer, List<InstallableBundle>> bundlesByStartLevel) {
		this._bundlesByStartLevel = bundlesByStartLevel;
		if (bundlesByStartLevel != null) {
			startLevels = new ArrayList<>(bundlesByStartLevel.keySet());
			Collections.sort(startLevels, new StartLevelComparator());
			if (startLevels.isEmpty()) {
				targetStartLevel = FelixConstants.FRAMEWORK_DEFAULT_STARTLEVEL;
			} else {
				targetStartLevel = startLevels.get(startLevels.size() - 1);
			}
		} else {
			startLevels = null;
			targetStartLevel = FelixConstants.FRAMEWORK_DEFAULT_STARTLEVEL;
		}
	}
	
	@Override
	public void frameworkEvent(FrameworkEvent event) {
		// do something
		if (event.getType() == FrameworkEvent.STARTED) {
			log.info("framework started (startlevel: " + startLevel.getStartLevel() + ")");
			Bundle[] allBundles = event.getBundle().getBundleContext().getBundles();
			for (Bundle bundle : allBundles) {
				if (bundle.getState() == Bundle.INSTALLED) {
					log.debug("bundle " + bundle.getSymbolicName() + " is only installed");
				} else if (bundle.getState() == Bundle.RESOLVED) {
					log.debug("bundle " + bundle.getSymbolicName() + " is only resolved");
				}
			}
		} else if (event.getType() == FrameworkEvent.STOPPED) {
			log.info("framework stopped");
		}
	}

	@Override
	public void stop() throws Exception {
		synchronized (this) {
			if (!started) {
				return;
			}
			log.info("framework is about to be stopped");
			for (FelixMainListener listener : listeners) {
				listener.beforeStop(felix);
			}
			felix.stop();
			felix.waitForStop(0);
			startLevel = null;
			framworkBundleContext = null;
			felix = null;
			log.info("framework is stopped");
			started = false;
		}
	}

	private void installBundles() throws Exception {
		for (Integer sl : startLevels) {
			List<InstallableBundle> bundles = _bundlesByStartLevel.get(sl);
			if (bundles != null) {
				for (InstallableBundle bundle : bundles) {
					// install
					boolean active = runModeSettings.isActive(bundle.getFileName());
					if (active) {
						String location = bundle.getLocation();
						Bundle existingBundle = framworkBundleContext.getBundle(location);
						// only install if the bundle is missing or if the provided bundle is newer
						if (existingBundle == null || existingBundle.getLastModified() < bundle.getLastModifiedMillis()) {
							try(InputStream bundleDataInputStream = bundle.getBundleDataInputStream()){
								Bundle installedBundle = framworkBundleContext.installBundle(location, bundleDataInputStream);
								BundleStartLevel bundleStartLevel = installedBundle.adapt(BundleStartLevel.class);
								if (sl > 0) {
									bundleStartLevel.setStartLevel(sl);
								}
								String symbolicName = installedBundle.getSymbolicName();
								log.debug("installed " + symbolicName);
								existingBundle = installedBundle;
							}
						}
						boolean isFragment = existingBundle.getHeaders().get(Constants.FRAGMENT_HOST) != null;
						if (!isFragment) {
							existingBundle.start();
						}
					} else {
						String location = bundle.getLocation();
						Bundle existingBundle = framworkBundleContext.getBundle(location);
						if (existingBundle != null) {
							existingBundle.stop();
						}
					}
				}
			}
		}
	}
	
	public void setSkipShutdownHook(boolean skipShutdownHook) {
		this.skipShutdownHook = skipShutdownHook;
	}

	@Override
	public void info(String message) {
		System.out.println("INFO: " + message);
	}

	@Override
	public void debug(String message) {
		System.out.println("DEBUG: " + message);
	}

	@Override
	public void warn(String message) {
		System.out.println("WARN: " + message);
	}

	@Override
	public void error(String message) {
		System.out.println("ERROR: " + message);
	}

	public Map<Integer, List<InstallableBundle>> getBundlesByStartLevel() {
		return _bundlesByStartLevel;
	}

	private void applyConfigProperties(String logPrefix, String[] propertyNames, Map felixConfig) {
		for (String propertyName : propertyNames) {
			if (System.getProperties().containsKey(propertyName)) {
				log.info(logPrefix + " PROPERTY: " + propertyName + "=" + System.getProperties().get(propertyName));
				felixConfig.put(propertyName, System.getProperties().get(propertyName));
			} else if (configProperties.containsKey(propertyName)) {
				log.info(logPrefix + " PROPERTY: " + propertyName + "=" + configProperties.get(propertyName));
				felixConfig.put(propertyName, configProperties.get(propertyName));
			}
		}
	}

	public void waitForStop() throws InterruptedException {
		if (!started) {
			return;
		}
		felix.waitForStop(0);
	}



}
