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
import org.bndly.schema.model.MixinAttribute;
import org.bndly.schema.model.NamedAttributeHolder;
import org.bndly.schema.model.NamedAttributeHolderAttribute;
import org.bndly.schema.model.Schema;
import org.bndly.schema.model.SchemaUtil;
import org.bndly.schema.model.StringAttribute;
import org.bndly.schema.model.Type;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Generates Schema Beans from a predefined Schema
 */
@Mojo(
		name = "generateXmlBeans"
)
public class XmlSchemaBeanGeneratorMojo extends AbstractSchemaBasedBeanGeneratorMojo {
	/**
	 * The target folder for generated files
	 */
	@Parameter(defaultValue = "false")
	protected boolean prefixingEnabled;
	
	private static enum PrefixingMode {
		DEFAULT,
		SKIP,
		ENFORCE
	}
	
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
			CodeBlock provider = ctx.create(CodeBlock.class);
			provider.line("package ").append(ctx.getBasePackage()).append(";");
			CodeBlock importBlock = provider.createContained(CodeBlock.class);
			importBlock.line("import " + Collection.class.getName() + ";");
			importBlock.line("import " + HashSet.class.getName() + ";");
			importBlock.line("import org.bndly.rest.atomlink.api.JAXBMessageClassProvider;");
			importBlock.line("import org.osgi.service.component.annotations.Component;");
			provider.line("@Component(service = JAXBMessageClassProvider.class, immediate = true, property = {\"schema=" + schema.getName() + "\",\"schemaRestBeanPackage=" + ctx.getBasePackage() + "\",\"schemaRestBeanProvider:Boolean=true\"})");
			provider.line("public class JAXBMessageClassProviderImpl implements JAXBMessageClassProvider");
			CodeBracket classContent = provider.createContained(CodeBracket.class);
			classContent.line("private final static Collection<Class<?>> classes;");
			classContent.line("static");
			CodeBracket initBlock = classContent.createContained(CodeBracket.class);
			initBlock.line("classes = new HashSet<>();");
			classContent.line("@Override");
			classContent.line("public Collection<Class<?>> getJAXBMessageClasses()");
			classContent.createContained(CodeBracket.class).line("return classes;");

