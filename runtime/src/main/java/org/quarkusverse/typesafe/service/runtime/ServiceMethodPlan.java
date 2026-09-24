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

package org.quarkusverse.typesafe.service.runtime;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.quarkusverse.typesafe.service.Choice;
import org.quarkusverse.typesafe.service.Choices;
import org.quarkusverse.typesafe.service.Model;
import org.quarkusverse.typesafe.service.Noul;
import org.quarkusverse.typesafe.service.Nouls;
import org.quarkusverse.typesafe.service.Option;
import org.quarkusverse.typesafe.service.Questions;
import org.quarkusverse.typesafe.service.RegisterTypeSafeService;
import org.quarkusverse.typesafe.service.Score;
import org.quarkusverse.typesafe.service.Scores;
import org.quarkusverse.typesafe.service.TypeSafeServiceException;
import org.springaicommunity.typesafe.JsonContent;
import org.springaicommunity.typesafe.TypeSafeClient;
import org.springaicommunity.typesafe.question.Question;
import org.springaicommunity.typesafe.question.QuestionType;
import org.springaicommunity.typesafe.question.SystemOneRequest;
import org.springaicommunity.typesafe.response.SystemOneResponse;

/**
 * Everything one service method needs to become a Jev call, read from its annotations once
 * and then reused.
 *
 * <p>
 * A plan is built on the first call of a method and cached by the handler. The build has
 * already rejected the shapes that cannot work, so the checks here are the defensive half of
 * the same rules: they exist so that a mistake reaching this far fails with a sentence
 * naming the method rather than a {@code ClassCastException} three frames deep.
 *
 * <p>
 * Note which types this class imports: the question <em>annotations</em> from
 * {@code org.quarkusverse.typesafe.service} and the SDK's {@code Question},
 * {@code QuestionType} and {@code SystemOneRequest}, but not the SDK's {@code Noul},
 * {@code Choice} or {@code Score} classes — those collide by simple name with the
 * annotations, so they live in {@link ServiceQuestionFactory} and {@link AnswerCoercers}
 * instead. Keeping one class to one side of the collision is what makes it harmless.
 */
final class ServiceMethodPlan {

	/** Converts a response into whatever the method declared. */
	@FunctionalInterface
	interface ResultMapper {

		/**
		 * @param response the response of the call
		 * @return the value the method returns
		 * @throws Throwable whatever the declared type's construction threw
		 */
		Object map(SystemOneResponse response) throws Throwable;

	}

	/**
	 * One question as the annotations described it.
	 *
	 * @param name the answer key
	 * @param kind which primitive is being asked
	 * @param instructions plain text instructions, or an empty string
	 * @param whenTrue the true outcome's description, or an empty string
	 * @param whenFalse the false outcome's description, or an empty string
	 * @param options the choice options, in declaration order
	 * @param levels the score levels, ordered from the lowest upwards
	 * @param threshold the truth threshold, meaningful only for a boolean target
	 */
	record QuestionTemplate(String name, QuestionType kind, String instructions, String whenTrue, String whenFalse,
			List<OptionTemplate> options, List<String> levels, double threshold) {
	}

	/**
	 * One choice option.
	 *
	 * @param name the label the model selects
	 * @param criteria what the label means, or an empty string
	 */
	record OptionTemplate(String name, String criteria) {
	}

	/**
	 * One component of a returned record, paired with the question that fills it.
	 *
	 * @param name the component name, which is the answer key unless the annotation renamed it
	 * @param type the component type
	 * @param question the question that answers it
	 */
	private record ComponentTemplate(String name, Class<?> type, QuestionTemplate question) {
	}

	private static final ResultMapper RESPONSE_MAPPER = response -> response;

	private final String methodName;

	private final int[] stateIndexes;

	private final String[] stateNames;

	private final int questionsIndex;

	private final String model;

	private final List<QuestionTemplate> questions;

	private final ResultMapper mapper;

	private ServiceMethodPlan(String methodName, int[] stateIndexes, String[] stateNames, int questionsIndex,
			String model, List<QuestionTemplate> questions, ResultMapper mapper) {
		this.methodName = methodName;
		this.stateIndexes = stateIndexes;
		this.stateNames = stateNames;
		this.questionsIndex = questionsIndex;
		this.model = model;
		this.questions = questions;
		this.mapper = mapper;
	}

