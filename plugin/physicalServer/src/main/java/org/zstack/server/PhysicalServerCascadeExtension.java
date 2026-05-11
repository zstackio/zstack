package org.zstack.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cascade.AbstractAsyncCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.core.Completion;
import org.zstack.header.server.PhysicalServerAO_;
import org.zstack.header.server.PhysicalServerInventory;
import org.zstack.header.server.PhysicalServerVO;
import org.zstack.header.server.ServerPoolInventory;
import org.zstack.header.server.ServerPoolVO;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.header.zone.ZoneVO;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Cascade Zone deletion to PhysicalServerVO. Without this extension Zone
 * deletion fails with FK constraint fkPhysicalServerVOZoneEO when any
 * PhysicalServer rows reference the zone (e.g. test cleanup paths that leak
 * servers due to mid-test assertion failures).
 */
public class PhysicalServerCascadeExtension extends AbstractAsyncCascadeExtension {
    @Autowired
    private DatabaseFacade dbf;

    private static final String NAME = PhysicalServerVO.class.getSimpleName();

    @Override
    public void asyncCascade(CascadeAction action, Completion completion) {
        if (action.isActionCode(CascadeConstant.DELETION_DELETE_CODE,
                CascadeConstant.DELETION_FORCE_DELETE_CODE)) {
            handleDeletion(action, completion);
        } else if (action.isActionCode(CascadeConstant.DELETION_CHECK_CODE)) {
            completion.success();
        } else if (action.isActionCode(CascadeConstant.DELETION_CLEANUP_CODE)) {
            dbf.eoCleanup(PhysicalServerVO.class);
            completion.success();
        } else {
            completion.success();
        }
    }

    private void handleDeletion(CascadeAction action, Completion completion) {
        List<PhysicalServerInventory> servers = serversFromAction(action);
        if (servers == null || servers.isEmpty()) {
            completion.success();
            return;
        }
        List<String> uuids = servers.stream()
                .map(PhysicalServerInventory::getUuid)
                .collect(Collectors.toList());
        dbf.removeByPrimaryKeys(uuids, PhysicalServerVO.class);
        completion.success();
    }

    private List<PhysicalServerInventory> serversFromAction(CascadeAction action) {
        if (ZoneVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<ZoneInventory> zones = action.getParentIssuerContext();
            if (zones == null || zones.isEmpty()) {
                return null;
            }
            List<String> zoneUuids = zones.stream()
                    .map(ZoneInventory::getUuid)
                    .collect(Collectors.toList());
            List<PhysicalServerVO> vos = Q.New(PhysicalServerVO.class)
                    .in(PhysicalServerAO_.zoneUuid, zoneUuids)
                    .isNull(PhysicalServerAO_.poolUuid)
                    .list();
            if (vos.isEmpty()) {
                return null;
            }
            return PhysicalServerInventory.valueOf(vos);
        }
        if (ServerPoolVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<ServerPoolInventory> pools = action.getParentIssuerContext();
            if (pools == null || pools.isEmpty()) {
                return null;
            }
            List<String> poolUuids = pools.stream()
                    .map(ServerPoolInventory::getUuid)
                    .collect(Collectors.toList());
            List<PhysicalServerVO> vos = Q.New(PhysicalServerVO.class)
                    .in(PhysicalServerAO_.poolUuid, poolUuids)
                    .list();
            if (vos.isEmpty()) {
                return null;
            }
            return PhysicalServerInventory.valueOf(vos);
        }
        if (NAME.equals(action.getParentIssuer())) {
            return action.getParentIssuerContext();
        }
        return null;
    }

    @Override
    public List<String> getEdgeNames() {
        return Arrays.asList(ZoneVO.class.getSimpleName(), ServerPoolVO.class.getSimpleName());
    }

    @Override
    public String getCascadeResourceName() {
        return NAME;
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        if (CascadeConstant.DELETION_CODES.contains(action.getActionCode())) {
            List<PhysicalServerInventory> invs = serversFromAction(action);
            if (invs != null) {
                return action.copy().setParentIssuer(NAME).setParentIssuerContext(invs);
            }
        }
        return null;
    }
}
