package org.zstack.header.zwatch;

import java.util.List;

public interface ResourceMetricBindingExtensionPoint {
    class ResourceMetricBinding {
        private Class<?> resourceType;
        private String logicalMetricName;
        private String sourceNamespace;
        private String sourceMetricName;
        private String resourceField;
        private String sourceLabel;
        private boolean requireUniqueSourceKey;

        public Class<?> getResourceType() {
            return resourceType;
        }

        public void setResourceType(Class<?> resourceType) {
            this.resourceType = resourceType;
        }

        public String getLogicalMetricName() {
            return logicalMetricName;
        }

        public void setLogicalMetricName(String logicalMetricName) {
            this.logicalMetricName = logicalMetricName;
        }

        public String getSourceNamespace() {
            return sourceNamespace;
        }

        public void setSourceNamespace(String sourceNamespace) {
            this.sourceNamespace = sourceNamespace;
        }

        public String getSourceMetricName() {
            return sourceMetricName;
        }

        public void setSourceMetricName(String sourceMetricName) {
            this.sourceMetricName = sourceMetricName;
        }

        public String getResourceField() {
            return resourceField;
        }

        public void setResourceField(String resourceField) {
            this.resourceField = resourceField;
        }

        public String getSourceLabel() {
            return sourceLabel;
        }

        public void setSourceLabel(String sourceLabel) {
            this.sourceLabel = sourceLabel;
        }

        public boolean isRequireUniqueSourceKey() {
            return requireUniqueSourceKey;
        }

        public void setRequireUniqueSourceKey(boolean requireUniqueSourceKey) {
            this.requireUniqueSourceKey = requireUniqueSourceKey;
        }
    }

    List<ResourceMetricBinding> getResourceMetricBindings();
}
