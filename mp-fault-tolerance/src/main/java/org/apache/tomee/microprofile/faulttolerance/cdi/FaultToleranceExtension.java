package org.apache.tomee.microprofile.faulttolerance.cdi;

import org.apache.tomee.microprofile.faulttolerance.retry.RetryManager;
import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.inject.spi.DefinitionException;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessBean;
import javax.enterprise.inject.spi.WithAnnotations;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class FaultToleranceExtension implements Extension {

    private final List<Annotated> beansToValidate = new ArrayList<>();

    /**
     * 1 - Mark beans containing the Fault Tolerance annotations.
     * This system event is fired for every class scanned by the container during boot.
     *
     * @param pat The annotated types.
     */
    public <X> void findFaultTolerantBeans(@Observes @WithAnnotations({
            Retry.class,
            CircuitBreaker.class,
            Timeout.class,
            Bulkhead.class,
            Fallback.class}) final ProcessAnnotatedType<X> pat) {

        if (faultToleranceAnnotations().anyMatch(it -> pat.getAnnotatedType().isAnnotationPresent(it))) {
            // class level
            pat.configureAnnotatedType().add(FaultToleranceEnabled.Literal.INSTANCE);
        } else {
            // methods in class
            final List<Method> methods = pat.getAnnotatedType().getMethods().stream()
                    .filter(method -> faultToleranceAnnotations().anyMatch(method::isAnnotationPresent))
                    .map(AnnotatedMethod::getJavaMember).collect(toList());
            pat.configureAnnotatedType()
                    .filterMethods(method -> methods.contains(method.getJavaMember()))
                    .forEach(m -> m.add(FaultToleranceEnabled.Literal.INSTANCE));
        }
    }

    /**
     * 2 - Check which beans do we need to buildModels and cache.
     *
     * @param bean
     */
    void onBean(@Observes final ProcessBean<?> bean) {
        if (AnnotatedType.class.isInstance(bean.getAnnotated())) {
            final AnnotatedType<?> at = AnnotatedType.class.cast(bean.getAnnotated());
            if (at.getMethods().stream().anyMatch(m -> m.isAnnotationPresent(FaultToleranceEnabled.class))) {
                beansToValidate.add(bean.getAnnotated());
            }
        }
    }

    /**
     * Will build the different models and add deployment problems if found.
     *
     * @param validation
     */
    void buildModels(@Observes final AfterDeploymentValidation validation) {

        beansToValidate.stream()
                .map(this::buildModels)
                .filter(Objects::nonNull)
                .forEach(validation::addDeploymentProblem);
        beansToValidate.clear();

    }

    private Throwable buildModels(final Annotated annotatedBean) {

        final RetryManager retryManager = CDI.current().select(RetryManager.class).get();
        final AnnotatedType<?> retryType = AnnotatedType.class.cast(annotatedBean);

        try {
            retryType.getMethods().stream()
                    .filter(method -> annotatedBean.isAnnotationPresent(Retry.class) ||
                            method.isAnnotationPresent(Retry.class))
                    .forEach(annotatedMethod -> retryManager.createRetryModel(annotatedMethod.getJavaMember()));

            return null;
        } catch (final RuntimeException re) {
            return new DefinitionException(re);
        }
    }

    private Stream<Class<? extends Annotation>> faultToleranceAnnotations() {
        return Stream.of(Asynchronous.class, Bulkhead.class, CircuitBreaker.class,
                Fallback.class, Retry.class, Timeout.class);
    }
}
