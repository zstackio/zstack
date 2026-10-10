package org.zstack.kvm.memory;

import org.junit.Test;
import org.zstack.rest.RestServer;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Exercises the official REST documentation DSL against the typed memory DTO templates. */
public class MemoryInventoryDocTemplateTest {
    @Test
    public void allTypedInventoryAndReplyTemplatesParseIncludingNestedDtos() throws Exception {
        Path sourceRoot = sourceRoot();
        String[] names = {
                "MemoryUncertainRecovery", "MemoryBackendPreparation", "MemoryZramPoolPreparation",
                "MemoryVmAccountingInventory", "MemoryVmAccountingMetricsInventory",
                "MemoryVmIdentityInventory", "MemoryHostOperationsInventory",
                "MemoryHostOperationsInventory_Entry", "MemoryHostOperationsInventory_Concurrency",
                "MemoryHostOperationsInventory_RecordBudget", "APIGetVmMemoryOptimizationReply",
                "APIGetVmMemoryOptimizationsReply", "APIQueryHostMemoryOperationsReply",
                "MemoryWritebackBackendInventory", "MemoryWritebackBackendCandidateInventory",
                "MemoryWritebackStableIdentityInventory", "MemoryWritebackMaintenanceInventory",
                "MemoryWritebackMaintenanceTargetIdentityInventory", "APIGetHostMemoryWritebackBackendsReply"
        };

        Class<?> generatorClass = org.zstack.utils.GroovyUtils.getClass(
                "scripts/RestDocumentationGenerator.groovy", RestServer.class.getClassLoader());
        Object generator = allocateWithoutConstructor(generatorClass);
        Method createDoc = generatorClass.getMethod("createDoc", String.class);
        for (String name : names) {
            Path template = sourceRoot.resolve(name + "Doc_zh_cn.groovy");
            assertTrue("missing official template " + template, Files.isRegularFile(template));
            Object parsed;
            try {
                parsed = createDoc.invoke(generator, template.toAbsolutePath().toString());
            } catch (java.lang.reflect.InvocationTargetException e) {
                throw new AssertionError(name + " failed to parse with the platform REST DSL",
                        e.getTargetException());
            }
            assertNotNull(name + " produced no Doc", parsed);
        }
    }

    @Test
    public void platformNestedTemplateNamingMatchesTheCommittedFiles() throws Exception {
        Class<?> generatorClass = org.zstack.utils.GroovyUtils.getClass(
                "scripts/RestDocumentationGenerator.groovy", RestServer.class.getClassLoader());
        Object generator = allocateWithoutConstructor(generatorClass);
        java.lang.reflect.Field language = generatorClass.getDeclaredField("CHINESE_CN");
        language.setAccessible(true);
        language.set(generator, "zh_cn");
        Method makeFileName = generatorClass.getMethod("makeFileNameForChinese", Class.class);
        assertEquals("MemoryHostOperationsInventory_EntryDoc_zh_cn.groovy",
                makeFileName.invoke(generator, MemoryHostOperationsInventory.Entry.class));
        assertEquals("MemoryHostOperationsInventory_ConcurrencyDoc_zh_cn.groovy",
                makeFileName.invoke(generator, MemoryHostOperationsInventory.Concurrency.class));
        assertEquals("MemoryHostOperationsInventory_RecordBudgetDoc_zh_cn.groovy",
                makeFileName.invoke(generator, MemoryHostOperationsInventory.RecordBudget.class));
    }

    private static Path sourceRoot() {
        for (String candidate : new String[]{
                "src/main/java/org/zstack/kvm/memory",
                "plugin/kvm/src/main/java/org/zstack/kvm/memory",
                "../plugin/kvm/src/main/java/org/zstack/kvm/memory",
                "../../plugin/kvm/src/main/java/org/zstack/kvm/memory"}) {
            Path path = Paths.get(candidate);
            if (Files.isDirectory(path)) {
                return path;
            }
        }
        throw new AssertionError("memory API source directory not found");
    }

    private static Object allocateWithoutConstructor(Class<?> type) throws Exception {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        java.lang.reflect.Field singleton = unsafeClass.getDeclaredField("theUnsafe");
        singleton.setAccessible(true);
        Object unsafe = singleton.get(null);
        return unsafeClass.getMethod("allocateInstance", Class.class).invoke(unsafe, type);
    }
}
