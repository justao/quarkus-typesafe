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

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.quarkusverse.typesafe.service.runtime.CoercionRules;
import org.springaicommunity.typesafe.question.QuestionType;

/**
 * Checks a {@code @RegisterTypeSafeService} interface while the application is built.
 *
 * <p>
 * Everything here could be left to the call that eventually fails, and doing it now is the
 * whole point of the declarative layer: a return type that a question can never fill, a
 * record component with no question, an interface with a CDI scope — each of those is a
 * sentence in the build log instead of an exception in production, and none of them costs a
 * token to discover.
 *
 * <p>
 * The class is a pure function of the index, so the rules can be tested without starting
 * Quarkus. The same rules are enforced again while the proxy answers a call, from
 * {@link CoercionRules} — the one place the two views share — so a mistake that slips
 * through still fails with a sentence rather than a {@code ClassCastException}.
 *
 * <p>
 * It reads Jandex in a way that works on both major versions: {@code MethodInfo.parameters()}
 * only for its size, field and method flags instead of the newer {@code isDefault()}, and
 * nested annotation arrays through {@link #nested(AnnotationValue)} rather than assuming
 * whether the elements are {@code AnnotationInstance} or {@code AnnotationValue}.
 */
public final class TypeSafeServiceValidator {

	private static final String PREFIX = "org.quarkusverse.typesafe.service.";

	/** The registration annotation, which the processor also scans for. */
	static final DotName REGISTER = DotName.createSimple(PREFIX + "RegisterTypeSafeService");

	private static final DotName NOUL = DotName.createSimple(PREFIX + "Noul");

	private static final DotName NOULS = DotName.createSimple(PREFIX + "Nouls");

	private static final DotName CHOICE = DotName.createSimple(PREFIX + "Choice");

	private static final DotName CHOICES = DotName.createSimple(PREFIX + "Choices");

	private static final DotName SCORE = DotName.createSimple(PREFIX + "Score");

	private static final DotName SCORES = DotName.createSimple(PREFIX + "Scores");

	private static final DotName QUESTIONS = DotName.createSimple(PREFIX + "Questions");

	private static final DotName RECORD = DotName.createSimple("java.lang.Record");

	private static final double DEFAULT_THRESHOLD = 0.5d;

	/**
	 * Scopes that must not be on the interface: the extension registers the service itself,
	 * and ArC would otherwise look for an implementation of the interface.
	 */
	private static final DotName[] FORBIDDEN_SCOPES = { DotName.createSimple("jakarta.enterprise.context.ApplicationScoped"),
			DotName.createSimple("jakarta.enterprise.context.Dependent"),
			DotName.createSimple("jakarta.enterprise.context.RequestScoped"),
			DotName.createSimple("jakarta.enterprise.context.SessionScoped"),
			DotName.createSimple("jakarta.enterprise.context.ConversationScoped"),
			DotName.createSimple("jakarta.inject.Singleton") };

	/**
	 * What validating one interface produced.
	 *
	 * @param problems the reasons it cannot be a service, empty when it can
	 * @param reflectionTypes the types the runtime needs reflection on: the records it has
	 * to construct and the enums it has to enumerate
	 */
	public record Result(List<String> problems, Set<DotName> reflectionTypes) {
	}

	private TypeSafeServiceValidator() {
	}

	/**
	 * @param service the annotated interface, from the combined index
	 * @param index the combined index, for the types the interface mentions
	 * @return the problems and the types to register for reflection
	 */
	public static Result validate(ClassInfo service, IndexView index) {
		List<String> problems = new ArrayList<>();
		Set<DotName> reflectionTypes = new LinkedHashSet<>();
		String name = service.name().toString();

		if (!service.isInterface()) {
			problems.add(name + " is annotated @RegisterTypeSafeService but is not an interface");
			return new Result(problems, reflectionTypes);
		}
		for (DotName scope : FORBIDDEN_SCOPES) {
			if (service.classAnnotation(scope) != null) {
				problems.add(name + " declares @" + scope.withoutPackagePrefix()
						+ "; remove it, the extension registers the service as a singleton itself and a scoped "
						+ "interface makes ArC look for an implementation that does not exist");
			}
		}
		if (!service.typeParameters().isEmpty()) {
			problems.add(name + " declares type parameters; a service interface cannot be generic");
		}

		List<MethodInfo> methods = new ArrayList<>();
		for (MethodInfo method : service.methods()) {
			int flags = method.flags();
			if (Modifier.isStatic(flags) || Modifier.isPrivate(flags) || method.isSynthetic()) {
				continue;
			}
			// A default method is the interface's own code and is never sent to the API.
			if (!Modifier.isAbstract(flags)) {
				continue;
			}
			methods.add(method);
		}
		if (methods.isEmpty()) {
			problems.add(name + " declares no method to answer; add one or remove @RegisterTypeSafeService");
		}
		for (MethodInfo method : methods) {
			validateMethod(service, method, index, problems, reflectionTypes);
		}
		return new Result(problems, reflectionTypes);
	}

