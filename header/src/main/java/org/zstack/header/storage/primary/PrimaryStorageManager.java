package org.zstack.header.storage.primary;

import org.zstack.header.storage.primary.PrimaryStorageFactory;
import org.zstack.header.storage.primary.PrimaryStorageType;

public interface PrimaryStorageManager {
	PrimaryStorageFactory getPrimaryStorageFactory(PrimaryStorageType type);
}
