package org.bndly.rest.swagger.model;

/*-
 * #%L
 * REST Swagger Integration
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

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class Parameter {

	public static enum Location {
		body, query, path, formData, header
	}
	
	public static enum Type {
		STRING("string"), 
		NUMBER("number"), 
		INTEGER("integer"), 
		BOOLEAN("boolean"), 
		ARRAY("array"), 
		OBJECT("object"), 
		FILE("file");
		
		private final String asSwaggerString;
		
		private Type(String asSwaggerString) {
			this.asSwaggerString = asSwaggerString;
		}

		public String getAsSwaggerString() {
			return asSwaggerString;
		}
		
	}
	
	private final String name;
	private final Location in;
	private Type type;
	private String description;
	private Boolean required;
	private SchemaReference schema;

	public Parameter(String name, Location in) {
		this.name = name;
		this.in = in;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Boolean getRequired() {
		return required;
	}

	public void setRequired(Boolean required) {
		this.required = required;
	}

	public SchemaReference getSchema() {
		return schema;
	}

	public void setSchema(SchemaReference schema) {
		this.schema = schema;
	}

	public String getName() {
		return name;
	}

	public Location getIn() {
		return in;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}
	
}
