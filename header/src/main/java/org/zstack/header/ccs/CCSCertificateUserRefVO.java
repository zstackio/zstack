package org.zstack.header.ccs;

import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ToInventory;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by Wenhao.Zhang on 21/10/15
 */
@Entity
@Table
@EntityGraph(
    friends = {
        @EntityGraph.Neighbour(type = CCSCertificateVO.class, myField = "certificateUuid", targetField = "uuid"),
    }
)
public class CCSCertificateUserRefVO implements ToInventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private long id;
    @Column
    private String userUuid;
    @Column
    @ForeignKey(parentEntityClass = CCSCertificateVO.class, parentKey = "uuid", onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String certificateUuid;
    @Column
    @Enumerated(EnumType.STRING)
    private CCSCertificateUserState state;
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

    public String getUserUuid() {
        return userUuid;
    }

    public void setUserUuid(String userUuid) {
        this.userUuid = userUuid;
    }

    public String getCertificateUuid() {
        return certificateUuid;
    }

    public void setCertificateUuid(String certificateUuid) {
        this.certificateUuid = certificateUuid;
    }

    public CCSCertificateUserState getState() {
        return state;
    }

    public void setState(CCSCertificateUserState state) {
        this.state = state;
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
        return "CCSCertificateUserRefVO{" +
        "id=" + id +
        ", userUuid='" + userUuid + '\'' +
        ", certificateUuid='" + certificateUuid + '\'' +
        ", state=" + state +
        ", createDate=" + createDate +
        ", lastOpDate=" + lastOpDate +
        '}';
    }
}
