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
import java.util.Set;

import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.springaicommunity.typesafe.TypeSafeClient;
import org.springaicommunity.typesafe.TypeSafeModels;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Defining a {@code TypeSafeClient} of your own switches the extension off entirely, the way
 * {@code @ConditionalOnMissingBean} does under Boot.
 *
 * <p>
 * The key is configured here, so the default producer could build a client perfectly well —
 * which is the point: what is being asserted is that it stepped aside, not that it failed.
 */
@QuarkusTest
@TestProfile(TypeSafeClientBacksOffTest.CustomClientProfile.class)
class TypeSafeClientBacksOffTest {

	@Inject
	TypeSafeClient client;

	@Test
	void aClientTheApplicationDefinesItselfWins() {
		assertThat(this.client).isInstanceOf(CustomTypeSafeClient.class);
		assertThat(this.client.defaultModel()).isEqualTo(TypeSafeModels.JEV_PREVIEW);
	}

	/**
	 * The configured application, with {@link CustomTypeSafeClient} selected as an enabled
	 * alternative.
	 */
	public static class CustomClientProfile implements QuarkusTestProfile {

		@Override
		public Map<String, String> getConfigOverrides() {
			return Map.of("quarkus.typesafe.api-key", "test-api-key", "quarkus.typesafe.base-url",
					MockTypeSafeServer.instance().baseUrl());
		}

		@Override
		public Set<Class<?>> getEnabledAlternatives() {
			return Set.of(CustomTypeSafeClient.class);
		}

	}

}
