package org.zstack.core.notification;

import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by xing5 on 2017/3/18.
 */
@Inventory(mappingVOClass = NotificationSubscriptionVO.class)
public class NotificationSubscriptionInventory {
    private String uuid;
    private String name;
    private String description;
    private String notificationName;
    private String filter;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static NotificationSubscriptionInventory valueOf(NotificationSubscriptionVO vo) {
        NotificationSubscriptionInventory inv = new NotificationSubscriptionInventory();
        inv.setUuid(vo.getUuid());
        inv.setName(vo.getName());
        inv.setDescription(vo.getDescription());
        inv.setNotificationName(vo.getNotificationName());
        inv.setFilter(vo.getFilter());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<NotificationSubscriptionInventory> valueOf(Collection<NotificationSubscriptionVO> vos) {
        List<NotificationSubscriptionInventory> invs = new ArrayList<>();
        for (NotificationSubscriptionVO vo : vos) {
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getNotificationName() {
        return notificationName;
    }

    public void setNotificationName(String notificationName) {
        this.notificationName = notificationName;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
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
