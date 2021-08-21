package org.zstack.core.apicost.analyzer.zstack;

import java.math.BigDecimal;

public class PredictResult {
    /**
     * 预计还需花费时间
     */
    private BigDecimal spendTime;
    /**
     * 预计还需执行步骤数
     */
    private Integer unFinishedSteps;
    /**
     * 已完成步骤数
     */
    private Integer finishedSteps;
    /**
     * 当前执行的步骤类
     */
    private String curStepClass;
    /**
     * 当前执行api的类名
     */
    private String curApiClass;

    public BigDecimal getSpendTime() {
        return spendTime;
    }

    public void setSpendTime(BigDecimal spendTime) {
        this.spendTime = spendTime;
    }

    public Integer getUnFinishedSteps() {
        return unFinishedSteps;
    }

    public void setUnFinishedSteps(Integer unFinishedSteps) {
        this.unFinishedSteps = unFinishedSteps;
    }

    public Integer getFinishedSteps() {
        return finishedSteps;
    }

    public void setFinishedSteps(Integer finishedSteps) {
        this.finishedSteps = finishedSteps;
    }

    public String getCurStepClass() {
        return curStepClass;
    }

    public void setCurStepClass(String curStepClass) {
        this.curStepClass = curStepClass;
    }

    public String getCurApiClass() {
        return curApiClass;
    }

    public void setCurApiClass(String curApiClass) {
        this.curApiClass = curApiClass;
    }
}
