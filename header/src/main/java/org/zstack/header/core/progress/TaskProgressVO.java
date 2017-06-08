package org.zstack.header.core.progress;

import org.zstack.header.managementnode.ManagementNodeVO;
import org.zstack.header.vo.*;
import org.zstack.header.vo.ForeignKey;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by xing5 on 2017/3/20.
 */
@Table
@Entity
@BaseResource
public class TaskProgressVO {
    @Id
    @Column
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private long id;
    @Column
    private String taskUuid;
    @Column
    private String apiId;
    @Column
    private String taskName;
    @Column
    private String parentUuid;
    @Column
    @Enumerated(EnumType.STRING)
    private TaskType type;
    @Column
    private String content;
    @Column
    private String arguments;
    @Column
    private String opaque;
    @Column
    @org.zstack.header.vo.ForeignKey(parentEntityClass = ManagementNodeVO.class, parentKey = "uuid", onDeleteAction = ForeignKey.ReferenceOption.SET_NULL)
    private String managementUuid;
    @Column
    private Long time;
    @Column
    private Long timeToDelete;

    public long getTimeToDelete() {
        return timeToDelete;
    }

    public void setTimeToDelete(long timeToDelete) {
        this.timeToDelete = timeToDelete;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getManagementUuid() {
        return managementUuid;
    }

    public void setManagementUuid(String managementUuid) {
        this.managementUuid = managementUuid;
    }

    public String getArguments() {
        return arguments;
    }

    public void setArguments(String arguments) {
        this.arguments = arguments;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTaskUuid() {
        return taskUuid;
    }

    public void setTaskUuid(String taskUuid) {
        this.taskUuid = taskUuid;
    }

    public String getParentUuid() {
        return parentUuid;
    }

    public void setParentUuid(String parentUuid) {
        this.parentUuid = parentUuid;
    }

    public String getApiId() {
        return apiId;
    }

    public void setApiId(String apiId) {
        this.apiId = apiId;
    }

    public TaskType getType() {
        return type;
    }

    public void setType(TaskType type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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
}
