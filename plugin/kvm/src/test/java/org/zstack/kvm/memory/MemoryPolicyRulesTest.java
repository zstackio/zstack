package org.zstack.kvm.memory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;
import java.lang.reflect.InvocationTargetException;
import static org.junit.Assert.*;

/** Boundary tests intentionally exercise the same JSON entry used by the MN. */
public class MemoryPolicyRulesTest {
    private String merge(String current, String patch, String scope) throws Exception {
        Class<?> rules;
        try {
            rules = Class.forName("org.zstack.kvm.memory.MemoryPolicyRules");
        } catch (ClassNotFoundException e) {
            throw new AssertionError("Memory policy validation is not implemented", e);
        }
        try {
            return (String) rules.getMethod("merge", String.class, String.class, String.class)
                    .invoke(null, current, patch, scope);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof IllegalArgumentException) {
                throw (IllegalArgumentException) e.getCause();
            }
            throw e;
        }
    }

    private void reject(String patch, String scope) throws Exception {
        try {
            merge("{}", patch, scope);
            fail("Invalid policy accepted: " + patch);
        } catch (IllegalArgumentException expected) {
            assertNotNull(expected.getMessage());
        }
    }

    @Test public void partialUpdatePreservesExistingFields() throws Exception {
        String result = merge("{\"ksm\":{\"enabled\":true,\"zeroPagesEnabled\":true}}",
                "{\"zram\":{\"enabled\":true}}", "Host");
        JsonObject config = new JsonParser().parse(result).getAsJsonObject();
        assertTrue(config.getAsJsonObject("ksm").get("zeroPagesEnabled").getAsBoolean());
        assertTrue(config.getAsJsonObject("ksm").get("enabled").getAsBoolean());
        assertTrue(config.getAsJsonObject("zram").get("enabled").getAsBoolean());
    }

    @Test public void nullAndUnknownFieldsAreRejected() throws Exception {
        reject("{\"ksm\":null}", "Host");
        reject("{\"ksm\":{\"zeroPagesEnabled\":null}}", "Host");
        reject("{\"ksm\":{\"command\":\"echo 2\"}}", "Host");
        reject("{\"backendPath\":\"/dev/sda\"}", "Host");
    }

    @Test public void booleansAndIntegersAreNotCoerced() throws Exception {
        reject("{\"zram\":{\"enabled\":\"true\"}}", "Host");
        reject("{\"ksm\":{\"pagesToScan\":1.5}}", "Host");
        reject("{\"ksm\":{\"pagesToScan\":true}}", "Host");
    }

    @Test public void capacityAndWritebackHaveBounds() throws Exception {
        reject("{\"zram\":{\"ramLimitBytes\":4097}}", "Host");
        reject("{\"zram\":{\"logicalCapacityBytes\":-4096}}", "Host");
        merge("{}", "{\"zram\":{\"logicalCapacityBytes\":1099511631872}}", "Host");
        merge("{}", "{\"writeback\":{\"batchBytes\":67112960}}", "Host");
        reject("{\"zram\":{\"ramLimitBytes\":9223372036854775808}}", "Host");
        reject("{\"writeback\":{\"enabled\":true}}", "Host");
    }

    @Test public void vmCannotModifyHostSettings() throws Exception {
        reject("{\"ksm\":{\"enabled\":true}}", "VM");
        reject("{\"participation\":\"always\"}", "VM");
        String result = merge("{}", "{\"participation\":\"deny\"}", "VM");
        assertEquals("deny", new JsonParser().parse(result).getAsJsonObject()
                .get("participation").getAsString());
        reject("{\"participation\":\"deny\"}", "Host");
    }

    @Test public void advancedConfigurationDoesNotInventExperimentalDefaults() throws Exception {
        JsonObject config = new JsonParser().parse(merge("{}", "{}", "Global")).getAsJsonObject();
        assertFalse(config.has("requiredAvailableBytes"));
        assertFalse(config.has("zram") && config.getAsJsonObject("zram").has("requiredAvailableBytes"));
        assertFalse(config.has("writeback") && config.getAsJsonObject("writeback").has("enabled")
                && config.getAsJsonObject("writeback").get("enabled").getAsBoolean());
    }
}
