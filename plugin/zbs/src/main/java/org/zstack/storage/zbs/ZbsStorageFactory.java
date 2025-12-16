package org.zstack.storage.zbs;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.ansible.SshFileMd5Checker;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.Q;
import org.zstack.core.trash.StorageTrash;
import org.zstack.externalStorage.primary.ExternalStorageFencerType;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.storage.addon.primary.*;
import org.zstack.header.storage.primary.DeleteVolumeBitsOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.storage.snapshot.VolumeSnapshotAfterDeleteExtensionPoint;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.volume.VolumeProtocol;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.utils.ShellResult;
import org.zstack.utils.ShellUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.ssh.Ssh;
import org.zstack.utils.ssh.SshResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.zstack.storage.zbs.ZbsHelper.*;

import static org.zstack.core.Platform.operr;

/**
 * @author Xingwei Yu
 * @date 2024/3/21 11:56
 */
public class ZbsStorageFactory implements ExternalPrimaryStorageSvcBuilder, BackupStorageSelector, VolumeSnapshotAfterDeleteExtensionPoint {
    private static CLogger logger = Utils.getLogger(ZbsStorageFactory.class);
    public static final ExternalStorageFencerType fencerType = new ExternalStorageFencerType(ZbsConstants.IDENTITY, VolumeProtocol.CBD.toString());

    @Autowired
    private CloudBus bus;
    @Autowired
    private StorageTrash trash;

    private List<String> preferBackupStorageTypes;

    @Override
    public PrimaryStorageControllerSvc buildControllerSvc(ExternalPrimaryStorageVO vo) {
        return new ZbsStorageController(vo);
    }

    @Override
    public PrimaryStorageNodeSvc buildNodeSvc(ExternalPrimaryStorageVO vo) {
        return new ZbsStorageController(vo);
    }

    @Override
    public void discover(String url, String config, ReturnValueCompletion<LinkedHashMap> completion) {
        AddonInfo addonInfo = new AddonInfo();

        Config conf = JSONObjectUtil.toObject(config, Config.class);

        if (CollectionUtils.isEmpty(conf.getMdsUrls())) {
            completion.fail(operr("mdsUrls cannot be null or empty"));
            return;
        }

        String errInfo = "";
        for (MdsInfo mdsInfo : MdsInfo.valueOf(conf.getMdsUrls())) {
            Ssh ssh = new Ssh();
            ssh.setUsername(mdsInfo.getUsername())
                    .setPassword(mdsInfo.getPassword()).setPort(mdsInfo.getPort())
                    .setHostname(mdsInfo.getAddr())
                    .setTimeout(5);
            try {
                ssh.sudoCommand("/usr/bin/zbs list logical-pool --format json");
                SshResult ret = ssh.run();
                if (ret.getReturnCode() != 0) {
                    errInfo += String.format("failed to list logical pools from MDS[%s], because %s\n", mdsInfo.getAddr(), ret.getStderr());
                    continue;
                }

                ssh.reset();

                String poolStr =  ret.getStdout();
                ZbsListPoolResult result = JSONObjectUtil.toObject(poolStr, ZbsListPoolResult.class);
                if (!result.isSuccess()) {
                    errInfo += String.format("failed to list logical pools from MDS[%s], because %s\n", mdsInfo.getAddr(), result.getError().getMessage());
                    continue;
                }

                for (ZbsListPoolResult.Result poolRet : result.getResult()) {
                    if (poolRet.getStatusCode() == 0) {
                        poolRet.getLogicalPoolInfos().forEach(it ->
                                addonInfo.addLogicalPoolInfo(LogicalPoolInfo.valueOf(it)));
                    }
                }
                completion.success(JSONObjectUtil.rehashObject(addonInfo, LinkedHashMap.class));
                return;
            } finally {
                ssh.close();
            }
        }

        completion.fail(operr("unable to discover logical pools from all MDSs, details: %s", errInfo));
    }

    public void setPreferBackupStorageTypes(List<String> preferBackupStorageTypes) {
        this.preferBackupStorageTypes = preferBackupStorageTypes;
    }

    @Override
    public List<String> getPreferBackupStorageTypes() {
        return preferBackupStorageTypes;
    }

    @Override
    public String getIdentity() {
        return ZbsConstants.IDENTITY;
    }

    @Override
    public void volumeSnapshotAfterDeleteExtensionPoint(VolumeSnapshotInventory snapshot, NoErrorCompletion completion) {
        completion.done();
    }

    private boolean isCbdProtocol(String volumeUuid) {
        return Q.New(VolumeVO.class).eq(VolumeVO_.protocol, VolumeProtocol.CBD.toString()).eq(VolumeVO_.uuid, volumeUuid).isExists();
    }

    // TODO: remove it, do not use extension point.
    @Override
    public void volumeSnapshotAfterCleanUpExtensionPoint(String volumeUuid, List<VolumeSnapshotInventory> snapshots) {
        if (CollectionUtils.isEmpty(snapshots) || !isCbdProtocol(volumeUuid)) {
            return;
        }

        Set<String> volumeInstallPaths = snapshots.stream().map(s -> getVolumeFromSnapshotPath(s.getPrimaryStorageInstallPath()))
                .collect(Collectors.toSet());
        if (volumeInstallPaths.isEmpty()) {
            return;
        }

        volumeInstallPaths.forEach(volumeInstallPath -> {
            String details = trash.makeSureInstallPathNotUsed(volumeInstallPath, VolumeVO.class.getSimpleName());

            if (StringUtils.isBlank(details)) {
                logger.debug(String.format("delete volume[InstallPath:%s] after cleaning up snapshots", volumeInstallPath));
                DeleteVolumeBitsOnPrimaryStorageMsg msg = new DeleteVolumeBitsOnPrimaryStorageMsg();
                msg.setPrimaryStorageUuid(snapshots.get(0).getPrimaryStorageUuid());
                msg.setInstallPath(volumeInstallPath);
                bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, snapshots.get(0).getPrimaryStorageUuid());
                bus.send(msg);
            }
        });
    }
}
