package org.zstack.storage.backup.sftp;

import org.zstack.core.db.Q;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.image.APICreateRootVolumeTemplateFromRootVolumeMsg;
import org.zstack.header.message.APIMessage;
import org.zstack.header.query.QueryCondition;
import org.zstack.header.query.QueryOp;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.backup.BackupStorageVO_;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.utils.network.IPv6NetworkUtils;

import java.util.Arrays;
import java.util.List;

import static java.util.Arrays.asList;
import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.*;

/**
 */
public class SftpBackupStorageApiInterceptor implements ApiMessageInterceptor, GlobalApiMessageInterceptor {
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
            validateEndpointNotDuplicated(msg.getUuid(), msg.getHostname(), ORG_ZSTACK_STORAGE_BACKUP_SFTP_10028);
        }
        if (msg.getIpv6Endpoint() != null) {
            validateIpv6Endpoint(msg.getIpv6Endpoint(), ORG_ZSTACK_STORAGE_BACKUP_SFTP_10028);
            validateEndpointNotDuplicated(msg.getUuid(), msg.getIpv6Endpoint(), ORG_ZSTACK_STORAGE_BACKUP_SFTP_10028);
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
        if (msg.getIpv6Endpoint() != null) {
            validateIpv6Endpoint(msg.getIpv6Endpoint(), ORG_ZSTACK_STORAGE_BACKUP_SFTP_10029);
        }

        validateEndpointNotDuplicated(null, msg.getHostname(), ORG_ZSTACK_STORAGE_BACKUP_SFTP_10023);
        if (msg.getIpv6Endpoint() != null) {
            validateEndpointNotDuplicated(null, msg.getIpv6Endpoint(), ORG_ZSTACK_STORAGE_BACKUP_SFTP_10023);
        }
        String dir = msg.getUrl();
        if (dir.startsWith("/proc")||dir.startsWith("/dev") || dir.startsWith("/sys")) {
            throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_STORAGE_BACKUP_SFTP_10024, " the url contains an invalid folder[/dev or /proc or /sys]"));
        }
    }

    private void validateHostname(String hostname, String errorCode) {
        if (!IPv6NetworkUtils.isValidManagementEndpoint(hostname)) {
            throw new ApiMessageInterceptionException(argerr(errorCode,
                    "hostname[%s] is not a valid IPv4 address, IPv6 address, or hostname", hostname));
        }
    }

    private void validateIpv6Endpoint(String endpoint, String errorCode) {
        if (!IPv6NetworkUtils.isValidManagementIpv6Address(endpoint)) {
            throw new ApiMessageInterceptionException(argerr(errorCode,
                    "ipv6Endpoint[%s] is not a valid remote IPv6 address", endpoint));
        }
    }

    private void validateEndpointNotDuplicated(String selfUuid, String endpoint, String errorCode) {
        if (isEndpointDuplicated(selfUuid, endpoint, true) || isEndpointDuplicated(selfUuid, endpoint, false)) {
            throw new ApiMessageInterceptionException(operr(errorCode,
                    "duplicate backup storage endpoint. There has been a sftp backup storage endpoint[%s] existing", endpoint));
        }
    }

    private boolean isEndpointDuplicated(String selfUuid, String endpoint, boolean hostnameColumn) {
        Q q = Q.New(SftpBackupStorageVO.class)
                .eq(hostnameColumn ? SftpBackupStorageVO_.hostname : SftpBackupStorageVO_.ipv6Endpoint, endpoint);
        if (selfUuid != null) {
            q.notEq(SftpBackupStorageVO_.uuid, selfUuid);
        }
        return q.isExists();
    }

    @Override
    public List<Class> getMessageClassToIntercept() {
        return asList(APICreateRootVolumeTemplateFromRootVolumeMsg.class);
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.FRONT;
    }
}
