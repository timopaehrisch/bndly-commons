package org.bndly.rest.api.account;

/*-
 * #%L
 * REST API
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

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = InitialAccount.class, immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(factory = true, ocd = InitialAccount.Configuration.class)
public final class InitialAccount {

	@ObjectClassDefinition(name = "Initial Account")
	public @interface Configuration {
		@AttributeDefinition(
				name = "Account name",
				description = "The name of the account to create"
		)
		String accountName();
		
		@AttributeDefinition(
				name = "Password",
				description = "The password to set on the created account. The password will be only set, if the account has been created. Existing accounts won't be updated."
		)
		String password();
	}
	

	static final String PROPERTY_ACCOUNT_NAME = "accountName";
	static final String PROPERTY_PASSWORD = "password";
	
	private static final Logger LOG = LoggerFactory.getLogger(InitialAccount.class);

	@Reference
	private AccountStore accountStore;

	@Activate
	protected void activate(Configuration configuration) {
		final String accountName = configuration.accountName();
		final String password = configuration.password();

		if (accountName != null && password != null) {
			try {
				Account account = accountStore.getAccount(accountName);
				LOG.info("Skipping creation of account '{}', because the account already exists. active: {} locked: {}", accountName, account.isActive(), account.isLocked());
			} catch (NoSuchAccountException ex) {
				LOG.info("Creating account '{}'", accountName);
				try {
					Account account = accountStore.createAccount(accountName, password);
					LOG.info("Successfully created account '{}'. Activating account now!", accountName);
					String token = account.createActivationToken();
					account.activate(token);
					LOG.info("Successfully activated account '{}'.", accountName);
				} catch (AccountCreationException e) {
					LOG.error("Failed to create account '{}'", accountName, e);
				} catch (AccountActivationException e) {
					LOG.error("Failed to activate account '{}'", accountName, e);
				}
			}
		}
	}

}
