package org.zstack.header.server;

import org.zstack.header.cluster.ClusterEO;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "PhysicalServerProvisionNetworkClusterRefVO",
        uniqueConstraints = @UniqueConstraint(columnNames = {"networkUuid", "clusterUuid"}))
public class PhysicalServerProvisionNetworkClusterRefVO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private long id;

    @Column
    @ForeignKey(parentEntityClass = PhysicalServerProvisionNetworkVO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String networkUuid;

    @Column
    @ForeignKey(parentEntityClass = ClusterEO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String clusterUuid;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getNetworkUuid() { return networkUuid; }
    public void setNetworkUuid(String networkUuid) { this.networkUuid = networkUuid; }

    public String getClusterUuid() { return clusterUuid; }
    public void setClusterUuid(String clusterUuid) { this.clusterUuid = clusterUuid; }

    public Timestamp getCreateDate() { return createDate; }
    public void setCreateDate(Timestamp createDate) { this.createDate = createDate; }

    public Timestamp getLastOpDate() { return lastOpDate; }
    public void setLastOpDate(Timestamp lastOpDate) { this.lastOpDate = lastOpDate; }
}
