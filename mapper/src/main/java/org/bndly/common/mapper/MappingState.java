package org.bndly.common.mapper;

/*-
 * #%L
 * Mapper
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

public class MappingState {
    private final Object source;
    private final Object target;
    private final MappedProperty sourceProperty;
    private final MappedProperty targetProperty;
    private final MappingContext context;
    private final MappingState parent;
    private final boolean mapsCollection;

    MappingState(Object source, Object target, MappedProperty sourceProperty, MappedProperty targetProperty, MappingContext context, MappingState parent, boolean mapsCollection) {
        this.source = source;
        this.target = target;
        this.context = context;
        this.sourceProperty = sourceProperty;
        this.targetProperty = targetProperty;
        this.parent = parent;
        this.mapsCollection = mapsCollection;
    }
    
    MappingState(Object source, Object target, MappedProperty sourceProperty, MappedProperty targetProperty, MappingContext context, boolean mapsCollection) {
        this(source, target, sourceProperty, targetProperty, context, null, mapsCollection);
    }
    
    MappingState(Object source, Object target, MappingContext context, MappingState parent, boolean mapsCollection) {
        this(source, target, null, null, context, parent, mapsCollection);
    }
    
    MappingState(Object source, Object target, MappingContext context, boolean mapsCollection) {
        this(source, target, context, null, mapsCollection);
    }

    public boolean mapsCollection() {
        return mapsCollection;
    }
    
    public Object getSource() {
        return source;
    }

    public Object getTarget() {
        return target;
    }

    public MappingState getParent() {
        return parent;
    }

    public MappedProperty getTargetProperty() {
        return targetProperty;
    }

    public MappedProperty getSourceProperty() {
        return sourceProperty;
    }

    public MappingContext getContext() {
        return context;
    }
    
}
