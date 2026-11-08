package org.zstack.header.vm;

import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.NopeFlow;

public interface RestoreVmExtensionPoint {
    String UNSAFE_TO_START_VM = RestoreVmExtensionPoint.class.getName() + ".unsafeToStartVm";
    String ROLLBACK_ERROR = RestoreVmExtensionPoint.class.getName() + ".rollbackError";

    default void failedToRecoverVmAfterRestore(RestoreVmMsg msg) {
    }

    default Flow getPreRestoreVmFlow(RestoreVmMsg msg, VmInstanceSpec spec) {
        return new NopeFlow();
    }

    default Flow getBeforeRestoreVmFlow(RestoreVmMsg msg, VmInstanceSpec spec) {
        return new NopeFlow();
    }

    default Flow getRestoreVmFlow(RestoreVmMsg msg, VmInstanceSpec spec, RestoreVmReply reply) {
        return new NopeFlow();
    }

    default Flow getAfterRestoreVmFlow(RestoreVmMsg msg, VmInstanceSpec spec) {
        return new NopeFlow();
    }
}
