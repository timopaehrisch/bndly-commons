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

/**
 * 
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public interface AccountStore {
	
	/**
	 * Creates a deactivated and unlocked account. The account has no password assigned and is therefore not activated.
	 * @param accountName the name for the account
	 * @return the created account instance. never null.
	 * @throws AccountCreationException if the account can not be created due to constraints, persistence issues or empty user name
	 */
	public Account createAccount(String accountName) throws AccountCreationException;
	
	/**
	 * Creates a deactivated and unlocked account. The account has a password assigned.
	 * @param accountName the name for the account
	 * @param password the password for the account
	 * @return the created account instance. never null.
	 * @throws AccountCreationException if the account can not be created due to constraints, persistence issues or empty user name/password
	 */
	public Account createAccount(String accountName, String password) throws AccountCreationException;

	/**
	 * Creates a deactivated and locked account. The account has a password assigned.
	 * @param accountName the name for the account
	 * @param password the password for the account
	 * @return the created account instance. never null.
	 * @throws AccountCreationException if the account can not be created due to constraints, persistence issues or empty user name/password
	 */
	public Account createAccountLocked(String accountName, String password) throws AccountCreationException;
	
	/**
	 * Gets the account object for a given user name. Any account can be retrieved regardless of activation or locking state.
	 * @param accountName the name for the account
	 * @return the retrieved account instance. never null.
	 * @throws org.bndly.rest.api.account.NoSuchAccountException if the account can not be found or loaded
	 */
	public Account getAccount(String accountName) throws NoSuchAccountException;
	
}
