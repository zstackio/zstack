package org.zstack.sdk.zwatch.datatype;



public class Datapoint  {

    public double value;
    public void setValue(double value) {
        this.value = value;
    }
    public double getValue() {
        return this.value;
    }

    public java.lang.Long time;
    public void setTime(java.lang.Long time) {
        this.time = time;
    }
    public java.lang.Long getTime() {
        return this.time;
    }

    public java.util.Map labels;
    public void setLabels(java.util.Map labels) {
        this.labels = labels;
    }
    public java.util.Map getLabels() {
        return this.labels;
    }

}
