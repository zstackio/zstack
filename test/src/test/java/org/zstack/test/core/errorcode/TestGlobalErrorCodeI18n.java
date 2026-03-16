package org.zstack.test.core.errorcode;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.errorcode.GlobalErrorCodeI18nService;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.test.BeanConstructor;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Arrays;

public class TestGlobalErrorCodeI18n {
    CLogger logger = Utils.getLogger(TestGlobalErrorCodeI18n.class);
    ComponentLoader loader;
    GlobalErrorCodeI18nService i18nService;

    @Before
    public void setUp() throws Exception {
        BeanConstructor con = new BeanConstructor();
        loader = con.build();
        i18nService = loader.getComponent(GlobalErrorCodeI18nService.class);
    }

    @Test
    public void testLoadJsonFiles() {
        Assert.assertTrue("should load at least 2 locales",
                i18nService.getAvailableLocales().size() >= 2);
        logger.debug(String.format("loaded locales: %s", i18nService.getAvailableLocales()));
    }

    @Test
    public void testZhCNMatch() {
        String msg = i18nService.getLocalizedMessage(
                "ORG_ZSTACK_STORAGE_PRIMARY_10039", "zh_CN", new String[]{"abc123"});
        Assert.assertNotNull(msg);
        Assert.assertTrue("should contain Chinese text", msg.contains("abc123"));
        logger.debug(String.format("zh_CN message: %s", msg));
    }

    @Test
    public void testEnUSMatch() {
        String msg = i18nService.getLocalizedMessage(
                "ORG_ZSTACK_STORAGE_PRIMARY_10039", "en_US", new String[]{"abc123"});
        Assert.assertNotNull(msg);
        Assert.assertTrue("should contain English text", msg.contains("abc123"));
        logger.debug(String.format("en_US message: %s", msg));
    }

    @Test
    public void testFallbackToEnUS() {
        String msg = i18nService.getLocalizedMessage(
                "ORG_ZSTACK_STORAGE_PRIMARY_10039", "pt_BR", new String[]{"abc123"});
        Assert.assertNotNull("should fallback to en_US", msg);
        logger.debug(String.format("fallback message: %s", msg));
    }

    @Test
    public void testNonExistentGlobalErrorCode() {
        String msg = i18nService.getLocalizedMessage(
                "NOT_EXIST_CODE", "zh_CN", null);
        Assert.assertNull("should return null for non-existent code", msg);
    }

    @Test
    public void testFormatArgsNull() {
        String msg = i18nService.getLocalizedMessage(
                "ORG_ZSTACK_STORAGE_PRIMARY_10039", "zh_CN", null);
        Assert.assertNotNull(msg);
        // template contains %s but args is null, should return raw template
        Assert.assertTrue("should contain %s placeholder", msg.contains("%s"));
    }

    @Test
    public void testFormatArgsMismatch() {
        // template has 1 %s but we pass 3 args - should not crash
        String msg = i18nService.getLocalizedMessage(
                "ORG_ZSTACK_STORAGE_PRIMARY_10039", "zh_CN",
                new String[]{"arg1", "arg2", "arg3"});
        Assert.assertNotNull(msg);
    }

    @Test
    public void testLocalizeErrorCodeWithCause() {
        ErrorCode cause = new ErrorCode();
        cause.setGlobalErrorCode("ORG_ZSTACK_STORAGE_PRIMARY_10039");
        cause.setFormatArgs(new String[]{"inner-uuid"});

        ErrorCode error = new ErrorCode();
        error.setGlobalErrorCode("ORG_ZSTACK_STORAGE_PRIMARY_10039");
        error.setFormatArgs(new String[]{"outer-uuid"});
        error.setCause(cause);

        i18nService.localizeErrorCode(error, "zh_CN");

        Assert.assertNotNull("error.message should be set", error.getMessage());
        Assert.assertNotNull("cause.message should be set", cause.getMessage());
        Assert.assertTrue(error.getMessage().contains("outer-uuid"));
        Assert.assertTrue(cause.getMessage().contains("inner-uuid"));
    }

    @Test
    public void testLocalizeErrorCodeList() {
        ErrorCodeList errorList = new ErrorCodeList();
        errorList.setCode("SYS.1000");

        ErrorCode cause1 = new ErrorCode();
        cause1.setGlobalErrorCode("ORG_ZSTACK_STORAGE_PRIMARY_10039");
        cause1.setFormatArgs(new String[]{"uuid1"});

        ErrorCode cause2 = new ErrorCode();
        cause2.setGlobalErrorCode("ORG_ZSTACK_STORAGE_PRIMARY_10039");
        cause2.setFormatArgs(new String[]{"uuid2"});

        errorList.setCauses(Arrays.asList(cause1, cause2));

        i18nService.localizeErrorCode(errorList, "zh_CN");

        Assert.assertNotNull("cause1.message should be set", cause1.getMessage());
        Assert.assertNotNull("cause2.message should be set", cause2.getMessage());
        Assert.assertTrue(cause1.getMessage().contains("uuid1"));
        Assert.assertTrue(cause2.getMessage().contains("uuid2"));
    }

    @Test
    public void testNoGlobalErrorCode() {
        ErrorCode error = new ErrorCode("SYS.1000", "test error");
        // no globalErrorCode set — message should fall back to description
        i18nService.localizeErrorCode(error, "zh_CN");
        Assert.assertEquals("message should fall back to description",
                "test error", error.getMessage());
    }

    @Test
    public void testMessageGuaranteeFallbackToDetails() {
        ErrorCode error = new ErrorCode("SYS.1000", "System Error", "disk full on /dev/sda1");
        i18nService.localizeErrorCode(error, "en_US");
        Assert.assertEquals("message should fall back to details",
                "disk full on /dev/sda1", error.getMessage());
    }

    @Test
    public void testMessageNeverNull() {
        ErrorCode error = new ErrorCode("SYS.1000", "System Error");
        error.setDetails(null);
        i18nService.localizeErrorCode(error, "en_US");
        Assert.assertNotNull("message must never be null after localizeErrorCode",
                error.getMessage());
        Assert.assertEquals("System Error", error.getMessage());
    }
}