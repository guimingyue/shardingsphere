/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.infra.optimizer.statistics;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * Statistics for a logical table.
 */
@Getter
@Setter
public final class TableStatistics {

    private final String tableName;

    private long rowCount;

    private Map<String, Long> columnCardinality;

    public TableStatistics(final String tableName) {
        this.tableName = tableName;
        columnCardinality = new HashMap<>();
    }

    /**
     * Get column cardinality.
     * @param columnName column name
     * @return column cardinality, or 0 if column cardinality not exist
     */
    public long getCardinality(final String columnName) {
        return columnCardinality.getOrDefault(columnName, 0L);
    }
}