package org.zstack.header.network.l3;

import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.service.NetworkServiceL3NetworkRefVO;
import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.EO;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.NoView;
import org.zstack.header.zone.ZoneVO;
import org.zstack.utils.network.IPv6Constants;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table
@EO(EOClazz = L3NetworkEO.class)
@BaseResource
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = L2NetworkVO.class, myField = "l2NetworkUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = ZoneVO.class, myField = "zoneUuid", targetField = "uuid"),
        }
)
public class L3NetworkVO extends L3NetworkAO implements OwnedByAccount {
    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "l3NetworkUuid", insertable = false, updatable = false)
    @NoView
    private Set<L3NetworkDnsVO> dns = new HashSet<L3NetworkDnsVO>();

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "l3NetworkUuid", insertable = false, updatable = false)
    @NoView
    private Set<IpRangeVO> ipRanges = new HashSet<IpRangeVO>();

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "l3NetworkUuid", insertable = false, updatable = false)
    @NoView
    private Set<NetworkServiceL3NetworkRefVO> networkServices = new HashSet<NetworkServiceL3NetworkRefVO>();

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "l3NetworkUuid", insertable = false, updatable = false)
    @NoView
    private Set<L3NetworkHostRouteVO> hostRoutes = new HashSet<L3NetworkHostRouteVO>();

    @Transient
    private String accountUuid;

    public L3NetworkVO() {
    }

    public L3NetworkVO(L3NetworkVO vo) {
        this.setUuid(vo.getUuid());
        this.setName(vo.getName());
        this.setDescription(vo.getDescription());
        this.setState(vo.getState());
        this.setType(vo.getType());
        this.setZoneUuid(vo.getZoneUuid());
        this.setL2NetworkUuid(vo.getL2NetworkUuid());
        this.setSystem(vo.isSystem());
        this.setDnsDomain(vo.getDnsDomain());
        this.setIpVersion(vo.getIpVersion());
        this.setEnableIPAM(vo.getEnableIPAM());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
        this.setCategory(vo.getCategory());
        this.setDns(vo.getDns());
        this.setIpRanges(vo.getIpRanges());
        this.setNetworkServices(vo.getNetworkServices());
        this.setHostRoutes(vo.getHostRoutes());
        this.setAccountUuid(vo.getAccountUuid());
    }

    @Override
    public String getAccountUuid() {
        return accountUuid;
    }

    @Override
    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public Set<L3NetworkDnsVO> getDns() {
        return dns;
    }

    public void setDns(Set<L3NetworkDnsVO> dns) {
        this.dns = dns;
    }

    public Set<IpRangeVO> getIpRanges() {
        return ipRanges;
    }

    public void setIpRanges(Set<IpRangeVO> ipRanges) {
        this.ipRanges = ipRanges;
    }

    public Set<NetworkServiceL3NetworkRefVO> getNetworkServices() {
        return networkServices;
    }

    public void setNetworkServices(Set<NetworkServiceL3NetworkRefVO> networkServices) {
        this.networkServices = networkServices;
    }

    public Set<L3NetworkHostRouteVO> getHostRoutes() {
        return hostRoutes;
    }

    public void setHostRoutes(Set<L3NetworkHostRouteVO> hostRoutes) {
        this.hostRoutes = hostRoutes;
    }

    public List<Integer> getIpVersions() {
        List<Integer> ipVersions = new ArrayList<>();
        if (super.getIpVersion() == IPv6Constants.IPv4) {
            ipVersions.add(IPv6Constants.IPv4);
        } else if (super.getIpVersion() == IPv6Constants.IPv6) {
            ipVersions.add(IPv6Constants.IPv6);
        } else if (super.getIpVersion() == IPv6Constants.DUAL_STACK) {
            ipVersions.add(IPv6Constants.IPv4);
            ipVersions.add(IPv6Constants.IPv6);
        }

        return ipVersions;
    }
}
