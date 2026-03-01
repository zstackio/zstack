package org.zstack.test.core.errorcode;

import org.junit.Assert;
import org.junit.Test;
import org.zstack.core.errorcode.LocaleUtils;

import java.util.HashSet;
import java.util.Set;

public class TestLocaleUtils {
    private static final Set<String> AVAILABLE_LOCALES = new HashSet<>();

    static {
        AVAILABLE_LOCALES.add("zh_CN");
        AVAILABLE_LOCALES.add("en_US");
        AVAILABLE_LOCALES.add("ja-JP");
        AVAILABLE_LOCALES.add("ko-KR");
        AVAILABLE_LOCALES.add("de-DE");
        AVAILABLE_LOCALES.add("fr-FR");
        AVAILABLE_LOCALES.add("ru-RU");
        AVAILABLE_LOCALES.add("th-TH");
        AVAILABLE_LOCALES.add("id-ID");
        AVAILABLE_LOCALES.add("zh_TW");
    }

    @Test
    public void testSimpleLocale() {
        String result = LocaleUtils.resolveLocale("zh-CN", AVAILABLE_LOCALES);
        Assert.assertEquals("zh_CN", result);
    }

    @Test
    public void testMultiLocaleWithQuality() {
        String result = LocaleUtils.resolveLocale("ja-JP,zh-CN;q=0.9,en;q=0.8", AVAILABLE_LOCALES);
        Assert.assertEquals("ja-JP", result);
    }

    @Test
    public void testLanguageCodeOnly() {
        String result = LocaleUtils.resolveLocale("zh", AVAILABLE_LOCALES);
        Assert.assertEquals("zh_CN", result);
    }

    @Test
    public void testUnsupportedLocale() {
        String result = LocaleUtils.resolveLocale("pt-BR", AVAILABLE_LOCALES);
        Assert.assertEquals("en_US", result);
    }

    @Test
    public void testNullAndEmpty() {
        Assert.assertEquals("en_US", LocaleUtils.resolveLocale(null, AVAILABLE_LOCALES));
        Assert.assertEquals("en_US", LocaleUtils.resolveLocale("", AVAILABLE_LOCALES));
    }

    @Test
    public void testEnUS() {
        String result = LocaleUtils.resolveLocale("en-US", AVAILABLE_LOCALES);
        Assert.assertEquals("en_US", result);
    }

    @Test
    public void testUnderscoreFormat() {
        String result = LocaleUtils.resolveLocale("zh_CN", AVAILABLE_LOCALES);
        Assert.assertEquals("zh_CN", result);
    }
}
