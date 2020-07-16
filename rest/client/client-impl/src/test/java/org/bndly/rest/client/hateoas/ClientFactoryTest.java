package org.bndly.rest.client.hateoas;

/*-
 * #%L
 * REST Client Impl
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

import org.bndly.common.crypto.impl.Base64ServiceImpl;
import org.bndly.rest.client.impl.hateoas.HATEOASClientFactoryImpl;
import org.bndly.rest.client.api.HATEOASClient;
import org.bndly.rest.client.exception.ClientException;
import org.apache.http.impl.client.DefaultHttpClient;

public class ClientFactoryTest {
    
    public void testBuildClient() throws ClientException {
        HATEOASClientFactoryImpl factory = new HATEOASClientFactoryImpl(
                new DefaultHttpClient(), 
                new TestMessageClassesProvider(), 
                new TestBackendAccountProvider(), 
                new TestLanguageProvider(), 
                new TestExceptionThrower(),
				new Base64ServiceImpl()
        );
        
	Foo foo = new Foo();
        try {
            HATEOASClient<Foo> fooClient = factory.build(foo);
            Foo fooReloaded = fooClient.read().execute();
            Foo fooUpdated = fooClient.update().execute(foo);
            Foo fooDeleted = fooClient.delete().execute();
        } catch(IllegalArgumentException e) {
            // there is a AtomLinkHolder annotation missing in the Foo class
        }
    }
}
