package org.zstack.header.identity;

import java.sql.Timestamp;

public class UserGroupUserRefInventory {
    private long id;
    private String userUuid;
    private String groupUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;
    
    public static UserGroupUserRefInventory valueOf(UserGroupUserRefVO vo) {
        UserGroupUserRefInventory inv = new UserGroupUserRefInventory();
        inv.setId(vo.getId());
        inv.setUserUuid(vo.getUserUuid());
        inv.setGroupUuid(vo.getGroupUuid());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
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
