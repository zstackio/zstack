package org.zstack.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cascade.AbstractAsyncCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.cluster.ClusterAO_;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.core.Completion;
import org.zstack.header.server.ServerPoolInventory;
import org.zstack.header.server.ServerPoolVO;
import org.zstack.header.server.ServerPoolVO_;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.header.zone.ZoneVO;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Cascade Zone deletion to ServerPoolVO. Without this extension Zone
 * deletion fails with FK constraint fkServerPoolVOZoneEO when any
 * ServerPool rows reference the zone (ServerPoolVO.zoneUuid is
 * @ForeignKey RESTRICT).
 */
public class ServerPoolCascadeExtension extends AbstractAsyncCascadeExtension {
    @Autowired
    private DatabaseFacade dbf;

    private static final String NAME = ServerPoolVO.class.getSimpleName();

    @Override
    public void asyncCascade(CascadeAction action, Completion completion) {
        if (action.isActionCode(CascadeConstant.DELETION_DELETE_CODE,
                CascadeConstant.DELETION_FORCE_DELETE_CODE)) {
            handleDeletion(action, completion);
        } else if (action.isActionCode(CascadeConstant.DELETION_CHECK_CODE)) {
            completion.success();
        } else if (action.isActionCode(CascadeConstant.DELETION_CLEANUP_CODE)) {
            dbf.eoCleanup(ServerPoolVO.class);
            completion.success();
        } else {
            completion.success();
        }
    }

    private void handleDeletion(CascadeAction action, Completion completion) {
        List<ServerPoolInventory> pools = poolsFromAction(action);
        if (pools == null || pools.isEmpty()) {
            completion.success();
            return;
        }
        List<String> uuids = pools.stream()
                .map(ServerPoolInventory::getUuid)
                .collect(Collectors.toList());
        SQL.New(ClusterVO.class)
                .in(ClusterAO_.serverPoolUuid, uuids)
                .set(ClusterAO_.serverPoolUuid, null)
                .update();
        dbf.removeByPrimaryKeys(uuids, ServerPoolVO.class);
        completion.success();
    }

    private List<ServerPoolInventory> poolsFromAction(CascadeAction action) {
        if (ZoneVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<ZoneInventory> zones = action.getParentIssuerContext();
            if (zones == null || zones.isEmpty()) {
                return null;
            }
            List<String> zoneUuids = zones.stream()
                    .map(ZoneInventory::getUuid)
                    .collect(Collectors.toList());
            List<ServerPoolVO> vos = Q.New(ServerPoolVO.class)
                    .in(ServerPoolVO_.zoneUuid, zoneUuids)
                    .list();
            if (vos.isEmpty()) {
                return null;
            }
            return ServerPoolInventory.valueOf(vos);
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
            List<ServerPoolInventory> invs = poolsFromAction(action);
            if (invs != null) {
                return action.copy().setParentIssuer(NAME).setParentIssuerContext(invs);
            }
        }
        return null;
    }
}
