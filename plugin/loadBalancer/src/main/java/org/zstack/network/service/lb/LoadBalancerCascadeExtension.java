package org.zstack.network.service.lb;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.AbstractAsyncCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.workflow.SimpleFlowChain;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.AccountVO;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.IpRangeInventory;
import org.zstack.header.network.l3.IpRangeVO;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.network.service.vip.*;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 */
public class LoadBalancerCascadeExtension extends AbstractAsyncCascadeExtension {
    private static final CLogger logger = Utils.getLogger(LoadBalancerCascadeExtension.class);
    private static final String NAME = LoadBalancerVO.class.getSimpleName();

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;

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
        FlowChain chain = new SimpleFlowChain();
        chain.setName("delete-lb-and-lb-certs");
        chain.then(new NoRollbackFlow() {
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
                }).run(new NoErrorCompletion(completion) {
                    @Override
                    public void done() {
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
                }).run(new NoErrorCompletion(completion) {
                    @Override
                    public void done() {
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
        return Arrays.asList(AccountVO.class.getSimpleName());
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
        }
        return null;
    }
}
