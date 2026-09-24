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

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import org.springaicommunity.typesafe.TypeSafeConstants;

/**
 * Configuration for the TypeSafe AI System One (Jev) client, under the
 * {@value #CONFIG_PREFIX} prefix.
 *
 * <pre>{@code
 * quarkus.typesafe.api-key=${TYPESAFE_API_KEY}
 * quarkus.typesafe.model=jev-latest
 * quarkus.typesafe.timeout=10s
 * quarkus.typesafe.retry.max-retries=2
 * }</pre>
 *
 * <p>
 * This is the Quarkus face of the reference
 * {@code spring-ai-starter-typesafe}'s {@code TypeSafeProperties}: the same keys, one
 * prefix deeper, bound by SmallRye Config instead of Boot's binder. Durations accept both
 * the simplified form ({@code 500ms}, {@code 10s}) and ISO-8601 ({@code PT10S}); enum-like
 * and collection keys are comma separated.
 *
 * <p>
 * The mapping is a run-time configuration root: a key supplied when the application starts
 * (an environment variable, a system property, a mounted secret) is read at run time and
 * does not have to be known when the application is built.
 */
@ConfigMapping(prefix = TypeSafeConfig.CONFIG_PREFIX)
public interface TypeSafeConfig {

	/** Prefix of every property this extension reads. */
	String CONFIG_PREFIX = "quarkus.typesafe";

	/**
	 * TypeSafe API key.
	 *
	 * <p>
	 * Nothing is created without it, and an application that never injects a
	 * {@code TypeSafeClient} starts normally. An application that does inject one without
	 * having configured a key fails at that injection point, naming this property.
	 * @return the configured key, or empty when none was configured
	 */
	Optional<String> apiKey();

	/**
	 * API root of the TypeSafe System One service.
	 * @return the base URL every request is sent to
	 */
	@WithDefault(TypeSafeConstants.DEFAULT_BASE_URL)
	String baseUrl();

	/**
	 * Model name or alias applied to requests that do not name one.
	 * @return the default model
	 */
	@WithDefault(TypeSafeConstants.DEFAULT_MODEL)
	String model();

	/**
	 * Timeout of each HTTP operation.
	 *
	 * <p>
	 * It is applied to the transport and declared to the client, which counts it against
	 * {@link Retry#totalTimeout()} before starting another attempt.
	 * @return the per attempt timeout; 10s unless configured, mirroring
	 * {@link TypeSafeConstants#DEFAULT_TIMEOUT}
	 */
	@WithDefault("10s")
	Duration timeout();

	/**
	 * How transient failures are retried.
	 * @return the retry settings
	 */
	Retry retry();

	/**
	 * Retry settings, mirroring the reference SDK's {@code RetryPolicy} defaults: two
	 * retries after the initial attempt, exponential backoff from 500ms up to 5s with 25%
	 * jitter subtracted, and a 30 second budget for the whole call including waits.
	 */
	interface Retry {

		/**
		 * Retries after the initial attempt. Zero disables retrying.
		 * @return the number of retries
		 */
		@WithDefault("2")
		int maxRetries();

		/**
		 * First backoff delay, doubled on each subsequent attempt.
		 * @return the initial delay
		 */
		@WithDefault("500ms")
		Duration initialBackoff();

		/**
		 * Upper bound on a single backoff delay.
		 * @return the largest delay
		 */
		@WithDefault("5s")
		Duration maxBackoff();

		/**
		 * Fraction of each delay randomly subtracted, between 0 and 1.
		 * @return the jitter fraction
		 */
		@WithDefault("0.25")
		double jitter();

		/**
		 * Status codes retried on top of every 5xx, which is always retried.
		 *
		 * <p>
		 * Unlike the reference starter this has no annotation default: an empty or absent
		 * value falls back to {@code 408,429}, the SDK's own default, which keeps
		 * {@code quarkus.typesafe.retry.statuses} from being the one key whose default a
		 * reader has to look up twice.
		 * @return the configured statuses, empty when none were configured
		 */
		List<Integer> statuses();

		/**
		 * Whether a server stated wait overrides the computed backoff.
		 * @return {@code true} to honour {@code retry-after-ms}
		 */
		@WithDefault("true")
		boolean respectRetryAfter();

		/**
		 * Whether failures without an HTTP response are retried.
		 * @return {@code true} to retry connection errors
		 */
		@WithDefault("true")
		boolean retryConnectionErrors();

		/**
		 * Budget for a whole call including waits.
		 *
		 * <p>
		 * Set it to {@code 0s} to remove the budget entirely, which is what the reference
		 * SDK expresses as a null total timeout.
		 * @return the budget; 30s unless configured
		 */
		@WithDefault("30s")
		Duration totalTimeout();

	}

}
