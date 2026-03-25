package org.zstack.core.rest.webhook;

import org.zstack.core.Platform;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.thread.ThreadFacadeImpl;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.rest.RESTFacade;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.zstack.core.Platform.operr;

/**
 * Generic async callback client for external systems that use a webhook pattern:
 * <ol>
 *   <li>Send an HTTP request to the external system</li>
 *   <li>External system returns immediately (e.g. 202 Accepted)</li>
 *   <li>External system later POSTs back the result to a callback URL</li>
 *   <li>This client matches the callback to the original request and completes it</li>
 * </ol>
 *
 * <p>Callback flow:</p>
 * <pre>
 * External Controller
 *     │  POST /asyncrest/sendcommand  (commandpath header → callbackPath)
 *     ▼
 * AsyncRESTCallController.sendCommand()
 *     ▼
 * RESTFacadeImpl.sendCommand()  →  httpCallhandlers.get(callbackPath)
 *     ▼
 * WebhookCallbackClient.onCallback(T cmd)
 *     ├─ protocol.extractTaskId(cmd)
 *     ├─ pendingCalls.remove(taskId)  ← CAS point (atomic, prevents double invocation)
 *     ├─ cancel timeout
 *     └─ protocol.isSuccess(cmd) ? completion.success(cmd) : completion.fail(error)
 * </pre>
 *
 * <p>This class is a plain POJO — not a Spring bean. It is created and owned by the
 * plugin-specific API client (e.g. ZnsApiClient) which passes in its dependencies.</p>
 *
 * <p>Thread safety: {@code ConcurrentHashMap.remove()} serves as the CAS point.
 * Among callback arrival, timeout, and send-failure, only one can successfully remove
 * and thus complete a pending call.</p>
 *
 * @param <T> the callback body type
 */
public class WebhookCallbackClient<T> {
    private static final CLogger logger = Utils.getLogger(WebhookCallbackClient.class);

    private final WebhookProtocol<T> protocol;
    private final RESTFacade restf;
    private final ThreadFacade thdf;
    private final ConcurrentHashMap<String, PendingEntry<T>> pendingCalls = new ConcurrentHashMap<>();
    private String callbackUrl;

    private static class PendingEntry<T> {
        final ReturnValueCompletion<T> completion;
        final ThreadFacadeImpl.TimeoutTaskReceipt timeoutReceipt;

        PendingEntry(ReturnValueCompletion<T> completion,
                     ThreadFacadeImpl.TimeoutTaskReceipt timeoutReceipt) {
            this.completion = completion;
            this.timeoutReceipt = timeoutReceipt;
        }
    }

    public WebhookCallbackClient(WebhookProtocol<T> protocol, RESTFacade restf, ThreadFacade thdf) {
        this.protocol = protocol;
        this.restf = restf;
        this.thdf = thdf;
    }

    /**
     * Register the callback handler on the sendCommand channel.
     * Must be called once during the owning component's start() lifecycle.
     */
    public void start() {
        this.callbackUrl = restf.getSendCommandUrl();
        restf.registerSyncHttpCallHandler(
                protocol.getCallbackPath(),
                protocol.getCallbackClass(),
                this::onCallback);
    }

    /**
     * Register a pending call and return its task identifier.
     *
     * <p>The caller should use the returned taskId to decorate the outgoing request
     * headers via {@link WebhookProtocol#decorateRequest}, then send the HTTP request.
     * If the send fails, the caller must invoke {@link #fail} to clean up.</p>
     *
     * @param completion the completion to invoke when the callback arrives (or on timeout)
     * @param unit       timeout time unit
     * @param timeout    timeout value
     * @return the generated taskId
     */
    public String submit(ReturnValueCompletion<T> completion, TimeUnit unit, long timeout) {
        String taskId = Platform.getUuid();

        ThreadFacadeImpl.TimeoutTaskReceipt timeoutReceipt = thdf.submitTimeoutTask(() -> {
            fail(taskId, operr("[Webhook Timeout] callback timed out for taskId[%s], path[%s]",
                    taskId, protocol.getCallbackPath()));
        }, unit, timeout);

        pendingCalls.put(taskId, new PendingEntry<>(completion, timeoutReceipt));
        return taskId;
    }

    /**
     * Actively fail a pending call (e.g. when the HTTP send fails).
     *
     * <p>{@code ConcurrentHashMap.remove()} is atomic — only one of
     * (callback / timeout / send-failure) can win, preventing double invocation.</p>
     */
    public void fail(String taskId, ErrorCode error) {
        PendingEntry<T> entry = pendingCalls.remove(taskId);
        if (entry != null) {
            entry.timeoutReceipt.cancel();
            entry.completion.fail(error);
        }
    }

    /**
     * @return the callback URL that the external system should POST results to
     */
    public String getCallbackUrl() {
        return callbackUrl;
    }

    /**
     * @return the protocol adapter
     */
    public WebhookProtocol<T> getProtocol() {
        return protocol;
    }

    /**
     * Callback handler invoked by the RESTFacade sendCommand channel.
     */
    private String onCallback(T cmd) {
        String taskId = protocol.extractTaskId(cmd);
        if (taskId == null) {
            logger.warn(String.format("received webhook callback without taskId on path[%s], ignoring",
                    protocol.getCallbackPath()));
            return null;
        }

        PendingEntry<T> entry = pendingCalls.remove(taskId);
        if (entry == null) {
            logger.warn(String.format("received webhook callback for unknown taskId[%s] on path[%s], ignoring",
                    taskId, protocol.getCallbackPath()));
            return null;
        }

        entry.timeoutReceipt.cancel();

        if (protocol.isSuccess(cmd)) {
            entry.completion.success(cmd);
        } else {
            String error = protocol.extractError(cmd);
            entry.completion.fail(operr("webhook callback failed for taskId[%s], path[%s], error: %s",
                    taskId, protocol.getCallbackPath(), error != null ? error : "unknown"));
        }

        return null;
    }
}

