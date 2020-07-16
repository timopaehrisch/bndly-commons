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

import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

/**
 * This interface encapsulates the logic for setting up the environment for starting an Apache Felix.
 * The environment will be call several times during the startup by {@link FelixMain}. The order is as follows:
 * <ol>
 *     <li>{@link #logConfig(Logger)} to log the configuration of the environment to the console for debugging</li>
 *     <li>{@link #prepareForStart(Logger)} to extract the application archive (if needed)</li>
 *     <li>{@link #initConfigProperties(Logger)} to load the properties, that will be passed to the Apache Felix Container instance</li>
 *     <li>{@link #init(Logger, FelixMain)} define the bundles to install and to optionally set required system properties</li>
 *     <li>{@link #resolveFelixCachePath()} to get the path in the filesystem, where Apache Felix can store its bundle cache (required!)</li>
 * </ol>
 */
public interface SimpleEnvironment {
    void logConfig(Logger log);

    void prepareForStart(Logger log) throws Exception;

    Properties initConfigProperties(Logger log) throws IOException;

    Path resolveFelixCachePath();

    void init(Logger log, FelixMain felixMain) throws Exception;
}
