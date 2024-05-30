package org.zstack.xinfini.sdk.metric;

import org.zstack.xinfini.sdk.XInfiniResponse;

/**
 * @ Author : yh.w
 * @ Date   : Created in 11:51 2024/5/28
 */
public class QueryMetricResponse extends XInfiniResponse {
    private MetricModule data;

    public MetricModule getData() {
        return data;
    }

    public void setData(MetricModule data) {
        this.data = data;
    }
}
