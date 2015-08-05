package org.zstack.storage.ceph.primary;

import org.zstack.storage.ceph.primary.CephPrimaryStorageBase.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by frank on 7/28/2015.
 */
public class CephPrimaryStorageSimulatorConfig {
    public static class CephPrimaryStorageConfig {
        public long totalCapacity;
        public long availCapacity;
        public String fsid;
    }

    public Map<String, CephPrimaryStorageConfig> config = new HashMap<String, CephPrimaryStorageConfig>();
    public List<CreateEmptyVolumeCmd> createEmptyVolumeCmds = new ArrayList<CreateEmptyVolumeCmd>();
    public List<DeleteCmd> deleteCmds = new ArrayList<DeleteCmd>();
    public List<CreateSnapshotCmd> createSnapshotCmds = new ArrayList<CreateSnapshotCmd>();
    public List<DeleteSnapshotCmd> deleteSnapshotCmds = new ArrayList<DeleteSnapshotCmd>();
    public List<ProtectSnapshotCmd> protectSnapshotCmds = new ArrayList<ProtectSnapshotCmd>();
    public List<UnprotectedSnapshotCmd> unprotectedSnapshotCmds = new ArrayList<UnprotectedSnapshotCmd>();
    public List<CloneCmd> cloneCmds = new ArrayList<CloneCmd>();
    public List<FlattenCmd> flattenCmds = new ArrayList<FlattenCmd>();
    public List<CpCmd> cpCmds = new ArrayList<CpCmd>();
    public List<SftpDownloadCmd> sftpDownloadCmds = new ArrayList<SftpDownloadCmd>();
    public List<SftpUpLoadCmd> sftpUpLoadCmds = new ArrayList<SftpUpLoadCmd>();
    public List<RollbackSnapshotCmd> rollbackSnapshotCmds = new ArrayList<RollbackSnapshotCmd>();
}
