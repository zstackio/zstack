package org.zstack.header.identity;

import org.zstack.header.search.SqlTrigger;
import org.zstack.header.search.TriggerIndex;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Date;

@Entity
@Table
@TriggerIndex
@SqlTrigger(foreignVOClass=UserVO.class, foreignVOJoinColumn="userUuid")
@Inheritance(strategy=InheritanceType.JOINED)
public class UserPolicyRefVO {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column
    private long id;
    
    @Column
    private String userUuid;
    
    @Column
    private String policyUuid;
    
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
    
    public String getUserUuid() {
        return userUuid;
    }

    public void setUserUuid(String userUuid) {
        this.userUuid = userUuid;
    }

    public String getPolicyUuid() {
        return policyUuid;
    }

    public void setPolicyUuid(String policyUuid) {
        this.policyUuid = policyUuid;
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
