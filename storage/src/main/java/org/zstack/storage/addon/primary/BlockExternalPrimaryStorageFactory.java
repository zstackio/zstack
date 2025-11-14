package org.zstack.storage.addon.primary;

import org.zstack.header.storage.primary.PrimaryStorageVO;

import java.util.Map;

/**
 * do not match external primary storage design.
 */
@Deprecated
public interface BlockExternalPrimaryStorageFactory {
    String getType();

    BlockExternalPrimaryStorageBackend getBlockExternalPrimaryStorageBackend(PrimaryStorageVO vo);
}
