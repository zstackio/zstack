package org.zstack.header.vm;

import java.util.List;

public interface NewVmInstanceMessage {
    String getName();
    String getDescription();
    String getZoneUuid();
    String getClusterUuid();
    String getHostUuid();
    List<String> getL3NetworkUuids();
    String getDefaultL3NetworkUuid();
    String getType();
    String getStrategy();
    List<String> getSystemTags();
}
