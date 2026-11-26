package org.zstack.compute.vm.devices;

import org.zstack.compute.vm.VmSystemTags;
import org.zstack.compute.vm.VmInstanceBase;
import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.image.ImageBootMode;
import org.zstack.header.message.APIMessage;
import org.zstack.header.tag.APICreateSystemTagMsg;
import org.zstack.header.tag.APICreateSystemTagsMsg;
import org.zstack.header.tag.APIDeleteTagMsg;
import org.zstack.header.tag.APIUpdateSystemTagMsg;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.header.vm.APICreateVmInstanceMsg;
import org.zstack.header.vm.APIDeleteVmBootModeMsg;
import org.zstack.header.vm.APISetVmBootModeMsg;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.tag.SystemTagApiMessageExtensionPoint;

import java.util.List;
import java.util.Objects;

import static org.zstack.core.Platform.err;
import static org.zstack.header.tpm.TpmErrors.VM_BOOT_MODE_ERROR;
import static org.zstack.utils.CollectionDSL.list;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_COMPUTE_VM_DEVICES_10011;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_COMPUTE_VM_DEVICES_10012;

public class TpmBootModeApiInterceptor implements GlobalApiMessageInterceptor, SystemTagApiMessageExtensionPoint {
    @Override
    public String getResourceUuid(APIMessage msg) {
        if (msg instanceof APICreateSystemTagMsg) {
            APICreateSystemTagMsg create = (APICreateSystemTagMsg) msg;
            return VmInstanceVO.class.getSimpleName().equals(create.getResourceType()) ? create.getResourceUuid() : null;
        }
        if (msg instanceof APICreateSystemTagsMsg) {
            APICreateSystemTagsMsg create = (APICreateSystemTagsMsg) msg;
            return VmInstanceVO.class.getSimpleName().equals(create.getResourceType()) ? create.getResourceUuid() : null;
        }
        String tagUuid = msg instanceof APIUpdateSystemTagMsg ? ((APIUpdateSystemTagMsg) msg).getUuid()
                : msg instanceof APIDeleteTagMsg ? ((APIDeleteTagMsg) msg).getUuid() : null;
        SystemTagVO tag = tagUuid == null ? null : findSystemTag(tagUuid);
        return tag != null && VmInstanceVO.class.getSimpleName().equals(tag.getResourceType())
                ? tag.getResourceUuid() : null;
    }

    @Override
    public String getSyncSignature(String resourceUuid) {
        return VmInstanceBase.makeSyncSignature(resourceUuid);
    }

    @Override
    public void beforeHandle(APIMessage msg) {
        intercept(msg);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List<Class> getMessageClassToIntercept() {
        return list(
                APISetVmBootModeMsg.class,
                APIDeleteVmBootModeMsg.class,
                APICreateSystemTagMsg.class,
                APICreateSystemTagsMsg.class,
                APIUpdateSystemTagMsg.class,
                APIDeleteTagMsg.class,
                APICreateVmInstanceMsg.class
        );
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.END;
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APISetVmBootModeMsg) {
            validate((APISetVmBootModeMsg) msg);
        } else if (msg instanceof APIDeleteVmBootModeMsg) {
            validate((APIDeleteVmBootModeMsg) msg);
        } else if (msg instanceof APICreateSystemTagMsg) {
            validate((APICreateSystemTagMsg) msg);
        } else if (msg instanceof APICreateSystemTagsMsg) {
            validate((APICreateSystemTagsMsg) msg);
        } else if (msg instanceof APIUpdateSystemTagMsg) {
            validate((APIUpdateSystemTagMsg) msg);
        } else if (msg instanceof APIDeleteTagMsg) {
            validate((APIDeleteTagMsg) msg);
        } else if (msg instanceof APICreateVmInstanceMsg) {
            validate((APICreateVmInstanceMsg) msg);
        }

        return msg;
    }

    private void validate(APISetVmBootModeMsg msg) {
        checkBootModeChangeAllowed(msg.getVmInstanceUuid(), msg.getBootMode());
    }

    private void validate(APIDeleteVmBootModeMsg msg) {
        checkBootModeChangeAllowed(msg.getVmInstanceUuid(), null);
    }

    private void validate(APICreateSystemTagMsg msg) {
        checkTpmAllowsBootModeTag(msg.getResourceType(), msg.getResourceUuid(), msg.getTag());
    }

    private void validate(APICreateSystemTagsMsg msg) {
        if (msg.getTags() == null) {
            return;
        }

        for (String tag : msg.getTags()) {
            checkTpmAllowsBootModeTag(msg.getResourceType(), msg.getResourceUuid(), tag);
        }
    }

