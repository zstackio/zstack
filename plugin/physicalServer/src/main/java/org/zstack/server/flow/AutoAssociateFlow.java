package org.zstack.server.flow;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.server.RoleMatchContext;
import org.zstack.header.server.flow.PathTwoFlowDataKey;
import org.zstack.server.PhysicalServerAutoAssociator;

import java.util.Map;

import static org.zstack.core.Platform.operr;

/**
 * Phase 3 fix-plan U1-lead — Path 2 FlowChain step 1.
 *
 * <p>If the caller pre-supplied {@link PathTwoFlowDataKey#SERVER_UUID} (legacy
 * {@code AddKVMHostMsg.serverUuid} / {@code AddBareMetal2ChassisMsg.serverUuid} was
 * non-null), this Flow is a no-op. Otherwise resolves the {@code PhysicalServerVO} via
 * {@link PhysicalServerAutoAssociator#findOrCreate} (FR-027 three-tier fallback:
 * serialNumber → oobAddress + zone → managementIp + zone, then auto-create from the
 * cluster's bound ServerPool).</p>
 *
 * <p>Read-only when caller supplied {@code SERVER_UUID}; otherwise may persist a new
 * {@code PhysicalServerVO}. {@code PhysicalServerCascadeExtension} cascades RoleVO and
 * {@code PhysicalServerCapacityVO} when the parent FlowChain rolls back at later steps,
 * so this Flow does not need its own rollback (it extends {@link NoRollbackFlow}).</p>
 *
 * <p>Closes AC-RS-04 (KVM path 2) / AC-RS-07 (BM2 path 2) common root cause.</p>
 */
public class AutoAssociateFlow extends NoRollbackFlow {
    @Autowired
    private PhysicalServerAutoAssociator autoAssociator;

    @Override
    public void run(FlowTrigger trigger, Map data) {
        Object preset = data.get(PathTwoFlowDataKey.SERVER_UUID);
        if (preset instanceof String && !((String) preset).isEmpty()) {
            // path 2 caller already nailed serverUuid; skip association entirely
            trigger.next();
            return;
        }

        Object ctxObj = data.get(PathTwoFlowDataKey.MATCH_CONTEXT);
        if (!(ctxObj instanceof RoleMatchContext)) {
            trigger.fail(operr(
                    "AutoAssociateFlow needs pre-supplied serverUuid or RoleMatchContext, got[%s]",
                    ctxObj == null ? "null" : ctxObj.getClass().getName()));
            return;
        }
        RoleMatchContext ctx = (RoleMatchContext) ctxObj;
        String clusterUuid = (String) data.get(PathTwoFlowDataKey.CLUSTER_UUID);

        String serverUuid;
        try {
            serverUuid = autoAssociator.findOrCreate(ctx, clusterUuid);
        } catch (OperationFailureException ofe) {
            trigger.fail(ofe.getErrorCode());
            return;
        }

        if (serverUuid == null) {
            trigger.fail(operr(
                    "no PhysicalServer matched and no ServerPool is bound on cluster[uuid:%s]; " +
                            "create or attach a ServerPool first",
                    clusterUuid));
            return;
        }

        data.put(PathTwoFlowDataKey.SERVER_UUID, serverUuid);
        trigger.next();
    }
}
