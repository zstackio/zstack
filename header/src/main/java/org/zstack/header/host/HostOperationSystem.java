package org.zstack.header.host;

import java.util.Objects;

/**
 * Define the description field for the operating system version for host.
 * 
 * Created by Wenhao.Zhang on 23/04/07
 */
public class HostOperationSystem {
    public final String distribution;
    public final String version;

    private HostOperationSystem(String osDistribution, String version) {
        this.distribution = osDistribution;
        this.version = version;
    }

    public boolean isValid() {
        return distribution != null && version != null;
    }

    @Override
    public String toString() {
        return String.format("%s %s", distribution, version);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        HostOperationSystem that = (HostOperationSystem) o;
        return Objects.equals(distribution, that.distribution)
                && Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(distribution, version);
    }

    public static HostOperationSystem of(String distribution, String osVersion) {
        return new HostOperationSystem(distribution, osVersion);
    }
}
