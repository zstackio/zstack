package org.zstack.test.integration.core

import org.zstack.core.errorcode.GlobalErrorCodeI18nServiceImpl
import org.zstack.core.errorcode.LocaleUtils
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.errorcode.ErrorCodeList
import org.zstack.testlib.SubCase

class ErrorCodeI18nCase extends SubCase {

    @Override
    void setup() {
        INCLUDE_CORE_SERVICES = false
    }

    @Override
    void environment() {
    }

    @Override
    void test() {
        testLocaleUtilsExactMatch()
        testLocaleUtilsBaseLanguageFallback()
        testLocaleUtilsQValueSorting()
        testLocaleUtilsNullAndEmpty()
        testLocaleUtilsNoMatch()
        testLocaleUtilsCaseInsensitive()
        testLocaleUtilsMalformedHeader()
        testLocaleUtilsComplexBrowserHeader()
        testErrorCodeCopyConstructor()
        testErrorCodeCopyConstructorWithNulls()
        testMessageGuaranteeFallbackToDetails()
        testMessageGuaranteeFallbackToDescription()
        testMessageGuaranteeOnCauseChain()
    }

    @Override
    void clean() {
    }

    // ---- LocaleUtils ----

    void testLocaleUtilsExactMatch() {
        def available = ["zh_CN", "en_US"] as Set
        assert LocaleUtils.resolveLocale("zh-CN", available) == "zh_CN"
        assert LocaleUtils.resolveLocale("en-US", available) == "en_US"
    }

    void testLocaleUtilsBaseLanguageFallback() {
        def available = ["zh_CN", "en_US"] as Set
        assert LocaleUtils.resolveLocale("en", available) == "en_US"
        assert LocaleUtils.resolveLocale("zh", available) == "zh_CN"
    }

    void testLocaleUtilsQValueSorting() {
        def available = ["zh_CN", "en_US"] as Set
        assert LocaleUtils.resolveLocale("zh-CN,en;q=0.8", available) == "zh_CN"
        assert LocaleUtils.resolveLocale("en-US,zh-CN;q=0.5", available) == "en_US"
        // q-value should override header order
        assert LocaleUtils.resolveLocale("en;q=0.8,zh-CN;q=1.0", available) == "zh_CN"
    }

    void testLocaleUtilsNullAndEmpty() {
        def available = ["zh_CN", "en_US"] as Set
        assert LocaleUtils.resolveLocale(null, available) == "en_US"
        assert LocaleUtils.resolveLocale("", available) == "en_US"
        assert LocaleUtils.resolveLocale("  ", available) == "en_US"
    }

    void testLocaleUtilsNoMatch() {
        def available = ["zh_CN", "en_US"] as Set
        assert LocaleUtils.resolveLocale("ja-JP,ko-KR", available) == "en_US"
    }

    void testLocaleUtilsCaseInsensitive() {
        def available = ["zh_CN", "en_US"] as Set
        assert LocaleUtils.resolveLocale("ZH-CN", available) == "zh_CN"
        assert LocaleUtils.resolveLocale("EN-US", available) == "en_US"
    }

    void testLocaleUtilsMalformedHeader() {
        def available = ["zh_CN", "en_US"] as Set
        // malformed header should fall back to default
        assert LocaleUtils.resolveLocale(";;;,,,", available) == "en_US"
    }

    void testLocaleUtilsComplexBrowserHeader() {
        def available = ["zh_CN", "en_US"] as Set
        // real Chrome header: zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6
        assert LocaleUtils.resolveLocale("zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6", available) == "zh_CN"
        // q=0 means "not acceptable" — should be skipped
        assert LocaleUtils.resolveLocale("zh-CN;q=0,en-US;q=1.0", available) == "en_US"
    }

    // ---- ErrorCode copy constructor ----

    void testErrorCodeCopyConstructor() {
        def original = new ErrorCode("SYS.1000", "System Error", "something failed")
        original.setElaboration("elaboration text")
        original.setLocation("org.zstack.Foo:123")
        original.setCost("50ms")
        original.setGlobalErrorCode("ORG_ZSTACK_FOO_10000")
        original.setMessage("系统错误")
        original.setFormatArgs(["arg1", "arg2"] as String[])

        def opaque = new LinkedHashMap()
        opaque.put("key1", "value1")
        original.setOpaque(opaque)

        def cause = new ErrorCode("INTERNAL.1001", "Internal Error")
        original.setCause(cause)

        def copy = new ErrorCode(original)

        assert copy.code == original.code
        assert copy.description == original.description
        assert copy.details == original.details
        assert copy.elaboration == original.elaboration
        assert copy.location == original.location
        assert copy.cost == original.cost
        assert copy.globalErrorCode == original.globalErrorCode
        assert copy.message == original.message
        assert copy.opaque.is(original.opaque)
        assert copy.cause.is(original.cause)
        // formatArgs should be cloned, not shared
        assert copy.formatArgs == original.formatArgs
        assert !copy.formatArgs.is(original.formatArgs)
    }

    void testErrorCodeCopyConstructorWithNulls() {
        def original = new ErrorCode("SYS.1000", "System Error")
        def copy = new ErrorCode(original)

        assert copy.code == original.code
        assert copy.description == original.description
        assert copy.details == null
        assert copy.cost == null
        assert copy.opaque == null
        assert copy.message == null
        assert copy.globalErrorCode == null
        assert copy.formatArgs == null
    }

    // ---- message guarantee (localizeErrorCode always populates message) ----

    void testMessageGuaranteeFallbackToDetails() {
        def i18n = new GlobalErrorCodeI18nServiceImpl()
        // no i18n JSON loaded, so getLocalizedMessage returns null
        // localizeErrorCode should fall back to details
        def error = new ErrorCode("SYS.1000", "System Error", "disk full on /dev/sda1")
        assert error.getMessage() == null

        i18n.localizeErrorCode(error, "en_US")

        assert error.getMessage() == "disk full on /dev/sda1"
    }

    void testMessageGuaranteeFallbackToDescription() {
        def i18n = new GlobalErrorCodeI18nServiceImpl()
        // no details, should fall back to description
        def error = new ErrorCode("SYS.1000", "System Error")
        assert error.getMessage() == null

        i18n.localizeErrorCode(error, "en_US")

        assert error.getMessage() == "System Error"
    }

    void testMessageGuaranteeOnCauseChain() {
        def i18n = new GlobalErrorCodeI18nServiceImpl()
        def root = new ErrorCode("INTERNAL.1001", "Internal Error", "root cause detail")
        def mid = new ErrorCode("SYS.1000", "System Error")
        mid.setCause(root)

        i18n.localizeErrorCode(mid, "en_US")

        // both should have message populated
        assert mid.getMessage() == "System Error"
        assert root.getMessage() == "root cause detail"
    }

}
