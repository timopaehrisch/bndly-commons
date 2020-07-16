package org.bndly.schema.beans;

/*-
 * #%L
 * Maven Schema Bean Generator
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

import static org.bndly.schema.beans.SchemaBeanGeneratorMojo.upperCaseFirstLetter;

import org.bndly.code.common.CodeGenerationContext;
import org.bndly.code.model.CodeBlock;
import org.bndly.code.model.CodeBracket;
import org.bndly.code.model.CodeLine;
import org.bndly.code.renderer.ImportResolver;
import static org.bndly.schema.beans.SchemaServiceImplGeneratorMojo.SERVICE_IMPL_SUFFIX;
import org.bndly.schema.model.Schema;
import org.bndly.schema.model.Type;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Generates Service Layer Client Interfaces from Schema
 */
@Mojo(
		name = "generateServices"
)
public class SchemaServiceInterfaceGeneratorMojo extends AbstractSchemaBasedBeanGeneratorMojo {

	private static final String GENERIC_RESOURCE_SERVICE = "org.bndly.common.service.shared.api.GenericResourceService";

	/**
	 * The package name to use for looking schema bean interfaces.
	 */
	@Parameter
	protected String schemaBeanSourcePackage;

	/**
	 * The package name to use for looking custom service interfaces.
	 */
	@Parameter
	public String customServiceInterfacesPath;

	/**
	 * The package name to use for service implementations to generate spring context file.
	 */
	@Parameter
	public String springBeanServiceImplementationPackage;

	/**
	 * A path to a file that should contain an example of a spring bean definition to use the services provided by the current maven module.
	 */
	@Parameter
	public String springBeanDefinitionPath;

	protected static final String SERVICE_INTERFACE_PREFIX = "Default";
	protected static final String CUSTOM_INTERFACE_PREFIX = "Custom";
	protected static final String SERVICE_INTERFACE_SUFFIX = "Service";

	private final List<String> customApiList = new ArrayList<>();

	@Override
	protected void doCodeGenerationWithSchema(Schema schema, CodeGenerationContext ctx, File path) throws IOException, MojoExecutionException, MojoFailureException {
		ctx.setImportResolver(new ImportResolver() {
			@Override
			public String resolveImport(String simpleName) {
				if (simpleName == null) {
					throw new IllegalArgumentException("can not resolve import when simpleName is null.");
				}
				return simpleName;
			}
		});

		loadCustomServiceInterfaceNamesToList(customServiceInterfacesPath);

		OutputStream os = null;
		XMLStreamWriter xmlStream = null;
		try {
			if (springBeanDefinitionPath != null && springBeanServiceImplementationPackage != null) {
				Path springBeanDefPath = Paths.get(springBeanDefinitionPath);
				Path springBeanFolder = springBeanDefPath.getParent();
				if (Files.notExists(springBeanFolder)) {
					Files.createDirectories(springBeanFolder);
				}
				if (Files.notExists(springBeanDefPath)) {
					Files.createFile(springBeanDefPath);
				}
				os = Files.newOutputStream(springBeanDefPath, StandardOpenOption.WRITE);
				xmlStream = XMLOutputFactory.newInstance().createXMLStreamWriter(os, "UTF-8");
			}

			if (xmlStream != null) {
				xmlStream.setDefaultNamespace("http://www.springframework.org/schema/beans");
				xmlStream.setPrefix("beans", "http://www.springframework.org/schema/beans");
				xmlStream.setPrefix("xmlns", "http://www.w3.org/2001/XMLSchema-instance");
				xmlStream.setPrefix("context", "http://www.springframework.org/schema/context");
				xmlStream.writeStartDocument();
				xmlStream.writeStartElement("beans");
				xmlStream.writeDefaultNamespace("http://www.springframework.org/schema/beans");
				xmlStream.writeNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
				xmlStream.writeNamespace("context", "http://www.springframework.org/schema/context");
				xmlStream.writeNamespace("util", "http://www.springframework.org/schema/util");
				xmlStream.writeAttribute("http://www.w3.org/2001/XMLSchema-instance", "schemaLocation", "http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-3.0.xsd http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context-3.0.xsd http://www.springframework.org/schema/util http://www.springframework.org/schema/util/spring-util-2.5.xsd");
			}
			List<Type> types = schema.getTypes();
			if (types != null) {
				if (xmlStream != null) {
					xmlStream.writeStartElement("http://www.springframework.org/schema/util", "list");
					xmlStream.writeAttribute("id", schema.getName() + "ServiceStubs");
					for (Type type : types) {
						if (!(type.isAbstract() || type.isVirtual())) {
							xmlStream.writeEmptyElement("ref");
							xmlStream.writeAttribute("bean", getServiceBeanName(type) + "Stub");
						}
					}
					xmlStream.writeEndElement();
				}
				for (Type type : types) {
					if (!(type.isAbstract() || type.isVirtual())) {
						importStateHolder.reset();

						// Generate default service interfaces
						CodeBlock defaultServiceCode = generateCodeForType(type, ctx);
						writeCodeToFile(defaultServiceCode, path, type, SERVICE_INTERFACE_PREFIX, SERVICE_INTERFACE_SUFFIX);

						// Generate Full-API-Interface for custom and default service interfaces
						CodeBlock fullAPIServiceCodeOfType = generateCodeForFullApiInterface(type, ctx);
						writeCodeToFile(fullAPIServiceCodeOfType, path, type, SERVICE_INTERFACE_SUFFIX);

						// Write spring bean definition
						if (xmlStream != null) {
							writeSpringBeanDefinition(xmlStream, type, ctx);
						}
					}
				}
			}
			if (xmlStream != null) {
				xmlStream.writeEndDocument();
			}
		} catch (XMLStreamException ex) {
			throw new MojoExecutionException("failed to create xml stream", ex);
		} finally {
			if (os != null) {
				try {
					os.close();
				} catch (Exception e) {
					// silently close
				}
			}
		}
	}

