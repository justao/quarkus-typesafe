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

import java.time.Duration;
import java.util.Map;

import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.springaicommunity.typesafe.RetryPolicy;
import org.springaicommunity.typesafe.TypeSafeClient;
import org.springaicommunity.typesafe.TypeSafeConstants;
import org.springaicommunity.typesafe.TypeSafeModels;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Nothing but a key: the client has to come out configured the way the reference SDK and the
 * reference starter say it should, not merely configured.
 */
@QuarkusTest
@TestProfile(TypeSafeClientDefaultsTest.KeyOnlyProfile.class)
class TypeSafeClientDefaultsTest {

	@Inject
	TypeSafeClient client;

	@Test
	void appliesTheDefaultRetryPolicyWhenNothingIsConfigured() {
		// Exactly the SDK's own defaults, not a policy that happens to look like them: this
		// is what the reference starter asserts, and it fails loudly if the extension ever
		// starts inventing its own backoff.
		assertThat(this.client.retryPolicy()).isEqualTo(RetryPolicy.defaults());
	}

	@Test
	void appliesTheDefaultModelAndTimeout() {
		assertThat(this.client.defaultModel()).isEqualTo(TypeSafeModels.JEV_LATEST);
		assertThat(this.client.timeout()).isEqualTo(TypeSafeConstants.DEFAULT_TIMEOUT);
		assertThat(this.client.timeout()).isEqualTo(Duration.ofSeconds(10));
	}

	/**
	 * A key and nothing else. The base URL still points at the mock server so that nothing
	 * in this class can reach the real API by accident.
	 */
	public static class KeyOnlyProfile implements QuarkusTestProfile {

		@Override
		public Map<String, String> getConfigOverrides() {
			return Map.of("quarkus.typesafe.api-key", "test-api-key", "quarkus.typesafe.base-url",
					MockTypeSafeServer.instance().baseUrl());
		}

	}

}
