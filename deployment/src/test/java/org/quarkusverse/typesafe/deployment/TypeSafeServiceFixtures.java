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

package org.quarkusverse.typesafe.deployment;

import java.util.Map;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import org.quarkusverse.typesafe.service.Choice;
import org.quarkusverse.typesafe.service.Noul;
import org.quarkusverse.typesafe.service.Option;
import org.quarkusverse.typesafe.service.Questions;
import org.quarkusverse.typesafe.service.RegisterTypeSafeService;
import org.quarkusverse.typesafe.service.Score;
import org.springaicommunity.typesafe.question.Question;
import org.springaicommunity.typesafe.response.NoulAnswer;
import org.springaicommunity.typesafe.response.SystemOneResponse;

/**
 * Interfaces that are right, and interfaces that are wrong in each of the ways the validator
 * knows about.
 *
 * <p>
 * They live in this module's test sources, and nothing registers them: they exist to be read
 * out of a Jandex index built here, which is what makes the rules testable without starting
 * Quarkus. The compiler is happy with all of them, which is the point — every one of these
 * mistakes is invisible until something checks it.
 */
final class TypeSafeServiceFixtures {

	private TypeSafeServiceFixtures() {
	}

	/** Every shape that is allowed. */
	@RegisterTypeSafeService
	interface Good {

		@Noul("Does this convey urgency?")
		boolean urgent(String message);

		@Choice(value = "Which team?", options = { @Option(name = "billing"), @Option(name = "technical") })
		String team(String message);

		@Score(value = "How frustrated?", levels = { "Calm", "Frustrated", "Very angry" })
		int level(String message);

		@Noul("Is this a sentence?")
		double probability(String message);

		@Noul("Is it urgent?")
		NoulAnswer answer(String message);

		@Noul(value = "Is it urgent?", name = "urgent")
		@Choice(value = "Which team?", name = "team",
				options = { @Option(name = "billing"), @Option(name = "technical") })
		SystemOneResponse everything(String message);

		@Noul("Is it urgent?")
		SystemOneResponse withExtra(String message, @Questions Map<String, ? extends Question> extra);

		GoodReport report(String message);

		default String describe() {
			return "good";
		}

	}

	/** A record whose components each declare their own question. */
	record GoodReport(@Noul("Is it urgent?") boolean urgent,
			@Choice(value = "Which team?", options = { @Option(name = "billing"), @Option(name = "technical") }) String team,
			@Score(value = "How frustrated?", levels = { "Calm", "Frustrated" }) int level,
			@Noul("Is it urgent?") NoulAnswer answer) {
	}

	/** An enum a method returns, which the runtime has to enumerate. */
	enum Shade {

		LIGHT,

		DARK

	}

	/** A choice answered into an enum. */
	@RegisterTypeSafeService
	interface WithEnum {

		@Choice(value = "Which shade?", options = { @Option(name = "LIGHT"), @Option(name = "DARK") })
		Shade shade(String message);

	}

	/** A record with an enum component, which also has to be enumerable. */
	record EnumReport(
			@Choice(value = "Which shade?", options = { @Option(name = "LIGHT"), @Option(name = "DARK") }) Shade shade) {
	}

	/** A record answered into a record. */
	@RegisterTypeSafeService
	interface WithEnumRecord {

		EnumReport report(String message);

	}

	/** The annotation on something that is not an interface. */
	@RegisterTypeSafeService
	static class NotAnInterface {

		@Noul("Is it urgent?")
		boolean urgent(String message) {
			return false;
		}

	}

	/** An interface that also carries a CDI scope. */
	@RegisterTypeSafeService
	@ApplicationScoped
	interface Scoped {

		@Noul("Is it urgent?")
		boolean urgent(String message);

	}

	/** An interface with a type parameter. */
	@RegisterTypeSafeService
	interface Generic<T> {

		@Noul("Is it urgent?")
		boolean urgent(T state);

	}

	/** An interface with nothing to answer. */
	@RegisterTypeSafeService
	interface NoMethods {
	}

	/** A method with no question at all. */
	@RegisterTypeSafeService
	interface UnannotatedMethod {

