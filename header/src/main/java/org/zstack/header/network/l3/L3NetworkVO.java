package org.zstack.header.network.l3;

import org.zstack.header.network.service.NetworkServiceL3NetworkRefVO;
import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.EO;
import org.zstack.header.vo.NoView;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table
@EO(EOClazz = L3NetworkEO.class)
@BaseResource
public class L3NetworkVO extends L3NetworkAO {
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
}
