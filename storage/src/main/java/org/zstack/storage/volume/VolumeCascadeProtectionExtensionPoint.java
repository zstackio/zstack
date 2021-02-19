package org.zstack.storage.volume;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.volume.VolumeInventory;

import java.util.Arrays;

import static org.zstack.core.Platform.operr;

/**
 * Created by yaoning.li on 2021/2/19.
 */
public class VolumeCascadeProtectionExtensionPoint implements VolumeCascadeExtensionPoint {
    @Override
    public ErrorCode preDestroyVm(VolumeInventory inv) {
        String status = VolumeGlobalConfig.CASCADE_ALLOWS_VOLUME_STATUS.value(String.class);
        if (status.equals(VolumeGlobalConfig.CASCADE_ALLOWS_VOLUME_STATUS.defaultValue(String.class))) {
            return null;
        }

        if (!Arrays.asList(status.split(",")).contains(inv.getStatus())) {
            return operr("volume[%s] status is not in %s", inv.getUuid(), status);
        }

        return null;
    }
}
