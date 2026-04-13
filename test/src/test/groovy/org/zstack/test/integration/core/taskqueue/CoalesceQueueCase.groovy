package org.zstack.test.integration.core.chaintask

import org.zstack.core.thread.CoalesceQueue
import org.zstack.core.thread.ReturnValueCoalesceQueue
import org.zstack.header.core.Completion
import org.zstack.header.core.ReturnValueCompletion
import org.zstack.header.errorcode.ErrorCode
import org.zstack.testlib.core.FailCoalesceQueue
import org.zstack.testlib.core.ThrowOnSuccessCompletion
import org.zstack.testlib.core.ThrowOnFailCompletion
import org.zstack.testlib.core.FailReturnValueCoalesceQueue
import org.zstack.testlib.core.ThrowOnSuccessReturnValueCompletion
import org.zstack.testlib.core.ThrowOnFailReturnValueCompletion
import org.zstack.testlib.SubCase

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class CoalesceQueueCase extends SubCase {
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
        testCoalesceMultipleRequests()
        testDifferentSignaturesNotCoalesced()
        testBatchFailureNotifiesAllRequests()
        testBatchThrowExceptionNotifiesAllRequests()
        testReturnValueCompletion()
        testResultCalculationFailure()
        testSequentialBatches()
        testHighVolumeNoLossAcrossBatches()
        testCompletionSuccessThrowDoesNotBlockChain()
        testCompletionFailThrowDoesNotBlockChain()
        testRvExecuteBatchThrowDoesNotBlockChain()
        testRvCompletionSuccessThrowDoesNotBlockChain()
        testRvCompletionFailThrowDoesNotBlockChain()
        testCalculateResultFailDoesNotBlockChain()
    }

    void testCoalesceMultipleRequests() {
        def requestCount = 10
        def completionLatch = new CountDownLatch(requestCount)
        def batchExecutionCount = new AtomicInteger(0)
        def processedItems = Collections.synchronizedList(new ArrayList<Integer>())
        def completedTokens = Collections.synchronizedSet(new LinkedHashSet<String>())

        def queue = new CoalesceQueue<Integer>() {
            @Override
            protected String getName() {
                return "test-coalesce"
            }

            @Override
            protected void executeBatch(List<Integer> items, Completion completion) {
                batchExecutionCount.incrementAndGet()
                processedItems.addAll(items)

                new Thread({
                    try {
                        TimeUnit.MILLISECONDS.sleep(100)
                    } catch (InterruptedException ignored) {
                    }
                    completion.success()
                }).start()
            }
        }

        def signature = "host-1"
        (0..<requestCount).each { idx ->
            def token = "done-${idx}"
            queue.submit(signature, idx, new Completion(null) {
                @Override
                void success() {
                    completedTokens.add(token)
                    completionLatch.countDown()
                }

                @Override
                void fail(ErrorCode errorCode) {
                    completedTokens.add(token)
                    completionLatch.countDown()
                }
            })
        }

        assert completionLatch.await(10, TimeUnit.SECONDS)
        assert processedItems.size() == requestCount
        assert batchExecutionCount.get() < requestCount
        assert completedTokens.size() == requestCount
        (0..<requestCount).each { idx ->
            assert completedTokens.contains("done-${idx}")
        }
    }

    void testDifferentSignaturesNotCoalesced() {
        def signaturesCount = 3
        def requestsPerSignature = 5
        def totalRequests = signaturesCount * requestsPerSignature
        def completionLatch = new CountDownLatch(totalRequests)
        def batchExecutionCount = new AtomicInteger(0)
        def completedTokens = Collections.synchronizedSet(new LinkedHashSet<String>())

        def queue = new CoalesceQueue<String>() {
            @Override
            protected String getName() {
                return "test-multi-sig"
            }

            @Override
            protected void executeBatch(List<String> items, Completion completion) {
                batchExecutionCount.incrementAndGet()
                completion.success()
            }
        }

        (0..<signaturesCount).each { sig ->
            def signature = "host-${sig}"
            (0..<requestsPerSignature).each { idx ->
                def item = "${signature}-item-${idx}"
                def token = "done-${item}"
                queue.submit(signature, item, new Completion(null) {
                    @Override
                    void success() {
                        completedTokens.add(token)
                        completionLatch.countDown()
                    }

                    @Override
                    void fail(ErrorCode errorCode) {
                        completedTokens.add(token)
                        completionLatch.countDown()
                    }
                })
            }
        }

        assert completionLatch.await(10, TimeUnit.SECONDS)
        assert batchExecutionCount.get() >= signaturesCount
        assert completedTokens.size() == totalRequests
        (0..<signaturesCount).each { sig ->
            def signature = "host-${sig}"
            (0..<requestsPerSignature).each { idx ->
                assert completedTokens.contains("done-${signature}-item-${idx}")
            }
        }
    }

    void testBatchFailureNotifiesAllRequests() {
        def requestCount = 5
        def completionLatch = new CountDownLatch(requestCount)
        def failureCount = new AtomicInteger(0)
        def testError = org.zstack.core.Platform.operr(org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_CORE_THREAD_10004, "test error")
        def completedTokens = Collections.synchronizedSet(new LinkedHashSet<String>())

        def queue = new CoalesceQueue<Integer>() {
            @Override
            protected String getName() {
                return "test-failure"
            }

            @Override
            protected void executeBatch(List<Integer> items, Completion completion) {
                completion.fail(testError)
            }
        }

        def signature = "host-fail"
        (0..<requestCount).each { idx ->
            def token = "fail-${idx}"
            queue.submit(signature, idx, new Completion(null) {
                @Override
                void success() {
                    completedTokens.add(token)
                    completionLatch.countDown()
                }

                @Override
                void fail(ErrorCode errorCode) {
                    failureCount.incrementAndGet()
                    completedTokens.add(token)
                    completionLatch.countDown()
                }
            })
        }

        assert completionLatch.await(10, TimeUnit.SECONDS)
        assert failureCount.get() == requestCount
        assert completedTokens.size() == requestCount
        (0..<requestCount).each { idx ->
            assert completedTokens.contains("fail-${idx}")
        }
    }

    void testBatchThrowExceptionNotifiesAllRequests() {
        def requestCount = 5
        def completionLatch = new CountDownLatch(requestCount)
        def failureCount = new AtomicInteger(0)
        def completedTokens = Collections.synchronizedSet(new LinkedHashSet<String>())

        def queue = new FailCoalesceQueue()

        def signature = "host-throw"
        (0..<requestCount).each { idx ->
            def token = "throw-${idx}"
            queue.submit(signature, idx, new Completion(null) {
                @Override
                void success() {
                    completedTokens.add(token)
                    completionLatch.countDown()
                }

                @Override
                void fail(ErrorCode errorCode) {
                    failureCount.incrementAndGet()
                    completedTokens.add(token)
                    completionLatch.countDown()
                }
            })
        }

        assert completionLatch.await(10, TimeUnit.SECONDS)
        assert failureCount.get() == requestCount
        assert completedTokens.size() == requestCount
        (0..<requestCount).each { idx ->
            assert completedTokens.contains("throw-${idx}")
        }
    }


    void testReturnValueCompletion() {
        def requestCount = 5
        def completionLatch = new CountDownLatch(requestCount)
        def receivedResults = Collections.synchronizedMap(new LinkedHashMap<Integer, String>())
        def mismatches = Collections.synchronizedList(new ArrayList<String>())
        def batchResult = "batch-success"

        def queue = new ReturnValueCoalesceQueue<Integer, String, String>() {
            @Override
            protected String getName() {
                return "test-return-value"
            }

            @Override
            protected void executeBatch(List<Integer> items, ReturnValueCompletion<String> completion) {
                completion.success(batchResult)
            }

            @Override
            protected String calculateResult(Integer item, String r) {
                return "${r}-item-${item}"
            }
        }

        def signature = "host-result"
        (0..<requestCount).each { idx ->
            queue.submit(signature, idx, new ReturnValueCompletion<String>(null) {
                @Override
                void success(String result) {
                    def expected = String.format("%s-item-%s", batchResult, idx)
                    if (result != expected) {
                        mismatches.add(String.format("item-%s=%s", idx, result))
                    }
                    receivedResults.put(idx, result)
                    completionLatch.countDown()
                }

                @Override
                void fail(ErrorCode errorCode) {
                    completionLatch.countDown()
                }
            })
        }

        assert completionLatch.await(10, TimeUnit.SECONDS)
        assert receivedResults.size() == requestCount
        assert mismatches.isEmpty()
        (0..<requestCount).each { idx ->
            def expected = String.format("%s-item-%s", batchResult, idx)
            assert receivedResults.get(idx) == expected
        }
    }

    void testResultCalculationFailure() {
        def completionLatch = new CountDownLatch(2)
        def successCount = new AtomicInteger(0)
        def failCount = new AtomicInteger(0)

        def queue = new ReturnValueCoalesceQueue<Integer, Void, String>() {
            @Override
            protected String getName() {
                return "test-calc-fail"
            }

            @Override
            protected void executeBatch(List<Integer> items, ReturnValueCompletion<Void> completion) {
                completion.success(null)
            }

            @Override
            protected String calculateResult(Integer item, Void batchResult) {
                if (item == 0) {
                    throw new RuntimeException("Calculation failed for item 0 (on purpose)")
                }
                return "success"
            }
        }

        def signature = "host-calc"
        queue.submit(signature, 0, new ReturnValueCompletion<String>(null) {
            @Override
            void success(String ret) {
                successCount.incrementAndGet()
                completionLatch.countDown()
            }

            @Override
            void fail(ErrorCode errorCode) {
                failCount.incrementAndGet()
                completionLatch.countDown()
            }
        })

        queue.submit(signature, 1, new ReturnValueCompletion<String>(null) {
            @Override
            void success(String ret) {
                successCount.incrementAndGet()
                completionLatch.countDown()
            }

            @Override
            void fail(ErrorCode errorCode) {
                failCount.incrementAndGet()
                completionLatch.countDown()
            }
        })

        assert completionLatch.await(10, TimeUnit.SECONDS)
        assert successCount.get() == 1
        assert failCount.get() == 1
    }

    void testSequentialBatches() {
        def firstBatchStart = new CountDownLatch(1)
        def firstBatchContinue = new CountDownLatch(1)
        def secondBatchStart = new CountDownLatch(1)
        def secondBatchContinue = new CountDownLatch(1)
        def allComplete = new CountDownLatch(6)
        def batches = Collections.synchronizedList(new ArrayList<List<Integer>>())

        def queue = new CoalesceQueue<Integer>() {
            @Override
            protected String getName() {
                return "test-sequential"
            }

            @Override
            protected void executeBatch(List<Integer> items, Completion completion) {
                batches.add(new ArrayList<>(items))

                if (batches.size() == 1) {
                    firstBatchStart.countDown()
                    try {
                        firstBatchContinue.await(5, TimeUnit.SECONDS)
                    } catch (InterruptedException ignored) {
                    }
                } else if (batches.size() == 2) {
                    secondBatchStart.countDown()
                    try {
                        secondBatchContinue.await(5, TimeUnit.SECONDS)
                    } catch (InterruptedException ignored) {
                    }
                }

                completion.success()
            }
        }

        def signature = "host-seq"
        queue.submit(signature, 0, new Completion(null) {
            @Override
            void success() {
                allComplete.countDown()
            }

            @Override
            void fail(ErrorCode errorCode) {
                allComplete.countDown()
            }
        })

        assert firstBatchStart.await(5, TimeUnit.SECONDS)

        (1..<4).each { idx ->
            queue.submit(signature, idx, new Completion(null) {
                @Override
                void success() {
                    allComplete.countDown()
                }

                @Override
                void fail(ErrorCode errorCode) {
                    allComplete.countDown()
                }
            })
        }

        // release first batch so chain.next() fires and second batch can start
        firstBatchContinue.countDown()
        assert secondBatchStart.await(5, TimeUnit.SECONDS)

        // submit more items while second batch is blocked on secondBatchContinue
        (4..<6).each { idx ->
            queue.submit(signature, idx, new Completion(null) {
                @Override
                void success() {
                    allComplete.countDown()
                }

                @Override
                void fail(ErrorCode errorCode) {
                    allComplete.countDown()
                }
            })
        }

        secondBatchContinue.countDown()
        assert allComplete.await(10, TimeUnit.SECONDS)
        assert batches.size() == 3
        assert batches.get(0) == [0]
        assert batches.get(1).containsAll([1, 2, 3])
        assert batches.get(2).containsAll([4, 5])
    }

    void testHighVolumeNoLossAcrossBatches() {
        def requestCount = 300
        def completionLatch = new CountDownLatch(requestCount)
        def processedItems = Collections.synchronizedSet(new LinkedHashSet<Integer>())
        def batchCount = new AtomicInteger(0)

        def queue = new CoalesceQueue<Integer>() {
            @Override
            protected String getName() {
                return "test-high-volume"
            }

            @Override
            protected void executeBatch(List<Integer> items, Completion completion) {
                batchCount.incrementAndGet()
                processedItems.addAll(items)

                new Thread({
                    try {
                        TimeUnit.MILLISECONDS.sleep(3)
                    } catch (InterruptedException ignored) {
                    }
                    completion.success()
                }).start()
            }
        }

        def signature = "host-high-volume"
        (0..<requestCount).each { idx ->
            queue.submit(signature, idx, new Completion(null) {
                @Override
                void success() {
                    completionLatch.countDown()
                }

                @Override
                void fail(ErrorCode errorCode) {
                    completionLatch.countDown()
                }
            })
        }

        assert completionLatch.await(6, TimeUnit.SECONDS)
        assert processedItems.size() == requestCount
        assert batchCount.get() >= 1
    }

    void testCompletionSuccessThrowDoesNotBlockChain() {
        def throwLatch = new CountDownLatch(1)
        def normalLatch = new CountDownLatch(1)

        def queue = new CoalesceQueue<Integer>() {
            @Override
            protected String getName() {
                return "test-success-throw"
            }

            @Override
            protected void executeBatch(List<Integer> items, Completion completion) {
                completion.success()
            }
        }

        def signature = "host-throw-success"

        // first request: Java Completion that throws on success() — AJ should catch it
        queue.submit(signature, 0, new ThrowOnSuccessCompletion(throwLatch))

        assert throwLatch.await(5, TimeUnit.SECONDS)

        // second request on same signature: must succeed if chain is not stuck
        queue.submit(signature, 1, new Completion(null) {
            @Override
            void success() {
                normalLatch.countDown()
            }

            @Override
            void fail(ErrorCode errorCode) {
                normalLatch.countDown()
            }
        })

        assert normalLatch.await(5, TimeUnit.SECONDS) : "chain stuck after completion.success() threw exception"
    }

    void testCompletionFailThrowDoesNotBlockChain() {
        def throwLatch = new CountDownLatch(1)
        def normalLatch = new CountDownLatch(1)

        def queue = new CoalesceQueue<Integer>() {
            @Override
            protected String getName() {
                return "test-fail-throw"
            }

            @Override
            protected void executeBatch(List<Integer> items, Completion completion) {
                completion.fail(org.zstack.core.Platform.operr(
                        org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_CORE_THREAD_10004,
                        "intentional batch failure"))
            }
        }

        def signature = "host-throw-fail"

        // first request: Java Completion that throws on fail() — AJ should catch it
        queue.submit(signature, 0, new ThrowOnFailCompletion(throwLatch))

        assert throwLatch.await(5, TimeUnit.SECONDS)

        // second request on same signature: must succeed if chain is not stuck
        queue.submit(signature, 1, new Completion(null) {
            @Override
            void success() {
                normalLatch.countDown()
            }

            @Override
            void fail(ErrorCode errorCode) {
                normalLatch.countDown()
            }
        })

        assert normalLatch.await(5, TimeUnit.SECONDS) : "chain stuck after completion.fail() threw exception"
    }

    void testRvExecuteBatchThrowDoesNotBlockChain() {
        def throwLatch = new CountDownLatch(1)
        def normalLatch = new CountDownLatch(1)

        def queue = new FailReturnValueCoalesceQueue()

        def signature = "host-rv-throw"
        queue.submit(signature, 0, new ThrowOnFailReturnValueCompletion(throwLatch))

        assert throwLatch.await(5, TimeUnit.SECONDS)

        queue.submit(signature, 1, new ReturnValueCompletion<String>(null) {
            @Override
            void success(String returnValue) {
                normalLatch.countDown()
            }

            @Override
            void fail(ErrorCode errorCode) {
                normalLatch.countDown()
            }
        })

        assert normalLatch.await(5, TimeUnit.SECONDS) : "chain stuck after RV executeBatch threw exception"
    }

    void testRvCompletionSuccessThrowDoesNotBlockChain() {
        def throwLatch = new CountDownLatch(1)
        def normalLatch = new CountDownLatch(1)

        def queue = new ReturnValueCoalesceQueue<Integer, String, String>() {
            @Override
            protected String getName() {
                return "test-rv-success-throw"
            }

            @Override
            protected void executeBatch(List<Integer> items, ReturnValueCompletion<String> completion) {
                completion.success("ok")
            }

            @Override
            protected String calculateResult(Integer item, String batchResult) {
                return batchResult
            }
        }

        def signature = "host-rv-success-throw"
        queue.submit(signature, 0, new ThrowOnSuccessReturnValueCompletion(throwLatch))

        assert throwLatch.await(5, TimeUnit.SECONDS)

        queue.submit(signature, 1, new ReturnValueCompletion<String>(null) {
            @Override
            void success(String returnValue) {
                normalLatch.countDown()
            }

            @Override
            void fail(ErrorCode errorCode) {
                normalLatch.countDown()
            }
        })

        assert normalLatch.await(5, TimeUnit.SECONDS) : "chain stuck after RV completion.success() threw exception"
    }

    void testRvCompletionFailThrowDoesNotBlockChain() {
        def throwLatch = new CountDownLatch(1)
        def normalLatch = new CountDownLatch(1)

        def queue = new ReturnValueCoalesceQueue<Integer, String, String>() {
            @Override
            protected String getName() {
                return "test-rv-fail-throw"
            }

            @Override
            protected void executeBatch(List<Integer> items, ReturnValueCompletion<String> completion) {
                completion.fail(org.zstack.core.Platform.operr(
                        org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_CORE_THREAD_10004,
                        "intentional rv batch failure"))
            }

            @Override
            protected String calculateResult(Integer item, String batchResult) {
                return null
            }
        }

        def signature = "host-rv-fail-throw"
        queue.submit(signature, 0, new ThrowOnFailReturnValueCompletion(throwLatch))

        assert throwLatch.await(5, TimeUnit.SECONDS)

        queue.submit(signature, 1, new ReturnValueCompletion<String>(null) {
            @Override
            void success(String returnValue) {
                normalLatch.countDown()
            }

            @Override
            void fail(ErrorCode errorCode) {
                normalLatch.countDown()
            }
        })

        assert normalLatch.await(5, TimeUnit.SECONDS) : "chain stuck after RV completion.fail() threw exception"
    }

    void testCalculateResultFailDoesNotBlockChain() {
        def firstLatch = new CountDownLatch(2)
        def normalLatch = new CountDownLatch(1)

        def queue = new ReturnValueCoalesceQueue<Integer, String, String>() {
            @Override
            protected String getName() {
                return "test-calc-fail-chain"
            }

            @Override
            protected void executeBatch(List<Integer> items, ReturnValueCompletion<String> completion) {
                completion.success("ok")
            }

            @Override
            protected String calculateResult(Integer item, String batchResult) {
                if (item == 0) {
                    throw new RuntimeException("intentional calculateResult failure")
                }
                return batchResult
            }
        }

        def signature = "host-calc-fail-chain"

        // item 0 will throw in calculateResult, item 1 should still succeed
        (0..1).each { idx ->
            queue.submit(signature, idx, new ReturnValueCompletion<String>(null) {
                @Override
                void success(String returnValue) {
                    firstLatch.countDown()
                }

                @Override
                void fail(ErrorCode errorCode) {
                    firstLatch.countDown()
                }
            })
        }

        assert firstLatch.await(5, TimeUnit.SECONDS)

        // subsequent request must work — chain not stuck
        queue.submit(signature, 2, new ReturnValueCompletion<String>(null) {
            @Override
            void success(String returnValue) {
                normalLatch.countDown()
            }

            @Override
            void fail(ErrorCode errorCode) {
                normalLatch.countDown()
            }
        })

        assert normalLatch.await(5, TimeUnit.SECONDS) : "chain stuck after calculateResult threw exception"
    }
}
