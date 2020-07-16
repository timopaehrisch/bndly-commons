package org.bndly.rest.security.resources;

/*-
 * #%L
 * REST Security Resource
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

import org.bndly.rest.api.Context;
import org.bndly.rest.api.ResourceURI;
import org.bndly.rest.api.StatusWriter;
import org.bndly.rest.api.account.Account;
import org.bndly.rest.api.account.AccountActivationException;
import org.bndly.rest.api.account.AccountCreationException;
import org.bndly.rest.api.account.AccountStore;
import org.bndly.rest.api.account.NoSuchAccountException;
import org.bndly.rest.atomlink.api.annotation.AtomLink;
import org.bndly.rest.common.beans.Services;
import org.bndly.rest.common.beans.error.ErrorRestBean;
import org.bndly.rest.controller.api.CacheControl;
import org.bndly.rest.controller.api.ControllerResourceRegistry;
import org.bndly.rest.controller.api.DELETE;
import org.bndly.rest.controller.api.Documentation;
import org.bndly.rest.controller.api.DocumentationResponse;
import org.bndly.rest.controller.api.GET;
import org.bndly.rest.controller.api.Meta;
import org.bndly.rest.controller.api.POST;
import org.bndly.rest.controller.api.Path;
import org.bndly.rest.controller.api.PathParam;
import org.bndly.rest.controller.api.Response;
import org.bndly.rest.security.beans.AccountActivationTokenRestBean;
import org.bndly.rest.security.beans.AccountCreationRestBean;
import org.bndly.rest.security.beans.AccountLookupRestBean;
import org.bndly.rest.security.beans.AccountPasswordRestBean;
import org.bndly.rest.security.beans.AccountRestBean;
import java.util.List;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = AccountStoreResource.class, immediate = true)
@Path("home")
public class AccountStoreResource {

	private static final Logger LOG = LoggerFactory.getLogger(AccountStoreResource.class);
	
	@Reference
	private ControllerResourceRegistry controllerResourceRegistry;
	@Reference
	private AccountStore accountStore;

	@Activate
	public void activate() {
		controllerResourceRegistry.deploy(this);
	}

	@Deactivate
	public void deactivate() {
		controllerResourceRegistry.undeploy(this);
	}

	@POST
	@AtomLink(rel = "createAccount", target = Services.class)
	@Documentation(
			authors = "bndly@bndly.org",
			consumes = Documentation.ANY_CONTENT_TYPE,
			value = "Creates an account within the AccountStore. The payload contains the information about the account name, password and locking state.",
			summary = "Create an account",
			responses = {
				@DocumentationResponse(
						code = StatusWriter.Code.CREATED,
						description = "If the account could be created, this response will be returned. The Location-Header contains the URL to the created account."
				),
				@DocumentationResponse(
						code = StatusWriter.Code.INTERNAL_SERVER_ERROR,
						description = "If the account could not be created, this response will be returned. Reasons for this could be account name collisions.",
						messageType = ErrorRestBean.class
				)
			}
	)
	public Response createAccount(AccountCreationRestBean accountCreationRestBean, @Meta Context context) throws AccountCreationException {
		boolean locked = accountCreationRestBean.getLocked() != null && accountCreationRestBean.getLocked();
		if (accountCreationRestBean.getPassword() == null) {
			if (locked) {
				accountStore.createAccount(accountCreationRestBean.getAccountName()).lock();
			} else {
				accountStore.createAccount(accountCreationRestBean.getAccountName());
			}
		} else {
			if (locked) {
				accountStore.createAccountLocked(accountCreationRestBean.getAccountName(), accountCreationRestBean.getPassword());
			} else {
				accountStore.createAccount(accountCreationRestBean.getAccountName(), accountCreationRestBean.getPassword());
			}
		}
		String location = context.createURIBuilder().pathElement("home").pathElement(accountCreationRestBean.getAccountName()).build().asString();
		return Response.created(location);
	}
	
	@POST
	@Path("find")
	@AtomLink(rel = "findAccount", target = Services.class)
	@Documentation(
			authors = "bndly@bndly.org",
			consumes = Documentation.ANY_CONTENT_TYPE,
			value = "Redirects to a URL, where the account with the provided name can be retrieved",
			summary = "Find an account",
			responses = {
				@DocumentationResponse(
						code = StatusWriter.Code.SEE_OTHER,
						description = "If an account name is provided, this response will be returned."
				),
				@DocumentationResponse(
						code = StatusWriter.Code.NOT_FOUND,
						description = "If not account name is provided, this response will be returned.",
						messageType = ErrorRestBean.class
				)
			}
	)
	public Response findAccount(AccountLookupRestBean accountLookupRestBean, @Meta Context context) throws NoSuchAccountException {
		String accountName = accountLookupRestBean.getAccountName();
		if (accountName == null) {
			throw new NoSuchAccountException("account name was not provided");
		}
		String location = context.createURIBuilder().pathElement("home").pathElement(accountLookupRestBean.getAccountName()).build().asString();
		return Response.seeOther(location);
	}
	
	@Path("{name}")
	@GET
	@CacheControl(preventCaching = true)
	@AtomLink(target = AccountRestBean.class, isContextExtensionEnabled = false)
	@Documentation(
			authors = "bndly@bndly.org",
			produces = Documentation.ANY_CONTENT_TYPE,
			value = "Loads account data and returns it in the response.",
			summary = "Get an account",
			responses = {
				@DocumentationResponse(
						code = StatusWriter.Code.OK,
						description = "If the account could be retrieved, this response will be returned.",
						messageType = AccountRestBean.class
				),
				@DocumentationResponse(
						code = StatusWriter.Code.NOT_FOUND,
						description = "If the account does not exist (anymore), this response will be returned.",
						messageType = ErrorRestBean.class
				)
			}
	)
	public Response getAccount(@PathParam("name") String name, @Meta Context context) throws NoSuchAccountException {
		name = rebuildAccountName(context);
		Account account = accountStore.getAccount(name);
		AccountRestBean accountRestBean = new AccountRestBean();
		accountRestBean.setActive(account.isActive());
		accountRestBean.setLocked(account.isLocked());
		accountRestBean.setAccountName(name);
		return Response.ok(accountRestBean);
	}
	
	@Path("{name}")
	@DELETE
	@CacheControl(preventCaching = true)
	@AtomLink(target = AccountRestBean.class, isContextExtensionEnabled = false)
	@Documentation(
			authors = "bndly@bndly.org",
			value = "Deletes an account, if such an account exists.",
			summary = "Delete an account",
			responses = {
				@DocumentationResponse(
						code = StatusWriter.Code.NO_CONTENT,
						description = "If no critical exceptions occur, this response will be returned."
				)
			}
	)
	public Response deleteAccount(@PathParam("name") String name, @Meta Context context) {
		name = rebuildAccountName(context);
		try {
			accountStore.getAccount(name).delete();
		} catch (NoSuchAccountException ex) {
			LOG.warn("could not delete account, because it did not exist");
		}
		return Response.NO_CONTENT;
	}
	
	@Path("changePassword/{name}")
	@POST
	@CacheControl(preventCaching = true)
	@AtomLink(rel = "changePassword", target = AccountRestBean.class, isContextExtensionEnabled = false)
	@Documentation(
			authors = "bndly@bndly.org",
			consumes = Documentation.ANY_CONTENT_TYPE,
			value = "Changes the password for an existing account.",
			summary = "Change account password",
			responses = {
				@DocumentationResponse(
						code = StatusWriter.Code.NO_CONTENT,
						description = "If the account exists and the password could be changed, this response will be returned."
				),
				@DocumentationResponse(
						code = StatusWriter.Code.BAD_REQUEST,
						description = "If the account exists, but the password could not be changed for whatever reason, this response will be returned.",
						messageType = ErrorRestBean.class
				),
				@DocumentationResponse(
						code = StatusWriter.Code.NOT_FOUND,
						description = "If the account does not exist (anymore), this response will be returned.",
						messageType = ErrorRestBean.class
				)
			}
	)
	public Response changePassword(@PathParam("name") String name, AccountPasswordRestBean accountPasswordRestBean, @Meta Context context) throws NoSuchAccountException {
		name = rebuildAccountName(context);
		Account account = accountStore.getAccount(name);
		if (account.changePassword(accountPasswordRestBean.getPassword())) {
			return Response.NO_CONTENT;
		} else {
			ErrorRestBean errorBean = new ErrorRestBean();
			errorBean.setName("accountPasswordNotChanged");
			return Response.status(StatusWriter.Code.BAD_REQUEST.getHttpCode()).entity(errorBean);
		}
	}
	
	@Path("login/{name}")
	@POST
	@CacheControl(preventCaching = true)
	@AtomLink(rel = "login", target = AccountRestBean.class, isContextExtensionEnabled = false)
	@Documentation(
			authors = "bndly@bndly.org",
			consumes = Documentation.ANY_CONTENT_TYPE,
			value = "Login into an account, by checking a provided password.",
			summary = "Login into account",
			responses = {
				@DocumentationResponse(
						code = StatusWriter.Code.NO_CONTENT,
						description = "If the account exists and the password matches, this response will be returned."
				),
				@DocumentationResponse(
						code = StatusWriter.Code.BAD_REQUEST,
						description = "If the account exists, but the password did not match, this response will be returned.",
						messageType = ErrorRestBean.class
				),
				@DocumentationResponse(
						code = StatusWriter.Code.NOT_FOUND,
						description = "If the account does not exist (anymore), this response will be returned.",
						messageType = ErrorRestBean.class
				)
			}
	)
	public Response login(@PathParam("name") String name, AccountPasswordRestBean accountPasswordRestBean, @Meta Context context) throws NoSuchAccountException {
		name = rebuildAccountName(context);
		if (accountStore.getAccount(name).checkPassword(accountPasswordRestBean.getPassword())) {
			return Response.NO_CONTENT;
		} else {
			ErrorRestBean errorBean = new ErrorRestBean();
			errorBean.setName("accountPasswordNotMatching");
			return Response.status(StatusWriter.Code.BAD_REQUEST.getHttpCode()).entity(errorBean);
		}
	}
	
	@Path("lock/{name}")
	@POST
	@AtomLink(rel = "lock", constraint = "${this.getLocked() == false}",target = AccountRestBean.class, isContextExtensionEnabled = false)
	@Documentation(
			authors = "bndly@bndly.org",
			consumes = Documentation.ANY_CONTENT_TYPE,
			value = "Lock an account",
			summary = "Lock an account",
			responses = {
				@DocumentationResponse(
						code = StatusWriter.Code.NO_CONTENT,
						description = "If the account exists and the account could be locked, this response will be returned."
				),
				@DocumentationResponse(
						code = StatusWriter.Code.NOT_FOUND,
						description = "If the account does not exist (anymore), this response will be returned.",
						messageType = ErrorRestBean.class
				)
			}
	)
	public Response lock(@PathParam("name") String name, @Meta Context context) throws NoSuchAccountException {
		name = rebuildAccountName(context);
		accountStore.getAccount(name).lock();
		return Response.NO_CONTENT;
	}
	
	@Path("unlock/{name}")
	@POST
	@AtomLink(rel = "unlock", constraint = "${this.getLocked() == true}",target = AccountRestBean.class, isContextExtensionEnabled = false)
	@Documentation(
			authors = "bndly@bndly.org",
			consumes = Documentation.ANY_CONTENT_TYPE,
			value = "Unlock an account",
			summary = "Unlock an account",
			responses = {
				@DocumentationResponse(
						code = StatusWriter.Code.NO_CONTENT,
						description = "If the account exists and the account could be unlocked, this response will be returned."
				),
				@DocumentationResponse(
						code = StatusWriter.Code.NOT_FOUND,
						description = "If the account does not exist (anymore), this response will be returned.",
						messageType = ErrorRestBean.class
				)
			}
	)
	public Response unlock(@PathParam("name") String name, @Meta Context context) throws NoSuchAccountException {
		name = rebuildAccountName(context);
		accountStore.getAccount(name).unlock();
		return Response.NO_CONTENT;
	}
	
	@Path("token/{name}")
	@POST
	@AtomLink(rel = "requestActivationToken", constraint = "${this.getActive() == false}",target = AccountRestBean.class, isContextExtensionEnabled = false)
	@Documentation(
			authors = "bndly@bndly.org",
			consumes = Documentation.ANY_CONTENT_TYPE,
			produces = Documentation.ANY_CONTENT_TYPE,
			value = "Generates an activation token for an account",
			summary = "Generate activation token",
			responses = {
				@DocumentationResponse(
						code = StatusWriter.Code.OK,
						description = "If the account exists and the activation token could be generated, this response will be returned. The response will contain the token.",
						messageType = AccountActivationTokenRestBean.class
				),
				@DocumentationResponse(
						code = StatusWriter.Code.NOT_FOUND,
						description = "If the account does not exist (anymore), this response will be returned.",
						messageType = ErrorRestBean.class
				)
			}
	)
	public Response requestActivationToken(@PathParam("name") String name, @Meta Context context) throws NoSuchAccountException, AccountActivationException {
		name = rebuildAccountName(context);
		String token = accountStore.getAccount(name).createActivationToken();
		AccountActivationTokenRestBean accountActivationToken = new AccountActivationTokenRestBean();
		accountActivationToken.setToken(token);
		accountActivationToken.setAccountName(name);
		return Response.ok(accountActivationToken);
	}
	
	@Path("activate/{name}")
	@POST
	@AtomLink(rel = "activate", constraint = "${this.getActive() == false}",target = AccountRestBean.class, isContextExtensionEnabled = false)
	@Documentation(
			authors = "bndly@bndly.org",
			consumes = Documentation.ANY_CONTENT_TYPE,
			produces = Documentation.ANY_CONTENT_TYPE,
			value = "Activates an account with the provided token.",
			summary = "Activate an account",
			responses = {
				@DocumentationResponse(
						code = StatusWriter.Code.NO_CONTENT,
						description = "If the account exists and the activation could be performed, this response will be returned."
				),
				@DocumentationResponse(
						code = StatusWriter.Code.INTERNAL_SERVER_ERROR,
						description = "If the account exists and the activation could not be performed, this response will be returned.",
						messageType = ErrorRestBean.class
				),
				@DocumentationResponse(
						code = StatusWriter.Code.NOT_FOUND,
						description = "If the account does not exist (anymore), this response will be returned.",
						messageType = ErrorRestBean.class
				)
			}
	)
	public Response activate(@PathParam("name") String name, AccountActivationTokenRestBean tokenRestBean, @Meta Context context) throws NoSuchAccountException, AccountActivationException {
		name = rebuildAccountName(context);
		accountStore.getAccount(name).activate(tokenRestBean.getToken());
		return Response.NO_CONTENT;
	}
	
	@Path("deactivate/{name}")
	@POST
	@AtomLink(rel = "deactivate", constraint = "${this.getActive() == true}",target = AccountRestBean.class, isContextExtensionEnabled = false)
	@Documentation(
			authors = "bndly@bndly.org",
			consumes = Documentation.ANY_CONTENT_TYPE,
			produces = Documentation.ANY_CONTENT_TYPE,
			value = "Deactivates an account",
			summary = "Deactivate an account",
			responses = {
				@DocumentationResponse(
						code = StatusWriter.Code.NO_CONTENT,
						description = "If the account exists and the deactivation could be performed, this response will be returned."
				),
				@DocumentationResponse(
						code = StatusWriter.Code.INTERNAL_SERVER_ERROR,
						description = "If the account exists and the deactivation could not be performed, this response will be returned.",
						messageType = ErrorRestBean.class
				),
				@DocumentationResponse(
						code = StatusWriter.Code.NOT_FOUND,
						description = "If the account does not exist (anymore), this response will be returned.",
						messageType = ErrorRestBean.class
				)
			}
	)
	public Response deactivate(@PathParam("name") String name, @Meta Context context) throws NoSuchAccountException, AccountActivationException {
		name = rebuildAccountName(context);
		accountStore.getAccount(name).deactivate();
		return Response.NO_CONTENT;
	}
	
	private String rebuildAccountName(Context context) {
		StringBuilder sb = new StringBuilder();
		ResourceURI uri = context.getURI();
		ResourceURI.Path path = uri.getPath();
		if (path != null) {
			List<String> elements = path.getElements();
			if (elements != null && elements.size() > 0) {
				sb.append(elements.get(elements.size() - 1));
			}
		}
		List<ResourceURI.Selector> selectors = uri.getSelectors();
		if (selectors != null) {
			for (ResourceURI.Selector selector : selectors) {
				sb.append(".").append(selector.getName());
			}
		}
		ResourceURI.Extension extension = uri.getExtension();
		if (extension != null) {
			sb.append(".").append(extension.getName());
		}
		return sb.toString();
	}
}
