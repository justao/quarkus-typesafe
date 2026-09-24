/*
 * Copyright 2026 - 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.quarkusverse.typesafe.it;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The extension's contract with Quarkus is one line in a properties file, and a properties
 * file does not fail to compile.
 *
 * <p>
 * Every other test in this module proves the wiring works, but they would mostly still pass
 * if the runtime artifact stopped naming a deployment module: the beans are found through
 * {@code beans.xml} either way, and only the build steps in that module — the reflection
 * registration a native image needs — would quietly stop running. This is the guard the
 * reference starter's {@code AutoConfigurationImportsTests} puts on its imports file.
 */
class ExtensionDescriptorTest {

	private static final String DESCRIPTOR = "META-INF/quarkus-extension.properties";

	@Test
	void theRuntimeArtifactNamesThisProjectsDeploymentModule() throws IOException {
		assertThat(deploymentArtifact())
			.as("Quarkus resolves the deployment module, and with it every build step, from this coordinate")
			.isNotNull()
			.matches("org\\.quarkusverse:quarkus-typesafe-deployment:.+");
	}

	@Test
	void theCoordinateCarriesTheVersionThisBuildProduces() throws IOException {
		// The version is expanded by processResources. An unexpanded placeholder, or an empty
		// version, resolves to nothing at all and the bootstrap then reports a missing
		// artifact rather than the descriptor that named it.
		assertThat(deploymentArtifact()).doesNotContain("${").doesNotEndWith(":");
	}

	private String deploymentArtifact() throws IOException {
		URL resource = getClass().getClassLoader().getResource(DESCRIPTOR);
		assertThat(resource).as("the runtime artifact must ship %s", DESCRIPTOR).isNotNull();

		Properties descriptor = new Properties();
		try (InputStream in = resource.openStream()) {
			descriptor.load(in);
		}
		return descriptor.getProperty("deployment-artifact");
	}

}
