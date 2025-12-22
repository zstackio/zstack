package org.zstack.compute.host;

import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.retry.Retry;
import org.zstack.core.retry.RetryCondition;
import org.zstack.core.thread.Task;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.host.*;
import org.zstack.header.rest.RESTFacade;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

public class HostReleaseNotificationExtension implements HostChangeStateExtensionPoint {
    private static final CLogger logger = Utils.getLogger(HostReleaseNotificationExtension.class);

    @Autowired
    private ThreadFacade thdf;

    @Autowired
    private RESTFacade restf;

    public HostReleaseNotificationExtension() {
    }

    @Override
    public void preChangeHostState(HostInventory inventory, HostStateEvent event, HostState nextState) throws HostException {
        // nothing
    }

    @Override
    public void beforeChangeHostState(HostInventory inventory, HostStateEvent event, HostState nextState) {
        // nothing
    }

    @Override
    public void afterChangeHostState(HostInventory inventory, HostStateEvent event, HostState previousState) {
        try {
            if (!CoreGlobalProperty.ENABLE_RELEASE_API) {
                return;
            }
            handleNotificationTrigger(inventory);
        } catch (Throwable t) {
            logger.warn(String.format("Error in notification trigger for host[%s]", inventory.getUuid()), t);
        }
    }

    private void handleNotificationTrigger(HostInventory inventory) {
        if (!HostState.Disabled.toString().equals(inventory.getState())) {
            return;
        }

        if (!validateConfig()) {
            return;
        }

        thdf.submit(new Task<Void>() {
            @Override
            public String getName() {
                return String.format("notify-host-release-%s", inventory.getUuid());
            }

            @Override
            public Void call() throws Exception {
                try {
                    sendHttpRequest(inventory.getUuid());
                } catch (Exception e) {
                    logger.warn(String.format("Failed to notify host[%s]: %s", inventory.getUuid(), e.getMessage()), e);
                }
                return null;
            }
        });
    }

    private void sendHttpRequest(String hostUuid) throws Exception {
        String fullUrl = CoreGlobalProperty.HANGKONG_API;
        String accessKeyId = CoreGlobalProperty.HANGKONG_ACCESSKEY_ID;
        String accessKeySecret = CoreGlobalProperty.HANGKONG_ACCESSKEY_SECRET;

        String productUuid = getProductUuid(hostUuid);

        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        URI uri = new URI(fullUrl);

        Map<String, String> bodyMap = new HashMap<>();
        bodyMap.put("productUuid", productUuid);
        String jsonBody = JSONObjectUtil.toJsonString(bodyMap);

        String signature = generateSignature("POST", uri.getPath(), null, jsonBody, accessKeyId, accessKeySecret, timestamp);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Access-Key", accessKeyId);
        headers.set("X-Timestamp", timestamp);
        headers.set("X-Signature", signature);

        HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);

        logger.debug(String.format("Notifying host[%s]...", hostUuid));

        ResponseEntity<String> rsp = new Retry<ResponseEntity<String>>() {
            @Override
            @RetryCondition(onExceptions = {IOException.class, HttpStatusCodeException.class})
            protected ResponseEntity<String> call() {
                return restf.getRESTTemplate().exchange(fullUrl, HttpMethod.POST, request, String.class);
            }
        }.run();

        logger.debug(String.format("Notification response: %s", rsp.getBody()));
    }

    private boolean validateConfig() {
        if (CoreGlobalProperty.HANGKONG_API == null || CoreGlobalProperty.HANGKONG_API.isEmpty() ||
                CoreGlobalProperty.HANGKONG_ACCESSKEY_ID == null ||
                CoreGlobalProperty.HANGKONG_ACCESSKEY_SECRET == null ||
                CoreGlobalProperty.HANGKONG_ACCESSKEY_ID.isEmpty() ||
                CoreGlobalProperty.HANGKONG_ACCESSKEY_SECRET.isEmpty()) {
            logger.debug("External API config incomplete, skipping notification.");
            return false;
        }
        return true;
    }

    private String getProductUuid(String hostUuid) {
        if (HostSystemTags.SYSTEM_UUID.hasTag(hostUuid)) {
            Map<String, String> tokens = HostSystemTags.SYSTEM_UUID.getTokensByResourceUuid(hostUuid);
            if (tokens != null) {
                return tokens.getOrDefault(HostSystemTags.SYSTEM_UUID_TOKEN, hostUuid);
            }
        }
        return hostUuid;
    }

    private String generateSignature(String method, String path, Map<String, String> params,
                                     String body, String accessKeyId, String accessKeySecret, String timestamp) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append(method.toUpperCase()).append("\n");
        sb.append(path).append("\n");
        sb.append(buildQueryString(params)).append("\n");
        sb.append(body != null && !body.isEmpty() ? sha256Hex(body) : "").append("\n");
        sb.append(accessKeyId).append("\n");
        sb.append(timestamp);
        return hmacSha256Hex(accessKeySecret, sb.toString());
    }

    private String buildQueryString(Map<String, String> params) {
        if (params == null || params.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        params.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> {
                    if (sb.length() > 0) sb.append("&");
                    sb.append(e.getKey()).append("=").append(e.getValue());
                });
        return sb.toString();
    }

    private String sha256Hex(String data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return Hex.encodeHexString(digest.digest(data.getBytes(StandardCharsets.UTF_8)));
    }

    private String hmacSha256Hex(String key, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Hex.encodeHexString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }
}