package org.zstack.core.apicost.analyzer.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import java.math.BigDecimal;

/**
 * Created by huaxin on 2021/7/9.
 */
@Entity
public class RStepVO extends BaseEntity<Integer> {

    @Column(name = "fromStepId", length = 255)
    private String fromStepId;

    @Column(name = "toStepId", length = 255)
    private String toStepId;

    @Column(name = "apiId", length = 255)
    private String apiId;

    @Column(name = "weight", columnDefinition = "decimal(7,2)")
    private BigDecimal weight;

    public String getFromStepId() {
        return fromStepId;
    }

    public void setFromStepId(String fromStepId) {
        this.fromStepId = fromStepId;
    }

    public String getToStepId() {
        return toStepId;
    }

    public void setToStepId(String toStepId) {
        this.toStepId = toStepId;
    }

    public String getApiId() {
        return apiId;
    }

    public void setApiId(String apiId) {
        this.apiId = apiId;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }
}
