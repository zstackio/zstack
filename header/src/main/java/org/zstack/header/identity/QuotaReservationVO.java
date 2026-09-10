package org.zstack.header.identity;

import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;
import org.zstack.header.vo.Index;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.sql.Timestamp;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"operationUuid", "quotaName"}))
public class QuotaReservationVO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private long id;

    @Column
    @Index
    @ForeignKey(parentEntityClass = AccountVO.class, parentKey = "uuid", onDeleteAction = ReferenceOption.CASCADE)
    private String accountUuid;

    @Column
    @Index
    private String quotaName;

    @Column
    private long amount;

    @Column
    @Index
    private String operationUuid;

    @Column
    @Index
    private String resourceUuid;

    @Column
    private Timestamp createDate;

    public long getId() {
        return id;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public String getQuotaName() {
        return quotaName;
    }

    public void setQuotaName(String quotaName) {
        this.quotaName = quotaName;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public String getOperationUuid() {
        return operationUuid;
    }

    public void setOperationUuid(String operationUuid) {
        this.operationUuid = operationUuid;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }
}