	/**
	 * Reads a method's annotations into a plan.
	 * @param method the interface method
	 * @param interfaceName the interface it belongs to, for error messages
	 * @return the plan
	 */
	static ServiceMethodPlan of(Method method, String interfaceName) {
		String methodName = describe(interfaceName, method);
		Class<?> returnType = method.getReturnType();
		CoercionRules.Target target = CoercionRules.targetOf(returnType.getName(), returnType.isEnum(),
				returnType.isRecord());

		List<QuestionTemplate> questions = new ArrayList<>();
		ResultMapper mapper;
		if (target == CoercionRules.Target.RECORD) {
			List<ComponentTemplate> components = components(returnType, methodName);
			for (ComponentTemplate component : components) {
				questions.add(component.question());
			}
			mapper = new RecordMapper(constructor(returnType, components, methodName), components, methodName);
		}
		else if (target == CoercionRules.Target.RESPONSE) {
			questions.addAll(declaredQuestions(method));
			mapper = RESPONSE_MAPPER;
		}
		else {
			questions.addAll(declaredQuestions(method));
			if (questions.size() != 1) {
				throw new TypeSafeServiceException(methodName + " returns " + CoercionRules.describe(target)
						+ ", so it must declare exactly one question; it declares " + questions.size());
			}
			QuestionTemplate question = questions.get(0);
			mapper = new ScalarMapper(
					AnswerCoercers.coercer(returnType, question.kind(), question.threshold(), methodName), question.name());
		}

		Parameter[] parameters = method.getParameters();
		String[] stateNames = new String[parameters.length];
		List<Integer> stateIndexes = new ArrayList<>();
		int questionsIndex = -1;
		for (int i = 0; i < parameters.length; i++) {
			Parameter parameter = parameters[i];
			stateNames[i] = parameter.getName();
			if (parameter.isAnnotationPresent(Questions.class)) {
				questionsIndex = i;
			}
			else {
				stateIndexes.add(i);
			}
		}
		if (stateIndexes.isEmpty()) {
			throw new TypeSafeServiceException(methodName + " has no state: a service method needs at least one "
					+ "parameter carrying the content to evaluate");
		}
		if (questionsIndex >= 0 && target != CoercionRules.Target.RESPONSE) {
			throw new TypeSafeServiceException(methodName + " has a @Questions parameter but returns "
					+ CoercionRules.describe(target) + "; only a SystemOneResponse can carry answers the method "
					+ "did not declare");
		}
		if (stateIndexes.size() > 1 && !parameters[stateIndexes.get(0)].isNamePresent()) {
			throw new TypeSafeServiceException(methodName + " takes several state parameters, which are sent as a "
					+ "map keyed by parameter name, but the names are not in the bytecode: compile with -parameters");
		}

		int[] indexes = new int[stateIndexes.size()];
		for (int i = 0; i < indexes.length; i++) {
			indexes[i] = stateIndexes.get(i);
		}
		return new ServiceMethodPlan(methodName, indexes, stateNames, questionsIndex, model(method), questions, mapper);
	}

	/**
	 * Answers the call.
	 * @param client the client to send it with
	 * @param args the arguments the method was invoked with
	 * @return the value the method returns
	 * @throws Throwable whatever the declared type's construction threw
	 */
	Object invoke(TypeSafeClient client, Object[] args) throws Throwable {
		Map<String, Question> asked = new LinkedHashMap<>();
		for (QuestionTemplate template : this.questions) {
			if (asked.put(template.name(), ServiceQuestionFactory.toQuestion(template)) != null) {
				throw new TypeSafeServiceException(
						"The question '" + template.name() + "' is declared twice on " + this.methodName);
			}
		}
		if (this.questionsIndex >= 0) {
			merge(asked, args[this.questionsIndex]);
		}
		if (asked.isEmpty()) {
			throw new TypeSafeServiceException(this.methodName + " asked nothing: declare a question annotation or pass "
					+ "a @Questions parameter with at least one entry");
		}

		SystemOneRequest.Builder request = SystemOneRequest.builder().state(stateOf(args));
		if (!this.model.isEmpty()) {
			request.model(this.model);
		}
		request.questions(asked);

		return this.mapper.map(client.systemOne(request.build()));
	}

