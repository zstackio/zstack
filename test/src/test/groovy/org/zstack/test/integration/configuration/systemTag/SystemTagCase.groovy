package org.zstack.test.integration.configuration.systemTag

import org.zstack.header.configuration.DiskOfferingVO
import org.zstack.header.zone.ZoneVO
import org.zstack.sdk.CreateSystemTagAction
import org.zstack.testlib.*
import org.zstack.utils.data.SizeUnit

/**
 * Created by lining on 02/03/2017.
 */
class SystemTagCase extends SubCase{
    EnvSpec env

    @Override
    void setup() {
    }

    @Override
    void environment() {
        env = env{
            diskOffering {
                name = "diskOffering"
                diskSize = SizeUnit.GIGABYTE.toByte(20)
            }
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
            testCreateSystemTagForTypeNotMatchedResource()
        }

    }

    void testCreateSystemTag(){
        ZoneSpec zone = env.specByName('zone')

        CreateSystemTagAction a = new CreateSystemTagAction(
                resourceType: ZoneVO.getSimpleName(),
                resourceUuid: zone.inventory.uuid,
                tag: "host::reservedCpu::{capacity}",
                sessionId: Test.currentEnvSpec.session.uuid
        )
        CreateSystemTagAction.Result res = a.call()

        assert res.error == null
        assert res.value.inventory.resourceUuid == a.resourceUuid
        assert res.value.inventory.tag == a.tag
        assert res.value.inventory.resourceType == a.resourceType
    }

    void testRepeatCreateSameResourceUuidSystemTag(){
        ZoneSpec zone = env.specByName('zone')

        CreateSystemTagAction a = new CreateSystemTagAction(
                resourceType: ZoneVO.getSimpleName(),
                resourceUuid: zone.inventory.uuid,
                tag: "host::reservedCpu::{capacity}",
                sessionId: Test.currentEnvSpec.session.uuid
        )

        CreateSystemTagAction.Result res = a.call()

        assert res.error == null
    }

    void testCreateSystemTagForTypeNotMatchedResource() {
        DiskOfferingSpec diskOfferingSpec = env.specByName('diskOffering')

        CreateSystemTagAction a = new CreateSystemTagAction(
                resourceType: DiskOfferingVO.getSimpleName(),
                resourceUuid: diskOfferingSpec.inventory.uuid,
                tag: "host::reservedCpu::{capacity}",
                sessionId: Test.currentEnvSpec.session.uuid
        )

        CreateSystemTagAction.Result res = a.call()

        assert res.error != null
    }

    @Override
    void clean() {
        env.delete()
    }
}
