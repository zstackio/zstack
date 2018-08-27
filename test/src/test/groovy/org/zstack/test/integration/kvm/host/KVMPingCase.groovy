package org.zstack.test.integration.kvm.host

import org.springframework.http.HttpEntity
import org.zstack.compute.host.HostGlobalConfig
import org.zstack.compute.host.HostReconnectTask
import org.zstack.compute.host.HostTrackImpl
import org.zstack.core.aspect.NoAsyncSafe
import org.zstack.core.cloudbus.CloudBus
import org.zstack.header.core.NoErrorCompletion
import org.zstack.header.host.PingHostMsg
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.kvm.KVMReconnectHostTask
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

    static Closure<HostReconnectTask.CanDoAnswer> canDoReconnectFunc

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
        canDoReconnectFunc = {  HostReconnectTask.CanDoAnswer.NotReady }

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

    private static class HostReconnectTaskForTest extends HostReconnectTask {
        @Override
        protected CanDoAnswer canDoReconnect() {
            if (canDoReconnectFunc != null) {
                return canDoReconnectFunc()
            }

            return  HostReconnectTask.CanDoAnswer.NoReconnect
        }

        @NoAsyncSafe
        HostReconnectTaskForTest(String uuid, NoErrorCompletion completion) {
            super(uuid, completion)
        }
    }

    @Override
    void test() {
        bus = bean(CloudBus.class)

        env.create {
            HostGlobalConfig.PING_HOST_INTERVAL.updateValue(1)
            functionForMockTestObjectFactory[HostReconnectTask.class] = { HostReconnectTask task ->
                if (task instanceof KVMReconnectHostTask) {
                    return new HostReconnectTaskForTest(task.uuid, task.completion)
                } else {
                    return task
                }
            }

            onCleanExecute {
                functionForMockTestObjectFactory.remove(HostReconnectTask.class)
            }

            testPing()
        }
    }
}
