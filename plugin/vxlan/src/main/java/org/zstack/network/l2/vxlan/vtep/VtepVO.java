package org.zstack.network.l2.vxlan.vtep;

import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.host.HostVO;
import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.NoView;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by weiwang on 02/03/2017.
 */
@Entity
@Table
@AutoDeleteTag
public class VtepVO {
    @Id
    @Column
    private String uuid;

    @Column
    @ForeignKey(parentEntityClass = HostVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String hostUuid;

    @Column
    @ForeignKey(parentEntityClass = ClusterVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String clusterUuid;

    @Column
    private String vtepIp;

    @Column
    private String vtepCidr;

    @Column
    private Integer port;

    @Column
    private String physicalInterface;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "VtepUuid", insertable = false, updatable = false)
    @NoView
    private Set<VtepL2NetworkRefVO> attachedNetworkRefs = new HashSet<>();

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getClusterUuid() {
        return clusterUuid;
    }

    public void setClusterUuid(String clusterUuid) {
        this.clusterUuid = clusterUuid;
    }

    public String getVtepIp() {
        return vtepIp;
    }

    public void setVtepIp(String vtepIp) {
        this.vtepIp = vtepIp;
    }

    public String getVtepCidr() {
        return vtepCidr;
    }

    public void setVtepCidr(String vtepCidr) {
        this.vtepCidr = vtepCidr;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getPhysicalInterface() {
        return physicalInterface;
    }

    public void setPhysicalInterface(String physicalInterface) {
        this.physicalInterface = physicalInterface;
    }

    public Set<VtepL2NetworkRefVO> getAttachedNetworkRefs() {
        return attachedNetworkRefs;
    }

    public void setAttachedNetworkRefs(Set<VtepL2NetworkRefVO> attachedNetworkRefs) {
        this.attachedNetworkRefs = attachedNetworkRefs;
    }
}
