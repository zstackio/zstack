package org.zstack.header.vm;

import java.util.List;

public class ArchiveResourceConfigBundle {
    public static class ResourceConfigBundle {
        String resourceUuid;
        String identity;
        String value;

        public String getIdentity() {
            return identity;
        }

        public String getValue() {
            return value;
        }

        public void setIdentity(String identity) {
            this.identity = identity;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getResourceUuid() {
            return resourceUuid;
        }

        public void setResourceUuid(String resourceUuid) {
            this.resourceUuid = resourceUuid;
        }
    }

    List<ResourceConfigBundle> resourceConfigBundles;

    public List<ResourceConfigBundle> getResourceConfigBundles() {
        return resourceConfigBundles;
    }

    public void setResourceConfigBundles(List<ResourceConfigBundle> resourceConfigBundles) {
        this.resourceConfigBundles = resourceConfigBundles;
    }

    public ArchiveResourceConfigBundle() {
    }

    public ArchiveResourceConfigBundle(List<ResourceConfigBundle> resourceConfigBundles) {
        this.resourceConfigBundles = resourceConfigBundles;
    }
}
