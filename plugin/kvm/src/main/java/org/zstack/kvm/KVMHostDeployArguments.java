package org.zstack.kvm;

import com.google.gson.annotations.SerializedName;
import org.zstack.core.ansible.SyncTimeRequestedDeployArguments;

public class KVMHostDeployArguments extends SyncTimeRequestedDeployArguments {
    @SerializedName("pkg_kvmagent")
    private final String packageName = KVMGlobalProperty.AGENT_PACKAGE_NAME;
    @SerializedName("init")
    private String init;
    @SerializedName("skipIpv6")
    private String skipIpv6;
    @SerializedName("isMini")
    private String isMini;
    @SerializedName("isBareMetal2Gateway")
    private String isBareMetal2Gateway;
    @SerializedName("bridgeDisableIptables")
    private String bridgeDisableIptables;
    @SerializedName("hostname")
    private String hostname;
    @SerializedName("skip_packages")
    private String skipPackages;
    @SerializedName("update_packages")
    private String updatePackages;
    @SerializedName("post_url")
    private String postUrl;

    @SerializedName("extra_packages")
    private String extraPackages;

    public String getInit() {
        return init;
    }

    public void setInit(String init) {
        this.init = init;
    }

    public String getSkipIpv6() {
        return skipIpv6;
    }

    public void setSkipIpv6(String skipIpv6) {
        this.skipIpv6 = skipIpv6;
    }

    public String getIsMini() {
        return isMini;
    }

    public void setIsMini(String isMini) {
        this.isMini = isMini;
    }

    public String getIsBareMetal2Gateway() {
        return isBareMetal2Gateway;
    }

    public void setIsBareMetal2Gateway(String isBareMetal2Gateway) {
        this.isBareMetal2Gateway = isBareMetal2Gateway;
    }

    public String getBridgeDisableIptables() {
        return bridgeDisableIptables;
    }

    public void setBridgeDisableIptables(String bridgeDisableIptables) {
        this.bridgeDisableIptables = bridgeDisableIptables;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getSkipPackages() {
        return skipPackages;
    }

    public void setSkipPackages(String skipPackages) {
        this.skipPackages = skipPackages;
    }

    public String getUpdatePackages() {
        return updatePackages;
    }

    public void setUpdatePackages(String updatePackages) {
        this.updatePackages = updatePackages;
    }

    public String getPostUrl() {
        return postUrl;
    }

    public void setPostUrl(String postUrl) {
        this.postUrl = postUrl;
    }

    @Override
    public String getPackageName() {
        return packageName;
    }

    public String getExtraPackages() {
        return extraPackages;
    }

    public void setExtraPackages(String extraPackages) {
        this.extraPackages = extraPackages;
    }
}
