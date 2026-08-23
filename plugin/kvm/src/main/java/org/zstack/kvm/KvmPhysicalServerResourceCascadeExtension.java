package org.zstack.kvm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.AbstractAsyncCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.physicalserver.PhysicalServerManager;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class KvmPhysicalServerResourceCascadeExtension extends AbstractAsyncCascadeExtension {
    private static final CLogger logger = Utils.getLogger(
            KvmPhysicalServerResourceCascadeExtension.class);
    private static final String NAME = "PhysicalServerComputeResourceAssignment";

    @Autowired(required = false)
    private PhysicalServerManager physicalServerManager;

    @Override
    public void asyncCascade(CascadeAction action, Completion completion) {
        if (physicalServerManager == null
                || !HostVO.class.getSimpleName().equals(action.getParentIssuer())
                || !action.isActionCode(
                CascadeConstant.DELETION_DELETE_CODE,
                CascadeConstant.DELETION_FORCE_DELETE_CODE)) {
            completion.success();
            return;
        }

        List<HostInventory> hosts = action.getParentIssuerContext();
        if (hosts == null || hosts.isEmpty()) {
            completion.success();
            return;
        }
        boolean forceDelete = action.isActionCode(CascadeConstant.DELETION_FORCE_DELETE_CODE);
        List<String> forceDeleteFailures = Collections.synchronizedList(new ArrayList<>());

        new While<>(hosts).each((host, each) -> {
            if (host.getServerUuid() == null) {
                each.done();
                return;
            }
            physicalServerManager.releaseResourceAssignment(
                    host.getServerUuid(),
                    KvmPhysicalServerAdapter.ROLE_TYPE,
                    host.getUuid(),
                    forceDelete,
                    new Completion(each) {
                        @Override
                        public void success() {
                            each.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            if (!forceDelete) {
                                each.addError(errorCode);
                                each.done();
                                return;
                            }
                            forceDeleteFailures.add(String.format(
                                    "host[uuid:%s]: %s", host.getUuid(), errorCode));
                            logger.error(String.format(
                                    "failed to release compute resource assignment before force deleting " +
                                            "host[uuid:%s], continuing without recovery reconcile: %s",
                                    host.getUuid(), errorCode));
                            each.done();
                        }
                    });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!forceDelete && !errorCodeList.getCauses().isEmpty()) {
                    completion.fail(errorCodeList.getCauses().get(0));
                    return;
                }
                if (!forceDeleteFailures.isEmpty()) {
                    logger.error(String.format(
                            "failed to release compute resource assignment for %s host(s) during " +
                                    "force deletion; recovery reconcile was skipped: %s",
                            forceDeleteFailures.size(), String.join("; ", forceDeleteFailures)));
                }
                completion.success();
            }
        });
    }

    @Override
    public List<String> getEdgeNames() {
        return Collections.singletonList(HostVO.class.getSimpleName());
    }

    @Override
    public String getCascadeResourceName() {
        return NAME;
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        return null;
    }
}
