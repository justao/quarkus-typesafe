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

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.quarkusverse.typesafe.it.service.TriageService;

import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * The promise the {@code TypeSafeClient} producer makes holds for the declarative layer too,
 * because the service resolves the client on its first call rather than when it is created:
 * an application with no API key starts, and only a call that needs the client fails.
 *
 * <p>
 * This is the application with no key: it configures nothing (see
 * {@code src/main/resources/application.properties}), and the service interface is still
 * injectable — including its default methods, which never reach the API.
 */
@QuarkusTest
class TypeSafeServiceMissingApiKeyTest {

	@Inject
	TriageService triage;

	@Test
	void theServiceIsInjectableWithoutAnApiKey() {
		assertThat(this.triage).isNotNull();
		assertThat(this.triage.describe()).isEqualTo("triage");
	}

	@Test
	void theFirstCallNamesThePropertyToSet() {
		Throwable failure = catchThrowable(() -> this.triage.urgent("Help!"));

		assertThat(failure).isNotNull();
		assertThat(messagesOf(failure))
			.as("the message has to say which property to set, however ArC wraps it")
			.anySatisfy(message -> assertThat(message).contains("No API key configured")
				.contains("quarkus.typesafe.api-key"));
	}

	private static List<String> messagesOf(Throwable failure) {
		List<String> messages = new ArrayList<>();
		for (Throwable current = failure; current != null; current = current.getCause()) {
			messages.add(String.valueOf(current.getMessage()));
		}
		return messages;
	}

}
