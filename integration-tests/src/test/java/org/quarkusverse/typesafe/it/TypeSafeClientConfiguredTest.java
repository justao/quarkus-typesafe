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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quarkusverse.typesafe.TypeSafeConfig;
import org.quarkusverse.typesafe.TypeSafeEndpoints;
import org.springaicommunity.typesafe.RetryPolicy;
import org.springaicommunity.typesafe.TypeSafeClient;
import org.springaicommunity.typesafe.TypeSafeModels;
import org.springaicommunity.typesafe.api.TypeSafeApi;
import org.springaicommunity.typesafe.question.Noul;
import org.springaicommunity.typesafe.response.ModelMetadata;
import org.springaicommunity.typesafe.response.SystemOneResponse;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The counterpart of the reference starter's {@code TypeSafeAutoConfigurationTests}: every
 * property binds, the bean carries what the properties say, and the bean it built actually
 * reaches the wire — the last part being the one a context-only assertion cannot make.
 *
 * <p>
 * The API root points at {@link MockTypeSafeServer}, so "reaches the wire" is checkable
 * without a key for the real API.
 */
@QuarkusTest
@TestProfile(TypeSafeClientConfiguredTest.ConfiguredProfile.class)
class TypeSafeClientConfiguredTest {

	@Inject
	TypeSafeClient client;

	@Inject
	TypeSafeConfig config;

	@Inject
	TypeSafeEndpoints endpoints;

	@BeforeEach
	void forgetEarlierRequests() {
		MockTypeSafeServer.instance().reset();
	}

	@Test
	void bindsEveryProperty() {
		assertThat(this.config.baseUrl()).isEqualTo(MockTypeSafeServer.instance().baseUrl());
		assertThat(this.config.model()).isEqualTo(TypeSafeModels.JEV_1_13_0);
		assertThat(this.config.timeout()).isEqualTo(Duration.ofSeconds(42));

		assertThat(this.config.retry().statuses()).containsExactly(408, 429, 409);

		RetryPolicy policy = this.client.retryPolicy();
		assertThat(policy.maxRetries()).isEqualTo(7);
		assertThat(policy.initialBackoff()).isEqualTo(Duration.ofMillis(250));
		assertThat(policy.maxBackoff()).isEqualTo(Duration.ofSeconds(9));
		assertThat(policy.jitter()).isEqualTo(0.5d);
		assertThat(policy.retryableStatuses()).containsExactlyInAnyOrder(408, 429, 409);
		assertThat(policy.respectRetryAfter()).isFalse();
		assertThat(policy.retryConnectionErrors()).isFalse();
		assertThat(policy.totalTimeout()).isEqualTo(Duration.ofSeconds(90));

		assertThat(this.client.defaultModel()).isEqualTo(TypeSafeModels.JEV_1_13_0);
	}

	@Test
	void theConfiguredTimeoutReachesTheClientAndNotOnlyTheTransport() {
		// RetryPolicy.totalTimeout is checked against this value when deciding whether
		// another attempt fits, so a client left on the 10s default would let a call with a
		// 42s read timeout overrun its own declared budget.
		assertThat(this.client.timeout()).isEqualTo(Duration.ofSeconds(42));
	}

	@Test
	void exposesTheEndpointPathsInUse() {
		assertThat(this.endpoints.systemOnePath()).isEqualTo(TypeSafeApi.DEFAULT_SYSTEM_ONE_PATH);
		assertThat(this.endpoints.modelsPath()).isEqualTo(TypeSafeApi.DEFAULT_MODELS_PATH);
	}

	@Test
	void answersQuestionsThroughTheInjectedClient() {
		SystemOneResponse response = this.client.systemOne("A short sentence.",
				Map.of("probe", Noul.of("Is this a sentence?")));

		assertThat(response.noulValue("probe")).isEqualTo(0.5d);
		assertThat(response.requestId()).isEqualTo(MockTypeSafeServer.REQUEST_ID);

		MockTypeSafeServer mock = MockTypeSafeServer.instance();
		assertThat(mock.authorizationHeaders()).contains("Bearer test-api-key");
		assertThat(mock.lastRequestBody()).contains("\"model\":\"" + TypeSafeModels.JEV_1_13_0 + "\"")
			.contains("\"probe\"");
	}

	@Test
	void listsModelsThroughTheInjectedClient() {
		assertThat(this.client.listModels()).extracting(ModelMetadata::name)
			.containsExactly(TypeSafeModels.JEV_LATEST, TypeSafeModels.JEV_PREVIEW);
	}

	/**
	 * The configured application: a key, a base URL pointing at the mock server, and every
	 * retry key at a value no default would produce.
	 */
	public static class ConfiguredProfile implements QuarkusTestProfile {

		@Override
		public Map<String, String> getConfigOverrides() {
			return Map.ofEntries(Map.entry("quarkus.typesafe.api-key", "test-api-key"),
					Map.entry("quarkus.typesafe.base-url", MockTypeSafeServer.instance().baseUrl()),
					Map.entry("quarkus.typesafe.model", TypeSafeModels.JEV_1_13_0),
					Map.entry("quarkus.typesafe.timeout", "42s"),
					Map.entry("quarkus.typesafe.retry.max-retries", "7"),
					Map.entry("quarkus.typesafe.retry.initial-backoff", "250ms"),
					Map.entry("quarkus.typesafe.retry.max-backoff", "9s"),
					Map.entry("quarkus.typesafe.retry.jitter", "0.5"),
					Map.entry("quarkus.typesafe.retry.statuses", "408,429,409"),
					Map.entry("quarkus.typesafe.retry.respect-retry-after", "false"),
					Map.entry("quarkus.typesafe.retry.retry-connection-errors", "false"),
					Map.entry("quarkus.typesafe.retry.total-timeout", "90s"));
		}

	}

}
