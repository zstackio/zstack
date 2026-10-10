package org.zstack.kvm.memory;

import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;
import org.zstack.core.Platform;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.errorcode.ErrorFacadeImpl;
import org.zstack.core.errorcode.GlobalErrorCodeI18nServiceImpl;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.utils.gson.JSONObjectUtil;
import org.springframework.context.MessageSource;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URLClassLoader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class MemoryOperationExceptionI18nTest {
    private static ErrorFacadeImpl errorFacade;
    private static GlobalErrorCodeI18nServiceImpl i18n;
    private static Object previousLoader;
    private static Object previousMessageSource;
    private static Path productRoot;

    @BeforeClass
    public static void initializePlatformErrorServices() throws Exception {
        addProductConfigToTestClasspath();
        errorFacade = new ErrorFacadeImpl();
        Method init = ErrorFacadeImpl.class.getDeclaredMethod("init");
        init.setAccessible(true);
        init.invoke(errorFacade);
        i18n = new GlobalErrorCodeI18nServiceImpl();
        i18n.start();
        assertTrue(i18n.getAvailableLocales().contains("en_US"));
        assertTrue(i18n.getAvailableLocales().contains("zh_CN"));

        Field loaderField = Platform.class.getDeclaredField("loader");
        loaderField.setAccessible(true);
        previousLoader = loaderField.get(null);
        ComponentLoader loader = (ComponentLoader) Proxy.newProxyInstance(
                ComponentLoader.class.getClassLoader(), new Class<?>[]{ComponentLoader.class},
                (proxy, method, args) -> {
                    if ("getComponent".equals(method.getName()) && args != null && args.length == 1) {
                        if (args[0] == ErrorFacade.class) return errorFacade;
                        if (args[0] == org.zstack.core.errorcode.GlobalErrorCodeI18nService.class) return i18n;
                    }
                    return null;
                });
        loaderField.set(null, loader);

        Field messageSourceField = Platform.class.getDeclaredField("messageSource");
        messageSourceField.setAccessible(true);
        previousMessageSource = messageSourceField.get(null);
        MessageSource messageSource = (MessageSource) Proxy.newProxyInstance(
                MessageSource.class.getClassLoader(), new Class<?>[]{MessageSource.class},
                (proxy, method, args) -> {
                    if ("getMessage".equals(method.getName())) {
                        Object[] formatArgs = (Object[]) args[1];
                        return formatArgs == null || formatArgs.length == 0 ? args[0]
                                : String.format((String) args[0], formatArgs);
                    }
                    return null;
                });
        messageSourceField.set(null, messageSource);
    }

    private static void addProductConfigToTestClasspath() throws Exception {
        Path root = Paths.get("").toAbsolutePath();
        while (root != null && !Files.isRegularFile(root.resolve("conf/errorCodes/memory.xml"))) {
            root = root.getParent();
        }
        assertNotNull("repository root with product conf not found", root);
        productRoot = root;
        ClassLoader classLoader = org.zstack.utils.path.PathUtil.class.getClassLoader();
        assertTrue("test classloader must expose URL classpath", classLoader instanceof URLClassLoader);
        Method addUrl = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
        addUrl.setAccessible(true);
        addUrl.invoke(classLoader, root.resolve("conf").toUri().toURL());
    }

    @AfterClass
    public static void restorePlatformStatics() throws Exception {
        Field loaderField = Platform.class.getDeclaredField("loader");
        loaderField.setAccessible(true);
        loaderField.set(null, previousLoader);
        Field messageSourceField = Platform.class.getDeclaredField("messageSource");
        messageSourceField.setAccessible(true);
        messageSourceField.set(null, previousMessageSource);
    }

    @Test
    public void revisionConflictUsesDeclaredMetadataAndLocalizes() {
        assertLocalized("MEMORY_REVISION_CONFLICT", 11034,
                "Inherited policy changed; preview and confirm again",
                "Revision Conflict in memory optimization",
                "内存优化策略版本冲突：Inherited policy changed; preview and confirm again");
    }

    @Test
    public void invalidPolicyUsesDeclaredMetadataAndLocalizes() {
        assertLocalized("MEMORY_INVALID_POLICY", 11026,
                "Writeback backend must be configured per host",
                "Invalid Policy in memory optimization",
                "内存优化策略配置无效：Writeback backend must be configured per host");
    }

    @Test
    public void everyMemoryErrorHasChineseCategoryWithoutEmbeddedEnglishLabels() throws Exception {
        Path mapping = productRoot.resolve("conf/i18n/globalErrorCodeMapping/global-error-zh_CN.json");
        assertTrue("Chinese global error mapping not found", Files.isRegularFile(mapping));
        String json = new String(Files.readAllBytes(mapping), StandardCharsets.UTF_8);
        Pattern entry = Pattern.compile("\\\"ORG_ZSTACK_MEMORY_(?:1[1][0-4][0-9]{2}|11999)\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"");
        Matcher matcher = entry.matcher(json);
        int count = 0;
        while (matcher.find()) {
            String value = matcher.group(1).replace("%s", "").replace("%d", "");
            value = value.replace("ZRAM", "");
            assertFalse("English category text remains in Chinese mapping: " + value,
                    Pattern.compile("(?i)\\b[a-z]{2,}\\b").matcher(value).find());
            count++;
        }
        assertEquals("all memory error categories must be checked", 47, count);
    }

    @Test
    public void unknownBusinessCodeRetainsCodeAndDiagnostic() {
        String diagnostic = "unrecognized backend rejection detail";
        ErrorCode code = new MemoryOperationException("MEMORY_FUTURE_REASON", diagnostic).toErrorCode();
        assertEquals("MEMORY_FUTURE_REASON", code.getCode());
        assertEquals(diagnostic, code.getDetails());
        assertEquals("ORG_ZSTACK_MEMORY_11999", code.getGlobalErrorCode());
        assertArrayEquals(new String[]{diagnostic}, code.getFormatArgs());

        i18n.localizeErrorCode(code, "en_US");
        assertTrue(code.getMessage().contains(diagnostic));
        i18n.localizeErrorCode(code, "zh_CN");
        assertTrue(code.getMessage().contains("未知的内存优化错误"));
        assertTrue(code.getMessage().contains(diagnostic));
        ErrorCode serialized = JSONObjectUtil.toObject(JSONObjectUtil.toJsonString(code), ErrorCode.class);
        assertEquals("MEMORY_FUTURE_REASON", serialized.getCode());
        assertEquals(diagnostic, serialized.getDetails());
        assertEquals("ORG_ZSTACK_MEMORY_11999", serialized.getGlobalErrorCode());
        assertArrayEquals(new String[]{diagnostic}, serialized.getFormatArgs());
    }

    @Test
    public void everyMemorySourceCodeHasUniqueDeclaredMetadata() throws Exception {
        Path source = productRoot.resolve("plugin/kvm/src/main/java/org/zstack/kvm/memory");
        assertTrue("memory source directory not found", Files.isDirectory(source));
        Pattern pattern = Pattern.compile("\\\"(MEMORY_[A-Z0-9_]+)\\\"");
        Set<String> businessCodes = new HashSet<>();
        try (java.util.stream.Stream<Path> paths = Files.walk(source)) {
            paths.filter(path -> path.toString().endsWith(".java")).forEach(path -> {
                try {
                    String text = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
                    Matcher matcher = pattern.matcher(text);
                    while (matcher.find()) {
                        String businessCode = matcher.group(1);
                        if ("MEMORY_ERROR".equals(businessCode)
                                || "MEMORY_UNKNOWN_ERROR".equals(businessCode)) continue;
                        businessCodes.add(businessCode);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
        Set<Integer> ids = new HashSet<>();
        for (String businessCode : businessCodes) {
            MemoryErrors error = MemoryErrors.fromBusinessCode(businessCode);
            assertNotSame("missing metadata mapping for " + businessCode, MemoryErrors.UNKNOWN, error);
            assertTrue("metadata id must be unique: " + error, ids.add(error.getId()));
            assertNotNull(errorFacade.instantiateErrorCode(error.toString(), businessCode));
        }
        assertEquals(46, businessCodes.size());
    }

    @Test
    public void taskCleanupErrorsHaveLocalizedCategoriesAndPreserveDiagnostics() {
        assertLocalized("MEMORY_TASK_NOT_ROOT", 11042, "child task UUID",
                "Memory task deletion requires a root task", "只能删除内存任务的根任务");
        assertLocalized("MEMORY_TASK_REFERENCED", 11043, "activeTaskUuid still references task",
                "Memory task is still referenced", "内存任务仍被运行状态或历史操作引用");
        assertLocalized("MEMORY_REQUEST_RESULT_PURGED", 11044, "Original task [abc], status [Succeeded]",
                "Memory task result was purged", "该内存任务结果已清理且不会重放");
        assertLocalized("MEMORY_SERVICE_NOT_STOPPED", 11045, "controller is still active",
                "Memory optimization service has not stopped", "内存优化服务尚未停止");
    }

    @Test public void lifecycleErrorsUseTheSameRegisteredMetadataAndFallback() throws Exception {
        Method method = MemoryLifecycleHooks.class.getDeclaredMethod("error", String.class, String.class);
        method.setAccessible(true);
        for (String businessCode : new String[]{"MEMORY_RESULT_UNKNOWN", "MEMORY_FUTURE_LIFECYCLE_ERROR"}) {
            ErrorCode expected = new MemoryOperationException(businessCode, "same-boot identity unresolved").toErrorCode();
            ErrorCode actual = (ErrorCode) method.invoke(new MemoryLifecycleHooks(), businessCode, "same-boot identity unresolved");
            assertEquals(expected.getCode(), actual.getCode());
            assertEquals(expected.getDescription(), actual.getDescription());
            assertEquals(expected.getGlobalErrorCode(), actual.getGlobalErrorCode());
            assertArrayEquals(expected.getFormatArgs(), actual.getFormatArgs());
            assertEquals(expected.getDetails(), actual.getDetails());
            assertTrue(actual.getGlobalErrorCode(), actual.getGlobalErrorCode().startsWith("ORG_ZSTACK_MEMORY_"));
        }
    }

    private void assertLocalized(String businessCode, int metadataId, String diagnostic,
                                 String english, String chinese) {
        ErrorCode code = new MemoryOperationException(businessCode, diagnostic).toErrorCode();
        assertEquals(businessCode, code.getCode());
        assertEquals(metadataDescription(metadataId), code.getDescription());
        assertEquals(diagnostic, code.getDetails());
        assertEquals("ORG_ZSTACK_MEMORY_" + metadataId, code.getGlobalErrorCode());
        assertArrayEquals(new String[]{diagnostic}, code.getFormatArgs());
        assertTrue(Platform.getErrorCounter().containsKey("MEMORY_ERROR"));

        i18n.localizeErrorCode(code, "en_US");
        assertTrue(code.getMessage(), code.getMessage().contains(english));
        i18n.localizeErrorCode(code, "zh_CN");
        assertTrue(code.getMessage(), code.getMessage().contains(chinese));
        assertTrue(code.getMessage(), code.getMessage().contains(diagnostic));
        ErrorCode serialized = JSONObjectUtil.toObject(JSONObjectUtil.toJsonString(code), ErrorCode.class);
        assertEquals(businessCode, serialized.getCode());
        assertEquals(diagnostic, serialized.getDetails());
        assertEquals("ORG_ZSTACK_MEMORY_" + metadataId, serialized.getGlobalErrorCode());
        assertTrue(serialized.getMessage(), serialized.getMessage().contains(diagnostic));
    }

    private String metadataDescription(int metadataId) {
        if (metadataId == 11034) {
            return "Revision Conflict in memory optimization";
        }
        if (metadataId == 11026) {
            return "Invalid Policy in memory optimization";
        }
        if (metadataId == 11042) { return "Memory Task Delete Requires Root Task"; }
        if (metadataId == 11043) { return "Memory Task Is Still Referenced"; }
        if (metadataId == 11044) { return "Memory Task Result Has Been Purged"; }
        if (metadataId == 11045) { return "Memory Optimization Service Has Not Stopped"; }
        throw new AssertionError("unexpected metadata id " + metadataId);
    }
}
