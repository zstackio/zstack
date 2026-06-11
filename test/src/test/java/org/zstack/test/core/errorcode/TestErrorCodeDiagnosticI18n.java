package org.zstack.test.core.errorcode;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.errorcode.GlobalErrorCodeI18nService;
import org.zstack.core.errorcode.GlobalErrorCodeI18nServiceImpl;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeDiagnostic;
import org.zstack.header.errorcode.ErrorCodeDiagnosticHelper;

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
}
