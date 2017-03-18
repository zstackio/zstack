package org.zstack.core.notification;

import org.zstack.header.message.NoJsonSchema;
import org.zstack.header.search.Inventory;
import org.zstack.utils.gson.JSONObjectUtil;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by xing5 on 2017/3/18.
 */
@Inventory(mappingVOClass = NotificationVO.class)
public class NotificationInventory {
    private String uuid;
    private String name;
    private String content;
    private String arguments;
    private String sender;
    private String status;
    private String resourceUuid;
    private String resourceType;
    private String type;
    private Long time;
    @NoJsonSchema
    private Object opaque;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static NotificationInventory valueOf(NotificationVO vo) {
        NotificationInventory inv = new NotificationInventory();
        inv.setUuid(vo.getUuid());
        inv.setName(vo.getName());
        inv.setContent(vo.getContent());
        inv.setArguments(vo.getArguments());
        inv.setSender(vo.getSender());
        inv.setStatus(vo.getStatus().toString());
        inv.setResourceUuid(vo.getResourceUuid());
        inv.setResourceType(vo.getResourceType());
        inv.setType(vo.getType().toString());
        inv.setTime(vo.getTime());
        if (vo.getOpaque() != null) {
            inv.setOpaque(JSONObjectUtil.toObject(vo.getOpaque(), LinkedHashMap.class));
        }
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());

        return inv;
    }

    public static List<NotificationInventory> valueOf(Collection<NotificationVO> vos) {
        List<NotificationInventory> invs = new ArrayList<>();
        for (NotificationVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getArguments() {
        return arguments;
    }

    public void setArguments(String arguments) {
        this.arguments = arguments;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public Object getOpaque() {
        return opaque;
    }

    public void setOpaque(Object opaque) {
        this.opaque = opaque;
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
