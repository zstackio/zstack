package org.zstack.storage.primary.nfs; /**
 * Created by xing5 on 2016/8/19.
 */

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class NfsApiParamChecker {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;

    public ErrorCode checkUrl(String zoneUuid, String url) {
        SimpleQuery<PrimaryStorageVO> q = dbf.createQuery(PrimaryStorageVO.class);
        q.add(PrimaryStorageVO_.type, Op.EQ, NfsPrimaryStorageConstant.NFS_PRIMARY_STORAGE_TYPE);
        q.add(PrimaryStorageVO_.url, Op.EQ, url);
        q.add(PrimaryStorageVO_.zoneUuid, Op.EQ, zoneUuid);
        if (q.isExists()) {
            return errf.instantiateErrorCode(SysErrors.INVALID_ARGUMENT_ERROR,
                    String.format("there has been a nfs primary storage having url as %s in zone[uuid:%s]", url, zoneUuid)
            );
        } else {
            return null;
        }
    }
}
