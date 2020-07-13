package org.bndly.common.data.api;

/*-
 * #%L
 * Data API
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

import org.bndly.common.data.io.ReplayableInputStream;
import java.util.Date;

public class SimpleData implements Data, ChangeableData {

	public static interface LazyLoader {

		ReplayableInputStream getBytes();
	}

	private String contentType;
	private String name;
	private Date createdOn;
	private Date updatedOn;
	private ReplayableInputStream inputStream;
	private final LazyLoader lazyLoader;

	public SimpleData(LazyLoader lazyLoader) {
		this.lazyLoader = lazyLoader;
	}

	public LazyLoader getLazyLoader() {
		return lazyLoader;
	}

	@Override
	public String getContentType() {
		return contentType;
	}

	@Override
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	@Override
	public ReplayableInputStream getInputStream() {
		if (inputStream == null && lazyLoader != null) {
			inputStream = lazyLoader.getBytes();
		}
		return inputStream;
	}

	@Override
	public void setInputStream(ReplayableInputStream inputStream) {
		this.inputStream = inputStream;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public Date getCreatedOn() {
		return createdOn;
	}

	@Override
	public void setCreatedOn(Date createdOn) {
		this.createdOn = createdOn;
	}

	@Override
	public Date getUpdatedOn() {
		return updatedOn;
	}

	@Override
	public void setUpdatedOn(Date updatedOn) {
		this.updatedOn = updatedOn;
	}
}
