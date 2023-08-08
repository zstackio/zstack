package org.zstack.kvm.xmlhook;

import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vo.*;
import org.zstack.header.vo.ForeignKey;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table
@SoftDeletionCascades({
        @SoftDeletionCascade(parent = VmInstanceVO.class, joinColumn = "vmInstanceUuid"),
        @SoftDeletionCascade(parent = XmlHookVO.class, joinColumn = "xmlHookUuid")
})
public class XmlHookVmInstanceRefVO implements ToInventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long id;

    @Column
    @ForeignKey(parentEntityClass = XmlHookVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String xmlHookUuid;

    @Column
    @ForeignKey(parentEntityClass = VmInstanceVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String vmInstanceUuid;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getXmlHookUuid() {
        return xmlHookUuid;
    }

    public void setXmlHookUuid(String xmlHookUuid) {
        this.xmlHookUuid = xmlHookUuid;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
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
