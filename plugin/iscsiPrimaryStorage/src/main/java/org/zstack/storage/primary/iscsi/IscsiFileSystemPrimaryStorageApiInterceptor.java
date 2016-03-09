package org.zstack.storage.primary.iscsi;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.message.APIMessage;

/**
 * Created by frank on 4/27/2015.
 */
public class IscsiFileSystemPrimaryStorageApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIAddIscsiFileSystemBackendPrimaryStorageMsg) {
            validate((APIAddIscsiFileSystemBackendPrimaryStorageMsg)msg);
        }
        return msg;
    }

    private void validate(APIAddIscsiFileSystemBackendPrimaryStorageMsg msg) {
        if (msg.getFilesystemType() == null) {
            msg.setFilesystemType(IscsiBtrfsPrimaryStorageConstants.BTRFS_TYPE);
        }

        SimpleQuery<IscsiFileSystemBackendPrimaryStorageVO> q = dbf.createQuery(IscsiFileSystemBackendPrimaryStorageVO.class);
        q.add(IscsiFileSystemBackendPrimaryStorageVO_.hostname, Op.EQ, msg.getHostname());
        if (q.isExists()) {
            throw new ApiMessageInterceptionException(
                    errf.stringToInvalidArgumentError(String.format("there is already a IscsiFileSystemBackendPrimaryStorageV with hostname[%s]", msg.getHostname()))
            );
        }

        if ((msg.getChapUsername() == null && msg.getChapPassword() != null) ||
                (msg.getChapUsername() != null && msg.getChapPassword() == null)) {
            throw new ApiMessageInterceptionException(
                    errf.stringToInvalidArgumentError("chapUsername and chapPassword must be all null or all not null")
            );
        }

        if (!IscsiBtrfsPrimaryStorageConstants.BTRFS_TYPE.equals(msg.getFilesystemType())) {
            throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                    String.format("in this version, the filesystemType must be %s", IscsiBtrfsPrimaryStorageConstants.BTRFS_TYPE)
            ));
        }
    }
}
