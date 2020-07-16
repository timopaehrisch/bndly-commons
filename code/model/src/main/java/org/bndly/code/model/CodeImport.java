package org.bndly.code.model;

/*-
 * #%L
 * Code Model
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


public class CodeImport extends CodeElement {
	private String typeName;
	private String importedFromWithin;
	
	public CodeImport(String className) {
		super();
		this.typeName = className;
	}
	public CodeImport() {
		super();
	}

	public String getTypeName() {
		return typeName;
	}
	public void setTypeName(String typeName) {
		this.typeName = typeName;
	}
	public String getImportedFromWithin() {
		return importedFromWithin;
	}
	public void setImportedFromWithin(String importedFromWithin) {
		this.importedFromWithin = importedFromWithin;
	}
}
