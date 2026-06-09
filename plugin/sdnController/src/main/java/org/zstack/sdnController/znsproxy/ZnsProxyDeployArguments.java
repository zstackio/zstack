package org.zstack.sdnController.znsproxy;

import com.google.gson.annotations.SerializedName;
import org.zstack.core.ansible.AnsibleBasicArguments;

import java.io.File;

public class ZnsProxyDeployArguments extends AnsibleBasicArguments {
    @SerializedName("src_pkg_znsproxy")
    private final String sourcePackagePath;

    @SerializedName("dst_pkg_znsproxy")
    private final String destinationPackagePath;

    @SerializedName("znsproxy_health_url")
    private final String healthUrl;

    public ZnsProxyDeployArguments(File sourcePackage, String destinationPackagePath, String healthUrl) {
        this.sourcePackagePath = sourcePackage.getAbsolutePath();
        this.destinationPackagePath = destinationPackagePath;
        this.healthUrl = healthUrl;
    }

    public String getSourcePackagePath() {
        return sourcePackagePath;
    }

    public String getDestinationPackagePath() {
        return destinationPackagePath;
    }

    public String getHealthUrl() {
        return healthUrl;
    }
}
