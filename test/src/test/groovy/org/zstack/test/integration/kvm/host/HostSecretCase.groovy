package org.zstack.test.integration.kvm.host

import org.zstack.core.Platform
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.SimpleQuery
import org.zstack.core.db.SimpleQuery.Op
import org.zstack.header.host.AddHostReply
import org.zstack.header.host.HostConstant
import org.zstack.header.host.HostInventory
import org.zstack.header.host.HostKeyIdentityVO
import org.zstack.header.host.HostKeyIdentityVO_
import org.zstack.header.host.HostStatus
import org.zstack.header.host.PingHostMsg
import org.zstack.header.host.PingHostReply
import org.zstack.header.message.MessageReply
import org.zstack.kvm.AddKVMHostMsg
import org.zstack.kvm.KVMConstant
import org.zstack.kvm.KVMAgentCommands
import org.zstack.storage.primary.local.LocalStorageKvmBackend
import org.zstack.test.integration.kvm.KvmTest
import org.springframework.http.HttpEntity
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.header.secret.SecretHostDefineMsg
import org.zstack.header.secret.SecretHostDefineReply

import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicInteger

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

    /** Counters for simulator call assertions (async secret sync / ensureSecret). */
    AtomicInteger createEnvelopeKeyCallCount
    AtomicInteger ensureSecretCallCount

    /** 32-byte X25519 public key (base64) for simulator; must be valid for HPKE seal. */
    static final String MOCK_PUBLIC_KEY_BASE64 = "AQIDBAUGBwgJCgsMDQ4PEBESExQVFhcYGRobHB0eHyA="

    /** Same algorithm as KVMHost.fingerprintFromPublicKey: SHA-256(decoded base64) in hex. */
    static String fingerprintFromPublicKey(String publicKeyBase64) {
        if (publicKeyBase64 == null || publicKeyBase64.isEmpty()) return ""
        try {
            byte[] keyBytes = java.util.Base64.getDecoder().decode(publicKeyBase64.trim())
            if (keyBytes == null || keyBytes.length == 0) return ""
            MessageDigest md = MessageDigest.getInstance("SHA-256")
            byte[] hash = md.digest(keyBytes)
            StringBuilder sb = new StringBuilder(hash.length * 2)
            for (byte b : hash) sb.append(String.format("%02x", b & 0xff))
            return sb.toString()
        } catch (Exception e) {
            return ""
        }
    }

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
        createEnvelopeKeyCallCount = new AtomicInteger(0)
        ensureSecretCallCount = new AtomicInteger(0)

        env.simulator(KVMConstant.KVM_CONNECT_PATH) {
            def rsp = new KVMAgentCommands.ConnectResponse()
            rsp.success = true
            rsp.libvirtVersion = "1.0.0"
            rsp.qemuVersion = "1.3.0"
            return rsp
        }
        // Use afterSimulator like AddHostCase: rely on testlib default HostFactResponse, only set what this test needs.
        env.afterSimulator(KVMConstant.KVM_HOST_FACT_PATH) { KVMAgentCommands.HostFactResponse rsp ->
            rsp.hvmCpuFlag = "vmx"  // default is ""; connect needs vmx/svm to pass checkVirtualizationEnabled
            return rsp
        }
        env.simulator(LocalStorageKvmBackend.INIT_PATH) { HttpEntity<String> e ->
            def rsp = new LocalStorageKvmBackend.InitRsp()
            rsp.success = true
            rsp.localStorageUsedCapacity = 0L
            rsp.totalCapacity = 0L
            rsp.availableCapacity = 0L
            return rsp
        }

        // Ping simulator so we can trigger pingHook (which runs sync-envelope-public-key -> KVM_CREATE_ENVELOPE_KEY_PATH).
        // needReconnectHost() is true when rsp.version != dbf.getDbVersion(), which sets KVM_HOST_SKIP_PING_NO_FAILURE_EXTENSIONS
        // and skips sync-envelope-public-key; so we must return the actual DB version.
        def dbVersion = bean(DatabaseFacade.class).getDbVersion()
        env.simulator(KVMConstant.KVM_PING_PATH) { HttpEntity<String> e ->
            def cmd = org.zstack.utils.gson.JSONObjectUtil.toObject(e.body, KVMAgentCommands.PingCmd.class)
            def rsp = new KVMAgentCommands.PingResponse()
            rsp.success = true
            rsp.hostUuid = cmd.hostUuid
            rsp.version = dbVersion
            rsp.sendCommandUrl = "http://127.0.0.2:7272"
            return rsp
        }

        env.simulator(KVMConstant.KVM_CREATE_ENVELOPE_KEY_PATH) {
            createEnvelopeKeyCallCount?.incrementAndGet()
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
            ensureSecretCallCount?.incrementAndGet()
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
        assert reply.isSuccess() : "AddHost failed: ${reply.error?.toString() ?: 'no error'}"
        assert reply.inventory.status == HostStatus.Connected.toString()
        addedHost = reply.inventory

        // Envelope key sync runs inside pingHook, not during connect. Trigger a ping so that
        // sync-envelope-public-key runs and KVM_CREATE_ENVELOPE_KEY_PATH is invoked.
        PingHostMsg pingMsg = new PingHostMsg()
        pingMsg.hostUuid = addedHost.uuid
        bus.makeTargetServiceIdByResourceUuid(pingMsg, HostConstant.SERVICE_ID, addedHost.uuid)
        MessageReply pingReply = bus.call(pingMsg)
        assert pingReply.isSuccess() : "PingHost failed: ${pingReply.error}"

        assert createEnvelopeKeyCallCount.get() >= 1 : "envelope key sync (KVM_CREATE_ENVELOPE_KEY_PATH) should be triggered at least once after add host"

        // Create/ping may already persist HostKeyIdentityVO (sync path calls GET then saveOrUpdateHostKeyIdentity).
        // Ensure HostKeyIdentity exists with expected key so SecretHostDefineMsg finds it and fingerprint check passes.
        DatabaseFacade dbf = bean(DatabaseFacade.class)
        SimpleQuery<HostKeyIdentityVO> q = dbf.createQuery(HostKeyIdentityVO.class)
        q.add(HostKeyIdentityVO_.hostUuid, Op.EQ, addedHost.uuid)
        HostKeyIdentityVO keyVo = q.find()
        if (keyVo == null) {
            keyVo = new HostKeyIdentityVO()
            keyVo.hostUuid = addedHost.uuid
            keyVo.publicKey = MOCK_PUBLIC_KEY_BASE64
            keyVo.fingerprint = fingerprintFromPublicKey(MOCK_PUBLIC_KEY_BASE64)
            keyVo.verified = true
            dbf.persist(keyVo)
        } else {
            keyVo.publicKey = MOCK_PUBLIC_KEY_BASE64
            keyVo.fingerprint = fingerprintFromPublicKey(MOCK_PUBLIC_KEY_BASE64)
            keyVo.verified = true
            dbf.update(keyVo)
        }
    }

    void testSecretHostDefineSuccess() {
        assert addedHost != null

        int countBefore = ensureSecretCallCount.get()

        SecretHostDefineMsg msg = new SecretHostDefineMsg()
        msg.hostUuid = addedHost.uuid
        msg.dekBase64 = "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8="
        msg.vmUuid = Platform.uuid
        msg.purpose = "test-vtpm"
        msg.providerName = "vtpm"

        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, addedHost.uuid)
        MessageReply reply = bus.call(msg)
        assert reply != null
        assert reply.isSuccess()
        SecretHostDefineReply defineReply = reply.castReply()
        assert defineReply.secretUuid != null

        // Ensure KVM_ENSURE_SECRET_PATH was actually called (asyncJsonPost to agent).
        assert ensureSecretCallCount.get() == countBefore + 1 : "KVM_ENSURE_SECRET_PATH simulator should be called exactly once for SecretHostDefineMsg"
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