	private JsonContent stateOf(Object[] args) {
		if (this.stateIndexes.length == 1) {
			Object state = args[this.stateIndexes[0]];
			if (state == null) {
				throw new TypeSafeServiceException("The state passed to " + this.methodName + " is null");
			}
			return JsonContent.of(state);
		}
		Map<String, Object> state = new LinkedHashMap<>();
		for (int i = 0; i < this.stateIndexes.length; i++) {
			state.put(this.stateNames[this.stateIndexes[i]], args[this.stateIndexes[i]]);
		}
		return JsonContent.of(state);
	}

	private void merge(Map<String, Question> asked, Object extra) {
		if (extra == null) {
			return;
		}
		if (!(extra instanceof Map<?, ?> questions)) {
			throw new TypeSafeServiceException("The @Questions parameter of " + this.methodName + " must be a Map, not "
					+ extra.getClass().getName());
		}
		for (Map.Entry<?, ?> entry : questions.entrySet()) {
			if (!(entry.getKey() instanceof String name) || !(entry.getValue() instanceof Question question)) {
				throw new TypeSafeServiceException("The @Questions parameter of " + this.methodName + " must map names to "
						+ "Questions; it carried " + entry.getKey() + " -> " + entry.getValue());
			}
			if (asked.putIfAbsent(name, question) != null) {
				throw new TypeSafeServiceException("The question '" + name + "' is declared both by an annotation on "
						+ this.methodName + " and by its @Questions parameter");
			}
		}
	}

	private static String model(Method method) {
		Model declared = method.getAnnotation(Model.class);
		if (declared != null) {
			return declared.value();
		}
		RegisterTypeSafeService service = method.getDeclaringClass().getAnnotation(RegisterTypeSafeService.class);
		return service == null ? "" : service.model();
	}

	private static List<QuestionTemplate> declaredQuestions(Method method) {
		List<QuestionTemplate> questions = new ArrayList<>();
		for (Noul noul : repeated(method, Noul.class, Nouls.class, Nouls::value)) {
			questions.add(noulTemplate(noul, method.getName()));
		}
		for (Choice choice : repeated(method, Choice.class, Choices.class, Choices::value)) {
			questions.add(choiceTemplate(choice, method.getName()));
		}
		for (Score score : repeated(method, Score.class, Scores.class, Scores::value)) {
			questions.add(scoreTemplate(score, method.getName()));
		}
		return questions;
	}

	private static QuestionTemplate questionOf(AnnotatedElement element, String defaultName, String where) {
		List<QuestionTemplate> questions = new ArrayList<>();
		for (Noul noul : repeated(element, Noul.class, Nouls.class, Nouls::value)) {
			questions.add(noulTemplate(noul, defaultName));
		}
		for (Choice choice : repeated(element, Choice.class, Choices.class, Choices::value)) {
			questions.add(choiceTemplate(choice, defaultName));
		}
		for (Score score : repeated(element, Score.class, Scores.class, Scores::value)) {
			questions.add(scoreTemplate(score, defaultName));
		}
		if (questions.size() != 1) {
			throw new TypeSafeServiceException(where + " must declare exactly one question, it declares "
					+ questions.size());
		}
		return questions.get(0);
	}

	/**
	 * Reads an annotation that may have been written once or repeated. The compiler stores
	 * only the container once the annotation is repeated, so both have to be looked for.
	 */
	private static <A extends Annotation, C extends Annotation> List<A> repeated(AnnotatedElement element,
			Class<A> annotation, Class<C> container, java.util.function.Function<C, A[]> unpack) {
		List<A> found = new ArrayList<>();
		A single = element.getAnnotation(annotation);
		if (single != null) {
			found.add(single);
		}
		C many = element.getAnnotation(container);
		if (many != null) {
			found.addAll(List.of(unpack.apply(many)));
		}
		return found;
	}

	private static QuestionTemplate noulTemplate(Noul noul, String defaultName) {
		return new QuestionTemplate(name(noul.name(), defaultName), QuestionType.NOUL, noul.value(), noul.whenTrue(),
				noul.whenFalse(), List.of(), List.of(), noul.threshold());
	}

	private static QuestionTemplate choiceTemplate(Choice choice, String defaultName) {
		List<OptionTemplate> options = new ArrayList<>();
		for (Option option : choice.options()) {
			options.add(new OptionTemplate(option.name(), option.criteria()));
		}
		return new QuestionTemplate(name(choice.name(), defaultName), QuestionType.CHOICE, choice.value(), "", "",
				options, List.of(), 0.5d);
	}

