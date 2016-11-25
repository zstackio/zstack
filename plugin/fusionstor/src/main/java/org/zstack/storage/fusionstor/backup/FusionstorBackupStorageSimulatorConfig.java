package org.zstack.storage.fusionstor.backup;

import org.zstack.storage.fusionstor.backup.FusionstorBackupStorageBase.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by frank on 7/28/2015.
 */
public class FusionstorBackupStorageSimulatorConfig {
    public static class FusionstorBackupStorageConfig {
        public long totalCapacity;
        public long availCapacity;
        public String fsid;
        public String name;
    }

    public volatile boolean monInitSuccess = true;
    public List<InitCmd> initCmds = new ArrayList<InitCmd>();
    public Map<String, FusionstorBackupStorageConfig> config = new HashMap<String, FusionstorBackupStorageConfig>();
    public List<DownloadCmd> downloadCmds = new ArrayList<DownloadCmd>();
    public List<DeleteCmd> deleteCmds = new ArrayList<DeleteCmd>();
    public List<PingCmd> pingCmds = new ArrayList<PingCmd>();
    public Map<String, Long> imageSize = new HashMap<String, Long>();
    public Map<String, Long> imageActualSize = new HashMap<String, Long>();

    public List<GetImageSizeCmd> getImageSizeCmds = new ArrayList<GetImageSizeCmd>();
    public Map<String, Long> getImageSizeCmdSize = new HashMap<String, Long>();
    public Map<String, Long> getImageSizeCmdActualSize = new HashMap<String, Long>();

    public Map<String, Boolean> pingCmdSuccess = new HashMap<String, Boolean>();
    public Map<String, Boolean> pingCmdOperationFailure = new HashMap<String, Boolean>();
    public List<GetFactsCmd> getFactsCmds = new ArrayList<GetFactsCmd>();
    public Map<String, String> getFactsCmdFsid = new HashMap<String, String>();
}
