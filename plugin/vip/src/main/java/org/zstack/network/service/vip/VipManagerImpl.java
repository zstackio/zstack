package org.zstack.network.service.vip;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginExtension;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.AbstractService;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.IdentityErrors;
import org.zstack.header.identity.Quota;
import org.zstack.header.identity.Quota.QuotaOperator;
import org.zstack.header.identity.Quota.QuotaPair;
import org.zstack.header.identity.ReportQuotaExtensionPoint;
import org.zstack.header.message.APIDeleteMessage.DeletionMode;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.*;
import org.zstack.identity.AccountManager;
import org.zstack.tag.TagManager;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.zstack.utils.CollectionDSL.list;

/**
 */
public class VipManagerImpl extends AbstractService implements VipManager, ReportQuotaExtensionPoint {
    private static final CLogger logger = Utils.getLogger(VipManagerImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private CascadeFacade casf;
    @Autowired
    private TagManager tagMgr;

    private Map<String, VipReleaseExtensionPoint> vipReleaseExts = new HashMap<String, VipReleaseExtensionPoint>();
    private Map<String, VipBackend> vipBackends = new HashMap<String, VipBackend>();

    private List<String> releaseVipByApiFlowNames;
    private FlowChainBuilder releaseVipByApiFlowChainBuilder;

    private void populateExtensions() {
        List<PluginExtension> exts = pluginRgty.getExtensionByInterfaceName(VipReleaseExtensionPoint.class.getName());
        for (PluginExtension ext : exts) {
            VipReleaseExtensionPoint extp = (VipReleaseExtensionPoint)ext.getInstance();
            VipReleaseExtensionPoint old = vipReleaseExts.get(extp.getVipUse());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate VirtualRouterVipReleaseExtensionPoint for %s, old[%s], new[%s]", old.getClass().getName(), extp.getClass().getName(), old.getVipUse()));
            }
            vipReleaseExts.put(extp.getVipUse(), extp);
        }

        exts = pluginRgty.getExtensionByInterfaceName(VipBackend.class.getName());
        for (PluginExtension ext : exts) {
            VipBackend extp = (VipBackend) ext.getInstance();
            VipBackend old = vipBackends.get(extp.getServiceProviderTypeForVip());
            if (old != null) {
                throw new CloudRuntimeException(
                    String.format("duplicate VipBackend[%s, %s] for provider type[%s]", old.getClass().getName(), extp.getClass().getName(), extp.getServiceProviderTypeForVip())
                );
            }
            vipBackends.put(extp.getServiceProviderTypeForVip(), extp);
        }
    }

