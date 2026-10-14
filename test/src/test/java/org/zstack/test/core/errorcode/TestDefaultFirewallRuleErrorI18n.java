package org.zstack.test.core.errorcode;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.errorcode.GlobalErrorCodeI18nServiceImpl;
import org.zstack.header.errorcode.ErrorCode;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Properties;

public class TestDefaultFirewallRuleErrorI18n {
    private static final String CODE = "ORG_ZSTACK_VPCFIREWALL_10019";
    private static final String UUID = "c1b5602f2eb24f18a4937818e784c13f";
    private static final String ENGLISH = "cannot change the state of default rule[" + UUID + "]";
    private static final String CHINESE = "默认规则[" + UUID + "]不支持修改启停状态";
    private GlobalErrorCodeI18nServiceImpl service;

    @Before
    public void setUp() {
        service = new GlobalErrorCodeI18nServiceImpl();
        service.start();
    }

    @Test
    public void testAllSupportedLocalesFormatRuleUuid() {
        String[] locales = {"en_US", "zh_CN", "zh_TW", "de-DE", "fr-FR",
                "id-ID", "ja-JP", "ko-KR", "ru-RU", "th-TH"};
        for (String locale : locales) {
            Assert.assertTrue(locale, service.getAvailableLocales().contains(locale));
            String message = service.getLocalizedMessage(CODE, locale, new String[]{UUID});
            Assert.assertNotNull(locale, message);
            Assert.assertTrue(locale + ": " + message, message.contains(UUID));
            Assert.assertFalse(locale + ": " + message, message.contains("%s"));
        }
    }

    @Test
    public void testRestrictionMessageAndEnglishFallback() {
        Assert.assertEquals(CHINESE, service.getLocalizedMessage(CODE, "zh_CN", new String[]{UUID}));
        Assert.assertEquals(ENGLISH, service.getLocalizedMessage(CODE, "en_US", new String[]{UUID}));
        Assert.assertEquals(ENGLISH, service.getLocalizedMessage(CODE, "pt_BR", new String[]{UUID}));
    }

    @Test
    public void testRestDetailsAreLocalizedWithoutChangingRawError() {
        ErrorCode error = new ErrorCode("SYS.1007", "One or more API argument is invalid",
                "cannot change the state of default rule[" + UUID + "]");
        error.setGlobalErrorCode(CODE);
        error.setFormatArgs(new String[]{UUID});

        ErrorCode localized = service.localizeErrorCodeDetails(error, "zh_CN");
        Assert.assertEquals(CHINESE, localized.getMessage());
        Assert.assertEquals(CHINESE, localized.getDetails());
        Assert.assertEquals("SYS.1007", localized.getCode());
        Assert.assertEquals(CODE, localized.getGlobalErrorCode());
        Assert.assertArrayEquals(new String[]{UUID}, localized.getFormatArgs());
        Assert.assertEquals(ENGLISH, error.getDetails());

        localized = service.localizeErrorCodeDetails(error, "en_US");
        Assert.assertEquals(ENGLISH, localized.getMessage());
        Assert.assertEquals(ENGLISH, localized.getDetails());
    }

    @Test
    public void testLegacyMessagesMatchGlobalErrorMapping() throws Exception {
        assertLegacyMessage("en_US", ENGLISH);
        assertLegacyMessage("zh_CN", CHINESE);
    }

    private void assertLegacyMessage(String locale, String expected) throws Exception {
        String path = "/i18n/messages_" + locale + ".properties";
        Assert.assertNotNull(path, getClass().getResource(path));
        Properties messages = new Properties();
        try (InputStreamReader reader = new InputStreamReader(
                getClass().getResourceAsStream(path), StandardCharsets.UTF_8)) {
            messages.load(reader);
        }
        String template = messages.getProperty("cannot change the state of default rule[%s]");
        Assert.assertNotNull(path, template);
        Assert.assertEquals(expected, MessageFormat.format(template, UUID));
    }
}
