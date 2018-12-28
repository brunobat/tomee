package org.apache.tomee.microprofile.faulttolerance.cdi;

import org.apache.tomee.microprofile.faulttolerance.bulkhead.BulkheadManager;
import org.apache.tomee.microprofile.faulttolerance.circuitbreaker.CircuitBreakerManager;
import org.apache.tomee.microprofile.faulttolerance.engine.ExecutionManager;
import org.apache.tomee.microprofile.faulttolerance.engine.ExecutionPlanFactory;
import org.apache.tomee.microprofile.faulttolerance.engine.MicroprofileAnnotationMapper;
import org.apache.tomee.microprofile.faulttolerance.retry.RetryManager;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Specializes;
import javax.naming.InitialContext;
import java.util.concurrent.ScheduledExecutorService;

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
@ApplicationScoped
public class ExecutionManagerProvider {

    @Produces
    @ApplicationScoped
    public ExecutionManager createExecutionManager() throws Exception {

        ScheduledExecutorService executor = new InitialContext().doLookup(
                System.getProperty("apache.safeguard.executorservice.location",
                        "java:comp/DefaultManagedScheduledExecutorService"));

        final MicroprofileAnnotationMapper mapper = MicroprofileAnnotationMapper.getInstance();
        final BulkheadManager bulkheadManager = new BulkheadManager();
        final CircuitBreakerManager circuitBreakerManager = new CircuitBreakerManager();
        final RetryManager retryManager = new RetryManager();

        return new ExecutionManager(
                mapper,
                bulkheadManager,
                circuitBreakerManager,
                retryManager,
                new ExecutionPlanFactory(circuitBreakerManager, retryManager, bulkheadManager, mapper, executor),
                executor);
    }
}
