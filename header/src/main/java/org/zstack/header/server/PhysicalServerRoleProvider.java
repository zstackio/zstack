package org.zstack.header.server;

import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.host.HostVO;

import java.util.Optional;

/**
 * SPI for role modules to integrate with unified physical server management (FR-022, v3 2026-04-16).
 *
 * <p>Each role module (KVM, BM2, Container) implements this interface and registers as a Spring bean.
 * Hardware discovery is <b>not</b> part of this SPI — see
 * {@code HardwareDiscoveryStrategy} (role SPI PRD §2.5b) for the dedicated hardware-discovery SPI.
 *
 * <p><b>Version history</b>:
 * <ul>
 *   <li>v1: {@code registerRole} decoupled from AddHost (incorrect — could leave partial state)
 *   <li>v2: added {@code createRoleEntity / deleteRoleEntity} to resolve v1's partial-state issue
 *   <li>v3: hardware discovery lifted to a separate SPI, {@code checkBeforeDetach} generalised to
 *          {@code getWorkloadStatus}, legacy Add*Msg accept an optional {@code serverUuid} so that
 *          PS-first path and legacy path converge on one internal flow
 *   <li>v4 (Phase 3 fix-plan U2, AC-CB-14/15/16): added optional {@code powerOn/powerOff/powerReset}
 *          default methods. Unified power is OOB-first from {@code PhysicalServerVO}; these methods
 *          remain for role-owned legacy power data such as BM2 roleConfig.
 *   <li>v5: added {@code getAttachUnsupportedErrorCode} so {@code EXTERNAL_READONLY} providers can
 *          keep their module-specific API contract when the generic dispatcher rejects attach.
 * </ul>
 */
public interface PhysicalServerRoleProvider {

    // -------- identity --------

    ServerRoleType getRoleType();

    SchedulingMode getSchedulingMode();

    default String getAttachUnsupportedErrorCode() {
        return SysErrors.OPERATION_ERROR.toString();
    }

    // -------- VO classification (v6, Phase 3 fix-plan U-pathTwoSpi) --------

    /**
     * Classify a {@link HostVO} into a {@link ServerRoleType} that this provider owns.
     *
     * <p>Implementations use {@code instanceof} on the concrete VO hierarchy rather than
     * matching {@code hypervisorType} strings, so that VO-subclass relationships drive role
     * dispatch. For example, {@code BareMetal2GatewayVO extends KVMHostVO} is correctly
     * classified by {@code KvmRoleProvider} as {@link ServerRoleType#KVM_HOST} even though
     * its {@code hypervisorType} string is {@code "baremetal2"}.
     *
     * <p>Default returns empty so providers added before this method existed behave as if
     * they don't claim any VO. Providers that opt into path-2 must override.
     *
     * <p>Used by {@code PhysicalServerPathTwoContributor} to decide whether to prepend
     * AutoAssociate / CreatePhysicalServerRole / InitPhysicalServerCapacity flows to the
     * AddHost chain (replaces the v5 hypervisor-string check, which mis-skipped BM2
     * gateway hosts living in baremetal2 clusters).
     */
    default Optional<ServerRoleType> classify(HostVO hvo) {
        return Optional.empty();
    }

    // -------- entity lifecycle (Path 1: PS-first orchestration) --------

    /**
     * Create the underlying role entity (HostVO / BareMetal2ChassisVO / NativeHostVO) and wire it
     * to the given PhysicalServerVO. Implementations forward to the legacy {@code Add*Msg} with
     * {@code ctx.serverUuid} via {@code bus.send + CloudBusCallBack} so the dispatcher thread is
     * never blocked waiting on AddHost / AddChassis SSH/IPMI rounds.
     *
     * <p>Successful completion delivers the created role entity UUID (= HostVO.uuid /
     * BareMetal2ChassisVO.uuid / NativeHostVO.uuid), which is persisted as
     * {@code PhysicalServerRoleVO.roleUuid}.
     */
    void createRoleEntity(CreateRoleEntityContext context, ReturnValueCompletion<String> completion);

