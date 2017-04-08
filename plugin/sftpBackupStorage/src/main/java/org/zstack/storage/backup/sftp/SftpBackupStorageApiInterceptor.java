package org.zstack.storage.backup.sftp;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.message.APIMessage;
import org.zstack.header.query.QueryCondition;
import org.zstack.header.query.QueryOp;
import org.zstack.utils.network.NetworkUtils;
import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;

/**
 */
public class SftpBackupStorageApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIAddSftpBackupStorageMsg) {
            validate((APIAddSftpBackupStorageMsg)msg);
        } else if (msg instanceof APIQuerySftpBackupStorageMsg) {
            validate((APIQuerySftpBackupStorageMsg)msg);
        } else if (msg instanceof APIUpdateSftpBackupStorageMsg) {
            validate((APIUpdateSftpBackupStorageMsg) msg);
        }

        return msg;
    }

    private void validate(APIUpdateSftpBackupStorageMsg msg) {
        if (msg.getHostname() != null && !NetworkUtils.isIpv4Address(msg.getHostname()) && !NetworkUtils.isHostname(msg.getHostname())) {
            throw new ApiMessageInterceptionException(argerr("hostname[%s] is neither an IPv4 address nor a valid hostname", msg.getHostname()));
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
        if (!NetworkUtils.isIpv4Address(msg.getHostname()) && !NetworkUtils.isHostname(msg.getHostname())) {
            throw new ApiMessageInterceptionException(argerr("hostname[%s] is neither an IPv4 address nor a valid hostname", msg.getHostname()));
        }

        SimpleQuery<SftpBackupStorageVO> q = dbf.createQuery(SftpBackupStorageVO.class);
        q.add(SftpBackupStorageVO_.hostname, Op.EQ, msg.getHostname());
        if (q.isExists()) {
            throw new ApiMessageInterceptionException(operr("duplicate backup storage. There has been a sftp backup storage[hostname:%s] existing", msg.getHostname()));
        }
        String dir = msg.getUrl();
        if (dir.startsWith("/proc")||dir.startsWith("/dev") || dir.startsWith("/sys")) {
            throw new ApiMessageInterceptionException(argerr(" the url contains an invalid folder[/dev or /proc or /sys]"));
        }
    }
}
