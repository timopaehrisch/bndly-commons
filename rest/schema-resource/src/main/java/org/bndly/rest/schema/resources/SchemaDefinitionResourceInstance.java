package org.bndly.rest.schema.resources;

/*-
 * #%L
 * REST Schema Resource
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

import org.bndly.rest.jaxb.renderer.JAXBResourceRenderer;
import org.bndly.rest.api.Resource;
import org.bndly.rest.api.ResourceProvider;
import org.bndly.rest.api.ResourceURI;
import org.bndly.rest.schema.beans.AttributeBean;
import org.bndly.rest.schema.beans.AttributesList;
import org.bndly.rest.schema.beans.BinaryAttributeBean;
import org.bndly.rest.schema.beans.BooleanAttributeBean;
import org.bndly.rest.schema.beans.CryptoAttributeBean;
import org.bndly.rest.schema.beans.DateAttributeBean;
import org.bndly.rest.schema.beans.DecimalAttributeBean;
import org.bndly.rest.schema.beans.InverseAttributeBean;
import org.bndly.rest.schema.beans.JSONAttributeBean;
import org.bndly.rest.schema.beans.MixinAttributeBean;
import org.bndly.rest.schema.beans.MixinBean;
import org.bndly.rest.schema.beans.NamedAttributeHolderBean;
import org.bndly.rest.schema.beans.SchemaBean;
import org.bndly.rest.schema.beans.StringAttributeBean;
import org.bndly.rest.schema.beans.TypeAttributeBean;
import org.bndly.rest.schema.beans.TypeBean;
import org.bndly.schema.api.services.Engine;
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
import org.bndly.schema.model.Schema;
import org.bndly.schema.model.StringAttribute;
import org.bndly.schema.model.Type;
import org.bndly.schema.model.TypeAttribute;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class SchemaDefinitionResourceInstance implements Resource, JAXBResourceRenderer.JAXBResource {

	private final ResourceURI uri;
	private final ResourceProvider provider;
	private final SchemaBean schemaBean;
	private final Engine engine;

	public SchemaDefinitionResourceInstance(ResourceURI uri, ResourceProvider provider, Engine engine) {
		this.uri = uri;
		this.engine = engine;
		this.provider = provider;
		this.schemaBean = new SchemaBean();
		Schema schema = getSchema();
		if (schema != null) {
			schemaBean.setMixins(new ArrayList<MixinBean>());
			if (schema.getMixins() != null) {
				for (Mixin mixin : schema.getMixins()) {
					schemaBean.getMixins().add(mapMixin(mixin));
				}
			}
			schemaBean.setTypes(new ArrayList<TypeBean>());
			if (schema.getTypes() != null) {
				for (Type type : schema.getTypes()) {
					schemaBean.getTypes().add(mapType(type));
				}
			}
		}
	}

	private TypeBean mapType(Type type) {
		TypeBean t = new TypeBean();
		t.setName(type.getName());
		t.setAttributes(mapAttributes(type.getAttributes()));
		t.setIsAbstract(type.isAbstract());
		if (type.getSuperType() != null) {
			t.setParentTypeName(type.getSuperType().getName());
		}
		List<Mixin> m = type.getMixins();
		if (m != null && !m.isEmpty()) {
			t.setMixins(new ArrayList<String>());
			for (Mixin mixin : m) {
				t.getMixins().add(mixin.getName());
			}
		}
		return t;
	}

	private MixinBean mapMixin(Mixin mixin) {
		MixinBean t = new MixinBean();
		t.setName(mixin.getName());
		t.setAttributes(mapAttributes(mixin.getAttributes()));
		return t;
	}

	private AttributesList mapAttributes(List<Attribute> attributes) {
		AttributesList l = new AttributesList();
		if (attributes != null) {
			for (Attribute attribute : attributes) {
				l.add(mapAttribute(attribute));
			}
		}
		return l;
	}

	private AttributeBean mapAttribute(Attribute a) {
		AttributeBean bean;
		if (StringAttribute.class.isInstance(a)) {
			StringAttribute att = (StringAttribute) a;
			StringAttributeBean tmp = new StringAttributeBean();
			tmp.setLength(att.getLength());
			bean = tmp;
		} else if (DecimalAttribute.class.isInstance(a)) {
			DecimalAttribute att = (DecimalAttribute) a;
			DecimalAttributeBean tmp = new DecimalAttributeBean();
			tmp.setLength(att.getLength());
			tmp.setDecimalPlaces(att.getDecimalPlaces());
			bean = tmp;
		} else if (BooleanAttribute.class.isInstance(a)) {
			BooleanAttribute att = (BooleanAttribute) a;
			BooleanAttributeBean tmp = new BooleanAttributeBean();
			bean = tmp;
		} else if (CryptoAttribute.class.isInstance(a)) {
			CryptoAttribute att = (CryptoAttribute) a;
			CryptoAttributeBean tmp = new CryptoAttributeBean();
			bean = tmp;
		} else if (DateAttribute.class.isInstance(a)) {
			DateAttribute att = (DateAttribute) a;
			DateAttributeBean tmp = new DateAttributeBean();
			bean = tmp;
		} else if (TypeAttribute.class.isInstance(a)) {
			TypeAttribute att = (TypeAttribute) a;
			TypeAttributeBean tmp = new TypeAttributeBean();
			TypeBean t = new TypeBean();
			t.setName(att.getNamedAttributeHolder().getName());
			tmp.setNamedAttributeHolderBean(t);
			bean = tmp;
		} else if (MixinAttribute.class.isInstance(a)) {
			MixinAttribute att = (MixinAttribute) a;
			MixinAttributeBean tmp = new MixinAttributeBean();
			MixinBean m = new MixinBean();
			m.setName(att.getNamedAttributeHolder().getName());
			tmp.setNamedAttributeHolderBean(m);
			bean = tmp;
		} else if (JSONAttribute.class.isInstance(a)) {
			JSONAttribute att = (JSONAttribute) a;
			JSONAttributeBean tmp = new JSONAttributeBean();
			tmp.setNamedAttributeHolder(simpleMapNamedAttributeHolder(att.getNamedAttributeHolder()));
			tmp.setAsByteArray(att.getAsByteArray());
			bean = tmp;
		} else if (BinaryAttribute.class.isInstance(a)) {
			BinaryAttribute att = (BinaryAttribute) a;
			BinaryAttributeBean tmp = new BinaryAttributeBean();
			tmp.setAsByteArray(att.getAsByteArray());
			bean = tmp;
		} else if (InverseAttribute.class.isInstance(a)) {
			InverseAttribute att = (InverseAttribute) a;
			InverseAttributeBean tmp = new InverseAttributeBean();
			NamedAttributeHolder holder = att.getReferencedAttributeHolder();
			NamedAttributeHolderBean m = simpleMapNamedAttributeHolder(holder);
			tmp.setReferencedNamedAttributeHolder(m);
			tmp.setReferencedAttributeName(att.getReferencedAttributeName());
			bean = tmp;
		} else {
			throw new IllegalStateException("unsupported attribute type: " + a.getClass().getName());
		}
		bean.setName(a.getName());
		return bean;
	}

	private NamedAttributeHolderBean simpleMapNamedAttributeHolder(NamedAttributeHolder holder) {
		NamedAttributeHolderBean m;
		if (Mixin.class.isInstance(holder)) {
			m = new MixinBean();
		} else if (Type.class.isInstance(holder)) {
			m = new TypeBean();
		} else {
			throw new IllegalStateException("unsupported named attribute holder");
		}
		m.setName(holder.getName());
		return m;
	}
	
	public Schema getSchema() {
		return engine.getDeployer().getDeployedSchema();
	}

	@Override
	public ResourceProvider getProvider() {
		return provider;
	}

	@Override
	public ResourceURI getURI() {
		return uri;
	}

	@Override
	public Class<?> getRootType() {
		return SchemaBean.class;
	}

	@Override
	public Object getRootObject() {
		return schemaBean;
	}
}