    public VipReleaseExtensionPoint getVipReleaseExtensionPoint(String use) {
        VipReleaseExtensionPoint extp = vipReleaseExts.get(use);
        if (extp == null) {
            throw new CloudRuntimeException(String.format("cannot VipReleaseExtensionPoint for use[%s]", use));
        }

        return extp;
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof VipDeletionMsg) {
            handle((VipDeletionMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(final VipDeletionMsg msg) {
        final VipDeletionReply reply = new VipDeletionReply();
        final VipVO vip = dbf.findByUuid(msg.getVipUuid(), VipVO.class);

        if (vip.getUseFor() == null) {
            returnVip(VipInventory.valueOf(vip));
            dbf.removeByPrimaryKey(vip.getUuid(), VipVO.class);
            logger.debug(String.format("released vip[uuid:%s, ip:%s] on l3Network[uuid:%s]", vip.getUuid(), vip.getIp(), vip.getL3NetworkUuid()));
            bus.reply(msg, reply);
            return;
        }

        final VipInventory vipinv = VipInventory.valueOf(vip);
        FlowChain chain = releaseVipByApiFlowChainBuilder.build();
        chain.setName(String.format("api-release-vip-uuid-%s-ip-%s-name-%s", vipinv.getUuid(), vipinv.getIp(), vipinv.getName()));
        chain.getData().put(VipConstant.Params.VIP.toString(), vipinv);
        chain.done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                returnVip(vipinv);
                dbf.removeByPrimaryKey(vip.getUuid(), VipVO.class);
                bus.reply(msg, reply);
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                reply.setError(errCode);
                bus.reply(msg, reply);
            }
        }).start();
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APICreateVipMsg) {
            handle((APICreateVipMsg) msg);
        } else if (msg instanceof APIDeleteVipMsg) {
            handle((APIDeleteVipMsg) msg);
        } else if (msg instanceof APIChangeVipStateMsg) {
            handle((APIChangeVipStateMsg) msg);
        } else if (msg instanceof APIUpdateVipMsg) {
            handle((APIUpdateVipMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIUpdateVipMsg msg) {
        VipVO vo = dbf.findByUuid(msg.getUuid(), VipVO.class);
        boolean update = false;
        if (msg.getName() != null) {
            vo.setName(msg.getName());
            update = true;
        }
        if (msg.getDescription() != null) {
            vo.setDescription(msg.getDescription());
            update = true;
        }
        if (update) {
            vo = dbf.updateAndRefresh(vo);
        }

        APIUpdateVipEvent evt = new APIUpdateVipEvent(msg.getId());
        evt.setInventory(VipInventory.valueOf(vo));
        bus.publish(evt);
    }

    private void handle(APIChangeVipStateMsg msg) {
        VipVO vip = dbf.findByUuid(msg.getUuid(), VipVO.class);
        VipStateEvent sevt = VipStateEvent.valueOf(msg.getStateEvent());
        vip.setState(vip.getState().nextState(sevt));
        vip = dbf.updateAndRefresh(vip);

        APIChangeVipStateEvent evt = new APIChangeVipStateEvent(msg.getId());
        evt.setInventory(VipInventory.valueOf(vip));
        bus.publish(evt);
    }

    private void returnVip(VipInventory vip) {
        ReturnIpMsg msg = new ReturnIpMsg();
        msg.setL3NetworkUuid(vip.getL3NetworkUuid());
        msg.setUsedIpUuid(vip.getUsedIpUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, L3NetworkConstant.SERVICE_ID, vip.getL3NetworkUuid());
        bus.send(msg);
    }

    private void handle(final APIDeleteVipMsg msg) {
        final VipVO vip = dbf.findByUuid(msg.getUuid(), VipVO.class);
        final APIDeleteVipEvent evt = new APIDeleteVipEvent(msg.getId());
        final String issuer = VipVO.class.getSimpleName();
        final List<VipInventory> ctx = Arrays.asList(VipInventory.valueOf(vip));
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("delete-vip-%s", vip.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                if (msg.getDeletionMode() == DeletionMode.Permissive) {
                    flow(new NoRollbackFlow() {
                        String __name__ = String.format("delete-vip-permissive-check");

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            casf.asyncCascade(CascadeConstant.DELETION_CHECK_CODE, issuer, ctx, new Completion(trigger) {
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
                    });

                    flow(new NoRollbackFlow() {
                        String __name__ = String.format("delete-vip-permissive-delete");

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            casf.asyncCascade(CascadeConstant.DELETION_DELETE_CODE, issuer, ctx, new Completion(trigger) {
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
                    });
                } else {
                    flow(new NoRollbackFlow() {
                        String __name__ = String.format("delete-vip-force-delete");

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            casf.asyncCascade(CascadeConstant.DELETION_FORCE_DELETE_CODE, issuer, ctx, new Completion(trigger) {
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
                    });
                }

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        casf.asyncCascadeFull(CascadeConstant.DELETION_CLEANUP_CODE, issuer, ctx, new NopeCompletion());
                        bus.publish(evt);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        evt.setErrorCode(errf.instantiateErrorCode(SysErrors.DELETE_RESOURCE_ERROR, errCode));
                        bus.publish(evt);
                    }
                });
            }
        }).start();
    }

    private void handle(final APICreateVipMsg msg) {
        final APICreateVipEvent evt = new APICreateVipEvent(msg.getId());

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("create-vip-%s-from-l3-%s", msg.getName(), msg.getL3NetworkUuid()));
        chain.then(new ShareFlow() {
            UsedIpInventory ip;
            VipInventory vip;

            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = String.format("allocate-ip-for-vip");

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        String strategyType = msg.getAllocatorStrategy() == null ? L3NetworkConstant.RANDOM_IP_ALLOCATOR_STRATEGY : msg.getAllocatorStrategy();
                        AllocateIpMsg amsg = new AllocateIpMsg();
                        amsg.setL3NetworkUuid(msg.getL3NetworkUuid());
                        amsg.setAllocateStrategy(strategyType);
                        amsg.setRequiredIp(msg.getRequiredIp());
                        bus.makeTargetServiceIdByResourceUuid(amsg, L3NetworkConstant.SERVICE_ID, msg.getL3NetworkUuid());
                        bus.send(amsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (reply.isSuccess()) {
                                    AllocateIpReply re = reply.castReply();
                                    ip = re.getIpInventory();
                                    trigger.next();
                                } else {
                                    trigger.fail(reply.getError());
                                }
                            }
                        });
                    }

                    @Override
                    public void rollback(final FlowRollback trigger, Map data) {
                        if (ip == null) {
                            trigger.rollback();
                            return;
                        }

                        ReturnIpMsg rmsg = new ReturnIpMsg();
                        rmsg.setL3NetworkUuid(ip.getL3NetworkUuid());
                        rmsg.setUsedIpUuid(ip.getUuid());
                        bus.makeTargetServiceIdByResourceUuid(rmsg, L3NetworkConstant.SERVICE_ID, rmsg.getL3NetworkUuid());
                        bus.send(rmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    logger.warn(String.format("failed to return ip[uuid:%s, ip:%s] to l3Network[uuid:%s], %s",
                                            ip.getUuid(), ip.getIp(), ip.getL3NetworkUuid(), reply.getError()));
                                }

                                trigger.rollback();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = String.format("create-vip-in-db");

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        VipVO vipvo = new VipVO();
                        if (msg.getResourceUuid() != null) {
                            vipvo.setUuid(msg.getResourceUuid());
                        } else {
                            vipvo.setUuid(Platform.getUuid());
                        }
                        vipvo.setName(msg.getName());
                        vipvo.setDescription(msg.getDescription());
                        vipvo.setState(VipState.Enabled);
                        vipvo.setGateway(ip.getGateway());
                        vipvo.setIp(ip.getIp());
                        vipvo.setIpRangeUuid(ip.getIpRangeUuid());
                        vipvo.setL3NetworkUuid(ip.getL3NetworkUuid());
                        vipvo.setNetmask(ip.getNetmask());
                        vipvo.setUsedIpUuid(ip.getUuid());

                        vipvo = dbf.persistAndRefresh(vipvo);

                        acntMgr.createAccountResourceRef(msg.getSession().getAccountUuid(), vipvo.getUuid(), VipVO.class);
                        tagMgr.createTagsFromAPICreateMessage(msg, vipvo.getUuid(), VipVO.class.getSimpleName());

                        vip = VipInventory.valueOf(vipvo);
                        trigger.next();
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        logger.debug(String.format("successfully acquired vip[uuid:%s, address:%s] on l3NetworkUuid[uuid:%s]", vip.getUuid(), ip.getIp(), ip.getL3NetworkUuid()));
                        evt.setInventory(vip);
                        bus.publish(evt);
                    }
                });

