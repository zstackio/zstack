package org.zstack.observability;

import io.opentelemetry.api.trace.Span;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.thread.CancelablePeriodicTask;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.header.Constants;
import org.zstack.header.Component;
import org.zstack.header.core.execution.*;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.message.BeforeDeliveryMessageInterceptor;
import org.zstack.header.rest.RESTConstant;
import org.zstack.header.rest.RestAPIExtensionPoint;

import java.sql.Timestamp;
import java.time.Instant;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * A bounded, node-local execution registry. The API handler aggregates this
 * registry from every management node through CloudBus, so no sticky REST
 * session or business-code changes are required.
 */
public class ExecutionObservabilityFacadeImpl implements ExecutionObservabilityFacade,
        ExecutionMessageObserver, ExecutionHttpObserver, ExecutionScheduledTaskObserver, Component,
        RestAPIExtensionPoint, BeforeDeliveryMessageInterceptor {
    private static final CLogger logger = Utils.getLogger(ExecutionObservabilityFacadeImpl.class);
    private static final int MAX_API_RECORDS = 5000;
    private static final int MAX_SCHEDULED_TASK_RECORDS = 3000;
    private static final int MAX_MESSAGE_RECORDS = 3000;
    private static final int MAX_RECORDS = MAX_API_RECORDS
            + MAX_SCHEDULED_TASK_RECORDS + MAX_MESSAGE_RECORDS;
    private static final int MAX_INDEX_ENTRIES = MAX_RECORDS * 2;
    private static final int MAX_ACTIVE_STAGES = 256;
    private static final int OBSERVATION_QUEUE_SIZE = 2048;
    private static final String EXECUTION_UUID_HEADER = "__execution_uuid__";
    private static final String EXECUTION_STAGE_CONTEXT = "__execution_stage_uuid__";
    private static final String IGNORED_EXECUTION_CONTEXT = "__execution_observation_ignored__";
    private static final String DETACHED_STAGE_DETAILS =
            "execution completed before a reply was observed";
    /*
     * CloudBus serializes the Log4j ThreadContext into the message task
     * context. Keeping the execution UUID in that context makes a child
     * message carry the same correlation when it crosses to another
     * management node, without requiring business code to copy a header.
     */
    private static final String EXECUTION_UUID_CONTEXT = EXECUTION_UUID_HEADER;

    private final Map<String, MutableExecution> executions = new ConcurrentHashMap<>();
    private final Map<String, String> messageToExecution = new ConcurrentHashMap<>();
    private final Map<String, String> messageToStage = new ConcurrentHashMap<>();
    private final Set<String> ignoredExecutionContexts = ConcurrentHashMap.newKeySet();
    private final Map<String, String> httpToExecution = new ConcurrentHashMap<>();
    private final Map<String, Map<String, String>> scheduledThreadContexts = new ConcurrentHashMap<>();
    private final Map<String, ConcurrentLinkedQueue<String>> executionOrders = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> executionCounts = new ConcurrentHashMap<>();
    private final AtomicBoolean cleanupQueued = new AtomicBoolean(false);
    private final Object executionAllocationLock = new Object();

    private volatile ThreadPoolExecutor observationExecutor;

    private final ExecutionObservationPolicy observationPolicy = new ExecutionObservationPolicy();

    /** Immutable fields extracted while the original Message is still owned by its caller. */
    private static final class MessageObservation {
        private final String messageUuid;
        private final String messageName;
        private final String propagatedExecutionUuid;
        private final boolean apiMessage;
        private final boolean observableApi;
        private final boolean localExecutionQuery;
        private final boolean noReply;

        private MessageObservation(Message message, boolean observableApi) {
            this.messageUuid = message.getId();
            this.messageName = message.getClass().getName();
            this.propagatedExecutionUuid = message.getHeaderEntry(EXECUTION_UUID_HEADER);
            this.apiMessage = message instanceof APIMessage;
            this.observableApi = observableApi;
            this.localExecutionQuery = message instanceof GetLocalExecutionMsg;
            this.noReply = doesNotExpectReply(message);
        }
    }

    @Autowired
    private CloudBus bus;

    private static boolean isObservationEnabled() {
        return ExecutionObservabilityGlobalProperty.ENABLED;
    }

    private MutableExecution getOrCreateExecution(String executionUuid,
                                                  java.util.function.Supplier<MutableExecution> factory) {
        MutableExecution existing = executions.get(executionUuid);
        if (existing != null) {
            return existing;
        }

        MutableExecution created = factory.get();
        MutableExecution previous = executions.putIfAbsent(executionUuid, created);
        if (previous != null) {
            return previous;
        }

        recordExecutionOrder(created);
        return created;
    }

    private MutableExecution putExecutionIfAbsent(MutableExecution execution) {
        MutableExecution previous = executions.putIfAbsent(execution.executionUuid, execution);
        if (previous != null) {
            return previous;
        }

        recordExecutionOrder(execution);
        return execution;
    }

    private void recordExecutionOrder(MutableExecution execution) {
        executionOrders.computeIfAbsent(execution.triggerType,
                ignored -> new ConcurrentLinkedQueue<>()).offer(execution.executionUuid);
        executionCounts.computeIfAbsent(execution.triggerType,
                ignored -> new AtomicInteger()).incrementAndGet();
    }

    private String nextAvailableExecutionUuid(String preferredUuid) {
        String candidate = preferredUuid;
        while (candidate == null || candidate.isEmpty() || executions.containsKey(candidate)) {
            candidate = uuid();
        }
        return candidate;
    }

    private MutableExecution getOrCreateRootMessageExecution(MessageObservation message) {
        synchronized (executionAllocationLock) {
            String mappedExecutionUuid = messageToExecution.get(message.messageUuid);
            MutableExecution mappedExecution = mappedExecutionUuid == null
                    ? null : executions.get(mappedExecutionUuid);
            if (mappedExecution != null && mappedExecution.isRootMessageExecution(message.messageUuid)) {
                return mappedExecution;
            }

            String executionUuid = nextAvailableExecutionUuid(message.messageUuid);
            MutableExecution execution = putExecutionIfAbsent(
                    MutableExecution.message(executionUuid, message));
            messageToExecution.put(message.messageUuid, execution.executionUuid);
            return execution;
        }
    }

    private boolean submitAsync(Runnable action, String operation) {
        // @AsyncThread only detaches the core caller. This queue owns the
        // observability backpressure and drop policy.
        ThreadPoolExecutor executor = observationExecutor;
        if (executor == null || executor.isShutdown()) {
            return false;
        }
        try {
            executor.execute(() -> {
                try {
                    action.run();
                } catch (Throwable t) {
                    logger.warn("failed to process asynchronous execution observation " + operation, t);
                }
            });
            return true;
        } catch (RejectedExecutionException ignored) {
            logger.debug("dropping asynchronous execution observation " + operation + " because the queue is full");
            return false;
        } catch (Throwable t) {
            logger.warn("failed to enqueue asynchronous execution observation " + operation, t);
            return false;
        }
    }

    private boolean submitTerminal(Runnable action, String operation) {
        if (submitAsync(action, operation)) {
            return true;
        }

        try {
            action.run();
            return true;
        } catch (Throwable t) {
            logger.warn("failed to process terminal execution observation " + operation, t);
            return false;
        }
    }

    @Override
    public void afterAPIRequest(Message message) {
        if (message instanceof APIMessage) {
            try {
                recordApiRequest((APIMessage) message);
            } catch (Throwable t) {
                logger.warn("failed to record API request observation", t);
            }
        }
    }

    @Override
    public void beforeAPIResponse(Message message) {
        if (!isObservationEnabled()) {
            return;
        }
        try {
            clearIgnoredForResponse(message);
        } catch (Throwable t) {
            logger.warn("failed to clear API observation context", t);
        }
        submitTerminal(() -> recordApiResponseInternal(message), "API response");
    }

    @Override
    public void beforeRestResponse(String method, int statusCode) {
        // The API response hook is the authoritative completion point. HTTP
        // response timing is intentionally kept out of the execution record.
    }

    @Override
    public void afterRestRequest(String method) {
        // no-op
    }

    @Override
    public int orderOfBeforeDeliveryMessageInterceptor() {
        return Integer.MIN_VALUE;
    }

    @Override
    public void beforeDeliveryMessage(Message message) {
        if (!isObservationEnabled()) {
            return;
        }
        if (message instanceof MessageReply) {
            MessageReply reply = (MessageReply) message;
            recordMessageCompleted(reply, reply.isSuccess() ? "SUCCEEDED" : "FAILED",
                    reply.getError() == null ? null : reply.getError().getDetails());
            return;
        }

        recordMessageStarted(message);
    }

    @Override
    public void recordMessageStarted(Message message) {
        if (!isObservationEnabled() || message == null) {
            return;
        }

        if (message instanceof APIMessage) {
            /*
             * The REST hook and CloudBus delivery hook can observe the same API
             * message concurrently. Reserve its canonical execution UUID on the
             * original message before either hook creates an immutable snapshot.
             */
            recordApiRequest((APIMessage) message);
        }
        MessageObservation observation = observeMessage(message);
        final Map<String, String> context = new HashMap<>(ThreadContext.getImmutableContext());
        ensureDeliveredExecution(observation);
        activateDeliveredStage(observation);
        Runnable observationAction = () -> {
            Map<String, String> previous = new HashMap<>(ThreadContext.getImmutableContext());
            try {
                ThreadContext.clearAll();
                ThreadContext.putAll(context);
                recordMessageDelivery(observation);
            } finally {
                ThreadContext.clearAll();
                ThreadContext.putAll(previous);
            }
        };
        if (!submitAsync(observationAction, "message delivery")) {
            // A start event is required to establish the reply correlation and
            // parent stage. Process it inline when the bounded queue is full;
            // dropping it would leave an execution permanently uncorrelated.
            observationAction.run();
        }
    }

    private MessageObservation observeMessage(Message message) {
        boolean observableApi = !(message instanceof APIMessage)
                || observationPolicy.shouldObserveApi((APIMessage) message);
        return new MessageObservation(message, observableApi);
    }

    private void ensureDeliveredExecution(MessageObservation message) {
        if (message.apiMessage || message.localExecutionQuery
                || ThreadContext.get(IGNORED_EXECUTION_CONTEXT) != null) {
            return;
        }

        String parentExecutionUuid = ThreadContext.get(EXECUTION_UUID_CONTEXT);
        if (parentExecutionUuid == null) {
            parentExecutionUuid = ThreadContext.get(Constants.THREAD_CONTEXT_API);
        }
        if (parentExecutionUuid == null) {
            parentExecutionUuid = ThreadContext.get(Constants.THREAD_CONTEXT_TASK);
        }
        if (parentExecutionUuid != null && ignoredExecutionContexts.contains(parentExecutionUuid)) {
            return;
        }

        boolean rootMessageContext = parentExecutionUuid != null
                && parentExecutionUuid.equals(message.messageUuid);
        String mappedParentUuid = parentExecutionUuid == null || rootMessageContext
                ? null : messageToExecution.get(parentExecutionUuid);
        MutableExecution execution = parentExecutionUuid == null || rootMessageContext ? null
                : executions.get(mappedParentUuid == null ? parentExecutionUuid : mappedParentUuid);
        if (execution == null && parentExecutionUuid != null
                && !parentExecutionUuid.equals(message.messageUuid)) {
            final String inheritedExecutionUuid = parentExecutionUuid;
            execution = getOrCreateExecution(inheritedExecutionUuid,
                    () -> MutableExecution.inherited(inheritedExecutionUuid,
                            ThreadContext.get(Constants.THREAD_CONTEXT_TASK_NAME)));
        }
        if (execution == null) {
            execution = getOrCreateRootMessageExecution(message);
        }
        messageToExecution.put(message.messageUuid, execution.executionUuid);
        trimIfNeeded();
    }

    private void activateDeliveredStage(MessageObservation message) {
        if (message.localExecutionQuery) {
            return;
        }
        if (message.apiMessage && !message.observableApi) {
            return;
        }
        if (ThreadContext.get(IGNORED_EXECUTION_CONTEXT) != null) {
            return;
        }

        /*
         * beforeDeliveryMessage runs on the business thread immediately before
         * Service.handleMessage. Make the delivered message the current stage
         * there so child messages and outbound HTTP requests inherit their real
         * parent. The observation worker receives the context captured above,
         * which still points to the parent stage and records the edge.
         */
        ThreadContext.put(EXECUTION_STAGE_CONTEXT, message.messageUuid);
    }

    public void recordApiRequest(APIMessage message) {
        if (!isObservationEnabled() || message == null) {
            return;
        }

        synchronized (message) {
            MessageObservation observation = observeMessage(message);
            String executionUuid = recordApiRequest(observation);
            if (executionUuid != null) {
                // The same API message is delivered first to the portal and then to its
                // business service. Persist the canonical UUID on the message so both
                // deliveries, including concurrent observer callbacks, reuse one record.
                message.putHeaderEntry(EXECUTION_UUID_HEADER, executionUuid);
            }
        }
    }

    private String recordApiRequest(MessageObservation message) {
        if (!message.observableApi) {
            markIgnored(message.messageUuid);
            return null;
        }

        String executionUuid;
        MutableExecution execution;
        synchronized (executionAllocationLock) {
            if (message.propagatedExecutionUuid == null || message.propagatedExecutionUuid.isEmpty()) {
                executionUuid = nextAvailableExecutionUuid(message.messageUuid);
                execution = putExecutionIfAbsent(MutableExecution.api(executionUuid, message));
            } else {
                executionUuid = message.propagatedExecutionUuid;
                final String inheritedExecutionUuid = executionUuid;
                execution = getOrCreateExecution(inheritedExecutionUuid,
                        () -> MutableExecution.api(inheritedExecutionUuid, message));
            }
            messageToExecution.put(message.messageUuid, executionUuid);
        }
        ThreadContext.put(EXECUTION_UUID_CONTEXT, executionUuid);
        execution.accepted();
        trimIfNeeded();
        return executionUuid;
    }

    private void markIgnored(String messageUuid) {
        if (messageUuid == null) {
            return;
        }
        ignoredExecutionContexts.add(messageUuid);
        ThreadContext.put(IGNORED_EXECUTION_CONTEXT, messageUuid);
        trimIfNeeded();
    }

    public void recordMessageDelivery(Message message) {
        if (!isObservationEnabled() || message == null) {
            return;
        }
        if (message instanceof MessageReply) {
            MessageReply reply = (MessageReply) message;
            recordMessageReply(reply.getCorrelationId(), reply.isSuccess(),
                    reply.getError() == null ? null : reply.getError().getDetails());
            return;
        }
        recordMessageDelivery(observeMessage(message));
    }

    private void recordMessageDelivery(MessageObservation message) {
        if (message.apiMessage && !message.observableApi) {
            // Querying executions must not itself become an observed execution.
            markIgnored(message.messageUuid);
            return;
        }

        if (message.localExecutionQuery) {
            // This is the fan-out control message used by the query API itself;
            // exposing it would make every read create another observable record.
            return;
        }

        if (ThreadContext.get(IGNORED_EXECUTION_CONTEXT) != null) {
            return;
        }

        if (message.apiMessage) {
            String executionUuid = recordApiRequest(message);
            MutableExecution execution = executionUuid == null ? null : executions.get(executionUuid);
            if (execution != null) {
                execution.deliveryStarted();
            }
            return;
        }

        String parentExecutionUuid = ThreadContext.get(EXECUTION_UUID_CONTEXT);
        if (parentExecutionUuid == null) {
            parentExecutionUuid = ThreadContext.get(Constants.THREAD_CONTEXT_API);
        }
        if (parentExecutionUuid == null) {
            parentExecutionUuid = ThreadContext.get(Constants.THREAD_CONTEXT_TASK);
        }
        if (parentExecutionUuid != null && ignoredExecutionContexts.contains(parentExecutionUuid)) {
            return;
        }
        boolean rootMessageContext = parentExecutionUuid != null
                && parentExecutionUuid.equals(message.messageUuid);
        String mappedParentUuid = parentExecutionUuid == null || rootMessageContext
                ? null : messageToExecution.get(parentExecutionUuid);
        MutableExecution parent = parentExecutionUuid == null || rootMessageContext ? null
                : executions.get(mappedParentUuid == null ? parentExecutionUuid : mappedParentUuid);
        // CloudBus uses the root message id as THREAD_CONTEXT_TASK for a
        // context-free message. That is the message being observed, not an
        // inherited parent execution; let the message branch below create
        // the execution with the message id as its rootMessageUuid.
        if (parent == null && parentExecutionUuid != null
                && !parentExecutionUuid.equals(message.messageUuid)) {
            final String inheritedExecutionUuid = parentExecutionUuid;
            parent = getOrCreateExecution(inheritedExecutionUuid,
                    () -> MutableExecution.inherited(inheritedExecutionUuid,
                            ThreadContext.get(Constants.THREAD_CONTEXT_TASK_NAME)));
        }
        if (parent != null) {
            String stageUuid = parent.startStage(message);
            messageToExecution.put(message.messageUuid, parent.executionUuid);
            if (stageUuid != null) {
                messageToStage.put(message.messageUuid, stageUuid);
            }
            if (stageUuid != null && message.noReply) {
                parent.finishStage(stageUuid, true, null);
                messageToStage.remove(message.messageUuid, stageUuid);
            }
            trimIfNeeded();
            return;
        }

        // A NeedReplyMessage is an observable execution even when no API is involved.
        // Fire-and-forget messages are completed at delivery because there is no reply
        // correlation from which a later completion could be inferred.
        String mappedExecutionUuid = messageToExecution.get(message.messageUuid);
        MutableExecution execution = mappedExecutionUuid == null
                ? null : executions.get(mappedExecutionUuid);
        if (execution == null) {
            execution = getOrCreateRootMessageExecution(message);
        }
        execution.deliveryStarted();
        if (message.noReply) {
            finishExecution(execution, "SUCCEEDED", null);
        }
        trimIfNeeded();
    }

    private void recordMessageReply(String correlationId, boolean success, String error) {
        if (correlationId == null) {
            return;
        }
        String executionUuid = messageToExecution.get(correlationId);
        String stageUuid = messageToStage.get(correlationId);
        MutableExecution parent = executionUuid == null ? null : executions.get(executionUuid);
        if (parent != null && stageUuid != null) {
            parent.finishStage(stageUuid, success, error);
            messageToStage.remove(correlationId, stageUuid);
            return;
        }
        finish(correlationId, success, error);
    }

    private static boolean doesNotExpectReply(Message message) {
        return !(message instanceof NeedReplyMessage)
                || Boolean.parseBoolean(message.getHeaderEntry(CloudBus.HEADER_NO_NEED_REPLY_MSG));
    }

    public void recordApiResponse(Message message) {
        if (!isObservationEnabled()) {
            return;
        }
        clearIgnoredForResponse(message);
        recordApiResponseInternal(message);
    }

    private void clearIgnoredForResponse(Message message) {
        if (message instanceof APIEvent) {
            clearIgnored(((APIEvent) message).getApiId());
            return;
        }
        if (message instanceof MessageReply) {
            String correlationId = ((MessageReply) message).getCorrelationId();
            if (correlationId != null) {
                clearIgnored(correlationId);
            }
        }
    }

    private void recordApiResponseInternal(Message message) {
        if (message instanceof APIEvent) {
            APIEvent event = (APIEvent) message;
            finish(event.getApiId(), event.isSuccess(), event.getError() == null ? null : event.getError().getDetails());
        }
        // CloudBus invokes RestAPIExtensionPoint.beforeAPIResponse for every
        // internal MessageReply as well. Those replies close their correlated
        // stages in recordMessageDelivery; they are not terminal API responses.
    }

    private void clearIgnored(String apiUuid) {
        if (apiUuid == null) {
            return;
        }
        ignoredExecutionContexts.remove(apiUuid);
        if (apiUuid.equals(ThreadContext.get(IGNORED_EXECUTION_CONTEXT))) {
            ThreadContext.remove(IGNORED_EXECUTION_CONTEXT);
        }
    }

    @Override
    public void recordMessageCompleted(Message message, String state, String reason) {
        if (!isObservationEnabled() || message == null) {
            return;
        }
        if (message instanceof MessageReply) {
            // A callback may forward the same reply instance to an outer
            // message and replace its correlation ID. Snapshot every mutable
            // field before handing observation to the worker thread.
            final String correlationId = ((MessageReply) message).getCorrelationId();
            final boolean success = "SUCCEEDED".equals(state);
            submitTerminal(() -> recordMessageReply(correlationId, success, reason), "message reply");
        } else {
            final String messageUuid = message.getId();
            submitTerminal(() -> finishTerminal(messageUuid, state, reason), "message completion");
        }
    }

    @Override
    public String recordHttpRequestStarted(HttpMethod method, String url, HttpEntity<?> request) {
        if (!isObservationEnabled()) {
            return null;
        }
        try {
            String methodName = method == null ? "UNKNOWN" : method.toString();
            String httpTaskUuid = request == null ? null
                    : request.getHeaders().getFirst(RESTConstant.TASK_UUID);
            return startHttpRequestInternal(methodName, url, httpTaskUuid);
        } catch (Throwable t) {
            logger.warn("failed to record started HTTP execution observation", t);
            return null;
        }
    }

    private String startHttpRequestInternal(String method, String url, String httpTaskUuid) {
        String parentExecutionUuid = ThreadContext.get(EXECUTION_UUID_CONTEXT);
        if (parentExecutionUuid == null) {
            parentExecutionUuid = ThreadContext.get(Constants.THREAD_CONTEXT_API);
        }
        if (parentExecutionUuid == null) {
            parentExecutionUuid = ThreadContext.get(Constants.THREAD_CONTEXT_TASK);
        }

        String mappedExecutionUuid = parentExecutionUuid == null ? null : messageToExecution.get(parentExecutionUuid);
        MutableExecution execution = parentExecutionUuid == null ? null
                : executions.get(mappedExecutionUuid == null ? parentExecutionUuid : mappedExecutionUuid);
        if (execution == null) {
            return null;
        }

        String requestUuid = uuid();
        String parentStageUuid = ThreadContext.get(EXECUTION_STAGE_CONTEXT);
        String stageUuid = execution.startHttpStage(requestUuid, parentStageUuid, method, sanitizeHttpUrl(url), httpTaskUuid);
        if (stageUuid == null) {
            return null;
        }
        httpToExecution.put(requestUuid, execution.executionUuid);
        execution.httpRequestUuids.add(requestUuid);
        trimIfNeeded();
        return requestUuid;
    }

    @Override
    public void recordHttpRequestCompleted(String requestUuid, String state, Integer statusCode, String error) {
        if (!isObservationEnabled()) {
            return;
        }
        submitTerminal(() -> finishHttpRequestInternal(requestUuid, state, statusCode, error),
                "HTTP response");
    }

    private void finishHttpRequestInternal(String requestUuid, String state, Integer statusCode, String error) {
        if (requestUuid == null) {
            return;
        }
        String executionUuid = httpToExecution.remove(requestUuid);
        MutableExecution execution = executionUuid == null ? null : executions.get(executionUuid);
        if (execution != null) {
            execution.httpRequestUuids.remove(requestUuid);
            execution.finishHttpStage(requestUuid, state, statusCode, error);
        }
    }

    @Override
    public String recordScheduledTaskStarted(Object task) {
        if (!isObservationEnabled()) {
            return null;
        }
        String taskName = scheduledTaskName(task);
        String taskClass = task == null ? "unknown-scheduled-task" : task.getClass().getName();
        String executionUuid = null;
        Map<String, String> previous = null;
        try {
            previous = new HashMap<>(ThreadContext.getImmutableContext());
            String taskRunUuid = uuid();
            synchronized (executionAllocationLock) {
                executionUuid = nextAvailableExecutionUuid(taskRunUuid);
                scheduledThreadContexts.put(executionUuid, previous);
                ThreadContext.put(Constants.THREAD_CONTEXT_API, executionUuid);
                ThreadContext.put(Constants.THREAD_CONTEXT_TASK_NAME, taskName);
                ThreadContext.put(EXECUTION_UUID_CONTEXT, executionUuid);
                MutableExecution execution = MutableExecution.scheduled(
                        executionUuid, taskRunUuid, taskName, taskClass);
                putExecutionIfAbsent(execution);
                execution.started();
            }
            trimIfNeeded();
            return executionUuid;
        } catch (Throwable t) {
            if (executionUuid != null) {
                scheduledThreadContexts.remove(executionUuid);
                removeExecution(executionUuid);
            }
            if (previous != null) {
                try {
                    ThreadContext.clearAll();
                    ThreadContext.putAll(previous);
                } catch (Throwable restoreFailure) {
                    logger.warn("failed to restore thread context after starting scheduled task observation", restoreFailure);
                }
            }
            logger.warn("failed to start scheduled task observation", t);
            return null;
        }
    }

    @Override
    public void recordScheduledTaskCompleted(String executionUuid, Object task, Throwable error) {
        if (executionUuid == null) {
            return;
        }
        try {
            Map<String, String> previous = scheduledThreadContexts.remove(executionUuid);
            if (previous != null) {
                ThreadContext.clearAll();
                ThreadContext.putAll(previous);
            }
        } catch (Throwable t) {
            logger.warn(String.format("failed to restore thread context for scheduled task[%s]", executionUuid), t);
        }
        // Context restoration must happen on the task thread; only the
        // terminal observation is handed to the asynchronous path.
        submitTerminal(() -> finishScheduledTaskInternal(executionUuid, error), "scheduled task response");
    }

    private static String scheduledTaskName(Object task) {
        if (task instanceof PeriodicTask) {
            return ((PeriodicTask) task).getName();
        }
        if (task instanceof CancelablePeriodicTask) {
            return ((CancelablePeriodicTask) task).getName();
        }
        return task == null ? "unknown-scheduled-task" : task.getClass().getName();
    }

    private void finishScheduledTaskInternal(String executionUuid, Throwable error) {
        MutableExecution execution = executions.get(executionUuid);
        if (execution != null) {
            finishExecution(execution, error == null ? "SUCCEEDED" : "FAILED",
                    error == null ? null : error.getMessage());
        }
    }

    @Override
    public List<ExecutionInventory> queryLocal(APIQueryExecutionMsg query) {
        if (!isObservationEnabled() || query == null) {
            return new ArrayList<>();
        }
        List<MutableExecution> candidates = new ArrayList<>(executions.values());
        candidates.removeIf(e -> !e.matches(query));
        candidates.sort(Comparator.comparingLong((MutableExecution e) -> e.acceptedAt).reversed());

        int offset = parseCursor(query.getCursor());
        int limit = query.getLimit() == null ? 50 : Math.max(1, Math.min(200, query.getLimit()));
        return candidates.stream().skip(offset).limit(limit)
                .map(e -> e.inventory(query.getDetail()))
                .collect(Collectors.toList());
    }

    private void finish(String correlationId, boolean success, String error) {
        if (correlationId == null) {
            return;
        }
        String executionUuid = messageToExecution.get(correlationId);
        MutableExecution execution = executions.get(executionUuid == null ? correlationId : executionUuid);
        if (execution != null) {
            finishExecution(execution, success ? "SUCCEEDED" : "FAILED", error);
        }
    }

    private void finishExecution(MutableExecution execution, String state, String error) {
        for (String stageUuid : execution.finish(state, error)) {
            messageToStage.remove(stageUuid, stageUuid);
            httpToExecution.remove(stageUuid, execution.executionUuid);
            execution.httpRequestUuids.remove(stageUuid);
        }
    }

    private void finishTerminal(String messageUuid, String state, String error) {
        if (messageUuid == null) {
            return;
        }
        String executionUuid = messageToExecution.get(messageUuid);
        String stageUuid = messageToStage.get(messageUuid);
        MutableExecution execution = executions.get(executionUuid == null ? messageUuid : executionUuid);
        if (execution == null) {
            return;
        }
        if (stageUuid != null) {
            execution.finishStage(stageUuid, state, error);
            messageToStage.remove(messageUuid, stageUuid);
        }
        if (stageUuid == null || execution.isRootMessage(messageUuid)) {
            finishExecution(execution, state, error);
        }
    }

    private static int parseCursor(String cursor) {
        if (cursor == null || cursor.isEmpty()) {
            return 0;
        }
        try {
            return Math.max(0, Integer.parseInt(cursor));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private void trimIfNeeded() {
        if (!isOverExecutionLimit()
                && messageToExecution.size() <= MAX_INDEX_ENTRIES
                && messageToStage.size() <= MAX_INDEX_ENTRIES
                && ignoredExecutionContexts.size() <= MAX_INDEX_ENTRIES
                && httpToExecution.size() <= MAX_INDEX_ENTRIES
                && scheduledThreadContexts.size() <= MAX_INDEX_ENTRIES) {
            return;
        }

        trimExecutionType("API", MAX_API_RECORDS);
        trimExecutionType("SCHEDULED_TASK", MAX_SCHEDULED_TASK_RECORDS);
        trimExecutionType("MESSAGE", MAX_MESSAGE_RECORDS);

        if (cleanupQueued.compareAndSet(false, true)) {
            boolean queued = submitAsync(() -> {
                boolean changed = false;
                try {
                    changed = cleanupIndexes();
                } finally {
                    cleanupQueued.set(false);
                    if (changed && isOverLimit()) {
                        trimIfNeeded();
                    }
                }
            }, "registry cleanup");
            if (!queued) {
                cleanupQueued.set(false);
            }
        }
    }

    private boolean isOverLimit() {
        return isOverExecutionLimit()
                || messageToExecution.size() > MAX_INDEX_ENTRIES
                || messageToStage.size() > MAX_INDEX_ENTRIES
                || ignoredExecutionContexts.size() > MAX_INDEX_ENTRIES
                || httpToExecution.size() > MAX_INDEX_ENTRIES
                || scheduledThreadContexts.size() > MAX_INDEX_ENTRIES;
    }

    private boolean isOverExecutionLimit() {
        return executionCount("API") > MAX_API_RECORDS
                || executionCount("SCHEDULED_TASK") > MAX_SCHEDULED_TASK_RECORDS
                || executionCount("MESSAGE") > MAX_MESSAGE_RECORDS;
    }

    private int executionCount(String triggerType) {
        AtomicInteger count = executionCounts.get(triggerType);
        return count == null ? 0 : count.get();
    }

    private void trimExecutionType(String triggerType, int maxRecords) {
        AtomicInteger count = executionCounts.get(triggerType);
        ConcurrentLinkedQueue<String> order = executionOrders.get(triggerType);
        if (count == null || order == null) {
            return;
        }
        while (count.get() > maxRecords) {
            String oldestUuid = order.poll();
            if (oldestUuid == null) {
                return;
            }
            MutableExecution execution = executions.get(oldestUuid);
            if (execution != null && triggerType.equals(execution.triggerType)
                    && executions.remove(oldestUuid, execution)) {
                count.decrementAndGet();
                removeExecutionIndexes(execution);
            }
        }
    }

    private void removeExecution(String executionUuid) {
        MutableExecution removed = executions.remove(executionUuid);
        if (removed != null) {
            AtomicInteger count = executionCounts.get(removed.triggerType);
            if (count != null) {
                count.decrementAndGet();
            }
            removeExecutionIndexes(removed);
        }
    }

    private void removeExecutionIndexes(MutableExecution execution) {
        String executionUuid = execution.executionUuid;
        execution.observedMessageUuids.forEach(messageUuid -> {
            messageToExecution.remove(messageUuid, executionUuid);
            messageToStage.remove(messageUuid);
        });
        for (String requestUuid : execution.httpRequestUuids) {
            httpToExecution.remove(requestUuid, executionUuid);
        }
        execution.httpRequestUuids.clear();
        scheduledThreadContexts.remove(executionUuid);
    }

    private boolean cleanupIndexes() {
        int before = messageToExecution.size() + messageToStage.size() + httpToExecution.size()
                + scheduledThreadContexts.size() + ignoredExecutionContexts.size();
        messageToExecution.entrySet().removeIf(e -> !executions.containsKey(e.getValue()));
        messageToStage.entrySet().removeIf(e -> {
            String executionUuid = messageToExecution.get(e.getKey());
            return executionUuid == null || !executions.containsKey(executionUuid);
        });
        httpToExecution.entrySet().removeIf(e -> !executions.containsKey(e.getValue()));
        scheduledThreadContexts.entrySet().removeIf(e -> !executions.containsKey(e.getKey()));

        trimCompletedMessageMappings();
        trimSetToSize(ignoredExecutionContexts, MAX_INDEX_ENTRIES);
        int after = messageToExecution.size() + messageToStage.size() + httpToExecution.size()
                + scheduledThreadContexts.size() + ignoredExecutionContexts.size();
        return after < before;
    }

    private void trimCompletedMessageMappings() {
        int excess = messageToExecution.size() - MAX_INDEX_ENTRIES;
        if (excess <= 0) {
            return;
        }
        for (Map.Entry<String, String> entry : messageToExecution.entrySet()) {
            if (excess-- <= 0) {
                break;
            }
            MutableExecution execution = executions.get(entry.getValue());
            if (execution == null || execution.isFinished()) {
                messageToExecution.remove(entry.getKey(), entry.getValue());
                messageToStage.remove(entry.getKey());
            }
        }
    }

    private static <T> void trimSetToSize(Set<T> set, int maxEntries) {
        int excess = set.size() - maxEntries;
        if (excess <= 0) {
            return;
        }
        for (T value : set) {
            if (excess-- <= 0) {
                break;
            }
            set.remove(value);
        }
    }

    private static String uuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static Timestamp now() {
        return Timestamp.from(Instant.now());
    }

    private static String sanitizeHttpUrl(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme != null && host != null) {
                StringBuilder sanitized = new StringBuilder(scheme).append("://");
                if (host.contains(":") && !host.startsWith("[")) {
                    sanitized.append('[').append(host).append(']');
                } else {
                    sanitized.append(host);
                }
                if (uri.getPort() > 0) {
                    sanitized.append(':').append(uri.getPort());
                }
                String path = uri.getRawPath();
                sanitized.append(path == null || path.isEmpty() ? "/" : path);
                return sanitized.toString();
            }
        } catch (URISyntaxException ignored) {
            // Keep malformed URLs from breaking the HTTP call or its callback.
        }
        return "<invalid-url>";
    }

    private static Timestamp parseTimestamp(String value) {
        return ExecutionTimestampParser.parse(value);
    }

    private static final class MutableExecution {
        private final String executionUuid;
        private final String apiUuid;
        private final String messageUuid;
        private final String taskRunUuid;
        private final String apiName;
        private final String requestKind;
        private final Boolean noReply;
        private final String triggerType;
        private final String triggerName;
        private final String taskClass;
        private final String traceId;
        private final long acceptedAt;
        private final String nodeUuid = Platform.getManagementServerId();
        private final AtomicLong sequence = new AtomicLong();
        private final List<ExecutionEventInventory> events = new ArrayList<>();
        private final Map<String, ExecutionStageInventory> stages = new ConcurrentHashMap<>();
        private final Map<String, Long> stageDurations = new ConcurrentHashMap<>();
        private final Set<String> observedMessageUuids = ConcurrentHashMap.newKeySet();
        private final Set<String> httpRequestUuids = ConcurrentHashMap.newKeySet();
        private volatile long startedAt;
        private volatile long finishedAt;
        private volatile long lastHeartbeatAt;
        private volatile long downstreamWaitMs;
        private volatile String state = "RECEIVED";
        private volatile String error;
        private String pendingTerminalState;
        private String pendingTerminalError;

        private MutableExecution(String executionUuid, String apiUuid, String messageUuid, String taskRunUuid,
                                 String apiName, String requestKind, Boolean noReply,
                                 String triggerType, String triggerName, String taskClass) {
            this.executionUuid = executionUuid;
            this.apiUuid = apiUuid;
            this.messageUuid = messageUuid;
            this.taskRunUuid = taskRunUuid;
            this.apiName = apiName;
            this.requestKind = requestKind;
            this.noReply = noReply;
            this.triggerType = triggerType;
            this.triggerName = triggerName;
            this.taskClass = taskClass;
            this.traceId = Span.current().getSpanContext().isValid() ? Span.current().getSpanContext().getTraceId() : null;
            this.acceptedAt = System.currentTimeMillis();
            this.lastHeartbeatAt = acceptedAt;
            if (messageUuid != null) {
                observedMessageUuids.add(messageUuid);
            }
        }

        static MutableExecution api(String executionUuid, MessageObservation message) {
            return new MutableExecution(executionUuid, message.messageUuid, message.messageUuid,
                    currentTaskId(), message.messageName, "API", null,
                    "API", message.messageName, message.messageName);
        }

        static MutableExecution message(String executionUuid, MessageObservation message) {
            // Message ids are globally unique and are carried unchanged across
            // management nodes, which lets the cluster query de-duplicate a
            // delivery observed on both sides of a CloudBus hop.
            String id = message.messageUuid;
            String taskRunUuid = currentTaskId();
            // CloudBus uses the root message id as a fallback THREAD_CONTEXT_TASK
            // for context-free messages. That fallback is not an independent task
            // run and must not be exposed as taskRunUuid.
            if (id.equals(taskRunUuid)) {
                taskRunUuid = null;
            }
            String messageClass = message.messageName;
            return new MutableExecution(executionUuid, null, id, taskRunUuid, messageClass,
                    "MESSAGE", message.noReply, "MESSAGE", messageClass, messageClass);
        }

        static MutableExecution scheduled(String executionUuid, String taskRunUuid,
                                          String name, String taskClass) {
            return new MutableExecution(executionUuid, null, null, taskRunUuid, name, "SCHEDULED_TASK",
                    null, "SCHEDULED_TASK", name, taskClass);
        }

        static MutableExecution inherited(String id, String name) {
            return new MutableExecution(id, null, null, null,
                    name == null ? "inherited-execution" : name, "MESSAGE", null, "MESSAGE",
                    name == null ? "inherited-execution" : name, name);
        }

        private static String currentTaskId() {
            String task = org.apache.logging.log4j.ThreadContext.get(Constants.THREAD_CONTEXT_TASK);
            return task == null ? null : task;
        }

        synchronized void accepted() {
            if ("RECEIVED".equals(state)) {
                state = "QUEUED";
                addEvent("API_ACCEPTED", null);
            }
            lastHeartbeatAt = System.currentTimeMillis();
        }

        synchronized void started() {
            if (startedAt == 0) {
                startedAt = System.currentTimeMillis();
            }
            state = "RUNNING";
            lastHeartbeatAt = System.currentTimeMillis();
            addEvent("EXECUTION_STARTED", null);
        }

        synchronized void deliveryStarted() {
            if (startedAt == 0) {
                startedAt = System.currentTimeMillis();
            }
            if (!"SUCCEEDED".equals(state) && !"FAILED".equals(state)) {
                state = "RUNNING";
            }
            lastHeartbeatAt = System.currentTimeMillis();
            addEvent("MESSAGE_DELIVERED", null);
        }

        synchronized String startStage(MessageObservation message) {
            String stageUuid = message.messageUuid;
            observedMessageUuids.add(stageUuid);
            if (startedAt == 0) {
                startedAt = System.currentTimeMillis();
            }
            if ("RECEIVED".equals(state) || "QUEUED".equals(state)) {
                state = "RUNNING";
            }
            ExecutionStageInventory stage = new ExecutionStageInventory();
            stage.setStageUuid(stageUuid);
            String parentStageUuid = ThreadContext.get(EXECUTION_STAGE_CONTEXT);
            if (parentStageUuid != null && !parentStageUuid.equals(stageUuid)) {
                stage.setParentStageUuid(parentStageUuid);
            }
            stage.setName(message.messageName);
            stage.setKind("MESSAGE");
            stage.setState("RUNNING");
            stage.setNodeUuid(nodeUuid);
            stage.setStartedAt(now());
            stage.setNoReply(message.noReply);
            addEvent("STAGE_STARTED", stageUuid, null, stage);
            if (finishedAt != 0) {
                detachStage(stage);
                return null;
            }
            stages.put(stageUuid, stage);
            trimStagesIfNeeded();
            ThreadContext.put(EXECUTION_STAGE_CONTEXT, stageUuid);
            lastHeartbeatAt = System.currentTimeMillis();
            return stageUuid;
        }

        synchronized void finishStage(String stageUuid, boolean success, String details) {
            finishStage(stageUuid, success ? "SUCCEEDED" : "FAILED", details);
        }

        synchronized void finishStage(String stageUuid, String terminalState, String details) {
            ExecutionStageInventory stage = stages.remove(stageUuid);
            if (stage == null) {
                return;
            }
            Timestamp finished = now();
            stage.setFinishedAt(finished);
            stage.setState(terminalState);
            stage.setElapsedMs(Math.max(0, finished.getTime() - stage.getStartedAt().getTime()));
            stageDurations.put(stageUuid, stage.getElapsedMs());
            addEvent("STAGE_" + terminalState, stageUuid, details, stage);
            if (stageUuid.equals(ThreadContext.get(EXECUTION_STAGE_CONTEXT))) {
                ThreadContext.remove(EXECUTION_STAGE_CONTEXT);
            }
            lastHeartbeatAt = finished.getTime();
            finishIfReady();
        }

        synchronized String startHttpStage(String requestUuid, String parentStageUuid, String method, String url,
                                           String httpTaskUuid) {
            if (startedAt == 0) {
                startedAt = System.currentTimeMillis();
            }
            if ("RECEIVED".equals(state) || "QUEUED".equals(state)) {
                state = "RUNNING";
            }
            ExecutionStageInventory stage = new ExecutionStageInventory();
            stage.setStageUuid(requestUuid);
            stage.setParentStageUuid(parentStageUuid);
            stage.setName("HTTP " + method + " " + url);
            stage.setKind("HTTP");
            stage.setState("RUNNING");
            stage.setNodeUuid(nodeUuid);
            stage.setStartedAt(now());
            stage.setHttpMethod(method);
            stage.setHttpUrl(url);
            stage.setHttpTaskUuid(httpTaskUuid);
            addHttpEvent("HTTP_REQUEST_STARTED", stage, null, null);
            if (finishedAt != 0) {
                detachStage(stage);
                return null;
            }
            stages.put(requestUuid, stage);
            trimStagesIfNeeded();
            lastHeartbeatAt = System.currentTimeMillis();
            return requestUuid;
        }

        private void trimStagesIfNeeded() {
            while (stages.size() > MAX_ACTIVE_STAGES) {
                ExecutionStageInventory oldest = stages.values().stream()
                        .min(Comparator.comparingLong(stage -> stage.getStartedAt() == null
                                ? Long.MIN_VALUE : stage.getStartedAt().getTime()))
                        .orElse(null);
                if (oldest == null || !stages.remove(oldest.getStageUuid(), oldest)) {
                    return;
                }
                if (oldest.getStageUuid().equals(ThreadContext.get(EXECUTION_STAGE_CONTEXT))) {
                    ThreadContext.remove(EXECUTION_STAGE_CONTEXT);
                }
            }
        }

        synchronized void finishHttpStage(String requestUuid, String terminalState, Integer statusCode, String error) {
            ExecutionStageInventory stage = stages.remove(requestUuid);
            if (stage == null) {
                return;
            }
            Timestamp finished = now();
            long elapsed = Math.max(0, finished.getTime() - stage.getStartedAt().getTime());
            stage.setFinishedAt(finished);
            stage.setState(terminalState);
            stage.setElapsedMs(elapsed);
            stage.setHttpStatusCode(statusCode);
            stage.setHttpElapsedMs(elapsed);
            stageDurations.put(requestUuid, elapsed);
            downstreamWaitMs += elapsed;
            addHttpEvent("HTTP_REQUEST_" + terminalState, stage, statusCode, error);
            lastHeartbeatAt = finished.getTime();
            finishIfReady();
        }

        synchronized List<String> finish(String terminalState, String error) {
            if (finishedAt != 0) {
                return new ArrayList<>();
            }
            pendingTerminalState = terminalState;
            pendingTerminalError = error;
            List<String> detachedStageUuids = new ArrayList<>(stages.keySet());
            for (ExecutionStageInventory stage : new ArrayList<>(stages.values())) {
                if (stages.remove(stage.getStageUuid(), stage)) {
                    detachStage(stage);
                }
            }
            finishIfReady();
            return detachedStageUuids;
        }

        private void detachStage(ExecutionStageInventory stage) {
            Timestamp finished = now();
            long elapsed = Math.max(0, finished.getTime() - stage.getStartedAt().getTime());
            stage.setFinishedAt(finished);
            stage.setState("DETACHED");
            stage.setElapsedMs(elapsed);
            stageDurations.put(stage.getStageUuid(), elapsed);
            if ("HTTP".equals(stage.getKind())) {
                stage.setHttpElapsedMs(elapsed);
                downstreamWaitMs += elapsed;
                addHttpEvent("HTTP_REQUEST_DETACHED", stage, null, DETACHED_STAGE_DETAILS);
            } else {
                addEvent("STAGE_DETACHED", stage.getStageUuid(), DETACHED_STAGE_DETAILS, stage);
            }
            lastHeartbeatAt = finished.getTime();
        }

        private void finishIfReady() {
            if (finishedAt != 0 || pendingTerminalState == null || !stages.isEmpty()) {
                return;
            }
            if (startedAt == 0) {
                startedAt = System.currentTimeMillis();
            }
            finishedAt = System.currentTimeMillis();
            lastHeartbeatAt = finishedAt;
            error = pendingTerminalError;
            state = pendingTerminalState;
            addEvent("EXECUTION_" + pendingTerminalState, pendingTerminalError);
        }

        synchronized boolean matches(APIQueryExecutionMsg query) {
            if (query.getExecutionUuid() != null && !query.getExecutionUuid().equals(executionUuid)) return false;
            if (query.getApiUuid() != null && !query.getApiUuid().equals(apiUuid)) return false;
            if (query.getMessageUuid() != null && !observedMessageUuids.contains(query.getMessageUuid())) return false;
            if (query.getTaskRunUuid() != null && !query.getTaskRunUuid().equals(taskRunUuid)) return false;
            if (query.getTriggerType() != null && !query.getTriggerType().equals(triggerType)) return false;
            if (query.getTriggerName() != null && !query.getTriggerName().equals(triggerName)) return false;
            if (query.getState() != null && !query.getState().equals(state)) return false;
            if (query.getNodeUuid() != null && !query.getNodeUuid().equals(nodeUuid)) return false;
            if (query.getMinExecutionDurationMs() != null
                    && elapsedMs() < query.getMinExecutionDurationMs()) return false;
            Timestamp after = parseTimestamp(query.getStartedAfter());
            Timestamp before = parseTimestamp(query.getStartedBefore());
            if (after != null && acceptedAt < after.getTime()) return false;
            if (before != null && acceptedAt > before.getTime()) return false;
            return true;
        }

        private long elapsedMs() {
            return (finishedAt == 0 ? System.currentTimeMillis() : finishedAt) - acceptedAt;
        }

        private boolean isRootMessage(String messageId) {
            return messageId != null && messageId.equals(messageUuid);
        }

        private boolean isRootMessageExecution(String messageId) {
            return "MESSAGE".equals(triggerType) && isRootMessage(messageId);
        }

        synchronized ExecutionInventory inventory(String detail) {
            ExecutionInventory inventory = new ExecutionInventory();
            inventory.setExecutionUuid(executionUuid);
            inventory.setApiUuid(apiUuid);
            inventory.setMessageUuid(messageUuid);
            inventory.setTaskRunUuid(taskRunUuid);
            inventory.setRootMessageUuid(messageUuid);
            inventory.setNodeUuid(nodeUuid);
            inventory.setOperationId(messageUuid == null ? executionUuid : messageUuid);
            inventory.setTraceId(traceId);
            inventory.setApiName(apiName);
            inventory.setRequestKind(requestKind);
            inventory.setNoReply(noReply);
            inventory.setState(state);
            ExecutionTriggerInventory trigger = new ExecutionTriggerInventory();
            trigger.setType(triggerType);
            trigger.setName(triggerName);
            trigger.setApiUuid(apiUuid);
            trigger.setTaskRunUuid(taskRunUuid);
            trigger.setMessageUuid(messageUuid);
            inventory.setTrigger(trigger);
            inventory.setAcceptedAt(new Timestamp(acceptedAt));
            inventory.setStartedAt(startedAt == 0 ? null : new Timestamp(startedAt));
            inventory.setFinishedAt(finishedAt == 0 ? null : new Timestamp(finishedAt));
            inventory.setLastHeartbeatAt(new Timestamp(lastHeartbeatAt));
            inventory.setObservedAt(now());
            inventory.setElapsedMs(startedAt == 0 ? null : elapsedMs());
            inventory.setQueueWaitMs(startedAt == 0 ? null : startedAt - acceptedAt);
            inventory.setExecutionMs(startedAt == 0 ? null : (finishedAt == 0 ? System.currentTimeMillis() : finishedAt) - startedAt);
            inventory.setDownstreamWaitMs(downstreamWaitMs);
            List<ExecutionStageInventory> active = new ArrayList<>();
            stages.values().forEach(stage -> active.add(copyStage(stage)));
            inventory.setActiveStages(active);
            String requestedDetail = detail == null ? "summary" : detail;
            if ("summary".equals(requestedDetail)) {
                inventory.setEvents(new ArrayList<ExecutionEventInventory>());
            } else if ("criticalPath".equals(requestedDetail)) {
                inventory.setEvents(criticalPathEvents());
            } else {
                inventory.setEvents(new ArrayList<>(events));
            }
            inventory.setPartial(false);
            inventory.setSourceNodes(new ArrayList<>(java.util.Collections.singletonList(nodeUuid)));
            inventory.setVisibility("LOCAL");
            inventory.setAttempt(1);
            inventory.setError(error);
            inventory.setTruncated(false);
            return inventory;
        }

        private ExecutionStageInventory copyStage(ExecutionStageInventory source) {
            ExecutionStageInventory stage = new ExecutionStageInventory();
            stage.setStageUuid(source.getStageUuid());
            stage.setParentStageUuid(source.getParentStageUuid());
            stage.setName(source.getName());
            stage.setKind(source.getKind());
            stage.setState(source.getState());
            stage.setNodeUuid(source.getNodeUuid());
            stage.setWaitingOn(source.getWaitingOn());
            stage.setWaitingReason(source.getWaitingReason());
            stage.setStartedAt(source.getStartedAt());
            stage.setFinishedAt(source.getFinishedAt());
            stage.setElapsedMs(source.getElapsedMs());
            stage.setNoReply(source.getNoReply());
            stage.setHttpMethod(source.getHttpMethod());
            stage.setHttpUrl(source.getHttpUrl());
            stage.setHttpTaskUuid(source.getHttpTaskUuid());
            stage.setHttpStatusCode(source.getHttpStatusCode());
            stage.setHttpElapsedMs(source.getHttpElapsedMs());
            return stage;
        }

        private void addHttpEvent(String type, ExecutionStageInventory stage, Integer statusCode, String details) {
            ExecutionEventInventory event = new ExecutionEventInventory();
            event.setSequence(sequence.incrementAndGet());
            event.setTimestamp(now());
            event.setType(type);
            event.setStageUuid(stage.getStageUuid());
            event.setStageName(stage.getName());
            event.setStageKind(stage.getKind());
            event.setNodeUuid(nodeUuid);
            event.setMessageUuid(stage.getStageUuid());
            event.setParentStageUuid(stage.getParentStageUuid());
            event.setDetails(details);
            event.setHttpMethod(stage.getHttpMethod());
            event.setHttpUrl(stage.getHttpUrl());
            event.setHttpTaskUuid(stage.getHttpTaskUuid());
            event.setHttpStatusCode(statusCode);
            event.setHttpElapsedMs(stage.getHttpElapsedMs());
            events.add(event);
            if (events.size() > 256) {
                events.remove(0);
            }
        }

        synchronized boolean isFinished() {
            return finishedAt != 0;
        }

        private static final class StagePath {
            private final long elapsedMs;
            private final List<String> stageUuids;

            private StagePath(long elapsedMs, List<String> stageUuids) {
                this.elapsedMs = elapsedMs;
                this.stageUuids = stageUuids;
            }
        }

        private List<ExecutionEventInventory> criticalPathEvents() {
            Map<String, String> parents = new HashMap<>();
            Map<String, Long> durations = new HashMap<>();
            Set<String> stageUuids = new java.util.HashSet<>();

            for (ExecutionEventInventory event : events) {
                String stageUuid = event.getStageUuid();
                if (stageUuid == null) {
                    continue;
                }
                stageUuids.add(stageUuid);
                parents.putIfAbsent(stageUuid, event.getParentStageUuid());
            }
            durations.putAll(stageDurations);
            for (ExecutionStageInventory stage : stages.values()) {
                stageUuids.add(stage.getStageUuid());
                parents.putIfAbsent(stage.getStageUuid(), stage.getParentStageUuid());
                long elapsed = stage.getElapsedMs() == null
                        ? Math.max(0, System.currentTimeMillis() - stage.getStartedAt().getTime())
                        : stage.getElapsedMs();
                durations.merge(stage.getStageUuid(), elapsed, Math::max);
            }

            Map<String, List<String>> children = new HashMap<>();
            for (String stageUuid : stageUuids) {
                String parent = parents.get(stageUuid);
                if (parent != null && stageUuids.contains(parent)) {
                    children.computeIfAbsent(parent, ignored -> new ArrayList<>()).add(stageUuid);
                }
            }

            Map<String, StagePath> memo = new HashMap<>();
            StagePath best = null;
            for (String stageUuid : stageUuids) {
                String parent = parents.get(stageUuid);
                if (parent == null || !stageUuids.contains(parent)) {
                    StagePath candidate = criticalPathFrom(stageUuid, children, durations, memo,
                            new java.util.HashSet<>());
                    if (best == null || candidate.elapsedMs > best.elapsedMs) {
                        best = candidate;
                    }
                }
            }
            if (best == null) {
                for (String stageUuid : stageUuids) {
                    StagePath candidate = criticalPathFrom(stageUuid, children, durations, memo,
                            new java.util.HashSet<>());
                    if (best == null || candidate.elapsedMs > best.elapsedMs) {
                        best = candidate;
                    }
                }
            }

            Set<String> criticalStages = best == null
                    ? java.util.Collections.emptySet() : new java.util.HashSet<>(best.stageUuids);
            return events.stream()
                    .filter(event -> event.getStageUuid() == null || criticalStages.contains(event.getStageUuid()))
                    .collect(Collectors.toList());
        }

        private StagePath criticalPathFrom(String stageUuid, Map<String, List<String>> children,
                                           Map<String, Long> durations, Map<String, StagePath> memo,
                                           Set<String> visiting) {
            StagePath cached = memo.get(stageUuid);
            if (cached != null) {
                return cached;
            }
            if (!visiting.add(stageUuid)) {
                return new StagePath(0, new ArrayList<>());
            }

            long ownDuration = durations.getOrDefault(stageUuid, 0L);
            StagePath bestChild = null;
            for (String child : children.getOrDefault(stageUuid, java.util.Collections.emptyList())) {
                StagePath candidate = criticalPathFrom(child, children, durations, memo, visiting);
                if (bestChild == null || candidate.elapsedMs > bestChild.elapsedMs) {
                    bestChild = candidate;
                }
            }
            visiting.remove(stageUuid);

            List<String> path = new ArrayList<>();
            path.add(stageUuid);
            long elapsed = ownDuration;
            if (bestChild != null) {
                path.addAll(bestChild.stageUuids);
                elapsed += bestChild.elapsedMs;
            }
            StagePath result = new StagePath(elapsed, path);
            memo.put(stageUuid, result);
            return result;
        }

        private void addEvent(String type, String details) {
            addEvent(type, null, details);
        }

        private void addEvent(String type, String stageUuid, String details) {
            ExecutionEventInventory event = new ExecutionEventInventory();
            event.setSequence(sequence.incrementAndGet());
            event.setTimestamp(now());
            event.setType(type);
            event.setStageUuid(stageUuid);
            event.setNodeUuid(nodeUuid);
            event.setMessageUuid(stageUuid == null ? messageUuid : stageUuid);
            ExecutionStageInventory stage = stageUuid == null ? null : stages.get(stageUuid);
            if (stage != null) {
                event.setStageName(stage.getName());
                event.setStageKind(stage.getKind());
                event.setParentStageUuid(stage.getParentStageUuid());
            }
            event.setDetails(details);
            event.setNoReply(noReply);
            events.add(event);
            if (events.size() > 256) {
                events.remove(0);
            }
        }

        private void addEvent(String type, String stageUuid, String details, ExecutionStageInventory stage) {
            ExecutionEventInventory event = new ExecutionEventInventory();
            event.setSequence(sequence.incrementAndGet());
            event.setTimestamp(now());
            event.setType(type);
            event.setStageUuid(stageUuid);
            event.setNodeUuid(nodeUuid);
            event.setMessageUuid(stageUuid == null ? messageUuid : stageUuid);
            event.setStageName(stage == null ? null : stage.getName());
            event.setStageKind(stage == null ? null : stage.getKind());
            event.setParentStageUuid(stage == null ? null : stage.getParentStageUuid());
            event.setDetails(details);
            event.setNoReply(stage == null ? null : stage.getNoReply());
            events.add(event);
            if (events.size() > 256) {
                events.remove(0);
            }
        }
    }

    public String getId() {
        return "executionObservability";
    }

    @Override
    public boolean start() {
        cleanupQueued.set(false);
        if (!isObservationEnabled()) {
            return true;
        }
        observationExecutor = new ThreadPoolExecutor(
                1, 1, 60, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(OBSERVATION_QUEUE_SIZE),
                new ObservationThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy());
        bus.installBeforeDeliveryMessageInterceptor(this);
        return true;
    }

    @Override
    public boolean stop() {
        ThreadPoolExecutor executor = observationExecutor;
        observationExecutor = null;
        if (executor != null) {
            executor.shutdownNow();
        }
        cleanupQueued.set(false);
        executions.clear();
        executionOrders.clear();
        executionCounts.clear();
        messageToExecution.clear();
        messageToStage.clear();
        ignoredExecutionContexts.clear();
        httpToExecution.clear();
        scheduledThreadContexts.clear();
        return true;
    }

    private static final class ObservationThreadFactory implements ThreadFactory {
        private final AtomicLong sequence = new AtomicLong();

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable,
                    "zs-execution-observation-" + sequence.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }
}
