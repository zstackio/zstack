package org.zstack.core.thread;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.core.AbstractCompletion;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_CORE_THREAD_10003;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_CORE_THREAD_10004;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Base implementation for coalesce queues.
 *
 * @param <T> Request Item Type
 * @param <R> Batch Execution Result Type
 * @param <V> Single Request Result Type
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public abstract class AbstractCoalesceQueue<T, R, V> {
    private static final CLogger logger = Utils.getLogger(AbstractCoalesceQueue.class);

    @Autowired
    private ThreadFacade thdf;

    private final ConcurrentHashMap<String, SignatureQueue> signatureQueues = new ConcurrentHashMap<>();

    protected class PendingRequest {
        final T item;
        final AbstractCompletion completion;

        PendingRequest(T item, AbstractCompletion completion) {
            this.item = item;
            this.completion = completion;
        }

        @SuppressWarnings("unchecked")
        void notifySuccess(V result) {
            if (completion == null) {
                return;
            }

            if (completion instanceof ReturnValueCompletion) {
                ((ReturnValueCompletion<V>) completion).success(result);
            } else if (completion instanceof Completion) {
                ((Completion) completion).success();
            }
        }

        void notifyFailure(ErrorCode errorCode) {
            if (completion == null) {
                return;
            }

            if (completion instanceof ReturnValueCompletion) {
                ((ReturnValueCompletion<V>) completion).fail(errorCode);
            } else if (completion instanceof Completion) {
                ((Completion) completion).fail(errorCode);
            }
        }
    }

    private class SignatureQueue {
        final String syncSignature;
        List<PendingRequest> pendingList = Collections.synchronizedList(new ArrayList<>());

        SignatureQueue(String syncSignature) {
            this.syncSignature = syncSignature;
        }

        synchronized List<PendingRequest> takeAll() {
            List<PendingRequest> toProcess = pendingList;
            pendingList = Collections.synchronizedList(new ArrayList<>());
            return toProcess;
        }

        synchronized void add(PendingRequest request) {
            pendingList.add(request);
        }

        synchronized boolean isEmpty() {
            return pendingList.isEmpty();
        }
    }

    protected abstract String getName();

    // Changed to take AbstractCompletion, subclasses cast it to specific type
    protected abstract void executeBatch(List<T> items, AbstractCompletion completion);

    protected abstract AbstractCompletion createBatchCompletion(String syncSignature, List<PendingRequest> requests, SyncTaskChain chain);

    protected abstract V calculateResult(T item, R batchResult);

    protected final void handleSuccess(String syncSignature, List<PendingRequest> requests, R batchResult, SyncTaskChain chain) {
        for (PendingRequest req : requests) {
            try {
                V singleResult = calculateResult(req.item, batchResult);
                req.notifySuccess(singleResult);
            } catch (Throwable t) {
                logger.warn(String.format("[%s] failed to calculate result for item %s", getName(), req.item), t);
                req.notifyFailure(operr(ORG_ZSTACK_CORE_THREAD_10003, "failed to calculate result: %s", t.getMessage()));
            }
        }
        cleanup(syncSignature);
        chain.next();
    }

    protected final void handleFailure(String syncSignature, List<PendingRequest> requests, ErrorCode errorCode, SyncTaskChain chain) {
        for (PendingRequest req : requests) {
            req.notifyFailure(errorCode);
        }
        cleanup(syncSignature);
        chain.next();
    }

    void setThreadFacade(ThreadFacade thdf) {
        this.thdf = thdf;
    }

    protected final void submitRequest(String syncSignature, T item, AbstractCompletion completion) {
        doSubmit(syncSignature, new PendingRequest(item, completion));
    }

    private void doSubmit(String syncSignature, PendingRequest request) {
        SignatureQueue queue = signatureQueues.computeIfAbsent(syncSignature, SignatureQueue::new);
        queue.add(request);

        thdf.chainSubmit(new ChainTask(null) {
            @Override
            public String getSyncSignature() {
                return String.format("coalesce-queue-%s-%s", AbstractCoalesceQueue.this.getName(), syncSignature);
            }

            @Override
            public void run(SyncTaskChain chain) {
                List<PendingRequest> requests = queue.takeAll();

                if (requests.isEmpty()) {
                    chain.next();
                    return;
                }

                String name = getName();
                logger.debug(String.format("[%s] coalescing %d requests for signature[%s]",
                        name, requests.size(), syncSignature));


                // Create the specific completion type (Completion or ReturnValueCompletion)
                AbstractCompletion batchCompletion = createBatchCompletion(syncSignature, requests, chain);

                // Execute batch with the direct completion object
                List<T> items = requests.stream().map(req -> req.item).collect(Collectors.toList());

                /** *(.., AbstractCompletion, ..) is not AsyncSafeAspect's pointcut, but it will call
                 * executeBatch(.., Completion/ReturnValueCompletion) which is pointcut,
                 * so we do not need try-catch here.
                 */
                executeBatch(items, batchCompletion);
            }

            @Override
            public String getName() {
                return String.format("%s-coalesced-batch-%s", AbstractCoalesceQueue.this.getName(), syncSignature);
            }

            @Override
            protected int getSyncLevel() {
                return 1;
            }
        });
    }

    private void cleanup(String syncSignature) {
        signatureQueues.computeIfPresent(syncSignature, (k, queue) -> {
            if (queue.isEmpty()) {
                return null;
            }
            return queue;
        });
    }

    // For testing
    int getActiveQueueCount() {
        return signatureQueues.size();
    }
}
