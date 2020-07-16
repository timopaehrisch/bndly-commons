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
import org.bndly.code.model.CodeImport;
import org.bndly.code.model.CodeLine;
import org.bndly.code.model.CodePackage;
import org.bndly.code.output.JavaCodeElementStringWriter;
import org.bndly.code.renderer.ImportResolver;
import org.bndly.schema.model.Attribute;
import org.bndly.schema.model.BinaryAttribute;
import org.bndly.schema.model.BooleanAttribute;
import org.bndly.schema.model.CryptoAttribute;
import org.bndly.schema.model.DateAttribute;
import org.bndly.schema.model.DecimalAttribute;
import org.bndly.schema.model.InverseAttribute;
import org.bndly.schema.model.JSONAttribute;
import org.bndly.schema.model.Mixin;
import org.bndly.schema.model.NamedAttributeHolder;
import org.bndly.schema.model.NamedAttributeHolderAttribute;
import org.bndly.schema.model.Schema;
import org.bndly.schema.model.StringAttribute;
import org.bndly.schema.model.Type;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

/**
 *  Generates Schema Beans from a predefined Schema
 */
@Mojo(
		name = "generate"
)
public class SchemaBeanGeneratorMojo extends AbstractSchemaBasedBeanGeneratorMojo {

	@Override
	protected void doCodeGenerationWithSchema(Schema schema, CodeGenerationContext ctx, File targetPath) throws IOException, MojoExecutionException, MojoFailureException {
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
				importStateHolder.reset();
				CodeBlock code = generateCodeForType(type, ctx);
				writeCodeToFile(code, targetPath, type);
			}
		}
		List<Mixin> mixins = schema.getMixins();
		if (mixins != null) {
			for (Mixin mixin : mixins) {
				importStateHolder.reset();
				CodeBlock code = generateCodeForMixin(mixin, ctx);
				writeCodeToFile(code, targetPath, mixin);
			}
		}
	}

	private CodeBlock generateCodeForMixin(Mixin mixin, CodeGenerationContext ctx) {
		getLog().info("generating java interface for mixin " + mixin.getName());
		CodePackage block = ctx.create(CodePackage.class, ctx.getBasePackage());
		CodeBlock importBlock = block.createContained(CodeBlock.class);
		block.line(convertToJavaDoc(generateTypeJavaDoc(mixin), 0));
		block.line("public interface ").append(mixin.getName());
		CodeBracket bracket = block.createContained(CodeBracket.class);
		generateGetterAndSetter(mixin, bracket, importBlock);
		return block;
	}

	private CodeBlock generateCodeForType(Type type, CodeGenerationContext ctx) {
		getLog().info("generating java interface for type " + type.getName());
		CodeBlock block = ctx.create(CodeBlock.class);
		block.line("package ").append(ctx.getBasePackage()).append(";");
		CodeBlock importBlock = block.createContained(CodeBlock.class);
		block.line(convertToJavaDoc(generateTypeJavaDoc(type), 0));
		CodeLine l = block.line("public interface ").append(type.getName());
		boolean first = true;
		if (type.getMixins() != null) {
			for (Mixin mixin : type.getMixins()) {
				first = appendExtendedAttributeHolder(first, l, mixin);
			}
		}
		if (type.getSuperType() != null) {
			appendExtendedAttributeHolder(first, l, type.getSuperType());
		}
		CodeBracket bracket = block.createContained(CodeBracket.class);
		generateGetterAndSetter(type, bracket, importBlock);
		return block;
	}

	protected static String getJavaTypeNameForAttribute(Attribute attribute, CodeBlock importBlock, ImportStateHolder importStateHolder) {
		return getJavaTypeNameForAttribute(attribute, importBlock, importStateHolder, false);
	}

	protected static String getJavaTypeNameForAttribute(Attribute attribute, CodeBlock importBlock, ImportStateHolder importStateHolder, boolean importAttributeHolders) {
		if (StringAttribute.class.isInstance(attribute)) {
			return "String";
		} else if (CryptoAttribute.class.isInstance(attribute)) {
			return "String";
		} else if (DecimalAttribute.class.isInstance(attribute)) {
			DecimalAttribute att = DecimalAttribute.class.cast(attribute);
			Integer dp = att.getDecimalPlaces();
			if (dp == null) {
				dp = 0;
			}
			Integer length = att.getLength();
			if (length == null) {
				if (dp == 0) {
					return "Long";
				} else {
					return "Double";
				}
			} else {
				assertIsImported(importBlock, BigDecimal.class, importStateHolder);
				return "BigDecimal";
			}
		} else if (BooleanAttribute.class.isInstance(attribute)) {
			return "Boolean";
		} else if (DateAttribute.class.isInstance(attribute)) {
			assertIsImported(importBlock, Date.class, importStateHolder);
			return "Date";
		} else if (NamedAttributeHolderAttribute.class.isInstance(attribute)) {
			NamedAttributeHolderAttribute att = NamedAttributeHolderAttribute.class.cast(attribute);
			if (importAttributeHolders) {
				assertIsImported(importBlock, importBlock.getContext().getImportResolver().resolveImport(att.getNamedAttributeHolder().getName()), importStateHolder);
			}
			return att.getNamedAttributeHolder().getName();
		} else if (InverseAttribute.class.isInstance(attribute)) {
			InverseAttribute att = InverseAttribute.class.cast(attribute);
			assertIsImported(importBlock, List.class, importStateHolder);
			if (importAttributeHolders) {
				assertIsImported(importBlock, importBlock.getContext().getImportResolver().resolveImport(att.getReferencedAttributeHolder().getName()), importStateHolder);
			}
			return "List<" + att.getReferencedAttributeHolder().getName() + ">";
		} else if (JSONAttribute.class.isInstance(attribute)) {
			JSONAttribute att = JSONAttribute.class.cast(attribute);
			if (importAttributeHolders) {
				assertIsImported(importBlock, importBlock.getContext().getImportResolver().resolveImport(att.getNamedAttributeHolder().getName()), importStateHolder);
			}
			return att.getNamedAttributeHolder().getName();
		} else if (BinaryAttribute.class.isInstance(attribute)) {
			BinaryAttribute att = BinaryAttribute.class.cast(attribute);
			if (att.getAsByteArray() != null && att.getAsByteArray()) {
				return "byte[]";
			} else {
				assertIsImported(importBlock, InputStream.class, importStateHolder);
				return "InputStream";
			}
		}
		throw new IllegalArgumentException("unsupported attribute type");
	}

	private void generateGetterAndSetter(NamedAttributeHolder nah, CodeBracket bracket, CodeBlock importBlock) {
		if (nah.getAttributes() != null) {
			for (Attribute attribute : nah.getAttributes()) {
				String javaTypeName = getJavaTypeNameForAttribute(attribute, importBlock, importStateHolder);
				String ucAttributeName = upperCaseFirstLetter(attribute.getName());
				convertToJavaDoc(generateGetterJavaDoc(attribute), bracket);
				bracket.line(javaTypeName).append(" get").append(ucAttributeName).append("();");
				convertToJavaDoc(generateSetterJavaDoc(attribute), bracket);
				bracket.line("void set").append(ucAttributeName).append("(").append(javaTypeName).append(" ").append(attribute.getName()).append(");");
			}
		}
	}

	protected static String upperCaseFirstLetter(String string) {
		if (string.length() > 1) {
			string = string.substring(0, 1).toUpperCase() + string.substring(1);
		} else {
			string = string.toUpperCase();
		}
		return string;
	}

	private boolean appendExtendedAttributeHolder(boolean first, CodeLine l, NamedAttributeHolder nah) {
		if (first) {
			first = false;
			l.append(" extends ");
		} else {
			l.append(", ");
		}
		l.append(nah.getName());
		return first;
	}

	private void writeCodeToFile(CodeBlock code, File path, NamedAttributeHolder nah) throws IOException {
		String codeAsString = JavaCodeElementStringWriter.toString(code);
		File f = new File(path, nah.getName() + ".java");
		f.createNewFile();
		FileWriter w = new FileWriter(f);
		w.write(codeAsString);
		w.flush();
		w.close();
	}

	protected static void assertIsImported(CodeBlock importBlock, String typeToImport, ImportStateHolder importStateHolder) {
		if (!importStateHolder.hasBeenImported(typeToImport)) {
			importBlock.createContained(CodeImport.class, typeToImport);
		}
	}

	protected static void assertIsImported(CodeBlock importBlock, Class<?> typeToImport, ImportStateHolder importStateHolder) {
		assertIsImported(importBlock, typeToImport.getName(), importStateHolder);
	}
	
