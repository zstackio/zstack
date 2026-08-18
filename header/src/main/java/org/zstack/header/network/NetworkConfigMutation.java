package org.zstack.header.network;

import org.zstack.header.network.l2.NetworkOperationOrigin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class NetworkConfigMutation {
    public enum Kind {
        ENCAP,
        L2_METADATA,
        L3_CREATE,
        IPAM,
        DHCP_DNS
    }

    public static final class MetadataTarget {
        private final String name;
        private final String description;

        private MetadataTarget(String name, String description) {
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

    public static final class EncapsulationTarget {
        private final String l2Type;
        private final int virtualNetworkId;

        private EncapsulationTarget(String l2Type, int virtualNetworkId) {
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

    public static final class L3CreateTarget {
        private final String l3Uuid;
        private final String l3Type;
        private final List<String> systemTags;

        private L3CreateTarget(String l3Uuid, String l3Type, List<String> systemTags) {
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

    public static final class IpRangeTarget {
        private final String rangeUuid;
        private final String startIp;
        private final String endIp;

        public IpRangeTarget(String rangeUuid, String startIp, String endIp) {
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

    public static final class IpamTarget {
        private final String l3Uuid;
        private final int ipVersion;
        private final String gatewayAddress;
        private final List<IpRangeTarget> ranges;
        private final boolean delete;

        private IpamTarget(String l3Uuid, int ipVersion, String gatewayAddress,
                           List<IpRangeTarget> ranges, boolean delete) {
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

        public List<IpRangeTarget> getRanges() {
            return ranges;
        }

        public boolean isDelete() {
            return delete;
        }
    }

    public static final class DhcpTarget {
        private final String l3Uuid;
        private final boolean enabled;
        private final List<String> systemTags;
        private final Integer ipVersion;
        private final List<String> dnsServers;

        private DhcpTarget(String l3Uuid, boolean enabled, List<String> systemTags,
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
    private final EncapsulationTarget encapsulation;
    private final MetadataTarget metadata;
    private final L3CreateTarget l3Create;
    private final IpamTarget ipam;
    private final DhcpTarget dhcp;

    private NetworkConfigMutation(Kind kind, String l2Uuid, NetworkOperationOrigin origin,
                                  String operationUuid, String accountUuid,
                                  EncapsulationTarget encapsulation,
                                  MetadataTarget metadata,
                                  L3CreateTarget l3Create,
                                  IpamTarget ipam,
                                  DhcpTarget dhcp) {
        this.kind = kind;
        this.l2Uuid = l2Uuid;
        this.origin = origin;
        this.operationUuid = operationUuid;
        this.accountUuid = accountUuid;
        this.encapsulation = encapsulation;
        this.metadata = metadata;
        this.l3Create = l3Create;
        this.ipam = ipam;
        this.dhcp = dhcp;
    }

    public static NetworkConfigMutation encapsulation(String l2Uuid,
                                                       NetworkOperationOrigin origin,
                                                       String operationUuid,
                                                       String accountUuid,
                                                       String l2Type,
                                                       int virtualNetworkId) {
        return new NetworkConfigMutation(Kind.ENCAP, l2Uuid, origin, operationUuid,
                accountUuid, new EncapsulationTarget(l2Type, virtualNetworkId), null,
                null, null, null);
    }

    public static NetworkConfigMutation metadata(String l2Uuid,
                                                  NetworkOperationOrigin origin,
                                                  String operationUuid,
                                                  String accountUuid,
                                                  String name,
                                                  String description) {
        return new NetworkConfigMutation(Kind.L2_METADATA, l2Uuid, origin, operationUuid,
                accountUuid, null, new MetadataTarget(name, description),
                null, null, null);
    }

    public static NetworkConfigMutation l3Create(String l2Uuid,
                                                  NetworkOperationOrigin origin,
                                                  String operationUuid,
                                                  String accountUuid,
                                                  String l3Uuid,
                                                  String l3Type,
                                                  List<String> systemTags) {
        return new NetworkConfigMutation(Kind.L3_CREATE, l2Uuid, origin, operationUuid,
                accountUuid, null, null,
                new L3CreateTarget(l3Uuid, l3Type, systemTags), null, null);
    }

    public static NetworkConfigMutation ipam(String l2Uuid,
                                              NetworkOperationOrigin origin,
                                              String operationUuid,
                                              String accountUuid,
                                              String l3Uuid,
                                              int ipVersion,
                                              String gatewayAddress,
                                              List<IpRangeTarget> ranges) {
        return new NetworkConfigMutation(Kind.IPAM, l2Uuid, origin, operationUuid,
                accountUuid, null, null, null,
                new IpamTarget(l3Uuid, ipVersion, gatewayAddress, ranges, false), null);
    }

    public static NetworkConfigMutation deleteIpam(String l2Uuid,
                                                    NetworkOperationOrigin origin,
                                                    String operationUuid,
                                                    String accountUuid,
                                                    String l3Uuid,
                                                    int ipVersion) {
        return new NetworkConfigMutation(Kind.IPAM, l2Uuid, origin, operationUuid,
                accountUuid, null, null, null,
                new IpamTarget(l3Uuid, ipVersion, null, Collections.emptyList(), true), null);
    }

    public static NetworkConfigMutation dhcp(String l2Uuid,
                                              NetworkOperationOrigin origin,
                                              String operationUuid,
                                              String accountUuid,
                                              String l3Uuid,
                                              boolean enabled,
                                              List<String> systemTags) {
        return new NetworkConfigMutation(Kind.DHCP_DNS, l2Uuid, origin, operationUuid,
                accountUuid, null, null, null, null,
                new DhcpTarget(l3Uuid, enabled, systemTags, null, null));
    }

    public static NetworkConfigMutation dhcpDns(String l2Uuid,
                                                 NetworkOperationOrigin origin,
                                                 String operationUuid,
                                                 String accountUuid,
                                                 String l3Uuid,
                                                 int ipVersion,
                                                 List<String> dnsServers) {
        return new NetworkConfigMutation(Kind.DHCP_DNS, l2Uuid, origin, operationUuid,
                accountUuid, null, null, null, null,
                new DhcpTarget(l3Uuid, true, Collections.emptyList(), ipVersion, dnsServers));
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

    public EncapsulationTarget getEncapsulation() {
        return encapsulation;
    }

    public MetadataTarget getMetadata() {
        return metadata;
    }

    public L3CreateTarget getL3Create() {
        return l3Create;
    }

    public IpamTarget getIpam() {
        return ipam;
    }

    public DhcpTarget getDhcp() {
        return dhcp;
    }
}