	private static void validateMethod(ClassInfo service, MethodInfo method, IndexView index, List<String> problems,
			Set<DotName> reflectionTypes) {
		String where = describe(service, method);
		Type returnType = method.returnType();
		ClassInfo returnClass = index.getClassByName(returnType.name());
		CoercionRules.Target target = CoercionRules.targetOf(returnType.name().toString(),
				returnClass != null && returnClass.isEnum(), isRecord(returnClass));
		if (target == CoercionRules.Target.ENUM) {
			// The runtime enumerates the constants to match a label or a level index, and a
			// native image has to be told about a class before it can be enumerated.
			reflectionTypes.add(returnType.name());
		}

		List<AnnotationInstance> questions = questions(method);
		List<Integer> askedParameters = askedParameters(method);
		int parameterCount = method.parameters().size();

		if (askedParameters.size() > 1) {
			problems.add(where + " marks " + askedParameters.size()
					+ " parameters with @Questions; only one can carry the runtime questions");
		}
		if (parameterCount - askedParameters.size() <= 0) {
			problems.add(where + " has no state: a service method needs at least one parameter carrying the content "
					+ "to evaluate");
		}
		else if (parameterCount - askedParameters.size() > 1) {
			for (int i = 0; i < parameterCount; i++) {
				if (askedParameters.contains(i)) {
					continue;
				}
				if (method.parameterName(i) == null) {
					problems.add(where + " takes several state parameters, which are sent as a map keyed by parameter "
							+ "name, but the names are not in the bytecode; compile with -parameters");
					break;
				}
			}
		}

		if (target == CoercionRules.Target.RECORD) {
			validateRecordReturn(where, returnType, returnClass, questions, askedParameters, index, problems,
					reflectionTypes);
			return;
		}

		if (target == CoercionRules.Target.RESPONSE) {
			if (questions.isEmpty() && askedParameters.isEmpty()) {
				problems.add(where + " returns a SystemOneResponse but asks nothing; declare a question annotation or "
						+ "a @Questions parameter");
			}
			checkDistinctNames(where, method.name(), questions, problems);
			return;
		}

		if (!CoercionRules.isSupported(target)) {
			problems.add(where + " returns " + returnType.name()
					+ ", which cannot hold an answer; supported returns are boolean, double, an integral type, String, "
					+ "an enum, NoulAnswer, ChoiceAnswer, ScoreAnswer, Answer, SystemOneResponse, or a record whose "
					+ "components declare their own questions");
			return;
		}
		if (questions.size() != 1) {
			problems.add(where + " returns " + CoercionRules.describe(target) + " and must declare exactly one "
					+ "question; it declares " + questions.size());
			return;
		}
		if (!askedParameters.isEmpty()) {
			problems.add(where + " has a @Questions parameter but returns " + CoercionRules.describe(target)
					+ "; only a SystemOneResponse can carry answers the method did not declare");
		}

		AnnotationInstance question = questions.get(0);
		QuestionType kind = kindOf(question);
		if (kind == null) {
			problems.add(where + " declares an annotation this extension does not know: " + question.name());
			return;
		}
		if (!CoercionRules.isCompatible(target, kind)) {
			problems.add(where + " returns " + CoercionRules.describe(target) + ", which needs "
					+ CoercionRules.acceptedKinds(target) + " question, but the method declares a "
					+ kind.name().toLowerCase() + " question");
		}
		if (threshold(question) != DEFAULT_THRESHOLD && !CoercionRules.allowsThreshold(target)) {
			problems.add(where + " sets a noul threshold but returns " + CoercionRules.describe(target)
					+ "; a threshold only applies to a boolean");
		}
		validateQuestion(where, question, problems);
	}

