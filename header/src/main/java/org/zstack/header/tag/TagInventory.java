package org.zstack.header.tag;

import java.sql.Timestamp;

/**
 */
public class TagInventory {
    private String uuid;
    private String resourceUuid;
    private String resourceType;
    private String tag;
    private String type;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    private static TagInventory valueOf(TagAO ao) {
        TagInventory inv = new TagInventory();
        inv.setUuid(ao.getUuid());
        inv.setResourceType(ao.getResourceType());
        inv.setCreateDate(ao.getCreateDate());
        inv.setLastOpDate(ao.getLastOpDate());
        inv.setResourceUuid(ao.getResourceUuid());
        inv.setTag(ao.getTag());
        return inv;
    }

    public static TagInventory valueOf(UserTagVO vo) {
        TagInventory inv = valueOf((TagAO) vo);
        inv.setType(TagType.User.toString());
        return inv;
    }

    public static TagInventory valueOf(SystemTagVO vo) {
        TagInventory inv = valueOf((TagAO) vo);
        inv.setType(TagType.System.toString());
        return inv;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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
