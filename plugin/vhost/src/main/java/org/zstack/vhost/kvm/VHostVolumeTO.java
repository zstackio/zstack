package org.zstack.vhost.kvm;

import org.zstack.header.storage.addon.primary.ActiveVolumeTO;
import org.zstack.header.volume.VolumeProtocol;

public class VHostVolumeTO extends ActiveVolumeTO {
    {
        protocol = VolumeProtocol.VHost.name();
    }
}
