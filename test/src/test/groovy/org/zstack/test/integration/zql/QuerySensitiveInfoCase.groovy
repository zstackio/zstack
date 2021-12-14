package org.zstack.test.integration.zql

import org.zstack.compute.vm.VmSystemTags
import org.zstack.sdk.QuerySystemTagAction
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.ZQLQueryResult
import org.zstack.test.integration.ZStackTest
import org.zstack.test.integration.kvm.Env
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class QuerySensitiveInfoCase extends SubCase {
    EnvSpec env
    VmInstanceInventory vmInstanceInventory

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
        env = Env.oneVmBasicEnv()
    }

    @Override
    void test() {
        env.create {
            prepare()
            setConsolePassword()
            testQuerySensitiveTag()
            testQuerySensitiveField()
        }
    }

    void prepare() {
        vmInstanceInventory = env.inventoryByName("vm") as VmInstanceInventory
    }

    void setConsolePassword() {
        setVmConsolePassword {
            uuid = vmInstanceInventory.uuid
            consolePassword = "123456"
        }
    }

    void testQuerySensitiveTag() {
        QuerySystemTagAction action = new QuerySystemTagAction()
        action.sessionId = adminSession()
        action.conditions = ["resourceUuid=${vmInstanceInventory.uuid}".toString(), "resourceType=VmInstanceVO"]
        QuerySystemTagAction.Result result = action.call()
        assert result.error == null
        assert result.value.inventories != null
        Boolean flag = false
        result.value.inventories.each {
            if (VmSystemTags.CONSOLE_PASSWORD.getTokenByTag(it.tag, VmSystemTags.CONSOLE_PASSWORD_TOKEN) == "*****") {
                flag = true
            }
        }
        assert flag
    }

    void testQuerySensitiveField() {
        ZQLQueryResult result = zQLQuery {
            delegate.sessionId = adminSession()
            delegate.zql = "query systemTag.tag where resourceUuid='${vmInstanceInventory.uuid}' and resourceType='VmInstanceVO'"
        } as ZQLQueryResult
        List list = result.results[0].inventories
        Boolean flag = false
        list.each {
            if (VmSystemTags.CONSOLE_PASSWORD.getTokenByTag(it.tag, VmSystemTags.CONSOLE_PASSWORD_TOKEN) == "*****") {
                flag = true
            }
        }
        assert flag
    }
}
