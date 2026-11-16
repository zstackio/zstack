package org.zstack.storage.zbs;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cascade.AbstractAsyncCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.db.Q;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.storage.addon.primary.ExternalPrimaryStorageVO;
import org.zstack.header.storage.addon.primary.ExternalPrimaryStorageVO_;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

public class ZbsResourceAssignmentCascadeExtension extends AbstractAsyncCascadeExtension {
    private static final CLogger logger = Utils.getLogger(ZbsResourceAssignmentCascadeExtension.class);

    @Autowired
    private ZbsResourceAssignmentFactory assignments;

    @Override
    public void asyncCascade(CascadeAction action, Completion completion) {
        if (!PrimaryStorageVO.class.getSimpleName().equals(action.getParentIssuer())
                || !action.isActionCode(
                        CascadeConstant.DELETION_DELETE_CODE, CascadeConstant.DELETION_FORCE_DELETE_CODE)) {
            completion.success();
            return;
        }
        List<PrimaryStorageInventory> inventories = action.getParentIssuerContext();
        if (inventories == null || inventories.isEmpty()) {
            completion.success();
            return;
        }
        List<ExternalPrimaryStorageVO> primaryStorages = Q.New(ExternalPrimaryStorageVO.class)
                .in(ExternalPrimaryStorageVO_.uuid, inventories.stream().map(PrimaryStorageInventory::getUuid)
                        .collect(Collectors.toList()))
                .eq(ExternalPrimaryStorageVO_.identity, ZbsConstants.IDENTITY).list();
        forgetAssignments(primaryStorages, new NoErrorCompletion(completion) {
            @Override
            public void done() {
                completion.success();
            }
        });
    }

    private void forgetAssignments(List<ExternalPrimaryStorageVO> primaryStorages, NoErrorCompletion completion) {
        Set<String> serialNumbers = new HashSet<>();
        for (ExternalPrimaryStorageVO primaryStorage : primaryStorages) {
            serialNumbers.addAll(ZbsNodeRefContributorImpl.serialNumbers(
                    ZbsNodeRefContributorImpl.parseAddonInfo(primaryStorage)));
        }
        assignments.forgetAssignments(serialNumbers, new Completion(completion) {
            @Override
            public void success() {
                completion.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.warn(String.format("failed to forget ZBS assignments during storage deletion: %s", errorCode));
                completion.done();
            }
        });
    }

    @Override
    public List<String> getEdgeNames() {
        return Collections.singletonList(PrimaryStorageVO.class.getSimpleName());
    }

    @Override
    public String getCascadeResourceName() {
        return "PhysicalServerZbsResourceAssignment";
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        return null;
    }
}
