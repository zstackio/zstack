package org.zstack.core.notification;

import org.zstack.header.vo.BaseResource;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by xing5 on 2017/3/15.
 */
@Entity
@Table
@BaseResource
public class NotificationVO {
    @Id
    @Column
    private String uuid;
    @Column
    private String name;
    @Column
    private String content;
    @Column
    private String arguments;
    @Column
    private String sender;
    @Column
    @Enumerated(EnumType.STRING)
    private NotificationStatus status;
    @Column
    private String resourceUuid;
    @Column
    private String resourceType;
    @Column
    @Enumerated(EnumType.STRING)
    private NotificationType type;
    @Column
    private long time;
    @Column
    private String opaque;
    @Column
    private Timestamp createDate;
    @Column
    private Timestamp lastOpDate;

    // this column is for DB partitions, don't use it for application
    @Column
    private Timestamp dateTime;

    @PrePersist
    private void prePersist() {
        dateTime = new Timestamp(System.currentTimeMillis());
    }

    public String getOpaque() {
        return opaque;
    }

    public void setOpaque(String opaque) {
        this.opaque = opaque;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getArguments() {
        return arguments;
    }

    public void setArguments(String arguments) {
        this.arguments = arguments;
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

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public void setStatus(NotificationStatus status) {
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

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
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
