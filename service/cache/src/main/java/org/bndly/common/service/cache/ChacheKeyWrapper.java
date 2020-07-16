package org.bndly.common.service.cache;

/*-
 * #%L
 * Service Cache
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

import org.bndly.common.service.cache.api.CacheKey;
import java.io.Serializable;

public class ChacheKeyWrapper implements Serializable {

    private final CacheKey cacheKey;

    public ChacheKeyWrapper(CacheKey cacheKey) {
        this.cacheKey = cacheKey;
    }
    
    @Override
    public int hashCode() {
        return cacheKey._hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return cacheKey._equals(o);
    }
}
