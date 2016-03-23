package org.zstack.core.cloudbus;

import org.zstack.header.message.LocalEvent;
import org.zstack.utils.JsonWrapper;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 11:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class CanonicalEvent extends LocalEvent {
    private String path;
    private Object content;
    private String managementNodeId;

    public String getManagementNodeId() {
        return managementNodeId;
    }

    public void setManagementNodeId(String managementNodeId) {
        this.managementNodeId = managementNodeId;
    }

    @Override
    public String getSubCategory() {
        return "canonicalEvent";
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Object getContent() {
        return content;
    }

    public void setContent(Object content) {
        this.content = content;
    }
}