                error(new FlowErrorHandler() {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        evt.setErrorCode(errCode);
                        bus.publish(evt);
                    }
                });
            }
        }).start();
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(VipConstant.SERVICE_ID);
    }

    private void prepareFlows() {
        releaseVipByApiFlowChainBuilder = FlowChainBuilder.newBuilder().setFlowClassNames(releaseVipByApiFlowNames).construct();
    }

    @Override
    public boolean start() {
        populateExtensions();
        prepareFlows();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public VipBackend getVipBackend(String providerType) {
        VipBackend backend = vipBackends.get(providerType);
        DebugUtils.Assert(backend!=null, String.format("cannot find VipBackend for provider type[%s]", providerType));
        return backend;
    }

    @Override
    public void saveVipInfo(String vipUuid, String networkServiceType, String peerL3NetworkUuid) {
        VipVO vo = dbf.findByUuid(vipUuid, VipVO.class);
        vo.setServiceProvider(networkServiceType);
        if (vo.getPeerL3NetworkUuid() == null) {
            vo.setPeerL3NetworkUuid(peerL3NetworkUuid);
        }
        dbf.update(vo);
    }

    @Override
    public void lockAndAcquireVip(VipInventory vip, L3NetworkInventory peerL3Network, String networkServiceType, String networkServiceProviderType, Completion completion) {
        lockVip(vip, networkServiceType);
        acquireVip(vip, peerL3Network, networkServiceProviderType, completion);
    }

    @Override
    public void releaseAndUnlockVip(VipInventory vip, Completion completion) {
        releaseAndUnlockVip(vip, true, completion);
    }

    @Override
    public void releaseAndUnlockVip(final VipInventory vip, boolean releasePeerL3Network, final Completion completion) {
        releaseVip(vip, releasePeerL3Network, new Completion(completion) {
            @Override
            public void success() {
                unlockVip(vip);
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public void acquireVip(final VipInventory vip, final L3NetworkInventory peerL3Network, final String networkServiceProviderType, final Completion completion) {
        if (vip.getPeerL3NetworkUuid() != null && !vip.getPeerL3NetworkUuid().equals(peerL3Network.getUuid())) {
            completion.fail(
                    errf.stringToOperationError(String.format("vip[uuid:%s, name:%s] has been serving l3Network[name:%s, uuid:%s], can't serve l3Network[name:%s, uuid:%s]",
                            vip.getUuid(), vip.getName(), vip.getName(), vip.getPeerL3NetworkUuid(), peerL3Network.getName(), peerL3Network.getUuid()))
            );
            return;
        }

        VipBackend bkd = getVipBackend(networkServiceProviderType);
        bkd.acquireVip(vip, peerL3Network, new Completion(completion) {
            @Override
            public void success() {
                saveVipInfo(vip.getUuid(), networkServiceProviderType, peerL3Network.getUuid());

                logger.debug(String.format("successfully acquired vip[uuid:%s, name:%s, ip:%s] on service[%s]",
                        vip.getUuid(), vip.getName(), vip.getIp(), networkServiceProviderType));
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public void releaseVip(final VipInventory vip, final boolean releasePeerL3Network, final Completion completion) {
        if (vip.getServiceProvider() == null) {
            // the vip has been released by other descendant network service
            completion.success();
            return;
        }

        VipBackend bkd = getVipBackend(vip.getServiceProvider());
        // service provider should ensure vip always release successfully,
        // use its garbage collector on failure
        bkd.releaseVip(vip, new Completion() {
            @Override
            public void success() {
                logger.debug(String.format("successfully released vip[uuid:%s, name:%s, ip:%s] on service[%s]",
                        vip.getUuid(), vip.getName(), vip.getIp(), vip.getServiceProvider()));
                VipVO vo = dbf.findByUuid(vip.getUuid(), VipVO.class);
                vo.setServiceProvider(null);
                if (releasePeerL3Network) {
                    vo.setPeerL3NetworkUuid(null);
                }
                dbf.update(vo);
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.warn(String.format("failed to release vip[uuid:%s, name:%s, ip:%s] on service[%s], its garbage collector should" +
                        " handle this", vip.getUuid(), vip.getName(), vip.getIp(), vip.getServiceProvider()));
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public void releaseVip(final VipInventory vip, Completion completion) {
        releaseVip(vip, true, completion);
    }

    @Override
    public void unlockVip(VipInventory vip) {
        VipVO vo = dbf.findByUuid(vip.getUuid(), VipVO.class);

        vo.setUseFor(null);
        dbf.update(vo);
        logger.debug(String.format("successfully unlocked vip[uuid:%s, name:%s, ip:%s]",
                vip.getUuid(), vip.getName(), vip.getIp()));
    }

    @Override
    public void lockVip(VipInventory vip,  String networkServiceType) {
        SimpleQuery<VipVO> q = dbf.createQuery(VipVO.class);
        q.add(VipVO_.uuid, SimpleQuery.Op.EQ, vip.getUuid());
        VipVO vipvo = q.find();

        if (vipvo == null) {
            throw new OperationFailureException(
                    errf.stringToOperationError(String.format("no vip[uuid:%s, name:%s, ip:%s] found for lock", vip.getUuid(), vip.getName(), vip.getIp()))
            );
        }

        if ((vipvo.getUseFor() != null && !vipvo.getUseFor().equals(networkServiceType))) {
            throw new OperationFailureException(
                    errf.stringToOperationError(String.format("vip[uuid:%s, name:%s, ip:%s] has been occupied by usage[%s]",
                            vipvo.getUuid(), vipvo.getName(), vipvo.getIp(), vipvo.getUseFor()))
            );
        }

        if (networkServiceType.equals(vipvo.getUseFor())) {
            return;
        }

        vipvo.setUseFor(networkServiceType);
        dbf.update(vipvo);

        logger.debug(String.format("successfully locked vip[uuid:%s, name:%s, ip:%s] for %s",
                vip.getUuid(), vip.getName(), vip.getIp(), networkServiceType));
    }

    public void setReleaseVipByApiFlowNames(List<String> releaseVipByApiFlowNames) {
        this.releaseVipByApiFlowNames = releaseVipByApiFlowNames;
    }

    @Override
    public List<Quota> reportQuota() {
        QuotaOperator checker = new QuotaOperator() {
            @Override
            public void checkQuota(APIMessage msg, Map<String, QuotaPair> pairs) {
                if (msg instanceof APICreateVipMsg) {
                    check((APICreateVipMsg)msg, pairs);
                }
            }

            @Override
            public List<Quota.QuotaUsage> getQuotaUsageByAccount(String accountUuid) {
                Quota.QuotaUsage usage = new Quota.QuotaUsage();
                usage.setUsed(getUsedVip(accountUuid));
                usage.setName(VipConstant.QUOTA_VIP_NUM);
                return list(usage);
            }

            @Transactional(readOnly = true)
            private long getUsedVip(String accountUuid) {
                String sql = "select count(vip) from VipVO vip, AccountResourceRefVO ref where ref.resourceUuid = vip.uuid" +
                        " and ref.accountUuid = :auuid and ref.resourceType = :rtype";
                TypedQuery<Long> q = dbf.getEntityManager().createQuery(sql, Long.class);
                q.setParameter("auuid", accountUuid);
                q.setParameter("rtype", VipVO.class.getSimpleName());
                Long vn = q.getSingleResult();
                vn = vn == null ? 0 : vn;
                return vn;
            }

            private void check(APICreateVipMsg msg, Map<String, QuotaPair> pairs) {
                long vipNum = pairs.get(VipConstant.QUOTA_VIP_NUM).getValue();
                long vn = getUsedVip(msg.getSession().getAccountUuid());

                if (vn + 1 > vipNum) {
                    throw new ApiMessageInterceptionException(errf.instantiateErrorCode(IdentityErrors.QUOTA_EXCEEDING,
                            String.format("quota exceeding. The account[uuid: %s] exceeds a quota[name: %s, value: %s]",
                                    msg.getSession().getAccountUuid(), VipConstant.QUOTA_VIP_NUM, vipNum)
                    ));
                }
            }
        };

        Quota quota = new Quota();
        quota.addMessageNeedValidation(APICreateVipMsg.class);
        quota.setOperator(checker);

        QuotaPair p = new QuotaPair();
        p.setName(VipConstant.QUOTA_VIP_NUM);
        p.setValue(20);
        quota.addPair(p);

        return list(quota);
    }
}
