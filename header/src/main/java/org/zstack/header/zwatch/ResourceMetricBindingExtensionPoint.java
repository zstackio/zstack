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

        private static <T> T requireValue(String fieldName, T value) {
            if (value == null) {
                throw new IllegalStateException(String.format("ResourceMetricBinding.%s must not be null", fieldName));
            }
            return value;
        }

        private static String requireText(String fieldName, String value) {
            requireValue(fieldName, value);
            if (value.trim().isEmpty()) {
                throw new IllegalStateException(String.format("ResourceMetricBinding.%s must not be empty", fieldName));
            }
            return value;
        }

        public Class<?> getResourceType() {
            return requireValue("resourceType", resourceType);
        }

        public void setResourceType(Class<?> resourceType) {
            this.resourceType = resourceType;
        }

        public String getLogicalMetricName() {
            return requireText("logicalMetricName", logicalMetricName);
        }

        public void setLogicalMetricName(String logicalMetricName) {
            this.logicalMetricName = logicalMetricName;
        }

        public String getSourceNamespace() {
            return requireText("sourceNamespace", sourceNamespace);
        }

        public void setSourceNamespace(String sourceNamespace) {
            this.sourceNamespace = sourceNamespace;
        }

        public String getSourceMetricName() {
            return requireText("sourceMetricName", sourceMetricName);
        }

        public void setSourceMetricName(String sourceMetricName) {
            this.sourceMetricName = sourceMetricName;
        }

        public String getResourceField() {
            return requireText("resourceField", resourceField);
        }

        public void setResourceField(String resourceField) {
            this.resourceField = resourceField;
        }

        public String getSourceLabel() {
            return requireText("sourceLabel", sourceLabel);
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
