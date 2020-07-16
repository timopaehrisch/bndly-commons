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

import static org.bndly.schema.beans.SchemaServiceInterfaceGeneratorMojo.*;

import org.bndly.code.common.CodeGenerationContext;
import org.bndly.code.model.CodeBlock;
import org.bndly.code.model.CodeBracket;
import org.bndly.code.renderer.ImportResolver;
import org.bndly.schema.model.Schema;
import org.bndly.schema.model.Type;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.bndly.schema.beans.SchemaBeanGeneratorMojo.upperCaseFirstLetter;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Generates Service Layer Client Interfaces from Schema
 */
@Mojo(
		name = "generateServiceImpl"
)
public class SchemaServiceImplGeneratorMojo extends AbstractSchemaBasedBeanGeneratorMojo {

	protected static final String GENERIC_RESOURCE_SERVICE_IMPL = "org.bndly.common.service.shared.GenericResourceServiceImpl";
	protected static final String SERVICE_IMPL_SUFFIX = "ServiceImpl";

	/**
	 * The package name to use for looking generated service interfaces.
	 */
	@Parameter
	protected String generatedServiceApiPackage;

	/**
	 * The package name to use for looking schema bean interfaces.
	 */
	@Parameter
	protected String schemaBeanSourcePackage;

	/**
	 * The package name to use for looking schema rest bean interfaces.
	 */
	@Parameter
	protected String schemaRestBeanSourcePackage;

	/**
	 * The package name to use for looking schema rest bean interfaces.
	 */
	@Parameter
	protected String generatedModelImplPackage;

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

		List<Type> types = schema.getTypes();
		if (types != null) {
			for (Type type : types) {
				if (!(type.isAbstract() || type.isVirtual())) {
					importStateHolder.reset();
					CodeBlock code = generateCodeForType(type, ctx);
					writeCodeToFile(code, path, type, SERVICE_INTERFACE_PREFIX, SERVICE_IMPL_SUFFIX);
				}
			}
		}
	}

	private CodeBlock generateCodeForType(Type type, CodeGenerationContext ctx) {
		String tName = type.getName();
		String genServiceCName = getGeneratedServiceClassName(tName);

		getLog().info("generating java service client implementation for type " + tName);

		CodeBlock block = ctx.create(CodeBlock.class);
		block.line("package ").append(ctx.getBasePackage()).append(";");
		block.line("");
		block.line("import ").append(schemaBeanSourcePackage).append(".").append(tName).append(";");
		block.line("import ").append(generatedModelImplPackage).append(".").append(tName).append("Impl;");
		block.line("import ").append(schemaRestBeanSourcePackage).append(".").append(tName).append("ListRestBean;");
		block.line("import ").append(schemaRestBeanSourcePackage).append(".").append(tName).append("ReferenceRestBean;");
		block.line("import ").append(schemaRestBeanSourcePackage).append(".").append(tName).append("RestBean;");
		block.line("import ").append(generatedServiceApiPackage).append(".").append(upperCaseFirstLetter(tName)).append(SERVICE_INTERFACE_SUFFIX).append(";");
		block.line("import ").append(generatedServiceApiPackage).append(".").append(genServiceCName).append(";");
		block.line("import ").append(GENERIC_RESOURCE_SERVICE_IMPL).append(";");
		block.line("");

		block.line("public class ")
				.append(genServiceCName).append("Impl")
				.append(" extends ")
				.append("GenericResourceServiceImpl");
		block.line("\t\t<")
				.append(tName).append(", ")
				.append(tName).append("ListRestBean").append(", ")
				.append(tName).append("ReferenceRestBean").append(", ")
				.append(tName).append("RestBean")
				.append(">");

		block.line("\timplements ").append(genServiceCName);

		CodeBracket bracket = block.createContained(CodeBracket.class);
		bracket.line("");
		bracket.line("@Override");
		bracket.line("public ").append(tName).append(" instantiateModel() ");
		CodeBracket methodbracket = bracket.createContained(CodeBracket.class);
		methodbracket.line("return new ").append(tName).append("Impl();");

		bracket.line("");
		bracket.line("@Override");
		bracket.line("public String ").append("getDefaultServiceName() ");
		methodbracket = bracket.createContained(CodeBracket.class);
		methodbracket.line("return ").append(upperCaseFirstLetter(tName)).append(SERVICE_INTERFACE_SUFFIX).append(".NAME;");

		return block;
	}

	private String getGeneratedServiceClassName(String typeName) {
		return SERVICE_INTERFACE_PREFIX + upperCaseFirstLetter(typeName) + SERVICE_INTERFACE_SUFFIX;
	}
}
