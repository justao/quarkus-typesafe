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
import org.springaicommunity.typesafe.TypeSafeClient;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.inject.Instance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * The promise the reference starter makes — an application with no API key starts normally —
 * expressed the way Quarkus can keep it.
 *
 * <p>
 * Boot keeps it by not creating the bean at all; here the bean exists but is only built when
 * something asks for it, so an application that never injects a client never touches the
 * key. This class is that application: it configures nothing (see
 * {@code src/main/resources/application.properties}) and boots.
 */
@QuarkusTest
class TypeSafeClientMissingApiKeyTest {

	@Inject
	Instance<TypeSafeClient> client;

	@Test
	void theApplicationStartsWithoutAnApiKey() {
		// Booting this test at all is most of the assertion: an application with no key and
		// no client injection point starts. The bean is still there to be asked for, which
		// is why the failure below is a configuration error and not an unsatisfied
		// dependency.
		assertThat(this.client.isResolvable()).isTrue();
	}

	@Test
	void askingForTheClientWithoutAKeyNamesThePropertyToSet() {
		Throwable failure = catchThrowable(this.client::get);

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
