/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.safeguard.impl.bulkhead;

import org.apache.safeguard.api.bulkhead.BulkheadBuilder;
import org.apache.safeguard.api.bulkhead.BulkheadDefinition;

public class BulkheadBuilderImpl implements BulkheadBuilder{
    private final String name;
    private final BulkheadManagerImpl bulkheadManager;
    private int maxWaitingExecutions;
    private int maxConcurrentExecutions;
    private boolean asynchronous = false;

    BulkheadBuilderImpl(String name, BulkheadManagerImpl bulkheadManager) {
        this.name = name;
        this.bulkheadManager = bulkheadManager;
    }

    @Override
    public BulkheadBuilder withMaxConcurrency(int maxConcurrency) {
        this.maxConcurrentExecutions = maxConcurrency;
        return this;
    }

    @Override
    public BulkheadBuilder withMaxWaiting(int overflowCapacity) {
        this.maxWaitingExecutions = overflowCapacity;
        return this;
    }

    @Override
    public BulkheadBuilder asynchronous() {
        this.asynchronous = true;
        return this;
    }

    @Override
    public BulkheadDefinition build() {
        BulkheadDefinitionImpl definition = new BulkheadDefinitionImpl(maxConcurrentExecutions, maxWaitingExecutions, asynchronous);
        bulkheadManager.register(name, definition);
        return definition;

    }
}
