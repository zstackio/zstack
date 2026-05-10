package org.zstack.header.host;

/**
 * Created by david on 9/12/16.
 */
public interface AddHostMessage {
    String getName();

    String getDescription();

    String getManagementIp();

    String getClusterUuid();

    String getResourceUuid();

    /**
     * Pre-resolved {@code PhysicalServerVO.uuid} for path-2 (legacy AddHost) integration with
     * unified physical server management. Returns {@code null} for messages that have not opted
     * into path 2; in that case path-2 contributors fall back to {@code RoleMatchContext}-based
     * three-tier auto-association (FR-027).
     *
     * <p>Phase 3 fix-plan U1a — see ADR-012 for the {@code preGeneratedRoleUuid} ordering this
     * field participates in.</p>
     */
    default String getServerUuid() {
        return null;
    }
}
