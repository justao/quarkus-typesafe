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

import org.springaicommunity.typesafe.question.QuestionType;

/**
 * Which declared Java type can hold which kind of Jev answer.
 *
 * <p>
 * This is the single source of truth for that question, and it is deliberately expressed on
 * type <em>names</em> rather than on {@code Class} objects: the build step validating a
 * service interface sees Jandex types, the invocation handler sees reflected types, and both
 * have to agree. A rule that lived in two places would let a build pass a combination the
 * call then fails on.
 *
 * <p>
 * It is public because the deployment module reads it; it is not part of the supported API.
 */
public final class CoercionRules {

	/**
	 * What a method — or one component of a returned record — can be turned into.
	 */
	public enum Target {

		/** A truth value, thresholded. */
		BOOLEAN,

		/** A weighted value: a noul's truth value or a score's weighted score. */
		DOUBLE,

		/** A level index. */
		INTEGRAL,

		/** A choice label, or the description of the nearest score level. */
		STRING,

		/** An enum constant: by name for a choice, by level index for a score. */
		ENUM,

		/** The noul answer itself. */
		NOUL_ANSWER,

		/** The choice answer itself. */
		CHOICE_ANSWER,

		/** The score answer itself. */
		SCORE_ANSWER,

		/** Any answer, whatever its kind. */
		ANSWER,

		/** The whole response, which is the only target that can carry several answers. */
		RESPONSE,

		/** A record whose components each declare their own question. */
		RECORD,

		/** A type this layer cannot produce. */
		UNSUPPORTED

	}

	private CoercionRules() {
	}

	/**
	 * Classifies a declared type.
	 * @param typeName the binary name, as Jandex and {@code Class.getName()} both report it
	 * ({@code int}, not {@code java.lang.Integer}, for a primitive)
	 * @param enumType whether the type is an enum
	 * @param recordType whether the type is a record
	 * @return what the type can hold
	 */
	public static Target targetOf(String typeName, boolean enumType, boolean recordType) {
		return switch (typeName) {
			case "boolean", "java.lang.Boolean" -> Target.BOOLEAN;
			case "double", "java.lang.Double" -> Target.DOUBLE;
			case "int", "java.lang.Integer", "long", "java.lang.Long", "short", "java.lang.Short", "byte",
					"java.lang.Byte" ->
				Target.INTEGRAL;
			case "java.lang.String" -> Target.STRING;
			case "org.springaicommunity.typesafe.response.NoulAnswer" -> Target.NOUL_ANSWER;
			case "org.springaicommunity.typesafe.response.ChoiceAnswer" -> Target.CHOICE_ANSWER;
			case "org.springaicommunity.typesafe.response.ScoreAnswer" -> Target.SCORE_ANSWER;
			case "org.springaicommunity.typesafe.response.Answer" -> Target.ANSWER;
			case "org.springaicommunity.typesafe.response.SystemOneResponse" -> Target.RESPONSE;
			default -> enumType ? Target.ENUM : (recordType ? Target.RECORD : Target.UNSUPPORTED);
		};
	}

	/**
	 * @param target a declared type
	 * @return whether this layer can produce a value for it at all
	 */
	public static boolean isSupported(Target target) {
		return target != Target.UNSUPPORTED;
	}

	/**
	 * Decides whether a question kind can be answered into a declared type.
	 * @param target the declared type
	 * @param kind the question's kind
	 * @return {@code true} when the combination can work
	 */
	public static boolean isCompatible(Target target, QuestionType kind) {
		return switch (target) {
			case BOOLEAN -> kind == QuestionType.NOUL;
			case DOUBLE -> kind == QuestionType.NOUL || kind == QuestionType.SCORE;
			case INTEGRAL -> kind == QuestionType.SCORE;
			case STRING, ENUM -> kind == QuestionType.SCORE || kind == QuestionType.CHOICE;
			case NOUL_ANSWER -> kind == QuestionType.NOUL;
			case CHOICE_ANSWER -> kind == QuestionType.CHOICE;
			case SCORE_ANSWER -> kind == QuestionType.SCORE;
			case ANSWER, RESPONSE -> true;
			case RECORD, UNSUPPORTED -> false;
		};
	}

	/**
	 * @param target a declared type
	 * @return whether a noul threshold is meaningful for it; declaring one anywhere else is a
	 * mistake rather than a setting, so the build rejects it
	 */
	public static boolean allowsThreshold(Target target) {
		return target == Target.BOOLEAN;
	}

	/**
	 * @param target a declared type
	 * @return the question kinds that can be answered into it, for an error message
	 */
	public static String acceptedKinds(Target target) {
		return switch (target) {
			case BOOLEAN -> "a noul";
			case DOUBLE -> "a noul or a score";
			case INTEGRAL -> "a score";
			case STRING, ENUM -> "a choice or a score";
			case NOUL_ANSWER -> "a noul";
			case CHOICE_ANSWER -> "a choice";
			case SCORE_ANSWER -> "a score";
			case ANSWER -> "any question";
			case RESPONSE -> "any question";
			case RECORD, UNSUPPORTED -> "nothing";
		};
	}

	/**
	 * @param target a declared type
	 * @return a description of the type, for an error message
	 */
	public static String describe(Target target) {
		return switch (target) {
			case BOOLEAN -> "a boolean";
			case DOUBLE -> "a double";
			case INTEGRAL -> "an integral number";
			case STRING -> "a String";
			case ENUM -> "an enum";
			case NOUL_ANSWER -> "a NoulAnswer";
			case CHOICE_ANSWER -> "a ChoiceAnswer";
			case SCORE_ANSWER -> "a ScoreAnswer";
			case ANSWER -> "an Answer";
			case RESPONSE -> "a SystemOneResponse";
			case RECORD -> "a record";
			case UNSUPPORTED -> "an unsupported type";
		};
	}

}
