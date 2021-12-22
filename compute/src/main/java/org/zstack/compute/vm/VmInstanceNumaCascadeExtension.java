package org.zstack.compute.vm;

import org.zstack.core.cascade.AbstractAsyncCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.db.SQL;
import org.zstack.header.core.Completion;
import org.zstack.header.vm.VmDeletionStruct;
import org.zstack.header.vm.VmInstanceNumaNodeVO;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VmInstanceNumaCascadeExtension extends AbstractAsyncCascadeExtension {
    private static final CLogger logger = Utils.getLogger(VmInstanceNumaCascadeExtension.class);

    @Override
    public void asyncCascade(CascadeAction action, Completion completion) {
        if (action.isActionCode(CascadeConstant.DELETION_CLEANUP_CODE)) {
            handleCleanDeletion(action, completion);
        } else {
            completion.success();
        }
    }

    private void handleCleanDeletion(CascadeAction action, Completion completion) {
        try {
            final List<String> vmUuids = vmFromAction(action);
            if (vmUuids != null && !vmUuids.isEmpty()) {
                vmUuids.forEach(h -> SQL.New("delete from VmInstanceNumaNodeVO where vmUuid = :uuid")
                        .param("uuid", h)
                        .execute());
            }
        } catch (NullPointerException e) {
            logger.warn(e.getMessage());
        } finally {
            completion.success();
        }
    }

    private List<String> vmFromAction(CascadeAction action) {
        List<String> vmUuid = new ArrayList<>();
        if (VmInstanceVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<VmDeletionStruct> vms = action.getParentIssuerContext();
            vmUuid = CollectionUtils.transformToList(vms, new Function<String, VmDeletionStruct>() {
                @Override
                public String call(VmDeletionStruct arg) {
                    return arg.getInventory().getUuid();
                }
            });
        }
        return vmUuid;
    }

    @Override
    public List<String> getEdgeNames() {
        return Arrays.asList(VmInstanceVO.class.getSimpleName());
    }

    @Override
    public String getCascadeResourceName() {
        return VmInstanceNumaNodeVO.class.getSimpleName();
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        return null;
    }
}
