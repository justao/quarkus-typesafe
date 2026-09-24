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

import java.util.ArrayList;
import java.util.List;

import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.builder.BuildProducer;
import io.quarkus.deployment.DeploymentException;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import jakarta.inject.Singleton;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.quarkusverse.typesafe.service.runtime.TypeSafeServiceRecorder;

/**
 * Turns every {@code @RegisterTypeSafeService} interface in the application into a bean.
 *
 * <p>
 * Three steps, in the order they matter: the annotation becomes a bean defining one, so an
 * interface needs no scope of its own to be found; each interface is checked while the
 * application is built, so a shape that can never be answered fails the build instead of the
 * first call; and each one that passes becomes a synthetic singleton whose value is a proxy
 * the recorder builds at run time.
 *
 * <p>
 * What the proxy needs to exist in a native image is registered here as well: the interface
 * itself for reflection, without which the runtime could not read its annotations, and a
 * proxy definition, without which {@code Proxy.newProxyInstance} has nothing to instantiate.
 * The records a service returns are registered too, because filling one means calling its
 * canonical constructor reflectively.
 */
public class TypeSafeServiceProcessor {

	/**
	 * The scope the service beans are registered under: {@code @Singleton} is a pseudo-scope,
	 * so the bean needs no client proxy and the interface needs no implementation.
	 */
	private static final DotName SINGLETON = DotName.createSimple(Singleton.class.getName());

	/**
	 * Makes {@code @RegisterTypeSafeService} a bean defining annotation, with
	 * {@code @Singleton} as the scope it implies.
	 * @param beanDefiningAnnotations the build item producer
	 */
	@BuildStep
	void beanDefiningAnnotation(BuildProducer<BeanDefiningAnnotationBuildItem> beanDefiningAnnotations) {
		beanDefiningAnnotations
			.produce(new BeanDefiningAnnotationBuildItem(TypeSafeServiceValidator.REGISTER, SINGLETON));
	}

	/**
	 * Checks every annotated interface and reports all of the problems at once, rather than
	 * one per build.
	 * @param index the combined index
	 * @param services the validated interfaces
	 */
	@BuildStep
	void validateServices(CombinedIndexBuildItem index, BuildProducer<TypeSafeServiceBuildItem> services) {
		List<String> problems = new ArrayList<>();
		for (AnnotationInstance registration : index.getIndex().getAnnotations(TypeSafeServiceValidator.REGISTER)) {
			AnnotationTarget target = registration.target();
			if (target.kind() != AnnotationTarget.Kind.CLASS) {
				continue;
			}
			ClassInfo service = target.asClass();
			TypeSafeServiceValidator.Result result = TypeSafeServiceValidator.validate(service, index.getIndex());
			problems.addAll(result.problems());
			if (result.problems().isEmpty()) {
				services.produce(new TypeSafeServiceBuildItem(service.name(), result.reflectionTypes()));
			}
		}
		if (!problems.isEmpty()) {
			String separator = System.lineSeparator() + "  - ";
			throw new DeploymentException(problems.size() + " problem(s) with the @RegisterTypeSafeService interfaces:"
					+ separator + String.join(separator, problems));
		}
	}

	/**
	 * Registers one synthetic singleton per service, plus what the proxy and the records need
	 * in a native image.
	 * @param services the validated interfaces
	 * @param recorder the recorder that creates the proxies
	 * @param beans the synthetic beans
	 * @param proxies the native image proxy definitions
	 * @param reflection the classes to register for reflection
	 */
	@BuildStep
	@Record(ExecutionTime.STATIC_INIT)
	void registerServices(List<TypeSafeServiceBuildItem> services, TypeSafeServiceRecorder recorder,
			BuildProducer<SyntheticBeanBuildItem> beans, BuildProducer<NativeImageProxyDefinitionBuildItem> proxies,
			BuildProducer<ReflectiveClassBuildItem> reflection) {
		for (TypeSafeServiceBuildItem service : services) {
			String name = service.interfaceName().toString();
			Class<?> serviceInterface = load(name);

			beans.produce(SyntheticBeanBuildItem.configure(serviceInterface)
				.scope(Singleton.class)
				.unremovable()
				.addType(serviceInterface)
				.supplier(recorder.typeSafeService(name))
				.done());

			// The proxy the supplier creates has to be known while the image is built.
			proxies.produce(new NativeImageProxyDefinitionBuildItem(name));

			// ... and its annotations are read reflectively when a method is first called.
			reflection.produce(ReflectiveClassBuildItem.builder(name).methods().build());

			for (DotName type : service.reflectionTypes()) {
				reflection.produce(ReflectiveClassBuildItem.builder(type.toString())
					.methods()
					.fields()
					.constructors()
					.build());
			}
		}
	}

	/**
	 * Loads the service interface so the synthetic bean can name it as its type.
	 *
	 * <p>
	 * The deployment class loader sees the application's classes while the application is
	 * built, which is what lets the bean carry the interface as a bean type instead of an
	 * opaque name. Loading it without initialising it keeps this to a class lookup.
	 * @param className the interface's binary name
	 * @return the interface
	 */
	private static Class<?> load(String className) {
		try {
			return Class.forName(className, false, Thread.currentThread().getContextClassLoader());
		}
		catch (ClassNotFoundException ex) {
			throw new DeploymentException("Cannot load the service interface " + className, ex);
		}
	}

}
