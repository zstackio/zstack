package org.zstack.sdk;

import org.zstack.sdk.MemoryVmExclusionInventory;

public class MemoryPolicyInventory  {

    public MemoryVmExclusionInventory migrationExclusion;
    public void setMigrationExclusion(MemoryVmExclusionInventory migrationExclusion) {
        this.migrationExclusion = migrationExclusion;
    }
    public MemoryVmExclusionInventory getMigrationExclusion() {
        return this.migrationExclusion;
    }

    public java.util.Map sourceRevisions;
    public void setSourceRevisions(java.util.Map sourceRevisions) {
        this.sourceRevisions = sourceRevisions;
    }
    public java.util.Map getSourceRevisions() {
        return this.sourceRevisions;
    }

    public java.util.Map fieldSources;
    public void setFieldSources(java.util.Map fieldSources) {
        this.fieldSources = fieldSources;
    }
    public java.util.Map getFieldSources() {
        return this.fieldSources;
    }

    public java.util.Map fieldModes;
    public void setFieldModes(java.util.Map fieldModes) {
        this.fieldModes = fieldModes;
    }
    public java.util.Map getFieldModes() {
        return this.fieldModes;
    }

    public long sourceRevision;
    public void setSourceRevision(long sourceRevision) {
        this.sourceRevision = sourceRevision;
    }
    public long getSourceRevision() {
        return this.sourceRevision;
    }

    public java.util.List allowedActions;
    public void setAllowedActions(java.util.List allowedActions) {
        this.allowedActions = allowedActions;
    }
    public java.util.List getAllowedActions() {
        return this.allowedActions;
    }

    public java.util.List preflightRequiredActions;
    public void setPreflightRequiredActions(java.util.List preflightRequiredActions) {
        this.preflightRequiredActions = preflightRequiredActions;
    }
    public java.util.List getPreflightRequiredActions() {
        return this.preflightRequiredActions;
    }

    public java.util.Map fieldCapabilities;
    public void setFieldCapabilities(java.util.Map fieldCapabilities) {
        this.fieldCapabilities = fieldCapabilities;
    }
    public java.util.Map getFieldCapabilities() {
        return this.fieldCapabilities;
    }

    public java.lang.String source;
    public void setSource(java.lang.String source) {
        this.source = source;
    }
    public java.lang.String getSource() {
        return this.source;
    }

    public java.lang.String scope;
    public void setScope(java.lang.String scope) {
        this.scope = scope;
    }
    public java.lang.String getScope() {
        return this.scope;
    }

    public java.lang.String resourceUuid;
    public void setResourceUuid(java.lang.String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }
    public java.lang.String getResourceUuid() {
        return this.resourceUuid;
    }

    public long revision;
    public void setRevision(long revision) {
        this.revision = revision;
    }
    public long getRevision() {
        return this.revision;
    }

    public java.lang.String policy;
    public void setPolicy(java.lang.String policy) {
        this.policy = policy;
    }
    public java.lang.String getPolicy() {
        return this.policy;
    }

    public java.lang.String effectivePolicy;
    public void setEffectivePolicy(java.lang.String effectivePolicy) {
        this.effectivePolicy = effectivePolicy;
    }
    public java.lang.String getEffectivePolicy() {
        return this.effectivePolicy;
    }

    public java.lang.String bootstrapStatus;
    public void setBootstrapStatus(java.lang.String bootstrapStatus) {
        this.bootstrapStatus = bootstrapStatus;
    }
    public java.lang.String getBootstrapStatus() {
        return this.bootstrapStatus;
    }

    public java.lang.String bootstrapReason;
    public void setBootstrapReason(java.lang.String bootstrapReason) {
        this.bootstrapReason = bootstrapReason;
    }
    public java.lang.String getBootstrapReason() {
        return this.bootstrapReason;
    }

}
