package org.zstack.server.flow;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatch;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.server.PhysicalServerRoleVO;
import org.zstack.header.server.PhysicalServerRoleVO_;
import org.zstack.header.server.PhysicalServerVO;
import org.zstack.header.server.SchedulingMode;
import org.zstack.header.server.flow.PathTwoFlowDataKey;

import javax.persistence.LockModeType;
import java.util.Map;

import static org.zstack.core.Platform.operr;

/**
 * Phase 3 fix-plan U1-lead — Path 2 FlowChain step 2.
 *
 * <p>Persists a {@link PhysicalServerRoleVO} for the (serverUuid, roleType, roleUuid) tuple
 * the caller supplied via {@link PathTwoFlowDataKey}. Idempotent upsert — if a row already
 * exists for {@code (serverUuid, roleType)} (path 1 attach may have written it), the
 * existing row is reused and {@link PathTwoFlowDataKey#ROLE_UUID} in {@code data} is
 * rewritten to point at the existing entity UUID. Rollback only removes rows that THIS
 * run actually persisted.</p>
 *
 * <p>NB-24 ordering: this Flow runs <b>before</b> any role-module connect / sync flow that
 * might invoke {@code HostCapacityUpdater.resolveServerUuidOrThrow(roleUuid)}. ADR-012 is
 * the normative source for the {@code preGeneratedRoleUuid} pattern.</p>
 *
 * <p>Closes AC-RS-04 / AC-RS-07.</p>
 */
public class CreatePhysicalServerRoleFlow implements Flow {
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public void run(FlowTrigger trigger, Map data) {
        String serverUuid = (String) data.get(PathTwoFlowDataKey.SERVER_UUID);
        String roleUuid = (String) data.get(PathTwoFlowDataKey.ROLE_UUID);
        String roleType = (String) data.get(PathTwoFlowDataKey.ROLE_TYPE);
        SchedulingMode mode = (SchedulingMode) data.get(PathTwoFlowDataKey.SCHEDULING_MODE);

        if (serverUuid == null || roleUuid == null || roleType == null || mode == null) {
            trigger.fail(operr(
                    "CreatePhysicalServerRoleFlow missing required data: " +
                            "serverUuid=%s, roleUuid=%s, roleType=%s, mode=%s",
                    serverUuid, roleUuid, roleType, mode));
            return;
        }

        // ZSTAC-84191: idempotent upsert keyed on (roleUuid, roleType).
        //
        // Downstream consumers (notably ContainerEndpointBase
        // .recalcAndEvaluateCordonForEndpoint) call
        //   Q.New(PhysicalServerRoleVO.class)
        //     .eq(roleUuid, h.getUuid())
        //     .eq(roleType, CONTAINER_HOST)
        //     .findValue()
        // so (roleUuid, roleType) is the real uniqueness invariant — NOT
        // (serverUuid, roleType). The previous (serverUuid, roleType) check
        // failed to detect duplicates when the same role entity (NativeHost) was
        // re-associated to a DIFFERENT PhysicalServerVO across syncs, because
        // PhysicalServerAutoAssociator's three-tier match (serialNumber /
        // oobAddress / managementIp+zoneUuid) returns a fresh PSVO when any
        // mutable input (managementIp, cluster, …) changes between syncs. Two
        // PSVOs → two RoleVO rows with the same roleUuid → NonUniqueResultException
        // at the cordon evaluate flow.
        //
        // The SQLBatch opens a transaction so the PESSIMISTIC_WRITE on
        // PhysicalServerVO actually takes effect — mirrors the PS-row mutex
        // used in PhysicalServerManagerImpl.attachRoleVO (path-1) and
        // serialises concurrent path-2 flows that target the SAME serverUuid.
        // Cross-PSVO duplicates are caught by the (roleUuid, roleType) lookup
        // and resolved by rebinding the existing row to the new serverUuid.
        new SQLBatch() {
            @Override
            protected void scripts() {
                databaseFacade.getEntityManager().find(PhysicalServerVO.class, serverUuid,
                        LockModeType.PESSIMISTIC_WRITE);

                // Primary uniqueness key: (roleUuid, roleType). Pick up ANY existing
                // RoleVO for this role entity regardless of serverUuid — rebind if
                // the associated PSVO drifted, treat as no-op if it's already on
                // the same PSVO.
                PhysicalServerRoleVO byRole = Q.New(PhysicalServerRoleVO.class)
                        .eq(PhysicalServerRoleVO_.roleUuid, roleUuid)
                        .eq(PhysicalServerRoleVO_.roleType, roleType)
                        .find();
                if (byRole != null) {
                    if (!serverUuid.equals(byRole.getServerUuid())) {
                        byRole.setServerUuid(serverUuid);
                        byRole.setSchedulingMode(mode);
                        merge(byRole);
                    }
                    data.put(PathTwoFlowDataKey.ROLE_PRE_EXISTED, Boolean.TRUE);
                    data.put(PathTwoFlowDataKey.ROLE_UUID, byRole.getRoleUuid());
                    return;
                }

                // Secondary check: path 1 (APIAttachPhysicalServerRoleMsg) may have
                // pre-written a RoleVO for this (serverUuid, roleType) with a
                // different pre-generated roleUuid. Honour it.
                PhysicalServerRoleVO byServer = Q.New(PhysicalServerRoleVO.class)
                        .eq(PhysicalServerRoleVO_.serverUuid, serverUuid)
                        .eq(PhysicalServerRoleVO_.roleType, roleType)
                        .find();
                if (byServer != null) {
                    data.put(PathTwoFlowDataKey.ROLE_PRE_EXISTED, Boolean.TRUE);
                    data.put(PathTwoFlowDataKey.ROLE_UUID, byServer.getRoleUuid());
                    return;
                }

                PhysicalServerRoleVO vo = new PhysicalServerRoleVO();
                vo.setUuid(Platform.getUuid());
                vo.setServerUuid(serverUuid);
                vo.setRoleType(roleType);
                vo.setRoleUuid(roleUuid);
                vo.setSchedulingMode(mode);
                persist(vo);
                data.put(PathTwoFlowDataKey.ROLE_VO_PK, vo.getUuid());
            }
        }.execute();
        trigger.next();
    }

    @Override
    public void rollback(FlowRollback trigger, Map data) {
        if (Boolean.TRUE.equals(data.get(PathTwoFlowDataKey.ROLE_PRE_EXISTED))) {
            trigger.rollback();
            return;
        }
        String pk = (String) data.get(PathTwoFlowDataKey.ROLE_VO_PK);
        if (pk != null) {
            SQL.New("delete from PhysicalServerRoleVO where uuid = :uuid")
                    .param("uuid", pk)
                    .execute();
        }
        trigger.rollback();
    }
}
