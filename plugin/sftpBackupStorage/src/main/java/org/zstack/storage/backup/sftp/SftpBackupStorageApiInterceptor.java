package org.zstack.storage.backup.sftp;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.image.APICreateRootVolumeTemplateFromRootVolumeMsg;
import org.zstack.header.message.APIMessage;
import org.zstack.header.query.QueryCondition;
import org.zstack.header.query.QueryOp;
import org.zstack.header.storage.backup.APIAttachBackupStorageToZoneMsg;
import org.zstack.header.storage.backup.BackupStorageZoneRefVO;
import org.zstack.header.storage.backup.BackupStorageZoneRefVO_;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.backup.BackupStorageVO_;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.zone.ManagementNetworkIpVersionManager;
import org.zstack.header.zone.ManagementNetworkIpVersionResourceExtensionPoint;
import org.zstack.utils.network.IPv6NetworkUtils;

import java.util.Arrays;
import java.util.List;

import static java.util.Arrays.asList;
import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.*;

/**
 */
public class SftpBackupStorageApiInterceptor implements ApiMessageInterceptor, GlobalApiMessageInterceptor,
        ManagementNetworkIpVersionResourceExtensionPoint {
    private static final String SFTP_BACKUP_STORAGE_RESOURCE_TYPE = "sftp backup storage";

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private ManagementNetworkIpVersionManager managementNetworkIpVersionManager;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIAddSftpBackupStorageMsg) {
            validate((APIAddSftpBackupStorageMsg)msg);
        } else if (msg instanceof APIQuerySftpBackupStorageMsg) {
            validate((APIQuerySftpBackupStorageMsg)msg);
        } else if (msg instanceof APIUpdateSftpBackupStorageMsg) {
            validate((APIUpdateSftpBackupStorageMsg) msg);
        } else if (msg instanceof APICreateRootVolumeTemplateFromRootVolumeMsg) {
            validate((APICreateRootVolumeTemplateFromRootVolumeMsg) msg);
        } else if (msg instanceof APIAttachBackupStorageToZoneMsg) {
            validate((APIAttachBackupStorageToZoneMsg) msg);
        }

        return msg;
    }

    private void validate(APICreateRootVolumeTemplateFromRootVolumeMsg msg) {
        if (msg.getBackupStorageUuids() == null || msg.getBackupStorageUuids().isEmpty()) {
            return;
        }

        // if vm in running or pause
        if (Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.rootVolumeUuid, msg.getRootVolumeUuid())
                .in(VmInstanceVO_.state, Arrays.asList(VmInstanceState.Running, VmInstanceState.Paused)).isExists()) {
            for (String bsUuid : msg.getBackupStorageUuids()) {
                String bsType = Q.New(BackupStorageVO.class)
                        .eq(BackupStorageVO_.uuid, bsUuid)
                        .select(BackupStorageVO_.type).findValue();

                if (bsType.equals(SftpBackupStorageConstant.SFTP_BACKUP_STORAGE_TYPE)) {
                    throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_STORAGE_BACKUP_SFTP_10020, "Please stop the vm before create volume template to sftp backup storage %s", bsUuid));
                }
            }
        }
    }

    private void validate(APIUpdateSftpBackupStorageMsg msg) {
        if (msg.getHostname() != null) {
            validateHostname(msg.getHostname(), ORG_ZSTACK_STORAGE_BACKUP_SFTP_10028);
            validateUpdatedHostnameWithAttachedZones(msg);
        }
    }

    private void validate(APIQuerySftpBackupStorageMsg msg) {
        boolean found = false;
        for (QueryCondition qcond : msg.getConditions()) {
            if ("type".equals(qcond.getName())) {
                qcond.setOp(QueryOp.EQ.toString());
                qcond.setValue(SftpBackupStorageConstant.SFTP_BACKUP_STORAGE_TYPE);
                found = true;
                break;
            }
        }

        if (!found) {
            msg.addQueryCondition("type", QueryOp.EQ, SftpBackupStorageConstant.SFTP_BACKUP_STORAGE_TYPE);
        }
    }

    private void validate(APIAddSftpBackupStorageMsg msg) {
        validateHostname(msg.getHostname(), ORG_ZSTACK_STORAGE_BACKUP_SFTP_10029);

        SimpleQuery<SftpBackupStorageVO> q = dbf.createQuery(SftpBackupStorageVO.class);
        q.add(SftpBackupStorageVO_.hostname, Op.EQ, msg.getHostname());
        if (q.isExists()) {
            throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_STORAGE_BACKUP_SFTP_10023, "duplicate backup storage. There has been a sftp backup storage[hostname:%s] existing", msg.getHostname()));
        }
        String dir = msg.getUrl();
        if (dir.startsWith("/proc")||dir.startsWith("/dev") || dir.startsWith("/sys")) {
            throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_STORAGE_BACKUP_SFTP_10024, " the url contains an invalid folder[/dev or /proc or /sys]"));
        }
    }

    private void validate(APIAttachBackupStorageToZoneMsg msg) {
        String hostname = Q.New(SftpBackupStorageVO.class)
                .select(SftpBackupStorageVO_.hostname)
                .eq(SftpBackupStorageVO_.uuid, msg.getBackupStorageUuid())
                .findValue();
        if (hostname == null) {
            return;
        }

        managementNetworkIpVersionManager.validateEndpointInZone(msg.getZoneUuid(), hostname,
                SFTP_BACKUP_STORAGE_RESOURCE_TYPE, msg.getBackupStorageUuid(), ORG_ZSTACK_STORAGE_BACKUP_SFTP_10025);
    }

    private void validateUpdatedHostnameWithAttachedZones(APIUpdateSftpBackupStorageMsg msg) {
        List<String> zoneUuids = Q.New(BackupStorageZoneRefVO.class)
                .select(BackupStorageZoneRefVO_.zoneUuid)
                .eq(BackupStorageZoneRefVO_.backupStorageUuid, msg.getUuid())
                .listValues();

        for (String zoneUuid : zoneUuids) {
            managementNetworkIpVersionManager.validateEndpointInZone(zoneUuid, msg.getHostname(),
                    SFTP_BACKUP_STORAGE_RESOURCE_TYPE, msg.getUuid(), ORG_ZSTACK_STORAGE_BACKUP_SFTP_10026);
        }
    }

    private void validateHostname(String hostname, String errorCode) {
        if (!IPv6NetworkUtils.isValidManagementEndpoint(hostname)) {
            throw new ApiMessageInterceptionException(argerr(errorCode,
                    "hostname[%s] is not a valid IPv4 address, IPv6 address, or hostname", hostname));
        }
    }

    @Override
    public void validateExistingResourcesInZone(String zoneUuid, String ipVersion) {
        List<String> backupStorageUuids = Q.New(BackupStorageZoneRefVO.class)
                .select(BackupStorageZoneRefVO_.backupStorageUuid)
                .eq(BackupStorageZoneRefVO_.zoneUuid, zoneUuid)
                .listValues();
        if (backupStorageUuids.isEmpty()) {
            return;
        }

        List<SftpBackupStorageVO> sftpBackupStorages = Q.New(SftpBackupStorageVO.class)
                .in(SftpBackupStorageVO_.uuid, backupStorageUuids)
                .list();

        for (SftpBackupStorageVO sftp : sftpBackupStorages) {
            managementNetworkIpVersionManager.validateEndpointMatchesIpVersion(zoneUuid, ipVersion, sftp.getHostname(),
                    SFTP_BACKUP_STORAGE_RESOURCE_TYPE, sftp.getUuid(), ORG_ZSTACK_STORAGE_BACKUP_SFTP_10027);
        }
    }

    @Override
    public List<Class> getMessageClassToIntercept() {
        return asList(APICreateRootVolumeTemplateFromRootVolumeMsg.class, APIAttachBackupStorageToZoneMsg.class);
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.FRONT;
    }
}
