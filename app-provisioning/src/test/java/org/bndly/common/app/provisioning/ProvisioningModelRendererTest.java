package org.bndly.common.app.provisioning;

/*-
 * #%L
 * App Provisioning
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

import org.bndly.common.app.provisioning.model.ProvisioningModel;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for {@link ProvisioningModelRenderer}
 */
public class ProvisioningModelRendererTest {

	private static final ProvisioningModel TEST_PROVISIONING_MODEL = new ProvisioningModel(new ProvisioningModelBuilder()
			.runMode("modeA")
					.bundles()
							.startLevel("0",
									"org.bndly.common:org.bndly.common.osgi:jar:2.0.30"
							)
							.startLevel("1",
									"org.bndly.common:org.bndly.common.osgi:jar:2.0.30",
									"org.bndly.common:org.bndly.common.osgi:jar:2.0.30"
							)
							.done()
					.configs()
							.entry("org.bndly.common.SomeService")
									.string("value")
									.number(BigDecimal.valueOf(47.11))
									.bool(true)
									.nullValue()
									.array()
									.done()
							.done()
					.resources()
						.entry("some/folder/in/the/project/sources",
								"some/folder/from/jar/root",
								Arrays.asList("**/*.xml"),
								Arrays.asList("**/*.class"))
						.entry("some/folder/from/jar/root",
								"org.bndly.common:org.bndly.common.admin:zip:2.0.30")
						.done()
					.done()
			.runMode("modeA.modeAA")
					.bundles()
							.startLevel("2","org.bndly.common:org.bndly.common.osgi:jar:2.0.30")
							.done()
					.done()
			.build());

	private static final String EXPECTED_RENDER_OUTPUT = "{" +
			"\"modeA\":{" +
				"\"bundles\":{" +
					"\"0\":[" +
						"\"org.bndly.common:org.bndly.common.osgi:jar:2.0.30\"" +
					"]," +
					"\"1\":[" +
						"\"org.bndly.common:org.bndly.common.osgi:jar:2.0.30\"," +
						"\"org.bndly.common:org.bndly.common.osgi:jar:2.0.30\"" +
					"]" +
				"}," +
				"\"configs\":{" +
					"\"org.bndly.common.SomeService\":{" +
						"\"string\":\"value\"," +
						"\"number\":47.11," +
						"\"boolean\":true," +
						"\"null\":null," +
						"\"array\":[]" +
					"}" +
				"}," +
				"\"resources\":["+
					"{" +
						"\"source\":\"some/folder/in/the/project/sources\"," +
						"\"target\":\"some/folder/from/jar/root\"," +
						"\"includes\":[" +
							"\"**/*.xml\"" +
						"]," +
						"\"excludes\":[" +
							"\"**/*.class\"" +
						"]" +
					"}," +
					"{" +
						"\"target\":\"some/folder/from/jar/root\"," +
						"\"artifacts\":[" +
							"\"org.bndly.common:org.bndly.common.admin:zip:2.0.30\"" +
						"]" +
					"}" +
				"]" +
			"}," +
			"\"modeA.modeAA\":{" +
				"\"bundles\":{" +
					"\"2\":[" +
						"\"org.bndly.common:org.bndly.common.osgi:jar:2.0.30\"" +
					"]" +
				"}" +
			"}" +
		"}";

	@Test
	public void render_knownInput_renderOutputMatchesExpectedString() throws Exception {
		final String renderResult = new ProvisioningModelRenderer().render(TEST_PROVISIONING_MODEL);
		Assert.assertEquals(EXPECTED_RENDER_OUTPUT, renderResult);
	}

	@Test
	public void render_parseAndThenRenderAgain_renderOutputMatchesParserInput() throws Exception {
		final String parserInput = EXPECTED_RENDER_OUTPUT;
		final ProvisioningModel parsedModel = new ProvisioningModelParser().parse(new ByteArrayInputStream(parserInput.getBytes()), null);
		final String renderOutput = new ProvisioningModelRenderer().render(parsedModel);
		Assert.assertEquals(parserInput, renderOutput);
	}
}
