package org.zstack.acl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cascade.AbstractAsyncCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.acl.AccessControlListConstants;
import org.zstack.header.acl.AccessControlListInventory;
import org.zstack.header.acl.AccessControlListVO;
import org.zstack.header.acl.DeleteAccessControlListMsg;
import org.zstack.header.core.Completion;
import org.zstack.header.identity.AccessLevel;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.AccountVO;
import org.zstack.header.message.MessageReply;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

public class AccessControlListCascadeExtension extends AbstractAsyncCascadeExtension {
    private static final CLogger logger = Utils.getLogger(AccessControlListCascadeExtension.class);
    private static final String NAME = AccessControlListVO.class.getSimpleName();

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Override
    public void asyncCascade(CascadeAction action, Completion completion) {
        if (action.isActionCode(CascadeConstant.DELETION_CHECK_CODE)) {
            handleDeletionCheck(action, completion);
        } else if (action.isActionCode(CascadeConstant.DELETION_DELETE_CODE, CascadeConstant.DELETION_FORCE_DELETE_CODE)) {
            handleDeletion(action, completion);
        } else if (action.isActionCode(CascadeConstant.DELETION_CLEANUP_CODE)) {
            handleDeletionCleanup(action, completion);
        } else {
            completion.success();
        }
    }

    private void handleDeletionCheck(CascadeAction action, Completion completion) {
        completion.success();
    }

    private void handleDeletion(CascadeAction action, Completion completion) {
        final List<AccessControlListInventory> acls = aclFromAction(action);
        if (acls == null || acls.isEmpty()) {
            completion.success();
            return;
        }

        List<DeleteAccessControlListMsg> msgs = CollectionUtils.transformAndRemoveNull(acls, arg -> {
            DeleteAccessControlListMsg msg = new DeleteAccessControlListMsg();
            msg.setUuid(arg.getUuid());
            bus.makeTargetServiceIdByResourceUuid(msg, AccessControlListConstants.SERVICE_ID, msg.getUuid());
            return msg;
        });

        bus.send(msgs, 10, new CloudBusListCallBack(completion) {
            @Override
            public void run(List<MessageReply> replies) {
                for (MessageReply r : replies) {
                    AccessControlListInventory acl = acls.get(replies.indexOf(r));
                    if (!r.isSuccess()) {
                        logger.warn(String.format("failed to delete acl[uuid:%s], %s",
                                acl.getUuid() , r.getError()));
                        completion.fail(r.getError());
                        return;
                    }
                }

                completion.success();
            }
        });
    }

    private void handleDeletionCleanup(CascadeAction action, Completion completion) {
        dbf.eoCleanup(AccessControlListVO.class);
        completion.success();
    }

    @Override
    public List<String> getEdgeNames() {
        return Arrays.asList(AccountVO.class.getSimpleName());
    }

    @Override
    public String getCascadeResourceName() {
        return NAME;
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        if (CascadeConstant.DELETION_CODES.contains(action.getActionCode())) {
            List<AccessControlListInventory> acls = aclFromAction(action);
            if (acls != null) {
                return action.copy().setParentIssuer(NAME).setParentIssuerContext(acls);
            }
        }
        return null;
    }

    private List<AccessControlListInventory> aclFromAction(CascadeAction action) {
        if (AccountVO.class.getSimpleName().equals(action.getParentIssuer())) {
            final List<AccountInventory> parentIssuerContext = action.getParentIssuerContext();
            final List<String> auuids = CollectionUtils.transformAndRemoveNull(parentIssuerContext, AccountInventory::getUuid);

            List<AccessControlListVO> vos = new Callable<List<AccessControlListVO>>() {
                @Override
                @Transactional(readOnly = true)
                public List<AccessControlListVO> call() {
                    String sql = "select d from AccessControlListVO d, AccountResourceRefVO r where d.uuid = r.resourceUuid and" +
                            " r.resourceType = :rtype and r.type = :type and r.accountUuid in (:auuids)";

                    TypedQuery<AccessControlListVO> q = dbf.getEntityManager().createQuery(sql, AccessControlListVO.class);
                    q.setParameter("auuids", auuids);
                    q.setParameter("rtype", AccessControlListVO.class.getSimpleName());
                    q.setParameter("type", AccessLevel.Own);
                    return q.getResultList();
                }
            }.call();

            if (!vos.isEmpty()) {
                return AccessControlListInventory.valueOf(vos);
            }
        }
        return null;
    }
}
