package org.zstack.header.network;

import org.zstack.header.network.l2.NetworkOperationOrigin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class NetworkConfigChange {
    public enum Kind {
        L2_ENCAPSULATION_CHANGE,
        L2_METADATA_CHANGE,
        L3_CREATE,
        IP_RANGE_CONFIGURATION,
        DHCP_DNS_CONFIGURATION
    }

    public static final class L2MetadataChange {
        private final String name;
        private final String description;

        private L2MetadataChange(String name, String description) {
            this.name = name;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }
    }

    public static final class L2EncapsulationChange {
        private final String l2Type;
        private final int virtualNetworkId;

        private L2EncapsulationChange(String l2Type, int virtualNetworkId) {
            this.l2Type = l2Type;
            this.virtualNetworkId = virtualNetworkId;
        }

        public String getL2Type() {
            return l2Type;
        }

        public int getVirtualNetworkId() {
            return virtualNetworkId;
        }
    }

    public static final class L3Creation {
        private final String l3Uuid;
        private final String l3Type;
        private final List<String> systemTags;

        private L3Creation(String l3Uuid, String l3Type, List<String> systemTags) {
            this.l3Uuid = l3Uuid;
            this.l3Type = l3Type;
            this.systemTags = Collections.unmodifiableList(new ArrayList<>(
                    systemTags == null ? Collections.emptyList() : systemTags));
        }

        public String getL3Uuid() {
            return l3Uuid;
        }

        public String getL3Type() {
            return l3Type;
        }

        public List<String> getSystemTags() {
            return systemTags;
        }
    }

    public static final class IpRange {
        private final String rangeUuid;
        private final String startIp;
        private final String endIp;

        public IpRange(String rangeUuid, String startIp, String endIp) {
            this.rangeUuid = rangeUuid;
            this.startIp = startIp;
            this.endIp = endIp;
        }

        public String getRangeUuid() {
            return rangeUuid;
        }

        public String getStartIp() {
            return startIp;
        }

        public String getEndIp() {
            return endIp;
        }
    }

    public static final class IpRangeConfiguration {
        private final String l3Uuid;
        private final int ipVersion;
        private final String gatewayAddress;
        private final List<IpRange> ranges;
        private final boolean delete;

        private IpRangeConfiguration(String l3Uuid, int ipVersion, String gatewayAddress,
                                     List<IpRange> ranges, boolean delete) {
            this.l3Uuid = l3Uuid;
            this.ipVersion = ipVersion;
            this.gatewayAddress = gatewayAddress;
            this.ranges = Collections.unmodifiableList(new ArrayList<>(ranges));
            this.delete = delete;
        }

        public String getL3Uuid() {
            return l3Uuid;
        }

        public int getIpVersion() {
            return ipVersion;
        }

        public String getGatewayAddress() {
            return gatewayAddress;
        }

        public List<IpRange> getRanges() {
            return ranges;
        }

        public boolean isDelete() {
            return delete;
        }
    }

    public static final class DhcpDnsConfiguration {
        private final String l3Uuid;
        private final boolean enabled;
        private final List<String> systemTags;
        private final Integer ipVersion;
        private final List<String> dnsServers;

        private DhcpDnsConfiguration(String l3Uuid, boolean enabled, List<String> systemTags,
                                     Integer ipVersion, List<String> dnsServers) {
            this.l3Uuid = l3Uuid;
            this.enabled = enabled;
            this.systemTags = Collections.unmodifiableList(new ArrayList<>(
                    systemTags == null ? Collections.emptyList() : systemTags));
            this.ipVersion = ipVersion;
            this.dnsServers = dnsServers == null ? null
                    : Collections.unmodifiableList(new ArrayList<>(dnsServers));
        }

        public String getL3Uuid() {
            return l3Uuid;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public List<String> getSystemTags() {
            return systemTags;
        }

        public Integer getIpVersion() {
            return ipVersion;
        }

        public List<String> getDnsServers() {
            return dnsServers;
        }
    }

    private final Kind kind;
    private final String l2Uuid;
    private final NetworkOperationOrigin origin;
    private final String operationUuid;
    private final String accountUuid;
    private final L2EncapsulationChange l2EncapsulationChange;
    private final L2MetadataChange l2MetadataChange;
    private final L3Creation l3Creation;
    private final IpRangeConfiguration ipRangeConfiguration;
    private final DhcpDnsConfiguration dhcpDnsConfiguration;

    private NetworkConfigChange(Kind kind, String l2Uuid, NetworkOperationOrigin origin,
                                String operationUuid, String accountUuid,
                                L2EncapsulationChange l2EncapsulationChange,
                                L2MetadataChange l2MetadataChange,
                                L3Creation l3Creation,
                                IpRangeConfiguration ipRangeConfiguration,
                                DhcpDnsConfiguration dhcpDnsConfiguration) {
        this.kind = kind;
        this.l2Uuid = l2Uuid;
        this.origin = origin;
        this.operationUuid = operationUuid;
        this.accountUuid = accountUuid;
        this.l2EncapsulationChange = l2EncapsulationChange;
        this.l2MetadataChange = l2MetadataChange;
        this.l3Creation = l3Creation;
        this.ipRangeConfiguration = ipRangeConfiguration;
        this.dhcpDnsConfiguration = dhcpDnsConfiguration;
    }

    public static NetworkConfigChange changeL2Encapsulation(String l2Uuid,
                                                             NetworkOperationOrigin origin,
                                                             String operationUuid,
                                                             String accountUuid,
                                                             String l2Type,
                                                             int virtualNetworkId) {
        return new NetworkConfigChange(Kind.L2_ENCAPSULATION_CHANGE, l2Uuid, origin, operationUuid,
                accountUuid, new L2EncapsulationChange(l2Type, virtualNetworkId), null,
                null, null, null);
    }

    public static NetworkConfigChange updateL2Metadata(String l2Uuid,
                                                        NetworkOperationOrigin origin,
                                                        String operationUuid,
                                                        String accountUuid,
                                                        String name,
                                                        String description) {
        return new NetworkConfigChange(Kind.L2_METADATA_CHANGE, l2Uuid, origin, operationUuid,
                accountUuid, null, new L2MetadataChange(name, description),
                null, null, null);
    }

    public static NetworkConfigChange createL3(String l2Uuid,
                                                NetworkOperationOrigin origin,
                                                String operationUuid,
                                                String accountUuid,
                                                String l3Uuid,
                                                String l3Type,
                                                List<String> systemTags) {
        return new NetworkConfigChange(Kind.L3_CREATE, l2Uuid, origin, operationUuid,
                accountUuid, null, null,
                new L3Creation(l3Uuid, l3Type, systemTags), null, null);
    }

    public static NetworkConfigChange replaceIpRangeConfiguration(String l2Uuid,
                                                                    NetworkOperationOrigin origin,
                                                                    String operationUuid,
                                                                    String accountUuid,
                                                                    String l3Uuid,
                                                                    int ipVersion,
                                                                    String gatewayAddress,
                                                                    List<IpRange> ranges) {
        return new NetworkConfigChange(Kind.IP_RANGE_CONFIGURATION, l2Uuid, origin, operationUuid,
                accountUuid, null, null, null,
                new IpRangeConfiguration(l3Uuid, ipVersion, gatewayAddress, ranges, false), null);
    }

    public static NetworkConfigChange removeIpRangeConfiguration(String l2Uuid,
                                                                   NetworkOperationOrigin origin,
                                                                   String operationUuid,
                                                                   String accountUuid,
                                                                   String l3Uuid,
                                                                   int ipVersion) {
        return new NetworkConfigChange(Kind.IP_RANGE_CONFIGURATION, l2Uuid, origin, operationUuid,
                accountUuid, null, null, null,
                new IpRangeConfiguration(l3Uuid, ipVersion, null, Collections.emptyList(), true), null);
    }

    public static NetworkConfigChange updateDhcpConfiguration(String l2Uuid,
                                                                NetworkOperationOrigin origin,
                                                                String operationUuid,
                                                                String accountUuid,
                                                                String l3Uuid,
                                                                boolean enabled,
                                                                List<String> systemTags) {
        return new NetworkConfigChange(Kind.DHCP_DNS_CONFIGURATION, l2Uuid, origin, operationUuid,
                accountUuid, null, null, null, null,
                new DhcpDnsConfiguration(l3Uuid, enabled, systemTags, null, null));
    }

    public static NetworkConfigChange updateDnsConfiguration(String l2Uuid,
                                                               NetworkOperationOrigin origin,
                                                               String operationUuid,
                                                               String accountUuid,
                                                               String l3Uuid,
                                                               int ipVersion,
                                                               List<String> dnsServers) {
        return new NetworkConfigChange(Kind.DHCP_DNS_CONFIGURATION, l2Uuid, origin, operationUuid,
                accountUuid, null, null, null, null,
                new DhcpDnsConfiguration(l3Uuid, true, Collections.emptyList(), ipVersion, dnsServers));
    }

    public Kind getKind() {
        return kind;
    }

    public String getL2Uuid() {
        return l2Uuid;
    }

    public NetworkOperationOrigin getOrigin() {
        return origin;
    }

    public String getOperationUuid() {
        return operationUuid;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public L2EncapsulationChange getL2EncapsulationChange() {
        return l2EncapsulationChange;
    }

    public L2MetadataChange getL2MetadataChange() {
        return l2MetadataChange;
    }

    public L3Creation getL3Creation() {
        return l3Creation;
    }

    public IpRangeConfiguration getIpRangeConfiguration() {
        return ipRangeConfiguration;
    }

    public DhcpDnsConfiguration getDhcpDnsConfiguration() {
        return dhcpDnsConfiguration;
    }
}
