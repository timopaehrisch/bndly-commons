package org.bndly.rest.entity.resources;

/*-
 * #%L
 * REST Entity Resource
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

import org.bndly.common.mapper.ComplexTypeDetector;
import org.bndly.rest.common.beans.RestBean;
import org.bndly.schema.beans.ActiveRecord;
import org.bndly.schema.json.beans.StreamingObject;

public class ComplexTypeDetectorImpl implements ComplexTypeDetector {

    @Override
    public boolean isComplexType(Class<?> type, Object instance) {
        boolean isRestBean = RestBean.class.isAssignableFrom(type) || RestBean.class.isInstance(instance);
        boolean isSchemaBean = ActiveRecord.class.isAssignableFrom(type) || ActiveRecord.class.isInstance(instance);
        boolean isStreamingObject = StreamingObject.class.isAssignableFrom(type) || StreamingObject.class.isInstance(instance);
        return isRestBean || isSchemaBean || isStreamingObject;
    }
}
