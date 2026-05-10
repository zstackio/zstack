package org.zstack.server.flow;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQL;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.server.PhysicalServerCapacityState;
import org.zstack.header.server.PhysicalServerCapacityVO;
import org.zstack.header.server.flow.PathTwoFlowDataKey;

import java.util.Map;

import static org.zstack.core.Platform.operr;

/**
 * Phase 3 fix-plan U1-lead — Path 2 FlowChain step 3.
 *
 * <p>Persists an initial {@link PhysicalServerCapacityVO} row (PK == serverUuid, 1:1 with
 * {@code PhysicalServerVO}). The row is inserted in
 * {@link PhysicalServerCapacityState#Stale} so {@code PhysicalServerCapacityUpdater.recalculate()}
 * (Wave 1 U4) is the source of truth for actual capacity numbers — InitFlow does not
 * compute capacity itself.</p>
 *
 * <p>Idempotent: skips the persist if a capacity row already exists (path 1 attach may have
 * written one).</p>
 *
 * <p>Closes AC-RS-04 / AC-RS-07 / AC-CM-04.</p>
 */
public class InitPhysicalServerCapacityFlow implements Flow {
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public void run(FlowTrigger trigger, Map data) {
        String serverUuid = (String) data.get(PathTwoFlowDataKey.SERVER_UUID);
        if (serverUuid == null) {
            trigger.fail(operr(
                    "InitPhysicalServerCapacityFlow missing serverUuid in flow data key[%s]",
                    PathTwoFlowDataKey.SERVER_UUID));
            return;
        }

        if (dbf.findByUuid(serverUuid, PhysicalServerCapacityVO.class) != null) {
            data.put(PathTwoFlowDataKey.CAPACITY_PRE_EXISTED, Boolean.TRUE);
            trigger.next();
            return;
        }

        PhysicalServerCapacityVO cap = new PhysicalServerCapacityVO();
        cap.setUuid(serverUuid);
        cap.setCapacityState(PhysicalServerCapacityState.Stale);
        dbf.persist(cap);
        trigger.next();
    }

    @Override
    public void rollback(FlowRollback trigger, Map data) {
        if (Boolean.TRUE.equals(data.get(PathTwoFlowDataKey.CAPACITY_PRE_EXISTED))) {
            trigger.rollback();
            return;
        }
        String serverUuid = (String) data.get(PathTwoFlowDataKey.SERVER_UUID);
        if (serverUuid != null) {
            SQL.New("delete from PhysicalServerCapacityVO where uuid = :uuid")
                    .param("uuid", serverUuid)
                    .execute();
        }
        trigger.rollback();
    }
}
