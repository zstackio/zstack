package org.zstack.storage.backup;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.StopRoutingException;
import org.zstack.header.message.APIMessage;
import org.zstack.header.storage.backup.*;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 4:21 PM
 * To change this template use File | Settings | File Templates.
 */
public class BackupStorageApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private DatabaseFacade dbf;

    private void setServiceId(APIMessage msg) {
        if (msg instanceof BackupStorageMessage) {
            BackupStorageMessage bmsg = (BackupStorageMessage)msg;
            bus.makeTargetServiceIdByResourceUuid(msg, BackupStorageConstant.SERVICE_ID, bmsg.getBackupStorageUuid());
        }
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIAttachBackupStorageToZoneMsg) {
            validate((APIAttachBackupStorageToZoneMsg) msg);
        } else if (msg instanceof APIDetachBackupStorageFromZoneMsg) {
            validate((APIDetachBackupStorageFromZoneMsg) msg);
        } else if (msg instanceof APIDeleteBackupStorageMsg) {
            validate((APIDeleteBackupStorageMsg) msg);
        } else if (msg instanceof APIExportImageFromBackupStorageMsg) {
            validate((APIExportImageFromBackupStorageMsg) msg);
        } else if (msg instanceof APIDeleteExportedImageFromBackupStorageMsg) {
            validate((APIDeleteExportedImageFromBackupStorageMsg) msg);
        } else if (msg instanceof APIGetBackupStorageCapacityMsg) {
            validate((APIGetBackupStorageCapacityMsg) msg);
        }

        setServiceId(msg);
        return msg;
    }

    private void checkNull(final String name, final String val) {
        if (val == null) {
            throw new ApiMessageInterceptionException(argerr("%s should not be null", name));
        }
    }
    private void validate(APIDeleteExportedImageFromBackupStorageMsg msg) {
        checkNull("backup storage uuid", msg.getBackupStorageUuid());
        checkNull("image uuid", msg.getImageUuid());
    }

    private void validate(APIExportImageFromBackupStorageMsg msg) {
        checkNull("backup storage uuid", msg.getBackupStorageUuid());
        checkNull("image uuid", msg.getImageUuid());
    }

    private void validate(APIGetBackupStorageCapacityMsg msg) {
        boolean pass = false;
        if (msg.getBackupStorageUuids() != null && !msg.getBackupStorageUuids().isEmpty()) {
            pass = true;
        }
        if (msg.getZoneUuids() != null && !msg.getZoneUuids().isEmpty()) {
            pass = true;
        }

        if (!pass && !msg.isAll()) {
            throw new ApiMessageInterceptionException(argerr("zoneUuids, backupStorageUuids must have at least one be none-empty list, or all is set to true"));
        }

        if (msg.isAll() && (msg.getBackupStorageUuids() == null || msg.getBackupStorageUuids().isEmpty())) {
            SimpleQuery<BackupStorageVO> q = dbf.createQuery(BackupStorageVO.class);
            q.select(BackupStorageVO_.uuid);
            List<String> bsUuids = q.listValue();
            msg.setBackupStorageUuids(bsUuids);

            if (msg.getBackupStorageUuids().isEmpty()) {
                APIGetBackupStorageCapacityReply reply = new APIGetBackupStorageCapacityReply();
                bus.reply(msg, reply);
                throw new StopRoutingException();
            }
        }
    }

    private void validate(APIDeleteBackupStorageMsg msg) {
        if (!dbf.isExist(msg.getUuid(), BackupStorageVO.class)) {
            APIDeleteBackupStorageEvent evt = new APIDeleteBackupStorageEvent(msg.getId());
            bus.publish(evt);
            throw new StopRoutingException();
        }
    }

    private void validate(APIDetachBackupStorageFromZoneMsg msg) {
        SimpleQuery<BackupStorageZoneRefVO> q = dbf.createQuery(BackupStorageZoneRefVO.class);
        q.add(BackupStorageZoneRefVO_.backupStorageUuid, Op.EQ, msg.getBackupStorageUuid());
        q.add(BackupStorageZoneRefVO_.zoneUuid, Op.EQ, msg.getZoneUuid());
        if (!q.isExists()) {
            throw new ApiMessageInterceptionException(operr("backup storage[uuid:%s] has not been attached to zone[uuid:%s]", msg.getBackupStorageUuid(), msg.getZoneUuid()));
        }
    }

    private void validate(APIAttachBackupStorageToZoneMsg msg) {
        SimpleQuery<BackupStorageZoneRefVO> q = dbf.createQuery(BackupStorageZoneRefVO.class);
        q.add(BackupStorageZoneRefVO_.backupStorageUuid, Op.EQ, msg.getBackupStorageUuid());
        q.add(BackupStorageZoneRefVO_.zoneUuid, Op.EQ, msg.getZoneUuid());
        if (q.isExists()) {
            throw new ApiMessageInterceptionException(operr("backup storage[uuid:%s] has been attached to zone[uuid:%s]", msg.getBackupStorageUuid(), msg.getZoneUuid()));
        }
    }
}
