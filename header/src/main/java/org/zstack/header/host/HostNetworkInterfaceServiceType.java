package org.zstack.header.host;

import java.util.ArrayList;
import java.util.List;

public enum HostNetworkInterfaceServiceType {
    ManagementNetwork,
    TenantNetwork,
    StorageNetwork,
    BackupNetwork,
    MigrationNetwork;

    public static List<HostNetworkInterfaceServiceType> fromStrings(List<String> strings) {
        List<HostNetworkInterfaceServiceType> list = new ArrayList<>();
        for (String s : strings) {
            try {
                list.add(HostNetworkInterfaceServiceType.valueOf(s));
            } catch (IllegalArgumentException e) {
                // Ignore
            }
        }
        return list;
    }
}
