package org.zstack.kvm.memory;

import org.junit.Test;
import org.springframework.context.support.StaticMessageSource;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.PluginRegistry;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.function.Consumer;

import static org.junit.Assert.*;

/** Real manager preview callback: an incomplete successful envelope is not a resolved default. */
public class MemoryPreviewCapacityCallbackTest {
    private static final String HOST = "0123456789abcdef0123456789abcdef";
    private static final String POLICY = "{\"zram\":{\"enabled\":true,\"logicalCapacityBytes\":8589934592}}";

    @Test public void missingDefaultProducesWarningAndDoesNotChangeEitherPolicy() throws Exception {
        APIPreviewMemoryPolicyReply reply = preview(Collections.emptyMap());
        assertEquals(POLICY, reply.getInventory().getPolicy());
        assertEquals(POLICY, reply.getInventory().getEffectivePolicy());
        assertTrue(MemoryPreviewRules.needsCapacity(reply.getInventory()));
        assertEquals(1, reply.getWarningDetails().stream()
                .filter(warning -> "ZRAM_CAPACITY_UNVERIFIED".equals(warning.getCode())).count());
        assertTrue(reply.getWarnings().contains(MemoryPreviewWarnings.legacyMessage("ZRAM_CAPACITY_UNVERIFIED")));
    }

    @Test public void knownRequiredDefaultResolvesWithoutWarningOrOverwritingExplicitCapacity() throws Exception {
        APIPreviewMemoryPolicyReply reply = preview(Collections.singletonMap("ramLimitBytes", 4L << 30));
        MemoryPolicyConfig policy = MemoryPolicyRules.decode(reply.getInventory().getEffectivePolicy());
        assertEquals(Long.valueOf(8L << 30), policy.zram.logicalCapacityBytes);
        assertEquals(Long.valueOf(4L << 30), policy.zram.ramLimitBytes);
        assertFalse(MemoryPreviewRules.needsCapacity(reply.getInventory()));
        assertEquals(0, reply.getWarningDetails().stream()
                .filter(warning -> "ZRAM_CAPACITY_UNVERIFIED".equals(warning.getCode())).count());
    }

    private static APIPreviewMemoryPolicyReply preview(Map<String, Object> defaults) throws Exception {
        List<APIPreviewMemoryPolicyReply> replies = new ArrayList<>();
        List<String> calls = new ArrayList<>();
        MemoryOptimizationManager manager = new MemoryOptimizationManager() {
            @Override void call(String host, String action, Map<String, Object> command,
                                Consumer<MemoryAgentResponse> callback) {
                assertEquals(HOST, host);
                calls.add(action);
                MemoryAgentResponse response = new MemoryAgentResponse();
                response.operationUuid = (String) command.get("operationUuid");
                response.state = Collections.singletonMap("policyDefaults", Collections.singletonMap("zram", defaults));
                callback.accept(response);
            }
        };
        MemoryPolicyInventory inventory = new MemoryPolicyInventory();
        inventory.setScope("Host"); inventory.setResourceUuid(HOST);
        inventory.setPolicy(POLICY); inventory.setEffectivePolicy(POLICY);
        MemoryRepository repository = new MemoryRepository() {
            @Override public MemoryPolicyInventory preview(String scope, String resource, String policy,
                                                            String action, List<String> fields) { return inventory; }
            @Override public List<MemoryStateVO> states(List<String> hosts, int start, int limit) {
                return Collections.emptyList();
            }
            @Override public List<String> targets(String scope, String resource) {
                return Collections.singletonList(HOST);
            }
            @Override public Map<String, MemoryHostPolicyPreviewSnapshot> previewHostPolicies(
                    String scope, String resource, String policy, String action, List<String> fields, List<String> hosts) {
                return Collections.singletonMap(HOST, new MemoryHostPolicyPreviewSnapshot(POLICY, POLICY, null));
            }
        };
        inject(manager, "repository", repository);
        inject(manager, "bus", Proxy.newProxyInstance(CloudBus.class.getClassLoader(), new Class<?>[]{CloudBus.class},
                (proxy, method, args) -> {
                    if ("reply".equals(method.getName())) replies.add((APIPreviewMemoryPolicyReply) args[1]);
                    return null;
                }));
        MemoryLicenseExtensionPoint license = () -> Long.MAX_VALUE;
        inject(manager, "pluginRgty", Proxy.newProxyInstance(PluginRegistry.class.getClassLoader(),
                new Class<?>[]{PluginRegistry.class}, (proxy, method, args) ->
                        "getExtensionList".equals(method.getName()) ? Collections.singletonList(license) : null));
        Field messageSource = Platform.class.getDeclaredField("messageSource");
        messageSource.setAccessible(true);
        Object previous = messageSource.get(null);
        StaticMessageSource messages = new StaticMessageSource();
        messages.setUseCodeAsDefaultMessage(true);
        messageSource.set(null, messages);
        try {
            APIPreviewMemoryPolicyMsg msg = new APIPreviewMemoryPolicyMsg();
            msg.setScope("Host"); msg.setResourceUuid(HOST); msg.setPolicy("{}");
            Method preview = MemoryOptimizationManager.class.getDeclaredMethod("preview", APIPreviewMemoryPolicyMsg.class);
            preview.setAccessible(true); preview.invoke(manager, msg);
        } finally {
            messageSource.set(null, previous);
        }
        assertEquals("one reply, no apply/write call", 1, replies.size());
        assertEquals(Collections.singletonList("preview"), calls);
        return replies.get(0);
    }

    private static void inject(Object target, String name, Object value) throws Exception {
        Field field = MemoryOptimizationManager.class.getDeclaredField(name);
        field.setAccessible(true); field.set(target, value);
    }
}