    /**
     * Delete the underlying role entity. Implementations forward to the legacy {@code Delete*Msg}
     * via {@code bus.send}. {@code PhysicalServerRoleVO} deletion is handled in the same cascade
     * chain (not by this method) so there is no partial-state window.
     */
    void deleteRoleEntity(String roleUuid, Completion completion);

    // -------- workload query --------

    /**
     * Report how much CPU / memory this role consumes on the given physical server. Invoked by
     * {@code RecalculatePhysicalServerCapacityMsg} when computing the business-tax bucket of the
     * unified capacity ledger.
     */
    CapacityUsage getCapacityConsumption(String serverUuid, String roleUuid);

    /**
     * Query the workload-state capability model for the given role. A non-null
     * {@code *BlockReason} field means the corresponding destructive operation (detach / poweroff
     * / powerreset / maintenance / migration) should be rejected unless {@code force=true}.
     *
     * <p>This capability model replaces v2's {@code checkBeforeDetach(serverUuid, roleUuid):
     * String} — extending to a new destructive operation only requires adding a new field to
     * {@link RoleWorkloadStatus}; the SPI signature never changes.
     */
    RoleWorkloadStatus getWorkloadStatus(String serverUuid, String roleUuid);

    // -------- power management (v4, Phase 3 U2 — AC-CB-14/15/16) --------

    /**
     * Legacy fallback for role-owned power metadata. {@code PhysicalServerManagerImpl} uses
     * {@code PhysicalServerVO.oob*} first and calls this only when the server itself has no OOB
     * credentials.
     */
    default void powerOn(String serverUuid, String roleUuid, Completion completion) {
        completion.fail(unsupportedPowerOp("power-on", roleUuid));
    }

    /**
     * Power off the role entity. See {@link #powerOn(String, String, Completion)} for semantics.
     */
    default void powerOff(String serverUuid, String roleUuid, Completion completion) {
        completion.fail(unsupportedPowerOp("power-off", roleUuid));
    }

    /**
     * Power reset (cycle) the role entity. See {@link #powerOn(String, String, Completion)} for
     * semantics.
     */
    default void powerReset(String serverUuid, String roleUuid, Completion completion) {
        completion.fail(unsupportedPowerOp("power-reset", roleUuid));
    }

    // -------- OOB power-credential fallback priority --------

    /**
     * Priority used by {@code PhysicalServerManagerImpl.choosePowerFallbackRole} when a
     * {@link org.zstack.header.server.PhysicalServerVO} has no native OOB credentials and
     * the manager must pick one role to delegate power operations to.
     *
     * <p>Higher value = preferred. When multiple roles share the same priority the first
     * one in iteration order is selected. Default 0 (lowest priority).
     *
     * <p>BM2 overrides to 100 because the BM2 chassis role always owns IPMI credentials
     * in its {@code roleConfig}, making it the natural OOB fallback for a multi-role server.
     */
    default int getPowerFallbackPriority() {
        return 0;
    }

    /**
     * Header-only error builder used by the power-op default methods. {@code header} cannot
     * import {@code org.zstack.core.Platform.operr}, so we hand-build the {@code ErrorCode}
     * with {@link SysErrors#OPERATION_ERROR} and a formatted detail string. Mirrors the result
     * shape of {@code Platform.operr(...)} as used elsewhere in the codebase (the runtime
     * {@code Platform.operr} also delegates to {@code SysErrors.OPERATION_ERROR}).
     */
    default ErrorCode unsupportedPowerOp(String op, String roleUuid) {
        return new ErrorCode(SysErrors.OPERATION_ERROR.toString(), String.format(
                "%s not supported for role[type:%s, uuid:%s]; this role type has no IPMI/Redfish path",
                op, getRoleType(), roleUuid));
    }
}
