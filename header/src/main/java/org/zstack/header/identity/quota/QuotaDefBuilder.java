package org.zstack.header.identity.quota;

import org.zstack.utils.DebugUtils;

public class QuotaDefBuilder {
    private String name;
    private Long defaultValue;
    private GetQuotaUsage getUsage;

    public static QuotaDefBuilder newBuilder() {
        return new QuotaDefBuilder();
    }

    public QuotaDefBuilder name(String name) {
        this.name = name;
        return this;
    }

    public QuotaDefBuilder defaultValue(Long defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public QuotaDefBuilder getUsage(GetQuotaUsage usage) {
        this.getUsage = usage;
        return this;
    }

    public interface GetQuotaUsage {
        Long apply(String accountUuid, String name);
    }

    static class QuotaDefinitionImpl implements QuotaDefinition {
        private String name;
        private Long defaultValue;
        private GetQuotaUsage getUsage;

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public Long getDefaultValue() {
            return this.defaultValue;
        }

        @Override
        public Long getQuotaUsage(String accountUuid) {
            return getUsage.apply(accountUuid, name);
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setDefaultValue(Long defaultValue) {
            this.defaultValue = defaultValue;
        }

        public GetQuotaUsage getGetUsage() {
            return getUsage;
        }

        public void setGetUsage(GetQuotaUsage getUsage) {
            this.getUsage = getUsage;
        }
    }

    public QuotaDefinition build() {
        DebugUtils.Assert(this.name != null, "quota name is required");
        DebugUtils.Assert(this.defaultValue != null, "quota defaultValue is required");
        DebugUtils.Assert(this.getUsage != null, "quota getUsage is required");

        QuotaDefinitionImpl quota = new QuotaDefinitionImpl();
        quota.name = this.name;
        quota.defaultValue = this.defaultValue;
        quota.getUsage = this.getUsage;
        return quota;
    }
}
