package org.zstack.server.flow;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.server.PhysicalServerRoleVO;
import org.zstack.header.server.PhysicalServerRoleVO_;
import org.zstack.header.server.SchedulingMode;
import org.zstack.header.server.flow.PathTwoFlowDataKey;

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

        PhysicalServerRoleVO existing = Q.New(PhysicalServerRoleVO.class)
                .eq(PhysicalServerRoleVO_.serverUuid, serverUuid)
                .eq(PhysicalServerRoleVO_.roleType, roleType)
                .find();
        if (existing != null) {
            data.put(PathTwoFlowDataKey.ROLE_PRE_EXISTED, Boolean.TRUE);
            // honour the existing roleUuid (path 1 may have set a different one)
            data.put(PathTwoFlowDataKey.ROLE_UUID, existing.getRoleUuid());
            trigger.next();
            return;
        }

        PhysicalServerRoleVO vo = new PhysicalServerRoleVO();
        vo.setUuid(Platform.getUuid());
        vo.setServerUuid(serverUuid);
        vo.setRoleType(roleType);
        vo.setRoleUuid(roleUuid);
        vo.setSchedulingMode(mode);
        dbf.persist(vo);
        data.put(PathTwoFlowDataKey.ROLE_VO_PK, vo.getUuid());
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
