package org.zstack.tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.*;
import org.zstack.header.message.APIMessage;
import org.zstack.header.tag.*;
import org.zstack.identity.QuotaUtil;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;

import javax.persistence.TypedQuery;

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
        } else if (msg instanceof APIUpdateSystemTagMsg) {
            validate((APIUpdateSystemTagMsg) msg);
        }

        return msg;
    }

    private void validate(APIUpdateSystemTagMsg msg) {
        SystemTagVO vo = dbf.findByUuid(msg.getUuid(), SystemTagVO.class);
        try {
            tagMgr.validateSystemTag(vo.getResourceUuid(), vo.getResourceType(), msg.getTag());
        } catch (OperationFailureException oe) {
            throw new ApiMessageInterceptionException(oe.getErrorCode());
        }
    }


    private void validate(APICreateTagMsg msg) {
        if (!tagMgr.getManagedEntityNames().contains(msg.getResourceType())) {
            throw new ApiMessageInterceptionException(argerr("no resource type[%s] found in tag system", msg.getResourceType()));
        }

        if (msg instanceof APICreateSystemTagMsg) {
            try {
                tagMgr.validateSystemTag(msg.getResourceUuid(), msg.getResourceType(), msg.getTag());
            } catch (OperationFailureException oe) {
                throw new ApiMessageInterceptionException(oe.getErrorCode());
            }
            checkIfResourceHasThisTagType(msg.getResourceUuid(), msg.getResourceType());
        }
    }

    private void validate(APIDeleteTagMsg msg) {
        SimpleQuery<SystemTagVO> q = dbf.createQuery(SystemTagVO.class);
        q.add(SystemTagVO_.uuid, Op.EQ, msg.getUuid());
        q.add(SystemTagVO_.inherent, Op.EQ, true);
        if (q.isExists()) {
            throw new ApiMessageInterceptionException(operr("tag[uuid:%s] is an inherent system tag, can not be removed", msg.getUuid()));
        }

        boolean userTag = dbf.isExist(msg.getUuid(), UserTagVO.class);
        boolean sysTag = dbf.isExist(msg.getUuid(), SystemTagVO.class);
        if (!new QuotaUtil().isAdminAccount(msg.getSession().getAccountUuid())) {
            if (userTag) {
                checkAccountForUserTag(msg);
            } else if (sysTag) {
                checkAccountForSystemTag(msg);
            }
        }
    }

    @Transactional(readOnly = true)
    private void checkIfResourceHasThisTagType(String resourceUuid, String resourceType) {
        String sql = String.format("select count(vo.uuid) from %s vo where " +
                " vo.uuid = :resourceUuid", resourceType);
        TypedQuery<Long> q = dbf.getEntityManager().createQuery(sql, Long.class);
        q.setParameter("resourceUuid", resourceUuid);

        Long size = q.getSingleResult();
        if (size <= 0) {
            throw new ApiMessageInterceptionException(argerr("The argument :'resourceType' doesn't match uuid"));
        }

    }

    @Transactional(readOnly = true)
    private void checkAccountForSystemTag(APIDeleteTagMsg msg) {
        String sql = "select ref.accountUuid" +
                " from SystemTagVO tag, AccountResourceRefVO ref" +
                " where tag.resourceUuid = ref.resourceUuid" +
                " and tag.uuid = :tuuid";
        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("tuuid", msg.getUuid());
        String accountUuid = q.getSingleResult();
        if (!msg.getSession().getAccountUuid().equals(accountUuid)) {
            throw new ApiMessageInterceptionException(errf.instantiateErrorCode(IdentityErrors.PERMISSION_DENIED,
                    String.format("permission denied. The system tag[uuid: %s] refer to a resource not belonging to the account[uuid: %s]",
                            msg.getUuid(), msg.getSession().getAccountUuid())
            ));
        }
    }

    @Transactional(readOnly = true)
    private void checkAccountForUserTag(APIDeleteTagMsg msg) {
        String sql = "select ref.accountUuid from UserTagVO tag, AccountResourceRefVO ref where tag.resourceUuid = ref.resourceUuid and tag.uuid = :tuuid";
        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("tuuid", msg.getUuid());
        String accountUuid = q.getSingleResult();
        if (!msg.getSession().getAccountUuid().equals(accountUuid)) {
            throw new ApiMessageInterceptionException(errf.instantiateErrorCode(IdentityErrors.PERMISSION_DENIED,
                    String.format("permission denied. The user tag[uuid: %s] refer to a resource not belonging to the account[uuid: %s]",
                            msg.getUuid(), msg.getSession().getAccountUuid())
            ));
        }
    }
}
