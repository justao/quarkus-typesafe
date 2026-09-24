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

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springaicommunity.typesafe.TypeSafeClient;
import org.springaicommunity.typesafe.TypeSafeConstants;
import org.springaicommunity.typesafe.TypeSafeModels;
import org.springaicommunity.typesafe.question.Noul;
import org.springaicommunity.typesafe.response.ModelMetadata;
import org.springaicommunity.typesafe.response.SystemOneResponse;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The extension against the real Jev API.
 *
 * <p>
 * The other tests prove the properties bind and the bean appears, but every value there is a
 * placeholder and the server is a fixture: a client assembled with the wrong base URL or a
 * mis-threaded API key looks identical in a bean assertion. This boots an application from
 * {@code quarkus.typesafe.*} exactly as an application would and makes real calls with the
 * resulting bean.
 *
 * <p>
 * Opt-in, like the reference starter's {@code TypeSafeAutoConfigurationIT}: it skips unless
 * {@code TYPESAFE_API_KEY} is exported, so the build stays offline and free by default.
 */
@QuarkusTest
@TestProfile(TypeSafeLiveApiIT.LiveApiProfile.class)
@EnabledIfEnvironmentVariable(named = TypeSafeConstants.API_KEY_ENV, matches = ".+",
		disabledReason = "Set TYPESAFE_API_KEY to run the extension against the real Jev API")
class TypeSafeLiveApiIT {

	@Inject
	TypeSafeClient client;

	@Test
	void theInjectedClientTalksToTheRealApi() {
		SystemOneResponse response = this.client.systemOne("A short sentence.",
				Map.of("probe", Noul.of("Is this a sentence?")));

		assertThat(response.noulValue("probe")).isBetween(0.0d, 1.0d);
		assertThat(response.requestId()).isNotBlank();
	}

	@Test
	void theInjectedClientListsModels() {
		assertThat(this.client.listModels()).extracting(ModelMetadata::name).contains(TypeSafeModels.JEV_LATEST);
	}

	@Test
	void honoursTheConfiguredModelOnARealCall() {
		assertThat(this.client.defaultModel()).isEqualTo(TypeSafeModels.JEV_PREVIEW);

		SystemOneResponse response = this.client.systemOne("A short sentence.",
				Map.of("probe", Noul.of("Is this a sentence?")));

		// The server answers with the version it resolved the alias to, so this asserts the
		// property reached the wire, not that the alias comes back.
		assertThat(response.model()).isNotBlank();
		assertThat(response.noulValue("probe")).isBetween(0.0d, 1.0d);
	}

	/**
	 * The key from the environment, a pinned preview model and the real API root.
	 */
	public static class LiveApiProfile implements QuarkusTestProfile {

		@Override
		public Map<String, String> getConfigOverrides() {
			// Not `Map.of`: a missing environment variable would make that throw while the
			// profile is being read, turning a skipped test into a failing one.
			return Map.of("quarkus.typesafe.api-key",
					Optional.ofNullable(System.getenv(TypeSafeConstants.API_KEY_ENV)).orElse(""),
					"quarkus.typesafe.model", TypeSafeModels.JEV_PREVIEW);
		}

	}

}
