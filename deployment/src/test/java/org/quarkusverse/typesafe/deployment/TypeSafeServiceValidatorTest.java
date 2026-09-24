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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The rules the build enforces, one test per rule.
 *
 * <p>
 * Each fixture compiles — that is the point of the class: a method that returns a boolean
 * where a choice was declared, a record component with no question, a threshold on a double.
 * None of those is a compile error and none of them is visible until something reads the
 * annotations, which is what this test does through the same validator the build step calls.
 */
class TypeSafeServiceValidatorTest {

	private static final IndexView FIXTURES = indexFixtures();

	@Test
	void acceptsEverySupportedShape() {
		assertThat(problemsOf(TypeSafeServiceFixtures.Good.class)).isEmpty();
	}

	@Test
	void registersTheRecordsAndEnumsTheRuntimeReflectsOn() {
		assertThat(reflectionTypesOf(TypeSafeServiceFixtures.WithEnum.class))
			.containsExactly(name(TypeSafeServiceFixtures.Shade.class));

		assertThat(reflectionTypesOf(TypeSafeServiceFixtures.WithEnumRecord.class))
			.containsExactly(name(TypeSafeServiceFixtures.EnumReport.class), name(TypeSafeServiceFixtures.Shade.class));
	}

	@Test
	void rejectsSomethingThatIsNotAnInterface() {
		assertThat(problemsOf(TypeSafeServiceFixtures.NotAnInterface.class))
			.singleElement()
			.satisfies(problem -> assertThat(problem).contains("is not an interface"));
	}

	@Test
	void rejectsAnInterfaceThatAlsoCarriesAScope() {
		assertThat(problemsOf(TypeSafeServiceFixtures.Scoped.class))
			.singleElement()
			.satisfies(problem -> assertThat(problem).contains("ApplicationScoped").contains("remove it"));
	}

	@Test
	void rejectsAGenericInterface() {
		assertThat(problemsOf(TypeSafeServiceFixtures.Generic.class))
			.singleElement()
			.satisfies(problem -> assertThat(problem).contains("cannot be generic"));
	}

	@Test
	void rejectsAnInterfaceWithNothingToAnswer() {
		assertThat(problemsOf(TypeSafeServiceFixtures.NoMethods.class))
			.singleElement()
			.satisfies(problem -> assertThat(problem).contains("declares no method to answer"));
	}

	@Test
	void rejectsAMethodWithNoQuestion() {
		assertThat(problemsOf(TypeSafeServiceFixtures.UnannotatedMethod.class))
			.singleElement()
			.satisfies(problem -> assertThat(problem).contains("declares exactly one question").contains("declares 0"));
	}

	@Test
	void rejectsTwoQuestionsAnsweredIntoOneValue() {
		assertThat(problemsOf(TypeSafeServiceFixtures.TwoQuestionsOnAScalar.class))
			.singleElement()
			.satisfies(problem -> assertThat(problem).contains("declares exactly one question").contains("declares 2"));
	}

	@Test
	void rejectsAQuestionThatCannotFillTheReturnType() {
		assertThat(problemsOf(TypeSafeServiceFixtures.WrongKind.class))
			.singleElement()
			.satisfies(problem -> assertThat(problem).contains("needs a noul question")
				.contains("declares a choice question"));
	}

	@Test
	void rejectsAReturnTypeNothingCanFill() {
		assertThat(problemsOf(TypeSafeServiceFixtures.UnsupportedReturnType.class))
			.singleElement()
			.satisfies(problem -> assertThat(problem).contains("cannot hold an answer")
				.contains("SystemOneResponse"));
	}

	@Test
	void rejectsAThresholdWhereItMeansNothing() {
		assertThat(problemsOf(TypeSafeServiceFixtures.ThresholdOnADouble.class))
			.singleElement()
			.satisfies(problem -> assertThat(problem).contains("threshold").contains("only applies to a boolean"));
	}

	@Test
	void rejectsAChoiceWithOneOption() {
		assertThat(problemsOf(TypeSafeServiceFixtures.ChoiceWithOneOption.class))
			.singleElement()
			.satisfies(problem -> assertThat(problem).contains("1 option(s)").contains("at least two"));
	}

	@Test
	void rejectsAnOptionWithoutALabel() {
		assertThat(problemsOf(TypeSafeServiceFixtures.ChoiceOptionWithoutName.class))
			.singleElement()
			.satisfies(problem -> assertThat(problem).contains("empty name"));
	}

	@Test
	void rejectsANoulWithNeitherInstructionsNorCriteria() {
		assertThat(problemsOf(TypeSafeServiceFixtures.EmptyNoul.class))
			.singleElement()
			.satisfies(problem -> assertThat(problem).contains("neither instructions nor criteria"));
	}

