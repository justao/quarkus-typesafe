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
import org.quarkusverse.typesafe.it.service.Department;
import org.quarkusverse.typesafe.it.service.Frustration;
import org.quarkusverse.typesafe.it.service.PinnedService;
import org.quarkusverse.typesafe.it.service.TriageReport;
import org.quarkusverse.typesafe.it.service.TriageService;
import org.springaicommunity.typesafe.question.Noul;
import org.springaicommunity.typesafe.response.NoulAnswer;
import org.springaicommunity.typesafe.response.SystemOneResponse;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.quarkusverse.typesafe.it.MockTypeSafeServer.body;
import static org.quarkusverse.typesafe.it.MockTypeSafeServer.choice;
import static org.quarkusverse.typesafe.it.MockTypeSafeServer.noul;
import static org.quarkusverse.typesafe.it.MockTypeSafeServer.score;

/**
 * The declarative layer end to end: an interface with no implementation is injected, and
 * each method's call comes out as the question its annotation described and the value its
 * return type promised.
 *
 * <p>
 * Every test asserts against the request the mock recorded as well as against the value, so
 * a conversion that passes by accident — an answer name nothing asked for — cannot.
 */
@QuarkusTest
@TestProfile(MockJevProfile.class)
class TypeSafeServiceTest {

	@Inject
	TriageService triage;

	@Inject
	PinnedService pinned;

	@BeforeEach
	void forgetEarlierRequests() {
		mock().reset();
	}

	@Test
	void answersABooleanFromANoul() {
		mock().respondNext(body("urgent", noul(0.9d)));

		assertThat(this.triage.urgent("Help! My payouts have been failing.")).isTrue();

		assertThat(mock().requestCount()).isEqualTo(1);
		assertThat(mock().lastRequestBody()).contains("\"type\":\"noul\"")
			.contains("Does this convey urgency?")
			.contains("\"urgent\"")
			.contains("\"model\":\"jev-latest\"");
	}

	@Test
	void appliesTheNoulThreshold() {
		mock().respondNext(body("refund", noul(0.7d)));
		assertThat(this.triage.refundRequested("I want my money back")).isFalse();

		mock().respondNext(body("refund", noul(0.85d)));
		assertThat(this.triage.refundRequested("I want my money back")).isTrue();

		assertThat(mock().requestBodies().get(0)).contains("\"refund\"");
	}

	@Test
	void answersADoubleFromANoul() {
		mock().respondNext(body("sentenceProbability", noul(0.25d)));

		assertThat(this.triage.sentenceProbability("A short sentence.")).isEqualTo(0.25d);
	}

	@Test
	void answersAChoiceAsALabel() {
		mock().respondNext(body("department", choice("billing")));

		assertThat(this.triage.department("Double charged")).isEqualTo("billing");

		assertThat(mock().lastRequestBody()).contains("\"type\":\"choice\"")
			.contains("Payments, invoicing, refunds")
			.contains("Bugs, outages, integrations");
	}

	@Test
	void answersAChoiceAsAnEnumConstant() {
		mock().respondNext(body("team", choice("TECHNICAL")));

		assertThat(this.triage.departmentAsEnum("Double charged")).isEqualTo(Department.TECHNICAL);
	}

	@Test
	void answersAScoreAsALevelIndex() {
		mock().respondNext(body("frustration", score(1.6d, 2)));

		assertThat(this.triage.frustration("Three days of failures")).isEqualTo(2);

		assertThat(mock().lastRequestBody()).contains("\"type\":\"score\"")
			.contains("\"Calm\"")
			.contains("\"Frustrated\"")
			.contains("\"Very angry\"");
	}

	@Test
	void answersAScoreAsAnEnumConstant() {
		mock().respondNext(body("frustrationLevel", score(1.6d, 2)));

		assertThat(this.triage.frustrationAsEnum("Three days of failures")).isEqualTo(Frustration.VERY_ANGRY);
	}

