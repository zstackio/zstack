package org.zstack.compute.vm.devices;

import org.zstack.core.db.Q;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.tpm.entity.TpmVO;
import org.zstack.header.tpm.entity.TpmVO_;

import java.util.Objects;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_COMPUTE_VM_DEVICES_10013;

public final class TpmLiveMigrationPolicy {
    private TpmLiveMigrationPolicy() {
    }

    public static ErrorCode checkHostChange(String vmUuid, String sourceHostUuid, String destinationHostUuid) {
        if (Objects.equals(sourceHostUuid, destinationHostUuid)
                || !Q.New(TpmVO.class).eq(TpmVO_.vmInstanceUuid, vmUuid).isExists()) {
            return null;
        }

        return operr(ORG_ZSTACK_COMPUTE_VM_DEVICES_10013,
                "VM[uuid:%s] with vTPM cannot live migrate from host[uuid:%s] to host[uuid:%s]",
                vmUuid, sourceHostUuid, destinationHostUuid);
    }
}