			for (Type type : types) {
				if (!type.isAbstract()) {
					initBlock.line("classes.add(" + type.getName() + "RestBean.class);");
					importStateHolder.reset();
					CodeBlock code = generateRestBeanCodeForType(type, ctx);
					writeCodeToFile(code, targetPath, type, "RestBean");
				}

				importStateHolder.reset();
				CodeBlock code = generateReferenceRestBeanCodeForType(type, ctx);
				initBlock.line("classes.add(" + type.getName() + "ReferenceRestBean.class);");
				writeCodeToFile(code, targetPath, type, "ReferenceRestBean");

				importStateHolder.reset();
				code = generateListRestBeanCodeForType(type, ctx);
				initBlock.line("classes.add(" + type.getName() + "ListRestBean.class);");
				writeCodeToFile(code, targetPath, type, "ListRestBean");
			}
			writeCodeToFile(provider, targetPath, "JAXBMessageClassProviderImpl.java");
		}
	}
	
	
	private CodeBlock generateListRestBeanCodeForType(Type type, CodeGenerationContext ctx) {
		getLog().info("generating reference java class for type " + type.getName());
		CodeBlock block = ctx.create(CodeBlock.class);
		CodeBlock importBlock = createClassBaseBlock(block, ctx.getBasePackage(), getTypeXmlElementName(type, PrefixingMode.DEFAULT) + "List", getTypeXmlElementName(type, PrefixingMode.ENFORCE) + "List");
		CodeLine l = block.line("public class ").append(type.getName()).append("ListRestBean");
		assertIsImported(importBlock, "org.bndly.rest.common.beans.ListRestBean");
		l.append(" extends ").append("ListRestBean<").append(type.getName()).append("ReferenceRestBean>");
		CodeBracket bracket = block.createContained(CodeBracket.class);

		CodeBlock fieldBlock = bracket.createContained(CodeBlock.class);
		appendXmlElementsForEachType(fieldBlock, null, importBlock, type);
		assertIsImported(importBlock, List.class);
		fieldBlock.line("private List<").append(type.getName()).append("ReferenceRestBean> items;");

		CodeBlock methodBlock = bracket.createContained(CodeBlock.class);

		methodBlock.line("@Override");
		methodBlock.line("public void setItems(List<").append(type.getName()).append("ReferenceRestBean> items) ");
		methodBlock.createContained(CodeBracket.class).line("this.items = items;");

		methodBlock.line("@Override");
		methodBlock.line("public List<").append(type.getName()).append("ReferenceRestBean> getItems() ");
		methodBlock.createContained(CodeBracket.class).line("return items;");
		return block;
	}

	private CodeBlock generateReferenceRestBeanCodeForType(Type type, CodeGenerationContext ctx) {
		getLog().info("generating reference java class for type " + type.getName());
		CodeBlock block = ctx.create(CodeBlock.class);
		CodeBlock importBlock = createClassBaseBlock(block, ctx.getBasePackage(), getTypeXmlElementName(type, PrefixingMode.DEFAULT) + "Ref", getTypeXmlElementName(type, PrefixingMode.ENFORCE) + "Ref");
		if (!type.isAbstract()) {
			assertIsImported(importBlock, "org.bndly.rest.atomlink.api.annotation.Reference");
			block.line("@Reference");
		}
		CodeLine l = block.line("public");
		if (type.isAbstract()) {
			l.append(" abstract");
		}

		String extendedTypeName;
		l.append(" class ").append(type.getName()).append("ReferenceRestBean");
		if (type.getSuperType() != null) {
			extendedTypeName = type.getSuperType().getName() + "ReferenceRestBean";
		} else {
			assertIsImported(importBlock, "org.bndly.rest.common.beans.RestBean");
			extendedTypeName = "RestBean";
		}
		l.append(" extends ").append(extendedTypeName);
		CodeBracket bracket = block.createContained(CodeBracket.class);

		if (!type.isAbstract() && !type.isVirtual()) {
			CodeBlock fieldBlock = bracket.createContained(CodeBlock.class);
			assertIsImported(importBlock, XmlElement.class);
			assertIsImported(importBlock, "org.bndly.rest.atomlink.api.annotation.BeanID");
			fieldBlock.line("@XmlElement");
			fieldBlock.line("@BeanID");
			fieldBlock.line("private Long id;");

			CodeBlock methodBlock = bracket.createContained(CodeBlock.class);

			methodBlock.line("public void setId(Long id) ");
			methodBlock.createContained(CodeBracket.class).line("this.id = id;");

			methodBlock.line("public Long getId() ");
			methodBlock.createContained(CodeBracket.class).line("return id;");
		}
		return block;
	}

	private CodeBlock generateRestBeanCodeForType(Type type, CodeGenerationContext ctx) {
		getLog().info("generating java class for type " + type.getName());
		CodeBlock block = ctx.create(CodeBlock.class);
		CodeBlock importBlock = createClassBaseBlock(block, ctx.getBasePackage(), getTypeXmlElementName(type, PrefixingMode.DEFAULT), getTypeXmlElementName(type, PrefixingMode.ENFORCE));
		CodeLine l = block.line("public");
		if (type.isAbstract()) {
			l.append(" abstract");
		}
		assertIsImported(importBlock, "org.bndly.rest.common.beans.SmartReferable");
		l.append(" class ")
				.append(type.getName()).append("RestBean")
				.append(" extends ").append(type.getName()).append("ReferenceRestBean")
				.append(" implements ").append("SmartReferable");
		CodeBracket bracket = block.createContained(CodeBracket.class);
		CodeBlock fieldBlock = bracket.createContained(CodeBlock.class);
		CodeBlock methodBlock = bracket.createContained(CodeBlock.class);
		generateSmartReferenceImplementation(fieldBlock, methodBlock, importBlock);
		generateFieldAndGetterAndSetter(type, fieldBlock, methodBlock, importBlock);
		return block;
	}

	private CodeBlock createClassBaseBlock(CodeBlock block, String packageName, String typeElementName, String xmlTypeName) {
		block.line("package ").append(packageName).append(";");
		CodeBlock importBlock = block.createContained(CodeBlock.class);
		assertIsImported(importBlock, XmlType.class);
		assertIsImported(importBlock, XmlRootElement.class);
		assertIsImported(importBlock, XmlAccessorType.class);
		assertIsImported(importBlock, XmlAccessType.class);
		block.line("@XmlType(name=\"").append(xmlTypeName).append("\")");
		block.line("@XmlRootElement(name=\"").append(typeElementName).append("\")");
		block.line("@XmlAccessorType(XmlAccessType.NONE)");
		return importBlock;
	}

	private String getJavaTypeNameForAttribute(Attribute attribute, CodeBlock importBlock) {
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
				assertIsImported(importBlock, BigDecimal.class);
				return "BigDecimal";
			}
		} else if (BooleanAttribute.class.isInstance(attribute)) {
			return "Boolean";
		} else if (DateAttribute.class.isInstance(attribute)) {
			assertIsImported(importBlock, Date.class);
			return "Date";
		} else if (NamedAttributeHolderAttribute.class.isInstance(attribute)) {
			NamedAttributeHolderAttribute att = NamedAttributeHolderAttribute.class.cast(attribute);
			return att.getNamedAttributeHolder().getName() + "ReferenceRestBean";
		} else if (InverseAttribute.class.isInstance(attribute)) {
			InverseAttribute att = InverseAttribute.class.cast(attribute);
			return att.getReferencedAttributeHolder().getName() + "ListRestBean";
		} else if (JSONAttribute.class.isInstance(attribute)) {
			JSONAttribute att = JSONAttribute.class.cast(attribute);
			return att.getNamedAttributeHolder().getName() + "RestBean";
		}
		throw new IllegalArgumentException("unsupported attribute type");
	}

	private void generateFieldAndGetterAndSetter(NamedAttributeHolder nah, CodeBlock fieldBlock, CodeBlock methodBlock, CodeBlock importBlock) {
		List<Attribute> atts = SchemaUtil.collectAttributes(nah);
		if (atts != null) {
			for (Attribute attribute : atts) {
				if (!BinaryAttribute.class.isInstance(attribute) || JSONAttribute.class.isInstance(attribute)) {
					if (attribute.isVirtual()) {
						if (MixinAttribute.class.isInstance(attribute)) {
							continue;
						}
						if (InverseAttribute.class.isInstance(attribute)) {
							InverseAttribute ia = (InverseAttribute) attribute;
							NamedAttributeHolder rah = ia.getReferencedAttributeHolder();
							if (rah.isVirtual() && Mixin.class.isInstance(rah)) {
								continue;
							}
						}
					}
					String javaTypeName = getJavaTypeNameForAttribute(attribute, importBlock);
					String ucAttributeName = attribute.getName();
					if (ucAttributeName.length() > 1) {
						ucAttributeName = ucAttributeName.substring(0, 1).toUpperCase() + ucAttributeName.substring(1);
					} else {
						ucAttributeName = ucAttributeName.toUpperCase();
					}
					if (NamedAttributeHolderAttribute.class.isInstance(attribute)) {
						assertIsImported(importBlock, XmlElements.class);
						NamedAttributeHolder holder = NamedAttributeHolderAttribute.class.cast(attribute).getNamedAttributeHolder();
						if (Mixin.class.isInstance(holder)) {
							javaTypeName = Object.class.getSimpleName();
						}
						appendXmlElementsForEachType(fieldBlock, (NamedAttributeHolderAttribute) attribute, importBlock, holder);
					} else {
						assertIsImported(importBlock, XmlElement.class);
						fieldBlock.line("@XmlElement");
					}
					fieldBlock.line("private ").append(javaTypeName).append(" ").append(attribute.getName()).append(";");

					methodBlock.line("public ").append(javaTypeName).append(" get").append(ucAttributeName).append("()");
					methodBlock.createContained(CodeBracket.class).line("return ").append(attribute.getName()).append(";");

					methodBlock.line("public void set").append(ucAttributeName).append("(").append(javaTypeName).append(" ").append(attribute.getName()).append(")");
					methodBlock.createContained(CodeBracket.class).line("this.").append(attribute.getName()).append("=").append(attribute.getName()).append(";");
				}
			}
		}
	}

	private boolean appendExtendedAttributeHolder(boolean first, CodeLine l, NamedAttributeHolder nah) {
		if (first) {
			first = false;
			l.append(" extends ");
		} else {
			l.append(", ");
		}
		l.append(nah.getName());
		l.append("RestBean");
		return first;
	}

	private void assertIsImported(CodeBlock importBlock, Class<?> typeToImport) {
		assertIsImported(importBlock, typeToImport.getName());
	}

	private void assertIsImported(CodeBlock importBlock, String fullName) {
		if (!importStateHolder.hasBeenImported(fullName)) {
			importBlock.createContained(CodeImport.class, fullName);
		}
	}

	private CodeLine appendXmlElementForType(NamedAttributeHolderAttribute attribute, Type type, CodeBlock fieldBlock, CodeLine prevLine, CodeBlock importBlock) {
		CodeLine l = prevLine;
		if (!type.isAbstract()) {
			if (prevLine != null) {
				prevLine.append(",");
			}
			assertIsImported(importBlock, XmlElement.class);
			String typeElementName = getTypeXmlElementName(type, PrefixingMode.SKIP);
			if (attribute != null) {
				typeElementName = attribute.getName() + typeElementName.substring(0, 1).toUpperCase() + typeElementName.substring(1);
			}
			fieldBlock.line("@XmlElement(name=\"").append(typeElementName).append("Ref\", type=").append(type.getName()).append("ReferenceRestBean.class),");
			l = fieldBlock.line("@XmlElement(name=\"").append(typeElementName).append("\", type=").append(type.getName()).append("RestBean.class)");
		}
		if (type.getSubTypes() != null) {
			for (Type type1 : type.getSubTypes()) {
				l = appendXmlElementForType(attribute, type1, fieldBlock, l, importBlock);
			}
		}
		return l;
	}

	private String getTypeXmlElementName(Type type, PrefixingMode prefixingMode) {
		String typeElementName = type.getName();
		if (typeElementName.length() > 1) {
			typeElementName = typeElementName.substring(0, 1).toLowerCase() + typeElementName.substring(1);
		} else {
			typeElementName = typeElementName.toLowerCase();
		}
		if (
				(prefixingMode == PrefixingMode.SKIP)
				|| (prefixingMode == PrefixingMode.DEFAULT && !prefixingEnabled)
		) {
			return typeElementName;
		} else {
			return type.getSchema().getName() + "_" + typeElementName;
		}
	}

	private void appendXmlElementsForEachType(CodeBlock fieldBlock, NamedAttributeHolderAttribute attribute, CodeBlock importBlock, NamedAttributeHolder holder) {
		assertIsImported(importBlock, XmlElements.class);
		fieldBlock.line("@XmlElements({");
		List<Type> possibleTypes = new ArrayList<>();
		if (Type.class.isInstance(holder)) {
			Type t = Type.class.cast(holder);
			possibleTypes.add(t);
		} else if (Mixin.class.isInstance(holder)) {
			Mixin m = Mixin.class.cast(holder);
			List<Type> l = m.getMixedInto();
			if (l != null) {
				possibleTypes.addAll(l);
			}
		}
		CodeLine l = null;
		for (Type type : possibleTypes) {
			l = appendXmlElementForType(attribute, type, fieldBlock, l, importBlock);
		}

		fieldBlock.line("})");
	}

	private void generateSmartReferenceImplementation(CodeBlock fieldBlock, CodeBlock methodBlock, CodeBlock importBlock) {
		assertIsImported(importBlock, XmlAttribute.class);
		fieldBlock.line("@XmlAttribute");
		fieldBlock.line("private Boolean smartRef;");
		methodBlock.line("@Override");
		methodBlock.line("public Boolean getSmartRef()");
		methodBlock.createContained(CodeBracket.class).line("return smartRef;");
		methodBlock.line("@Override");
		methodBlock.line("public void setSmartRef(Boolean smartRef)");
		methodBlock.createContained(CodeBracket.class).line("this.smartRef = smartRef;");
	}
}
