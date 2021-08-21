package org.zstack.core.apicost.analyzer.entity;

import com.alibaba.fastjson.annotation.JSONField;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by huaxin on 2021/7/9.
 */
@Entity
public class ApiVO extends BaseEntity<Integer> {

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "apiId", length = 255)
    private String apiId;

    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "lastUpdate")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastUpdate;

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

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }
}