	private static void validateRecordReturn(String where, Type returnType, ClassInfo returnClass,
			List<AnnotationInstance> questions, List<Integer> askedParameters, IndexView index, List<String> problems,
			Set<DotName> reflectionTypes) {
		if (!questions.isEmpty()) {
			problems.add(where + " returns the record " + returnType.name()
					+ ", so its questions belong on the record's components, not on the method");
		}
		if (!askedParameters.isEmpty()) {
			problems.add(where + " returns a record and takes a @Questions parameter; the record's components already "
					+ "declare every question it can hold");
		}
		if (returnClass == null) {
			problems.add(where + " returns " + returnType.name() + ", which is not in the index; the record has to be "
					+ "part of the application (or of an indexed dependency) for its questions to be read");
			return;
		}
		reflectionTypes.add(returnType.name());

		int components = 0;
		Set<String> names = new LinkedHashSet<>();
		for (FieldInfo field : returnClass.fields()) {
			int flags = field.flags();
			if (Modifier.isStatic(flags) || field.isSynthetic()) {
				continue;
			}
			components++;
			List<AnnotationInstance> componentQuestions = questions(field);
			String componentWhere = where + " component " + field.name();
			if (componentQuestions.size() != 1) {
				problems.add(componentWhere + " must declare exactly one question; it declares "
						+ componentQuestions.size());
				continue;
			}
			AnnotationInstance question = componentQuestions.get(0);
			String answerName = answerName(question, field.name());
			if (!names.add(answerName)) {
				problems.add(componentWhere + " answers under the name '" + answerName
						+ "', which another component already uses; name it with @Noul(name = ...)");
			}
			QuestionType kind = kindOf(question);
			if (kind == null) {
				problems.add(componentWhere + " declares an annotation this extension does not know: " + question.name());
				continue;
			}
			validateQuestion(componentWhere, question, problems);

			ClassInfo componentClass = index.getClassByName(field.type().name());
			CoercionRules.Target componentTarget = CoercionRules.targetOf(field.type().name().toString(),
					componentClass != null && componentClass.isEnum(), isRecord(componentClass));
			if (componentTarget == CoercionRules.Target.RECORD) {
				problems.add(componentWhere + " is a record itself; a record component cannot hold several answers");
				continue;
			}
			if (!CoercionRules.isSupported(componentTarget) || componentTarget == CoercionRules.Target.RESPONSE) {
				problems.add(componentWhere + " is " + field.type().name() + ", which cannot hold an answer");
				continue;
			}
			if (!CoercionRules.isCompatible(componentTarget, kind)) {
				problems.add(componentWhere + " is " + CoercionRules.describe(componentTarget) + ", which needs "
						+ CoercionRules.acceptedKinds(componentTarget) + " question, but it declares a "
						+ kind.name().toLowerCase() + " question");
			}
			if (threshold(question) != DEFAULT_THRESHOLD && !CoercionRules.allowsThreshold(componentTarget)) {
				problems.add(componentWhere + " sets a noul threshold but is " + CoercionRules.describe(componentTarget)
						+ "; a threshold only applies to a boolean");
			}
			if (componentClass != null && componentClass.isEnum()) {
				reflectionTypes.add(field.type().name());
			}
		}
		if (components == 0) {
			problems.add(where + " returns the record " + returnType.name() + ", which declares no component");
		}
	}

	private static void validateQuestion(String where, AnnotationInstance question, List<String> problems) {
		QuestionType kind = kindOf(question);
		if (kind == QuestionType.NOUL) {
			if (string(question, "value").isEmpty() && string(question, "whenTrue").isEmpty()
					&& string(question, "whenFalse").isEmpty()) {
				problems.add(where + " declares a noul with neither instructions nor criteria; give it a value, a "
						+ "whenTrue, a whenFalse, or all three");
			}
		}
		else if (kind == QuestionType.CHOICE) {
			List<AnnotationInstance> options = nested(question.value("options"));
			if (options.size() < 2) {
				problems.add(where + " declares a choice with " + options.size() + " option(s); a choice needs at least "
						+ "two, otherwise there is nothing to choose between");
			}
			for (AnnotationInstance option : options) {
				if (string(option, "name").isEmpty()) {
					problems.add(where + " declares a choice option with an empty name");
				}
			}
		}
		else if (kind == QuestionType.SCORE) {
			AnnotationValue levels = question.value("levels");
			int count = levels == null ? 0 : levels.asStringArray().length;
			if (count < 2) {
				problems.add(where + " declares a score with " + count + " level(s); the SDK requires at least two, "
						+ "which is also the least a rubric can say");
			}
		}
	}

