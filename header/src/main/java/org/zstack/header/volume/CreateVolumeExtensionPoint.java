package org.zstack.header.volume;

/**
 * Created by mingjian.deng on 2017/9/20.
 */
public interface CreateVolumeExtensionPoint {
    void preCreateVolume(APICreateDataVolumeMsg msg);

    void beforeCreateVolume(VolumeVO volume);

    void afterCreateVolume(VolumeVO volume);
}
