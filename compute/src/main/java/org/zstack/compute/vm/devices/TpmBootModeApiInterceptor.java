package org.zstack.compute.vm.devices;

import org.zstack.compute.vm.VmSystemTags;
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
import org.zstack.header.vm.APIDeleteVmBootModeMsg;
import org.zstack.header.vm.APISetVmBootModeMsg;
import org.zstack.header.vm.VmInstanceVO;

import java.util.List;

import static org.zstack.core.Platform.err;
import static org.zstack.header.tpm.TpmErrors.VM_BOOT_MODE_ERROR;
import static org.zstack.utils.CollectionDSL.list;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_COMPUTE_VM_DEVICES_10011;

public class TpmBootModeApiInterceptor implements GlobalApiMessageInterceptor {
    @Override
    @SuppressWarnings("rawtypes")
    public List<Class> getMessageClassToIntercept() {
        return list(
                APISetVmBootModeMsg.class,
                APIDeleteVmBootModeMsg.class,
                APICreateSystemTagMsg.class,
                APICreateSystemTagsMsg.class,
                APIUpdateSystemTagMsg.class,
                APIDeleteTagMsg.class
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
        }

        return msg;
    }

    private void validate(APISetVmBootModeMsg msg) {
        checkTpmAllowsBootMode(msg.getVmInstanceUuid(), msg.getBootMode());
    }

    private void validate(APIDeleteVmBootModeMsg msg) {
        checkTpmAllowsBootMode(msg.getVmInstanceUuid(), null);
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
            checkTpmAllowsBootMode(tag.getResourceUuid(), bootModeOf(msg.getTag()));
        }
    }

    private void validate(APIDeleteTagMsg msg) {
        SystemTagVO tag = findSystemTag(msg.getUuid());
        if (tag != null && isVmBootModeTag(tag.getResourceType(), tag.getTag())) {
            checkTpmAllowsBootMode(tag.getResourceUuid(), null);
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
            checkTpmAllowsBootMode(resourceUuid, bootModeOf(tag));
        }
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
