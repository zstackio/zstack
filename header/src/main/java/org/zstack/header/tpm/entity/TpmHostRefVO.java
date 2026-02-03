package org.zstack.header.tpm.entity;

import org.zstack.header.host.HostVO;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ToInventory;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.sql.Timestamp;

@Entity
@Table
@EntityGraph(
        friends = {
                @EntityGraph.Neighbour(type = TpmVO.class, myField = "tpmUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = HostVO.class, myField = "hostUuid", targetField = "uuid"),
        }
)
public class TpmHostRefVO implements ToInventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private long id;

    @Column
    @ForeignKey(parentEntityClass = TpmVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String tpmUuid;

    @Column
    @ForeignKey(parentEntityClass = HostVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String hostUuid;

    @Column
    private String path;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTpmUuid() {
        return tpmUuid;
    }

    public void setTpmUuid(String tpmUuid) {
        this.tpmUuid = tpmUuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }

    @Override
    public String toString() {
        return "TpmHostRefVO{" +
        "id=" + id +
        ", tpmUuid='" + tpmUuid + '\'' +
        ", hostUuid='" + hostUuid + '\'' +
        ", path='" + path + '\'' +
        ", createDate=" + createDate +
        ", lastOpDate=" + lastOpDate +
        '}';
    }
}
