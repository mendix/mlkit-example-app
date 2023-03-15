package mendixsso.implementation.utils;

import com.mendix.core.Core;
import com.mendix.logging.ILogNode;

import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public final class ThreadingBatchingListProcessor {

    // NOTE: according to formula by Brian Goetz:
    //      number of threads = number of available cores * (1 + wait time / service time)
    // here it is assumed that the "blocking coefficent" (wait time / service time) is close to 1
    // NOTE: this number has to stay below your JDBC connection pool amount
    private static final ExecutorService WORKER_THREAD_POOL =
            Executors.newFixedThreadPool(2 * Runtime.getRuntime().availableProcessors());

    private ThreadingBatchingListProcessor() {
    }

    public static <T> long process(final String logNodeName, final int batchSize,
                                   final BiFunction<Integer, Long, List<T>> getNextBatch,
                                   final Consumer<T> processItem) throws InterruptedException {
        final ILogNode logNode = Core.getLogger(logNodeName);
        final long startTime = new Date().getTime();

        long total = 0;
        int count;

        do {
            final List<T> itemsToProcess = getNextBatch.apply(batchSize, total);
            count = itemsToProcess.size();

            if (count > 0) {
                final CountDownLatch latch = new CountDownLatch(count);

                for (final T item : itemsToProcess) {
                    WORKER_THREAD_POOL.submit(() -> {
                        try {
                            // do work here
                            processItem.accept(item);
                        } finally {
                            latch.countDown();
                        }
                    });
                }

                logNode.trace("Waiting for latch...");
                latch.await();

                total += count;

                logNode.info(String.format("Processed %d items (total: %d).", count, total));
            }
        } while (count == batchSize);

        if (total > 0) {
            logNode.info(String.format("Done, processed all items (took: %d ms, total: %d).",
                    new Date().getTime() - startTime, total));
        }

        return total;
    }

}
