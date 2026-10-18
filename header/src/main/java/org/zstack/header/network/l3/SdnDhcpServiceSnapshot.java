package org.zstack.header.network.l3;

import java.util.List;

public class SdnDhcpServiceSnapshot {
    private String uuid;
    private Long configVersion;
    private List<Config> configs;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Long getConfigVersion() {
        return configVersion;
    }

    public void setConfigVersion(Long configVersion) {
        this.configVersion = configVersion;
    }

    public List<Config> getConfigs() {
        return configs;
    }

    public void setConfigs(List<Config> configs) {
        this.configs = configs;
    }

    public static class Config {
        private String uuid;
        private String dhcpServiceUuid;
        private int ipVersion;
        private String dhcpIp;
        private int leaseTime;
        private String dhcpv6Mode;
        private Boolean allocationPending;
        private List<String> dnsServers;

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public String getDhcpServiceUuid() {
            return dhcpServiceUuid;
        }

        public void setDhcpServiceUuid(String dhcpServiceUuid) {
            this.dhcpServiceUuid = dhcpServiceUuid;
        }

        public int getIpVersion() {
            return ipVersion;
        }

        public void setIpVersion(int ipVersion) {
            this.ipVersion = ipVersion;
        }

        public String getDhcpIp() {
            return dhcpIp;
        }

        public void setDhcpIp(String dhcpIp) {
            this.dhcpIp = dhcpIp;
        }

        public int getLeaseTime() {
            return leaseTime;
        }

        public void setLeaseTime(int leaseTime) {
            this.leaseTime = leaseTime;
        }

        public String getDhcpv6Mode() {
            return dhcpv6Mode;
        }

        public void setDhcpv6Mode(String dhcpv6Mode) {
            this.dhcpv6Mode = dhcpv6Mode;
        }

        public Boolean getAllocationPending() {
            return allocationPending;
        }

        public void setAllocationPending(Boolean allocationPending) {
            this.allocationPending = allocationPending;
        }

        public List<String> getDnsServers() {
            return dnsServers;
        }

        public void setDnsServers(List<String> dnsServers) {
            this.dnsServers = dnsServers;
        }
    }
}
