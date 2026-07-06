package org.zstack.storage.encrypt;

public final class ZbsVolumeEncryptionSizes {
    public static final long LUKS_PAYLOAD_OFFSET = 2068480L;

    private ZbsVolumeEncryptionSizes() {
    }

    public static long luksBackingSize(long virtualSize) {
        return virtualSize + LUKS_PAYLOAD_OFFSET;
    }
}
