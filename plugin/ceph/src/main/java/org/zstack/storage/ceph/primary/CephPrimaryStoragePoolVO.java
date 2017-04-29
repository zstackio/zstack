package org.zstack.storage.ceph.primary;

import org.zstack.header.storage.primary.PrimaryStorageEO;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ResourceVO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.sql.Timestamp;

/**
 * Created by xing5 on 2017/2/28.
 */
@Entity
@Table
public class CephPrimaryStoragePoolVO extends ResourceVO {
    @Column
    @ForeignKey(parentEntityClass = PrimaryStorageEO.class, parentKey = "uuid", onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String primaryStorageUuid;
    @Column
    private String poolName;
    @Column
    private String description;
    @Column
    private Timestamp createDate;
    @Column
    private Timestamp lastOpDate;

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public String getPoolName() {
        return poolName;
    }

    public void setPoolName(String poolName) {
        this.poolName = poolName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
}
