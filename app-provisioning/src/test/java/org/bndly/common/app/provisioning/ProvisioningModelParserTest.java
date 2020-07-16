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

import org.bndly.common.app.provisioning.model.ArtifactDefinition;
import org.bndly.common.app.provisioning.model.Config;
import org.bndly.common.app.provisioning.model.ProvisioningModel;
import org.bndly.common.app.provisioning.model.ResourceDefinition;
import org.bndly.common.app.provisioning.model.RunMode;
import org.bndly.common.app.provisioning.model.StartLevelBundle;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class ProvisioningModelParserTest {

	@Test
	public void testParsing() throws IOException, InterpolationException {
		try(Reader r = Files.newBufferedReader(Paths.get("src","test","resources","provisioning.json"), Charset.forName("UTF-8"))) {
			ProvisioningModel model = new ProvisioningModelParser().parse(r, null);
			Assert.assertNotNull(model);
			List<RunMode> modes = model.getRunModes();
			Assert.assertNotNull(modes);
			Assert.assertEquals(2, modes.size());
			RunMode mode0 = modes.get(0);
			Assert.assertEquals("modeA", mode0.getName());
			List<StartLevelBundle> bundles = mode0.getBundles();
			Assert.assertNotNull(bundles);
			Assert.assertEquals(2, bundles.size());
			StartLevelBundle bsl0 = bundles.get(0);
			Assert.assertEquals(0, bsl0.getStartLevel());
			List<ArtifactDefinition> bsl0artifacts = bsl0.getArtifacts();
			Assert.assertNotNull(bsl0artifacts);
			Assert.assertEquals(1, bsl0artifacts.size());
			Assert.assertEquals("org.bndly.common:org.bndly.common.osgi:jar:2.0.30", bsl0artifacts.get(0).toString());
			
			
			StartLevelBundle bsl1 = bundles.get(1);
			Assert.assertEquals(1, bsl1.getStartLevel());
			List<ArtifactDefinition> bsl1artifacts = bsl1.getArtifacts();
			Assert.assertNotNull(bsl1artifacts);
			Assert.assertEquals(2, bsl1artifacts.size());
			Assert.assertEquals("org.bndly.common:org.bndly.common.osgi:jar:2.0.30", bsl1artifacts.get(0).toString());
			Assert.assertEquals("org.bndly.common:org.bndly.common.osgi:jar:2.0.30", bsl1artifacts.get(1).toString());
			
			List<Config> configs = mode0.getConfigs();
			Assert.assertNotNull(configs);
			Assert.assertEquals(1, configs.size());
			Config config = configs.get(0);
			Assert.assertEquals("org.bndly.common.SomeService", config.getName());
			Map<String, Object> configProps = config.getProperties();
			Assert.assertNotNull(configProps);
			Assert.assertEquals("value", configProps.get("string"));
			Assert.assertEquals(new BigDecimal("47.11"), configProps.get("number"));
			Assert.assertTrue(configProps.containsKey("null"));
			Assert.assertNull(configProps.get("null"));
			Assert.assertEquals(true, configProps.get("boolean"));
			Object arr = configProps.get("array");
			Assert.assertNotNull(arr);
			Assert.assertTrue(arr.getClass().isArray());
			
			List<ResourceDefinition> resources = mode0.getResources();
			Assert.assertNotNull(resources);
			Assert.assertEquals(2, resources.size());
			ResourceDefinition res = resources.get(0);
			Assert.assertEquals("some/folder/in/the/project/sources", res.getSource());
			Assert.assertEquals("some/folder/from/jar/root", res.getTarget());
			List<String> includes = res.getIncludes();
			Assert.assertNotNull(includes);
			Assert.assertEquals(1, includes.size());
			Assert.assertEquals("**/*.xml", includes.get(0));
			List<String> excludes = res.getExcludes();
			Assert.assertNotNull(excludes);
			Assert.assertEquals(1, excludes.size());
			Assert.assertEquals("**/*.class", excludes.get(0));
			
			ResourceDefinition res2 = resources.get(1);
			Assert.assertEquals(0, res2.getIncludes().size());
			Assert.assertEquals(0, res2.getExcludes().size());
			List<ArtifactDefinition> resourceArtifacts = res2.getArtifacts();
			Assert.assertNotNull(resourceArtifacts);
			Assert.assertEquals(1, resourceArtifacts.size());
			Assert.assertEquals("org.bndly.common:org.bndly.common.admin:zip:2.0.30", resourceArtifacts.get(0).toString());
			
			RunMode mode1 = modes.get(1);
			Assert.assertEquals("modeA.modeAA", mode1.getName());
			Assert.assertTrue(mode1.getConfigs().isEmpty());
			Assert.assertTrue(mode1.getResources().isEmpty());
			bundles = mode1.getBundles();
			Assert.assertNotNull(bundles);
			Assert.assertEquals(1, bundles.size());
			StartLevelBundle bsl = bundles.get(0);
			Assert.assertEquals(2, bsl.getStartLevel());
			List<ArtifactDefinition> artifacts = bsl.getArtifacts();
			Assert.assertNotNull(artifacts);
			Assert.assertEquals(1, artifacts.size());
			Assert.assertEquals("org.bndly.common:org.bndly.common.osgi:jar:2.0.30", artifacts.get(0).toString());
			
		}
	}
}
