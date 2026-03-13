package org.zstack.core.rest.webhook;

import java.util.Map;

/**
 * Defines the protocol adaptation for an external system's webhook callback mechanism.
 *
 * <p>Different external controllers (e.g., SDN controllers) use different header conventions,
 * callback body formats, and success/failure semantics. This interface abstracts those
 * differences so that {@link WebhookCallbackClient} can handle the common async lifecycle
 * (pending call registration, timeout, CAS-guarded callback dispatch) generically.</p>
 *
 * @param <T> the callback body type that the external system POSTs back
 */
public interface WebhookProtocol<T> {

    /**
     * The path to register on the sendCommand channel (e.g. "/zns/callback").
     * This will be passed to {@code RESTFacade.registerSyncHttpCallHandler}.
     */
    String getCallbackPath();

    /**
     * The class used to deserialize the callback JSON body.
     */
    Class<T> getCallbackClass();

    /**
     * Extract the task identifier from the callback body.
     * This must match the taskId returned by {@link WebhookCallbackClient#submit}.
     */
    String extractTaskId(T callback);

    /**
     * Determine whether the callback indicates a successful operation.
     */
    boolean isSuccess(T callback);

    /**
     * Extract a human-readable error description from the callback body.
     * Called only when {@link #isSuccess} returns false.
     */
    String extractError(T callback);

    /**
     * Decorate the outgoing HTTP request headers with the task identifier and callback URL,
     * following the conventions of the external system.
     *
     * @param headers     mutable map to add headers to
     * @param taskId      the unique task identifier for this async call
     * @param callbackUrl the URL the external system should POST the result to
     */
    void decorateRequest(Map<String, String> headers, String taskId, String callbackUrl);
}

