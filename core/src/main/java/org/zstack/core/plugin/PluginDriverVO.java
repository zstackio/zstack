package org.zstack.core.plugin;

import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.ResourceVO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table
@BaseResource
public class PluginDriverVO extends ResourceVO {
    @Column
    private String name;

    @Column
    private String type;

    @Column
    private String vendor;

    @Column
    private String features;

    public PluginDriverVO() {
    }

    public PluginDriverVO(PluginDriverVO other) {
        this.uuid = other.uuid;
        this.name = other.name;
        this.vendor = other.vendor;
        this.features = other.features;
        this.type = other.type;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFeatures() {
        return features;
    }

    public void setFeatures(String features) {
        this.features = features;
    }
}
