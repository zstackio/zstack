package org.zstack.test.integration.kvm.status

import org.zstack.core.db.DatabaseFacade
import org.zstack.header.host.HostAO
import org.zstack.header.host.HostState
import org.zstack.header.host.HostStateEvent
import org.zstack.header.host.HostStatus
import org.zstack.header.host.HostVO
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.kvm.KVMConstant
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.sdk.ChangeHostStateAction
import org.zstack.test.compute.host.ChangeHostStateExtension
import org.zstack.test.integration.kvm.Env
import org.zstack.test.kvm.KVMStartVmExtension
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.HostSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by Administrator on 2017-03-03.
 */
class MaintainHostCase extends SubCase {
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
            testMaintainHostWhichIsConnecting()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void testMaintainHostWhichIsConnecting() {
        HostSpec hostSpec = env.specByName("kvm") as HostSpec
        DatabaseFacade dbf = bean(DatabaseFacade.class)
        String hostUuid = hostSpec.inventory.uuid
        assert hostUuid

        HostVO hvo = dbf.findByUuid(hostUuid, HostVO.class)
        hvo.status = HostStatus.Connecting

        dbf.updateAndRefresh(hvo)

        assert dbf.findByUuid(hostUuid, HostVO.class).status == HostStatus.Connecting

        ChangeHostStateAction action = new ChangeHostStateAction()
        action.stateEvent = HostStateEvent.maintain
        action.uuid = hostUuid
        action.sessionId = currentEnvSpec.session.uuid

        ChangeHostStateAction.Result res = action.call()
        assert res.error != null


    }
}