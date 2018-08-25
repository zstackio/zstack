package org.zstack.test.integration.kvm.host

import org.springframework.http.HttpEntity
import org.zstack.compute.host.HostGlobalConfig
import org.zstack.compute.host.HostTrackImpl

import org.zstack.core.cloudbus.CloudBus
import org.zstack.header.host.PingHostMsg
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant

import org.zstack.sdk.HostInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

import java.util.concurrent.TimeUnit

class KVMPingCase extends SubCase {
    EnvSpec env
    CloudBus bus

    @Override
    void clean() {
        env.delete()
    }

    static Closure<Boolean> canDoReconnectFunc

    static class KVMHostTrackerPreReconnectCheckerForTest implements HostTrackerPreReconnectChecker {
        @Override
        Boolean canDoReconnect(String hostUuid) {
            if (canDoReconnectFunc == null) {
                return true
            }

            return canDoReconnectFunc(hostUuid)
        }

        @Override
        String getHypervisorType() {
            return KVMConstant.KVM_HYPERVISOR_TYPE
        }
    }


    @Override
    void setup() {
        env = makeEnv {
            zone {
                name = "zone"

                cluster {
                    name = "cluster"

                    kvm {
                        name = "kvm1"
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                    }

                    kvm {
                        name = "kvm2"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"
                    }
                }
            }
        }
    }

    @Override
    void environment() {
        useSpring(KvmTest.springSpec)
    }

    void testPing() {
        canDoReconnectFunc = { false }

        HostInventory kvm1 = env.inventoryByName("kvm1")

        env.simulator(KVMConstant.KVM_PING_PATH) { HttpEntity<String> e, EnvSpec espec ->
            KVMAgentCommands.PingCmd cmd = JSONObjectUtil.toObject(e.getBody(), KVMAgentCommands.PingCmd.class)

            def rsp = new KVMAgentCommands.PingResponse()
            if (cmd.hostUuid == kvm1.uuid) {
                rsp.success = false
                rsp.error = "on purpose"
            } else {
                rsp.hostUuid = cmd.hostUuid
            }

            return rsp
        }

        int count = 0

        def cleanup = notifyWhenReceivedMessage(PingHostMsg.class) { PingHostMsg msg ->
            if (msg.hostUuid == kvm1.uuid) {
                count ++
            }
        }

        TimeUnit.SECONDS.sleep(3L)

        assert count == 0

        env.cleanSimulatorHandlers()
        cleanup()
    }

    @Override
    void test() {
        bus = bean(CloudBus.class)
        def origin = KVMHostTrackerPreReconnectChecker.metaClass.methods.find { it.name == "canDoReconnect" }
        onCleanExecute { KVMHostTrackerPreReconnectChecker.metaClass["canDoReconnect"] = origin }

        env.create {
            HostGlobalConfig.PING_HOST_INTERVAL.updateValue(1)
            HostTrackImpl ht = bean(HostTrackImpl.class)
            def origin = ht.hostTrackerPreReconnectCheckers[KVMConstant.KVM_HYPERVISOR_TYPE]
            onCleanExecute {
                ht.hostTrackerPreReconnectCheckers[KVMConstant.KVM_HYPERVISOR_TYPE] = origin
            }
            ht.hostTrackerPreReconnectCheckers[KVMConstant.KVM_HYPERVISOR_TYPE] = KVMHostTrackerPreReconnectCheckerForTest.class

            testPing()
        }
    }
}
