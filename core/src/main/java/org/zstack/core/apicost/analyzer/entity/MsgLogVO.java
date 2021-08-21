package org.zstack.core.apicost.analyzer.entity;

import com.alibaba.fastjson.annotation.JSONField;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Created by huaxin on 2021/7/15.
 */
@Entity
public class MsgLogVO extends BaseEntity<Long> {

    @Column(name="msgId", length = 255)
    private String msgId;

    @Column(name="msgName", length = 255)
    private String msgName;

    @Column(name="taskName", length = 255)
    private String taskName;

    @Column(name="wait", columnDefinition = "decimal(7,2)")
    private BigDecimal wait;

    @Column(name="apiId", length = 255)
    private String apiId;

    @Column(name = "startTime")
    private long startTime;

    @Column(name = "replyTime")
    private long replyTime;

    @Column(name = "status", length = 1)
    private int status;

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public String getMsgName() {
        return msgName;
    }

    public void setMsgName(String msgName) {
        this.msgName = msgName;
    }

    public BigDecimal getWait() {
        return wait;
    }

    public void setWait(BigDecimal wait) {
        this.wait = wait;
    }

    public String getApiId() {
        return apiId;
    }

    public void setApiId(String apiId) {
        this.apiId = apiId;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getReplyTime() {
        return replyTime;
    }

    public void setReplyTime(long replyTime) {
        this.replyTime = replyTime;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
