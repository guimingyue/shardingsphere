package org.apache.shardingsphere.infra.executor.exec;

import org.apache.shardingsphere.infra.executor.exec.meta.Row;

import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;

/**
 * Executor that keep only top n elements in the heap.
 */
public class TopNExecutor extends LimitSortExecutor {
    
    private final PriorityQueue<Row> heap;
    
    public TopNExecutor(final Executor executor, final Comparator<Row> ordering, final int offset, final int fetch,
                        final ExecContext execContext) {
        super(executor, ordering, offset, fetch, execContext);
        heap = new PriorityQueue<>(fetch, ordering);
    }
    
    /**
     * initialize input interator.
     * @return initialized iterator.
     */
    @Override
    protected final Iterator<Row> initInputRowIterator() {
        while (getExecutor().moveNext()) {
            if (heap.size() > (getFetch() + getOffset())) {
                heap.poll();
            }
            Row row = getExecutor().current();
            heap.add(row);
        }
        Iterator<Row> inputRowIterator = new Iterator<Row>() {
            @Override
            public boolean hasNext() {
                return heap.size() > 0;
            }
    
            @Override
            public Row next() {
                return heap.poll();
            }
        };
        skipOffsetRows(inputRowIterator);
        return inputRowIterator;
    }
}
