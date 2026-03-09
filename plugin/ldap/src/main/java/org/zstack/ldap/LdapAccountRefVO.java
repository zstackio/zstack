package org.zstack.ldap;

import org.zstack.header.identity.AccountVO;
import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.Index;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Timestamp;

/**
 */
@Entity
@Table
@AutoDeleteTag
public class LdapAccountRefVO {
    @Id
    @Column
    private String uuid;

    @Column
    @Index
    private String ldapUid;

    @Column
    @ForeignKey(parentEntityClass = LdapServerVO.class, parentKey = "uuid", onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String ldapServerUuid;

    @Column
    @ForeignKey(parentEntityClass = AccountVO.class, parentKey = "uuid", onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String accountUuid;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
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

    public String getLdapUid() {
        return ldapUid;
    }

    public void setLdapUid(String ldapUid) {
        this.ldapUid = ldapUid;
    }

    public String getLdapServerUuid() {
        return ldapServerUuid;
    }

    public void setLdapServerUuid(String ldapServerUuid) {
        this.ldapServerUuid = ldapServerUuid;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }
}
