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

package org.quarkusverse.typesafe.it.service;

import java.util.Map;

import org.quarkusverse.typesafe.service.Choice;
import org.quarkusverse.typesafe.service.Model;
import org.quarkusverse.typesafe.service.Noul;
import org.quarkusverse.typesafe.service.Option;
import org.quarkusverse.typesafe.service.Questions;
import org.quarkusverse.typesafe.service.RegisterTypeSafeService;
import org.quarkusverse.typesafe.service.Score;
import org.springaicommunity.typesafe.question.Question;
import org.springaicommunity.typesafe.response.NoulAnswer;
import org.springaicommunity.typesafe.response.SystemOneResponse;

/**
 * Every shape the declarative layer supports, in one interface — the fixture the service
 * tests answer against.
 *
 * <p>
 * Note which types this file imports: the question annotations, and the SDK's
 * {@code Question}, {@code NoulAnswer} and {@code SystemOneResponse} — but not the SDK's
 * {@code Noul}, {@code Choice} or {@code Score} classes, which collide by simple name with
 * the annotations. A file that needs both has to spell one of them out.
 */
@RegisterTypeSafeService
public interface TriageService {

	/**
	 * @param message the content to evaluate
	 * @return whether the message conveys urgency, at the default threshold
	 */
	@Noul("Does this convey urgency?")
	boolean urgent(String message);

	/**
	 * @param message the content to evaluate
	 * @return whether a refund is being asked for, at a threshold of {@code 0.8}
	 */
	@Noul(value = "Is the customer asking for money back?", name = "refund", threshold = 0.8d)
	boolean refundRequested(String message);

	/**
	 * @param message the content to evaluate
	 * @return the truth value itself, not a thresholded boolean
	 */
	@Noul("Is this a sentence?")
	double sentenceProbability(String message);

	/**
	 * @param message the content to evaluate
	 * @return the selected option's label
	 */
	@Choice(value = "Which team should handle this?",
			options = { @Option(name = "billing", criteria = "Payments, invoicing, refunds"),
					@Option(name = "technical", criteria = "Bugs, outages, integrations") })
	String department(String message);

	/**
	 * @param message the content to evaluate
	 * @return the option as an enum constant, matched by name
	 */
	@Choice(value = "Which team should handle this?", name = "team",
			options = { @Option(name = "BILLING"), @Option(name = "TECHNICAL"), @Option(name = "SALES") })
	Department departmentAsEnum(String message);

	/**
	 * @param message the content to evaluate
	 * @return the level carrying the most probability
	 */
	@Score(value = "How frustrated is the customer?", levels = { "Calm", "Frustrated", "Very angry" })
	int frustration(String message);

	/**
	 * @param message the content to evaluate
	 * @return the level as an enum constant, by index
	 */
	@Score(value = "How frustrated is the customer?", name = "frustrationLevel",
			levels = { "Calm", "Frustrated", "Very angry" })
	Frustration frustrationAsEnum(String message);

	/**
	 * @param message the content to evaluate
	 * @return the nearest level's description
	 */
	@Score(value = "How frustrated is the customer?", name = "frustrationLabel",
			levels = { "Calm", "Frustrated", "Very angry" })
	String frustrationLabel(String message);

	/**
	 * @param message the content to evaluate
	 * @return a score over a rubric longer than the enum, which only the call can detect
	 */
	@Score(value = "How bad is it?", name = "severity", levels = { "Calm", "Frustrated", "Very angry" })
	Severity severity(String message);

	/**
	 * @param message the content to evaluate
	 * @return the answer itself, with its value and kind
	 */
	@Noul("Does this convey urgency?")
	NoulAnswer urgencyAnswer(String message);

	/**
	 * @param message the content to evaluate
	 * @return the whole response, for a method that wants the usage and the request id too
	 */
	@Noul("Does this convey urgency?")
	@Choice(value = "Which team should handle this?", name = "team",
			options = { @Option(name = "billing"), @Option(name = "technical") })
	SystemOneResponse everything(String message);

	/**
	 * @param message the content to evaluate
	 * @return three answers from one call
	 */
	TriageReport report(String message);

	/**
	 * @param message the content to evaluate
	 * @param extra questions the annotations cannot express, merged into the declared ones
	 * @return the whole response
	 */
	@Noul("Does this convey urgency?")
	SystemOneResponse withExtraQuestions(String message, @Questions Map<String, ? extends Question> extra);

	/**
	 * @param message the content to evaluate
	 * @return the truth value, answered by a pinned model rather than the configured alias
	 */
	@Model("jev-1.13.0")
	@Noul("Is this a sentence?")
	double pinnedModel(String message);

	/**
	 * @param sender who wrote it
	 * @param message what they wrote
	 * @return whether the message conveys urgency, with the two parts sent as a named state
	 */
	@Noul("Does this convey urgency?")
	boolean twoPartState(String sender, String message);

	/**
	 * Never sent to the API: a default method is the interface's own code.
	 * @return a description of the service
	 */
	default String describe() {
		return "triage";
	}

}
