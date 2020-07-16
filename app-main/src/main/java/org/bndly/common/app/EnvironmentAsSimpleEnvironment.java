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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class EnvironmentAsSimpleEnvironment implements SimpleEnvironment {
    private final Environment environment;

    public EnvironmentAsSimpleEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void logConfig(Logger log) {
        log.info("Home: " + environment.getHomeFolder());
        log.info("AutoDeployPath: " + environment.getAutoDeployPath());
        log.info("ApplicationConfigPath: " + environment.getApplicationConfigPath());
        log.info("ConfigPropertiesPath: " + environment.getConfigPropertiesPath());
        log.info("JarPath: " + environment.getJarPath());
        log.info("LogbackConfigFilePath: " + environment.getLogbackConfigFilePath());
        log.info("TempFolder: " + environment.getTempFolder());
    }

    @Override
    public void prepareForStart(Logger log) throws Exception {
        Boolean unpack = environment.needsUnpack();
        if (unpack) {
            // get the current jar
            log.info("JAR: " + environment.getJarPath());
            environment.createUnpackCallable(environment, log).call();
            log.info("unpacked jar");
        } else {
            log.info("skipping unpacking");
        }
    }

    @Override
    public Properties initConfigProperties(Logger log) throws IOException {
        Properties configProperties = new Properties();
        Path configPropertiesLocation = environment.getConfigPropertiesPath();
        log.info("loading properties from " + configPropertiesLocation.toString());
        if (Files.isRegularFile(configPropertiesLocation)) {
            try (InputStream inStream = Files.newInputStream(configPropertiesLocation)) {
                configProperties.load(inStream);
            }
        } else {
            log.warn("could not load properties from " + configPropertiesLocation.toString());
        }
        return configProperties;
    }

    @Override
    public Path resolveFelixCachePath() {
        return environment.getHomeFolder().resolve("felix-cache");
    }

    @Override
    public void init(Logger log, FelixMain felixMain) throws Exception {

        setSystemPropertyDefaults(log, felixMain);

        prepareBundlesToInstall(log, felixMain);
    }



    private Set<String> initActiveRunModes() {
        String runModesString = System.getProperty(SharedConstants.SYSTEM_PROPERTY_RUN_MODES);
        if (runModesString != null) {
            String[] runModes = runModesString.split(",");
            Set<String> activeRunModes = new HashSet<>();
            for (String runMode : runModes) {
                activeRunModes.add(runMode);
            }
            return activeRunModes;
        }
        return null;
    }

    private void prepareBundlesToInstall(Logger log, FelixMain felixMain) throws Exception {
        Set<String> activeRunModes = initActiveRunModes();

        if (felixMain.getBundlesByStartLevel() != null) {
            return;
        }
        Path autoDeployPath = environment.getAutoDeployPath();
        log.info("installing bundles from " + autoDeployPath.toString());
        List<Integer> startLevels = new ArrayList<>();
        final Map<Integer, List<InstallableBundle>> tmp = new HashMap<>();
        final Map<Integer, Properties> runModePropertiesByStartLevel = new HashMap<>();
        Files.walkFileTree(autoDeployPath, new FileVisitor<Path>() {

            Integer currentStartLevel;

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                String n = dir.getFileName().toString();
                try {
                    currentStartLevel = Integer.valueOf(n);
                    log.debug("detected start level " + currentStartLevel);
                    startLevels.add(currentStartLevel);
                } catch (NumberFormatException e) {
                    currentStartLevel = null;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.getFileName().toString().endsWith(SharedConstants.FILE_SUFFIX_JAR)) {
                    if (currentStartLevel != null) {
                        log.debug("detected start level file" + file);
                        List<InstallableBundle> bundlesOfStartLevel = tmp.get(currentStartLevel);
                        if (bundlesOfStartLevel == null) {
                            bundlesOfStartLevel = new ArrayList<>();
                            tmp.put(currentStartLevel, bundlesOfStartLevel);
                        }
                        bundlesOfStartLevel.add(new PathInstallableBundle(file));
                    }
                } else if (file.getFileName().toString().equals(SharedConstants.RUN_MODE_PROPERTIES_FILE)) {
                    if (currentStartLevel != null) {
                        runModePropertiesByStartLevel.put(currentStartLevel, loadPropertiesFromFile(file));
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                return FileVisitResult.TERMINATE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                currentStartLevel = null;
                return FileVisitResult.CONTINUE;
            }

            private Properties loadPropertiesFromFile(Path file) throws IOException {
                try (Reader reader = Files.newBufferedReader(file, Charset.forName("UTF-8"))) {
                    Properties properties = new Properties();
                    properties.load(reader);
                    return properties;
                }
            }
        });

        felixMain.setBundlesByStartLevel(tmp);
        felixMain.setRunModeSettings(new MapBasedRunModeSettings(runModePropertiesByStartLevel, activeRunModes));
    }

    private void setSystemPropertyDefaults(Logger log, FelixMain felixMain) {
        Properties configProperties = felixMain.getConfigProperties();
        for (Object key : configProperties.keySet()) {
            setSystemPropertyDefault((String) key, configProperties.getProperty((String) key), log);
        }
        setSystemPropertyDefault(SharedConstants.SYSTEM_PROPERTY_CONFIG_DIR, environment.getApplicationConfigPath().toString(), log);
        setSystemPropertyDefault(SharedConstants.SYSTEM_PROPERTY_JETTY_HOME, environment.getJettyHome().toString(), log);
        try {
            String jettyConfigs = collectJettyConfigs(environment.getJettyHome());
            setSystemPropertyDefault(SharedConstants.SYSTEM_PROPERTY_JETTY_ETC_CONFIG_URLS, jettyConfigs, log);
        } catch (IOException ex) {
            log.info("failed to collect jetty config names.");
            setSystemPropertyDefault(SharedConstants.SYSTEM_PROPERTY_JETTY_ETC_CONFIG_URLS, SharedConstants.SYSTEM_PROPERTY_VALUE_JETTY_ETC_CONFIG_URLS, log);
        }
        setSystemPropertyDefault(SharedConstants.SYSTEM_PROPERTY_SOLR_HOME, environment.getEmbeddedSolrHome().toString(), log);
        setSystemPropertyDefault(SharedConstants.SYSTEM_PROPERTY_JAVA_IO_TEMP, environment.getTempFolder().toString(), log);
        setSystemPropertyDefault(SharedConstants.SYSTEM_PROPERTY_LOGBACK_CONFIGURATION_FILE, environment.getLogbackConfigFilePath().toString(), log);
        setSystemPropertyDefault(SharedConstants.SYSTEM_PROPERTY_HOME_FOLDER, environment.getHomeFolder().toString(), log);
        setSystemPropertyDefault(SharedConstants.SYSTEM_PROPERTY_HOME_FOLDER_EXTENDED, environment.getHomeFolder().toString() + File.separator, log);
    }

    private void setSystemPropertyDefault(String key, String value, Logger log) {
        Properties properties = System.getProperties();
        if (properties.containsKey(key)) {
            log.info("system property already defined: " + key + "=" + properties.get(key));
            return;
        }
        log.info("setting system property: " + key + "=" + value);
        properties.setProperty(key, value);
    }

    private String collectJettyConfigs(final Path jettyHomePath) throws IOException {
        if (!Files.isDirectory(jettyHomePath)) {
            return "";
        }
        final List<String> configFiles = new ArrayList<>();
        Files.walkFileTree(jettyHomePath, new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path fn = file.getFileName();
                if (fn != null) {
                    String fns = fn.toString();
                    if (fns.endsWith(".xml") && fns.startsWith("jetty")) {
                        configFiles.add(jettyHomePath.relativize(file).toString());
                    }
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
        Collections.sort(configFiles);
        final StringBuilder sb = new StringBuilder();
        for (String configFile : configFiles) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(configFile);
        }
        return sb.toString();
    }
}
