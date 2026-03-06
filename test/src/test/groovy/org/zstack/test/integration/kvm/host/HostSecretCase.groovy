package org.zstack.test.integration.kvm.host

import org.zstack.core.Platform
import org.zstack.core.cloudbus.CloudBus
import org.zstack.header.host.AddHostReply
import org.zstack.header.host.HostConstant
import org.zstack.header.host.HostInventory
import org.zstack.header.host.HostStatus
import org.zstack.header.message.MessageReply
import org.zstack.kvm.AddKVMHostMsg
import org.zstack.kvm.KVMConstant
import org.zstack.kvm.KVMAgentCommands
import org.zstack.storage.primary.local.LocalStorageKvmBackend
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.header.secret.SecretHostDefineMsg
import org.zstack.header.secret.SecretHostDefineReply

/**
 * Integration test for host secret: create/get/rotate/verify public key on connect,
 * and SecretHostDefine (ensure secret on agent).
 * Uses simulated agent for all secret paths.
 */
class HostSecretCase extends SubCase {
    EnvSpec env
    def cluster
    CloudBus bus
    HostInventory addedHost

    /** 32-byte X25519 public key (base64) for simulator; must be valid for HPKE seal. */
    static final String MOCK_PUBLIC_KEY_BASE64 = "AQIDBAUGBwgJCgsMDQ4PEBESExQVFhcYGRobHB0eHyA="

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = HostEnv.noHostBasicEnv()
    }

    @Override
    void test() {
        env.create {
            prepare()
            testAddHostWithSecretSync()
            testSecretHostDefineSuccess()
            testSecretHostDefineFailWhenNoDek()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void prepare() {
        cluster = env.inventoryByName("cluster")
        bus = bean(CloudBus.class)
    }

    void registerSecretSimulators() {
        env.simulator(KVMConstant.KVM_CONNECT_PATH) {
            def rsp = new KVMAgentCommands.ConnectResponse()
            rsp.success = true
            rsp.libvirtVersion = "1.0.0"
            rsp.qemuVersion = "1.3.0"
            return rsp
        }
        env.simulator(KVMConstant.KVM_HOST_FACT_PATH) {
            def rsp = new KVMAgentCommands.HostFactResponse()
            rsp.osDistribution = "CentOS"
            rsp.osVersion = "7.0"
            return rsp
        }
        env.simulator(LocalStorageKvmBackend.INIT_PATH) { rsp, _ -> return rsp }

        env.simulator(KVMConstant.KVM_CREATE_ENVELOPE_KEY_PATH) {
            return new KVMAgentCommands.CreatePublicKeyResponse()
        }
        env.simulator(KVMConstant.KVM_GET_ENVELOPE_KEY_PATH) {
            def rsp = new KVMAgentCommands.GetPublicKeyResponse()
            rsp.publicKey = MOCK_PUBLIC_KEY_BASE64
            return rsp
        }
        env.simulator(KVMConstant.KVM_VERIFY_ENVELOPE_KEY_PATH) {
            return new KVMAgentCommands.VerifyPublicKeyResponse()
        }
        env.simulator(KVMConstant.KVM_ROTATE_ENVELOPE_KEY_PATH) {
            return new KVMAgentCommands.RotatePublicKeyResponse()
        }
        env.simulator(KVMConstant.KVM_ENSURE_SECRET_PATH) {
            def rsp = new KVMAgentCommands.SecretHostDefineResponse()
            rsp.secretUuid = Platform.uuid
            return rsp
        }
    }

    void testAddHostWithSecretSync() {
        registerSecretSimulators()

        AddKVMHostMsg amsg = new AddKVMHostMsg()
        amsg.accountUuid = loginAsAdmin().accountUuid
        amsg.name = "kvm"
        amsg.managementIp = "127.0.0.2"
        amsg.resourceUuid = Platform.uuid
        amsg.clusterUuid = cluster.uuid
        amsg.setPassword("password")
        amsg.setUsername("root")

        bus.makeLocalServiceId(amsg, HostConstant.SERVICE_ID)
        AddHostReply reply = (AddHostReply) bus.call(amsg)
        assert reply != null
        assert reply.isSuccess()
        assert reply.inventory.status == HostStatus.Connected.toString()
        addedHost = reply.inventory
    }

    void testSecretHostDefineSuccess() {
        assert addedHost != null

        SecretHostDefineMsg msg = new SecretHostDefineMsg()
        msg.hostUuid = addedHost.uuid
        msg.dekBase64 = "dGVzdERFSw=="  // base64 of "testDEK"

        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, addedHost.uuid)
        MessageReply reply = bus.call(msg)
        assert reply != null
        assert reply.isSuccess()
        SecretHostDefineReply defineReply = reply.castReply()
        assert defineReply.secretUuid != null
    }

    void testSecretHostDefineFailWhenNoDek() {
        assert addedHost != null

        SecretHostDefineMsg msg = new SecretHostDefineMsg()
        msg.hostUuid = addedHost.uuid
        msg.dekBase64 = null

        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, addedHost.uuid)
        MessageReply reply = bus.call(msg)
        assert reply != null
        assert !reply.isSuccess()
        assert reply.error != null
    }
}