package org.zstack.network.service.header.acl;

import org.apache.commons.net.ntp.TimeStamp;
import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;

import java.util.List;

/**
 * @author: zhanyong.miao
 * @date: 2020-03-05
 **/
@PythonClassInventory
@Inventory(mappingVOClass = AccessControlListVO.class)
public class AccessControlListInventory {
    private String uuid;
    private String  name;
    private Integer ipVersion;
    private String description;

    private TimeStamp createDate;
    private TimeStamp lastOpDate;
    private List<AccessControlListEntryInventory> entries;

    public AccessControlListInventory() {
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getIpVersion() {
        return ipVersion;
    }

    public void setIpVersion(Integer ipVersion) {
        this.ipVersion = ipVersion;
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

    public List<AccessControlListEntryInventory> getEntries() {
        return entries;
    }

    public void setEntries(List<AccessControlListEntryInventory> entries) {
        this.entries = entries;
    }
}