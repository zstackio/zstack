package org.zstack.test.integration.core

import org.zstack.core.Platform
import org.zstack.core.errorcode.ElaborationFailedReason
import org.zstack.sdk.CheckElaborationContentAction
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.path.PathUtil
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
        assert reasons.contains(ElaborationFailedReason.NotSameCategoriesInFile.toString())
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

        assert reasons.contains(ElaborationFailedReason.DuplicatedFileName.toString())
        assert reasons.contains(ElaborationFailedReason.DuplicatedRegex.toString())
        assert reasons.contains(ElaborationFailedReason.InValidJsonSchema.toString())
        assert reasons.contains(ElaborationFailedReason.InValidJsonArraySchema.toString())
        assert reasons.contains(ElaborationFailedReason.FileNameWithoutJson.toString())
        assert reasons.contains(ElaborationFailedReason.DuplicatedErrorCode.toString())
    }
}
