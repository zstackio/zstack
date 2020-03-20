package org.zstack.header.acl;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**F
 * @author: zhanyong.miao
 * @date: 2020-03-05
 **/
@PythonClassInventory
@Inventory(mappingVOClass = AccessControlListEntryVO.class)
public class AccessControlListEntryInventory {
    private String uuid;
    private String aclUuid;

    private String ipEntries;

    private String description;

    private Timestamp createDate;

    private Timestamp lastOpDate;

    public AccessControlListEntryInventory() {
    }

    public static List<AccessControlListEntryInventory> valueOf(Collection<AccessControlListEntryVO> vos) {
        List<AccessControlListEntryInventory> invs = new ArrayList<>();
        for (AccessControlListEntryVO vo : vos ) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public static AccessControlListEntryInventory valueOf(AccessControlListEntryVO vo) {
        return new AccessControlListEntryInventory(vo);
    }

    public AccessControlListEntryInventory(AccessControlListEntryVO vo) {
        this.uuid = vo.getUuid();
        this.aclUuid = vo.getAclUuid();
        this.description = vo.getDescription();
        this.ipEntries = vo.getIpEntries();
        this.createDate = vo.getCreateDate();
        this.lastOpDate = vo.getLastOpDate();
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getAclUuid() {
        return aclUuid;
    }

    public void setAclUuid(String aclUuid) {
        this.aclUuid = aclUuid;
    }

    public String getIpEntries() {
        return ipEntries;
    }

    public void setIpEntries(String ipEntries) {
        this.ipEntries = ipEntries;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

