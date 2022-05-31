package org.zstack.header.vm;

import org.zstack.header.vo.Index;
import org.zstack.header.vo.ToInventory;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table
public class VmSchedHistoryVO implements ToInventory {
    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column
    @Index
    private String vmInstanceUuid;

    @Column
    private String accountUuid;

    @Column
    private String schedType;

    @Column
    private Boolean success;

    @Column
    private String lastHostUuid;

    @Column
    private String destHostUuid;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public String getSchedType() {
        return schedType;
    }

    public void setSchedType(String schedType) {
        this.schedType = schedType;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getLastHostUuid() {
        return lastHostUuid;
    }

    public void setLastHostUuid(String lastHostUuid) {
        this.lastHostUuid = lastHostUuid;
    }

    public String getDestHostUuid() {
        return destHostUuid;
    }

    public void setDestHostUuid(String destHostUuid) {
        this.destHostUuid = destHostUuid;
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
