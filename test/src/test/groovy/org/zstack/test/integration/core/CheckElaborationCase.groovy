package org.zstack.test.integration.core

import org.zstack.core.Platform
import org.zstack.core.errorcode.ElaborationFailedReason
import org.zstack.sdk.CheckElaborationContentAction
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.WebBeanConstructor
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.utils.path.PathUtil

import java.nio.file.Files
import java.nio.file.Path
/**
 * Created by mingjian.deng on 2018/12/26.*/
class CheckElaborationCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        INCLUDE_CORE_SERVICES = false
    }

    @Override
    void environment() {
        env = new EnvSpec()
    }

    @Override
    void test() {
        env.create {
            testCheckElaboration()
            testUnreadableTemplatesUseLocalizedArguments()
        }
    }

    String getFilePath(String file) {
        File absPath = PathUtil.findFileOnClassPath(file)
        return absPath.toPath().toString()
    }

    void testCheckElaboration() {
        checkNonExisted()
        checkFolder("elaborations")
        check1()
        check2()
        check3()
    }

    void checkNonExisted() {
        expect(AssertionError.class) {
            checkElaborationContent {
                elaborateFile = "/tmp-${Platform.uuid}"
            }
        }
    }

    void checkFolder(String folder) {
        String path = getFilePath(folder)
        checkElaborationContent {
            elaborateFile = path
        }
    }

    void check1() {
        def action = new CheckElaborationContentAction()
        action.sessionId = adminSession()
        action.elaborateFile = getFilePath("elaborations/host/host.json")

        def result = action.call()

        assert result.error == null
        def reasons = new ArrayList()
        reasons.addAll(result.value.results.collect{it.reason})

        assert reasons.contains(ElaborationFailedReason.RegexAlreadyExisted.toString())
        assert reasons.contains(ElaborationFailedReason.MessageNotFound.toString())
    }

    void check2() {
        def action = new CheckElaborationContentAction()
        action.sessionId = adminSession()
        action.elaborateFile = getFilePath("elaborations/vm/vm.json")

        def result = action.call()

        assert result.error == null
        def reasons = new ArrayList()
        reasons.addAll(result.value.results.collect{it.reason})

        assert reasons.contains(ElaborationFailedReason.RegexNotFound.toString())
        assert reasons.contains(ElaborationFailedReason.CategoryNotFound.toString())
        assert reasons.contains(ElaborationFailedReason.ErrorCodeAlreadyExisted.toString())
    }

    void check3() {
        def action = new CheckElaborationContentAction()
        action.sessionId = adminSession()
        action.elaborateFile = getFilePath("elaborations/host")

        def result = action.call()

        assert result.error == null
        def reasons = new ArrayList()
        reasons.addAll(result.value.results.collect{it.reason})

        assert reasons.contains(ElaborationFailedReason.DuplicatedRegex.toString())
        assert reasons.contains(ElaborationFailedReason.InValidJsonSchema.toString())
        assert reasons.contains(ElaborationFailedReason.InValidJsonArraySchema.toString())
        assert reasons.contains(ElaborationFailedReason.FileNameWithoutJson.toString())
        assert reasons.contains(ElaborationFailedReason.DuplicatedErrorCode.toString())
    }

    void testUnreadableTemplatesUseLocalizedArguments() {
        Path base = Files.createTempDirectory("zstac-90917-")
        Path input = base.resolve("templates")
        Path target = base.resolve("actual.json")
        Path alias = input.resolve("alias.json")
        String aliasPath = alias.toAbsolutePath().toString()
        List<String> failures = []
        try {
            Files.createDirectory(input)
            Files.write(target, "[]".getBytes("UTF-8"))
            Files.createSymbolicLink(alias, target)
            ["readable", "missing-target", "recovered"].each { state ->
                if (state == "missing-target") {
                    Files.delete(target)
                    assert Files.isSymbolicLink(alias)
                    assert !Files.exists(alias)
                } else if (state == "recovered") {
                    Files.write(target, "[]".getBytes("UTF-8"))
                }
                ["en-US": "read error while elaborating template files", "zh-CN": "读取详细模板文件"].each { language, prefix ->
                    HttpURLConnection conn = (HttpURLConnection) new URL(
                            "http://127.0.0.1:${WebBeanConstructor.port}/v1/errorcode/elaborations/check").openConnection()
                    try {
                        conn.setRequestMethod("POST")
                        conn.setDoOutput(true)
                        conn.setRequestProperty("Authorization", "OAuth ${adminSession()}")
                        conn.setRequestProperty("Content-Type", "application/json")
                        conn.setRequestProperty("Accept-Language", language)
                        String request = JSONObjectUtil.toJsonString([params: [elaborateFile: input.toAbsolutePath().toString()]])
                        conn.getOutputStream().withCloseable { it.write(request.getBytes("UTF-8")) }
                        int status = conn.getResponseCode()
                        def stream = status >= 400 ? conn.getErrorStream() : conn.getInputStream()
                        String body = stream.withCloseable { it.getText("UTF-8") }
                        Map response = JSONObjectUtil.toObject(body, LinkedHashMap.class)
                        println("elaboration-read ${state} ${language}: ${JSONObjectUtil.toJsonString(response)}")
                        if (state != "missing-target") {
                            assert status == 200 : "Readable and restored template links must pass validation"
                            assert response.get("error") == null
                            assert response.get("results") == []
                        } else {
                            assert status == 503
                            Map error = response.get("error") as Map
                            assert error != null
                            assert error.get("code") == "SYS.1006"
                            assert error.get("globalErrorCode") == "ORG_ZSTACK_CORE_ERRORCODE_10004"
                            assert error.get("details").contains(aliasPath)
                            List args = error.get("formatArgs") as List
                            boolean hasReadArguments = args?.size() == 2 && args[0] == aliasPath &&
                                    args[1]?.trim() && args[1].contains(aliasPath)
                            boolean formatted = hasReadArguments && error.message?.startsWith(prefix) &&
                                    error.message.contains(args[0]) && error.message.contains(args[1]) &&
                                    !error.message.contains("%s") && error.details == error.message
                            if (!formatted) {
                                failures.add("${language} must format template path and read failure from two arguments; got ${error}")
                            }
                        }
                    } finally {
                        conn.disconnect()
                    }
                }
            }
        } finally {
            [alias, target, input, base].each { Files.deleteIfExists(it) }
        }
        assert !Files.exists(base) : "The test must remove its own template fixture"
        println("elaboration-read: temporary template fixture cleaned")
        assert failures.isEmpty() : failures.join("\n")
    }
}
