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

import org.bndly.code.common.CodeGenerationContext;
import org.bndly.code.model.CodeBlock;
import org.bndly.code.output.JavaCodeElementStringWriter;
import org.bndly.schema.definition.parser.api.ParsingException;
import org.bndly.schema.definition.parser.impl.SchemaDefinitionIOImpl;
import org.bndly.schema.model.NamedAttributeHolder;
import org.bndly.schema.model.Schema;
import org.bndly.schema.model.SchemaProvider;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public abstract class AbstractSchemaBasedBeanGeneratorMojo extends AbstractMojo {
	/**
	 * The target folder for generated files
	 */
	@Parameter
	protected String targetFolder;

	/**
	 * The package name to use for the generated schema beans.
	 */
	@Parameter
	protected String targetPackage;

	/**
	 * The class that provides the schema
	 */
	@Parameter
	protected String schemaProviderClass;

	/**
	 * The path to the root schema definition file
	 */
	@Parameter
	protected String schemaRoot;

	/**
	 * The paths to the schema extension definition files
	 */
	@Parameter
	protected List<String> schemaExtensions;
	
	protected ImportStateHolderImpl importStateHolder;
	
	@Override
	public final void execute() throws MojoExecutionException, MojoFailureException {
		try {
			Schema schema;
			if (schemaProviderClass != null) {
				Class<?> cls = getClass().getClassLoader().loadClass(schemaProviderClass);
				SchemaProvider schemaProvider = (SchemaProvider) cls.newInstance();
				schema = schemaProvider.getSchema();
			} else if (schemaRoot != null) {
				try {
					if (schemaExtensions != null) {
						String[] args = new String[schemaExtensions.size()];
						for (int i = 0; i < schemaExtensions.size(); i++) {
							String extensionPath = schemaExtensions.get(i);
							args[i] = extensionPath;
						}
						schema = new SchemaDefinitionIOImpl().parse(schemaRoot, args);
					} else {
						schema = new SchemaDefinitionIOImpl().parse(schemaRoot);
					}
				} catch (ParsingException ex) {
					throw new MojoExecutionException("failed to parse schema definition: " + ex.getMessage(), ex);
				}
			} else {
				throw new MojoExecutionException("no schema provided");
			}
			CodeGenerationContext ctx = new CodeGenerationContext();
			importStateHolder = ctx.create(ImportStateHolderImpl.class);
			ctx.setBasePackage(targetPackage);
			String targetPackageAsPathSegments = targetPackage.replaceAll("\\.", "/");
			String folder = targetFolder;
			if (!folder.endsWith("/")) {
				folder += "/";
			}
			folder += targetPackageAsPathSegments;

			File path = new File(folder);
			path.mkdirs();

			doCodeGenerationWithSchema(schema, ctx, path);
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
			throw new MojoExecutionException("could not instantiate schema provider class: " + schemaProviderClass + ": " + ex.getMessage(), ex);
		} catch (IOException ex) {
			throw new MojoExecutionException("could not write generated code to file. " + ex.getMessage(), ex);
		}
	}

	protected void writeCodeToFile(CodeBlock code, File path, NamedAttributeHolder nah, String prefix, String suffix) throws IOException {
		writeCodeToFile(code, path, prefix + nah.getName() + suffix + ".java");
	}

	protected void writeCodeToFile(CodeBlock code, File path, NamedAttributeHolder nah, String suffix) throws IOException {
		writeCodeToFile(code, path, nah.getName() + suffix + ".java");
	}

	protected void writeCodeToFile(CodeBlock code, File path, String fileName) throws IOException {
		String codeAsString = JavaCodeElementStringWriter.toString(code);
		File f = new File(path, fileName);
		f.createNewFile();
		FileWriter w = new FileWriter(f);
		w.write(codeAsString);
		w.flush();
		w.close();
	}

	protected abstract void doCodeGenerationWithSchema(Schema schema, CodeGenerationContext ctx, File path) throws IOException, MojoExecutionException, MojoFailureException;
}