/**
* Sets the value for the attribute 'city'.
* The attribute is declared as mandatory.
* The value is allowed to have a maximum of 255 characters.
* @param city 
*/
	static String generateSetterJavaDoc(Attribute attribute) {
		StringBuilder sb = new StringBuilder();
		sb.append("Sets the value for the attribute '").append(attribute.getName()).append("'.\n");
		if (attribute.isMandatory()) {
			sb.append("The attribute is declared as mandatory.\n");
		}
		if (attribute.isVirtual()) {
			sb.append("The attribute is declared as virtual. You can not use it in queries!\n");
		}
		if (StringAttribute.class.isInstance(attribute)) {
			StringAttribute sa = (StringAttribute) attribute;
			Integer length = sa.getLength();
			if (length != null) {
				sb.append("The value is allowed to have a maximum of ").append(length).append(" characters.\n");
			}
			if (sa.getIsLong() != null && sa.getIsLong()) {
				sb.append("The value is allowed to be long. This means it can not be used in queries.\n");
			}
		} else if (DecimalAttribute.class.isInstance(attribute)) {
			DecimalAttribute da = (DecimalAttribute) attribute;
			if (da.getLength() != null) {
				sb.append("The value is allowed to have ").append(da.getLength()).append(" digits (precision).\n");
			}
			if (da.getDecimalPlaces() != null) {
				sb.append("The value is allowed to have ").append(da.getDecimalPlaces()).append(" decimal places (scale).\n");
			}
		} else if (JSONAttribute.class.isInstance(attribute)) {
			sb.append("The value is stored as JSON data and can not be queried.\n");
		} else if (BinaryAttribute.class.isInstance(attribute)) {
			sb.append("The value is stored as a blob and can not be queried.\n");
		}
		String docs = attribute.getAnnotations().get("docs");
		if (docs != null) {
			sb.append(docs).append("\n");
		}
		sb.append("@param ").append(attribute.getName()).append(" value of '").append(attribute.getName()).append("'");
		return sb.toString();
	}
	
	static String generateGetterJavaDoc(Attribute attribute) {
		StringBuilder sb = new StringBuilder();
		sb.append("Returns the value for the attribute '").append(attribute.getName()).append("'.\n");
		if (attribute.isVirtual()) {
			sb.append("The attribute is declared virtual. Therefore your application has to provide logic, that implements the resolving of the attribute values.\n");
		}
		String docs = attribute.getAnnotations().get("docs");
		if (docs != null) {
			sb.append(docs).append("\n");
		}
		sb.append("@return value of '").append(attribute.getName()).append("'");
		return sb.toString();
	}
	
	static String generateTypeJavaDoc(NamedAttributeHolder namedAttributeHolder) {
		StringBuilder sb = new StringBuilder();
		sb.append("This interface corresponds to the schema ");
		if (Type.class.isInstance(namedAttributeHolder)) {
			sb.append("type");
		} else if (Mixin.class.isInstance(namedAttributeHolder)) {
			sb.append("mixin");
		} else {
			sb.append("element");
		}
		sb.append(" '").append(namedAttributeHolder.getName()).append("' from the schema '").append(namedAttributeHolder.getSchema().getName()).append("' with the namespace '").append(namedAttributeHolder.getSchema().getNamespace()).append("'.\n");
		if (namedAttributeHolder.isVirtual()) {
			sb.append("It is declared virtual and can therefore not be queried.\n");
		}
		String docs = namedAttributeHolder.getAnnotations().get("docs");
		if (docs != null) {
			sb.append(docs).append("\n");
		}
		sb.append("@author ").append(SchemaBeanGeneratorMojo.class.getName());
		return sb.toString();
	}
	
	static void convertToJavaDoc(String documentation, CodeBlock codeBlock) {
		BufferedReader br = new BufferedReader(new StringReader(documentation));
		String line;
		try {
			codeBlock.line("/**");
			while ((line = br.readLine()) != null) {
				// escape closing comments or starting comments in the line with html entities
				codeBlock.line(" * " + line.replaceAll("\\*", "&#42;"));
			}
			codeBlock.line(" */");
		} catch (IOException e) {
			// can't happen, because we are using strings from memory
		}
	}
	
	static String convertToJavaDoc(String documentation, int indent) {
		StringBuilder sb = new StringBuilder();
		BufferedReader br = new BufferedReader(new StringReader(documentation));
		String line;
		try {
			for (int i = 0; i < indent; i++) {
				sb.append('\t');
			}
			sb.append("/**\n");
			while ((line = br.readLine()) != null) {
				for (int i = 0; i < indent; i++) {
					sb.append('\t');
				}
				sb.append(" * ");
				// escape closing comments or starting comments in the line with html entities
				sb.append(line.replaceAll("\\*", "&#42;"));
				sb.append('\n');
			}
			for (int i = 0; i < indent; i++) {
				sb.append('\t');
			}
			sb.append(" */");

		} catch (IOException e) {
			// can't happen, because we are using strings from memory
		}
		return sb.toString();
	}

}
