package org.zstack.network.l2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cascade.AbstractAsyncCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.AccountVO;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.*;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.header.zone.ZoneVO;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import static org.zstack.core.Platform.inerr;

/**
 */
public class L2NetworkCascadeExtension extends AbstractAsyncCascadeExtension {
    private static final CLogger logger = Utils.getLogger(L2NetworkCascadeExtension.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private L2NetworkExtensionPointEmitter extpEmitter;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;

    private static final String NAME = L2NetworkVO.class.getSimpleName();

    @Override
    public void asyncCascade(CascadeAction action, Completion completion) {
        if (action.isActionCode(CascadeConstant.DELETION_CHECK_CODE)) {
            handleDeletionCheck(action, completion);
        } else if (action.isActionCode(CascadeConstant.DELETION_DELETE_CODE, CascadeConstant.DELETION_FORCE_DELETE_CODE)) {
            handleDeletion(action, completion);
        } else if (action.isActionCode(CascadeConstant.DELETION_CLEANUP_CODE)) {
            handleDeletionCleanup(action, completion);
        } else if (action.isActionCode(L2NetworkConstant.DETACH_L2NETWORK_CODE)) {
            handleDetach(action, completion);
        } else {
            completion.success();
        }
    }

    private void handleDetach(CascadeAction action, final Completion completion) {
        List<L2NetworkDetachStruct> structs = action.getParentIssuerContext();
        List<DetachL2NetworkFromClusterMsg> msgs = CollectionUtils.transformToList(structs, new Function<DetachL2NetworkFromClusterMsg, L2NetworkDetachStruct>() {
            @Override
            public DetachL2NetworkFromClusterMsg call(L2NetworkDetachStruct arg) {
                DetachL2NetworkFromClusterMsg msg = new DetachL2NetworkFromClusterMsg();
                msg.setClusterUuid(arg.getClusterUuid());
                msg.setL2NetworkUuid(arg.getL2NetworkUuid());
                bus.makeTargetServiceIdByResourceUuid(msg, L2NetworkConstant.SERVICE_ID, arg.getL2NetworkUuid());
                return msg;
            }
        });

        bus.send(msgs, new CloudBusListCallBack(completion) {
            @Override
            public void run(List<MessageReply> replies) {
                for (MessageReply r : replies) {
                    if (!r.isSuccess()) {
                        completion.fail(r.getError());
                        return;
                    }
                }

                completion.success();
            }
        });
    }

    private void handleDeletionCleanup(CascadeAction action, Completion completion) {
        try {
            final List<L2NetworkInventory> l2invs = l2NetworkFromAction(action);
            if (l2invs != null) {
                l2invs.forEach(l -> dbf.eoCleanup(L2NetworkVO.class, l.getUuid()));
            } else {
                dbf.eoCleanup(L2NetworkVO.class);
            }
        } catch (NullPointerException e) {
            logger.warn(e.getLocalizedMessage());
            dbf.eoCleanup(L2NetworkVO.class);
        } finally {
            completion.success();
        }
    }

    private void handleDeletion(final CascadeAction action, final Completion completion) {
        final List<L2NetworkInventory> l2invs = l2NetworkFromAction(action);
        if (l2invs == null) {
            completion.success();
            return;
        }

        List<L2NetworkDeletionMsg> msgs = new ArrayList<L2NetworkDeletionMsg>();
        for (L2NetworkInventory l2inv : l2invs) {
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
                    L2NetworkInventory inv = l2invs.get(replies.indexOf(r));
                    uuids.add(inv.getUuid());
                    logger.debug(String.format("delete l2 network[uuid:%s, name:%s]", inv.getUuid(), inv.getName()));
                }

                dbf.removeByPrimaryKeys(uuids, L2NetworkVO.class);
                completion.success();
            }
        });
    }

    private void handleDeletionCheck(CascadeAction action, Completion completion) {
        List<L2NetworkInventory> l2invs = l2NetworkFromAction(action);
        if (l2invs == null) {
            completion.success();
            return;
        }

        try {
            for (L2NetworkInventory prinv : l2invs) {
                extpEmitter.preDelete(prinv);
            }

            completion.success();
        } catch (L2NetworkException e) {
            completion.fail(inerr(e.getMessage()));
        }
    }

    @Override
    public List<String> getEdgeNames() {
        return Arrays.asList(ZoneVO.class.getSimpleName(), AccountVO.class.getSimpleName());
    }

    @Override
    public String getCascadeResourceName() {
        return NAME;
    }

    private List<L2NetworkInventory> l2NetworkFromAction(CascadeAction action) {
        List<L2NetworkInventory> ret = null;
        if (ZoneVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<String> zuuids = CollectionUtils.transformToList((List<ZoneInventory>)action.getParentIssuerContext(), new Function<String, ZoneInventory>() {
                @Override
                public String call(ZoneInventory arg) {
                    return arg.getUuid();
                }
            });

            SimpleQuery<L2NetworkVO> q = dbf.createQuery(L2NetworkVO.class);
            q.add(L2NetworkVO_.zoneUuid, SimpleQuery.Op.IN, zuuids);
            List<String> dtypes = new ArrayList<String>();
            dtypes.addAll(L2NetworkType.getAllDTypeNames());
            q.add(L2NetworkVO_.type, SimpleQuery.Op.IN, dtypes);
            List<L2NetworkVO> l2vos = q.list();
            if (!l2vos.isEmpty()) {
                ret = L2NetworkInventory.valueOf(l2vos);
            }
        } else if (NAME.equals(action.getParentIssuer())) {
            ret = action.getParentIssuerContext();
        } else if (AccountVO.class.getSimpleName().equals(action.getParentIssuer())) {
            final List<String> auuids = CollectionUtils.transformToList((List<AccountInventory>) action.getParentIssuerContext(), new Function<String, AccountInventory>() {
                @Override
                public String call(AccountInventory arg) {
                    return arg.getUuid();
                }
            });

            List<L2NetworkVO> vos = new Callable<List<L2NetworkVO>>() {
                @Override
                @Transactional(readOnly = true)
                public List<L2NetworkVO> call() {
                    String sql = "select d from L2NetworkVO d, AccountResourceRefVO r where d.uuid = r.resourceUuid and" +
                            " r.resourceType = :rtype and r.accountUuid in (:auuids)";
//                    and d.type in (:dtype)";
                    TypedQuery<L2NetworkVO> q = dbf.getEntityManager().createQuery(sql, L2NetworkVO.class);
                    q.setParameter("auuids", auuids);
                    q.setParameter("rtype", L2NetworkVO.class.getSimpleName());
//                    List<String> dtypes = new ArrayList<String>();
//                    dtypes.addAll(L2NetworkType.getAllDTypeNames());
////                    q.setParameter("dtype", dtypes);
//                    List<L2NetworkVO> vos = q.getResultList();
//                    List<L2NetworkVO> ret = new ArrayList<>();
//                    vos.forEach(i -> {
//                        if(dtypes.contains(i.getType())){
//                            ret.add(i);
//                        }
//                    });
//                    if(ret.size() == 0) return vos;
//                    else return ret;
                    return q.getResultList();
                }
            }.call();

            if (!vos.isEmpty()) {
                ret = L2NetworkInventory.valueOf(vos);
            }
        }

        return ret;
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        if (CascadeConstant.DELETION_CODES.contains(action.getActionCode())) {
            List<L2NetworkInventory> ctx = l2NetworkFromAction(action);
            if (ctx != null) {
                return action.copy().setParentIssuer(NAME).setParentIssuerContext(ctx);
            }
        } else if (action.isActionCode(L2NetworkConstant.DETACH_L2NETWORK_CODE)) {
            return action.copy().setParentIssuer(NAME);
        }

        return null;
    }
}
