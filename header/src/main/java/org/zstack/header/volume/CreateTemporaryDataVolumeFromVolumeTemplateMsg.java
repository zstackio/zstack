package org.zstack.header.volume;

/**
 * Created by MaJin on 2019/8/1.
 */
public class CreateTemporaryDataVolumeFromVolumeTemplateMsg extends CreateDataVolumeFromVolumeTemplateMsg {
    private String originVolumeUuid;

    public String getOriginVolumeUuid() {
        return originVolumeUuid;
    }

    public void setOriginVolumeUuid(String originVolumeUuid) {
        this.originVolumeUuid = originVolumeUuid;
    }
}
