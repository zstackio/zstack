package org.zstack.kvm.memory;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cascade.AbstractAsyncCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.core.Completion;
import org.zstack.header.host.HostVO;
import org.zstack.header.vm.VmInstanceVO;
import java.util.Arrays;
import java.util.List;

/** Cleanup is post-deletion, never the pre-delete drain/Unknown safety gate. */
public class MemoryResourceCascadeExtension extends AbstractAsyncCascadeExtension {
    @Autowired private MemoryRepository repository;

    @Override public void asyncCascade(CascadeAction action, Completion completion) {
        if (action.isActionCode(CascadeConstant.DELETION_CLEANUP_CODE)) {
            repository.cleanupDeletedResources();
        }
        completion.success();
    }

    @Override public List<String> getEdgeNames() {
        return Arrays.asList(HostVO.class.getSimpleName(), ClusterVO.class.getSimpleName(), VmInstanceVO.class.getSimpleName());
    }
    @Override public String getCascadeResourceName() { return MemoryPolicyVO.class.getSimpleName(); }
    @Override public CascadeAction createActionForChildResource(CascadeAction action) { return null; }
}
