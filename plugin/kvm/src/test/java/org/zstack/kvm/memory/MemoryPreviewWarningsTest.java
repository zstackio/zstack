package org.zstack.kvm.memory;

import org.junit.Test;
import org.zstack.core.Platform;
import org.zstack.utils.gson.JSONObjectUtil;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.net.URL;
import java.net.URLClassLoader;
import java.lang.reflect.Method;
import java.util.Locale;

import org.springframework.context.support.ResourceBundleMessageSource;

import static org.junit.Assert.*;

public class MemoryPreviewWarningsTest {
    @Test
    public void warningDetailsExposeStableCodeAndPlatformLocalizedMessageAfterJsonSerialization() throws Exception {
        Path root = Paths.get("").toAbsolutePath();
        while (root != null && !Files.isRegularFile(root.resolve("conf/i18n/messages_zh_CN.properties"))) {
            root = root.getParent();
        }
        assertNotNull("repository root with platform message bundles not found", root);
        ClassLoader loader = Platform.class.getClassLoader();
        assertTrue(loader instanceof URLClassLoader);
        Method addUrl = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
        addUrl.setAccessible(true);
        addUrl.invoke(loader, root.resolve("conf").toUri().toURL());

        Field field = Platform.class.getDeclaredField("messageSource");
        field.setAccessible(true);
        Object previous = field.get(null);
        ResourceBundleMessageSource messages = new ResourceBundleMessageSource();
        messages.setBasename("i18n.messages");
        messages.setDefaultEncoding("UTF-8");
        messages.setBeanClassLoader(loader);
        field.set(null, messages);
        try {
            java.util.Map<String, Double> errorsBefore = new java.util.HashMap<>(Platform.getErrorCounter());
            String[][] expectations = {
                    {"VM_EXCLUSION_GLOBAL_SWAP", "内存优化的 VM 排除项只限制受管回收，不会关闭内核全局交换。", "VM exclusions limit managed reclamation; they do not disable kernel global swap."},
                    {"HOST_REVALIDATED_BEFORE_APPLY", "执行前会重新检查主机接口、容量和生效版本。", "Host ABI, capacity, and effective revision are checked again before execution."},
                    {"CLOUD_LICENSE_REQUIRED", "当前云授权不允许修改内存优化配置。", "The current Cloud License does not permit memory configuration changes."},
                    {"ZRAM_CAPACITY_UNVERIFIED", "无法验证主机容量默认值；启用 ZRAM 前请提供有效的显式容量。", "Host capacity defaults could not be verified; provide explicit validated capacity before enabling ZRAM."}
            };
            for (String[] expected : expectations) {
                MemoryPreviewWarningInventory zh = MemoryPreviewWarnings.create(expected[0], Locale.SIMPLIFIED_CHINESE);
                MemoryPreviewWarningInventory en = MemoryPreviewWarnings.create(expected[0], Locale.US);
                assertEquals(expected[0], zh.getCode());
                assertEquals(expected[1], zh.getMessage());
                assertEquals(expected[2], en.getMessage());
                assertNotNull(zh.getMessageKey());
                assertNotNull(zh.getFormatArgs());
                MemoryPreviewWarningInventory serialized = JSONObjectUtil.toObject(
                        JSONObjectUtil.toJsonString(zh), MemoryPreviewWarningInventory.class);
                assertEquals(expected[0], serialized.getCode());
                assertEquals(expected[1], serialized.getMessage());
                assertEquals(zh.getMessageKey(), serialized.getMessageKey());
                assertEquals(zh.getFormatArgs(), serialized.getFormatArgs());
            }
            MemoryPreviewWarningInventory example = MemoryPreviewWarningInventory.__example__();
            MemoryPreviewWarningInventory registered = MemoryPreviewWarnings.create(example.getCode(), Locale.US);
            assertEquals("documentation must use a registered warning key",
                    registered.getMessageKey(), example.getMessageKey());
            assertEquals(registered.getFormatArgs(), example.getFormatArgs());
            assertEquals(MemoryPreviewWarnings.legacyMessage(example.getCode()), example.getMessage());
            assertEquals("preview warnings must not increment platform error counters",
                    errorsBefore, Platform.getErrorCounter());
        } finally {
            field.set(null, previous);
        }
    }

    @Test
    public void unknownWarningCodeFailsClosed() {
        try {
            MemoryPreviewWarnings.create("UNKNOWN", Locale.US);
            fail("unknown warning code must not silently invent a message");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("UNKNOWN"));
        }
    }
}
