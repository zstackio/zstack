package org.zstack.sdk;

import org.zstack.sdk.ScaleInStrategy;

public class AutoScalingHorizontalScalingProfileInventory extends org.zstack.sdk.AutoScalingProfileInventory {

    public java.lang.Long maxCapacity;
    public void setMaxCapacity(java.lang.Long maxCapacity) {
        this.maxCapacity = maxCapacity;
    }
    public java.lang.Long getMaxCapacity() {
        return this.maxCapacity;
    }

    public java.lang.Long minCapacity;
    public void setMinCapacity(java.lang.Long minCapacity) {
        this.minCapacity = minCapacity;
    }
    public java.lang.Long getMinCapacity() {
        return this.minCapacity;
    }

    public java.lang.Long initialCapacity;
    public void setInitialCapacity(java.lang.Long initialCapacity) {
        this.initialCapacity = initialCapacity;
    }
    public java.lang.Long getInitialCapacity() {
        return this.initialCapacity;
    }

    public java.lang.Long scalingStep;
    public void setScalingStep(java.lang.Long scalingStep) {
        this.scalingStep = scalingStep;
    }
    public java.lang.Long getScalingStep() {
        return this.scalingStep;
    }

    public ScaleInStrategy scaleInStrategy;
    public void setScaleInStrategy(ScaleInStrategy scaleInStrategy) {
        this.scaleInStrategy = scaleInStrategy;
    }
    public ScaleInStrategy getScaleInStrategy() {
        return this.scaleInStrategy;
    }

}
