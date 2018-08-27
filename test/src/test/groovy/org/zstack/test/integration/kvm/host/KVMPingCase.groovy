package org.zstack.test.integration.kvm.host

import org.springframework.http.HttpEntity
import org.zstack.compute.host.HostGlobalConfig
import org.zstack.compute.host.HostReconnectTask
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.db.Q
import org.zstack.header.core.NoErrorCompletion
import org.zstack.header.host.HostStatus
import org.zstack.header.host.HostVO
import org.zstack.header.host.HostVO_
import org.zstack.header.host.PingHostMsg
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.kvm.KVMReconnectHostTask
import org.zstack.sdk.HostInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.FieldUtils
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
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
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

    void waitHostDisconnected(String hostUuid) {
        retryInSecs {
            assert Q.New(HostVO.class).select(HostVO_.status).eq(HostVO_.uuid, hostUuid).findValue() == HostStatus.Disconnected
        }
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

        waitHostDisconnected(kvm1.uuid)

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

    static class HostReconnectTaskForTest extends HostReconnectTask {
        @Override
        protected HostReconnectTask.CanDoAnswer canDoReconnect() {
            if (canDoReconnectFunc != null) {
                return (HostReconnectTask.CanDoAnswer) canDoReconnectFunc()
            }

            return  HostReconnectTask.CanDoAnswer.NoReconnect
        }

        HostReconnectTaskForTest(String uuid, NoErrorCompletion completion) {
            super(uuid, completion)
        }
    }

    @Override
    void test() {
        bus = bean(CloudBus.class)

        env.create {
            HostGlobalConfig.PING_HOST_INTERVAL.updateValue(1)
            HostGlobalConfig.MAXIMUM_PING_FAILURE.updateValue(1)
            HostGlobalConfig.SLEEP_TIME_AFTER_PING_FAILURE.updateValue(0)

            functionForMockTestObjectFactory[HostReconnectTask.class] = {
                if (it instanceof KVMReconnectHostTask) {
                    return new HostReconnectTaskForTest(it.uuid, FieldUtils.getFieldValue("completion", it))
                } else {
                    return it
                }
            }

            onCleanExecute {
                functionForMockTestObjectFactory.remove(HostReconnectTask.class)
            }

            testPing()
        }
    }
}