	/** Reads the name of the answer this question's answer will carry. */
	/** Reads the name of the answer this question's answer will carry. */
	private static String answerName(AnnotationInstance question, String defaultName) {
		String name = string(question, "name");
		return name.isEmpty() ? defaultName : name;
	}

	/**
	 * Two questions with one name would leave the response holding one answer where two were
	 * asked, and the method reading the missing one would fail at the call that matters, so
	 * the collision is a build error.
	 */
	private static void checkDistinctNames(String where, String defaultName, List<AnnotationInstance> questions,
			List<String> problems) {
		Set<String> names = new LinkedHashSet<>();
		for (AnnotationInstance question : questions) {
			String answerName = answerName(question, defaultName);
			if (!names.add(answerName)) {
				problems.add(where + " declares the answer name '" + answerName + "' more than once; every question of a "
						+ "method is answered under its own name, so name one of them with @Noul(name = ...) or move it "
						+ "to a record component");
			}
		}
	}

	private static String string(AnnotationInstance instance, String member) {
		AnnotationValue value = instance.value(member);
		return value == null ? "" : value.asString();
	}

	private static double threshold(AnnotationInstance question) {
		AnnotationValue value = question.value("threshold");
		return value == null ? DEFAULT_THRESHOLD : value.asDouble();
	}

	private static QuestionType kindOf(AnnotationInstance question) {
		DotName name = question.name();
		if (NOUL.equals(name) || NOULS.equals(name)) {
			return QuestionType.NOUL;
		}
		if (CHOICE.equals(name) || CHOICES.equals(name)) {
			return QuestionType.CHOICE;
		}
		if (SCORE.equals(name) || SCORES.equals(name)) {
			return QuestionType.SCORE;
		}
		return null;
	}

	/**
	 * The questions declared on a method or on one of a record's fields, with the repeatable
	 * containers unwrapped.
	 */
	private static List<AnnotationInstance> questions(AnnotationTarget target) {
		List<AnnotationInstance> found = new ArrayList<>();
		for (AnnotationInstance instance : target.annotations()) {
			if (target instanceof MethodInfo && instance.target().kind() != AnnotationTarget.Kind.METHOD) {
				// Method annotations include the ones on its parameters and return type.
				continue;
			}
			QuestionType kind = kindOf(instance.name());
			if (kind == null) {
				continue;
			}
			DotName name = instance.name();
			if (NOULS.equals(name) || CHOICES.equals(name) || SCORES.equals(name)) {
				found.addAll(nested(instance.value()));
			}
			else {
				found.add(instance);
			}
		}
		return found;
	}

	/** The positions of the parameters marked with {@code @Questions}. */
	private static List<Integer> askedParameters(MethodInfo method) {
		List<Integer> positions = new ArrayList<>();
		for (AnnotationInstance instance : method.annotations()) {
			if (!QUESTIONS.equals(instance.name())
					|| instance.target().kind() != AnnotationTarget.Kind.METHOD_PARAMETER) {
				continue;
			}
			positions.add(instance.target().asMethodParameter().position());
		}
		return positions;
	}

	/**
	 * Unwraps an array of nested annotations.
	 *
	 * <p>
	 * Jandex 2 hands back {@code AnnotationValue[]} here and Jandex 3 hands back
	 * {@code AnnotationInstance[]}, so the array is treated as {@code Object[]} and each
	 * element is inspected for which of the two it is. Both are supported because the
	 * extension has to build against either.
	 */
	private static List<AnnotationInstance> nested(AnnotationValue value) {
		List<AnnotationInstance> instances = new ArrayList<>();
		if (value == null || value.kind() != AnnotationValue.Kind.ARRAY) {
			return instances;
		}
		for (Object element : (Object[]) value.asNestedArray()) {
			if (element instanceof AnnotationInstance instance) {
				instances.add(instance);
			}
			else if (element instanceof AnnotationValue nested) {
				instances.add(nested.asNested());
			}
		}
		return instances;
	}

	private static boolean isRecord(ClassInfo info) {
		// ClassInfo.isRecord() is Jandex 3; every record extends java.lang.Record, which both
		// versions report.
		return info != null && RECORD.equals(info.superName());
	}

	private static String describe(ClassInfo service, MethodInfo method) {
		String base = service.name().toString() + "." + method.name();
		return method.parameters().isEmpty() ? base + "()" : base + "(...)";
	}

}
