package org.zstack.test.integration.kvm.host

import org.zstack.header.host.HostStateEvent
import org.zstack.header.host.HostStatus
import org.zstack.sdk.ChangeHostStateAction
import org.zstack.test.integration.kvm.Env
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.HostSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.Test

/**
 * Created by lining on 02/03/2017.
 */

class HostStateCase extends SubCase{

    EnvSpec env

    @Override
    void setup() {
        spring {
            sftpBackupStorage()
            localStorage()
            virtualRouter()
            securityGroup()
            kvm()

        }
    }

    @Override
    void environment() {
        env = Env.oneVmBasicEnv()
    }

    @Override
    void test() {
        env.create {
            testRealizeHost()
        }
    }

    void testRealizeHost(){
        HostSpec spec = env.specByName("kvm")

        ChangeHostStateAction action = new ChangeHostStateAction(
                sessionId: Test.currentEnvSpec.session.uuid,
                uuid: spec.inventory.uuid,
                stateEvent: HostStateEvent.enable
        )
        ChangeHostStateAction.Result res = action.call()

        assert res.value != null
        assert res.value.inventory.uuid == spec.inventory.uuid
        assert res.value.inventory.status == HostStatus.Connected.name()
    }

    @Override
    void clean() {
        env.delete()
    }

}