	private CodeBlock generateCodeForType(Type type, CodeGenerationContext ctx) {
		String tName = type.getName();

		getLog().info("generating java service client interface for type " + tName);
		CodeBlock block = ctx.create(CodeBlock.class);
		block.line("package ").append(ctx.getBasePackage()).append(";");
		block.line("");
		block.line("import ").append(schemaBeanSourcePackage).append(".").append(tName).append(";");
		block.line("import ").append(GENERIC_RESOURCE_SERVICE).append(";");
		block.line("");
		block.line("public interface ")
			.append(SERVICE_INTERFACE_PREFIX).append( upperCaseFirstLetter(tName) ).append(SERVICE_INTERFACE_SUFFIX)
			.append(" extends ")
			.append("GenericResourceService<").append(tName).append(">");

		CodeBracket bracket = block.createContained(CodeBracket.class);
		bracket.line("");

		return block;
	}

	protected static String lowerCaseFirstLetter(String string) {
		if (string.length() > 1) {
			string = string.substring(0, 1).toLowerCase() + string.substring(1);
		} else {
			string = string.toLowerCase();
		}
		return string;
	}

	private void loadCustomServiceInterfaceNamesToList(String path) throws MojoExecutionException {

		DirectoryStream.Filter<Path> filter = new DirectoryStream.Filter<Path>() {
			@Override
			public boolean accept(Path file) throws IOException {
				if (file != null) {
					return file.getFileName().toString().startsWith(CUSTOM_INTERFACE_PREFIX) && file.toString().endsWith(SERVICE_INTERFACE_SUFFIX + ".java");
				} else {
					return false;
				}
			}
		};

		try (DirectoryStream<Path> customServiceDir = Files.newDirectoryStream(Paths.get(path), filter)) {
			for (Path p : customServiceDir) {
				int end = p.getFileName().toString().length() - ".java".length(); //
				String customClazzName = p.getFileName().toString().substring(0, end);

				customApiList.add(customClazzName);
			}
		} catch (IOException io) {
			throw new MojoExecutionException("No custom interfaces found!", io);
		}
	}

