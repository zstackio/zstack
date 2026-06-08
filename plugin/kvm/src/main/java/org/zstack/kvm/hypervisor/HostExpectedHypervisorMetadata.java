package org.zstack.kvm.hypervisor;

/**
 * Expected host hypervisor version resolved from the local MN repository.
 */
public class HostExpectedHypervisorMetadata {
    private String uuid;
    private String architecture;
    private String osReleaseVersion;
    private String hypervisor;
    private String version;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getArchitecture() {
        return architecture;
    }

    public void setArchitecture(String architecture) {
        this.architecture = architecture;
    }

    public String getOsReleaseVersion() {
        return osReleaseVersion;
    }

    public void setOsReleaseVersion(String osReleaseVersion) {
        this.osReleaseVersion = osReleaseVersion;
    }

    public String getHypervisor() {
        return hypervisor;
    }

    public void setHypervisor(String hypervisor) {
        this.hypervisor = hypervisor;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
