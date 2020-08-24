package org.zstack.test.integration.kvm.host

import org.springframework.http.HttpEntity
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.cloudbus.CloudBusCallBack
import org.zstack.header.host.ConnectHostMsg
import org.zstack.header.host.HostConstant
import org.zstack.header.host.HostStatus
import org.zstack.header.message.MessageReply
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.HostInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by mingjian.deng on 2019/10/16.*/
class ConnectHostMultiTimesCase extends SubCase {
    EnvSpec env
    HostInventory host1
    HostInventory host2
    CloudBus bus

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.noVmTwoHostsEnv()
    }

    @Override
    void test() {
        bus = bean(CloudBus.class)
        env.create {
            host1 = env.inventoryByName("kvm1")
            host2 = env.inventoryByName("kvm2")

            env.afterSimulator(KVMConstant.KVM_CONNECT_PATH) { KVMAgentCommands.AgentResponse rsp, HttpEntity<String> e ->
                sleep 100
                return rsp
            }

            testConnectHost(20)
            testConnectTwoHosts(10, 10)

            env.cleanAfterSimulatorHandlers()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void testConnectHost(int times) {
        CountDownLatch latch = new CountDownLatch(times)
        AtomicInteger successCount = new AtomicInteger(0)
        AtomicInteger cancelCount = new AtomicInteger(0)

        for (int i in 1..times) {
            def msg = new ConnectHostMsg()
            msg.uuid = host1.uuid
            bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, host1.uuid)
            bus.send(msg, new CloudBusCallBack(msg) {
                @Override
                void run(MessageReply reply) {
                    if (reply.isSuccess()) {
                        successCount.incrementAndGet()
                    } else if (reply.isCanceled()) {
                        cancelCount.incrementAndGet()
                    }
                    latch.countDown()
                }
            })
        }

        latch.await()
        assert successCount.get() == 1
        assert cancelCount.get() == 19

        refreshHost()
        assert host1.status == HostStatus.Connected.toString()
        assert host2.status == HostStatus.Connected.toString()
    }

    void testConnectTwoHosts(int times1, int times2) {
        CountDownLatch latch = new CountDownLatch(times1+times2)
        AtomicInteger successCount = new AtomicInteger(0)
        AtomicInteger cancelCount = new AtomicInteger(0)

        for (int i in 1..times1) {
            def msg = new ConnectHostMsg()
            msg.uuid = host1.uuid
            bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, host1.uuid)
            bus.send(msg, new CloudBusCallBack(msg) {
                @Override
                void run(MessageReply reply) {
                    if (reply.isSuccess()) {
                        successCount.incrementAndGet()
                    } else if (reply.isCanceled()) {
                        cancelCount.incrementAndGet()
                    }
                    latch.countDown()
                }
            })
        }

        for (int i in 1..times2) {
            def msg = new ConnectHostMsg()
            msg.uuid = host2.uuid
            bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, host2.uuid)
            bus.send(msg, new CloudBusCallBack(msg) {
                @Override
                void run(MessageReply reply) {
                    if (reply.isSuccess()) {
                        successCount.incrementAndGet()
                    } else if (reply.isCanceled()) {
                        cancelCount.incrementAndGet()
                    }
                    latch.countDown()
                }
            })
        }

        latch.await()
        assert successCount.get() == 2
        assert cancelCount.get() == 18

        refreshHost()
        assert host1.status == HostStatus.Connected.toString()
        assert host2.status == HostStatus.Connected.toString()
    }

    void refreshHost() {
        host1 = queryHost {
            conditions = ["uuid=${host1.uuid}"]
        }[0] as HostInventory
        host2 = queryHost {
            conditions = ["uuid=${host2.uuid}"]
        }[0] as HostInventory
    }
}
