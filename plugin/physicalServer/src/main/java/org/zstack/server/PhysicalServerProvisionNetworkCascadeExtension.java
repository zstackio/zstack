package org.zstack.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cascade.AbstractAsyncCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.core.Completion;
import org.zstack.header.server.PhysicalServerProvisionNetworkInventory;
import org.zstack.header.server.PhysicalServerProvisionNetworkVO;
import org.zstack.header.server.PhysicalServerProvisionNetworkVO_;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.header.zone.ZoneVO;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Cascade Zone deletion to PhysicalServerProvisionNetworkVO. PSPNVO.zoneUuid
 * is @ForeignKey RESTRICT against ZoneEO; the legacy
 * BareMetal2ProvisionNetworkCascadeExtension queries the historical
 * BareMetal2ProvisionNetworkVO entity (now a compat shim after the V5.5.18
 * RENAME) and does not cleanly cascade for tests that create rows via the
 * unified PhysicalServerProvisionNetworkVO entity directly.
 */
public class PhysicalServerProvisionNetworkCascadeExtension extends AbstractAsyncCascadeExtension {
    @Autowired
    private DatabaseFacade dbf;

    private static final String NAME = PhysicalServerProvisionNetworkVO.class.getSimpleName();

    @Override
    public void asyncCascade(CascadeAction action, Completion completion) {
        if (action.isActionCode(CascadeConstant.DELETION_DELETE_CODE,
                CascadeConstant.DELETION_FORCE_DELETE_CODE)) {
            handleDeletion(action, completion);
        } else if (action.isActionCode(CascadeConstant.DELETION_CHECK_CODE)) {
            completion.success();
        } else if (action.isActionCode(CascadeConstant.DELETION_CLEANUP_CODE)) {
            dbf.eoCleanup(PhysicalServerProvisionNetworkVO.class);
            completion.success();
        } else {
            completion.success();
        }
    }

    private void handleDeletion(CascadeAction action, Completion completion) {
        List<PhysicalServerProvisionNetworkInventory> nets = networksFromAction(action);
        if (nets == null || nets.isEmpty()) {
            completion.success();
            return;
        }
        List<String> uuids = nets.stream()
                .map(PhysicalServerProvisionNetworkInventory::getUuid)
                .collect(Collectors.toList());
        dbf.removeByPrimaryKeys(uuids, PhysicalServerProvisionNetworkVO.class);
        completion.success();
    }

    private List<PhysicalServerProvisionNetworkInventory> networksFromAction(CascadeAction action) {
        if (ZoneVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<ZoneInventory> zones = action.getParentIssuerContext();
            List<String> zoneUuids = zones.stream()
                    .map(ZoneInventory::getUuid)
                    .collect(Collectors.toList());
            List<PhysicalServerProvisionNetworkVO> vos = Q.New(PhysicalServerProvisionNetworkVO.class)
                    .in(PhysicalServerProvisionNetworkVO_.zoneUuid, zoneUuids)
                    .list();
            if (vos.isEmpty()) {
                return null;
            }
            return PhysicalServerProvisionNetworkInventory.valueOf(vos);
        }
        if (NAME.equals(action.getParentIssuer())) {
            return action.getParentIssuerContext();
        }
        return null;
    }

    @Override
    public List<String> getEdgeNames() {
        return Arrays.asList(ZoneVO.class.getSimpleName());
    }

    @Override
    public String getCascadeResourceName() {
        return NAME;
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        if (CascadeConstant.DELETION_CODES.contains(action.getActionCode())) {
            List<PhysicalServerProvisionNetworkInventory> invs = networksFromAction(action);
            if (invs != null) {
                return action.copy().setParentIssuer(NAME).setParentIssuerContext(invs);
            }
        }
        return null;
    }
}
