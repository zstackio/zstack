package org.zstack.test.unittest.storage.encrypt

import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.zstack.core.cloudbus.CloudBus
import org.zstack.header.keyprovider.EncryptedResourceKeyManager
import org.zstack.header.message.MessageReply
import org.zstack.header.message.NeedReplyMessage
import org.zstack.header.secret.SecretHostDefineMsg
import org.zstack.header.secret.SecretHostDefineReply
import org.zstack.header.secret.SecretHostDeleteMsg
import org.zstack.header.secret.SecretHostGetMsg
import org.zstack.header.secret.SecretHostGetReply
import org.zstack.kvm.KVMHostAsyncHttpCallMsg
import org.zstack.kvm.KVMHostAsyncHttpCallReply
import org.zstack.kvm.KVMAgentCommands
import org.zstack.storage.encrypt.VolumeEncryptedResourceKeyBackend
import org.zstack.storage.encrypt.VolumeEncryptedSecretHelper
import org.zstack.utils.gson.JSONObjectUtil

import java.lang.reflect.Field

class VolumeEncryptedSecretHelperTest {
    private static final String SRC_HOST_UUID = "src-host"
    private static final String DST_HOST_UUID = "dst-host"
    private static final String VM_UUID = "vm-uuid"
    private static final String VOLUME_UUID = "volume-uuid"
    private static final String SOURCE_SECRET_UUID = "source-secret"
    private static final String STALE_DEST_SECRET_UUID = "stale-dest-secret"
    private static final int KEY_VERSION = 1

    private VolumeEncryptedSecretHelper helper
    private CloudBus bus
    private VolumeEncryptedResourceKeyBackend keyBackend
    private List<Object> calls

    @Before
    void setUp() {
        helper = new VolumeEncryptedSecretHelper()
        bus = Mockito.mock(CloudBus.class)
        keyBackend = Mockito.mock(VolumeEncryptedResourceKeyBackend.class)
        calls = []

        setField("bus", bus)
        setField("volumeEncryptedResourceKeyBackend", keyBackend)

        Mockito.when(keyBackend.findKeyVersionByVolume(VOLUME_UUID)).thenReturn(KEY_VERSION)
        Mockito.when(keyBackend.findKeyProviderUuidByVolume(VOLUME_UUID)).thenReturn("key-provider-uuid")

        def keyResult = new EncryptedResourceKeyManager.ResourceKeyResult()
        keyResult.dekBase64 = Base64.encoder.encodeToString("0123456789abcdef".bytes)
        keyResult.keyVersion = KEY_VERSION
        setField("encryptedResourceKeyManager", materializeDekManager(keyResult))

        Mockito.doAnswer { invocation ->
            def msg = invocation.arguments[0]
            calls.add(msg)

            if (msg instanceof KVMHostAsyncHttpCallMsg) {
                return sourceDomainSecretReply()
            }
            if (msg instanceof SecretHostGetMsg) {
                return secretGetReply(msg.hostUuid == DST_HOST_UUID ? STALE_DEST_SECRET_UUID : SOURCE_SECRET_UUID)
            }
            if (msg instanceof SecretHostDeleteMsg) {
                return new MessageReply()
            }
            if (msg instanceof SecretHostDefineMsg) {
                def reply = new SecretHostDefineReply()
                reply.secretUuid = msg.secretUuid
                return reply
            }
            return new MessageReply()
        }.when(bus).call(Mockito.any(NeedReplyMessage.class) as NeedReplyMessage)
    }

    @Test
    void testMigrationReplacesStaleDestinationSecretBeforeDefine() {
        def secretUuid = helper.resolveOrDefineSecretForVolumeMigration(
                SRC_HOST_UUID, DST_HOST_UUID, VM_UUID, VOLUME_UUID)

        assert secretUuid == SOURCE_SECRET_UUID :
                "destination host should use the secret UUID from source VM domain XML: expected=${SOURCE_SECRET_UUID} actual=${secretUuid}"

        def deleteIndex = calls.findIndexOf { it instanceof SecretHostDeleteMsg }
        def defineIndex = calls.findIndexOf { it instanceof SecretHostDefineMsg }

        assert deleteIndex >= 0 :
                "destination host has stale secret[${STALE_DEST_SECRET_UUID}], the old secret with same usage must be deleted first"
        assert defineIndex > deleteIndex :
                "destination stale secret must be deleted before redefining the source secret UUID: deleteIndex=${deleteIndex} defineIndex=${defineIndex}"

        SecretHostDeleteMsg deleteMsg = calls[deleteIndex] as SecretHostDeleteMsg
        assert deleteMsg.hostUuid == DST_HOST_UUID :
                "stale secret must be deleted on the destination host: expected=${DST_HOST_UUID} actual=${deleteMsg.hostUuid}"
        assert deleteMsg.usageInstance == "volume-${VOLUME_UUID}" :
                "stale secret deletion must target the volume usage: expected=volume-${VOLUME_UUID} actual=${deleteMsg.usageInstance}"

        SecretHostDefineMsg defineMsg = calls[defineIndex] as SecretHostDefineMsg
        assert defineMsg.hostUuid == DST_HOST_UUID :
                "secret must be redefined on the destination host: expected=${DST_HOST_UUID} actual=${defineMsg.hostUuid}"
        assert defineMsg.secretUuid == SOURCE_SECRET_UUID :
                "secret redefine must use the source VM domain XML secret UUID: expected=${SOURCE_SECRET_UUID} actual=${defineMsg.secretUuid}"
    }

    private MessageReply secretGetReply(String secretUuid) {
        def reply = new SecretHostGetReply()
        reply.secretUuid = secretUuid
        return reply
    }

    private KVMHostAsyncHttpCallReply sourceDomainSecretReply() {
        def rsp = new KVMAgentCommands.ResolveVolumeLibvirtSecretResponse()
        rsp.success = true
        rsp.secretUuid = SOURCE_SECRET_UUID

        def reply = new KVMHostAsyncHttpCallReply()
        reply.response = JSONObjectUtil.toObject(JSONObjectUtil.toJsonString(rsp), LinkedHashMap.class)
        return reply
    }

    private EncryptedResourceKeyManager materializeDekManager(EncryptedResourceKeyManager.ResourceKeyResult keyResult) {
        def manager = Mockito.mock(EncryptedResourceKeyManager.class)
        Mockito.doAnswer { invocation ->
            def completion = invocation.arguments[1]
            completion.success(keyResult)
            return null
        }.when(manager).getOrCreateKey(Mockito.any(), Mockito.any())
        return manager
    }

    private void setField(String name, Object value) {
        Field field = VolumeEncryptedSecretHelper.class.getDeclaredField(name)
        field.accessible = true
        field.set(helper, value)
    }
}
