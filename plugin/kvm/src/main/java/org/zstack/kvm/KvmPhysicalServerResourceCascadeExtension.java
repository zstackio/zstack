package org.zstack.kvm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.AbstractAsyncCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.physicalserver.PhysicalServerManager;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Collections;
import java.util.List;

public class KvmPhysicalServerResourceCascadeExtension extends AbstractAsyncCascadeExtension {
    private static final CLogger logger = Utils.getLogger(KvmPhysicalServerResourceCascadeExtension.class);
    private static final String NAME = "PhysicalServerComputeResourceAssignment";

    @Autowired(required = false)
    private PhysicalServerManager physicalServerManager;

    @Override
    public void asyncCascade(CascadeAction action, Completion completion) {
        if (physicalServerManager == null
                || !HostVO.class.getSimpleName().equals(action.getParentIssuer())
                || !action.isActionCode(
                CascadeConstant.DELETION_DELETE_CODE, CascadeConstant.DELETION_FORCE_DELETE_CODE)) {
            completion.success();
            return;
        }

        List<HostInventory> hosts = action.getParentIssuerContext();
        if (hosts == null || hosts.isEmpty()) {
            completion.success();
            return;
        }
        new While<>(hosts).each((host, each) -> {
            if (host.getServerUuid() == null) {
                each.done();
                return;
            }
            physicalServerManager.releaseResourceAssignment(
                    host.getServerUuid(), KvmPhysicalServerAdapter.type.toString(), new Completion(each) {
                        @Override
                        public void success() {
                            forget(host, each);
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            logger.warn(String.format(
                                    "failed to release compute resource assignment before deleting host[uuid:%s], " +
                                            "forgetting the Assignment: %s", host.getUuid(), errorCode));
                            forget(host, each);
                        }
                    });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList ignored) {
                completion.success();
            }
        });
    }

    private void forget(HostInventory host, NoErrorCompletion completion) {
        physicalServerManager.forgetResourceAssignment(
                host.getServerUuid(), KvmPhysicalServerAdapter.type.toString(), new Completion(completion) {
                    @Override
                    public void success() {
                        completion.done();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        logger.warn(String.format(
                                "failed to forget compute resource assignment while deleting host[uuid:%s]: %s",
                                host.getUuid(), errorCode));
                        completion.done();
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
