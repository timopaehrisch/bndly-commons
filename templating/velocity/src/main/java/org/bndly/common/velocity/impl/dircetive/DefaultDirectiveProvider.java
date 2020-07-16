package org.bndly.common.velocity.impl.dircetive;

/*-
 * #%L
 * Velocity
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

import org.bndly.common.velocity.api.DirectiveProvider;
import org.apache.velocity.runtime.RuntimeInstance;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = DirectiveProvider.class)
@Designate(ocd = DefaultDirectiveProvider.Configuration.class)
public class DefaultDirectiveProvider implements DirectiveProvider {

	@ObjectClassDefinition(
			name = "Default Directive Provider",
			description = "This provider enables partials, xss protection and translation support"
	)
	public @interface Configuration {
	}
	
	@Override
	public void registerDirectives(RuntimeInstance runtimeInstance) {
		runtimeInstance.addDirective(new Partial());
		runtimeInstance.addDirective(new XSS());
		runtimeInstance.addDirective(new Translate());
	}

	@Override
	public void unregisterDirectives(RuntimeInstance runtimeInstance) {
		runtimeInstance.removeDirective(Partial.NAME);
		runtimeInstance.removeDirective(XSS.NAME);
		runtimeInstance.removeDirective(Translate.NAME);
	}

}
