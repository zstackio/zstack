package org.zstack.tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.APIMessage;
import org.zstack.header.tag.*;

/**
 */
public class TagApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private TagManager tagMgr;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIDeleteTagMsg) {
            validate((APIDeleteTagMsg) msg);
        } else if (msg instanceof APICreateTagMsg) {
            validate((APICreateTagMsg) msg);
        }

        return msg;
    }

    private void validate(APICreateTagMsg msg) {
        if (!tagMgr.getManagedEntityNames().contains(msg.getResourceType())) {
            throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                    String.format("no resource type[%s] found in tag system", msg.getResourceType())
            ));
        }

        if (msg instanceof APICreateSystemTagMsg) {
            try {
                tagMgr.validateSystemTag(msg.getResourceUuid(), msg.getResourceType(), msg.getTag());
            } catch (OperationFailureException oe) {
                throw new ApiMessageInterceptionException(oe.getErrorCode());
            }
        }
    }

    private void validate(APIDeleteTagMsg msg) {
        SimpleQuery<SystemTagVO> q = dbf.createQuery(SystemTagVO.class);
        q.add(SystemTagVO_.uuid, Op.EQ, msg.getUuid());
        q.add(SystemTagVO_.inherent, Op.EQ, true);
        if (q.isExists()) {
            throw new ApiMessageInterceptionException(errf.stringToOperationError(
                    String.format("tag[uuid:%s] is an inherent system tag, can not be removed", msg.getUuid())
            ));
        }
    }
}
