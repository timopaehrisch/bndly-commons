package org.bndly.rest.controller.api;

/*-
 * #%L
 * REST Annotation API
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

import org.bndly.rest.api.StatusWriter;
import org.bndly.rest.controller.api.DocumentationInfo.ResponseInfo;
import java.lang.reflect.Type;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class ResponseInfoWrapper implements DocumentationInfo.ResponseInfo {
	private final ResponseInfo wrapped;

	public ResponseInfoWrapper(ResponseInfo wrapped) {
		if (wrapped == null) {
			throw new IllegalArgumentException("wrapped response info is not allowed to be null");
		}
		this.wrapped = wrapped;
	}
	
	@Override
	public StatusWriter.Code getCode() {
		return wrapped.getCode();
	}

	@Override
	public String getDescription() {
		return wrapped.getDescription();
	}

	@Override
	public Type getJavaType() {
		return wrapped.getJavaType();
	}
	
}
