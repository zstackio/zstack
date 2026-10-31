package org.zstack.header.volume;

public interface VolumeEncryptionSecretExtensionPoint {
    String sealVolumeDekForHost(String volumeUuid, String hostUuid);
}
