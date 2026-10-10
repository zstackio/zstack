package org.zstack.kvm.memory;

import org.junit.Test;
import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Smoke-checks representative DTO examples and the platform's generic-type resolver without a WAR. */
public class MemorySdkNestedExampleSmokeTest {
    @Test
    public void replyAndInventoryExamplesPopulateRepresentativeTypedModels() {
        MemoryVmAccountingInventory vm = MemoryVmAccountingInventory.__example__();
        assertEquals("partial_unattributed_present", vm.getQuality());
        assertNotNull(vm.getIdentity());
        assertNotNull(vm.getMetrics());
        assertEquals("116424049", vm.getIdentity().getStart_time());
        assertEquals("1051626769722038414", vm.getIdentity().getPool_generation());
        assertEquals(Long.valueOf(20137L), vm.getMetrics().getCompressed_pages());

        MemoryHostOperationsInventory host = MemoryHostOperationsInventory.__example__();
        assertEquals(1, host.getEntries().size());
        assertEquals("", host.getEntries().get(0).getVm_uuid());
        assertNotNull(host.getCq());
        assertNotNull(host.getRecord_budget());
        assertEquals("partial_unattributed_present",
                APIGetVmMemoryOptimizationReply.__example__().getInventory().getQuality());
        assertEquals("03d98313faa24670a0944b10da8d1ac2",
                APIGetVmMemoryOptimizationsReply.__example__().getInventories().keySet().iterator().next());
        assertEquals("writeback", APIQueryHostMemoryOperationsReply.__example__()
                .getInventory().getEntries().get(0).getKind());

        com.google.gson.Gson gson = new com.google.gson.Gson();
        com.google.gson.JsonObject batchJson = gson.toJsonTree(
                APIGetVmMemoryOptimizationsReply.__example__()).getAsJsonObject();
        String vmUuid = batchJson.getAsJsonObject("inventories").keySet().iterator().next();
        com.google.gson.JsonObject vmJson = batchJson.getAsJsonObject("inventories").getAsJsonObject(vmUuid);
        assertEquals(20137L, vmJson.getAsJsonObject("metrics").get("compressedPages").getAsLong());
        assertEquals("116424049",
                vmJson.getAsJsonObject("identity").get("startTime").getAsString());
        com.google.gson.JsonObject hostJson = gson.toJsonTree(
                APIQueryHostMemoryOperationsReply.__example__()).getAsJsonObject().getAsJsonObject("inventory");
        assertEquals("", hostJson.getAsJsonArray("entries").get(0).getAsJsonObject().get("vmUuid").getAsString());
        assertTrue(hostJson.has("concurrency"));
        assertTrue(hostJson.has("recordBudget"));
    }

    @Test
    public void sdkGenericTypeResolutionFindsMapValuesAndNestedModelElements() throws Exception {
        Field vmMap = APIGetVmMemoryOptimizationsReply.class.getDeclaredField("inventories");
        assertEquals(MemoryVmAccountingInventory.class,
                org.zstack.utils.FieldUtils.getGenericType(vmMap));
        Field entries = MemoryHostOperationsInventory.class.getDeclaredField("entries");
        assertEquals(MemoryHostOperationsInventory.Entry.class,
                org.zstack.utils.FieldUtils.getGenericType(entries));

        // The Java SDK generator consumes FieldUtils.getGenericType() to queue these classes;
        // the Go generator independently maps generic Map values and List elements through
        // generateFieldGeneric()/generateFieldType() into typed view structs.
        assertTrue(java.lang.reflect.Modifier.isStatic(MemoryHostOperationsInventory.Entry.class.getModifiers()));
        assertTrue(java.lang.reflect.Modifier.isStatic(MemoryHostOperationsInventory.Concurrency.class.getModifiers()));
        assertTrue(java.lang.reflect.Modifier.isStatic(MemoryHostOperationsInventory.RecordBudget.class.getModifiers()));
    }
}
