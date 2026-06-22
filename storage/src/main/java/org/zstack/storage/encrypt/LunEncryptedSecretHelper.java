package org.zstack.storage.encrypt;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.header.core.FutureReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.HostConstant;
import org.zstack.header.keyprovider.EncryptedResourceKeyManager;
import org.zstack.header.message.MessageReply;
import org.zstack.header.secret.SecretHostDefineMsg;
import org.zstack.header.secret.SecretHostDefineReply;
import org.zstack.header.secret.SecretHostDeleteMsg;
import org.zstack.header.secret.SecretHostGetMsg;
import org.zstack.header.secret.SecretHostGetReply;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.TimeUnit;

import static org.zstack.core.Platform.operr;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class LunEncryptedSecretHelper {
    private static final CLogger logger = Utils.getLogger(LunEncryptedSecretHelper.class);
    private static final String LUN_RESOURCE_TYPE = "LunVO";
    private static final String SECRET_PURPOSE = "scsi-lun";

    @Autowired
    private CloudBus bus;
    @Autowired
    private EncryptedResourceKeyManager encryptedResourceKeyManager;
    @Autowired
    private VolumeEncryptedSecretHelper volumeSecretHelper;

    public String prepareLuksEnvelopeDekOnHost(String hostUuid, String lunUuid, String kpUuid) {
        EncryptedResourceKeyManager.ResourceKeyResult keyResult = materializeDek(lunUuid, kpUuid);
        return volumeSecretHelper.prepareLuksEnvelopeDekOnHost(hostUuid, lunUuid, keyResult.getDekBase64());
    }

    public String resolveOrDefineSecretOnHost(String hostUuid, String vmUuid, String lunUuid,
                                              String kpUuid, Integer keyVersion) {
        if (keyVersion == null) {
            throw new OperationFailureException(operr(
                    "encrypted scsi lun[uuid:%s] has no key version bound; cannot resolve libvirt LUKS secret on host[uuid:%s] for vm[uuid:%s]",
                    lunUuid, hostUuid, vmUuid));
        }

        String secretUuid = getSecretOnHost(hostUuid, vmUuid, lunUuid, keyVersion);
        if (StringUtils.isNotBlank(secretUuid)) {
            return secretUuid;
        }
        return defineSecretFromBinding(hostUuid, vmUuid, lunUuid, kpUuid, null);
    }

    public String resolveOrDefineSecretForMigration(String srcHostUuid, String dstHostUuid, String vmUuid,
                                                    String lunUuid, String kpUuid, Integer keyVersion) {
        if (StringUtils.isBlank(srcHostUuid) || StringUtils.isBlank(dstHostUuid)
                || StringUtils.isBlank(vmUuid) || StringUtils.isBlank(lunUuid)) {
            throw new OperationFailureException(operr(
                    "resolve migration LUKS secret requires non-blank srcHostUuid, dstHostUuid, vmUuid and lunUuid"));
        }

        if (keyVersion == null) {
            throw new OperationFailureException(operr(
                    "encrypted scsi lun[uuid:%s] has no key version bound; cannot resolve migration LUKS secret for vm[uuid:%s]",
                    lunUuid, vmUuid));
        }

        String sourceSecretUuid = getSecretOnHost(srcHostUuid, vmUuid, lunUuid, keyVersion);
        if (StringUtils.isBlank(sourceSecretUuid)) {
            sourceSecretUuid = defineSecretFromBinding(srcHostUuid, vmUuid, lunUuid, kpUuid, null);
        }

        String destSecretUuid = getSecretOnHost(dstHostUuid, vmUuid, lunUuid, keyVersion);
        if (StringUtils.equals(destSecretUuid, sourceSecretUuid)) {
            return destSecretUuid;
        }

        return defineSecretFromBinding(dstHostUuid, vmUuid, lunUuid, kpUuid, sourceSecretUuid);
    }

    public void deleteSecretOnHostBestEffort(String hostUuid, String vmUuid, String lunUuid, Integer keyVersion) {
        if (StringUtils.isBlank(hostUuid) || StringUtils.isBlank(vmUuid)
                || StringUtils.isBlank(lunUuid) || keyVersion == null) {
            return;
        }

        SecretHostDeleteMsg msg = new SecretHostDeleteMsg();
        msg.setHostUuid(hostUuid);
        msg.setVmUuid(vmUuid);
        msg.setPurpose(SECRET_PURPOSE);
        msg.setKeyVersion(keyVersion);
        msg.setUsageInstance(usageInstance(lunUuid));
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);

        MessageReply reply = bus.call(msg);
        if (!reply.isSuccess()) {
            logger.warn(String.format(
                    "best-effort delete libvirt LUKS secret failed for scsi lun[uuid:%s] on host[uuid:%s] vm[uuid:%s]: %s",
                    lunUuid, hostUuid, vmUuid, reply.getError()));
        }
    }

    private EncryptedResourceKeyManager.ResourceKeyResult materializeDek(String lunUuid, String kpUuid) {
        if (StringUtils.isBlank(kpUuid)) {
            throw new OperationFailureException(operr(
                    "encrypted scsi lun[uuid:%s] has no key provider binding", lunUuid));
        }

        EncryptedResourceKeyManager.GetOrCreateResourceKeyContext ctx =
                new EncryptedResourceKeyManager.GetOrCreateResourceKeyContext();
        ctx.setResourceUuid(lunUuid);
        ctx.setResourceType(LUN_RESOURCE_TYPE);
        ctx.setKeyProviderUuid(kpUuid);
        ctx.setPurpose("instantiate-scsi-lun");

        FutureReturnValueCompletion completion = new FutureReturnValueCompletion(null);
        encryptedResourceKeyManager.getOrCreateKey(ctx, completion);
        completion.await(TimeUnit.MINUTES.toMillis(5));
        if (!completion.isSuccess()) {
            throw new OperationFailureException(operr(
                    "failed to materialize encryption key for scsi lun[uuid:%s]", lunUuid)
                    .withCause(completion.getErrorCode()));
        }

        EncryptedResourceKeyManager.ResourceKeyResult result = completion.getResult();
        if (result == null || StringUtils.isBlank(result.getDekBase64())) {
            throw new OperationFailureException(operr(
                    "key manager returned empty DEK for encrypted scsi lun[uuid:%s]", lunUuid));
        }
        return result;
    }

    private String defineSecretFromBinding(String hostUuid, String vmUuid, String lunUuid,
                                           String kpUuid, String secretUuid) {
        EncryptedResourceKeyManager.ResourceKeyResult keyResult = materializeDek(lunUuid, kpUuid);
        return defineLibvirtSecretOnHost(hostUuid, vmUuid, lunUuid,
                keyResult.getDekBase64(), keyResult.getKeyVersion(), secretUuid);
    }

    private String defineLibvirtSecretOnHost(String hostUuid, String vmUuid, String lunUuid,
                                             String dekBase64, Integer keyVersion, String secretUuid) {
        if (StringUtils.isBlank(hostUuid) || StringUtils.isBlank(lunUuid)
                || StringUtils.isBlank(dekBase64) || keyVersion == null) {
            throw new OperationFailureException(operr(
                    "define scsi lun libvirt secret requires non-blank hostUuid, lunUuid, dekBase64 and a non-null keyVersion"));
        }

        SecretHostDefineMsg msg = new SecretHostDefineMsg();
        msg.setHostUuid(hostUuid);
        msg.setVmUuid(vmUuid);
        msg.setDekBase64(dekBase64);
        msg.setPurpose(SECRET_PURPOSE);
        msg.setKeyVersion(keyVersion);
        msg.setUsageInstance(usageInstance(lunUuid));
        if (StringUtils.isNotBlank(secretUuid)) {
            msg.setSecretUuid(secretUuid);
        }
        msg.setDescription(String.format("LUKS DEK for scsi lun %s", lunUuid));
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);

        MessageReply reply = bus.call(msg);
        if (!reply.isSuccess()) {
            throw new OperationFailureException(operr(
                    "failed to ensure libvirt secret for encrypted scsi lun[uuid:%s] on host[uuid:%s]",
                    lunUuid, hostUuid).withCause(reply.getError()));
        }

        SecretHostDefineReply r = reply.castReply();
        if (StringUtils.isBlank(r.getSecretUuid())) {
            throw new OperationFailureException(operr(
                    "ensure scsi lun LUKS secret on host succeeded but secretUuid is empty, host[uuid:%s]",
                    hostUuid));
        }
        return r.getSecretUuid();
    }

    private String getSecretOnHost(String hostUuid, String vmUuid, String lunUuid, Integer keyVersion) {
        SecretHostGetMsg msg = new SecretHostGetMsg();
        msg.setHostUuid(hostUuid);
        msg.setVmUuid(vmUuid);
        msg.setPurpose(SECRET_PURPOSE);
        msg.setKeyVersion(keyVersion);
        msg.setUsageInstance(usageInstance(lunUuid));
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);

        MessageReply reply = bus.call(msg);
        if (reply.isSuccess()) {
            SecretHostGetReply r = reply.castReply();
            return r.getSecretUuid();
        }

        ErrorCode err = reply.getError();
        if (err != null && SecretHostGetReply.ERROR_CODE_SECRET_NOT_FOUND.equals(err.getCode())) {
            return null;
        }
        throw new OperationFailureException(operr(
                "failed to get libvirt LUKS secret on host[uuid:%s] vm[uuid:%s] scsi lun[uuid:%s] keyVersion[%s]: %s",
                hostUuid, vmUuid, lunUuid, keyVersion, err));
    }

    private String usageInstance(String lunUuid) {
        return String.format("scsi-lun-%s", lunUuid);
    }
}
