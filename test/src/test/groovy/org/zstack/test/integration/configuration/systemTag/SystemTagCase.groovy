package org.zstack.test.integration.configuration.systemTag

import org.zstack.header.zone.ZoneVO
import org.zstack.sdk.SystemTagInventory
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.ZoneSpec

/**
 * Created by lining on 02/03/2017.
 */
class SystemTagCase extends SubCase{

    EnvSpec env

    String tagValue = "host::reservedCpu::{capacity}"
    String newTagUuid

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
            testRepeatCreateSameResourceUuidSystemTag()
        }

    }

    void testCreateSystemTag(){
        ZoneSpec zone = env.specByName('zone')

        /*
        CreateSystemTagAction a = new CreateSystemTagAction(
                resourceType: ZoneVO.getSimpleName(),
                resourceUuid: zone.inventory.uuid,
                tag: "host::reservedCpu::{capacity}",
                sessionId: Test.currentEnvSpec.session.uuid
        )
        CreateSystemTagAction.Result res = a.call()
        */
        SystemTagInventory inventory = createSystemTag {
            resourceType = ZoneVO.getSimpleName()
            resourceUuid =  zone.inventory.uuid
            tag = tagValue
        }
        newTagUuid = inventory.uuid
        assert inventory.resourceUuid == zone.inventory.uuid
        assert inventory.tag ==  tagValue
        assert inventory.resourceType == ZoneVO.getSimpleName()
    }

    void testRepeatCreateSameResourceUuidSystemTag(){
        ZoneSpec zone = env.specByName('zone')

        SystemTagInventory inventory = createSystemTag {
            resourceType = ZoneVO.getSimpleName()
            resourceUuid =  zone.inventory.uuid
            tag = tagValue
        }
        assert null == inventory

        deleteTag {
            uuid = newTagUuid
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
