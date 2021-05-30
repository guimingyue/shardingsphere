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

package org.apache.shardingsphere.infra.executor.exec;

import com.google.common.collect.Iterables;
import org.apache.calcite.rex.RexDynamicParam;
import org.apache.calcite.rex.RexLiteral;
import org.apache.calcite.rex.RexNode;
import org.apache.shardingsphere.infra.executor.exec.meta.Row;
import org.apache.shardingsphere.infra.executor.exec.tool.RowComparatorUtil;
import org.apache.shardingsphere.infra.executor.sql.execute.result.query.QueryResultMetaData;
import org.apache.shardingsphere.infra.optimizer.rel.physical.SSMergeSort;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Executor with multi input Executors and read rows from these Executors useing merge sort algorithm.
 */
public final class MergeSortExecutor extends AbstractExecutor {
    
    private List<Executor> executors;
    
    private long offset;
    
    private long fetch;
    
    private Comparator<Row> ordering;
    
    private Iterator<Row> mergeSortIterator;
    
    private long count;
    
    public MergeSortExecutor(final ExecContext execContext, final List<Executor> executors, final Comparator<Row> ordering,
                             final int offset, final int fetch) {
        super(execContext);
        this.executors = executors;
        this.ordering = ordering;
        this.offset = offset;
        this.fetch = fetch;
    }
    
    @Override
    protected void executeInit() {
        executors.forEach(Executor::init);
        this.mergeSortIterator = Iterables.mergeSorted(executors, ordering).iterator();
        
        for (int i = 0; i < offset; i++) {
            if (!executeMove()) {
                break;
            }
        }
        
    }
    
    @Override
    public boolean executeMove() {
        if (count > fetch) {
            return false;
        }
        if (mergeSortIterator.hasNext()) {
            count++;
            replaceCurrent(mergeSortIterator.next());
            return true;
        } else {
            return false;
        }
    }
    
    @Override
    public QueryResultMetaData getMetaData() {
        return executors.get(0).getMetaData();
    }
    
    /**
     * Build an <code>Executor</code> instance using merge sort algorithm. 
     * @param rel <code>SSMergeSort</code> rational operator
     * @param executorBuilder see {@link ExecutorBuilder}
     * @return <code>MergeSortExecutor</code>
     */
    public static Executor build(final SSMergeSort rel, final ExecutorBuilder executorBuilder) {
        Executor executor = executorBuilder.build(rel.getInput());
        if (executor instanceof MultiExecutor) {
            ExecContext execContext = executorBuilder.getExecContext();
            Comparator<Row> ordering = RowComparatorUtil.convertCollationToRowComparator(rel.collation);
            int offset = resolveRexNodeValue(rel.offset, 0, execContext.getParameters(), Integer.class);
            int fetch = resolveRexNodeValue(rel.fetch, Integer.MAX_VALUE, execContext.getParameters(), Integer.class);
            return new MergeSortExecutor(execContext, ((MultiExecutor) executor).getExecutors(), ordering, offset, fetch);
        }
        return executor;
    }
    
    private static <T> T resolveRexNodeValue(final RexNode rexNode, final T defaultValue, final List<Object> parameters,
                                             final Class<T> clazz) {
        if (rexNode == null) {
            return defaultValue;
        }
        if (rexNode instanceof RexLiteral) {
            return ((RexLiteral) rexNode).getValueAs(clazz);
        } else if (rexNode instanceof RexDynamicParam) {
            RexDynamicParam rexDynamicParam = (RexDynamicParam) rexNode;
            int idx = rexDynamicParam.getIndex();
            Object val = parameters.get(idx);
            // TODO using a Data type conveter
            return clazz.cast(val);
        }
        return defaultValue;
    }
    
    @Override
    public void close() {
        executors.forEach(Executor::close);
    }
}
