package org.zstack.header.cluster;

import org.zstack.header.core.ApiTimeout;
import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by GuoYi on 3/12/18
 */
@ApiTimeout(apiClasses = {APIUpdateClusterOSMsg.class})
public class UpdateClusterOSMsg extends NeedReplyMessage implements ClusterMessage {
    private String uuid;
    private String excludePackages;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getExcludePackages() {
        return excludePackages;
    }

    public void setExcludePackages(String excludePackages) {
        this.excludePackages = excludePackages;
    }

    @Override
    public String getClusterUuid() {
        return uuid;
    }
}
