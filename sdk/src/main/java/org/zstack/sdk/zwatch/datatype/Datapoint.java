package org.zstack.sdk.zwatch.datatype;



public class Datapoint  {

    public double value;
    public void setValue(double value) {
        this.value = value;
    }
    public double getValue() {
        return this.value;
    }

    public long time;
    public void setTime(long time) {
        this.time = time;
    }
    public long getTime() {
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
