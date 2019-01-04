package org.apache.tomee.microprofile.faulttolerance.retry;

import org.apache.tomee.microprofile.faulttolerance.engine.ExecutionPlan;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceException;

import javax.interceptor.InvocationContext;
import java.util.concurrent.TimeUnit;

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


public class RetryPlan implements ExecutionPlan {


    private final String methodName;
    private final RetryModel retryModel;
    private final RetryState retryState;

    public RetryPlan(final String name,
                     final RetryModel retryModel) {

        final long now = System.nanoTime();
        this.methodName = name;
        this.retryModel = retryModel;
        this.retryState = new RetryState(0, now, now + retryModel.getMaxDuration());
    }

    @Override
    public Object execute(final InvocationContext invocationContext) throws Exception {// TODO this is the sync impl

        while (retryState.retryCount <= retryModel.getMaxRetries()) {
            try {
                final Object proceed = invocationContext.proceed();
                return proceed;
            } catch (final Exception e) {

                if (retryModel.abortOn(e) ||
                        retryState.retryCount >= retryModel.getMaxRetries() ||
                        System.nanoTime() >= retryState.endTime) {
                    throw e;
                }
                if (!retryModel.retryOn(e)) {
                    throw e;
                }
                retryState.retryCount++;
                if (retryState.retryCount > retryModel.getMaxRetries()) {
                    throw e;
                }
                TimeUnit.MILLISECONDS.sleep(retryModel.nextPause());
            }
        }
        throw new FaultToleranceException("Inaccessible normally, here for compilation");
    }

    private static class RetryState {
        private final long startTime;
        private final long endTime;
        private int retryCount;

        public RetryState(final int retryCount,
                          final long startTime,
                          final long endTime) {
            this.retryCount = retryCount;
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }
}