    private void validate(APIUpdateSystemTagMsg msg) {
        SystemTagVO tag = findSystemTag(msg.getUuid());
        if (tag == null) {
            return;
        }

        if (isVmBootModeTag(tag.getResourceType(), tag.getTag())
                || isVmBootModeTag(tag.getResourceType(), msg.getTag())) {
            checkBootModeChangeAllowed(tag.getResourceUuid(), bootModeOf(msg.getTag()));
        }
    }

    private void validate(APIDeleteTagMsg msg) {
        SystemTagVO tag = findSystemTag(msg.getUuid());
        if (tag != null && isVmBootModeTag(tag.getResourceType(), tag.getTag())) {
            checkBootModeChangeAllowed(tag.getResourceUuid(), null);
        }
    }

    private void validate(APICreateVmInstanceMsg msg) {
        if (msg.getDevicesSpec() == null || msg.getDevicesSpec().getTpm() == null
                || !msg.getDevicesSpec().getTpm().isEnable()) {
            return;
        }

        String bootMode = null;
        if (msg.getSystemTags() != null) {
            for (String tag : msg.getSystemTags()) {
                if (VmSystemTags.BOOT_MODE.isMatch(tag)) {
                    bootMode = bootModeOf(tag);
                }
            }
        }

        if (!VmTpmManager.isUefiBootMode(bootMode)) {
            throw new ApiMessageInterceptionException(err(ORG_ZSTACK_COMPUTE_VM_DEVICES_10012, VM_BOOT_MODE_ERROR,
                    "cannot create VM[name:%s] with a TPM device in %s boot mode, TPM requires UEFI boot mode",
                    msg.getName(), bootMode == null ? ImageBootMode.Legacy : bootMode));
        }
    }

    private static SystemTagVO findSystemTag(String uuid) {
        return Q.New(SystemTagVO.class)
                .eq(SystemTagVO_.uuid, uuid)
                .find();
    }

    private static boolean isVmBootModeTag(String resourceType, String tag) {
        return VmInstanceVO.class.getSimpleName().equals(resourceType)
                && tag != null
                && VmSystemTags.BOOT_MODE.isMatch(tag);
    }

    private static String bootModeOf(String tag) {
        if (!VmSystemTags.BOOT_MODE.isMatch(tag)) {
            return null;
        }
        return VmSystemTags.BOOT_MODE.getTokenByTag(tag, VmSystemTags.BOOT_MODE_TOKEN);
    }

    private void checkTpmAllowsBootModeTag(String resourceType, String resourceUuid, String tag) {
        if (isVmBootModeTag(resourceType, tag)) {
            checkBootModeChangeAllowed(resourceUuid, bootModeOf(tag));
        }
    }

    public static void checkBootModeChangeAllowed(String vmUuid, String bootMode) {
        String current = VmSystemTags.BOOT_MODE.getTokenByResourceUuid(vmUuid, VmSystemTags.BOOT_MODE_TOKEN);
        String effectiveCurrent = current == null ? ImageBootMode.Legacy.toString() : current;
        String effectiveTarget = bootMode == null ? ImageBootMode.Legacy.toString() : bootMode;
        if (!Objects.equals(effectiveCurrent, effectiveTarget)) {
            VmInstanceState state = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vmUuid)
                    .select(VmInstanceVO_.state).findValue();
            if (state != null && state != VmInstanceState.Created && state != VmInstanceState.Stopped) {
                throw new ApiMessageInterceptionException(err(ORG_ZSTACK_COMPUTE_VM_DEVICES_10011, VM_BOOT_MODE_ERROR,
                        "cannot change boot mode of VM[uuid:%s] in state %s; stop the VM first", vmUuid, state));
            }
        }
        checkTpmAllowsBootMode(vmUuid, bootMode);
    }

    public static void checkTpmAllowsBootMode(String vmUuid, String bootMode) {
        if (VmTpmManager.isUefiBootMode(bootMode) || VmTpmManager.findTpmUuidForVmOrNull(vmUuid) == null) {
            return;
        }

        throw new ApiMessageInterceptionException(err(ORG_ZSTACK_COMPUTE_VM_DEVICES_10011, VM_BOOT_MODE_ERROR,
                "cannot change boot mode of VM[uuid:%s] to %s, because the VM has a TPM device which requires" +
                        " UEFI boot mode, remove the TPM first",
                vmUuid, bootMode == null ? ImageBootMode.Legacy : bootMode));
    }
}
