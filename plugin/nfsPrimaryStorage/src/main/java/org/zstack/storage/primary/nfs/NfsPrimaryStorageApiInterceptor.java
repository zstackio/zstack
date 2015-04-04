package org.zstack.storage.primary.nfs;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.message.APIMessage;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;

/**
 */
public class NfsPrimaryStorageApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIAddNfsPrimaryStorageMsg) {
            validate((APIAddNfsPrimaryStorageMsg) msg);
        }

        return msg;
    }

    private void validate(APIAddNfsPrimaryStorageMsg msg) {
        SimpleQuery<PrimaryStorageVO> q = dbf.createQuery(PrimaryStorageVO.class);
        q.add(PrimaryStorageVO_.type, Op.EQ, NfsPrimaryStorageConstant.NFS_PRIMARY_STORAGE_TYPE);
        q.add(PrimaryStorageVO_.url, Op.EQ, msg.getUrl());
        q.add(PrimaryStorageVO_.zoneUuid, Op.EQ, msg.getZoneUuid());
        if (q.isExists()) {
            throw new ApiMessageInterceptionException(errf.instantiateErrorCode(SysErrors.INVALID_ARGUMENT_ERROR,
                    String.format("there has been a nfs primary storage having url as %s in zone[uuid:%s]", msg.getUrl(), msg.getZoneUuid())
            ));
        }
    }
}
