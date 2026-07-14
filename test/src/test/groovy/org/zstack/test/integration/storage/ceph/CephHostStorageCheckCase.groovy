package org.zstack.test.integration.storage.ceph

import org.springframework.http.HttpEntity
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.cloudbus.CloudBusCallBack
import org.zstack.header.host.HostConstant
import org.zstack.header.message.MessageReply
import org.zstack.header.rest.BeforeAsyncJsonPostInterceptor
import org.zstack.header.rest.RESTFacade
import org.zstack.header.storage.primary.PrimaryStorageConstant
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMGlobalConfig
import org.zstack.kvm.KVMHostAsyncHttpCallMsg
import org.zstack.sdk.HostInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.storage.ceph.primary.CephPrimaryStorageBase
import org.zstack.storage.primary.CheckHostStorageConnectionMsg
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

class CephHostStorageCheckCase extends SubCase {
    EnvSpec env
    CloudBus bus
    RESTFacade restf

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = makeEnv {
            zone {
                name = "zone"
                cluster {
                    name = "test-cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "host1"
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                        usedMem = 1000
                        totalCpu = 10
                    }

                    kvm {
                        name = "host2"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"
                        usedMem = 1000
                        totalCpu = 10
                    }

                    attachPrimaryStorage("ceph-pri")
                }

                cephPrimaryStorage {
                    name = "ceph-pri"
                    description = "Test"
                    totalCapacity = SizeUnit.GIGABYTE.toByte(100)
                    availableCapacity = SizeUnit.GIGABYTE.toByte(100)
                    url = "ceph://pri"
                    fsid = "7ff218d9-f525-435f-8a40-3618d1772a64"
                    monUrls = ["root:password@localhost/?monPort=7777"]
                }
            }
        }
    }

    @Override
    void test() {
        env.create {
            bus = bean(CloudBus.class)
            restf = bean(RESTFacade.class)
            testConnectivityCheckTimeoutRejectsValuesBelowOneSecond()
            testCheckNotSerializedAcrossHosts()
            testConnectivityCheckUsesConfiguredTimeoutBeforeSend()
            testConnectivityCheckUsesRemainingDeadlineBeforeSend()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void testConnectivityCheckTimeoutRejectsValuesBelowOneSecond() {
        expect(AssertionError.class) {
            updateConnectivityCheckTimeout(0)
        }
    }

    void testCheckNotSerializedAcrossHosts() {
        def ps = env.inventoryByName("ceph-pri") as PrimaryStorageInventory
        def host1 = env.inventoryByName("host1") as HostInventory
        def host2 = env.inventoryByName("host2") as HostInventory

        CountDownLatch host1Entered = new CountDownLatch(1)
        CountDownLatch release = new CountDownLatch(1)

        env.simulator(CephPrimaryStorageBase.CHECK_HOST_STORAGE_CONNECTION_PATH) { HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, CephPrimaryStorageBase.CheckHostStorageConnectionCmd)
            if (cmd.hostUuid == host1.uuid) {
                host1Entered.countDown()
                release.await(60, TimeUnit.SECONDS)
            }
            return new KVMAgentCommands.AgentResponse()
        }

        CountDownLatch reply1Done = new CountDownLatch(1)
        sendCheckMsg(ps.uuid, host1.uuid, { MessageReply r -> reply1Done.countDown() })
        assert host1Entered.await(10, TimeUnit.SECONDS) :
                "stuck host check did not enter the real Ceph path within 10 seconds"

        AtomicReference<MessageReply> reply2 = new AtomicReference<>()
        CountDownLatch reply2Done = new CountDownLatch(1)
        sendCheckMsg(ps.uuid, host2.uuid, { MessageReply r -> reply2.set(r); reply2Done.countDown() })

        assert reply2Done.await(15, TimeUnit.SECONDS) :
                "healthy host check was blocked by another host on the same Ceph primary storage"
        assert reply2.get().isSuccess() :
                "healthy host check failed while the stuck host occupied another concurrency slot: actual=${reply2.get().error}"
        assert reply1Done.getCount() == 1 :
                "stuck host check unexpectedly completed before release: remaining=${reply1Done.getCount()}"

        release.countDown()
        assert reply1Done.await(15, TimeUnit.SECONDS) :
                "stuck host check did not complete after its simulator was released"
    }

    void testConnectivityCheckUsesConfiguredTimeoutBeforeSend() {
        def host1 = env.inventoryByName("host1") as HostInventory
        long originalTimeout = KVMGlobalConfig.AGENT_CONNECTIVITY_CHECK_TIMEOUT.value(Long.class)
        long expectedTimeout = TimeUnit.SECONDS.toMillis(2)

        updateConnectivityCheckTimeout(2)
        long actualTimeout = captureConnectivityCheckTimeout(host1.uuid) {
            reconnectHost {
                uuid = host1.uuid
                apiTimeout = 30L
            }
        }

        assert actualTimeout == expectedTimeout :
                "real ReconnectHost Ceph check did not use agent.connectivityCheck.timeout before CloudBus send: " +
                        "expected=${expectedTimeout}ms actual=${actualTimeout}ms"
        updateConnectivityCheckTimeout(originalTimeout)
    }

    void testConnectivityCheckUsesRemainingDeadlineBeforeSend() {
        def host1 = env.inventoryByName("host1") as HostInventory
        long originalTimeout = KVMGlobalConfig.AGENT_CONNECTIVITY_CHECK_TIMEOUT.value(Long.class)
        long remaining = TimeUnit.SECONDS.toMillis(15)

        updateConnectivityCheckTimeout(60)
        long actualTimeout = captureConnectivityCheckTimeout(host1.uuid) {
            sendKvmConnectivityCheck(host1.uuid, TimeUnit.SECONDS.toMillis(30),
                    System.currentTimeMillis() + remaining)
        }

        assert actualTimeout >= remaining - TimeUnit.SECONDS.toMillis(5) && actualTimeout <= remaining :
                "Ceph check exceeded the parent remaining deadline before CloudBus send: " +
                        "expected=[${remaining - TimeUnit.SECONDS.toMillis(5)},${remaining}]ms actual=${actualTimeout}ms"
        updateConnectivityCheckTimeout(originalTimeout)
    }

    private void sendKvmConnectivityCheck(String hostUuid, long timeout, long messageDeadline) {
        CephPrimaryStorageBase.CheckHostStorageConnectionCmd cmd =
                new CephPrimaryStorageBase.CheckHostStorageConnectionCmd()
        cmd.setHostUuid(hostUuid)

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg()
        msg.setHostUuid(hostUuid)
        msg.setPath(CephPrimaryStorageBase.CHECK_HOST_STORAGE_CONNECTION_PATH)
        msg.setCommand(cmd)
        msg.setNoStatusCheck(true)
        msg.setTimeout(timeout)
        msg.setMessageDeadline(messageDeadline)
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid)

        CountDownLatch done = new CountDownLatch(1)
        bus.send(msg, new CloudBusCallBack(null) {
            @Override
            void run(MessageReply reply) {
                done.countDown()
            }
        })
        assert done.await(10, TimeUnit.SECONDS) :
                "direct KVM connectivity check did not complete while verifying the pre-send timeout hook"
    }

    private long captureConnectivityCheckTimeout(String hostUuid, Closure trigger) {
        AtomicLong captured = new AtomicLong(-1)
        CountDownLatch capturedDone = new CountDownLatch(1)
        Runnable close = restf.installBeforeAsyncJsonPostInterceptor(new BeforeAsyncJsonPostInterceptor() {
            @Override
            void beforeAsyncJsonPost(String url, Object body, TimeUnit unit, long timeout) {
            }

            @Override
            void beforeAsyncJsonPost(String url, String body, TimeUnit unit, long timeout) {
                if (!url.endsWith(CephPrimaryStorageBase.CHECK_HOST_STORAGE_CONNECTION_PATH)) {
                    return
                }

                def cmd = JSONObjectUtil.toObject(body, CephPrimaryStorageBase.CheckHostStorageConnectionCmd)
                if (cmd.hostUuid == hostUuid) {
                    captured.set(unit.toMillis(timeout))
                    capturedDone.countDown()
                }
            }
        })

        trigger()
        assert capturedDone.await(10, TimeUnit.SECONDS) :
                "real Ceph host storage connection path was not invoked for host=${hostUuid}"
        close.run()
        return captured.get()
    }

    private void updateConnectivityCheckTimeout(long timeout) {
        updateGlobalConfig {
            category = KVMGlobalConfig.CATEGORY
            name = "agent.connectivityCheck.timeout"
            value = timeout.toString()
        }
    }

    private void sendCheckMsg(String psUuid, String hostUuid, Closure cb) {
        CheckHostStorageConnectionMsg msg = new CheckHostStorageConnectionMsg()
        msg.setPrimaryStorageUuid(psUuid)
        msg.setHostUuids([hostUuid])
        bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, psUuid)
        bus.send(msg, new CloudBusCallBack(null) {
            @Override
            void run(MessageReply r) {
                if (cb != null) {
                    cb(r)
                }
            }
        })
    }
}
