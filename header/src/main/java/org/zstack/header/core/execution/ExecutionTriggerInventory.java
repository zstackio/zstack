package org.zstack.header.core.execution;

import org.zstack.header.configuration.PythonClassInventory;

import java.io.Serializable;

@PythonClassInventory
public class ExecutionTriggerInventory implements Serializable {
    private String type;
    private String name;
    private String apiUuid;
    private String taskRunUuid;
    private String messageUuid;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getApiUuid() {
        return apiUuid;
    }

    public void setApiUuid(String apiUuid) {
        this.apiUuid = apiUuid;
    }

    public String getTaskRunUuid() {
        return taskRunUuid;
    }

    public void setTaskRunUuid(String taskRunUuid) {
        this.taskRunUuid = taskRunUuid;
    }

    public String getMessageUuid() {
        return messageUuid;
    }

    public void setMessageUuid(String messageUuid) {
        this.messageUuid = messageUuid;
    }
}
