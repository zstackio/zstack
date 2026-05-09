package org.zstack.storage.encrypt;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.Q;
import org.zstack.header.core.ReturnValueCompletion;
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
import org.zstack.header.secret.SecretHostEnsureLuksSecretFileMsg;
import org.zstack.header.secret.SecretHostEnsureLuksSecretFileReply;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.kvm.KVMConstant;
import org.zstack.storage.volume.VolumeSystemTags;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Collections;

import static org.zstack.core.Platform.operr;

/**
 * Shared helpers for the volume LUKS secret lifecycle on a KVM host:
 *
 * <ul>
 *   <li>{@link #materializeDek} — unseal/get-or-create the DEK for a volume's
 *       bound key provider. Idempotent; safe to call on every start_vm.</li>
 *   <li>{@link #defineLibvirtSecretOnHost} — define+set-value the libvirt secret
 *       on the destination host (RAM-only). Idempotent in key-agent
 *       (EnsureSecret keyed on {@code vmUuid, purpose, keyVersion, usageInstance}).</li>
 *   <li>{@link #getSecretOnHost} — ask the host whether a previously-defined
 *       libvirt secret is still resident; returns {@code null} on miss so
 *       callers can decide to re-define.</li>
 * </ul>
 *
 * <p>Used by the create path ({@link VolumeEncryptedInitialExtension}), the
 * start path ({@link VolumeEncryptedStartExtension}) and the hot-attach path
 * ({@link VolumeEncryptedAttachExtension}). All cloudbus calls are synchronous
 * via {@link CloudBus#call(org.zstack.header.message.NeedReplyMessage)};
 * timeouts are enforced by the underlying KVMHost handlers (HTTP layer has its
 * own {@code ENVELOPE_KEY_HTTP_TIMEOUT_SEC}). {@link EncryptedResourceKeyManager#getOrCreateKey}
 * is itself a synchronous DB / NKP / KMS call on the management server — we use
 * a one-shot capturing {@link ReturnValueCompletion} to read the result without
 * pulling in {@code FutureReturnValueCompletion}'s wait/notify and timeout layer.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VolumeEncryptedSecretHelper {
    private static final CLogger logger = Utils.getLogger(VolumeEncryptedSecretHelper.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private EncryptedResourceKeyManager encryptedResourceKeyManager;
    @Autowired
    private VolumeEncryptedResourceKeyBackend volumeEncryptedResourceKeyBackend;

    public EncryptedResourceKeyManager.ResourceKeyResult materializeDek(String volUuid, String kpUuid) {
        EncryptedResourceKeyManager.GetOrCreateResourceKeyContext ctx =
                new EncryptedResourceKeyManager.GetOrCreateResourceKeyContext();
        ctx.setResourceUuid(volUuid);
        ctx.setResourceType(VolumeVO.class.getSimpleName());
        ctx.setKeyProviderUuid(kpUuid);
        ctx.setPurpose("instantiate-volume");

        // getOrCreateKey is synchronous in EncryptedResourceKeyManagerImpl —
        // the completion fires on this thread before the call returns.
        final EncryptedResourceKeyManager.ResourceKeyResult[] resultRef =
                new EncryptedResourceKeyManager.ResourceKeyResult[1];
        final ErrorCode[] errorRef = new ErrorCode[1];
        encryptedResourceKeyManager.getOrCreateKey(ctx,
                new ReturnValueCompletion<EncryptedResourceKeyManager.ResourceKeyResult>(null) {
                    @Override
                    public void success(EncryptedResourceKeyManager.ResourceKeyResult r) {
                        resultRef[0] = r;
                    }

                    @Override
                    public void fail(ErrorCode err) {
                        errorRef[0] = err;
                    }
                });
        if (errorRef[0] != null) {
            throw new OperationFailureException(operr(
                    "failed to materialize encryption key for volume[uuid:%s]", volUuid)
                    .withCause(errorRef[0]));
        }
        return resultRef[0];
    }

    public String ensureLuksSecretFileOnHost(String hostUuid, String resourceUuid, String dekBase64) {
        SecretHostEnsureLuksSecretFileMsg ensureMsg = new SecretHostEnsureLuksSecretFileMsg();
        ensureMsg.setHostUuid(hostUuid);
        ensureMsg.setDekBase64(dekBase64);
        bus.makeTargetServiceIdByResourceUuid(ensureMsg, HostConstant.SERVICE_ID, hostUuid);

        MessageReply reply = bus.call(ensureMsg);
        if (!reply.isSuccess()) {
            throw new OperationFailureException(operr(
                    "failed to prepare secret material file for encrypted resource[uuid:%s] on host[uuid:%s]",
                    resourceUuid, hostUuid).withCause(reply.getError()));
        }
        SecretHostEnsureLuksSecretFileReply r = reply.castReply();
        if (StringUtils.isBlank(r.getSecFilePath())) {
            throw new OperationFailureException(operr(
                    "ensure LUKS secret file on host succeeded but secFilePath is empty, host[uuid:%s]",
                    hostUuid));
        }
        return r.getSecFilePath();
    }

    /**
     * Define a per-volume libvirt secret on {@code hostUuid}. Returns the
     * libvirt secret UUID. Throws on failure / blank reply.
     */
    public String defineLibvirtSecretOnHost(String hostUuid, String vmUuid, String volUuid,
                                            String dekBase64, Integer keyVersion) {
        if (StringUtils.isBlank(hostUuid) || StringUtils.isBlank(volUuid) ||
                StringUtils.isBlank(dekBase64) || keyVersion == null) {
            throw new OperationFailureException(operr(
                    "defineLibvirtSecretOnHost requires non-blank hostUuid, volUuid, dekBase64 and a non-null keyVersion"));
        }
        SecretHostDefineMsg defineMsg = new SecretHostDefineMsg();
        defineMsg.setHostUuid(hostUuid);
        defineMsg.setVmUuid(vmUuid);
        defineMsg.setDekBase64(dekBase64);
        defineMsg.setPurpose("volume");
        defineMsg.setKeyVersion(keyVersion);
        defineMsg.setUsageInstance(KVMConstant.volumeSecretUsageInstance(volUuid));
        defineMsg.setDescription(String.format("LUKS DEK for volume %s", volUuid));
        bus.makeTargetServiceIdByResourceUuid(defineMsg, HostConstant.SERVICE_ID, hostUuid);

        MessageReply reply = bus.call(defineMsg);
        if (!reply.isSuccess()) {
            throw new OperationFailureException(operr(
                    "failed to ensure libvirt secret for encrypted volume[uuid:%s] on host[uuid:%s]",
                    volUuid, hostUuid).withCause(reply.getError()));
        }
        SecretHostDefineReply r = reply.castReply();
        if (StringUtils.isBlank(r.getSecretUuid())) {
            throw new OperationFailureException(operr(
                    "ensure volume LUKS secret on host succeeded but secretUuid is empty, host[uuid:%s]",
                    hostUuid));
        }

        // Remember which host now owns this volume's libvirt secret so that
        // expunge can clean it up later, even if the owning VM is gone by then.
        // recreate=true overwrites any stale tag from a previous host.
        try {
            SystemTagCreator tc = VolumeSystemTags.VOLUME_LIBVIRT_SECRET_HOST.newSystemTagCreator(volUuid);
            tc.setTagByTokens(Collections.singletonMap(
                    VolumeSystemTags.VOLUME_LIBVIRT_SECRET_HOST_TOKEN, hostUuid));
            tc.inherent = false;
            tc.recreate = true;
            tc.create();
        } catch (RuntimeException tagEx) {
            // Tag write failure must not break the actual secret define -- the
            // define already succeeded, the tag is for cleanup bookkeeping only.
            logger.warn(String.format(
                    "failed to stamp VOLUME_LIBVIRT_SECRET_HOST tag on volume[uuid:%s] for host[uuid:%s]: %s",
                    volUuid, hostUuid, tagEx.getMessage()));
        }

        return r.getSecretUuid();
    }

    public String lookupVmInstanceUuid(String volumeUuid) {
        return Q.New(VolumeVO.class)
                .eq(VolumeVO_.uuid, volumeUuid)
                .select(VolumeVO_.vmInstanceUuid)
                .findValue();
    }

    /**
     * Materialize the DEK for {@code volUuid} under the binding in
     * {@code kpUuid}, then define+set-value the libvirt secret on
     * {@code hostUuid}. Used by both the create-time path and the start-time
     * fallback when the host's libvirt secret value was lost.
     */
    public String defineSecretFromBinding(String hostUuid, String vmUuid, String volUuid, String kpUuid) {
        if (StringUtils.isBlank(kpUuid)) {
            throw new OperationFailureException(operr(
                    "encrypted volume[uuid:%s] has no key provider binding; cannot define libvirt secret on host[uuid:%s]",
                    volUuid, hostUuid));
        }
        EncryptedResourceKeyManager.ResourceKeyResult keyResult = materializeDek(volUuid, kpUuid);
        String dekBase64 = keyResult.getDekBase64();
        if (StringUtils.isBlank(dekBase64)) {
            throw new OperationFailureException(operr(
                    "encrypted volume[uuid:%s]: key manager returned empty DEK for libvirt secret",
                    volUuid));
        }
        return defineLibvirtSecretOnHost(hostUuid, vmUuid, volUuid, dekBase64, keyResult.getKeyVersion());
    }

    /**
     * Ask {@code hostUuid} for the libvirt secret UUID identified by the
     * (vm, volume, keyVersion) tuple. Returns null on SECRET_NOT_FOUND so
     * callers can fall back to {@link #defineSecretFromBinding}; throws on
     * any other failure.
     */
    public String getSecretOnHost(String hostUuid, String vmUuid, String volUuid, Integer keyVersion) {
        SecretHostGetMsg msg = new SecretHostGetMsg();
        msg.setHostUuid(hostUuid);
        msg.setVmUuid(vmUuid);
        msg.setPurpose("volume");
        msg.setKeyVersion(keyVersion);
        msg.setUsageInstance(KVMConstant.volumeSecretUsageInstance(volUuid));
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);

        MessageReply reply = bus.call(msg);
        if (reply.isSuccess()) {
            SecretHostGetReply r = reply.castReply();
            return r.getSecretUuid();
        }
        ErrorCode err = reply.getError();
        if (SecretHostGetReply.isSecretNotFound(err)) {
            return null;
        }
        throw new OperationFailureException(operr(
                "failed to get libvirt LUKS secret on host[uuid:%s] vm[uuid:%s] volume[uuid:%s] keyVersion[%s]: %s",
                hostUuid, vmUuid, volUuid, keyVersion, err));
    }

    /**
     * One-shot resolver: "give me the libvirt secret UUID for this encrypted
     * volume on this host". Used by both the start-vm path
     * ({@link VolumeEncryptedStartExtension}) and the attach-data-volume
     * path ({@link VolumeEncryptedAttachExtension}).
     *
     * <p>Order:
     * <ol>
     *   <li>{@code findKeyVersionByVolume} — fail loudly if the volume has
     *       no {@code EncryptedResourceKeyRefVO} binding (would otherwise
     *       produce an unreadable qcow2 once attached).</li>
     *   <li>{@link #getSecretOnHost} — fast path, hits when the secret has
     *       already been defined on the host.</li>
     *   <li>{@link #defineSecretFromBinding} — fall back when the secret
     *       value is missing on the host (first attach, libvirtd restart,
     *       host reboot, migrate to a fresh host).</li>
     * </ol>
     */
    /**
     * Best-effort delete of a single per-volume libvirt secret on {@code hostUuid}.
     * The underlying KVMHost handler treats SECRET_NOT_FOUND as success, so it's
     * safe to call regardless of whether the secret was ever defined.
     *
     * <p>Returns silently on failure (logs at warn): cleanup must never break
     * caller flows like VM destroy.
     */
    public void deleteSecretOnHostBestEffort(String hostUuid, String vmUuid, String volUuid, Integer keyVersion) {
        if (StringUtils.isBlank(hostUuid) || StringUtils.isBlank(vmUuid)
                || StringUtils.isBlank(volUuid) || keyVersion == null) {
            return;
        }
        SecretHostDeleteMsg msg = new SecretHostDeleteMsg();
        msg.setHostUuid(hostUuid);
        msg.setVmUuid(vmUuid);
        msg.setPurpose("volume");
        msg.setKeyVersion(keyVersion);
        msg.setUsageInstance(KVMConstant.volumeSecretUsageInstance(volUuid));
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);

        MessageReply reply = bus.call(msg);
        if (!reply.isSuccess()) {
            logger.warn(String.format(
                    "best-effort delete libvirt LUKS secret failed for volume[uuid:%s] on host[uuid:%s] vm[uuid:%s]: %s",
                    volUuid, hostUuid, vmUuid, reply.getError()));
        }
    }

    public String resolveOrDefineSecretForVolume(String hostUuid, String vmUuid, String volUuid) {
        Integer keyVersion = volumeEncryptedResourceKeyBackend.findKeyVersionByVolume(volUuid);
        if (keyVersion == null) {
            throw new OperationFailureException(operr(
                    "encrypted volume[uuid:%s] has no key version bound (EncryptedResourceKeyRefVO missing);" +
                            " cannot resolve libvirt LUKS secret on host[uuid:%s] for vm[uuid:%s]",
                    volUuid, hostUuid, vmUuid));
        }
        String secretUuid = getSecretOnHost(hostUuid, vmUuid, volUuid, keyVersion);
        if (StringUtils.isNotBlank(secretUuid)) {
            return secretUuid;
        }
        String kpUuid = volumeEncryptedResourceKeyBackend.findKeyProviderUuidByVolume(volUuid);
        return defineSecretFromBinding(hostUuid, vmUuid, volUuid, kpUuid);
    }
}
