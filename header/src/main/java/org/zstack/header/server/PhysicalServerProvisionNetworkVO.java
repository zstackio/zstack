package org.zstack.header.server;

import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.zone.ZoneEO;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

/**
 * Provisioning network intentionally does not implement {@code OwnedByAccount} — mirrors BM2's
 * {@code BareMetal2ProvisionNetworkVO} which is admin-only infrastructure (provision PRD §2.1).
 */
@Entity
@Table(name = "PhysicalServerProvisionNetworkVO")
@BaseResource
public class PhysicalServerProvisionNetworkVO extends ResourceVO {
    @Column
    @ForeignKey(parentEntityClass = ZoneEO.class, onDeleteAction = ReferenceOption.RESTRICT)
    private String zoneUuid;

    @Column
    private String name;

    @Column
    private String description;

    @Column
    @Enumerated(EnumType.STRING)
    private ProvisionNetworkType type;

    @Column
    private String dhcpInterface;

    @Column
    private String dhcpRangeStartIp;

    @Column
    private String dhcpRangeEndIp;

    @Column
    private String dhcpRangeNetmask;

    @Column
    private String dhcpRangeGateway;

    @Column
    @Enumerated(EnumType.STRING)
    private ProvisionNetworkState state;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "networkUuid", insertable = false, updatable = false)
    private Set<PhysicalServerProvisionNetworkClusterRefVO> clusterRefs;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "networkUuid")
    private Set<PhysicalServerProvisionNetworkPoolRefVO> poolRefs = new HashSet<>();

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public String getZoneUuid() { return zoneUuid; }
    public void setZoneUuid(String zoneUuid) { this.zoneUuid = zoneUuid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public ProvisionNetworkType getType() { return type; }
    public void setType(ProvisionNetworkType type) { this.type = type; }

    public String getDhcpInterface() { return dhcpInterface; }
    public void setDhcpInterface(String dhcpInterface) { this.dhcpInterface = dhcpInterface; }

    public String getDhcpRangeStartIp() { return dhcpRangeStartIp; }
    public void setDhcpRangeStartIp(String dhcpRangeStartIp) { this.dhcpRangeStartIp = dhcpRangeStartIp; }

    public String getDhcpRangeEndIp() { return dhcpRangeEndIp; }
    public void setDhcpRangeEndIp(String dhcpRangeEndIp) { this.dhcpRangeEndIp = dhcpRangeEndIp; }

    public String getDhcpRangeNetmask() { return dhcpRangeNetmask; }
    public void setDhcpRangeNetmask(String dhcpRangeNetmask) { this.dhcpRangeNetmask = dhcpRangeNetmask; }

    public String getDhcpRangeGateway() { return dhcpRangeGateway; }
    public void setDhcpRangeGateway(String dhcpRangeGateway) { this.dhcpRangeGateway = dhcpRangeGateway; }

    public ProvisionNetworkState getState() { return state; }
    public void setState(ProvisionNetworkState state) { this.state = state; }

    public Timestamp getCreateDate() { return createDate; }
    public void setCreateDate(Timestamp createDate) { this.createDate = createDate; }

    public Timestamp getLastOpDate() { return lastOpDate; }
    public void setLastOpDate(Timestamp lastOpDate) { this.lastOpDate = lastOpDate; }

    public Set<PhysicalServerProvisionNetworkClusterRefVO> getClusterRefs() { return clusterRefs; }
    public void setClusterRefs(Set<PhysicalServerProvisionNetworkClusterRefVO> clusterRefs) { this.clusterRefs = clusterRefs; }

    public Set<PhysicalServerProvisionNetworkPoolRefVO> getPoolRefs() { return poolRefs; }
    public void setPoolRefs(Set<PhysicalServerProvisionNetworkPoolRefVO> poolRefs) { this.poolRefs = poolRefs; }
}
