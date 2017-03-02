package org.zstack.network.l2.vxlan.vtep;

import org.zstack.header.cluster.ClusterEO;
import org.zstack.header.host.HostEO;
import org.zstack.header.network.l2.L2NetworkEO;
import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.ForeignKey;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

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
    @ForeignKey(parentEntityClass = HostEO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String hostUuid;

    @Column
    @ForeignKey(parentEntityClass = ClusterEO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String clusterUuid;

    @Column
    private String vtepIp;

    @Column
    private Integer port;

    @Column
    @ForeignKey(parentEntityClass = L2NetworkEO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String poolUuid;

    @Column
    private String type;

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

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getPoolUuid() {
        return poolUuid;
    }

    public void setPoolUuid(String poolUuid) {
        this.poolUuid = poolUuid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
