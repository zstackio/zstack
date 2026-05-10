package org.zstack.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.core.workflow.FlowDoneHandler;
import org.zstack.header.core.workflow.FlowErrorHandler;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostVO;
import org.zstack.header.server.PhysicalServerEnqueueDiscoveryHook;
import org.zstack.header.server.PhysicalServerRoleProvider;
import org.zstack.header.server.RoleMatchContext;
import org.zstack.header.server.SchedulingMode;
import org.zstack.header.server.ServerRoleType;
import org.zstack.header.server.flow.PathTwoFlowDataKey;
import org.zstack.server.flow.AutoAssociateFlow;
import org.zstack.server.flow.CreatePhysicalServerRoleFlow;
import org.zstack.server.flow.InitPhysicalServerCapacityFlow;

import java.util.Map;
import java.util.Optional;

/**
 * Phase 1 (path-two refactor) — single reusable orchestrator that wires the path-2
 * 4-flow sequence (init data → AutoAssociate → CreatePhysicalServerRole →
 * InitPhysicalServerCapacity → post-commit EnqueueDiscovery) onto either an existing
 * FlowChain (Mode A: KVM AddHost / BM2 AddChassis) or a freshly built standalone chain
 * (Mode B: Container per-NativeHost sync, fan-out one chain per node).
 *
 * <p>This class consolidates the previously duplicated wire-up logic that lived in
 * {@link PhysicalServerPathTwoContributor#contributeAddHostFlows} (KVM, lines 71-134)
 * and {@code BareMetal2ChassisManagerImpl.contributePathTwoFlows} (BM2). After the
 * Phase 2/3 follow-ups land, both call sites delegate to {@link #appendPathTwoFlows}
 * and the orchestrator is the single place where the 4-step path-2 sequence is
 * defined.</p>
 *
 * <p>Dispatch is SPI-driven: each {@link PhysicalServerRoleProvider} declares which
 * {@link HostVO} subclasses it owns via {@link PhysicalServerRoleProvider#classify(HostVO)}.
 * The first provider that claims the VO supplies the {@code roleType} and
 * {@code schedulingMode} for the path-2 flow data. Providers that return empty are
 * skipped — when no provider claims the VO, Mode A is a no-op and Mode B short-circuits
 * to {@link Completion#success()}.</p>
 *
 * <p>NB-24 ordering: this orchestrator wires the role-side persists (RoleVO + PSC)
 * <b>before</b> any role-module connect/sync flow that might invoke
 * {@code HostCapacityUpdater.resolveServerUuidOrThrow(roleUuid)}. ADR-012 is the
 * normative source for the {@code preGeneratedRoleUuid} pattern — caller must
 * pre-generate {@code hvo.uuid} before invoking either entry point.</p>
 */
public class PhysicalServerPathTwoOrchestrator {

    @Autowired
    private AutoAssociateFlow autoAssociateFlow;
    @Autowired
    private CreatePhysicalServerRoleFlow createPhysicalServerRoleFlow;
    @Autowired
    private InitPhysicalServerCapacityFlow initPhysicalServerCapacityFlow;
    @Autowired
    private PhysicalServerEnqueueDiscoveryHook enqueueDiscoveryHook;
    @Autowired
    private PluginRegistry pluginRgty;

    private Optional<PhysicalServerRoleProvider> findOwningProvider(HostVO hvo) {
        for (PhysicalServerRoleProvider p : pluginRgty.getExtensionList(PhysicalServerRoleProvider.class)) {
            if (p.classify(hvo).isPresent()) {
                return Optional.of(p);
            }
        }
        return Optional.empty();
    }

    /**
     * Mode A — append the path-2 4-flow sequence onto an existing {@link FlowChain}.
     *
     * <p>Caller (KVM AddHost contributor / BM2 AddChassis manager) has already built
     * its chain and pre-derived {@code preResolvedServerUuid}, {@code matchCtx}, and
     * {@code clusterUuid}. This method only invokes {@code chain.then(...)} four times
     * (no {@code done}/{@code error}/{@code start}); chain lifecycle remains the
     * caller's responsibility.</p>
     *
     * <p>No-op when no {@link PhysicalServerRoleProvider} claims the VO via
     * {@link PhysicalServerRoleProvider#classify(HostVO)} — caller's downstream flows
     * proceed unmodified, preserving legacy unmatched-host / unmatched-chassis
     * behaviour.</p>
     *
     * @param chain                 existing FlowChain to extend (caller-owned)
     * @param hvo                   role-side VO with {@code uuid} pre-generated
     *                              (ADR-012)
     * @param preResolvedServerUuid PS UUID pre-resolved by caller (KVM
     *                              {@code APIAddKVMHostMsg.serverUuid} carrier-only),
     *                              or {@code null} to trigger AutoAssociate three-tier
     *                              fallback
     * @param matchCtx              serial / oob / mgmt-ip / zone tuple for
     *                              AutoAssociate (required when
     *                              {@code preResolvedServerUuid} is null)
     * @param clusterUuid           cluster UUID — used by AutoAssociate to resolve the
     *                              bound ServerPool
     */
    public void appendPathTwoFlows(FlowChain chain, HostVO hvo,
                                   String preResolvedServerUuid,
                                   RoleMatchContext matchCtx,
                                   String clusterUuid) {
        Optional<PhysicalServerRoleProvider> ownerOpt = findOwningProvider(hvo);
        if (!ownerOpt.isPresent()) {
            return;
        }
        PhysicalServerRoleProvider owner = ownerOpt.get();
        ServerRoleType roleType = owner.classify(hvo).orElseThrow(IllegalStateException::new);
        doAppendFlows(chain, hvo.getUuid(), roleType, owner.getSchedulingMode(),
                      preResolvedServerUuid, matchCtx, clusterUuid);
    }

