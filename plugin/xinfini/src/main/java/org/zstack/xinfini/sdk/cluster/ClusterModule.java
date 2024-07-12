package org.zstack.xinfini.sdk.cluster;

import java.util.Map;

/**
 * @ Author : yh.w
 * @ Date   : Created in 11:29 2024/5/28
 */
public class ClusterModule {

    public ClusterModule(String name, String uuid, VersionInfo versionInfo) {
        this.name = name;
        this.uuid = uuid;
        this.versionInfo = versionInfo;
    }

    private String name;
    private String uuid;
    private VersionInfo versionInfo;

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public VersionInfo getVersionInfo() {
        return versionInfo;
    }

    public void setVersionInfo(VersionInfo versionInfo) {
        this.versionInfo = versionInfo;
    }

    public class VersionInfo {
        private String os;
        private Map<String, ModuleVersion> module;
        private Map<String, ProductVersion> product;
        private String version;
        private String arch;

        public String getArch() {
            return arch;
        }

        public void setArch(String arch) {
            this.arch = arch;
        }

        // Getters and Setters
        public String getOs() {
            return os;
        }

        public void setOs(String os) {
            this.os = os;
        }

        public Map<String, ModuleVersion> getModule() {
            return module;
        }

        public void setModule(Map<String, ModuleVersion> module) {
            this.module = module;
        }

        public Map<String, ProductVersion> getProduct() {
            return product;
        }

        public void setProduct(Map<String, ProductVersion> product) {
            this.product = product;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }
    }

    public static class ModuleVersion {
        private String version;

        // Getter and Setter
        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }
    }

    public static class ProductVersion {
        private String version;

        // Getter and Setter
        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }
    }

}
