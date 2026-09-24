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

import org.springaicommunity.typesafe.question.Choice;
import org.springaicommunity.typesafe.question.Noul;
import org.springaicommunity.typesafe.question.Question;
import org.springaicommunity.typesafe.question.Score;

/**
 * Builds the SDK question a service method's annotation described.
 *
 * <p>
 * This is the only class in the runtime that touches the question builders, and it does so
 * under the SDK's own types rather than the annotations — the annotations in
 * {@code org.quarkusverse.typesafe.service} collide by simple name with {@code Noul},
 * {@code Choice} and {@code Score}, so keeping the two apart per class is what makes the
 * collision a non-issue here.
 */
final class ServiceQuestionFactory {

	private ServiceQuestionFactory() {
	}

	/**
	 * @param template what the annotation declared
	 * @return the question to send
	 */
	static Question toQuestion(ServiceMethodPlan.QuestionTemplate template) {
		return switch (template.kind()) {
			case NOUL -> {
				Noul.Builder builder = Noul.builder();
				if (!template.instructions().isEmpty()) {
					builder.instructions(template.instructions());
				}
				if (!template.whenTrue().isEmpty()) {
					builder.whenTrue(template.whenTrue());
				}
				if (!template.whenFalse().isEmpty()) {
					builder.whenFalse(template.whenFalse());
				}
				yield builder.build();
			}
			case CHOICE -> {
				Choice.Builder builder = Choice.builder();
				if (!template.instructions().isEmpty()) {
					builder.instructions(template.instructions());
				}
				for (ServiceMethodPlan.OptionTemplate option : template.options()) {
					if (option.criteria().isEmpty()) {
						builder.option(option.name());
					}
					else {
						builder.option(option.name(), option.criteria());
					}
				}
				yield builder.build();
			}
			case SCORE -> {
				Score.Builder builder = Score.builder();
				if (!template.instructions().isEmpty()) {
					builder.instructions(template.instructions());
				}
				for (String level : template.levels()) {
					builder.level(level);
				}
				yield builder.build();
			}
		};
	}

}
