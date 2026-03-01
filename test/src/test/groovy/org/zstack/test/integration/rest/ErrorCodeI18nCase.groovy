package org.zstack.test.integration.rest

import org.zstack.core.errorcode.GlobalErrorCodeI18nService
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.errorcode.ErrorCodeList
import org.zstack.header.errorcode.SysErrors
import org.zstack.header.zone.APIQueryZoneMsg
import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.WebBeanConstructor
import org.zstack.utils.gson.JSONObjectUtil

class ErrorCodeI18nCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(ZStackTest.springSpec)
    }

    @Override
    void environment() {
        env = env {}
    }

    @Override
    void test() {
        env.create {
            testServiceLocalizesZhCN()
            testServiceLocalizesEnUS()
            testServiceFallbackToEnUS()
            testServiceFormatsArgs()
            testServiceRecursiveCauseChain()
            testServiceErrorCodeList()
            testRestServerSyncApiWithAcceptLanguage()
            testRestServerAsyncApiWithAcceptLanguage()
            testNoAcceptLanguageHeaderFallsBackToEnUS()
        }
    }

    void testServiceLocalizesZhCN() {
        GlobalErrorCodeI18nService i18nService = bean(GlobalErrorCodeI18nService.class)
        // ORG_ZSTACK_STORAGE_PRIMARY_10039: zh_CN = "未找到主存储[uuid:%s]"
        String msg = i18nService.getLocalizedMessage(
                "ORG_ZSTACK_STORAGE_PRIMARY_10039", "zh_CN", ["test-uuid-123"] as String[])
        assert msg != null
        assert msg.contains("未找到主存储")
        assert msg.contains("test-uuid-123")
    }

    void testServiceLocalizesEnUS() {
        GlobalErrorCodeI18nService i18nService = bean(GlobalErrorCodeI18nService.class)
        // ORG_ZSTACK_STORAGE_PRIMARY_10039: en_US = "no primary storage[uuid:%s] exists"
        String msg = i18nService.getLocalizedMessage(
                "ORG_ZSTACK_STORAGE_PRIMARY_10039", "en_US", ["test-uuid-456"] as String[])
        assert msg != null
        assert msg.contains("no primary storage")
        assert msg.contains("test-uuid-456")
    }

    void testServiceFallbackToEnUS() {
        GlobalErrorCodeI18nService i18nService = bean(GlobalErrorCodeI18nService.class)
        // Request a locale that doesn't exist, should fallback to en_US
        String msg = i18nService.getLocalizedMessage(
                "ORG_ZSTACK_STORAGE_PRIMARY_10039", "nonexistent_locale", ["uuid-789"] as String[])
        assert msg != null
        assert msg.contains("no primary storage")
    }

    void testServiceFormatsArgs() {
        GlobalErrorCodeI18nService i18nService = bean(GlobalErrorCodeI18nService.class)
        // Test with null formatArgs - should return template as-is
        String msg = i18nService.getLocalizedMessage(
                "ORG_ZSTACK_STORAGE_PRIMARY_10039", "zh_CN", null)
        assert msg != null
        assert msg.contains("%s")  // template not formatted
    }

    void testServiceRecursiveCauseChain() {
        GlobalErrorCodeI18nService i18nService = bean(GlobalErrorCodeI18nService.class)

        ErrorCode cause = new ErrorCode()
        cause.setCode(SysErrors.INTERNAL.toString())
        cause.setGlobalErrorCode("ORG_ZSTACK_STORAGE_PRIMARY_10039")
        cause.setFormatArgs(["inner-uuid"] as String[])

        ErrorCode outer = new ErrorCode()
        outer.setCode(SysErrors.INTERNAL.toString())
        outer.setGlobalErrorCode("ORG_ZSTACK_STORAGE_PRIMARY_10039")
        outer.setFormatArgs(["outer-uuid"] as String[])
        outer.setCause(cause)

        i18nService.localizeErrorCode(outer, "zh_CN")

        assert outer.getMessage() != null
        assert outer.getMessage().contains("outer-uuid")
        assert outer.getCause().getMessage() != null
        assert outer.getCause().getMessage().contains("inner-uuid")
    }

    void testServiceErrorCodeList() {
        GlobalErrorCodeI18nService i18nService = bean(GlobalErrorCodeI18nService.class)

        ErrorCodeList errorList = new ErrorCodeList()
        errorList.setCode(SysErrors.INTERNAL.toString())
        errorList.setGlobalErrorCode("ORG_ZSTACK_STORAGE_PRIMARY_10039")
        errorList.setFormatArgs(["list-uuid"] as String[])

        ErrorCode child1 = new ErrorCode()
        child1.setCode(SysErrors.INTERNAL.toString())
        child1.setGlobalErrorCode("ORG_ZSTACK_STORAGE_PRIMARY_10039")
        child1.setFormatArgs(["child1-uuid"] as String[])

        ErrorCode child2 = new ErrorCode()
        child2.setCode(SysErrors.INTERNAL.toString())
        child2.setGlobalErrorCode("ORG_ZSTACK_STORAGE_PRIMARY_10039")
        child2.setFormatArgs(["child2-uuid"] as String[])

        errorList.setCauses([child1, child2])

        i18nService.localizeErrorCode(errorList, "zh_CN")

        assert errorList.getMessage().contains("list-uuid")
        assert errorList.getCauses()[0].getMessage().contains("child1-uuid")
        assert errorList.getCauses()[1].getMessage().contains("child2-uuid")
    }

    void testRestServerSyncApiWithAcceptLanguage() {
        // Intercept APIQueryZoneMsg and return error with known globalErrorCode
        ErrorCode mockError = new ErrorCode()
        mockError.setCode(SysErrors.INTERNAL.toString())
        mockError.setDetails("no primary storage[uuid:test-sync-uuid] exists")
        mockError.setGlobalErrorCode("ORG_ZSTACK_STORAGE_PRIMARY_10039")
        mockError.setFormatArgs(["test-sync-uuid"] as String[])

        env.message(APIQueryZoneMsg.class) { APIQueryZoneMsg msg, bus ->
            bus.replyErrorByMessageType(msg, mockError)
        }

        try {
            // Make raw HTTP GET to /v1/zones with Accept-Language: zh-CN
            String sessionId = adminSession()
            String url = "http://127.0.0.1:${WebBeanConstructor.port}/v1/zones"
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection()
            try {
                conn.setRequestMethod("GET")
                conn.setRequestProperty("Authorization", "OAuth ${sessionId}")
                conn.setRequestProperty("Accept-Language", "zh-CN")

                int responseCode = conn.getResponseCode()
                String responseBody
                if (responseCode >= 400) {
                    responseBody = conn.getErrorStream()?.text ?: ""
                } else {
                    responseBody = conn.getInputStream()?.text ?: ""
                }

                assert responseCode == 503  // SERVICE_UNAVAILABLE for error responses
                Map response = JSONObjectUtil.toObject(responseBody, LinkedHashMap.class)
                Map error = response.get("error") as Map
                assert error != null
                assert error.get("message") != null
                String message = error.get("message") as String
                assert message.contains("未找到主存储")
                assert message.contains("test-sync-uuid")
            } finally {
                conn.disconnect()
            }
        } finally {
            env.cleanMessageHandlers()
        }
    }

    void testRestServerAsyncApiWithAcceptLanguage() {
        // Use an async API (create zone) and intercept to return error
        ErrorCode asyncError = new ErrorCode()
        asyncError.setCode(SysErrors.INTERNAL.toString())
        asyncError.setDetails("no primary storage[uuid:test-async-uuid] exists")
        asyncError.setGlobalErrorCode("ORG_ZSTACK_STORAGE_PRIMARY_10039")
        asyncError.setFormatArgs(["test-async-uuid"] as String[])

        env.message(org.zstack.header.zone.APICreateZoneMsg.class) { msg, bus ->
            bus.replyErrorByMessageType(msg, asyncError)
        }

        try {
            // POST to create zone
            String sessionId = adminSession()
            String postUrl = "http://127.0.0.1:${WebBeanConstructor.port}/v1/zones"
            String body = '{"params":{"name":"test-i18n-zone","description":"test"}}'

            HttpURLConnection postConn = (HttpURLConnection) new URL(postUrl).openConnection()
            try {
                postConn.setRequestMethod("POST")
                postConn.setRequestProperty("Authorization", "OAuth ${sessionId}")
                postConn.setRequestProperty("Accept-Language", "zh-CN")
                postConn.setRequestProperty("Content-Type", "application/json")
                postConn.setDoOutput(true)
                postConn.getOutputStream().write(body.getBytes("UTF-8"))

                int postCode = postConn.getResponseCode()
                String postBody
                if (postCode >= 400) {
                    postBody = postConn.getErrorStream()?.text ?: ""
                } else {
                    postBody = postConn.getInputStream()?.text ?: ""
                }

                // Async API returns 202 with location header for job polling
                if (postCode == 202) {
                    Map postResponse = JSONObjectUtil.toObject(postBody, LinkedHashMap.class)
                    String location = postResponse.get("location") as String
                    assert location != null

                    // Poll the job with Accept-Language header
                    retryInSecs(5) {
                        String jobUrl = location.startsWith("http") ? location : "http://127.0.0.1:${WebBeanConstructor.port}${location}"
                        HttpURLConnection jobConn = (HttpURLConnection) new URL(jobUrl).openConnection()
                        try {
                            jobConn.setRequestMethod("GET")
                            jobConn.setRequestProperty("Authorization", "OAuth ${sessionId}")
                            jobConn.setRequestProperty("Accept-Language", "zh-CN")

                            int jobCode = jobConn.getResponseCode()
                            String jobBody
                            if (jobCode >= 400) {
                                jobBody = jobConn.getErrorStream()?.text ?: ""
                            } else {
                                jobBody = jobConn.getInputStream()?.text ?: ""
                            }

                            // Job should be done (503 for error)
                            assert jobCode == 503
                            Map jobResponse = JSONObjectUtil.toObject(jobBody, LinkedHashMap.class)
                            Map jobError = jobResponse.get("error") as Map
                            assert jobError != null
                            String jobMessage = jobError.get("message") as String
                            assert jobMessage != null
                            assert jobMessage.contains("未找到主存储")
                            assert jobMessage.contains("test-async-uuid")
                        } finally {
                            jobConn.disconnect()
                        }
                    }
                } else {
                    // If returned directly (e.g. 503), check the error
                    assert postCode == 503
                    Map directResponse = JSONObjectUtil.toObject(postBody, LinkedHashMap.class)
                    Map directError = directResponse.get("error") as Map
                    assert directError != null
                    String directMessage = directError.get("message") as String
                    assert directMessage != null
                    assert directMessage.contains("未找到主存储")
                }
            } finally {
                postConn.disconnect()
            }
        } finally {
            env.cleanMessageHandlers()
        }
    }

    void testNoAcceptLanguageHeaderFallsBackToEnUS() {
        // Intercept APIQueryZoneMsg and return error with known globalErrorCode
        ErrorCode mockError = new ErrorCode()
        mockError.setCode(SysErrors.INTERNAL.toString())
        mockError.setDetails("no primary storage[uuid:test-no-lang] exists")
        mockError.setGlobalErrorCode("ORG_ZSTACK_STORAGE_PRIMARY_10039")
        mockError.setFormatArgs(["test-no-lang"] as String[])

        env.message(APIQueryZoneMsg.class) { APIQueryZoneMsg msg, bus ->
            bus.replyErrorByMessageType(msg, mockError)
        }

        try {
            // Make raw HTTP GET without Accept-Language header
            String sessionId = adminSession()
            String url = "http://127.0.0.1:${WebBeanConstructor.port}/v1/zones"
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection()
            try {
                conn.setRequestMethod("GET")
                conn.setRequestProperty("Authorization", "OAuth ${sessionId}")
                // No Accept-Language header

                int responseCode = conn.getResponseCode()
                String responseBody
                if (responseCode >= 400) {
                    responseBody = conn.getErrorStream()?.text ?: ""
                } else {
                    responseBody = conn.getInputStream()?.text ?: ""
                }

                assert responseCode == 503
                Map response = JSONObjectUtil.toObject(responseBody, LinkedHashMap.class)
                Map error = response.get("error") as Map
                assert error != null
                // Without Accept-Language, resolveLocale defaults to en_US
                // So message should be the en_US version
                String message = error.get("message") as String
                assert message != null
                assert message.contains("no primary storage")
                assert message.contains("test-no-lang")
            } finally {
                conn.disconnect()
            }
        } finally {
            env.cleanMessageHandlers()
        }
    }
}
