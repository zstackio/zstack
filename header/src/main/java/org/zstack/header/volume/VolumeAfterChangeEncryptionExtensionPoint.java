package org.zstack.header.volume;

import org.zstack.header.core.Completion;

public interface VolumeAfterChangeEncryptionExtensionPoint {
    void volumeAfterChangeEncryption(VolumeInventory volume, boolean encrypted, Completion completion);
}
