package org.zstack.network.hostNetwork.lldp.entity;

import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.*;
import org.zstack.header.vo.Index;
import org.zstack.network.hostNetwork.HostNetworkInterfaceVO;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table
@SoftDeletionCascades({
        @SoftDeletionCascade(parent = HostNetworkInterfaceVO.class, joinColumn = "interfaceUuid")
})
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = HostNetworkInterfaceVO.class, myField = "interfaceUuid", targetField = "uuid")
        }
)
public class HostNetworkInterfaceLldpVO extends ResourceVO implements ToInventory, OwnedByAccount {
    @Column
    @Index
    @ForeignKey(parentEntityClass = HostNetworkInterfaceVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String interfaceUuid;

    @Column
    private String mode;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @Transient
    private String accountUuid;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name="interfaceUuid", insertable=false, updatable=false)
    @NoView
    private HostNetworkInterfaceLldpRefVO lldpRefVO;

    public String getInterfaceUuid() {
        return interfaceUuid;
    }

    public void setInterfaceUuid(String interfaceUuid) {
        this.interfaceUuid = interfaceUuid;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
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
    public String getAccountUuid() {
        return accountUuid;
    }

    @Override
    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public HostNetworkInterfaceLldpRefVO getLldpRefVO() {
        return lldpRefVO;
    }

    public void setLldpRefVO(HostNetworkInterfaceLldpRefVO lldpRefVO) {
        this.lldpRefVO = lldpRefVO;
    }

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }
}