	@Test
	void answersAScoreAsTheNearestLevelsLabel() {
		mock().respondNext(body("frustrationLabel", score(1.6d, 2)));

		assertThat(this.triage.frustrationLabel("Three days of failures")).isEqualTo("Very angry");
	}

	@Test
	void returnsTheAnswerItselfWhenTheMethodAsksForIt() {
		mock().respondNext(body("urgencyAnswer", noul(0.92d)));

		NoulAnswer answer = this.triage.urgencyAnswer("Help!");

		assertThat(answer.value()).isEqualTo(0.92d);
		assertThat(answer.isTrue()).isTrue();
	}

	@Test
	void answersSeveralQuestionsInOneCall() {
		mock().respondNext(body(Map.of("urgent", noul(0.9d), "team", choice("billing"))));

		SystemOneResponse response = this.triage.everything("Help! My payouts have been failing.");

		assertThat(response.noulValue("urgent")).isEqualTo(0.9d);
		assertThat(response.choiceValue("team")).isEqualTo("billing");
		assertThat(response.requestId()).isEqualTo(MockTypeSafeServer.REQUEST_ID);
		assertThat(mock().requestCount()).isEqualTo(1);
		assertThat(mock().lastRequestBody()).contains("\"urgent\"").contains("\"team\"");
	}

	@Test
	void fillsARecordFromOneCall() {
		mock().respondNext(
				body(Map.of("urgent", noul(0.9d), "team", choice("billing"), "level", score(1.6d, 2))));

		TriageReport report = this.triage.report("Help! My payouts have been failing.");

		assertThat(report.urgent()).isTrue();
		assertThat(report.team()).isEqualTo("billing");
		assertThat(report.level()).isEqualTo(2);

		// The point of the shape: three questions, one state, one round trip.
		assertThat(mock().requestCount()).isEqualTo(1);
		assertThat(mock().lastRequestBody()).contains("\"urgent\"").contains("\"team\"").contains("\"level\"");
	}

	@Test
	void mergesQuestionsPassedAtRuntime() {
		mock().respondNext(body(Map.of("urgent", noul(0.9d), "sentiment", noul(0.4d))));

		SystemOneResponse response = this.triage.withExtraQuestions("Help!",
				Map.of("sentiment", Noul.of("Is this negative?")));

		assertThat(response.noulValue("sentiment")).isEqualTo(0.4d);
		assertThat(mock().lastRequestBody()).contains("\"sentiment\"").contains("Is this negative?");
	}

	@Test
	void treatsAbsentRuntimeQuestionsAsNone() {
		mock().respondNext(body("urgent", noul(0.9d)));

		assertThat(this.triage.withExtraQuestions("Help!", null).noulValue("urgent")).isEqualTo(0.9d);
	}

	@Test
	void appliesTheModelAMethodPins() {
		mock().respondNext(body("pinnedModel", noul(0.7d)));

		this.triage.pinnedModel("Anything");

		assertThat(mock().lastRequestBody()).contains("\"model\":\"jev-1.13.0\"");
	}

	@Test
	void appliesTheModelTheInterfacePins() {
		mock().respondNext(body("sentence", noul(0.7d)));

		this.pinned.sentence("Anything");

		assertThat(mock().lastRequestBody()).contains("\"model\":\"jev-preview\"");
	}

	@Test
	void sendsSeveralParametersAsANamedState() {
		mock().respondNext(body("twoPartState", noul(0.9d)));

		this.triage.twoPartState("customer", "Help!");

		assertThat(mock().lastRequestBody()).contains("\"state\":{\"sender\":\"customer\",\"message\":\"Help!\"}");
	}

	@Test
	void doesNotCallTheApiForADefaultMethod() {
		assertThat(this.triage.describe()).isEqualTo("triage");
		assertThat(mock().requestCount()).isZero();
	}

	@Test
	void answersTheObjectMethodsItself() {
		assertThat(this.triage.toString()).contains("TriageService service proxy");
		assertThat(this.triage.equals(this.triage)).isTrue();
	}

	private static MockTypeSafeServer mock() {
		return MockTypeSafeServer.instance();
	}

}
