package org.zstack.xinfini.sdk.metric;

import java.util.List;

/**
 * @ Author : yh.w
 * @ Date   : Created in 17:30 2024/5/29
 */
public class MetricModule {
    private String resultType;
    private List<Value> result;

    public String getResultType() {
        return resultType;
    }

    public void setResultType(String resultType) {
        this.resultType = resultType;
    }

    public List<Value> getResult() {
        return result;
    }

    public void setResult(List<Value> result) {
        this.result = result;
    }

    public static class Value {
        private String resource;
        private List<Label> lables;
        private long time;
        private long value;

        public String getResource() {
            return resource;
        }

        public void setResource(String resource) {
            this.resource = resource;
        }

        public List<Label> getLables() {
            return lables;
        }

        public void setLables(List<Label> lables) {
            this.lables = lables;
        }

        public long getTime() {
            return time;
        }

        public void setTime(long time) {
            this.time = time;
        }

        public long getValue() {
            return value;
        }

        public void setValue(long value) {
            this.value = value;
        }
    }


    public static class Label {
        private String name;
        private String value;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
