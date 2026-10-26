package org.zstack.sdk;

import org.zstack.sdk.ZsDatasetTaskSettingsConfigSpec;
import org.zstack.sdk.ZsDatasetTaskSettingsSourcesSpec;

public class ZsDatasetTaskSettingsInventory  {

    public java.lang.String spaceUuid;
    public void setSpaceUuid(java.lang.String spaceUuid) {
        this.spaceUuid = spaceUuid;
    }
    public java.lang.String getSpaceUuid() {
        return this.spaceUuid;
    }

    public ZsDatasetTaskSettingsConfigSpec config;
    public void setConfig(ZsDatasetTaskSettingsConfigSpec config) {
        this.config = config;
    }
    public ZsDatasetTaskSettingsConfigSpec getConfig() {
        return this.config;
    }

    public ZsDatasetTaskSettingsSourcesSpec sources;
    public void setSources(ZsDatasetTaskSettingsSourcesSpec sources) {
        this.sources = sources;
    }
    public ZsDatasetTaskSettingsSourcesSpec getSources() {
        return this.sources;
    }

    public java.lang.String updateAt;
    public void setUpdateAt(java.lang.String updateAt) {
        this.updateAt = updateAt;
    }
    public java.lang.String getUpdateAt() {
        return this.updateAt;
    }

}
