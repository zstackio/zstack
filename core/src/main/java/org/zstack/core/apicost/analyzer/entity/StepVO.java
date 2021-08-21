package org.zstack.core.apicost.analyzer.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import java.math.BigDecimal;

/**
 * Created by huaxin on 2021/7/9.
 */
@Entity
public class StepVO extends BaseEntity<Integer> {

    @Column(name="stepId", length = 255)
    private String stepId;

    @Column(name="name", length = 255)
    private String name;

    @Column(name="meanWait", columnDefinition = "decimal(7,2)")
    private BigDecimal meanWait;

    @Column(name="apiId", length = 255)
    private String apiId;

    @Column(name="logCount", length = 10)
    private int logCount;

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

    public BigDecimal getMeanWait() {
        return meanWait;
    }

    public void setMeanWait(BigDecimal meanWait) {
        this.meanWait = meanWait;
    }

    public String getApiId() {
        return apiId;
    }

    public void setApiId(String apiId) {
        this.apiId = apiId;
    }

    public int getLogCount() {
        return logCount;
    }

    public void setLogCount(int logCount) {
        this.logCount = logCount;
    }
}
