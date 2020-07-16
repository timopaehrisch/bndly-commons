package org.bndly.schema.impl.repository;

/*-
 * #%L
 * Schema Impl
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

import org.bndly.common.crypto.api.Base64Service;
import org.bndly.schema.api.repository.RepositoryImporter;
import org.bndly.schema.api.repository.PropertyNotFoundException;
import org.bndly.schema.api.repository.RepositoryException;
import org.bndly.schema.api.repository.NodeNotFoundException;
import org.bndly.schema.api.repository.RepositorySession;
import org.bndly.schema.api.repository.Property;
import org.bndly.schema.api.repository.NodeTypes;
import org.bndly.schema.api.repository.Node;
import org.bndly.common.data.io.ReplayableInputStream;
import org.bndly.common.json.model.JSArray;
import org.bndly.common.json.model.JSBoolean;
import org.bndly.common.json.model.JSMember;
import org.bndly.common.json.model.JSNull;
import org.bndly.common.json.model.JSNumber;
import org.bndly.common.json.model.JSObject;
import org.bndly.common.json.model.JSString;
import org.bndly.common.json.model.JSValue;
import org.bndly.common.json.parsing.JSONParser;
import org.bndly.common.json.parsing.ParsingException;
import org.bndly.common.json.serializing.JSONSerializer;
import org.bndly.common.json.serializing.JSONWriter;
import org.bndly.schema.api.Record;
import org.bndly.schema.api.RecordContext;
import org.bndly.schema.api.repository.RepositoryExporter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = {RepositoryImporter.class, RepositoryExporter.class})
@Designate(ocd = RepositoryImporterImpl.Configuration.class)
public class RepositoryImporterImpl implements RepositoryImporter, RepositoryExporter {
	
	@ObjectClassDefinition(
			name = "Repository Importer",
			description = "This importer imports JSON file structures into a repository"
	)
	public @interface Configuration {
	}
	
	private static final Logger LOG = LoggerFactory.getLogger(RepositoryImporterImpl.class);
	
	@Reference
	private Base64Service base64Service;

	private static interface Transformer {
		Object transform(JSValue value, Property.Type desiredType, RecordContext recordContext);
		void writeToJson(Property property, Object value, JSONWriter writer, Writer originalWriter) throws IOException;
	}
	
	private static interface FilePathStrategy {
		Path getFilePath(Node node, Path rootPath);
	}
	
	private static final FilePathStrategy ROOT_NODE_PATH_STRATEGY = new FilePathStrategy() {
		@Override
		public Path getFilePath(Node node, Path rootPath) {
			return rootPath.resolve("root.json");
		}
	};
	private static final FilePathStrategy NODE_PATH_STRATEGY = new FilePathStrategy() {
		@Override
		public Path getFilePath(Node node, Path rootPath) {
			Path p = rootPath;
			List<String> elements = node.getPath().getElementNames();
			for (int i = 0; i < elements.size(); i++) {
				String nodePathElement = elements.get(i);
				if (i == elements.size() - 1) {
					p = p.resolve(nodePathElement + ".json");
				} else {
					p = p.resolve(nodePathElement);
				}
			}
			return p;
		}
	};
	
	private final Transformer stringTransformer = new Transformer() {

		@Override
		public Object transform(JSValue value, Property.Type desiredType, RecordContext recordContext) {
			if (desiredType != Property.Type.STRING) {
				return null;
			}
			if (JSNull.class.isInstance(value)) {
				return null;
			} else if (JSString.class.isInstance(value)) {
				return ((JSString) value).getValue();
			} else {
				return null;
			}
		}

		@Override
		public void writeToJson(Property property, Object value, JSONWriter writer, Writer originalWriter) throws IOException {
			if (value == null) {
				writer.writeNull();
			} else {
				writer.writeString((String) value);
			}
		}
		
	};
	private final Transformer longTransformer = new Transformer() {

		@Override
		public Object transform(JSValue value, Property.Type desiredType, RecordContext recordContext) {
			if (desiredType != Property.Type.LONG) {
				return null;
			}
			if (JSNull.class.isInstance(value)) {
				return null;
			} else if (JSNumber.class.isInstance(value)) {
				return ((JSNumber) value).getValue().longValue();
			} else {
				return null;
			}
		}

		@Override
		public void writeToJson(Property property, Object value, JSONWriter writer, Writer originalWriter) throws IOException {
			if (value == null) {
				writer.writeNull();
			} else {
				writer.writeLong((long) value);
			}
		}
		
	};
	private final Transformer doubleTransformer = new Transformer() {

		@Override
		public Object transform(JSValue value, Property.Type desiredType, RecordContext recordContext) {
			if (desiredType != Property.Type.DOUBLE) {
				return null;
			}
			if (JSNull.class.isInstance(value)) {
				return null;
			} else if (JSNumber.class.isInstance(value)) {
				return ((JSNumber) value).getValue().doubleValue();
			} else {
				return null;
			}
		}

		@Override
		public void writeToJson(Property property, Object value, JSONWriter writer, Writer originalWriter) throws IOException {
			if (value == null) {
				writer.writeNull();
			} else {
				writer.writeDouble((double) value);
			}
		}
	};
	private final Transformer decimalTransformer = new Transformer() {

		@Override
		public Object transform(JSValue value, Property.Type desiredType, RecordContext recordContext) {
			if (desiredType != Property.Type.DECIMAL) {
				return null;
			}
			if (JSNull.class.isInstance(value)) {
				return null;
			} else if (JSNumber.class.isInstance(value)) {
				return ((JSNumber) value).getValue();
			} else {
				return null;
			}
		}

		@Override
		public void writeToJson(Property property, Object value, JSONWriter writer, Writer originalWriter) throws IOException {
			if (value == null) {
				writer.writeNull();
			} else {
				writer.writeDecimal((BigDecimal) value);
			}
		}
	};
	private final Transformer dateTransformer = new Transformer() {

		@Override
		public Object transform(JSValue value, Property.Type desiredType, RecordContext recordContext) {
			if (desiredType != Property.Type.DATE) {
				return null;
			}
			if (JSNull.class.isInstance(value)) {
				return null;
			} else if (JSNumber.class.isInstance(value)) {
				return new Date(((JSNumber) value).getValue().longValue());
			} else {
				return null;
			}
		}

		@Override
		public void writeToJson(Property property, Object value, JSONWriter writer, Writer originalWriter) throws IOException {
			if (value == null) {
				writer.writeNull();
			} else {
				writer.writeLong(((Date) value).getTime());
			}
		}
	};
	private final Transformer booleanTransformer = new Transformer() {

		@Override
		public Object transform(JSValue value, Property.Type desiredType, RecordContext recordContext) {
			if (desiredType != Property.Type.BOOLEAN) {
				return null;
			}
			if (JSNull.class.isInstance(value)) {
				return null;
			} else if (JSBoolean.class.isInstance(value)) {
				return ((JSBoolean) value).isValue();
			} else {
				return null;
			}
		}

		@Override
		public void writeToJson(Property property, Object value, JSONWriter writer, Writer originalWriter) throws IOException {
			if (value == null) {
				writer.writeNull();
			} else {
				if ((Boolean) value) {
					writer.writeTrue();
				} else {
					writer.writeFalse();
				}
			}
		}
	};
	private final Transformer binaryTransformer = new Transformer() {

		@Override
		public Object transform(JSValue value, Property.Type desiredType, RecordContext recordContext) {
			if (desiredType != Property.Type.BINARY) {
				return null;
			}
			if (JSNull.class.isInstance(value)) {
				return null;
			} else if (JSString.class.isInstance(value)) {
				String base64String = ((JSString) value).getValue();
				if (base64Service == null) {
					return null;
				}
				byte[] bytes = base64Service.base64Decode(base64String);
				return ReplayableInputStream.newInstance(bytes);
			} else {
				return null;
			}
		}

		@Override
		public void writeToJson(Property property, Object value, JSONWriter writer, Writer originalWriter) throws IOException {
			if (value == null || base64Service == null) {
				writer.writeNull();
			} else {
				originalWriter.write("\"");
				base64Service.base64EncodeStream((InputStream) value, originalWriter);
				originalWriter.write("\"");
			}
		}
	};
	private final Transformer entityTransformer = new Transformer() {

		@Override
		public Object transform(JSValue value, Property.Type desiredType, RecordContext recordContext) {
			if (recordContext == null) {
				return null;
			}
			if (desiredType != Property.Type.ENTITY) {
				return null;
			}
			if (JSNull.class.isInstance(value)) {
				return null;
			} else if (JSString.class.isInstance(value)) {
				String string = ((JSString) value).getValue();
				int i = string.indexOf(";");
				if (i < 1) {
					return null;
				}
				String typeName = string.substring(0, i);
				String id = string.substring(i + 1);
				try {
					Long idLong = Long.valueOf(id);
					return recordContext.create(typeName, idLong);
				} catch (NumberFormatException e) {
					return null;
				}
			} else {
				return null;
			}
		}

		@Override
		public void writeToJson(Property property, Object value, JSONWriter writer, Writer originalWriter) throws IOException {
			if (value == null || base64Service == null) {
				writer.writeNull();
			} else {
				Record record = (Record) value;
				if (record.getId() == null) {
					writer.writeNull();
				} else {
					writer.writeString(record.getType().getName() + ";" + record.getId());
				}
			}
		}
	};

	private static final JSArray EMPTY_JSON_ARRAY = new JSArray();
	private static final JSObject EMPTY_JSON_OBJECT = new JSObject();
	private static final JSONSerializer JSON_SERIALIZER = new JSONSerializer();
	
	@Override
	public void importRepositoryData(final Path rootFile, final RepositorySession repositorySession) throws RepositoryException {
		importFile(rootFile, null, null, repositorySession);
	}

	@Override
	public void exportRepositoryData(Path outputFolder, RepositorySession repositorySession) throws RepositoryException {
		try {
			if (Files.notExists(outputFolder)) {
				Files.createDirectory(outputFolder);
			}
			Node root = repositorySession.getRoot();
			exportToFile(root, ROOT_NODE_PATH_STRATEGY.getFilePath(root, outputFolder), outputFolder);
		} catch (IOException e) {
			throw new RepositoryException("failed to export repository: " + e.getMessage(), e);
		}
	}
	
	private void exportToFile(Node node, Path pathToFile, Path outputFolder) throws IOException, RepositoryException {
		Files.deleteIfExists(pathToFile);
		Path parent = pathToFile.getParent();
		if (parent != null) {
			if (Files.notExists(parent)) {
				Files.createDirectories(parent);
			}
		}
		Files.createFile(pathToFile);
		Iterator<Node> childNodes = node.getChildren();
		JSObject jsonChildNodes = new JSObject();
		boolean hasChildren = false;
		while (childNodes.hasNext()) {
			Node next = childNodes.next();
			hasChildren = true;
			jsonChildNodes.createMember(next.getName()).setValue(next.getType().equals(NodeTypes.ARRAY) ? EMPTY_JSON_ARRAY : EMPTY_JSON_OBJECT );
			Path childNodePath = NODE_PATH_STRATEGY.getFilePath(next, outputFolder);
			exportToFile(next, childNodePath, outputFolder);
		}
		
		try (Writer writer = Files.newBufferedWriter(pathToFile, Charset.forName("UTF-8"), StandardOpenOption.WRITE)) {
			JSONWriter jsonWriter = new JSONWriter(writer);
			jsonWriter
					.writeObjectStart()
					.writeString("type").writeColon().writeString(node.getType());
			Iterator<Property> properties = node.getProperties();
			if (properties.hasNext()) {
				jsonWriter
						.writeComma().writeString("properties").writeColon().writeObjectStart();
				boolean first = true;
				while (properties.hasNext()) {
					Property property = properties.next();
					if (!first) {
						jsonWriter.writeComma();
					}
					jsonWriter.writeString(property.getName() + "@" + property.getType().toString()).writeColon();
					exportPropertyValueToJson(property, jsonWriter, writer);
					first = false;
				}
				jsonWriter.writeObjectEnd();
			}
			if (hasChildren) {
				jsonWriter.writeComma().writeString("children").writeColon();
				JSON_SERIALIZER.serialize(jsonChildNodes, writer);
			}
			jsonWriter.writeObjectEnd();
			writer.flush();
		}
	}
	
	private void exportPropertyValueToJson(Property property, JSONWriter jsonWriter, Writer writer) throws RepositoryException, IOException {
		Property.Type type = property.getType();
		boolean isMulti = property.isMultiValued();
		Transformer transformer;
		switch (type) {
			case BINARY:
				transformer = binaryTransformer;
				break;
			case BOOLEAN:
				transformer = booleanTransformer;
				break;
			case DATE:
				transformer = dateTransformer;
				break;
			case DECIMAL:
				transformer = decimalTransformer;
				break;
			case DOUBLE:
				transformer = doubleTransformer;
				break;
			case ENTITY:
				transformer = entityTransformer;
				break;
			case LONG:
				transformer = longTransformer;
				break;
			case STRING:
				transformer = stringTransformer;
				break;
			default:
				throw new RepositoryException("unsupported property type: " + type);
		}
		if (isMulti) {
			jsonWriter.writeArrayStart();
			boolean first = true;
			Object[] values = property.getValues();
			if (values != null) {
				for (Object value : values) {
					if (!first) {
						jsonWriter.writeComma();
					}
					transformer.writeToJson(property, value, jsonWriter, writer);
					first = false;
				}
			}
			jsonWriter.writeArrayEnd();
		} else {
			transformer.writeToJson(property, property.getValue(), jsonWriter, writer);
		}
	}
	
	private void importFile(Path file, String nodeName, Node parentNode, final RepositorySession repositorySession) throws RepositoryException {
		try {
			// iterate over the json files and not the folders
			JSObject jsonRoot = null;
			try (InputStream is = Files.newInputStream(file, StandardOpenOption.READ)) {
				try {
					JSValue parsed = new JSONParser().parse(is, "UTF-8");
					if (JSObject.class.isInstance(parsed)) {
						jsonRoot = (JSObject) parsed;
					}
				} catch (ParsingException e) {
					LOG.error("failed to parse file with data for node " + nodeName + ": " + e.getMessage(), e);
				}
			} catch (NoSuchFileException e) {
				LOG.warn("skipping import of node {} at path {} because the source file was missing.", nodeName, parentNode != null ? parentNode.getPath() : null);
				return;
			}
			if (jsonRoot == null) {
				return;
			}
			Node targetNode;
			if (nodeName == null) {
				targetNode = repositorySession.getRoot();
			} else {
				if (parentNode == null) {
					throw new RepositoryException("missing parent node");
				}
				try {
					targetNode = parentNode.getChild(nodeName);
				} catch (NodeNotFoundException e) {
					String childNodeType = jsonRoot.getMemberStringValue("type");
					if (childNodeType == null) {
						throw new RepositoryException(
							"could not find node " + nodeName + " in " + parentNode.getPath().toString() + " and there was no node type in the import data"
						);
					}
					targetNode = parentNode.createChild(nodeName, childNodeType);
				}
			}
			importToNode(jsonRoot, targetNode, repositorySession, file);
		} catch (IOException e) {
			throw new RepositoryException("failed to iterate import data from path " + file.toString(), e);
		}
	}
	
	private void importToNode(final JSObject jsonRoot, final Node target, final RepositorySession repositorySession, Path currentFile) throws RepositoryException {
		// evaluate data after the file has been read and closed
		if (jsonRoot != null) {
			String type = jsonRoot.getMemberStringValue("type");
			if (type != null) {
				JSObject properties = jsonRoot.getMemberValue("properties", JSObject.class);
				importPropertiesToNode(properties, target);
				JSObject children = jsonRoot.getMemberValue("children", JSObject.class);
				importChildren(children, target, currentFile, repositorySession);
			}
		}
	}
	
	private void importPropertiesToNode(JSObject properties, Node target) throws RepositoryException {
		if (properties == null) {
			return;
		}
		Set<JSMember> members = properties.getMembers();
		if (members == null || members.isEmpty()) {
			return;
		}
		for (JSMember member : members) {
			String propertyName = member.getName().getValue();
			JSValue propertyValue = member.getValue();
			importProperty(propertyName, propertyValue, target);
		}
	}
	
	private void importProperty(String propertyName, JSValue propertyValue, Node target) throws RepositoryException {
		RepositorySession repositorySession = target.getRepositorySession();
		RecordContext recordContext = null;
		if (RepositorySessionImpl.class.isInstance(repositorySession)) {
			recordContext = ((RepositorySessionImpl) repositorySession).getRecordContext();
		}
		Property propertyTmp;
		Property.Type type;
		final boolean isMulti = JSArray.class.isInstance(propertyValue);
		final String propertyNameTidy = tidyUpPropertyName(propertyName);
		try {
			propertyTmp = target.getProperty(propertyNameTidy);
			type = propertyTmp.getType();
		} catch (PropertyNotFoundException ex) {
			type = mapPropertyNameToType(propertyName, mapPropertyValueToType(propertyValue));
			if (isMulti) {
				propertyTmp = target.createMultiProperty(propertyNameTidy, type);
			} else {
				propertyTmp = target.createProperty(propertyNameTidy, type);
			}
		}
		final Object value;
		final Object[] values;
		switch (type) {
			case BINARY:
				if (isMulti) {
					values = new Object[((JSArray)propertyValue).size()];
					for (int i = 0; i < ((JSArray)propertyValue).size(); i++) {
						JSValue item = ((JSArray)propertyValue).getItems().get(i);
						values[i] = binaryTransformer.transform(item, type, recordContext);
					}
					value = values;
				} else {
					value = binaryTransformer.transform(propertyValue, type, recordContext);
				}
				break;
			case BOOLEAN:
				if (isMulti) {
					values = new Object[((JSArray)propertyValue).size()];
					for (int i = 0; i < ((JSArray)propertyValue).size(); i++) {
						JSValue item = ((JSArray)propertyValue).getItems().get(i);
						values[i] = booleanTransformer.transform(item, type, recordContext);
					}
					value = values;
				} else {
					value = booleanTransformer.transform(propertyValue, type, recordContext);
				}
				break;
			case DATE:
				if (isMulti) {
					values = new Object[((JSArray)propertyValue).size()];
					for (int i = 0; i < ((JSArray)propertyValue).size(); i++) {
						JSValue item = ((JSArray)propertyValue).getItems().get(i);
						values[i] = dateTransformer.transform(item, type, recordContext);
					}
					value = values;
				} else {
					value = dateTransformer.transform(propertyValue, type, recordContext);
				}
				break;
			case DECIMAL:
				if (isMulti) {
					values = new Object[((JSArray)propertyValue).size()];
					for (int i = 0; i < ((JSArray)propertyValue).size(); i++) {
						JSValue item = ((JSArray)propertyValue).getItems().get(i);
						values[i] = decimalTransformer.transform(item, type, recordContext);
					}
					value = values;
				} else {
					value = decimalTransformer.transform(propertyValue, type, recordContext);
				}
				break;
			case DOUBLE:
				if (isMulti) {
					values = new Object[((JSArray)propertyValue).size()];
					for (int i = 0; i < ((JSArray)propertyValue).size(); i++) {
						JSValue item = ((JSArray)propertyValue).getItems().get(i);
						values[i] = doubleTransformer.transform(item, type, recordContext);
					}
					value = values;
				} else {
					value = doubleTransformer.transform(propertyValue, type, recordContext);
				}
				break;
			case ENTITY:
				if (isMulti) {
					values = new Object[((JSArray)propertyValue).size()];
					for (int i = 0; i < ((JSArray)propertyValue).size(); i++) {
						JSValue item = ((JSArray)propertyValue).getItems().get(i);
						values[i] = entityTransformer.transform(item, type, recordContext);
					}
					value = values;
				} else {
					value = entityTransformer.transform(propertyValue, type, recordContext);
				}
				break;
			case LONG:
				if (isMulti) {
					values = new Object[((JSArray)propertyValue).size()];
					for (int i = 0; i < ((JSArray)propertyValue).size(); i++) {
						JSValue item = ((JSArray)propertyValue).getItems().get(i);
						values[i] = longTransformer.transform(item, type, recordContext);
					}
					value = values;
				} else {
					value = longTransformer.transform(propertyValue, type, recordContext);
				}
				break;
			case STRING:
				if (isMulti) {
					values = new Object[((JSArray)propertyValue).size()];
					for (int i = 0; i < ((JSArray)propertyValue).size(); i++) {
						JSValue item = ((JSArray)propertyValue).getItems().get(i);
						values[i] = stringTransformer.transform(item, type, recordContext);
					}
					value = values;
				} else {
					value = stringTransformer.transform(propertyValue, type, recordContext);
				}
				break;
			default:
				throw new RepositoryException("unsupported property type: " + type);
		}
		if (isMulti) {
			propertyTmp.setValues(((Object[]) value));
		} else {
			propertyTmp.setValue(value);
		}
	}
	
	private Property.Type mapPropertyNameToType(String propertyName, Property.Type defaultType) {
		int i = propertyName.indexOf("@");
		if (i > -1) {
			String typeHint = propertyName.substring(i + 1);
			Property.Type type = Property.Type.valueOf(typeHint);
			if (type == null) {
				return defaultType;
			} else {
				return type;
			}
		}
		return defaultType;
	}
	
	private String tidyUpPropertyName(String propertyName) {
		int i = propertyName.indexOf("@");
		if (i > -1) {
			return propertyName.substring(0, i);
		}
		return propertyName;
	}
	
	private Property.Type mapPropertyValueToType(JSValue propertyValue) {
		if (JSNull.class.isInstance(propertyValue)) {
			return null;
		} else if (JSArray.class.isInstance(propertyValue)) {
			JSArray array = (JSArray) propertyValue;
			Iterator<JSValue> iterator = array.iterator();
			if (!iterator.hasNext()) {
				return null;
			}
			return mapPropertyValueToType(iterator.next());
		} else if (JSString.class.isInstance(propertyValue)) {
			return Property.Type.STRING;
		} else if (JSNumber.class.isInstance(propertyValue)) {
			JSNumber number = (JSNumber) propertyValue;
			BigDecimal val = number.getValue();
			if (val.scale() > 0) {
				return Property.Type.DECIMAL;
			} else {
				return Property.Type.LONG;
			}
		} else if (JSBoolean.class.isInstance(propertyValue)) {
			return Property.Type.BOOLEAN;
		} else {
			return null;
		}
	}
	
	private void importChildren(JSObject children, Node parentNode, Path parentImportFile, final RepositorySession repositorySession) throws RepositoryException {
		if (children == null) {
			return;
		}
		Set<JSMember> members = children.getMembers();
		if (members == null) {
			return;
		}
		for (JSMember member : members) {
			final String childName = member.getName().getValue();
			JSValue childValue = member.getValue();
			boolean isArray = JSArray.class.isInstance(childValue);
			Path childPath = getChildPath(parentNode, parentImportFile, childName);
			if (isArray) {
				if (Files.notExists(childPath)) {
					// iterate the files in a folder
					final Path itemFolder = parentImportFile.resolveSibling(childName);
					if (Files.isDirectory(itemFolder)) {
						Node itemParentTmp;
						try {
							itemParentTmp = parentNode.getChild(childName);
						} catch (NodeNotFoundException e) {
							itemParentTmp = parentNode.createChild(childName, NodeTypes.ARRAY);
						}
						final Node itemParent = itemParentTmp;
						// iterate the children and import them to itemParent

						try {
							Files.walkFileTree(itemFolder, new FileVisitor<Path>() {

								@Override
								public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
									if (dir.equals(itemFolder)) {
										return FileVisitResult.CONTINUE;
									} else {
										return FileVisitResult.SKIP_SUBTREE;
									}
								}

								@Override
								public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
									String nodeName = getNodeNameFromFileName(file);
									if (nodeName == null) {
										return FileVisitResult.CONTINUE;
									}
									try {
										importFile(file, nodeName, itemParent, repositorySession);
									} catch (RepositoryException ex) {
										throw new IOException("could not import data from " + file.toString(), ex);
									}
									return FileVisitResult.CONTINUE;
								}

								@Override
								public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
									return FileVisitResult.TERMINATE;
								}

								@Override
								public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
									return FileVisitResult.CONTINUE;
								}
							});
						} catch (IOException e) {
							throw new RepositoryException("failed to import items of array node", e);
						}
					}
				} else {
					// business as usual. this means there is a file, that defines the order of the items
					importFile(childPath, childName, parentNode, repositorySession);
				}
			} else {
				importFile(childPath, childName, parentNode, repositorySession);
			}
		}
	}

	private Path getChildPath(Node parentNode, Path parentImportFile, final String childNodeName) {
		Path childPath;
		if (NodeTypes.ROOT.equals(parentNode.getType())) {
			// the child will be next to the current file
			childPath = parentImportFile.resolveSibling(childNodeName + ".json");
		} else {
			// the child will be in a subfolder
			childPath = parentImportFile.resolveSibling(parentNode.getName()).resolve(childNodeName + ".json");
		}
		return childPath;
	}
	
	private String getNodeNameFromFileName(Path path) {
		Path file = path.getFileName();
		if (file == null) {
			return null;
		}
		String fileNameString = file.toString();
		if (fileNameString == null || !fileNameString.endsWith(".json")) {
			return null;
		}
		fileNameString = fileNameString.substring(0, fileNameString.length() - ".json".length());
		return fileNameString;
	}
	
	public void setBase64Service(Base64Service base64Service) {
		this.base64Service = base64Service;
	}
}
