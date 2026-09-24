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
import java.util.Set;

import org.springaicommunity.typesafe.RetryPolicy;

/**
 * Turns the retry group of {@link TypeSafeConfig} into the {@link RetryPolicy} the client
 * applies.
 *
 * <p>
 * This is the Quarkus counterpart of the reference starter's
 * {@code TypeSafeProperties.toRetryPolicy()}. It lives outside the configuration mapping
 * because SmallRye treats every method of a mapping interface as a property, so a
 * {@code RetryPolicy}-returning method on {@link TypeSafeConfig} would be read as a
 * property of its own.
 */
public final class TypeSafeRetryPolicies {

	private TypeSafeRetryPolicies() {
	}

	/**
	 * @param retry the configured retry group
	 * @return the equivalent policy, whose defaults match {@link RetryPolicy#defaults()} when
	 * nothing was configured
	 */
	public static RetryPolicy toRetryPolicy(TypeSafeConfig.Retry retry) {
		Duration budget = retry.totalTimeout();
		return RetryPolicy.builder()
			.maxRetries(retry.maxRetries())
			.initialBackoff(retry.initialBackoff())
			.maxBackoff(retry.maxBackoff())
			.jitter(retry.jitter())
			.retryableStatuses(retryableStatuses(retry))
			.respectRetryAfter(retry.respectRetryAfter())
			.retryConnectionErrors(retry.retryConnectionErrors())
			// A zero budget is the property form of the SDK's `null` total timeout: a real
			// zero would fail every call on the budget check alone.
			.totalTimeout(isNoBudget(budget) ? null : budget)
			.build();
	}

	/**
	 * An unset {@code statuses} keeps the SDK's own default rather than replacing it with
	 * an empty set, which would silently stop retrying 408 and 429.
	 */
	private static Set<Integer> retryableStatuses(TypeSafeConfig.Retry retry) {
		return retry.statuses().isEmpty() ? RetryPolicy.DEFAULT_RETRYABLE_STATUSES : Set.copyOf(retry.statuses());
	}

	/**
	 * @param budget the configured budget
	 * @return {@code true} when the budget was configured as zero or negative, which the
	 * reference SDK expresses as no budget at all
	 */
	static boolean isNoBudget(Duration budget) {
		return budget.isZero() || budget.isNegative();
	}

}
