package org.zstack.storage.encrypt;

public final class ZbsVolumeEncryptionSizes {
    public static final long LUKS_PAYLOAD_OFFSET = 8L * 1024 * 1024;

    private ZbsVolumeEncryptionSizes() {
    }

    public static long luksBackingSize(long virtualSize) {
        return virtualSize + LUKS_PAYLOAD_OFFSET;
    }
}
