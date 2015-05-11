package org.zstack.header.identity;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table
@Inheritance(strategy=InheritanceType.JOINED)
public class UserGroupUserRefVO {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column
    private long id;
    
    @Column
    private String userUuid;
    
    @Column
    private String groupUuid;
    
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

    public String getGroupUuid() {
        return groupUuid;
    }

    public void setGroupUuid(String groupUuid) {
        this.groupUuid = groupUuid;
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
