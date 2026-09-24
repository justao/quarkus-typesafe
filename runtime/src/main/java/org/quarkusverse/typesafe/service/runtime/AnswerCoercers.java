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

import java.util.StringJoiner;

import org.quarkusverse.typesafe.service.TypeSafeServiceCoercionException;
import org.quarkusverse.typesafe.service.TypeSafeServiceException;
import org.springaicommunity.typesafe.question.QuestionType;
import org.springaicommunity.typesafe.response.SystemOneResponse;

/**
 * Turns one answer of a response into the value a service method declared.
 *
 * <p>
 * A coercer only ever sees kinds the build accepted; the SDK's own exceptions (an answer of
 * an unexpected kind, a name that is not in the response) are left to propagate unchanged,
 * so a caller keeps catching the typed exceptions it already knows.
 */
final class AnswerCoercers {

	private AnswerCoercers() {
	}

	/**
	 * Converts one answer.
	 */
	@FunctionalInterface
	interface Coercer {

		/**
		 * @param response the response the answer arrived in
		 * @param questionName the answer key
		 * @return the declared return value
		 */
		Object coerce(SystemOneResponse response, String questionName);

	}

	/**
	 * @param type the declared type
	 * @param kind the question's kind
	 * @param threshold the noul threshold, used only for a boolean target
	 * @param method the service method, for error messages
	 * @return the coercer for that combination
	 */
	static Coercer coercer(Class<?> type, QuestionType kind, double threshold, String method) {
		CoercionRules.Target target = CoercionRules.targetOf(type.getName(), type.isEnum(), type.isRecord());
		return switch (target) {
			case BOOLEAN -> (response, name) -> response.noul(name).isTrue(threshold);
			case DOUBLE -> kind == QuestionType.SCORE ? (response, name) -> response.score(name).value()
					: (response, name) -> response.noul(name).value();
			case INTEGRAL -> (response, name) -> narrow(response.score(name).nearestLevel(), type, method, name);
			case STRING -> kind == QuestionType.SCORE ? (response, name) -> response.score(name).nearestLabel()
					: (response, name) -> response.choice(name).value();
			case ENUM -> enumCoercer(type, kind, method);
			case NOUL_ANSWER -> (response, name) -> response.noul(name);
			case CHOICE_ANSWER -> (response, name) -> response.choice(name);
			case SCORE_ANSWER -> (response, name) -> response.score(name);
			case ANSWER -> (response, name) -> response.answer(name);
			// The build rejects these, so reaching one means the two views of the rule table
			// disagreed; failing loudly here is better than a ClassCastException later.
			case RECORD, RESPONSE, UNSUPPORTED -> throw new TypeSafeServiceException(
					"Cannot produce " + CoercionRules.describe(target) + " from one answer of " + method);
		};
	}

	private static Coercer enumCoercer(Class<?> type, QuestionType kind, String method) {
		Object[] constants = type.getEnumConstants();
		if (constants == null || constants.length == 0) {
			throw new TypeSafeServiceException(type.getName() + " is declared as an enum but declares no constants (" + method + ")");
		}
		if (kind == QuestionType.SCORE) {
			return (response, name) -> {
				int level = response.score(name).nearestLevel();
				if (level < 0 || level >= constants.length) {
					throw new TypeSafeServiceCoercionException(method, name,
							"the score placed the state at level " + level + ", which is outside " + type.getSimpleName()
									+ " (" + constants.length + " constants)");
				}
				return constants[level];
			};
		}
		return (response, name) -> {
			String value = response.choice(name).value();
			for (Object constant : constants) {
				if (((Enum<?>) constant).name().equals(value)) {
					return constant;
				}
			}
			throw new TypeSafeServiceCoercionException(method, name,
					"the choice selected '" + value + "', which is not a constant of " + type.getName() + " ("
							+ names(constants) + ")");
		};
	}

	private static Object narrow(int value, Class<?> type, String method, String name) {
		return switch (type.getName()) {
			case "int", "java.lang.Integer" -> value;
			case "long", "java.lang.Long" -> (long) value;
			case "short", "java.lang.Short" -> {
				if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) {
					throw new TypeSafeServiceCoercionException(method, name, "the level " + value + " does not fit a short");
				}
				yield (short) value;
			}
			case "byte", "java.lang.Byte" -> {
				if (value < Byte.MIN_VALUE || value > Byte.MAX_VALUE) {
					throw new TypeSafeServiceCoercionException(method, name, "the level " + value + " does not fit a byte");
				}
				yield (byte) value;
			}
			default -> throw new TypeSafeServiceException("Cannot produce " + type.getName() + " from a level (" + method + ")");
		};
	}

	private static String names(Object[] constants) {
		StringJoiner names = new StringJoiner(", ");
		for (Object constant : constants) {
			names.add(((Enum<?>) constant).name());
		}
		return names.toString();
	}

}