	private static QuestionTemplate scoreTemplate(Score score, String defaultName) {
		return new QuestionTemplate(name(score.name(), defaultName), QuestionType.SCORE, score.value(), "", "",
				List.of(), List.of(score.levels()), 0.5d);
	}

	private static String name(String declared, String defaultName) {
		return declared.isEmpty() ? defaultName : declared;
	}

	/**
	 * Reads a returned record's components. The record components are the first choice
	 * because they carry the names and the order the canonical constructor expects; the
	 * declared fields are the fallback, and hold the same annotations because the question
	 * annotations target {@code FIELD} as well as {@code RECORD_COMPONENT}.
	 */
	private static List<ComponentTemplate> components(Class<?> recordType, String methodName) {
		List<ComponentTemplate> components = new ArrayList<>();
		RecordComponent[] recordComponents = recordType.getRecordComponents();
		if (recordComponents != null && recordComponents.length > 0) {
			for (RecordComponent component : recordComponents) {
				components.add(new ComponentTemplate(component.getName(), component.getType(),
						questionOf(component, component.getName(), methodName + " component " + component.getName())));
			}
			return components;
		}
		for (Field field : recordType.getDeclaredFields()) {
			if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
				continue;
			}
			components.add(new ComponentTemplate(field.getName(), field.getType(),
					questionOf(field, field.getName(), methodName + " component " + field.getName())));
		}
		if (components.isEmpty()) {
			throw new TypeSafeServiceException(methodName + " returns " + recordType.getName()
					+ ", which the runtime sees neither as a record nor as one with fields");
		}
		return components;
	}

	private static Constructor<?> constructor(Class<?> recordType, List<ComponentTemplate> components,
			String methodName) {
		Class<?>[] types = new Class<?>[components.size()];
		for (int i = 0; i < types.length; i++) {
			types[i] = components.get(i).type();
		}
		try {
			Constructor<?> constructor = recordType.getDeclaredConstructor(types);
			constructor.setAccessible(true);
			return constructor;
		}
		catch (NoSuchMethodException ex) {
			throw new TypeSafeServiceException(
					"Cannot find the canonical constructor of " + recordType.getName() + " (" + methodName + ")", ex);
		}
	}

	private static String describe(String interfaceName, Method method) {
		StringBuilder description = new StringBuilder(interfaceName).append('.').append(method.getName()).append('(');
		Class<?>[] types = method.getParameterTypes();
		for (int i = 0; i < types.length; i++) {
			if (i > 0) {
				description.append(", ");
			}
			description.append(types[i].getSimpleName());
		}
		return description.append(')').toString();
	}

	/** Maps one answer into a scalar, enum or answer-typed return value. */
	private record ScalarMapper(AnswerCoercers.Coercer coercer, String questionName) implements ResultMapper {

		@Override
		public Object map(SystemOneResponse response) {
			return this.coercer.coerce(response, this.questionName);
		}

	}

	/** Fills a record's components from the answers of one call. */
	private static final class RecordMapper implements ResultMapper {

		private final Constructor<?> constructor;

		private final String[] questionNames;

		private final AnswerCoercers.Coercer[] coercers;

		private final String methodName;

		RecordMapper(Constructor<?> constructor, List<ComponentTemplate> components, String methodName) {
			this.constructor = constructor;
			this.methodName = methodName;
			this.questionNames = new String[components.size()];
			this.coercers = new AnswerCoercers.Coercer[components.size()];
			for (int i = 0; i < components.size(); i++) {
				ComponentTemplate component = components.get(i);
				this.questionNames[i] = component.question().name();
				this.coercers[i] = AnswerCoercers.coercer(component.type(), component.question().kind(),
						component.question().threshold(), methodName);
			}
		}

		@Override
		public Object map(SystemOneResponse response) throws Throwable {
			Object[] values = new Object[this.coercers.length];
			for (int i = 0; i < values.length; i++) {
				values[i] = this.coercers[i].coerce(response, this.questionNames[i]);
			}
			try {
				return this.constructor.newInstance(values);
			}
			catch (InvocationTargetException ex) {
				// The record's own validation is the caller's exception, not ours.
				throw ex.getCause() == null ? ex : ex.getCause();
			}
			catch (ReflectiveOperationException ex) {
				throw new TypeSafeServiceException("Cannot construct " + this.constructor.getDeclaringClass().getName()
						+ " for " + this.methodName, ex);
			}
		}

	}

}