	private CodeBlock generateCodeForFullApiInterface(Type type, CodeGenerationContext ctx) {
		String tName = type.getName();

		getLog().info("generating " + tName + "Service FULL API-Interface for default and custom services");

		CodeBlock block = ctx.create(CodeBlock.class);
		block.line("package ").append(ctx.getBasePackage()).append(";");
		block.line("");

		CodeLine line = block.createContained(CodeLine.class);
		line.append("public interface ")
				.append(upperCaseFirstLetter(tName)).append(SERVICE_INTERFACE_SUFFIX)
				.append(" extends ")
				.append(SERVICE_INTERFACE_PREFIX).append(upperCaseFirstLetter(tName)).append(SERVICE_INTERFACE_SUFFIX);

		String customClazzName = getCustomServiceInterfaceClassNameForType(type);
		if (customClazzName != null) {
			line.append(", ").append(customClazzName);
		}

		CodeBracket bracket = block.createContained(CodeBracket.class);

		bracket.line("String NAME = \"").append(getServiceBeanName(type)).append("\";");

		return block;
	}

	private String getServiceBeanName(Type type) {
		return lowerCaseFirstLetter(type.getSchema().getName()) + type.getName() + SERVICE_INTERFACE_SUFFIX;
	}

	private String getFullApiName(Type type, CodeGenerationContext ctx) {
		return ctx.getBasePackage() + "." + getSimpleFullApiName(type);
	}

	private String getSimpleFullApiName(Type type) {
		return upperCaseFirstLetter(type.getName()) + SERVICE_INTERFACE_SUFFIX;
	}

	private String getCustomServiceInterfaceClassNameForType(Type type) {
		for (String customClazzName : customApiList) {
			int end = customClazzName.length() - SERVICE_INTERFACE_SUFFIX.length(); //
			String typeName = customClazzName.substring(CUSTOM_INTERFACE_PREFIX.length(), end);

			if (type.getName().equals(typeName)) {
				return customClazzName;
			}
		}
		return null;
	}

	private void writeSpringBeanDefinition(XMLStreamWriter xmlStream, Type type, CodeGenerationContext ctx) throws XMLStreamException {
		String stubBeanName = getServiceBeanName(type) + "Stub";

		String customServiceInterfaceClassNameForType = getCustomServiceInterfaceClassNameForType(type);

		xmlStream.writeStartElement("bean");
		xmlStream.writeAttribute("name", stubBeanName);
		xmlStream.writeAttribute("class", "org.bndly.common.service.setup.SchemaServiceStub");

		xmlStream.writeEmptyElement("property");
		xmlStream.writeAttribute("name", "schemaName");
		xmlStream.writeAttribute("value", type.getSchema().getName());

		xmlStream.writeEmptyElement("property");
		xmlStream.writeAttribute("name", "fullApiClassName");
		xmlStream.writeAttribute("value", getFullApiName(type, ctx));

		if (customServiceInterfaceClassNameForType != null) {
			xmlStream.writeEmptyElement("property");
			xmlStream.writeAttribute("name", "customServiceClassName");
			xmlStream.writeAttribute("value", springBeanServiceImplementationPackage + "." + CUSTOM_INTERFACE_PREFIX + type.getName() + SERVICE_IMPL_SUFFIX);
		}

		xmlStream.writeEmptyElement("property");
		xmlStream.writeAttribute("name", "genericServiceClassName");
		xmlStream.writeAttribute("value", springBeanServiceImplementationPackage + "." + SERVICE_INTERFACE_PREFIX + type.getName() + SERVICE_IMPL_SUFFIX);

		xmlStream.writeEndElement();

		xmlStream.writeStartElement("bean");
		xmlStream.writeAttribute("name", getServiceBeanName(type));
		xmlStream.writeAttribute("class", getFullApiName(type, ctx));
		xmlStream.writeAttribute("factory-bean", stubBeanName);
		xmlStream.writeAttribute("factory-method", "getFullApi");
		xmlStream.writeEndElement();
	}
}
