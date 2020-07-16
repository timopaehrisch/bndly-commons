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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class PathInstallableBundle implements InstallableBundle {
    private final Path path;

    public PathInstallableBundle(Path path) {
        this.path = path;
    }

    @Override
    public String getLocation() {
        return path.toUri().toString();
    }

    @Override
    public String getFileName() {
        return path.getFileName().toString();
    }

    @Override
    public InputStream getBundleDataInputStream() {
        return null;
    }

    @Override
    public long getLastModifiedMillis() throws IOException {
        return Files.getLastModifiedTime(path).toMillis();
    }
}
