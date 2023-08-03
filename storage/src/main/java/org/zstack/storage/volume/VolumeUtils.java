package org.zstack.storage.volume;

import org.zstack.header.volume.VolumeConstant;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeProvisioningStrategy;
import org.zstack.tag.SystemTagCreator;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

public class VolumeUtils {
    public static void SetVolumeProvisioningStrategy(String volumeUuid, VolumeProvisioningStrategy strategy) {
        SystemTagCreator tagCreator = VolumeSystemTags.VOLUME_PROVISIONING_STRATEGY.newSystemTagCreator(volumeUuid);
        tagCreator.setTagByTokens(
                map(
                        e(VolumeSystemTags.VOLUME_PROVISIONING_STRATEGY_TOKEN, strategy.toString())
                )
        );
        tagCreator.inherent = false;
        tagCreator.recreate = true;
        tagCreator.create();
    }

    public static boolean isDiskVolume(VolumeInventory volume) {
        return VolumeConstant.VOLUME_FORMAT_DISK.equals(volume.getFormat());
    }
}
