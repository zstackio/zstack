package org.zstack.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.host.HostSystemTags;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.host.AddHostMessage;
import org.zstack.header.host.HostVO;
import org.zstack.header.server.PhysicalServerPathTwoExtensionPoint;
import org.zstack.header.server.RoleMatchContext;

/**
 * Phase 3 fix-plan U1a — appends path-2 flows
 * (AutoAssociate → CreatePhysicalServerRole → InitPhysicalServerCapacity → post-commit
 * EnqueueDiscovery) to the AddHost {@link FlowChain}.
 *
 * <p>Delegates all flow wire-up to {@link PhysicalServerPathTwoOrchestrator#appendPathTwoFlows}.
 * SPI dispatch (provider classify, roleType, schedulingMode) and the 4-flow sequence
 * are now consolidated in the orchestrator — see its Javadoc for full details.
 *
 * <p>BM2 chassis path 2 (U1b) still lives in a parallel contributor on the BM2-add chain
 * (built separately in {@code BareMetal2ChassisManagerImpl}). This contributor fires only on
 * the AddHost chain.
 *
 * <p>Closes AC-RS-04 by making the FlowChain persist the {@link org.zstack.header.server.PhysicalServerRoleVO}
 * before the connect flow runs (NB-24 fail-loud ordering, ADR-012 normative).</p>
 */
public class PhysicalServerPathTwoContributor implements PhysicalServerPathTwoExtensionPoint {

    @Autowired
    private PhysicalServerPathTwoOrchestrator pathTwoOrchestrator;

    @Override
    public void contributeAddHostFlows(FlowChain chain, AddHostMessage msg, HostVO hvo, ClusterVO cluster) {
        // KVM AddHost only carries managementIp + zone for tier-3 fallback. serialNumber is
        // populated post-connect by HostSystemTags.SYSTEM_SERIAL_NUMBER (saveGeneralHostHardwareFacts).
        // For first-add the tag is absent and yields null — AutoAssociateFlow falls through to
        // the managementIp+zone tier. For re-add / reconnect the tag is present and tier-1 wins.
        final String serialNumber = HostSystemTags.SYSTEM_SERIAL_NUMBER.getTokenByResourceUuid(
                hvo.getUuid(), HostSystemTags.SYSTEM_SERIAL_NUMBER_TOKEN);
        final RoleMatchContext matchCtx = new RoleMatchContext()
                .setSerialNumber(serialNumber)
                .setManagementIp(msg.getManagementIp())
                .setZoneUuid(cluster.getZoneUuid());

        pathTwoOrchestrator.appendPathTwoFlows(chain, hvo, msg.getServerUuid(), matchCtx, cluster.getUuid());
    }
}
