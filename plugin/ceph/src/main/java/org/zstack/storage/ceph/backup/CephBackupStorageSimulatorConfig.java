package org.zstack.storage.ceph.backup;

import org.zstack.storage.ceph.backup.CephBackupStorageBase.DeleteCmd;
import org.zstack.storage.ceph.backup.CephBackupStorageBase.DownloadCmd;
import org.zstack.storage.ceph.backup.CephBackupStorageBase.InitCmd;
import org.zstack.storage.ceph.backup.CephBackupStorageBase.PingCmd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by frank on 7/28/2015.
 */
public class CephBackupStorageSimulatorConfig {
    public static class CephBackupStorageConfig {
        public long totalCapacity;
        public long availCapacity;
        public String fsid;
    }

    List<InitCmd> initCmds = new ArrayList<InitCmd>();
    Map<String, CephBackupStorageConfig> config = new HashMap<String, CephBackupStorageConfig>();
    List<DownloadCmd> downloadCmds = new ArrayList<DownloadCmd>();
    List<DeleteCmd> deleteCmds = new ArrayList<DeleteCmd>();
    List<PingCmd> pingCmds = new ArrayList<PingCmd>();
}
