package org.zstack.header.network.l2;

import org.zstack.header.host.HostMessage;
import org.zstack.header.message.NeedReplyMessage;

import java.util.List;
import java.util.Map;

/**
 * Created by boce.wang on 11/10/2023.
 */
public class L2NetworkIsolatedAttachOnHostMsg extends NeedReplyMessage implements HostMessage {
    private String hostUuid;

    private String migrateHostUuid;

    private Map<String, List<String>> isolatedL2NetworkMacMap;

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getMigrateHostUuid() {
        return migrateHostUuid;
    }

    public void setMigrateHostUuid(String migrateHostUuid) {
        this.migrateHostUuid = migrateHostUuid;
    }

    public Map<String, List<String>> getIsolatedL2NetworkMacMap() {
        return isolatedL2NetworkMacMap;
    }


    public void setIsolatedL2NetworkMacMap(Map<String, List<String>> isolatedL2NetworkMacMap) {
        this.isolatedL2NetworkMacMap = isolatedL2NetworkMacMap;
    }
}
