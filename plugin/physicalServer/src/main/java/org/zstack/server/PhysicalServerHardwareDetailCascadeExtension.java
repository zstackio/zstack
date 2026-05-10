package org.zstack.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cascade.AbstractAsyncCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.core.Completion;
import org.zstack.header.server.PhysicalServerHardwareDetailVO;
import org.zstack.header.server.PhysicalServerHardwareDetailVO_;
import org.zstack.header.server.PhysicalServerInventory;
import org.zstack.header.server.PhysicalServerVO;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PhysicalServerHardwareDetailCascadeExtension extends AbstractAsyncCascadeExtension {
    @Autowired
    private DatabaseFacade dbf;

    private static final String NAME = PhysicalServerHardwareDetailVO.class.getSimpleName();

    @Override
    public void asyncCascade(CascadeAction action, Completion completion) {
        if (action.isActionCode(CascadeConstant.DELETION_DELETE_CODE,
                CascadeConstant.DELETION_FORCE_DELETE_CODE)) {
            handleDeletion(action, completion);
        } else {
            completion.success();
        }
    }

    private void handleDeletion(CascadeAction action, Completion completion) {
        List<PhysicalServerHardwareDetailVO> details = detailsFromAction(action);
        if (details == null || details.isEmpty()) {
            completion.success();
            return;
        }

        List<Long> ids = details.stream()
                .map(PhysicalServerHardwareDetailVO::getId)
                .collect(Collectors.toList());
        dbf.removeByPrimaryKeys(ids, PhysicalServerHardwareDetailVO.class);
        completion.success();
    }

    private List<PhysicalServerHardwareDetailVO> detailsFromAction(CascadeAction action) {
        if (PhysicalServerVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<PhysicalServerInventory> servers = action.getParentIssuerContext();
            if (servers == null || servers.isEmpty()) {
                return null;
            }
            List<String> serverUuids = servers.stream()
                    .map(PhysicalServerInventory::getUuid)
                    .collect(Collectors.toList());
            List<PhysicalServerHardwareDetailVO> vos = Q.New(PhysicalServerHardwareDetailVO.class)
                    .in(PhysicalServerHardwareDetailVO_.serverUuid, serverUuids)
                    .list();
            return vos.isEmpty() ? null : vos;
        }
        if (NAME.equals(action.getParentIssuer())) {
            return action.getParentIssuerContext();
        }
        return null;
    }

    @Override
    public List<String> getEdgeNames() {
        return Arrays.asList(PhysicalServerVO.class.getSimpleName());
    }

    @Override
    public String getCascadeResourceName() {
        return NAME;
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        if (CascadeConstant.DELETION_CODES.contains(action.getActionCode())) {
            List<PhysicalServerHardwareDetailVO> vos = detailsFromAction(action);
            if (vos != null) {
                return action.copy().setParentIssuer(NAME).setParentIssuerContext(vos);
            }
        }
        return null;
    }
}
