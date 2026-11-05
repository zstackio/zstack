package org.zstack.test.core.errorcode;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.errorcode.GlobalErrorCodeI18nService;
import org.zstack.core.errorcode.GlobalErrorCodeI18nServiceImpl;
import org.zstack.core.errorcode.LocaleUtils;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeDiagnostic;
import org.zstack.header.errorcode.ErrorCodeDiagnosticHelper;

import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.*;

public class TestErrorCodeDiagnosticI18n {
    private GlobalErrorCodeI18nService i18nService;

    @Before
    public void setUp() throws Exception {
        GlobalErrorCodeI18nServiceImpl service = new GlobalErrorCodeI18nServiceImpl();
        service.start();
        i18nService = service;
    }

    @Test
    public void testVmNicLifecycleErrorCodeI18nAndDiagnostic() {
        ErrorCode error = new ErrorCode("SYS.1006", "Operation Error",
                "vm nic lifecycle setup failed while selecting applicable NICs: demo");
        error.setGlobalErrorCode("ORG_ZSTACK_COMPUTE_VM_10331");
        error.setFormatArgs(new String[]{"demo"});

        i18nService.localizeErrorCode(error, "zh_CN");
        Assert.assertTrue(error.getMessage().contains("虚拟机网卡生命周期"));

        ErrorCodeDiagnostic zhDiagnostic = ErrorCodeDiagnosticHelper.toDiagnostic(error);
        Assert.assertEquals("Cloud", zhDiagnostic.getComponent());
        Assert.assertEquals("VM", zhDiagnostic.getCategory());
        Assert.assertEquals("10331", zhDiagnostic.getCode());
        Assert.assertEquals(error.getMessage(), zhDiagnostic.getMessage());
        Assert.assertNull(zhDiagnostic.getRawMessage().getCause());
        Assert.assertNull(zhDiagnostic.getRawMessage().getSolution());

        i18nService.localizeErrorCode(error, "en_US");
        Assert.assertTrue(error.getMessage().contains("VM NIC lifecycle setup failed"));

        i18nService.localizeErrorCode(error, "pt_BR");
        Assert.assertTrue("unsupported locale should fallback to en_US",
                error.getMessage().contains("VM NIC lifecycle setup failed"));
    }

    @Test
    public void testStopVmWithoutHostI18nAndDiagnostic() {
        assertStopErrorLocalized(ORG_ZSTACK_COMPUTE_VM_10349,
                new String[]{"测试VM-100%", "vm-uuid"},
                "无法停止云主机[名称:测试VM-100%, uuid:vm-uuid]：hostUuid 为空，无法确定当前所在的物理机",
                "cannot stop vm[name:测试VM-100%, uuid:vm-uuid]: hostUuid is null, cannot determine its current host");
    }

    @Test
    public void testStopVmWithMissingHostI18nAndDiagnostic() {
        assertStopErrorLocalized(ORG_ZSTACK_COMPUTE_VM_10350,
                new String[]{"测试VM-100%", "vm-uuid", "host-uuid"},
                "无法停止云主机[名称:测试VM-100%, uuid:vm-uuid]：物理机[uuid:host-uuid]不存在",
                "cannot stop vm[name:测试VM-100%, uuid:vm-uuid]: host[uuid:host-uuid] does not exist");
    }

    private void assertStopErrorLocalized(String globalErrorCode, String[] args,
                                          String chineseMessage, String englishMessage) {
        ErrorCode error = new ErrorCode("SYS.1006", "Operation Error", englishMessage);
        error.setGlobalErrorCode(globalErrorCode);
        error.setFormatArgs(args);

        String locale = LocaleUtils.resolveLocale("zh-CN", i18nService.getAvailableLocales());
        Assert.assertEquals("zh_CN", locale);
        i18nService.localizeErrorCode(error, locale);
        Assert.assertEquals(chineseMessage, error.getMessage());
        Assert.assertEquals(englishMessage, error.getDetails());
        Assert.assertEquals("SYS.1006", error.getCode());
        Assert.assertEquals(globalErrorCode, error.getGlobalErrorCode());
        Assert.assertArrayEquals(args, error.getFormatArgs());

        ErrorCodeDiagnostic diagnostic = ErrorCodeDiagnosticHelper.toDiagnostic(error);
        Assert.assertEquals("Cloud", diagnostic.getComponent());
        Assert.assertEquals("VM", diagnostic.getCategory());
        Assert.assertEquals(globalErrorCode.substring(globalErrorCode.lastIndexOf('_') + 1),
                diagnostic.getCode());
        Assert.assertEquals(chineseMessage, diagnostic.getMessage());

        Assert.assertEquals(chineseMessage, i18nService.localizeErrorCodeDetails(error, locale).getDetails());
        Assert.assertEquals(englishMessage, i18nService.getLocalizedMessage(globalErrorCode, "en_US", args));
        Assert.assertEquals(englishMessage, i18nService.getLocalizedMessage(globalErrorCode, "pt_BR", args));
    }
}
