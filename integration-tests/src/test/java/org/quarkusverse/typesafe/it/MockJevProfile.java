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

import io.quarkus.test.junit.QuarkusTestProfile;

/**
 * A key and an API root pointing at {@link MockTypeSafeServer}, with retrying turned off.
 *
 * <p>
 * Retrying is off because every test scripts one response per call: a retry would consume
 * the answer scripted for the next call, and an error a test deliberately scripted would
 * succeed on the second attempt.
 */
public class MockJevProfile implements QuarkusTestProfile {

	@Override
	public Map<String, String> getConfigOverrides() {
		return Map.of("quarkus.typesafe.api-key", "test-api-key", "quarkus.typesafe.base-url",
				MockTypeSafeServer.instance().baseUrl(), "quarkus.typesafe.retry.max-retries", "0");
	}

}
