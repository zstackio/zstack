package org.zstack.test.integration.configuration.systemTag

import org.zstack.header.zone.Zone
import org.zstack.sdk.CreateSystemTagAction
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
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
                resourceUuid = "14d087f6d59a4d639094e6c2c9032161"
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
        a.resourceUuid = "14d087f6d59a4d639094e6c2c9032161"
        a.tag = "tag"
        CreateSystemTagAction.Result r = a.call()
    }

    @Override
    void clean() {
        env.delete()
    }
}
