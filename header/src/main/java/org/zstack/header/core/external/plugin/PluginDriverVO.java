package org.zstack.header.core.external.plugin;

import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.ResourceVO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.sql.Timestamp;

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
    private String license;

    @Column
    private String version;

    @Column
    private String description;

    @Column
    private String features;

    @Column
    private String optionTypes;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @Column
    private boolean deleted = false;

    public PluginDriverVO() {
    }

    public PluginDriverVO(PluginDriverVO other) {
        this.uuid = other.uuid;
        this.name = other.name;
        this.vendor = other.vendor;
        this.features = other.features;
        this.license = other.license;
        this.version = other.version;
        this.description = other.description;
        this.type = other.type;
        this.optionTypes = other.optionTypes;
        this.createDate = other.createDate;
        this.lastOpDate = other.lastOpDate;
        this.deleted = other.deleted;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
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

    public String getLicense() {
        return license;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOptionTypes() {
        return optionTypes;
    }

    public void setOptionTypes(String optionTypes) {
        this.optionTypes = optionTypes;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
}
