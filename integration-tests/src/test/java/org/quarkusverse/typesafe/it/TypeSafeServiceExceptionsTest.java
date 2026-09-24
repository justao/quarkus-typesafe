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

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quarkusverse.typesafe.it.service.TriageService;
import org.quarkusverse.typesafe.service.TypeSafeServiceCoercionException;
import org.quarkusverse.typesafe.service.TypeSafeServiceException;
import org.springaicommunity.typesafe.exception.TypeSafeAnswerTypeException;
import org.springaicommunity.typesafe.exception.TypeSafeRateLimitException;
import org.springaicommunity.typesafe.question.Noul;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.quarkusverse.typesafe.it.MockTypeSafeServer.body;
import static org.quarkusverse.typesafe.it.MockTypeSafeServer.choice;
import static org.quarkusverse.typesafe.it.MockTypeSafeServer.noul;
import static org.quarkusverse.typesafe.it.MockTypeSafeServer.score;

/**
 * What happens when the answer is not what the method declared.
 *
 * <p>
 * Two kinds of failure meet here, and the difference matters to a caller. The SDK's own
 * exceptions — an answer of an unexpected kind, a rate limit — travel through the
 * declarative layer untouched, so the typed contract the SDK documents is still the one to
 * catch. The layer's own exception is for what the build could not know: whether the label
 * the model picked happens to be one an enum declares, whether the state a caller passed is
 * null, whether two questions ended up sharing a name.
 */
@QuarkusTest
@TestProfile(MockJevProfile.class)
class TypeSafeServiceExceptionsTest {

	@Inject
	TriageService triage;

	@BeforeEach
	void forgetEarlierRequests() {
		mock().reset();
	}

	@Test
	void propagatesTheSdkExceptionWhenTheAnswerIsOfTheWrongKind() {
		mock().respondNext(body("urgent", choice("billing")));

		assertThatExceptionOfType(TypeSafeAnswerTypeException.class)
			.isThrownBy(() -> this.triage.urgent("Help!"));
	}

	@Test
	void propagatesTheSdkExceptionForAnErrorStatus() {
		mock().respondNext(429, "{\"error\":{\"message\":\"Too many requests\"}}");

		assertThatExceptionOfType(TypeSafeRateLimitException.class)
			.isThrownBy(() -> this.triage.urgent("Help!"));
	}

	@Test
	void failsWhenAChoiceSelectsALabelNoConstantMatches() {
		mock().respondNext(body("team", choice("marketing")));

		assertThatExceptionOfType(TypeSafeServiceCoercionException.class)
			.isThrownBy(() -> this.triage.departmentAsEnum("Help!"))
			.withMessageContaining("not a constant of")
			.withMessageContaining("BILLING")
			.withMessageContaining("marketing");
	}

	@Test
	void failsWhenAScorePointsPastTheEndOfTheEnum() {
		mock().respondNext(body("severity", score(1.6d, 2)));

		assertThatExceptionOfType(TypeSafeServiceCoercionException.class)
			.isThrownBy(() -> this.triage.severity("Help!"))
			.withMessageContaining("level 2")
			.withMessageContaining("outside");
	}

	@Test
	void refusesANullStateBeforeSpendingACall() {
		assertThatExceptionOfType(TypeSafeServiceException.class).isThrownBy(() -> this.triage.urgent(null))
			.withMessageContaining("is null");

		assertThat(mock().requestCount()).isZero();
	}

	@Test
	void refusesRuntimeQuestionsThatCollideWithDeclaredOnes() {
		assertThatExceptionOfType(TypeSafeServiceException.class)
			.isThrownBy(() -> this.triage.withExtraQuestions("Help!", Map.of("urgent", Noul.of("Is it urgent?"))))
			.withMessageContaining("declared both");

		assertThat(mock().requestCount()).isZero();
	}

	@Test
	void reportsAnUnparseableResponseAsAnSdkFailure() {
		mock().respondNext("this is not JSON");

		assertThatExceptionOfType(org.springaicommunity.typesafe.exception.TypeSafeException.class)
			.isThrownBy(() -> this.triage.urgent("Help!"));
	}

	private static MockTypeSafeServer mock() {
		return MockTypeSafeServer.instance();
	}

}
