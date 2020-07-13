package org.bndly.rest.atomlink.api.annotation;

/*-
 * #%L
 * REST Atom Link API
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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface AtomLink {

	/**
	 * the name of the link in the target object
	 */
	String rel() default "";

	/**
	 * the type of the message that will be returned
	 */
	Class<?> returns() default Void.class;

	/**
	 * the type of the target objects
	 */
	Class<?> target() default Void.class;

	/**
	 * the name of the type of the target objects. this value will be used if the 'target' is Void.class .
	 */
	String targetName() default "";

	/**
	 * the name of the property where the link should be injected in the target object. this can be used to inject a link in a nested object in the target.
	 */
	String targetProperty() default "";

	/**
	 * a list of parameters that will fill the path or query parameters to build the final link URI.
	 */
	Parameter[] parameters() default { };

	/**
	 * a constraint that defines whether the link should be injected or not
	 */
	String constraint() default "";

	/**
	 * true if the query parameters of the current request should be re-appended to the URI of this link
	 */
	boolean reuseQueryParameters() default false;

	/**
	 * a type that implements AtomLinkDescriptor and is able to provide an AtomLinkDescription
	 */
	Class<?> descriptor() default Void.class;

	/**
	 * an optional segment name, that will be placed in front of the uri template
	 */
	String segment() default "";

	/**
	 * if true, the desired content type of the current context will be added as an extension to the url of the link
	 */
	boolean isContextExtensionEnabled() default true;

	/**
	 * if true, the link injector will also inject superclass-annotations into its subclasses
	 * @return
	 */
	boolean allowSubclasses() default false;
}
