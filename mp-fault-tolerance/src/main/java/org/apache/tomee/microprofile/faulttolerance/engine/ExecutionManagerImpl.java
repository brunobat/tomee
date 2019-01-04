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
import org.apache.tomee.microprofile.faulttolerance.retry.RetryManager;
import org.apache.tomee.microprofile.faulttolerance.retry.RetryModel;
import org.apache.tomee.microprofile.faulttolerance.retry.RetryPlan;

import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.interceptor.InvocationContext;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@ApplicationScoped
public class ExecutionManagerImpl implements ExecutionManager {

    private final MicroprofileAnnotationMapper mapper = MicroprofileAnnotationMapper.getInstance();

    @Inject
    private BulkheadManager bulkheadManager;
    @Inject
    private CircuitBreakerManager circuitBreakerManager;
    @Inject
    private RetryManager retryManager;

    @Resource
    private ManagedScheduledExecutorService executor;

    private final ConcurrentMap<String, ExecutionPlan> executionPlanMap = new ConcurrentHashMap<>();

    public Object execute(final InvocationContext invocationContext) throws Exception {
        Method method = invocationContext.getMethod();
        return getExecutionPlan(method).execute(invocationContext);
    }

    private ExecutionPlan getExecutionPlan(Method method) {
        final String name = MicroprofileAnnotationMapper.createKeyName(method);
        return executionPlanMap.computeIfAbsent(name, key -> {

            RetryModel retryModel = retryManager.getRetryModel(name);
            if (retryModel == null) {
                retryModel = retryManager.createRetryModel(method);
            }
            return new RetryPlan(name, retryModel);
        });
    }

    public RetryManager getRetryManager() {
        return retryManager;
    }
}
