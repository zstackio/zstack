package org.zstack.header.network.l3;

import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.service.NetworkServiceL3NetworkRefVO;
import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.EO;
import org.zstack.header.vo.NoView;
import org.zstack.header.zone.ZoneVO;

import javax.persistence.*;
import java.util.HashSet;
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
}