		String whatever(String message);

	}

	/** A method declaring two questions but returning one answer. */
	@RegisterTypeSafeService
	interface TwoQuestionsOnAScalar {

		@Noul("Is it urgent?")
		@Score(value = "How frustrated?", levels = { "Calm", "Frustrated" })
		boolean urgent(String message);

	}

	/** A noul answered into a boolean's opposite: a choice. */
	@RegisterTypeSafeService
	interface WrongKind {

		@Choice(value = "Which team?", options = { @Option(name = "billing"), @Option(name = "technical") })
		boolean urgent(String message);

	}

	/** A return type nothing can fill. */
	@RegisterTypeSafeService
	interface UnsupportedReturnType {

		@Noul("Is it urgent?")
		Optional<String> urgent(String message);

	}

	/** A threshold where it means nothing. */
	@RegisterTypeSafeService
	interface ThresholdOnADouble {

		@Noul(value = "Is this a sentence?", threshold = 0.9d)
		double probability(String message);

	}

	/** A choice with nothing to choose between. */
	@RegisterTypeSafeService
	interface ChoiceWithOneOption {

		@Choice(value = "Which team?", options = { @Option(name = "billing") })
		String team(String message);

	}

	/** A choice whose second option has no label. */
	@RegisterTypeSafeService
	interface ChoiceOptionWithoutName {

		@Choice(value = "Which team?", options = { @Option(name = "billing"), @Option(name = "") })
		String team(String message);

	}

	/** A noul with neither instructions nor criteria. */
	@RegisterTypeSafeService
	interface EmptyNoul {

		@Noul
		boolean urgent(String message);

	}

	/** A score with one level. */
	@RegisterTypeSafeService
	interface ScoreWithOneLevel {

		@Score(value = "How frustrated?", levels = { "Calm" })
		int level(String message);

	}

	/** A method with no state to evaluate. */
	@RegisterTypeSafeService
	interface NoState {

		@Noul("Is it urgent?")
		boolean urgent();

	}

	/** A {@code @Questions} parameter where the answers could not be read anyway. */
	@RegisterTypeSafeService
	interface QuestionsOnAScalar {

		@Noul("Is it urgent?")
		boolean urgent(String message, @Questions Map<String, ? extends Question> extra);

	}

	/** A response asked for with no question. */
	@RegisterTypeSafeService
	interface NothingAsked {

		SystemOneResponse nothing(String message);

	}

	/** Two questions that would be answered under one name. */
	@RegisterTypeSafeService
	interface DuplicateAnswerNames {

		@Noul("Is it urgent?")
		@Choice(value = "Which team?", options = { @Option(name = "billing"), @Option(name = "technical") })
		SystemOneResponse everything(String message);

	}

	/** A record with a component that declares nothing. */
	record UnannotatedComponent(@Noul("Is it urgent?") boolean urgent, String team) {
	}

	/** A method returning a record whose components are not all questions. */
	@RegisterTypeSafeService
	interface RecordWithUnannotatedComponent {

		UnannotatedComponent report(String message);

	}

	/** A record nested in a record. */
	record NestedComponent(@Noul("Is it urgent?") GoodReport report) {
	}

	/** A method returning a record with a record component. */
	@RegisterTypeSafeService
	interface RecordWithRecordComponent {

		NestedComponent report(String message);

	}

	/** A record whose components declare questions under one name. */
	record SharedNames(@Noul(value = "Is it urgent?", name = "same") boolean urgent,
			@Noul(value = "Is the customer upset?", name = "same") boolean upset) {
	}

	/** A method returning that record. */
	@RegisterTypeSafeService
	interface RecordWithSharedNames {

		SharedNames report(String message);

	}

	/** A record return with a question on the method instead of on the components. */
	@RegisterTypeSafeService
	interface QuestionsOnARecordMethod {

		@Noul("Is it urgent?")
		GoodReport report(String message);

	}

	/** A record return with questions passed at run time, which nothing would read. */
	@RegisterTypeSafeService
	interface QuestionsParameterOnARecordMethod {

		GoodReport report(String message, @Questions Map<String, ? extends Question> extra);

	}

}
