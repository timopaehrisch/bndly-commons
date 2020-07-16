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
import org.bndly.code.model.CodeBracket;
import org.bndly.code.model.CodeLine;
import org.bndly.code.renderer.ImportResolver;
import static org.bndly.schema.beans.SchemaBeanGeneratorMojo.assertIsImported;
import static org.bndly.schema.beans.SchemaBeanGeneratorMojo.getJavaTypeNameForAttribute;
import static org.bndly.schema.beans.SchemaBeanGeneratorMojo.upperCaseFirstLetter;
import org.bndly.schema.model.Attribute;
import org.bndly.schema.model.Mixin;
import org.bndly.schema.model.NamedAttributeHolder;
import org.bndly.schema.model.Schema;
import org.bndly.schema.model.Type;
import org.bndly.schema.model.UniqueConstraint;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Generates Client Model implementations for Schema Beans
 */
@Mojo(
		name = "generateClientModel"
)
public class ClientModelBeanGeneratorMojo extends AbstractSchemaBasedBeanGeneratorMojo {

	/**
	 * The package name to use for looking schema bean interfaces.
	 */
	@Parameter
	protected String schemaBeanSourcePackage;

	@Override
	protected void doCodeGenerationWithSchema(Schema schema, CodeGenerationContext ctx, File path) throws IOException, MojoExecutionException, MojoFailureException {
		final Map<String, NamedAttributeHolder> mapOfNamedAttributeHolders = new HashMap<>();
		List<Type> types = schema.getTypes();
		if (types != null) {
			for (Type type : types) {
				mapOfNamedAttributeHolders.put(type.getName(), type);
			}
		}
		List<Mixin> mixins = schema.getMixins();
		if (mixins != null) {
			for (Mixin mixin : mixins) {
				mapOfNamedAttributeHolders.put(mixin.getName(), mixin);
			}
		}
		List<UniqueConstraint> uniqueConstraints = schema.getUniqueConstraints();
		Map<String, List<UniqueConstraint>> uniqueConstraintsByAttributeHolderName = Collections.EMPTY_MAP;
		if (uniqueConstraints != null) {
			uniqueConstraintsByAttributeHolderName = new HashMap<>();
			for (UniqueConstraint uniqueConstraint : uniqueConstraints) {
				List<UniqueConstraint> list = uniqueConstraintsByAttributeHolderName.get(uniqueConstraint.getHolder().getName());
				if (list == null) {
					list = new ArrayList<>();
					uniqueConstraintsByAttributeHolderName.put(uniqueConstraint.getHolder().getName(), list);
				}
				list.add(uniqueConstraint);
			}
		}
		ctx.setImportResolver(new ImportResolver() {

			@Override
			public String resolveImport(String simpleName) {
				NamedAttributeHolder ah = mapOfNamedAttributeHolders.get(simpleName);
				if (ah != null) {
					return schemaBeanSourcePackage + "." + ah.getName();
				} else if ("Date".equals(simpleName)) {
					return Date.class.getName();
				} else if ("InputStream".equals(simpleName)) {
					return InputStream.class.getName();
				} else if ("BigDecimal".equals(simpleName)) {
					return BigDecimal.class.getName();
				}
				return simpleName;
			}
		});
		if (types != null) {
			for (Type type : types) {
				importStateHolder.reset();
				CodeBlock code = generatePojoImplementationForSchemaType(type, ctx, uniqueConstraintsByAttributeHolderName);
				writeCodeToFile(code, path, type, "Impl");
			}
		}
	}

