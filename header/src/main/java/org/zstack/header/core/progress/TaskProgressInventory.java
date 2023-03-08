package org.zstack.header.core.progress;

import org.zstack.utils.gson.JSONObjectUtil;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by xing5 on 2017/3/20.
 */
public class TaskProgressInventory {
    private String taskUuid;
    private String taskName;
    private String parentUuid;
    private String type;
    private String content;
    private LinkedHashMap opaque;
    private Long time;
    private List<TaskProgressInventory> subTasks;
    private String arguments;

    public TaskProgressInventory() {
    }

    public TaskProgressInventory(TaskProgressVO vo) {
        taskUuid = vo.getTaskUuid();
        parentUuid = vo.getParentUuid();
        type = vo.getType().toString();
        if (vo.getOpaque() != null) {
            opaque = JSONObjectUtil.toObject(vo.getOpaque(), LinkedHashMap.class);
        }
        taskName = vo.getTaskName();
        time = vo.getTime();
        arguments = vo.getArguments();
    }

    public String getArguments() {
        return arguments;
    }

    public void setArguments(String arguments) {
        this.arguments = arguments;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public List<TaskProgressInventory> getSubTasks() {
        return subTasks;
    }

    public void setSubTasks(List<TaskProgressInventory> subTasks) {
        this.subTasks = subTasks;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LinkedHashMap getOpaque() {
        return opaque;
    }

    public void setOpaque(LinkedHashMap opaque) {
        this.opaque = opaque;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }
}
