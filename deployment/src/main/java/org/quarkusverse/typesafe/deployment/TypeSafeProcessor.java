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

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.quarkusverse.typesafe.TypeSafeClientProducer;
import org.quarkusverse.typesafe.TypeSafeConfig;
import org.quarkusverse.typesafe.TypeSafeEndpoints;
import org.springaicommunity.typesafe.JsonContent;
import org.springaicommunity.typesafe.question.Choice;
import org.springaicommunity.typesafe.question.Noul;
import org.springaicommunity.typesafe.question.NoulCriteria;
import org.springaicommunity.typesafe.question.Question;
import org.springaicommunity.typesafe.question.QuestionType;
import org.springaicommunity.typesafe.question.Score;
import org.springaicommunity.typesafe.question.SystemOneRequest;
import org.springaicommunity.typesafe.response.Answer;
import org.springaicommunity.typesafe.response.AnswerDeserializer;
import org.springaicommunity.typesafe.response.AnswerType;
import org.springaicommunity.typesafe.response.ChoiceAnswer;
import org.springaicommunity.typesafe.response.ListModelsResponse;
import org.springaicommunity.typesafe.response.ModelMetadata;
import org.springaicommunity.typesafe.response.NoulAnswer;
import org.springaicommunity.typesafe.response.ScoreAnswer;
import org.springaicommunity.typesafe.response.SystemOneResponse;
import org.springaicommunity.typesafe.response.UnknownAnswer;
import org.springaicommunity.typesafe.response.Usage;

/**
 * The build steps of the TypeSafe extension.
 *
 * <p>
 * The extension wires itself up through CDI, so there is no synthetic bean to record here.
 * What the build steps add is what a JVM-only library cannot: the runtime classes are put
 * in the Quarkus index explicitly, so the configuration mapping and the producers are found
 * even in a build that does not treat the extension's runtime jar as an application
 * archive, and the SDK's own JSON model is registered for reflection so the client keeps
 * working in a native image.
 */
public class TypeSafeProcessor {

	/**
	 * The client and the endpoint paths, so ArC sees the bean archive and SmallRye sees the
	 * configuration mapping whatever route the artifact took onto the classpath.
	 * @return the classes to add to the index
	 */
	@BuildStep
	public AdditionalIndexedClassesBuildItem indexTypeSafeRuntimeClasses() {
		return new AdditionalIndexedClassesBuildItem(TypeSafeConfig.class.getName(),
				TypeSafeClientProducer.class.getName(), TypeSafeEndpoints.class.getName());
	}

	/**
	 * The SDK binds its requests and responses with Jackson 3 through Spring's
	 * {@code RestClient}, which is reflection based: without this the client works in JVM
	 * mode and fails to read the first response in a native image.
	 * @return the model classes to register
	 */
	@BuildStep
	public ReflectiveClassBuildItem registerTypeSafeModelClassesForReflection() {
		return ReflectiveClassBuildItem
			.builder(JsonContent.class, Question.class, QuestionType.class, Noul.class, NoulCriteria.class,
					Choice.class, Score.class, SystemOneRequest.class, Answer.class, AnswerDeserializer.class,
					AnswerType.class, ChoiceAnswer.class, NoulAnswer.class, ScoreAnswer.class, UnknownAnswer.class,
					SystemOneResponse.class, ListModelsResponse.class, ModelMetadata.class, Usage.class)
			.methods()
			.fields()
			.constructors()
			.build();
	}

}
