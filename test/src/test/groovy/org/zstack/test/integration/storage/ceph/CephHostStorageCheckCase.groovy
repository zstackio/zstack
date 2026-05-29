package org.zstack.test.integration.storage.ceph

import org.springframework.http.HttpEntity
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.cloudbus.CloudBusCallBack
import org.zstack.header.host.HostConstant
import org.zstack.header.message.MessageReply
import org.zstack.header.storage.primary.PrimaryStorageConstant
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMHostAsyncHttpCallMsg
import org.zstack.kvm.KVMHostFactory
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
import java.util.concurrent.atomic.AtomicReference

class CephHostStorageCheckCase extends SubCase {
    EnvSpec env
    CloudBus bus

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
            testCheckNotSerializedAcrossHosts()
            testPerMessageTimeoutHonored()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    // ZSTAC-85421: a stuck per-host check must not block other hosts' check on the
    // same primary storage. The chain syncLevel is raised from 1 to 10 so the second
    // host's check runs concurrently instead of queueing behind the stuck one.
    void testCheckNotSerializedAcrossHosts() {
        def ps = env.inventoryByName("ceph-pri") as PrimaryStorageInventory
        def host1 = env.inventoryByName("host1") as HostInventory
        def host2 = env.inventoryByName("host2") as HostInventory

        CountDownLatch host1Entered = new CountDownLatch(1)
        CountDownLatch release = new CountDownLatch(1)

        // ceph registers the check path with a short timeout via
        // KVMAgentHttpTimeoutExtensionPoint; the value comes from the
        // agent.connectivityCheck.timeout global config (default 300s).
        // When sending, KVMHost clamps it down to the inherited total timeout.
        assert bean(KVMHostFactory.class).getAgentHttpShortTimeout(CephPrimaryStorageBase.CHECK_HOST_STORAGE_CONNECTION_PATH) == TimeUnit.MINUTES.toMillis(5)

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
        assert host1Entered.await(10, TimeUnit.SECONDS)

        AtomicReference<MessageReply> reply2 = new AtomicReference<>()
        CountDownLatch reply2Done = new CountDownLatch(1)
        sendCheckMsg(ps.uuid, host2.uuid, { MessageReply r -> reply2.set(r); reply2Done.countDown() })

        assert reply2Done.await(15, TimeUnit.SECONDS)
        assert reply2.get().isSuccess()
        assert reply1Done.getCount() == 1

        release.countDown()
        assert reply1Done.await(15, TimeUnit.SECONDS)
    }

    // ZSTAC-85421: a KVMHostAsyncHttpCallMsg carrying an explicit timeout must fail at
    // that timeout instead of riding the default 1800s. The ceph check relies on this to
    // limit its blast radius to 5 minutes.
    void testPerMessageTimeoutHonored() {
        def host1 = env.inventoryByName("host1") as HostInventory
        def stuckPath = "/test/zstac85421/stuck"
        CountDownLatch release = new CountDownLatch(1)

        env.simulator(stuckPath) { HttpEntity<String> e ->
            release.await(60, TimeUnit.SECONDS)
            return new KVMAgentCommands.AgentResponse()
        }

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg()
        msg.setHostUuid(host1.uuid)
        msg.setPath(stuckPath)
        msg.setCommand(new KVMAgentCommands.AgentCommand())
        msg.setNoStatusCheck(true)
        msg.setTimeout(TimeUnit.SECONDS.toMillis(3))
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, host1.uuid)

        AtomicReference<MessageReply> reply = new AtomicReference<>()
        CountDownLatch done = new CountDownLatch(1)
        long start = System.currentTimeMillis()
        bus.send(msg, new CloudBusCallBack(null) {
            @Override
            void run(MessageReply r) {
                reply.set(r)
                done.countDown()
            }
        })

        assert done.await(20, TimeUnit.SECONDS)
        long elapsed = System.currentTimeMillis() - start
        assert !reply.get().isSuccess()
        assert elapsed >= 2000
        assert elapsed < 20000

        release.countDown()
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
