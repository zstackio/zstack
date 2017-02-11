package org.zstack.test.core.asyncbatch;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.asyncbatch.AsyncBatch;
import org.zstack.core.asyncbatch.AsyncBatchRunner;
import org.zstack.core.thread.AsyncThread;
import org.zstack.header.core.FutureCompletion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.test.BeanConstructor;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class TestAsyncBatch {
    CLogger logger = Utils.getLogger(TestAsyncBatch.class);
    volatile int count = 0;
    int num = 10;

    @Before
    public void setUp() throws Exception {
        new BeanConstructor().build();
    }

    @Test
    public void test() {
        FutureCompletion completion = new FutureCompletion(null);

        new AsyncBatch(null) {
            @Override
            protected void setup() {
                for (int i = 0; i < num; i++) {
                    batch(new AsyncBatchRunner() {
                        @Override
                        @AsyncThread
                        public void run(NoErrorCompletion completion) {
                            count++;
                            completion.done();
                        }
                    });
                }
            }

            @Override
            protected void done() {
                completion.success();
            }
        }.start();

        completion.await();
        Assert.assertEquals(num, count);

        count = 0;

        FutureCompletion completion1 = new FutureCompletion(null);
        new AsyncBatch(null) {
            @Override
            protected void setup() {
                for (int i = 0; i < num; i++) {
                    batch(new AsyncBatchRunner() {
                        @Override
                        public void run(NoErrorCompletion completion) {
                            count++;
                            if (count % 2 == 0) {
                                throw new RuntimeException("on purpose");
                            } else {
                                completion.done();
                            }
                        }
                    });
                }
            }

            @Override
            protected void done() {
                completion1.success();
            }
        }.start();

        completion1.await();
        Assert.assertTrue(completion1.isSuccess());

        FutureCompletion completion2 = new FutureCompletion(null);

        new AsyncBatch(completion2) {
            @Override
            protected void setup() {
                for (int i = 0; i < num; i++) {
                    batch(new AsyncBatchRunner() {
                        @Override
                        public void run(NoErrorCompletion completion) {
                            completion.done();
                        }
                    });
                }
            }

            @Override
            protected void done() {
                throw new RuntimeException("on purpose");
            }
        }.start();

        completion.await();
        Assert.assertFalse(completion2.isSuccess());
    }
}
