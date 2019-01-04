package org.apache.tomee.microprofile.faulttolerance.retry;

import org.apache.tomee.microprofile.faulttolerance.engine.MicroprofileAnnotationMapper;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Vetoed;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.isNull;

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


/**
 * This will cache Retry model definitions by method
 */
@ApplicationScoped
public class RetryManager {

    private final Map<String, RetryModel> retries = new ConcurrentHashMap<>();
    private final MicroprofileAnnotationMapper mapper = MicroprofileAnnotationMapper.getInstance();

    public RetryModel getRetryModel(final String name) {
        return retries.get(name);
    }

    void addRetryModel(final String name,
                       final RetryModel retryModel) {
        retries.put(name, retryModel);
    }

    public RetryModel createRetryModel(final Method method) {

        final RetryModel retryModel = mapper.mapRetry(method);
        if (isNull(retryModel)) {
            return null;
        }
        retries.put(mapper.createKeyName(method), retryModel);
        return retryModel;
    }
}
