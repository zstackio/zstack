package org.zstack.test.core.asyncbatch;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.asyncbatch.AsyncBatchRunner;
import org.zstack.core.asyncbatch.LoopAsyncBatch;
import org.zstack.core.thread.AsyncThread;
import org.zstack.header.core.FutureCompletion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.test.BeanConstructor;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Collection;
import java.util.List;

import static java.util.Arrays.asList;

public class TestAsyncBatch1 {
    CLogger logger = Utils.getLogger(TestAsyncBatch1.class);
    volatile int count = 0;

    @Before
    public void setUp() throws Exception {
        new BeanConstructor().build();
    }

    @Test
    public void test() {
        FutureCompletion completion = new FutureCompletion(null);

        List<Integer> nums = asList(1, 2, 3);
        new LoopAsyncBatch<Integer>(completion) {
            @Override
            protected Collection<Integer> collect() {
                return nums;
            }

            @Override
            protected AsyncBatchRunner forEach(Integer item) {
                return new AsyncBatchRunner() {
                    @Override
                    @AsyncThread
                    public void run(NoErrorCompletion completion) {
                        count += item;
                        completion.done();
                    }
                };
            }

            @Override
            protected void done() {
                completion.success();
            }
        }.start();

        completion.await();
        Assert.assertEquals(1 + 2 + 3, count);

        count = 0;
        FutureCompletion completion1 = new FutureCompletion(null);
        new LoopAsyncBatch<Integer>(completion1) {
            @Override
            protected Collection<Integer> collect() {
                return nums;
            }

            @Override
            protected AsyncBatchRunner forEach(Integer item) {
                return new AsyncBatchRunner() {
                    @Override
                    @AsyncThread
                    public void run(NoErrorCompletion completion) {
                        if (item == 2) {
                            throw new RuntimeException("on purpose");
                        } else {
                            count += item;
                            completion.done();
                        }
                    }
                };
            }

            @Override
            protected void done() {
                completion1.success();
            }
        }.start();

        completion1.await();
        Assert.assertEquals(1 + 3, count);

        FutureCompletion completion2 = new FutureCompletion(null);
        new LoopAsyncBatch<Integer>(completion2) {
            @Override
            protected Collection<Integer> collect() {
                return nums;
            }

            @Override
            protected AsyncBatchRunner forEach(Integer item) {
                return new AsyncBatchRunner() {
                    @Override
                    public void run(NoErrorCompletion completion) {
                        completion.done();
                    }
                };
            }

            @Override
            protected void done() {
                throw new RuntimeException("on purpose");
            }
        }.start();

        completion2.await();
        Assert.assertFalse(completion2.isSuccess());
    }
}
