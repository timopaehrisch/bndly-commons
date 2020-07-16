package org.bndly.schema.api.exception;

/*-
 * #%L
 * Schema API
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

import org.bndly.schema.model.UniqueConstraint;

public class ConstraintViolationException extends SchemaException {
    private final UniqueConstraint constraint;

	public ConstraintViolationException(String message, Throwable cause) {
		super(message, cause);
		constraint = null;
	}

    public ConstraintViolationException(UniqueConstraint constraint, String message, Throwable cause) {
        super(message, cause);
        this.constraint = constraint;
    }

    public UniqueConstraint getConstraint() {
        return constraint;
    }
    
    
}
