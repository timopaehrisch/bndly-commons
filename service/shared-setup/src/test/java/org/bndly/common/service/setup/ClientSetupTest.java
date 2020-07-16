package org.bndly.common.service.setup;

/*-
 * #%L
 * Service Shared Client Setup
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

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class ClientSetupTest {

	/*
	@Test
	public void testSetup() throws ClassNotFoundException {
		SchemaServiceStub countrySchemaServiceStub = new SchemaServiceStub();
		countrySchemaServiceStub.setSchemaName("ebx");
		countrySchemaServiceStub.setCustomServiceClassName(CustomCountryServiceImpl.class.getName());
		countrySchemaServiceStub.setGenericServiceClassName(DefaultCountryServiceImpl.class.getName());
		countrySchemaServiceStub.setFullApiClassName(CountryService.class.getName());
		
		SchemaServiceStub cartItemschemaServiceStub = new SchemaServiceStub();
		cartItemschemaServiceStub.setSchemaName("ebx");
		cartItemschemaServiceStub.setCustomServiceClassName(CustomCartItemServiceImpl.class.getName());
		cartItemschemaServiceStub.setGenericServiceClassName(DefaultCartItemServiceImpl.class.getName());
		cartItemschemaServiceStub.setFullApiClassName(CartItemService.class.getName());
		
		SchemaServiceStub cartSchemaServiceStub = new SchemaServiceStub();
		cartSchemaServiceStub.setSchemaName("ebx");
		cartSchemaServiceStub.setCustomServiceClassName(CustomCartServiceImpl.class.getName());
		cartSchemaServiceStub.setGenericServiceClassName(DefaultCartServiceImpl.class.getName());
		cartSchemaServiceStub.setFullApiClassName(CartService.class.getName());
		
		ClientSetup clientSetup = new ClientSetup()
				.setHostUrl("http://localhost:8081/bndly/")
				.setDefaultLanguage("en")
				.addSchemaReference(new SchemaReference("ebx", "org.bndly.rest.beans.ebx"))
				.addSchemaServiceConstructionGuide(new SchemaServiceConstructionGuide("ebx", "org.bndly.shop.client.service.impl", "org.bndly.shop.client.service.api"))
				.addSchemaServiceStub(countrySchemaServiceStub)
				.addSchemaServiceStub(cartItemschemaServiceStub)
				.addSchemaServiceStub(cartSchemaServiceStub)
				.addServiceReference(ServiceReference.buildByType(UserIDProvider.class, new NoOpUserIDProviderImpl()))
				.addJAXBMessageClassProvider(new org.bndly.rest.common.beans.JAXBMessageClassProviderImpl())
				.addJAXBMessageClassProvider(new org.bndly.rest.schema.beans.JAXBMessageClassProviderImpl())
				.addJAXBMessageClassProvider(new org.bndly.rest.beans.ebx.JAXBMessageClassProviderImpl())
				.init();
		CountryService countryService = (CountryService) countrySchemaServiceStub.getFullApi();
		Assert.assertNotNull(countryService);
		Collection<Country> all = countryService.listAll();
		Assert.assertNotNull(all);
		UserService userService = clientSetup.getServiceByType(UserService.class);
		Assert.assertNotNull(userService);
	}
	*/
}