	private CodeBlock generatePojoImplementationForSchemaType(Type type, CodeGenerationContext ctx, Map<String, List<UniqueConstraint>> uniqueConstraintsByAttributeHolderName) {
		CodeBlock block = ctx.create(CodeBlock.class);
		block.line("package " + targetPackage + ";");
		block.line("");
		CodeBlock importBlock = block.createContained(CodeBlock.class);
		block.line("");
		
		boolean hasSubTypes = type.getSubTypes() != null && !type.getSubTypes().isEmpty();
		String resolved = importBlock.getContext().getImportResolver().resolveImport(type.getName());
		if (resolved != null) {
			assertIsImported(importBlock, resolved, importStateHolder);
		}
		CodeLine line = block.line("public " + (type.isAbstract() ? "abstract " : "") + "class " + type.getName() + "Impl");
		if (hasSubTypes) {
			line.append("<E extends " + type.getName() + ">");
		}

		if (type.getSuperType() != null) {
			line.append(" extends " + type.getSuperType().getName() + "Impl<" + type.getName() + ">");
		} else {
			assertIsImported(importBlock, "org.bndly.common.service.model.api.AbstractEntity", importStateHolder);
			line.append(" extends AbstractEntity");
			if (hasSubTypes) {
				line.append("<E>");
			} else {
				line.append("<" + type.getName() + ">");
			}
		}
		line.append(" implements " + type.getName());
		CodeBracket classImplementationBlock = block.createContained(CodeBracket.class);
		CodeBlock fieldsBlock = classImplementationBlock.createContained(CodeBlock.class);
		CodeBlock methodsBlock = classImplementationBlock.createContained(CodeBlock.class);

		if (typeRequiresIdField(type)) {
			assertIsImported(importBlock, "org.bndly.common.service.model.api.ReferenceAttribute", importStateHolder);
			fieldsBlock.line("@ReferenceAttribute");
			fieldsBlock.line("private Long id;");
			methodsBlock.line("public Long getId()");
			methodsBlock.createContained(CodeBracket.class).line("return this.id;");
			methodsBlock.line("public void setId(Long id)");
			methodsBlock.createContained(CodeBracket.class).line("this.id = id;");
		}
		
		generateCodeForAttributesOfAttributeHolder(type, fieldsBlock, methodsBlock, importBlock, uniqueConstraintsByAttributeHolderName);

		return block;
	}
	
	private boolean typeRequiresIdField(Type type) {
		return type.getSuperType() == null;
	}

	private void generateCodeForAttributesOfAttributeHolder(
			NamedAttributeHolder attributeHolder, 
			CodeBlock fieldsBlock, 
			CodeBlock methodsBlock, 
			CodeBlock importBlock, 
			Map<String, List<UniqueConstraint>> uniqueConstraintsByAttributeHolderName
	) {
		if (attributeHolder == null) {
			return;
		}
		List<Attribute> atts = attributeHolder.getAttributes();
		if (atts != null) {
			List<UniqueConstraint> uniqueConstraints = uniqueConstraintsByAttributeHolderName.get(attributeHolder.getName());
			for (Attribute att : atts) {
				if (uniqueConstraints != null) {
					for (UniqueConstraint uniqueConstraint : uniqueConstraints) {
						if (uniqueConstraint.getAttributes().size() == 1) {
							if (uniqueConstraint.getAttributes().get(0) == att) {
								assertIsImported(importBlock, "org.bndly.common.service.model.api.ReferenceAttribute", importStateHolder);
								fieldsBlock.line("@ReferenceAttribute");
							}
						}
					}
				}
				String javaTypeName = getJavaTypeNameForAttribute(att, importBlock, importStateHolder, true);
				fieldsBlock.line("private " + javaTypeName + " " + att.getName() + ";");
				
				String attUpperCased = upperCaseFirstLetter(att.getName());
				methodsBlock.line("@Override");
				methodsBlock.line("public void set" + attUpperCased + "(" + javaTypeName + " " + att.getName() + ")");
				methodsBlock.createContained(CodeBracket.class).line("this." + att.getName() + " = " + att.getName() + ";");

				methodsBlock.line("@Override");
				methodsBlock.line("public " + javaTypeName + " get" + attUpperCased + "()");
				methodsBlock.createContained(CodeBracket.class).line("return this." + att.getName() + ";");
			}
		}
		if (Type.class.isInstance(attributeHolder)) {
			Type t = ((Type) attributeHolder);
			List<Mixin> mixins = t.getMixins();
			if (mixins != null) {
				for (Mixin mixin : mixins) {
					generateCodeForAttributesOfAttributeHolder(mixin, fieldsBlock, methodsBlock, importBlock, uniqueConstraintsByAttributeHolderName);
				}
			}
		}
	}

}
