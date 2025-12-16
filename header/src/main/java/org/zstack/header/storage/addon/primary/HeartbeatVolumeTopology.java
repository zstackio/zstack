package org.zstack.header.storage.addon.primary;

import java.util.HashMap;
import java.util.Map;

public class HeartbeatVolumeTopology {
    /**
     * key: storage space url
     * value: heartbeat volume covering the storage space, if the volume has no heartbeat, we should
     * treat the storage space as disconnected too. one storage space can only be covered by one heartbeat volume.
     * and one heartbeat volume can cover multiple storage spaces.
     */
    private Map<String, HeartbeatVolumeTO> heartbeatVolumeByCoveringPaths = new HashMap<>();

    public Map<String, HeartbeatVolumeTO> getHeartbeatVolumeByCoveringPaths() {
        return heartbeatVolumeByCoveringPaths;
    }

    public void setHeartbeatVolumeByCoveringPaths(Map<String, HeartbeatVolumeTO> heartbeatVolumeByCoveringPaths) {
        this.heartbeatVolumeByCoveringPaths = heartbeatVolumeByCoveringPaths;
    }

    public void putHeartbeatVolume(String coveringPath, HeartbeatVolumeTO heartbeatVolumeTO) {
        this.heartbeatVolumeByCoveringPaths.put(coveringPath, heartbeatVolumeTO);
    }
}
