package org.zstack.test.integration.configuration.systemTag

import org.json.JSONObject
import org.zstack.header.zone.Zone
import org.zstack.header.zone.ZoneVO
import org.zstack.sdk.CreateSystemTagAction
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.Test
import org.zstack.testlib.ZoneSpec
import org.zstack.utils.gson.JSONObjectUtil;

/**
 * Created by lining on 02/03/2017.
 */
class SystemTagTest extends SubCase{
    EnvSpec env

    @Override
    void setup() {
    }

    @Override
    void environment() {
        env = env{
            zone{
                name = "zone"
            }
        }
    }

    @Override
    void test() {
        env.create {
            testCreateSystemTag()
        }

    }

    void testCreateSystemTag(){
        ZoneSpec zone = env.specByName('zone')

        CreateSystemTagAction a = new CreateSystemTagAction()
        a.resourceType = ZoneVO.getSimpleName()
        a.resourceUuid = zone.inventory.uuid
        a.tag = "host::reservedCpu::{capacity}"
        a.sessionId = Test.currentEnvSpec.session.uuid
        CreateSystemTagAction.Result res = a.call()

        assert res.error == null
        System.out.println(JSONObjectUtil.toJsonString(res))
        System.out.println(res.value)

    }

    @Override
    void clean() {
        env.delete()
    }
}
