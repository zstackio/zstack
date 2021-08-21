package org.zstack.core.apicost.analyzer.entity;

import javax.persistence.Column;
import javax.persistence.Entity;

/**
 * Created by huaxin on 2021/7/9.
 */
@Entity
public class ApiLogVO extends BaseEntity<Long> {

    @Column(name = "isAnalyzed", length = 1)
    private int isAnalyzed;

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "apiId", length = 255)
    private String apiId;

    @Column(name = "originApiId", length = 255)
    private String originApiId;

    public int getIsAnalyzed() {
        return isAnalyzed;
    }

    public void setIsAnalyzed(int analyzed) {
        this.isAnalyzed = analyzed;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getApiId() {
        return apiId;
    }

    public void setApiId(String apiId) {
        this.apiId = apiId;
    }

    public String getOriginApiId() {
        return originApiId;
    }

    public void setOriginApiId(String originApiId) {
        this.originApiId = originApiId;
    }
}
