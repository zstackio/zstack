package org.zstack.header.server;

import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "PhysicalServerProvisionNetworkPoolRefVO",
       uniqueConstraints = @UniqueConstraint(columnNames = {"networkUuid", "poolUuid"}))
public class PhysicalServerProvisionNetworkPoolRefVO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private long id;

    @Column(nullable = false, length = 32)
    @ForeignKey(parentEntityClass = PhysicalServerProvisionNetworkVO.class,
                onDeleteAction = ReferenceOption.CASCADE)
    private String networkUuid;

    @Column(nullable = false, length = 32)
    @ForeignKey(parentEntityClass = ServerPoolVO.class,
                onDeleteAction = ReferenceOption.CASCADE)
    private String poolUuid;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PrePersist
    private void prePersist() {
        if (createDate == null) {
            createDate = new Timestamp(System.currentTimeMillis());
        }
    }

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getNetworkUuid() { return networkUuid; }
    public void setNetworkUuid(String networkUuid) { this.networkUuid = networkUuid; }

    public String getPoolUuid() { return poolUuid; }
    public void setPoolUuid(String poolUuid) { this.poolUuid = poolUuid; }

    public Timestamp getCreateDate() { return createDate; }
    public void setCreateDate(Timestamp createDate) { this.createDate = createDate; }

    public Timestamp getLastOpDate() { return lastOpDate; }
    public void setLastOpDate(Timestamp lastOpDate) { this.lastOpDate = lastOpDate; }
}
