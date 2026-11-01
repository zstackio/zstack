package org.zstack.header.core.external.plugin;

public class FeaturePluginInventory {
    public static final String STATUS_LOADED = "Loaded";
    public static final String STATUS_FAILED = "Failed";
    // set by zstack-ctl plugin reconcile when it cannot bring a plugin to its desired state
    public static final String STATUS_INCOMPATIBLE = "Incompatible";
    public static final String STATUS_PACKAGE_MISSING = "PackageMissing";

    private String id;
    private String name;
    private String category;
    private String version;
    private String status;
    private String error;
    private String sha256;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getSha256() {
        return sha256;
    }

    public void setSha256(String sha256) {
        this.sha256 = sha256;
    }
}
