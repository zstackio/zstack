package org.zstack.sdnController;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.AbstractAsyncCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.AccountVO;
import org.zstack.header.message.MessageReply;
import org.zstack.sdnController.header.SdnControllerConstant;
import org.zstack.sdnController.header.SdnControllerDeletionMsg;
import org.zstack.sdnController.header.SdnControllerInventory;
import org.zstack.sdnController.header.SdnControllerVO;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;


public class SdnControllerCascadeExtension extends AbstractAsyncCascadeExtension {
    private static final CLogger logger = Utils.getLogger(SdnControllerCascadeExtension.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;

    private static final String NAME = SdnControllerVO.class.getSimpleName();

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

    private void handleDeletionCleanup(CascadeAction action, Completion completion) {
        completion.success();
    }

    private void handleDeletion(final CascadeAction action, final Completion completion) {
        final List<SdnControllerInventory> sdnControllers = sdnControllerFromAction(action);
        if (sdnControllers == null) {
            completion.success();
            return;
        }

        List<SdnControllerDeletionMsg> msgs = new ArrayList<SdnControllerDeletionMsg>();
        for (SdnControllerInventory sdn : sdnControllers) {
            SdnControllerDeletionMsg msg = new SdnControllerDeletionMsg();
            msg.setForceDelete(action.isActionCode(CascadeConstant.DELETION_FORCE_DELETE_CODE));
            msg.setSdnControllerUuid(sdn.getUuid());
            bus.makeTargetServiceIdByResourceUuid(msg, SdnControllerConstant.SERVICE_ID, sdn.getUuid());
            msgs.add(msg);
        }

        new While<>(msgs).all((msg, wcomp) -> {
            bus.send(msg, new CloudBusCallBack(wcomp) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        logger.debug(String.format("delete sdn controller failed, %s",
                                reply.getError().getDetails()));
                    }
                    wcomp.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                dbf.removeByPrimaryKeys(sdnControllers.stream().map(SdnControllerInventory::getUuid).collect(Collectors.toList()), SdnControllerVO.class);
                completion.success();
            }
        });
    }

    private void handleDeletionCheck(CascadeAction action, Completion completion) {
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

    private List<SdnControllerInventory> sdnControllerFromAction(CascadeAction action) {
        List<SdnControllerInventory> ret = null;
        if (NAME.equals(action.getParentIssuer())) {
            ret = action.getParentIssuerContext();
        } else if (AccountVO.class.getSimpleName().equals(action.getParentIssuer())) {
            final List<String> auuids = CollectionUtils.transformToList((List<AccountInventory>) action.getParentIssuerContext(), new Function<String, AccountInventory>() {
                @Override
                public String call(AccountInventory arg) {
                    return arg.getUuid();
                }
            });

            List<SdnControllerVO> vos = new Callable<List<SdnControllerVO>>() {
                @Override
                @Transactional(readOnly = true)
                public List<SdnControllerVO> call() {
                    String sql = "select d from SdnControllerVO d, AccountResourceRefVO r where d.uuid = r.resourceUuid and" +
                            " r.resourceType = :rtype and r.accountUuid in (:auuids)";
                    TypedQuery<SdnControllerVO> q = dbf.getEntityManager().createQuery(sql, SdnControllerVO.class);
                    q.setParameter("auuids", auuids);
                    q.setParameter("rtype", SdnControllerVO.class.getSimpleName());
                    return q.getResultList();
                }
            }.call();

            if (!vos.isEmpty()) {
                ret = SdnControllerInventory.valueOf(vos);
            }
        }

        return ret;
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        if (CascadeConstant.DELETION_CODES.contains(action.getActionCode())) {
            List<SdnControllerInventory> ctx = sdnControllerFromAction(action);
            if (ctx != null) {
                return action.copy().setParentIssuer(NAME).setParentIssuerContext(ctx);
            }
        }

        return null;
    }
}
