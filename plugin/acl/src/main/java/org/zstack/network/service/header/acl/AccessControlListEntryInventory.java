package org.zstack.network.service.header.acl;

import org.apache.commons.net.ntp.TimeStamp;
import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;

/**
 * @author: zhanyong.miao
 * @date: 2020-03-05
 **/
@PythonClassInventory
@Inventory(mappingVOClass = AccessControlListEntryVO.class)
public class AccessControlListEntryInventory {
    private Long entryId;
    private String aclUuid;

    private String ipEntries;

    private String description;

    private TimeStamp createDate;

    private TimeStamp lastOpDate;

    public AccessControlListEntryInventory() {
    }

    public AccessControlListEntryInventory(AccessControlListEntryVO vo) {
        this.entryId = vo.getEntryId();
        this.aclUuid = vo.getAclUuid();
        this.description = vo.getDescription();
        this.ipEntries = vo.getIpEntries();
        this.createDate = vo.getCreateDate();
        this.lastOpDate = vo.getLastOpDate();
    }

    public Long getEntryId() {
        return entryId;
    }

    public void setEntryId(Long entryId) {
        this.entryId = entryId;
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

    public TimeStamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(TimeStamp createDate) {
        this.createDate = createDate;
    }

    public TimeStamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(TimeStamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
}

