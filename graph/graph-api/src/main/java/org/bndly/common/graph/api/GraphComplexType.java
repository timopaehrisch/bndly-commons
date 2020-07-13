package org.bndly.common.graph.api;

/*-
 * #%L
 * Graph API
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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface GraphComplexType {
    public static class NoopReferenceBuilder implements ReferenceBuilder {
        public Object ref(Object in) {
            throw new IllegalStateException("please provide your own ReferenceBuilder");
        }
    }
    public static class NoopReferenceDetector implements ReferenceDetector {

        public boolean isReference(Object in) {
            throw new IllegalStateException("please provide your own ReferenceDetector");
        }
    }
    public static class NoopIDExtractor implements IDExtractor {

        public Identity getId(Object in) {
            throw new IllegalStateException("please provide your own IDExtractor");
        }
    }
    Class<? extends ReferenceBuilder> referenceBuilder() default NoopReferenceBuilder.class;
    Class<? extends ReferenceDetector> referenceDetector() default NoopReferenceDetector.class;
    Class<? extends IDExtractor> idExtractor() default NoopIDExtractor.class;
}
