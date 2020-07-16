package org.bndly.schema.vendor.def;

/*-
 * #%L
 * Schema Vendor
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

import org.bndly.schema.api.AttributeMediator;
import org.bndly.schema.api.CryptoProvider;
import org.bndly.schema.api.mapper.LobHandler;
import org.bndly.schema.api.services.Accessor;
import org.bndly.schema.json.RecordJsonConverter;
import org.bndly.schema.model.BinaryAttribute;
import org.bndly.schema.model.BooleanAttribute;
import org.bndly.schema.model.CryptoAttribute;
import org.bndly.schema.model.DateAttribute;
import org.bndly.schema.model.DecimalAttribute;
import org.bndly.schema.model.InverseAttribute;
import org.bndly.schema.model.JSONAttribute;
import org.bndly.schema.model.MixinAttribute;
import org.bndly.schema.model.StringAttribute;
import org.bndly.schema.model.TypeAttribute;
import org.bndly.schema.vendor.AttributeMediatorFactory;
import org.bndly.schema.vendor.mediator.BinaryAttributeMediator;
import org.bndly.schema.vendor.mediator.BooleanAttributeMediator;
import org.bndly.schema.vendor.mediator.CryptoAttributeMediator;
import org.bndly.schema.vendor.mediator.DateAttributeMediator;
import org.bndly.schema.vendor.mediator.DecimalAttributeMediator;
import org.bndly.schema.vendor.mediator.InverseAttributeMediator;
import org.bndly.schema.vendor.mediator.JSONAttributeMediator;
import org.bndly.schema.vendor.mediator.MixinAttributeMediator;
import org.bndly.schema.vendor.mediator.StringAttributeMediator;
import org.bndly.schema.vendor.mediator.TypeAttributeMediator;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class DefaultAttributeMediatorFactory implements AttributeMediatorFactory {

	@Override
	public AttributeMediator<BinaryAttribute> createBinaryAttributeMediator(Accessor accessor, RecordJsonConverter recordJsonConverter, LobHandler lobHandler, CryptoProvider cryptoProvider) {
		return new BinaryAttributeMediator<>(lobHandler);
	}

	@Override
	public AttributeMediator<JSONAttribute> createJSONAttributeMediator(Accessor accessor, RecordJsonConverter recordJsonConverter, LobHandler lobHandler, CryptoProvider cryptoProvider) {
		return new JSONAttributeMediator(lobHandler, recordJsonConverter);
	}

	@Override
	public AttributeMediator<BooleanAttribute> createBooleanAttributeMediator(Accessor accessor, RecordJsonConverter recordJsonConverter, LobHandler lobHandler, CryptoProvider cryptoProvider) {
		return new BooleanAttributeMediator();
	}

	@Override
	public AttributeMediator<DateAttribute> createDateAttributeMediator(Accessor accessor, RecordJsonConverter recordJsonConverter, LobHandler lobHandler, CryptoProvider cryptoProvider) {
		return new DateAttributeMediator();
	}

	@Override
	public AttributeMediator<DecimalAttribute> createDecimalAttributeMediator(Accessor accessor, RecordJsonConverter recordJsonConverter, LobHandler lobHandler, CryptoProvider cryptoProvider) {
		return new DecimalAttributeMediator();
	}

	@Override
	public AttributeMediator<InverseAttribute> createInverseAttributeMediator(Accessor accessor, RecordJsonConverter recordJsonConverter, LobHandler lobHandler, CryptoProvider cryptoProvider) {
		return new InverseAttributeMediator();
	}

	@Override
	public AttributeMediator<MixinAttribute> createMixinAttributeMediator(Accessor accessor, RecordJsonConverter recordJsonConverter, LobHandler lobHandler, CryptoProvider cryptoProvider) {
		return new MixinAttributeMediator(accessor);
	}

	@Override
	public AttributeMediator<StringAttribute> createStringAttributeMediator(Accessor accessor, RecordJsonConverter recordJsonConverter, LobHandler lobHandler, CryptoProvider cryptoProvider) {
		return new StringAttributeMediator();
	}

	@Override
	public AttributeMediator<TypeAttribute> createTypeAttributeMediator(Accessor accessor, RecordJsonConverter recordJsonConverter, LobHandler lobHandler, CryptoProvider cryptoProvider) {
		return new TypeAttributeMediator(accessor);
	}

	@Override
	public AttributeMediator<CryptoAttribute> createCryptoAttributeMediator(Accessor accessor, RecordJsonConverter recordJsonConverter, LobHandler lobHandler, CryptoProvider cryptoProvider) {
		return new CryptoAttributeMediator(cryptoProvider, lobHandler);
	}
	
}
