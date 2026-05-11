package org.zstack.header.server;

/**
 * Physical server management service contract (FR-001, FR-022 v3 2026-04-16).
 *
 * <p>The v2 {@code registerRole / unregisterRole} methods are retired — role lifecycle is now
 * driven by {@link PhysicalServerRoleProvider#createRoleEntity} /
 * {@link PhysicalServerRoleProvider#deleteRoleEntity}, invoked by the
 * {@code APIAttachPhysicalServerRoleMsg / APIDetachPhysicalServerRoleMsg} handlers (or by the
 * legacy {@code AddKVMHostMsg / AddBareMetal2ChassisMsg} path which carries an optional
 * {@code serverUuid}). Both paths share one internal transactional flow — creation of the role
 * entity and of {@code PhysicalServerRoleVO} happens in the same transaction; a failure rolls
 * the whole thing back, so there is no "HostVO created but RoleVO missing" window.
 */
public interface PhysicalServerManager {
    PhysicalServerRoleProvider getRoleProvider(ServerRoleType type);
}
