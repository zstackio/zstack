package org.zstack.header.identity;

import java.io.Serializable;

/**
 * External tenant context DTO.
 * Passed by external services (like ZCF, AIOS, etc.) through HTTP Headers,
 * attached to SessionInventory throughout the entire request chain.
 */
public class ExternalTenantContext implements Serializable {
    private static final long serialVersionUID = 1L;

    // ThreadLocal used to pass current request's external tenant context at AOP level
    // Set by RestServer after Header parsing, cleaned up after request completion
    private static final ThreadLocal<ExternalTenantContext> current = new ThreadLocal<>();

    public static void setCurrent(ExternalTenantContext ctx) {
        current.set(ctx);
    }

    public static ExternalTenantContext getCurrent() {
        return current.get();
    }

    public static void clearCurrent() {
        current.remove();
    }

    private String source;      // Source service identifier, such as "zcf", "svcX"
    private String tenantId;    // External tenant identifier
    private String userId;      // External user identifier (optional)

    public ExternalTenantContext() {
    }

    public ExternalTenantContext(String source, String tenantId, String userId) {
        this.source = source;
        this.tenantId = tenantId;
        this.userId = userId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public String toString() {
        return String.format("ExternalTenantContext{source='%s', tenantId='%s', userId='%s'}", source, tenantId, userId);
    }
}
