package org.zstack.header.core.external.plugin;

public class FeaturePluginPackageInventory {
    public static final String SOURCE_UPLOAD = "upload";
    public static final String SOURCE_ISO = "iso";

    private String path;
    private String source;
    private String id;
    private String name;
    private String category;
    private String version;
    private Boolean installed;
    private String sha256;
    private Boolean desired;
    private String licenseAddon;
    private String error;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

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

    public Boolean getInstalled() {
        return installed;
    }

    public void setInstalled(Boolean installed) {
        this.installed = installed;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public static FeaturePluginPackageInventory __example__() {
        FeaturePluginPackageInventory inv = new FeaturePluginPackageInventory();
        inv.setPath("/opt/zstack-dvd/x86_64/h84r/feature-plugins/flowMeter-5.5.48-1.zpkg");
        inv.setSource(SOURCE_ISO);
        inv.setId("flowMeter");
        inv.setName("flowMeter");
        inv.setCategory("network");
        inv.setVersion("5.5.48-1");
        inv.setInstalled(false);
        return inv;
    }

    public String getSha256() {
        return sha256;
    }

    public void setSha256(String sha256) {
        this.sha256 = sha256;
    }

    public Boolean getDesired() {
        return desired;
    }

    public void setDesired(Boolean desired) {
        this.desired = desired;
    }

    public String getLicenseAddon() {
        return licenseAddon;
    }

    public void setLicenseAddon(String licenseAddon) {
        this.licenseAddon = licenseAddon;
    }
}
