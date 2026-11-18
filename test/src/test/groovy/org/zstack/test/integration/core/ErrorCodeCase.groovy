package org.zstack.test.integration.core

import org.zstack.core.ansible.AnsibleFacadeImpl
import org.zstack.core.errorcode.ErrorFacade
import org.zstack.sdk.ApiResult
import org.zstack.sdk.ErrorCode
import org.zstack.sdk.ErrorCodeList
import org.zstack.testlib.SubCase
import org.zstack.utils.ShellResult
import org.zstack.utils.ShellUtils

class ErrorCodeCase extends SubCase {
    @Override
    void clean() {
    }

    @Override
    void setup() {
    }

    @Override
    void environment() {
    }

    @Override
    void test() {
        testErrorCodeDeserializer()
        testAnsibleOutputSurvivesErrorTruncation()
    }

    void testAnsibleOutputSurvivesErrorTruncation() {
        String diagnostic = "Management node cannot create Ansible local temporary directory [/root/.ansible/tmp]: inode exhaustion; check df -h and df -i"
        ShellResult result = new ShellResult(command: "ansible-playbook " + "x" * 5000,
                retCode: 1, stderr: diagnostic, stdout: "Ansible failed")
        ShellUtils.ShellException failure
        try {
            result.raiseExceptionIfFail()
            assert false: "the failed Ansible command must raise an error"
        } catch (ShellUtils.ShellException error) {
            failure = error
        }
        ErrorFacade errf = bean(ErrorFacade.class)
        assert !errf.stringToOperationError(failure.message).details.contains(diagnostic)
        String message = AnsibleFacadeImpl.prioritizeAnsibleOutput(failure.message)
        def error = errf.stringToOperationError(message)
        assert error.details.contains(diagnostic)
        assert error.details.contains("Ansible failed")
        assert message.contains(result.command)
        assert message.contains("ret code: 1")
        assert AnsibleFacadeImpl.prioritizeAnsibleOutput("local account expired") == "local account expired"
    }

    static void testErrorCodeDeserializer() {
        def text = """
{
  "apiId": "9de68fa15ead4341874f3f556a0539eb",
  "success": false,
  "error": {
    "causes": [{
      "code": "SYS.1000",
      "description": "An internal error happened in system",
      "details": "on purpose",
      "location": "somewhere",
      "cost": "1ms"
    }],
    "location": "VmInstanceBase.java: call-pre-vm-migration-extension (location:1/2)"
  },
  "headers": {
    "thread-context": {
      "api": "9de68fa15ead4341874f3f556a0539eb",
      "task-name": "org.zstack.header.vm.APIMigrateVmMsg",
      "progress-enabled": "true"
    },
    "task-context": {
      "__messagetimeout__": "3600000",
      "__messagedeadline__": "1705309421982"
    },
    "schema": {
      "error": "org.zstack.header.errorcode.ErrorCodeList",
      "error.causes[0]": "org.zstack.header.errorcode.ErrorCodeList"
    }
  },
  "id": "b442e4d29bf745ceb5efadcf7b8655ee",
  "createdTime": 1705305822094
}
"""
        def api = createApiResult(text)
        def result = api.getResult(Result.class)
        def errorCode = result.error

        assert errorCode instanceof ErrorCodeList
        assert errorCode.causes != null
        assert errorCode.causes.size() == 1
        assert errorCode.causes[0] instanceof ErrorCode

        def cause = errorCode.causes[0] as ErrorCode
        assert cause.code == "SYS.1000"
        assert cause.details == "on purpose"

        text = """
{
  "apiId": "d2826f9f252f4da5b721349f671b0e4b",
  "success": false,
  "error": {
    "code": "SYS.1000",
    "description": "An internal error happened in system",
    "details": "on purpose",
    "location": "somewhere",
    "cost": "1ms"
  },
  "headers": {
    "thread-context": {
      "api": "9de68fa15ead4341874f3f556a0539eb",
      "task-name": "org.zstack.header.vm.APIMigrateVmMsg",
      "progress-enabled": "true"
    },
    "task-context": {
      "__messagetimeout__": "3600000",
      "__messagedeadline__": "1705309421982"
    },
    "schema": {
      "error": "org.zstack.header.errorcode.ErrorCodeList",
      "error.causes[0]": "org.zstack.header.errorcode.ErrorCodeList"
    }
  },
  "id": "b442e4d29bf745ceb5efadcf7b8655ee",
  "createdTime": 1705305822094
}
"""
        api = createApiResult(text)
        result = api.getResult(Result.class)
        errorCode = result.error

        assert !(errorCode instanceof ErrorCodeList)
        assert errorCode.code == "SYS.1000"
        assert errorCode.details == "on purpose"
    }

    static ApiResult createApiResult(String text) {
        def result = new ApiResult()
        def method = result.class.getDeclaredMethod("setResultString", String.class)
        method.setAccessible(true)
        method.invoke(result, text)
        return result
    }

    static class Result {
        ErrorCode error
    }
}
