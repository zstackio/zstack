package org.zstack.test.integration.configuration.systemTag

import org.zstack.header.zone.Zone
import org.zstack.sdk.CreateSystemTagAction
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.Test
import org.zstack.testlib.ZoneSpec;

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
        a.resourceType = Zone.getSimpleName()
        a.resourceUuid = zone.inventory.uuid
        a.tag = "tag"
        a.sessionId = Test.currentEnvSpec.session.uuid
        CreateSystemTagAction.Result r = a.call()
    }

    @Override
    void clean() {
        env.delete()
    }
}
