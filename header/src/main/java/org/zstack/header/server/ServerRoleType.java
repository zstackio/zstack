package org.zstack.header.server;

import java.util.*;

public class ServerRoleType {
    private static Map<String, ServerRoleType> types = Collections.synchronizedMap(new HashMap<String, ServerRoleType>());
    private final String typeName;

    public static final ServerRoleType KVM_HOST = new ServerRoleType("KVM_HOST");
    public static final ServerRoleType BAREMETAL_V2 = new ServerRoleType("BAREMETAL_V2");
    public static final ServerRoleType CONTAINER_HOST = new ServerRoleType("CONTAINER_HOST");

    public ServerRoleType(String typeName) {
        this.typeName = typeName;
        types.put(typeName, this);
    }

    public static boolean hasType(String type) {
        return types.containsKey(type);
    }

    public static ServerRoleType valueOf(String typeName) {
        ServerRoleType type = types.get(typeName);
        if (type == null) {
            throw new IllegalArgumentException("ServerRoleType: " + typeName + " was not registered");
        }
        return type;
    }

    @Override
    public String toString() {
        return typeName;
    }

    @Override
    public boolean equals(Object t) {
        if (!(t instanceof ServerRoleType)) {
            return false;
        }

        ServerRoleType type = (ServerRoleType) t;
        return type.toString().equals(typeName);
    }

    @Override
    public int hashCode() {
        return typeName.hashCode();
    }

    public static Set<String> getAllTypeNames() {
        return new HashSet<String>(types.keySet());
    }
}
