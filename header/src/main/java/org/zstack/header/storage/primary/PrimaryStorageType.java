package org.zstack.header.storage.primary;

import java.util.*;


public class PrimaryStorageType {
	private static Map<String, PrimaryStorageType> types = Collections.synchronizedMap(new HashMap<String, PrimaryStorageType>());
	private final String typeName;
    private boolean exposed = true;
	private boolean supportHeartbeatFile;
	private boolean supportPingStorageGateway;
	
	public PrimaryStorageType(String typeName) {
		this.typeName = typeName;
		types.put(typeName, this);
	}

    public PrimaryStorageType(String typeName, boolean exposed) {
        this(typeName);
        this.exposed = exposed;
    }

    public static PrimaryStorageType valueOf(String typeName) {
		PrimaryStorageType type = types.get(typeName);
		if (type == null) {
			throw new IllegalArgumentException("PrimaryStorageType type: " + typeName + " was not registered by any PrimaryStorageFactory");
		}
		return type;
	}

    public boolean isExposed() {
        return exposed;
    }

    public void setExposed(boolean exposed) {
        this.exposed = exposed;
    }

    @Override
	public String toString() {
		return typeName;
	}
	
	@Override
	public boolean equals(Object t) {
		if (t == null || !(t instanceof PrimaryStorageType)) {
			return false;
		}
		
		PrimaryStorageType type = (PrimaryStorageType)t;
		return type.toString().equals(typeName);
	}
	
	@Override
	public int hashCode() {
		return typeName.hashCode();
	}
	
    public static Set<String> getAllTypeNames() {
        HashSet<String> exposedTypes = new HashSet<String>();
        for (PrimaryStorageType type : types.values()) {
            if (type.isExposed()) {
                exposedTypes.add(type.toString());
            }
        }
        return exposedTypes;
    }

	public boolean isSupportHeartbeatFile() {
		return supportHeartbeatFile;
	}

	public void setSupportHeartbeatFile(boolean supportHeartbeatFile) {
		this.supportHeartbeatFile = supportHeartbeatFile;
	}

	public boolean isSupportPingStorageGateway() {
		return supportPingStorageGateway;
	}

	public void setSupportPingStorageGateway(boolean supportPingStorageGateway) {
		this.supportPingStorageGateway = supportPingStorageGateway;
	}
}
