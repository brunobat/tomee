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

import org.apache.tomee.microprofile.faulttolerance.retry.RetryModel;
import org.eclipse.microprofile.faulttolerance.Retry;

import javax.enterprise.inject.Vetoed;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

@Vetoed
public class MicroprofileAnnotationMapper {

    private static final MicroprofileAnnotationMapper INSTANCE = new MicroprofileAnnotationMapper();

    public static MicroprofileAnnotationMapper getInstance() {
        return INSTANCE;
    }

    private static <T extends Annotation> T getAnnotation(final Method method,
                                                          final Class<T> clazz) {
        T annotation = method.getAnnotation(clazz);
        if (annotation != null) {
            return annotation;
        } else {
            return method.getDeclaringClass().getAnnotation(clazz);
        }
    }

    public static String createKeyName(final Method method) {
        return method.getDeclaringClass().getCanonicalName() + "." + method.getName();
    }

    public RetryModel mapRetry(final Method method) {
        Retry retry = getAnnotation(method, Retry.class);
        if (retry == null) {
            return null;
        }

        return new RetryModel(false, retry);
    }


}
