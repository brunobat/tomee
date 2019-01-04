package org.apache.tomee.microprofile.faulttolerance.retry;
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

import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class RetryModel {

    private final long delay;

    private final long maxDuration;

    private final int maxRetries;

    private final long jitter;

    private final Class<? extends Throwable>[] abortOn;

    private final Class<? extends Throwable>[] retryOn;

//    private final FaultToleranceMetrics.Counter callsSucceededNotRetried;
//
//    private final FaultToleranceMetrics.Counter callsSucceededRetried;
//
//    private final FaultToleranceMetrics.Counter callsFailed;
//
//    private final FaultToleranceMetrics.Counter retries;

    private final boolean disabled;

    //    private Model(final boolean disabled,
//                  final Retry retry, final FaultToleranceMetrics.Counter callsSucceededNotRetried,
//                  final FaultToleranceMetrics.Counter callsSucceededRetried, final FaultToleranceMetrics.Counter callsFailed,
//                  final FaultToleranceMetrics.Counter retries) {
    public RetryModel(final boolean disabled,
                      final Retry retry) {
        this.disabled = disabled;
        this.abortOn = retry.abortOn();
        this.retryOn = retry.retryOn();
        this.maxDuration = retry.delayUnit().getDuration().toNanos() * retry.maxDuration();
        this.maxRetries = retry.maxRetries();
        this.delay = retry.delayUnit().getDuration().toNanos() * retry.delay();
        this.jitter = retry.jitterDelayUnit().getDuration().toNanos() * retry.jitter();
//        this.callsSucceededNotRetried = callsSucceededNotRetried;
//        this.callsSucceededRetried = callsSucceededRetried;
//        this.callsFailed = callsFailed;
//        this.retries = retries;

        if (maxRetries < 0) {
            throw new FaultToleranceDefinitionException("max retries can't be negative");
        }
        if (delay < 0) {
            throw new FaultToleranceDefinitionException("delay can't be negative");
        }
        if (maxDuration < 0) {
            throw new FaultToleranceDefinitionException("max duration can't be negative");
        }
        if (jitter < 0) {
            throw new FaultToleranceDefinitionException("jitter can't be negative");
        }
        if (delay > maxDuration) {
            throw new FaultToleranceDefinitionException("delay can't be < max duration");
        }
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public boolean abortOn(final Exception re) {
        return matches(abortOn, re);
    }

    public boolean retryOn(final Exception re) {
        return matches(retryOn, re);
    }

    private boolean matches(final Class<? extends Throwable>[] list,
                            final Exception re) {
        return list.length > 0 && Stream.of(list).anyMatch(it -> it.isInstance(re) || it.isInstance(re.getCause()));
    }

    public long nextPause() {
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        return TimeUnit.NANOSECONDS
                .toMillis(min(maxDuration, max(0, ((random.nextBoolean() ? 1 : -1) * delay) + random.nextLong(jitter))));// todo redo formula
    }

    public long getMaxDuration() {
        return maxDuration;
    }
}
