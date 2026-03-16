package org.zstack.header.identity;

/**
 * External tenant Provider SPI.
 * Each external service (ZCF, AIOS, etc.) implements this interface to integrate with the universal tenant resource isolation framework.
 *
 * The framework automatically collects all implementations through {@link org.zstack.core.componentloader.PluginRegistry}.
 * Each Provider returns a unique source identifier (such as "zcf") through {@link #getSource()},
 * corresponding to the HTTP Header X-Tenant-Source value.
 */
public interface ExternalTenantProvider {
    /**
     * Source identifier, such as "zcf", "svcX".
     * Corresponds to X-Tenant-Source header value.
     * Must be globally unique.
     */
    String getSource();

    /**
     * Validate tenant context validity.
     * Called after RestServer parses Header and before injecting into Session.
     * Throwing an exception indicates validation failure, and the request will be rejected.
     *
     * @param ctx External tenant context (already parsed from Header)
     */
    void validateTenant(ExternalTenantContext ctx);

    /**
     * Whether to track this type of resource.
     * After resource creation, the framework calls this method to decide whether to write to ExternalTenantResourceRefVO.
     * Returning false indicates that this resource type does not need to be associated with tenant.
     * Default is true (track all resources).
     *
     * @param resourceType Resource type (VO SimpleName, such as "VmInstanceVO")
     */
    default boolean shouldTrackResource(String resourceType) {
        return true;
    }

    /**
     * Resource binding callback (optional).
     * Called after ExternalTenantResourceRefVO is written,
     * Provider can use this for custom logic such as sending notifications or writing audit logs.
     *
     * @param ctx          External tenant context
     * @param resourceUuid Resource UUID
     * @param resourceType Resource type (VO SimpleName)
     */
    default void onResourceBound(ExternalTenantContext ctx,
                                  String resourceUuid, String resourceType) {
    }
}
