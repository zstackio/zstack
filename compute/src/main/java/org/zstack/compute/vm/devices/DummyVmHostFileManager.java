package org.zstack.compute.vm.devices;

import org.zstack.core.db.SQL;
import org.zstack.core.db.Q;
import org.zstack.header.core.Completion;
import org.zstack.header.tpm.entity.TpmVO;
import org.zstack.header.tpm.entity.TpmVO_;
import org.zstack.header.vm.additions.VmHostBackupFileVO;
import org.zstack.header.vm.additions.VmHostBackupFileVO_;
import org.zstack.header.vm.additions.VmHostFileManager;
import org.zstack.header.vm.additions.VmHostFileVO;
import org.zstack.header.vm.additions.VmHostFileVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_KVM_10163;

public class DummyVmHostFileManager implements VmHostFileManager {
    private static final CLogger logger = Utils.getLogger(DummyVmHostFileManager.class);

    @Override
    public void backupVmHostFiles(String vmUuid, String resourceUuid, Completion completion) {
        if (Q.New(VmHostFileVO.class).eq(VmHostFileVO_.vmInstanceUuid, vmUuid).isExists()
                || Q.New(TpmVO.class).eq(TpmVO_.vmInstanceUuid, vmUuid).isExists()) {
            completion.fail(operr(ORG_ZSTACK_KVM_10163,
                    "VM[uuid:%s] requires a host file backup manager for resource[uuid:%s]",
                    vmUuid, resourceUuid));
            return;
        }
        completion.success();
    }

    @Override
    public void cleanVmHostBackupFile(String resourceUuid) {
        logger.debug(String.format("clean VmHostBackupFileVO[resourceUuid=%s]", resourceUuid));
        SQL.New(VmHostBackupFileVO.class)
                .eq(VmHostBackupFileVO_.resourceUuid, resourceUuid)
                .delete();
    }
}
