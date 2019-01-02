package org.apache.tomee.microprofile.faulttolerance.engine;

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

import org.apache.tomee.microprofile.faulttolerance.bulkhead.BulkheadManager;
import org.apache.tomee.microprofile.faulttolerance.circuitbreaker.CircuitBreakerManager;
import org.apache.tomee.microprofile.faulttolerance.engine.plan.ExecutionPlan;
import org.apache.tomee.microprofile.faulttolerance.retry.RetryManager;
import org.apache.tomee.microprofile.faulttolerance.retry.RetryModel;
import org.apache.tomee.microprofile.faulttolerance.retry.RetryPlan;

import javax.enterprise.inject.Vetoed;
import javax.interceptor.InvocationContext;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;

@Vetoed
public class ExecutionManager {

    private final MicroprofileAnnotationMapper mapper;
    private final BulkheadManager bulkheadManager;
    private final CircuitBreakerManager circuitBreakerManager;
    private final RetryManager retryManager;
    private final ScheduledExecutorService executor;
    private final ConcurrentMap<String, ExecutionPlan> executionPlanMap = new ConcurrentHashMap<>();

    public ExecutionManager(final MicroprofileAnnotationMapper mapper,
                            final BulkheadManager bulkheadManager,
                            final CircuitBreakerManager circuitBreakerManager,
                            final RetryManager retryManager,
                            final ScheduledExecutorService executor) {
        this.mapper = mapper;
        this.bulkheadManager = bulkheadManager;
        this.circuitBreakerManager = circuitBreakerManager;
        this.retryManager = retryManager;
        this.executor = executor;
    }

    public Object execute(final InvocationContext invocationContext) {
        Method method = invocationContext.getMethod();
        return getExecutionPlan(method).execute(invocationContext::proceed, invocationContext);
    }

    private ExecutionPlan getExecutionPlan(Method method) {
        final String name = MicroprofileAnnotationMapper.createKeyName(method);
        return executionPlanMap.computeIfAbsent(name, key -> {

            RetryModel retryModel = retryManager.getRetryModel(name);
            if (retryModel == null) {
                retryModel = retryManager.createRetryModel(name, method);
            }
            return new RetryPlan(name, retryModel);
        });
    }
}
