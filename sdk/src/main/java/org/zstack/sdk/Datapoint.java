package org.zstack.sdk;

public class Datapoint  {

    public double value;
    public void setValue(double value) {
        this.value = value;
    }
    public double getValue() {
        return this.value;
    }

    public float time;
    public void setTime(float time) {
        this.time = time;
    }
    public float getTime() {
        return this.time;
    }

    public java.util.Map<String, String> labels;
    public void setLabels(java.util.Map<String, String> labels) {
        this.labels = labels;
    }
    public java.util.Map<String, String> getLabels() {
        return this.labels;
    }

}
