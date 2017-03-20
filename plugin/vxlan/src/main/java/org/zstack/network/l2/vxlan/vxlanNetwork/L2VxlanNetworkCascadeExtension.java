package org.zstack.network.l2.vxlan.vxlanNetwork;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.AbstractAsyncCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.*;
import org.zstack.network.l2.L2NetworkExtensionPointEmitter;
import org.zstack.network.l2.vxlan.vxlanNetworkPool.L2VxlanNetworkPoolInventory;
import org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanNetworkPoolVO;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by weiwang on 16/03/2017.
 */
public class L2VxlanNetworkCascadeExtension extends AbstractAsyncCascadeExtension {
    private static final CLogger logger = Utils.getLogger(L2VxlanNetworkCascadeExtension.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private L2NetworkExtensionPointEmitter extpEmitter;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;

    private static final String NAME = VxlanNetworkVO.class.getSimpleName();

    @Override
    public String getCascadeResourceName() {
        return NAME;
    }

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

    @Override
    public List<String> getEdgeNames() {
        return Arrays.asList(VxlanNetworkPoolVO.class.getSimpleName());
    }

    private void handleDeletionCleanup(CascadeAction action, Completion completion) {
        dbf.eoCleanup(VxlanNetworkVO.class);
        completion.success();
    }

    private void handleDeletion(final CascadeAction action, final Completion completion) {
        final List<L2VxlanNetworkInventory> l2invs = l2VxlanNetworkFromAction(action);
        if (l2invs == null) {
            completion.success();
            return;
        }

        List<L2NetworkDeletionMsg> msgs = new ArrayList<L2NetworkDeletionMsg>();
        for (L2VxlanNetworkInventory l2inv : l2invs) {
            L2NetworkDeletionMsg msg = new L2NetworkDeletionMsg();
            msg.setForceDelete(action.isActionCode(CascadeConstant.DELETION_FORCE_DELETE_CODE));
            msg.setL2NetworkUuid(l2inv.getUuid());
            bus.makeTargetServiceIdByResourceUuid(msg, L2NetworkConstant.SERVICE_ID, l2inv.getUuid());
            msgs.add(msg);
        }

        bus.send(msgs, new CloudBusListCallBack(completion) {
            @Override
            public void run(List<MessageReply> replies) {
                if (!action.isActionCode(CascadeConstant.DELETION_FORCE_DELETE_CODE)) {
                    for (MessageReply r : replies) {
                        if (!r.isSuccess()) {
                            completion.fail(r.getError());
                            return;
                        }
                    }
                }

                List<String> uuids = new ArrayList<String>();
                for (MessageReply r : replies) {
                    L2VxlanNetworkInventory inv = l2invs.get(replies.indexOf(r));
                    uuids.add(inv.getUuid());
                    logger.debug(String.format("delete L2Vxlan Network [uuid:%s, name:%s]", inv.getUuid(), inv.getName()));
                }

                dbf.removeByPrimaryKeys(uuids, VxlanNetworkVO.class);
                completion.success();
            }
        });

    }

    private void handleDeletionCheck(CascadeAction action, Completion completion) {
        List<L2VxlanNetworkInventory> l2invs = l2VxlanNetworkFromAction(action);
        if (l2invs == null) {
            completion.success();
            return;
        }

        try {
            for (L2VxlanNetworkInventory prinv : l2invs) {
                extpEmitter.preDelete(prinv);
            }

            completion.success();
        } catch (L2NetworkException e) {
            completion.fail(errf.throwableToInternalError(e));
        }
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        if (CascadeConstant.DELETION_CODES.contains(action.getActionCode())) {
            List<L2VxlanNetworkInventory> ctx = l2VxlanNetworkFromAction(action);
            if (ctx != null) {
                return action.copy().setParentIssuer(NAME).setParentIssuerContext(ctx);
            }
        } else if (action.isActionCode(L2NetworkConstant.DETACH_L2NETWORK_CODE)) {
            return action.copy().setParentIssuer(NAME);
        }

        return null;
    }

    private List<L2VxlanNetworkInventory> l2VxlanNetworkFromAction(CascadeAction action) {
        List<L2VxlanNetworkInventory> ret = null;
        if (VxlanNetworkPoolVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<String> puuids = CollectionUtils.transformToList((List<L2VxlanNetworkPoolInventory>)action.getParentIssuerContext(), new Function<String, L2VxlanNetworkPoolInventory>() {
                @Override
                public String call(L2VxlanNetworkPoolInventory arg) {
                    return arg.getUuid();
                }
            });

            SimpleQuery<VxlanNetworkVO> q = dbf.createQuery(VxlanNetworkVO.class);
            q.add(VxlanNetworkVO_.poolUuid, SimpleQuery.Op.IN, puuids);
            List<VxlanNetworkVO> l2vos = q.list();
            if (!l2vos.isEmpty()) {
                ret = L2VxlanNetworkInventory.valueOf1(l2vos);
            }
        } else if (NAME.equals(action.getParentIssuer())) {
            ret = action.getParentIssuerContext();
        }

        return ret;
    }


}
