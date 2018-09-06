package org.zstack.header.identity;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = AccountAccessKeyVO.class)
@PythonClassInventory
public class AccessKeyInventory {
    private String uuid;
    private String description;
    private String accountUuid;
    private String userUuid;
    private String AccessKeyID;
    private String AccessKeySecret;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static AccessKeyInventory valueOf(AccountAccessKeyVO vo) {
        AccessKeyInventory inv = new AccessKeyInventory();
        inv.setUuid(vo.getUuid());
        inv.setDescription(vo.getDescription());
        inv.setAccountUuid(vo.getAccountUuid());
        inv.setUserUuid(vo.getUserUuid());
        inv.setAccessKeyID(vo.getAccessKeyID());
        inv.setAccessKeySecret(vo.getAccessKeySecret());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<AccessKeyInventory> valueOf(Collection<AccountAccessKeyVO> vos) {
        List<AccessKeyInventory> lst = new ArrayList<AccessKeyInventory>(vos.size());
        for (AccountAccessKeyVO vo : vos) {
            lst.add(AccessKeyInventory.valueOf(vo));
        }
        return lst;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public String getUserUuid() {
        return userUuid;
    }

    public void setUserUuid(String userUuid) {
        this.userUuid = userUuid;
    }

    public String getAccessKeyID() {
        return AccessKeyID;
    }

    public void setAccessKeyID(String accessKeyID) {
        AccessKeyID = accessKeyID;
    }

    public String getAccessKeySecret() {
        return AccessKeySecret;
    }

    public void setAccessKeySecret(String accessKeySecret) {
        AccessKeySecret = accessKeySecret;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

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
}
