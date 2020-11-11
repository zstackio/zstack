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
class ConnectHostSingleFlightCase extends SubCase {
    EnvSpec env
    HostInventory host1
    HostInventory host2
    CloudBus bus

    AtomicInteger caller = new AtomicInteger(0)

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
            host1 = env.inventoryByName("kvm1") as HostInventory
            host2 = env.inventoryByName("kvm2") as HostInventory

            env.afterSimulator(KVMConstant.KVM_CONNECT_PATH) { KVMAgentCommands.AgentResponse rsp, HttpEntity<String> e ->
                sleep 1000
                caller.incrementAndGet()
                return rsp
            }

            testConnectHost(40)
            testConnectTwoHosts(20, 20)
            
            env.cleanAfterSimulatorHandlers()
            env.simulator(KVMConstant.KVM_CONNECT_PATH) {
                sleep 3000
                caller.incrementAndGet()
                throw new Exception("on purpose")
            }
            
            testReconnectFail(10)
            env.cleanSimulatorHandlers()
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
        caller.set(0)

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
        assert caller.get() == 1
        assert successCount.get() == times
        assert cancelCount.get() == 0

        refreshHost()
        assert host1.status == HostStatus.Connected.toString()
        assert host2.status == HostStatus.Connected.toString()
    }

    void testConnectTwoHosts(int times1, int times2) {
        CountDownLatch latch = new CountDownLatch(times1 + times2)
        AtomicInteger successCount1 = new AtomicInteger(0)
        AtomicInteger cancelCount1 = new AtomicInteger(0)
        AtomicInteger successCount2 = new AtomicInteger(0)
        AtomicInteger cancelCount2 = new AtomicInteger(0)
        caller.set(0)

        for (int i in 1..times1) {
            def msg = new ConnectHostMsg()
            msg.uuid = host1.uuid
            bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, host1.uuid)
            bus.send(msg, new CloudBusCallBack(msg) {
                @Override
                void run(MessageReply reply) {
                    if (reply.isSuccess()) {
                        successCount1.incrementAndGet()
                    } else if (reply.isCanceled()) {
                        cancelCount1.incrementAndGet()
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
                        successCount2.incrementAndGet()
                    } else if (reply.isCanceled()) {
                        cancelCount2.incrementAndGet()
                    }
                    latch.countDown()
                }
            })
        }

        latch.await()
        assert caller.get() == 2
        assert successCount1.get() == times1
        assert cancelCount1.get() == 0
        assert successCount2.get() == times2
        assert cancelCount2.get() == 0

        refreshHost()
        assert host1.status == HostStatus.Connected.toString()
        assert host2.status == HostStatus.Connected.toString()
    }

    /**
     * 问题来源: ZSTAC-31304
     */
    void testReconnectFail(int times) {
        AtomicInteger failCount = new AtomicInteger(0)
        Runnable r = {
            expect(AssertionError.class) {
                reconnectHost {
                    uuid = host1.uuid
                }
            }
            failCount.incrementAndGet()
        }
        
        caller.set(0)
        1.upto(times) {
            new Thread(r).start() // 子线程工作
        }
        sleep 100
        r.run() // 主线程工作
        
        assert caller.get() == 1
        retryInSecs {
            assert failCount.get() == (times + 1) // times 个子线程, 一个主线程
        }
    }

    void refreshHost() {
        host1 = (queryHost {
            conditions = ["uuid=${host1.uuid}"]
        } as List<HostInventory>)[0]
        host2 = (queryHost {
            conditions = ["uuid=${host2.uuid}"]
        } as List<HostInventory>)[0]
    }
}
