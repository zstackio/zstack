package org.zstack.core.apicost.analyzer.entity;

import com.alibaba.fastjson.annotation.JSONField;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Created by huaxin on 2021/7/9.
 */
@Entity
public class StepLogVO extends BaseEntity<Integer>{

    @Column(name="stepId", length = 255)
    private String stepId;

    @Column(name="name", length = 255)
    private String name;

    @Column(name = "apiLogId")
    private long apiLogId;

    @Column(name = "startTime")
    private long startTime;

    @Column(name = "endTime")
    private long endTime;

    @Column(name="wait", columnDefinition = "decimal(7,2)")
    private BigDecimal wait;

    public String getStepId() {
        return stepId;
    }

    public void setStepId(String stepId) {
        this.stepId = stepId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getApiLogId() {
        return apiLogId;
    }

    public void setApiLogId(long apiLogId) {
        this.apiLogId = apiLogId;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public BigDecimal getWait() {
        return wait;
    }

    public void setWait(BigDecimal wait) {
        this.wait = wait;
    }
}