    /**
     * Mode A (direct) — append path-2 flows for a role entity not in the {@link HostVO}
     * hierarchy (e.g. {@code BareMetal2ChassisVO}). The caller already knows
     * {@code roleType} and {@code schedulingMode} and passes them directly; SPI
     * {@code classify} is bypassed.
     *
     * <p>Used by {@code BareMetal2ChassisManagerImpl} (U1b): BM2 chassis extend
     * {@code ResourceVO} directly so {@link PhysicalServerRoleProvider#classify(HostVO)}
     * cannot dispatch them via the SPI path.</p>
     */
    public void appendPathTwoFlows(FlowChain chain, String roleUuid,
                                   ServerRoleType roleType,
                                   SchedulingMode schedulingMode,
                                   String preResolvedServerUuid,
                                   RoleMatchContext matchCtx,
                                   String clusterUuid) {
        doAppendFlows(chain, roleUuid, roleType, schedulingMode,
                      preResolvedServerUuid, matchCtx, clusterUuid);
    }

    private void doAppendFlows(FlowChain chain, String roleUuid,
                               ServerRoleType roleType,
                               SchedulingMode schedulingMode,
                               String preResolvedServerUuid,
                               RoleMatchContext matchCtx,
                               String clusterUuid) {
        final String resolvedServerUuid = preResolvedServerUuid;
        final RoleMatchContext resolvedMatchCtx = matchCtx;
        final String resolvedClusterUuid = clusterUuid;
        final ServerRoleType resolvedRoleType = roleType;
        final SchedulingMode resolvedMode = schedulingMode;

        chain.then(new NoRollbackFlow() {
            String __name__ = "u1a-init-path-2-flow-data";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                if (resolvedServerUuid != null && !resolvedServerUuid.isEmpty()) {
                    data.put(PathTwoFlowDataKey.SERVER_UUID, resolvedServerUuid);
                }
                data.put(PathTwoFlowDataKey.MATCH_CONTEXT, resolvedMatchCtx);
                data.put(PathTwoFlowDataKey.CLUSTER_UUID, resolvedClusterUuid);
                // ADR-012: roleUuid is the role-side entity UUID; persist as roleUuid.
                data.put(PathTwoFlowDataKey.ROLE_UUID, roleUuid);
                data.put(PathTwoFlowDataKey.ROLE_TYPE, resolvedRoleType.toString());
                data.put(PathTwoFlowDataKey.SCHEDULING_MODE, resolvedMode);
                trigger.next();
            }
        }).then(autoAssociateFlow)
          .then(createPhysicalServerRoleFlow)
          .then(initPhysicalServerCapacityFlow)
          .then(new NoRollbackFlow() {
              String __name__ = "u1a-post-commit-enqueue-discovery";

              @Override
              public void run(FlowTrigger trigger, Map data) {
                  // Best-effort post-commit hook: fire after the 3 path-2 flows committed
                  // (RoleVO + PSC persisted). The remaining downstream flows (connect /
                  // os-version / arch check) may still fail and trigger the chain's
                  // .error() handler, which reverse-rolls the path-2 trio. The discovery
                  // queue is idempotent so a stray enqueue causes no harm — the discovery
                  // worker simply finds an absent PS row and no-ops.
                  String serverUuid = (String) data.get(PathTwoFlowDataKey.SERVER_UUID);
                  if (serverUuid != null && !serverUuid.isEmpty()) {
                      enqueueDiscoveryHook.enqueueDiscovery(serverUuid);
                  }
                  trigger.next();
              }
          });
    }

    /**
     * Mode B — build a fresh standalone {@link FlowChain} via
     * {@link FlowChainBuilder#newSimpleFlowChain()}, append the path-2 4-flow
     * sequence, then start it. Designed for the Container per-NativeHost sync
     * fan-out where each NativeHost gets its own short-lived chain.
     *
     * <p>{@code preResolvedServerUuid} is implicitly {@code null} — Container path
     * never pre-resolves a PS; AutoAssociate's three-tier fallback resolves via
     * {@code matchCtx}.</p>
     *
     * <p>Short-circuits to {@link Completion#success()} when no
     * {@link PhysicalServerRoleProvider} claims the VO (the chain is never
     * started).</p>
     */
    public void runStandalone(HostVO hvo, RoleMatchContext matchCtx,
                              String clusterUuid, Completion completion) {
        Optional<PhysicalServerRoleProvider> ownerOpt = findOwningProvider(hvo);
        if (!ownerOpt.isPresent()) {
            completion.success();
            return;
        }

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("path-two-standalone-%s", hvo.getUuid()));
        appendPathTwoFlows(chain, hvo, null, matchCtx, clusterUuid);
        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }
}
