package org.zstack.network.service.lb;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.AbstractAsyncCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.workflow.SimpleFlowChain;
import org.zstack.header.acl.AccessControlListInventory;
import org.zstack.header.acl.AccessControlListVO;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.AccountVO;
import org.zstack.header.message.MessageReply;
import org.zstack.network.service.vip.ModifyVipAttributesStruct;
import org.zstack.network.service.vip.Vip;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.network.service.vip.VipVO;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 */
public class LoadBalancerCascadeExtension extends AbstractAsyncCascadeExtension {
    private static final CLogger logger = Utils.getLogger(LoadBalancerCascadeExtension.class);
    private static final String NAME = LoadBalancerVO.class.getSimpleName();

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private LoadBalancerManager lbMgr;
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
        dbf.eoCleanup(LoadBalancerVO.class);
        completion.success();
    }

    private void handleDeletion(CascadeAction action, final Completion completion) {
        if (AccessControlListVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<AccessControlListInventory> acls = (List<AccessControlListInventory>) action.getParentIssuerContext();
            AccessControlListInventory acl = acls.get(0);

            List<LoadBalancerListenerACLRefVO> refVOS = Q.New(LoadBalancerListenerACLRefVO.class).eq(LoadBalancerListenerACLRefVO_.aclUuid, acl.getUuid()).list();
            if (refVOS.isEmpty()) {
                completion.success();
                return;
            }

            LoadBalancerListenerVO listenerVO = Q.New(LoadBalancerListenerVO.class).eq(LoadBalancerListenerVO_.uuid, refVOS.get(0).getListenerUuid()).find();
            if (listenerVO.getLoadBalancerUuid() == null) {
                completion.success();
                return;
            }

            RemoveAccessControlListFromLoadBalancerMsg msg = new RemoveAccessControlListFromLoadBalancerMsg();
            msg.setLoadBalancerUuid(listenerVO.getLoadBalancerUuid());
            msg.setAclUuids(Collections.singletonList(acl.getUuid()));
            msg.setServerGroupUuids(refVOS.stream().filter(ref -> ref.getServerGroupUuid() != null).map(LoadBalancerListenerACLRefVO::getServerGroupUuid).collect(Collectors.toList()));
            msg.setListenerUuid(listenerVO.getUuid());
            bus.makeTargetServiceIdByResourceUuid(msg, LoadBalancerConstants.SERVICE_ID, listenerVO.getLoadBalancerUuid());

            bus.send(msg, new CloudBusCallBack(msg) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        completion.fail(reply.getError());
                        return;
                    }
                    completion.success();
                }
            });

            return;
        }


        FlowChain chain = new SimpleFlowChain();
        chain.setName("delete-lb-and-lb-certs");
        chain.then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                Map<String, LoadBalancerVO> vipLoadBalancerMap = vipDetachLoadBalancerMapFromAction(action);
                if (vipLoadBalancerMap == null || vipLoadBalancerMap.isEmpty()) {
                    trigger.next();
                    return;
                }
                releaseOnlyVipOfLb(vipLoadBalancerMap, new Completion(trigger) {
                    @Override
                    public void success() {
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                final List<LoadBalancerInventory> lbs = loadBalancerFromAction(action);
                if (lbs == null || lbs.isEmpty()) {
                    trigger.next();
                    return;
                }

                List<DeleteLoadBalancerMsg> msgs = CollectionUtils.transformToList(lbs, new Function<DeleteLoadBalancerMsg, LoadBalancerInventory>() {
                    @Override
                    public DeleteLoadBalancerMsg call(LoadBalancerInventory arg) {
                        DeleteLoadBalancerMsg msg = new DeleteLoadBalancerMsg();
                        msg.setUuid(arg.getUuid());
                        bus.makeTargetServiceIdByResourceUuid(msg, LoadBalancerConstants.SERVICE_ID, msg.getUuid());
                        return msg;
                    }
                });

                List<ErrorCode> erros = new ArrayList<>();
                new While<>(msgs).all((msg, whileCompletion) -> {
                    bus.send(msg, new CloudBusCallBack(whileCompletion) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                logger.warn(String.format("failed to delete loadBalancer[uuid:%s], %s",
                                        msg.getUuid(), reply.getError()));
                                erros.add(reply.getError());
                            }

                            whileCompletion.done();
                        }
                    });
                }).run(new WhileDoneCompletion(completion) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (!erros.isEmpty()) {
                            completion.fail(erros.get(0));
                        }

                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                final List<CertificateInventory> certs = certificateFromAction(action);
                if (certs == null || certs.isEmpty()) {
                    trigger.next();
                    return;
                }

                List<CertificateDeletionMsg> msgs = CollectionUtils.transformToList(certs, new Function<CertificateDeletionMsg, CertificateInventory>() {
                    @Override
                    public CertificateDeletionMsg call(CertificateInventory arg) {
                        CertificateDeletionMsg msg = new CertificateDeletionMsg();
                        msg.setUuid(arg.getUuid());
                        bus.makeLocalServiceId(msg, LoadBalancerConstants.SERVICE_ID);
                        return msg;
                    }
                });

                new While<>(msgs).all((msg, whileCompletion) -> {
                    bus.send(msg, new CloudBusCallBack(whileCompletion) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                logger.warn(String.format("failed to delete loadBalancer[uuid:%s], %s",
                                        msg.getUuid(), reply.getError()));
                            }

                            whileCompletion.done();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        trigger.next();
                    }
                });
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    private void handleDeletionCheck(CascadeAction action, Completion completion) {
        completion.success();
    }

    @Override
    public List<String> getEdgeNames() {
        List<String> ret = new ArrayList<>();
        ret.add(AccountVO.class.getSimpleName());
        ret.add(VipVO.class.getSimpleName());
        ret.add(AccessControlListVO.class.getSimpleName());
        return ret;
    }

    @Override
    public String getCascadeResourceName() {
        return NAME;
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        if (CascadeConstant.DELETION_CODES.contains(action.getActionCode())) {
            List<LoadBalancerInventory> lbs = loadBalancerFromAction(action);
            if (lbs != null) {
                return action.copy().setParentIssuer(NAME).setParentIssuerContext(lbs);
            }
        }
        return null;
    }

    private List<CertificateInventory> certificateFromAction(CascadeAction action) {
        if (!AccountVO.class.getSimpleName().equals(action.getParentIssuer())) {
            return null;
        }

        final List<String> auuids = CollectionUtils.transformToList((List<AccountInventory>) action.getParentIssuerContext(), new Function<String, AccountInventory>() {
            @Override
            public String call(AccountInventory arg) {
                return arg.getUuid();
            }
        });

        List<CertificateVO> vos = new Callable<List<CertificateVO>>() {
            @Override
            @Transactional(readOnly = true)
            public List<CertificateVO> call() {
                String sql = "select d from CertificateVO d, AccountResourceRefVO r where d.uuid = r.resourceUuid and" +
                        " r.resourceType = :rtype and r.accountUuid in (:auuids)";
                TypedQuery<CertificateVO> q = dbf.getEntityManager().createQuery(sql, CertificateVO.class);
                q.setParameter("auuids", auuids);
                q.setParameter("rtype", CertificateVO.class.getSimpleName());
                return q.getResultList();
            }
        }.call();

        if (!vos.isEmpty()) {
            return CertificateInventory.valueOf(vos);
        }

        return null;
    }

    private List<LoadBalancerInventory> loadBalancerFromAction(CascadeAction action) {
        if (LoadBalancerVO.class.getSimpleName().equals(action.getParentIssuer())) {
            return action.getParentIssuerContext();
        } else if (AccountVO.class.getSimpleName().equals(action.getParentIssuer())) {
            final List<String> auuids = CollectionUtils.transformToList((List<AccountInventory>) action.getParentIssuerContext(), new Function<String, AccountInventory>() {
                @Override
                public String call(AccountInventory arg) {
                    return arg.getUuid();
                }
            });

            List<LoadBalancerVO> vos = new Callable<List<LoadBalancerVO>>() {
                @Override
                @Transactional(readOnly = true)
                public List<LoadBalancerVO> call() {
                    String sql = "select d from LoadBalancerVO d, AccountResourceRefVO r where d.uuid = r.resourceUuid and" +
                            " r.resourceType = :rtype and r.accountUuid in (:auuids)";
                    TypedQuery<LoadBalancerVO> q = dbf.getEntityManager().createQuery(sql, LoadBalancerVO.class);
                    q.setParameter("auuids", auuids);
                    q.setParameter("rtype", LoadBalancerVO.class.getSimpleName());
                    return q.getResultList();
                }
            }.call();

            if (!vos.isEmpty()) {
                return LoadBalancerInventory.valueOf(vos);
            }
        } else if (VipVO.class.getSimpleName().equals(action.getParentIssuer())) {

            final List<String> vipUuids = CollectionUtils.transformToList((List<VipInventory>) action.getParentIssuerContext(), new Function<String, VipInventory>() {
                @Override
                public String call(VipInventory arg) {
                    return arg.getUuid();
                }
            });

            if (vipUuids.isEmpty()) {
                return null;
            }

            List<LoadBalancerVO> vos = new Callable<List<LoadBalancerVO>>() {
                @Override
                @Transactional(readOnly = true)
                public List<LoadBalancerVO> call() {
                    String sql = "select d from LoadBalancerVO d where d.vipUuid in (:vipUuids) or d.ipv6VipUuid in (:vipUuids)";
                    TypedQuery<LoadBalancerVO> q = dbf.getEntityManager().createQuery(sql, LoadBalancerVO.class);
                    q.setParameter("vipUuids", vipUuids);
                    return q.getResultList();
                }
            }.call();

            // if delete l3, will delete all vip of l3, and delete lb
            // if delete iprange or delete vip, detach vip from lb ib lb has two vip
            Iterator<LoadBalancerVO> iterator = vos.iterator();
            while(iterator.hasNext()){
                LoadBalancerVO loadBalancerVO = iterator.next();
                if (!StringUtils.isEmpty(loadBalancerVO.getVipUuid()) && !StringUtils.isEmpty(loadBalancerVO.getIpv6VipUuid())) {
                    if (!(vipUuids.contains(loadBalancerVO.getVipUuid()) && vipUuids.contains(loadBalancerVO.getIpv6VipUuid()))) {
                        iterator.remove();
                    }
                }
            }

            if (!vos.isEmpty()) {
                return LoadBalancerInventory.valueOf(vos);
            }
        }

        return null;
    }

    private Map<String, LoadBalancerVO> vipDetachLoadBalancerMapFromAction(CascadeAction action) {
        HashMap<String, LoadBalancerVO> vipLoadBalancerMap = new HashMap<>();
        if (VipVO.class.getSimpleName().equals(action.getParentIssuer())) {

            final List<String> vipUuids = CollectionUtils.transformToList((List<VipInventory>) action.getParentIssuerContext(), new Function<String, VipInventory>() {
                @Override
                public String call(VipInventory arg) {
                    return arg.getUuid();
                }
            });

            if (vipUuids.isEmpty()) {
                return vipLoadBalancerMap;
            }

            List<LoadBalancerVO> vos = new Callable<List<LoadBalancerVO>>() {
                @Override
                @Transactional(readOnly = true)
                public List<LoadBalancerVO> call() {
                    String sql = "select d from LoadBalancerVO d where (d.vipUuid in (:vipUuids) or d.ipv6VipUuid in (:vipUuids))";
                    TypedQuery<LoadBalancerVO> q = dbf.getEntityManager().createQuery(sql, LoadBalancerVO.class);
                    q.setParameter("vipUuids", vipUuids);
                    return q.getResultList();
                }
            }.call();

            // detach vip from lb only when lb has two vip, and one is not in the vips will delete
            for (LoadBalancerVO loadBalancerVO : vos) {
                if (!StringUtils.isEmpty(loadBalancerVO.getVipUuid()) && !StringUtils.isEmpty(loadBalancerVO.getIpv6VipUuid())) {
                    if (!(vipUuids.contains(loadBalancerVO.getVipUuid()) && vipUuids.contains(loadBalancerVO.getIpv6VipUuid()))) {
                        if (vipUuids.contains(loadBalancerVO.getVipUuid())) {
                            vipLoadBalancerMap.put(loadBalancerVO.getVipUuid(), loadBalancerVO);
                        }
                        if (vipUuids.contains(loadBalancerVO.getIpv6VipUuid())) {
                            vipLoadBalancerMap.put(loadBalancerVO.getIpv6VipUuid(), loadBalancerVO);
                        }
                    }
                }
            }
        }
        return vipLoadBalancerMap;
    }

    private void releaseOnlyVipOfLb(Map<String, LoadBalancerVO> vipLoadBalancerMap, Completion completion) {
        if (vipLoadBalancerMap == null || vipLoadBalancerMap.isEmpty()) {
            completion.success();
            return;
        }

        ArrayList<DetachVipFromLoadBalancerMsg> dMsgs = new ArrayList<>();
        for (String vipUuid : vipLoadBalancerMap.keySet()) {
            LoadBalancerVO vo = vipLoadBalancerMap.get(vipUuid);
            DetachVipFromLoadBalancerMsg dmsg = new DetachVipFromLoadBalancerMsg();
            dmsg.setVipUuid(vipUuid);
            dmsg.setUuid(vo.getUuid());
            bus.makeTargetServiceIdByResourceUuid(dmsg, LoadBalancerConstants.SERVICE_ID, vipUuid);
            dMsgs.add(dmsg);
        }

        new While<>(dMsgs).step((dmsg, whileCompletion) -> {
            bus.send(dmsg, new CloudBusCallBack(dmsg) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        logger.debug(String.format("detach vip[%s] from lb[%s] error, because %s", dmsg.getVipUuid(), dmsg.getUuid(), reply.getError()));
                        whileCompletion.addError(reply.getError());
                    }
                    whileCompletion.done();
                }
            });

        }, dMsgs.size()).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errorCodeList.getCauses().isEmpty()) {
                    completion.fail(errorCodeList.getCauses().get(0));
                    return;
                }
                completion.success();
            }
        });
    }
}
