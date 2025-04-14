package org.zstack.header.network.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class NetworkServiceProviderType {
    private static Map<String, NetworkServiceProviderType> types = Collections.synchronizedMap(new HashMap<String, NetworkServiceProviderType>());

    private final String typeName;
    private boolean createDhcpNameSpace = true;
    private boolean allocateDhcpServerIp = true;

    public NetworkServiceProviderType(String typeName) {
        this.typeName = typeName;
        types.put(typeName, this);
    }

    public static NetworkServiceProviderType valueOf(String typeName) {
        NetworkServiceProviderType type = types.get(typeName);
        if (type == null) {
            throw new IllegalArgumentException("NetworkServiceProviderType type: " + typeName + " was not registered by any NetworkServiceProvider");
        }
        return type;
    }

    @Override
    public String toString() {
        return typeName;
    }

    @Override
    public boolean equals(Object t) {
        if (!(t instanceof NetworkServiceProviderType)) {
            return false;
        }

        NetworkServiceProviderType type = (NetworkServiceProviderType) t;
        return type.toString().equals(typeName);
    }

    public boolean isCreateDhcpNameSpace() {
        return createDhcpNameSpace;
    }

    public void setCreateDhcpNameSpace(boolean createDhcpNameSpace) {
        this.createDhcpNameSpace = createDhcpNameSpace;
    }

    public boolean isAllocateDhcpServerIp() {
        return allocateDhcpServerIp;
    }

    public void setAllocateDhcpServerIp(boolean allocateDhcpServerIp) {
        this.allocateDhcpServerIp = allocateDhcpServerIp;
    }

    @Override
    public int hashCode() {
        return typeName.hashCode();
    }
}
