package org.zstack.test.integration.kvm.host

import org.zstack.header.vm.VmInstanceState
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.Utils
import org.zstack.utils.logging.CLogger

/**
 * Created by MaJin on 2017-05-01.
 */
class ReconnectHostCase extends SubCase{
    EnvSpec env
    private final static CLogger logger = Utils.getLogger(ReconnectHostCase.class)
    static RECONNECT_TIME = 10

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.oneVmBasicEnv()
    }

    @Override
    void test() {
        env.create {
            testReconnectHostVmState()
            testReconnectFailureHostVmState()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void testReconnectFailureHostVmState() {
        VmInstanceInventory vmInv = env.inventoryByName("vm") as VmInstanceInventory

        env.simulator(KVMConstant.KVM_CONNECT_PATH) {
            throw new Exception("on purpose")
        }

        expect(AssertionError.class) {
            reconnectHost {
                uuid = vmInv.hostUuid
            }
        }

        retryInSecs {
            List<VmInstanceInventory> vmInvs = queryVmInstance {
                conditions = ["state=${VmInstanceState.Unknown}"]
            } as List<VmInstanceInventory>

            assert vmInvs.size() == 2
        }
    }

    // Reconnect host will not change VM's state, this test confirm VMs are always Running after host reconnecting
    void testReconnectHostVmState(){
        VmInstanceInventory vmInv = env.inventoryByName("vm") as VmInstanceInventory

        for (int i = 0; i < RECONNECT_TIME; i++) {
            reconnectHost {
                uuid = vmInv.hostUuid
                sessionId = currentEnvSpec.session.uuid
            }

            retryInSecs(){
                List<VmInstanceInventory> vmInvs = queryVmInstance {
                    conditions=["state=${VmInstanceState.Running}"]
                } as List<VmInstanceInventory>

                assert vmInvs.size() == 2
            }
        }
    }
}
