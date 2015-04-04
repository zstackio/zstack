package org.zstack.header.storage.backup;

import java.util.*;


public class BackupStorageType {
	private static Map<String, BackupStorageType> types = Collections.synchronizedMap(new HashMap<String, BackupStorageType>());
	private final String typeName;
	private final Set<String> supportedSchemes;
    private boolean exposed = true;
	
	public BackupStorageType(String typeName, String...protocols) {
		this.typeName = typeName;
		supportedSchemes = new HashSet<String>(protocols.length);
		Collections.addAll(supportedSchemes, protocols);
		types.put(typeName, this);
	}

    public BackupStorageType(String typeName, boolean expose, String...protocols) {
        this(typeName);
        this.exposed = expose;
    }

	public Set<String> getSupportedSchemes() {
        return supportedSchemes;
    }

    public static BackupStorageType valueOf(String typeName) {
		BackupStorageType type = types.get(typeName);
		if (type == null) {
			throw new IllegalArgumentException("BackupStorageType type: " + typeName + " was not registered by any BackupStorageFactory");
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
		if (t == null || !(t instanceof BackupStorageType)) {
			return false;
		}
		
		BackupStorageType type = (BackupStorageType)t;
		return type.toString().equals(typeName);
	}
	
	@Override
	public int hashCode() {
		return typeName.hashCode();
	}
	
    public static Set<String> getAllTypeNames() {
        HashSet<String> exposedTypes = new HashSet<String>();
        for (BackupStorageType type : types.values()) {
            if (type.isExposed()) {
                exposedTypes.add(type.toString());
            }
        }
        return exposedTypes;
    }
}
