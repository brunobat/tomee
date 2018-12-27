package org.apache.tomee.microprofile.faulttolerance.cdi;

import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

import javax.annotation.Priority;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import java.lang.reflect.Method;

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

@Interceptor
@FaultToleranceEnabled
@Priority(1)
@Dependent
public class FaultToleranceInterceptor {

    @Inject
    private ExecutionManager executionManager;

    @AroundInvoke
    public Object runSafeguards(InvocationContext invocationContext) throws Exception {
        if (isMethodSafeguarded(invocationContext.getMethod())) {
            return executionManager.execute(invocationContext);
        } else {
            return invocationContext.proceed();
        }
    }

    private boolean isMethodSafeguarded(Method method) {
        return AnnotationUtil.getAnnotation(method, Retry.class) != null ||
                AnnotationUtil.getAnnotation(method, CircuitBreaker.class) != null ||
                AnnotationUtil.getAnnotation(method, Timeout.class) != null ||
                AnnotationUtil.getAnnotation(method, Fallback.class) != null ||
                AnnotationUtil.getAnnotation(method, Bulkhead.class) != null;
    }
}
