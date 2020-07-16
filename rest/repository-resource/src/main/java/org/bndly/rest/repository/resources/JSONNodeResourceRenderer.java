package org.bndly.rest.repository.resources;

/*-
 * #%L
 * REST Repository Resource
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

import org.bndly.common.converter.api.Converter;
import org.bndly.common.crypto.api.Base64Service;
import org.bndly.common.json.serializing.JSONWriter;
import org.bndly.rest.api.ContentType;
import org.bndly.rest.api.Context;
import org.bndly.rest.api.Resource;
import org.bndly.rest.api.ResourceRenderer;
import org.bndly.rest.api.ResourceURI;
import org.bndly.rest.repository.resources.json.BinaryJSONPropertyWriter;
import org.bndly.rest.repository.resources.json.BinaryMultiJSONPropertyWriter;
import org.bndly.rest.repository.resources.json.BooleanJSONPropertyWriter;
import org.bndly.rest.repository.resources.json.BooleanMultiJSONPropertyWriter;
import org.bndly.rest.repository.resources.json.DateJSONPropertyWriter;
import org.bndly.rest.repository.resources.json.DateMultiJSONPropertyWriter;
import org.bndly.rest.repository.resources.json.DecimalJSONPropertyWriter;
import org.bndly.rest.repository.resources.json.DecimalMultiJSONPropertyWriter;
import org.bndly.rest.repository.resources.json.DoubleJSONPropertyWriter;
import org.bndly.rest.repository.resources.json.DoubleMultiJSONPropertyWriter;
import org.bndly.rest.repository.resources.json.EntityJSONPropertyWriter;
import org.bndly.rest.repository.resources.json.EntityMultiJSONPropertyWriter;
import org.bndly.rest.repository.resources.json.JSONPropertyWriter;
import org.bndly.rest.repository.resources.json.LongJSONPropertyWriter;
import org.bndly.rest.repository.resources.json.LongMultiJSONPropertyWriter;
import org.bndly.rest.repository.resources.json.StringJSONPropertyWriter;
import org.bndly.rest.repository.resources.json.StringMultiJSONPropertyWriter;
import org.bndly.schema.api.repository.Node;
import org.bndly.schema.api.repository.Property;
import org.bndly.schema.api.repository.RepositoryException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = ResourceRenderer.class, immediate = true)
public class JSONNodeResourceRenderer implements ResourceRenderer {
	private final Map<Property.Type, JSONPropertyWriter> writers = new HashMap<>();
	private final Map<Property.Type, JSONPropertyWriter> multiWriters = new HashMap<>();
	
	@Reference
	private Base64Service base64Service;
	@Reference(target = "(component.name=org.bndly.common.converter.impl.DateStringConverter)")
	private Converter<Date, String> dateConverter;
	
	@Activate
	public void activate() {
		writers.put(Property.Type.BINARY, new BinaryJSONPropertyWriter(base64Service));
		writers.put(Property.Type.BOOLEAN, new BooleanJSONPropertyWriter());
		writers.put(Property.Type.DATE, new DateJSONPropertyWriter(dateConverter));
		writers.put(Property.Type.DECIMAL, new DecimalJSONPropertyWriter());
		writers.put(Property.Type.DOUBLE, new DoubleJSONPropertyWriter());
		writers.put(Property.Type.ENTITY, new EntityJSONPropertyWriter());
		writers.put(Property.Type.LONG, new LongJSONPropertyWriter());
		writers.put(Property.Type.STRING, new StringJSONPropertyWriter());
		
		multiWriters.put(Property.Type.BINARY, new BinaryMultiJSONPropertyWriter(base64Service));
		multiWriters.put(Property.Type.BOOLEAN, new BooleanMultiJSONPropertyWriter());
		multiWriters.put(Property.Type.DATE, new DateMultiJSONPropertyWriter(dateConverter));
		multiWriters.put(Property.Type.DECIMAL, new DecimalMultiJSONPropertyWriter());
		multiWriters.put(Property.Type.DOUBLE, new DoubleMultiJSONPropertyWriter());
		multiWriters.put(Property.Type.ENTITY, new EntityMultiJSONPropertyWriter());
		multiWriters.put(Property.Type.LONG, new LongMultiJSONPropertyWriter());
		multiWriters.put(Property.Type.STRING, new StringMultiJSONPropertyWriter());
	}
	
	@Deactivate
	public void deactivate() {
		writers.clear();
		multiWriters.clear();
	}
	
	@Override
	public boolean supports(Resource resource, Context context) {
		if (!NodeResource.class.isInstance(resource)) {
			return false;
		}
		if (context.getDesiredContentType() == null) {
			return false;
		}
		return ContentType.JSON.getName().equals(context.getDesiredContentType().getName());
	}
	
	@Override
	public void render(Resource resource, Context context) throws IOException {
		NodeResource nodeResource = (NodeResource) resource;
		Node node = nodeResource.getNode();
		// render it
		context.setOutputContentType(ContentType.JSON, "UTF-8");
		List<ResourceURI.Selector> selectors = resource.getURI().getSelectors();
		boolean renderFlat = false;
		boolean infiniteDepth = true;
		int maxDepth = -1;
		if (selectors != null) {
			for (ResourceURI.Selector selector : selectors) {
				if ("flat".equals(selector.getName())) {
					renderFlat = true;
				} else if ("infinite".equals(selector.getName())) {
					infiniteDepth = true;
				} else if (selector.getName().startsWith("maxdepth")) {
					try {
						maxDepth = Integer.valueOf(selector.getName().substring("maxdepth".length()));
						infiniteDepth = false;
					} catch (NumberFormatException e) {
						// doesn't matter
					}
				}
			}
		}
		OutputStream outputStream = context.getOutputStream();
		try {
			if (renderFlat) {
				renderFlatNodeJson(0, infiniteDepth, maxDepth ,node, context, outputStream);
			} else {
				renderNodeJson(0, infiniteDepth, maxDepth, node, context, outputStream);
			}
		} catch (RepositoryException ex) {
			throw new IllegalStateException("failed to render node as json", ex);
		}
	}
	
	private boolean shouldRenderChildren(int depth, boolean infinite, int maxDepth) {
		if (infinite) {
			return true;
		}
		return depth < maxDepth;
	}
	
	private void renderFlatNodeJson(int depth, boolean infinite, int maxDepth, Node node, Context context, OutputStream outputStream) throws IOException, RepositoryException {
		try (JSONWriter writer = new JSONWriter(new OutputStreamWriter(outputStream, "UTF-8"))) {
			renderFlatNodeJson(depth, infinite, maxDepth, node, writer);
			writer.flush();
		}
	}
	
	private void renderNodeJson(int depth, boolean infinite, int maxDepth, Node node, Context context, OutputStream outputStream) throws IOException, RepositoryException {
		try (JSONWriter writer = new JSONWriter(new OutputStreamWriter(outputStream, "UTF-8"))) {
			renderNodeJson(depth, infinite, maxDepth, node, writer);
			writer.flush();
		}
	}

	private void renderFlatNodeJson(int depth, boolean infinite, int maxDepth, Node node, JSONWriter writer) throws RepositoryException, IOException {
		writer.writeObjectStart()
				.writeString("type").writeColon().writeString(node.getType()).writeComma()
				.writeString("path").writeColon().writeString(node.getPath().toString())
				.writeComma().writeString("properties").writeColon();
		renderFlatNodePropertiesJson(node, writer);
		if (shouldRenderChildren(depth, infinite, maxDepth)) {
			writer.writeComma().writeString("children").writeColon();
			renderFlatNodeChildrenJson(depth, infinite, maxDepth, node, writer);
		}
		writer.writeObjectEnd();
	}
	
	private void renderNodeJson(int depth, boolean infinite, int maxDepth, Node node, JSONWriter writer) throws RepositoryException, IOException {
		writer.writeObjectStart()
				.writeString("type").writeColon().writeString(node.getType()).writeComma()
				.writeString("path").writeColon().writeString(node.getPath().toString())
				.writeComma().writeString("properties").writeColon();
		renderNodePropertiesJson(node, writer);
		if (shouldRenderChildren(depth, infinite, maxDepth)) {
			writer.writeComma().writeString("children").writeColon();
			renderNodeChildrenJson(depth, infinite, maxDepth, node, writer);
		}
		writer.writeObjectEnd();
	}
	
	private void renderFlatNodePropertiesJson(Node node, JSONWriter writer) throws RepositoryException, IOException {
		writer.writeObjectStart();
		Iterator<Property> propertiesIter = node.getProperties();
		boolean isFirst = true;
		while (propertiesIter.hasNext()) {
			Property property = propertiesIter.next();
			if (!isFirst) {
				writer.writeComma();
			}
			writer.writeString(property.getName()).writeColon();
			if (property.isMultiValued()) {
				JSONPropertyWriter propWriter = multiWriters.get(property.getType());
				writer.writeArrayStart();
				propWriter.write(property, writer);
				writer.writeArrayEnd();
			} else {
				JSONPropertyWriter propWriter = writers.get(property.getType());
				propWriter.write(property, writer);
			}
			isFirst = false;
		}
		writer.writeObjectEnd();
	}
	
	private void renderNodePropertiesJson(Node node, JSONWriter writer) throws RepositoryException, IOException {
		writer.writeObjectStart();
		Iterator<Property> propertiesIter = node.getProperties();
		boolean isFirst = true;
		while (propertiesIter.hasNext()) {
			Property property = propertiesIter.next();
			if (!isFirst) {
				writer.writeComma();
			}
			writer.writeString(property.getName()).writeColon().writeObjectStart().writeString("type").writeColon().writeString(property.getType().toString());
			if (property.isMultiValued()) {
				JSONPropertyWriter propWriter = multiWriters.get(property.getType());
				writer.writeComma().writeString("value").writeColon().writeArrayStart();
				propWriter.write(property, writer);
				writer.writeArrayEnd();
			} else {
				JSONPropertyWriter propWriter = writers.get(property.getType());
				writer.writeComma().writeString("value").writeColon();
				propWriter.write(property, writer);
			}
			writer.writeObjectEnd();
			isFirst = false;
		}
		writer.writeObjectEnd();
	}

	private void renderFlatNodeChildrenJson(int depth, boolean infinite, int maxDepth, Node node, JSONWriter writer) throws RepositoryException, IOException {
		writer.writeArrayStart();
		Iterator<Node> childrenIter = node.getChildren();
		int newDepth = depth + 1;
		boolean isFirst = true;
		while (childrenIter.hasNext()) {
			Node childNode = childrenIter.next();
			if (!isFirst) {
				writer.writeComma();
			}
			renderFlatNodeJson(newDepth, infinite, maxDepth, childNode, writer);
			isFirst = false;
		}
		writer.writeArrayEnd();
	}
	
	private void renderNodeChildrenJson(int depth, boolean infinite, int maxDepth, Node node, JSONWriter writer) throws RepositoryException, IOException {
		writer.writeArrayStart();
		Iterator<Node> childrenIter = node.getChildren();
		int newDepth = depth + 1;
		boolean isFirst = true;
		while (childrenIter.hasNext()) {
			Node childNode = childrenIter.next();
			if (!isFirst) {
				writer.writeComma();
			}
			renderNodeJson(newDepth, infinite, maxDepth, childNode, writer);
			isFirst = false;
		}
		writer.writeArrayEnd();
	}
}
