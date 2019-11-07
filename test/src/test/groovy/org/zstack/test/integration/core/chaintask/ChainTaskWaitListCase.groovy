package org.zstack.test.integration.core.chaintask

import org.zstack.core.thread.ChainTask
import org.zstack.core.thread.SyncTaskChain
import org.zstack.core.thread.ThreadFacade
import org.zstack.testlib.SubCase

import java.util.concurrent.CountDownLatch
/**
 * Created by mingjian.deng on 2019/10/15.*/
class ChainTaskWaitListCase extends SubCase {
    @Override
    void clean() {
    }

    @Override
    void setup() {
    }

    @Override
    void environment() {
    }

    @Override
    void test() {
        testDefaultTasks()
        testMaxTasks()
        testTwoDedupTasks()
    }

    class LatchTask {
        String name
        def task = false
        def abandon = false
        def latch = new CountDownLatch(1)

        void execute() {
            task = true
            sleep 100
        }

        void lock() {
            task = true
            latch.await()
        }

        void release() {
            latch.countDown()
        }

        void throwed() {
            abandon = true
        }

        LatchTask(name) {
            this.name = name
        }
    }

    // test default running is not changed
    void testDefaultTasks() {
        logger.debug("start test default tasks")
        ThreadFacade thdf = bean(ThreadFacade.class)
        def latches = [new LatchTask("task1"), new LatchTask("task2"), new LatchTask("task3")]

        latches.each {
            thdf.chainSubmit(new ChainTask(null) {
                @Override
                String getSyncSignature() {
                    return "tasks"
                }

                @Override
                void run(SyncTaskChain chain) {
                    it.lock()
                    chain.next()
                }

                @Override
                String getName() {
                    return it.name
                }
            })
        }

        sleep 1000

        latches.each {
            if (it.name == "task1") {
                assert it.task
            } else {
                assert !it.task
            }
        }

        latches.each {
            it.release()
        }
    }

    // test max waiting list
    void testMaxTasks() {
        logger.debug("start test max tasks")
        ThreadFacade thdf = bean(ThreadFacade.class)
        def latches = [new LatchTask("task1"), new LatchTask("task2"), new LatchTask("task3"), new LatchTask("task4")]

        latches.each {
            thdf.chainSubmit(new ChainTask(null) {
                @Override
                String getSyncSignature() {
                    return "tasks"
                }

                @Override
                void run(SyncTaskChain chain) {
                    it.execute()
                    chain.next()
                }

                @Override
                String getName() {
                    return it.name
                }

                @Override
                int getMaxPendingTasks() {
                    return 2
                }

                @Override
                void exceedMaxPendingCallback() {
                    it.throwed()
                }

                @Override
                String getDeduplicateString() {
                    return "max tasks"
                }
            })
            sleep 5
        }

        sleep 1000

        latches.each {
            if (it.name == "task4") {
                assert !it.task
                assert it.abandon
            } else {
                assert it.task
                assert !it.abandon
            }
        }

        // test chain.next still works
        def task = new LatchTask("task5")
        thdf.chainSubmit(new ChainTask(null) {
            @Override
            String getSyncSignature() {
                return "task"
            }

            @Override
            void run(SyncTaskChain chain) {
                task.execute()
                chain.next()
            }

            @Override
            String getName() {
                return task.name
            }

            @Override
            int getMaxPendingTasks() {
                return 2
            }

            @Override
            void exceedMaxPendingCallback() {
                task.throwed()
            }

            @Override
            String getDeduplicateString() {
                return "max tasks"
            }
        })
        sleep 1000
        assert task.task
        assert !task.abandon
    }

    void testTwoDedupTasks() {
        logger.debug("start test two dedup tasks")
        ThreadFacade thdf = bean(ThreadFacade.class)
        def latches1 = [new LatchTask("task1-1"), new LatchTask("task1-2"), new LatchTask("task1-3")]
        def latches2 = [new LatchTask("task2-1"), new LatchTask("task2-2"), new LatchTask("task2-3")]

        latches1.each {
            thdf.chainSubmit(new ChainTask(null) {
                @Override
                String getSyncSignature() {
                    return "tasks"
                }

                @Override
                void run(SyncTaskChain chain) {
                    it.execute()
                    chain.next()
                }

                @Override
                String getName() {
                    return it.name
                }

                @Override
                int getMaxPendingTasks() {
                    return 0
                }

                @Override
                void exceedMaxPendingCallback() {
                    it.throwed()
                }

                @Override
                String getDeduplicateString() {
                    return "latches1 tasks"
                }
            })
            sleep 5
        }

        latches2.each {
            thdf.chainSubmit(new ChainTask(null) {
                @Override
                String getSyncSignature() {
                    return "tasks"
                }

                @Override
                void run(SyncTaskChain chain) {
                    it.execute()
                    chain.next()
                }

                @Override
                String getName() {
                    return it.name
                }

                @Override
                int getMaxPendingTasks() {
                    return 2
                }

                @Override
                void exceedMaxPendingCallback() {
                    it.throwed()
                }

                @Override
                String getDeduplicateString() {
                    return "latches2 tasks"
                }
            })
            sleep 5
        }
/**
 * now
 * running:   task1-1
 * pending:   task2-1, task2-2
 * throwed:   task1-2, task1-3, task2-3
 */
        sleep 1000

        latches1.each {
            if (it.name == "task1-1") {
                assert it.task
                assert !it.abandon
            } else {
                assert !it.task
                assert it.abandon
            }
        }

        latches2.each {
            if (it.name == "task2-3") {
                assert !it.task
                assert it.abandon
            } else {
                assert it.task
                assert !it.abandon
            }
        }
    }
}
