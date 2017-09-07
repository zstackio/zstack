package org.zstack.test.integration.kvm.host

import org.zstack.core.db.Q
import org.zstack.header.host.HostStateEvent
import org.zstack.header.host.HostStatus
import org.zstack.header.host.HostVO
import org.zstack.header.host.HostVO_
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.HostInventory
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
            testChangeHostStateByRealizeHost()
            testAddMultipleHostConcurrently()
        }
    }

    void testChangeHostStateByRealizeHost() {
        HostSpec spec = env.specByName("kvm")

        HostInventory inventory = changeHostState {
            sessionId = Test.currentEnvSpec.session.uuid
            uuid = spec.inventory.uuid
            stateEvent = HostStateEvent.enable
        }

        assert inventory.uuid == spec.inventory.uuid
        assert inventory.status == HostStatus.Connected.name()
    }

    void testAddMultipleHostConcurrently() {
        def ipAddr = "127.0.0.11"
        ClusterInventory clusterInv = env.inventoryByName("cluster") as ClusterInventory
        def threads = []
        1.upto(8, { it ->
            def vmName = "test-${it}"
            def thread = Thread.start {
                try {
                    addKVMHost {
                        name = vmName
                        managementIp = ipAddr
                        username = "root"
                        password = "password"
                        clusterUuid = clusterInv.uuid
                    }
                } catch (Throwable ignored) {
                }
            }

            threads.add(thread)
        })

        threads.each{it.join()}

        assert Q.New(HostVO.class).eq(HostVO_.managementIp, ipAddr).count() == 1L
    }

    @Override
    void clean() {
        env.delete()
    }

}
