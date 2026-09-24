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

package org.quarkusverse.typesafe;

import io.quarkus.arc.DefaultBean;
import io.quarkus.runtime.configuration.ConfigurationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.springaicommunity.typesafe.TypeSafeClient;
import org.springaicommunity.typesafe.api.TypeSafeApi;

/**
 * Produces the {@link TypeSafeClient} and the {@link TypeSafeEndpoints} from
 * {@link TypeSafeConfig}.
 *
 * <p>
 * This is the Quarkus counterpart of the reference starter's
 * {@code TypeSafeAutoConfiguration}. Both producers are {@link DefaultBean}s, so an
 * application that defines a {@code TypeSafeClient} of its own switches this off entirely —
 * the same promise {@code @ConditionalOnMissingBean} makes.
 *
 * <p>
 * The key is read at run time rather than at build time, and a missing key is reported when
 * the client is actually needed:
 *
 * <ul>
 * <li>an application that configures no key and never injects a client starts normally;
 * the default beans are simply never instantiated;</li>
 * <li>an application that injects one without a key fails at that injection point with a
 * message naming the property to set, rather than building a client whose every call would
 * come back 401.</li>
 * </ul>
 */
@ApplicationScoped
public class TypeSafeClientProducer {

	/**
	 * Builds the client from the configuration.
	 * @param config the extension configuration
	 * @return the client described by {@code quarkus.typesafe.*}
	 * @throws ConfigurationException when no API key is configured
	 */
	@Produces
	@Singleton
	@DefaultBean
	public TypeSafeClient typeSafeClient(TypeSafeConfig config) {
		String apiKey = config.apiKey()
			.map(String::trim)
			.filter(key -> !key.isEmpty())
			.orElseThrow(() -> new ConfigurationException(
					"No API key configured. Set " + TypeSafeConfig.CONFIG_PREFIX + ".api-key."));

		return TypeSafeClient.builder()
			.apiKey(apiKey)
			.baseUrl(config.baseUrl())
			.defaultModel(config.model())
			// Declared as well as applied to the transport: the client counts it against
			// RetryPolicy.totalTimeout when deciding whether another attempt fits, so
			// leaving it at the 10s default would let a call overrun its declared budget.
			.timeout(config.timeout())
			.retryPolicy(TypeSafeRetryPolicies.toRetryPolicy(config.retry()))
			.build();
	}

	/**
	 * @return the default paths, exposed so an application can see what the client talks to
	 * without reaching into the SDK
	 */
	@Produces
	@Singleton
	@DefaultBean
	public TypeSafeEndpoints typeSafeEndpoints() {
		return new TypeSafeEndpoints(TypeSafeApi.DEFAULT_SYSTEM_ONE_PATH, TypeSafeApi.DEFAULT_MODELS_PATH);
	}

}