	@Test
	void rejectsAScoreWithOneLevel() {
		assertThat(problemsOf(TypeSafeServiceFixtures.ScoreWithOneLevel.class))
			.singleElement()
			.satisfies(problem -> assertThat(problem).contains("1 level(s)").contains("at least two"));
	}

	@Test
	void rejectsAMethodWithNoState() {
		assertThat(problemsOf(TypeSafeServiceFixtures.NoState.class))
			.singleElement()
			.satisfies(problem -> assertThat(problem).contains("has no state"));
	}

	@Test
	void rejectsRuntimeQuestionsOnAMethodThatCouldNotReadThem() {
		assertThat(problemsOf(TypeSafeServiceFixtures.QuestionsOnAScalar.class))
			.singleElement()
			.satisfies(problem -> assertThat(problem).contains("@Questions parameter")
				.contains("only a SystemOneResponse"));
	}

	@Test
	void rejectsAResponseAskedForWithNoQuestion() {
		assertThat(problemsOf(TypeSafeServiceFixtures.NothingAsked.class))
			.singleElement()
			.satisfies(problem -> assertThat(problem).contains("asks nothing"));
	}

	@Test
	void rejectsTwoQuestionsAnsweredUnderOneName() {
		assertThat(problemsOf(TypeSafeServiceFixtures.DuplicateAnswerNames.class))
			.singleElement()
			.satisfies(problem -> assertThat(problem).contains("the answer name 'everything' more than once"));
	}

	@Test
	void rejectsARecordComponentWithoutAQuestion() {
		assertThat(problemsOf(TypeSafeServiceFixtures.RecordWithUnannotatedComponent.class))
			.singleElement()
			.satisfies(problem -> assertThat(problem).contains("component team")
				.contains("declares exactly one question")
				.contains("declares 0"));
	}

	@Test
	void rejectsARecordComponentThatIsItselfARecord() {
		assertThat(problemsOf(TypeSafeServiceFixtures.RecordWithRecordComponent.class))
			.singleElement()
			.satisfies(problem -> assertThat(problem).contains("component report").contains("is a record itself"));
	}

	@Test
	void rejectsRecordComponentsSharingAnAnswerName() {
		assertThat(problemsOf(TypeSafeServiceFixtures.RecordWithSharedNames.class))
			.singleElement()
			.satisfies(problem -> assertThat(problem).contains("another component already uses"));
	}

	@Test
	void rejectsQuestionsOnAMethodReturningARecord() {
		assertThat(problemsOf(TypeSafeServiceFixtures.QuestionsOnARecordMethod.class))
			.singleElement()
			.satisfies(problem -> assertThat(problem).contains("belong on the record's components"));
	}

	@Test
	void rejectsRuntimeQuestionsOnAMethodReturningARecord() {
		assertThat(problemsOf(TypeSafeServiceFixtures.QuestionsParameterOnARecordMethod.class))
			.singleElement()
			.satisfies(problem -> assertThat(problem).contains("takes a @Questions parameter"));
	}

	private static List<String> problemsOf(Class<?> fixture) {
		return TypeSafeServiceValidator.validate(classInfo(fixture), FIXTURES).problems();
	}

	private static List<DotName> reflectionTypesOf(Class<?> fixture) {
		return List.copyOf(TypeSafeServiceValidator.validate(classInfo(fixture), FIXTURES).reflectionTypes());
	}

	private static ClassInfo classInfo(Class<?> fixture) {
		ClassInfo info = FIXTURES.getClassByName(name(fixture));
		assertThat(info).as("%s has to be in the fixture index", fixture.getName()).isNotNull();
		return info;
	}

	private static DotName name(Class<?> type) {
		return DotName.createSimple(type.getName());
	}

	/**
	 * Indexes every fixture, so a record a method returns can be read back out of the index
	 * the way the build reads it out of the application's index.
	 */
	private static IndexView indexFixtures() {
		try {
			Indexer indexer = new Indexer();
			for (Class<?> fixture : TypeSafeServiceFixtures.class.getDeclaredClasses()) {
				try (InputStream in = fixture.getClassLoader()
					.getResourceAsStream(fixture.getName().replace('.', '/') + ".class")) {
					assertThat(in).as("%s has to be on the test classpath", fixture.getName()).isNotNull();
					indexer.index(in);
				}
			}
			return indexer.complete();
		}
		catch (IOException ex) {
			throw new IllegalStateException("Cannot index the fixtures", ex);
		}
	}

}
