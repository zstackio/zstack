package org.zstack.compute.host;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.header.host.*;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.springframework.stereotype.Component;

@Component
public class HostReleaseNotificationExtension implements HostChangeStateExtensionPoint {
    private static final CLogger logger = Utils.getLogger(HostReleaseNotificationExtension.class);
    private static final ExecutorService notificationExecutor = Executors.newFixedThreadPool(2);

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
        if (!CoreGlobalProperty.ENABLE_RELEASE_API) {
            return;
        }
        if (HostState.Disabled.equals(HostState.valueOf(inventory.getState()))) {
            notifyExternalSystem(inventory.getUuid(), inventory.getName());
        }
    }

    private void notifyExternalSystem(String hostUuid, String hostName) {
        CompletableFuture.runAsync(() -> {
            try {
                String url = CoreGlobalProperty.HANGKONG_API;
                String token = CoreGlobalProperty.HANGKONG_ACCESSKEY;

                if (url == null || url.trim().isEmpty()) {
                    return;
                }

                SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
                factory.setConnectTimeout(5000);
                factory.setReadTimeout(10000);
                RestTemplate restTemplate = new RestTemplate(factory);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                if (token != null && !token.trim().isEmpty()) {
                    headers.set("Authorization", token);
                }

                Map<String, Object> body = new HashMap<>();
                body.put("hostUuid", hostUuid);
                body.put("hostName", hostName);
                body.put("state", "Disabled");
                body.put("action", "Release");
                body.put("timestamp", System.currentTimeMillis());

                HttpEntity<String> request = new HttpEntity<>(JSONObjectUtil.toJsonString(body), headers);

                logger.debug(String.format("Async notifying external system for host[%s] release...", hostUuid));
                String response = restTemplate.postForObject(url, request, String.class);
                logger.debug(String.format("External system notification result: %s", response));

            } catch (Exception e) {
                logger.warn(String.format("Failed to notify external system for host[%s]. Error: %s", hostUuid, e.getMessage()));
            }
        }, notificationExecutor);
    }
}