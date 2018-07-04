package org.zstack.network.service.portforwarding;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.*;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.AbstractService;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.Quota;
import org.zstack.header.identity.Quota.QuotaOperator;
import org.zstack.header.identity.Quota.QuotaPair;
import org.zstack.header.identity.ReportQuotaExtensionPoint;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.NeedQuotaCheckMessage;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.service.NetworkServiceProviderType;
import org.zstack.header.network.service.NetworkServiceType;
import org.zstack.header.query.AddExpandedQueryExtensionPoint;
import org.zstack.header.query.ExpandedQueryAliasStruct;
import org.zstack.header.query.ExpandedQueryStruct;
import org.zstack.header.vm.*;
import org.zstack.identity.AccountManager;
import org.zstack.identity.QuotaUtil;
import org.zstack.network.service.NetworkServiceManager;
import org.zstack.network.service.vip.*;
import org.zstack.tag.TagManager;
import org.zstack.utils.*;
import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.zstack.utils.CollectionDSL.list;
import static org.zstack.utils.VipUseForList.SNAT_NETWORK_SERVICE_TYPE;

public class PortForwardingManagerImpl extends AbstractService implements PortForwardingManager,
        VipReleaseExtensionPoint, AddExpandedQueryExtensionPoint, ReportQuotaExtensionPoint, VipGetUsedPortRangeExtensionPoint, VipGetServiceReferencePoint {
    private static CLogger logger = Utils.getLogger(PortForwardingManagerImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private NetworkServiceManager nwServiceMgr;
    @Autowired
    private TagManager tagMgr;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private VipManager vipMgr;
    @Autowired
    private ThreadFacade thdf;

    private Map<String, PortForwardingBackend> backends = new HashMap<String, PortForwardingBackend>();
    private List<AttachPortForwardingRuleExtensionPoint> attachRuleExts = new ArrayList<AttachPortForwardingRuleExtensionPoint>();
    private List<RevokePortForwardingRuleExtensionPoint> revokeRuleExts = new ArrayList<RevokePortForwardingRuleExtensionPoint>();

    @Override
    public String getId() {
        return bus.makeLocalServiceId(PortForwardingConstant.SERVICE_ID);
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
        if (msg instanceof PortForwardingRuleDeletionMsg) {
            handle((PortForwardingRuleDeletionMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void removePortforwardingRule(final Iterator<String> it, final Completion completion) {
        if (!it.hasNext()) {
            completion.success();
            return;
        }

        String uuid = it.next();
        removePortforwardingRule(uuid, new Completion(completion) {
            @Override
            public void success() {
                removePortforwardingRule(it, completion);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private void handle(final PortForwardingRuleDeletionMsg msg) {
        final PortForwardingRuleDeletionReply reply = new PortForwardingRuleDeletionReply();
        removePortforwardingRule(msg.getRuleUuids().iterator(), new Completion(msg) {
            @Override
            public void success () {
                bus.reply(msg, reply);
            }

            @Override
            public void fail (ErrorCode errorCode){
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APICreatePortForwardingRuleMsg) {
            handle((APICreatePortForwardingRuleMsg) msg);
        } else if (msg instanceof APIDeletePortForwardingRuleMsg) {
            handle((APIDeletePortForwardingRuleMsg) msg);
        } else if (msg instanceof APIListPortForwardingRuleMsg) {
            handle((APIListPortForwardingRuleMsg) msg);
        } else if (msg instanceof APIAttachPortForwardingRuleMsg) {
            handle((APIAttachPortForwardingRuleMsg) msg);
        } else if (msg instanceof APIDetachPortForwardingRuleMsg) {
            handle((APIDetachPortForwardingRuleMsg) msg);
        } else if (msg instanceof APIChangePortForwardingRuleStateMsg) {
            handle((APIChangePortForwardingRuleStateMsg) msg);
        } else if (msg instanceof APIGetPortForwardingAttachableVmNicsMsg) {
            handle((APIGetPortForwardingAttachableVmNicsMsg) msg);
        } else if (msg instanceof APIUpdatePortForwardingRuleMsg) {
            handle((APIUpdatePortForwardingRuleMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIUpdatePortForwardingRuleMsg msg) {
        boolean update = false;

        PortForwardingRuleVO vo = dbf.findByUuid(msg.getUuid(), PortForwardingRuleVO.class);
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

        APIUpdatePortForwardingRuleEvent evt = new APIUpdatePortForwardingRuleEvent(msg.getId());
        evt.setInventory(PortForwardingRuleInventory.valueOf(vo));
        bus.publish(evt);
    }

    private List<VmNicInventory> getAttachableVmNics(String ruleUuid) {
        return new SQLBatchWithReturn<List<VmNicInventory>>(){

            @Override
            protected List<VmNicInventory> scripts() {
                String vmNicUuid = Q.New(PortForwardingRuleVO.class)
                        .select(PortForwardingRuleVO_.vmNicUuid)
                        .eq(PortForwardingRuleVO_.uuid, ruleUuid)
                        .findValue();
                if (vmNicUuid != null) {
                    return new ArrayList<>();
                }

                Tuple t = sql("select l3.zoneUuid, vip.uuid" +
                        " from L3NetworkVO l3, VipVO vip, PortForwardingRuleVO rule" +
                        " where vip.l3NetworkUuid = l3.uuid" +
                        " and vip.uuid = rule.vipUuid" +
                        " and rule.uuid = :ruleUuid",Tuple.class)
                        .param("ruleUuid", ruleUuid).find();
                String zoneUuid = t.get(0, String.class);
                String vipUuid = t.get(1, String.class);
                List<VipPeerL3NetworkRefVO> vipPeerL3Refs = Q.New(VipPeerL3NetworkRefVO.class)
                        .eq(VipPeerL3NetworkRefVO_.vipUuid, vipUuid)
                        .list();
                List<String> vipPeerL3Uuids = new ArrayList<>();
                if (vipPeerL3Refs != null && !vipPeerL3Refs.isEmpty()) {
                    vipPeerL3Uuids = vipPeerL3Refs.stream()
                            .map(ref -> ref.getL3NetworkUuid())
                            .collect(Collectors.toList());
                }
                VipVO vipVO = Q.New(VipVO.class).eq(VipVO_.uuid, vipUuid).find();

                //0.check the l3 of vm nic has been attached to port forwarding service
                List<String> l3Uuids = new ArrayList<>();
                if (vipPeerL3Uuids == null || vipPeerL3Uuids.isEmpty()) {
                    if (vipVO.getUseFor().contains(SNAT_NETWORK_SERVICE_TYPE)) {
                        l3Uuids = sql("select l3.uuid" +
                                " from L3NetworkVO l3, VipVO vip, NetworkServiceL3NetworkRefVO ref, " +
                                " VmNicVO vmnic, VirtualRouterVipVO vrVip" +
                                " where vip.uuid = :vipUuid" +
                                " and vrVip.uuid = vip.uuid" +
                                " and vmnic.vmInstanceUuid = vrVip.virtualRouterVmUuid" +
                                " and vmnic.l3NetworkUuid = l3.uuid" +
                                " and l3.uuid != vip.l3NetworkUuid" +
                                " and l3.uuid = ref.l3NetworkUuid" +
                                " and ref.networkServiceType = :nsType" +
                                " and l3.zoneUuid = :zoneUuid")
                                .param("vipUuid", vipUuid)
                                .param("zoneUuid", zoneUuid)
                                .param("nsType", PortForwardingConstant.PORTFORWARDING_NETWORK_SERVICE_TYPE).list();
                    } else {
                        l3Uuids = sql("select l3.uuid" +
                                " from L3NetworkVO l3, VipVO vip, NetworkServiceL3NetworkRefVO ref" +
                                " where l3.system = :system" +
                                " and l3.uuid != vip.l3NetworkUuid" +
                                " and l3.uuid = ref.l3NetworkUuid" +
                                " and ref.networkServiceType = :nsType" +
                                " and l3.zoneUuid = :zoneUuid" +
                                " and vip.uuid = :vipUuid")
                                .param("vipUuid", vipUuid)
                                .param("system", false)
                                .param("zoneUuid", zoneUuid)
                                .param("nsType", PortForwardingConstant.PORTFORWARDING_NETWORK_SERVICE_TYPE).list();
                    }
                } else {
                    VmNicVO rnic = Q.New(VmNicVO.class).in(VmNicVO_.l3NetworkUuid, vipPeerL3Uuids)
                            .notNull(VmNicVO_.metaData).limit(1).find();
                    if (rnic == null) {
                        l3Uuids.addAll(vipPeerL3Uuids);
                    } else {
                        List<String> vrAttachedL3Uuids = Q.New(VmNicVO.class)
                                .select(VmNicVO_.l3NetworkUuid)
                                .eq(VmNicVO_.vmInstanceUuid, rnic.getVmInstanceUuid())
                                .listValues();
                        Set l3UuidSet = new HashSet<>(vipPeerL3Uuids);
                        l3UuidSet.addAll(vrAttachedL3Uuids);
                        l3Uuids.addAll(l3UuidSet);
                    }
                }

                if (l3Uuids.isEmpty()) {
                    return new ArrayList<>();
                } else {
                    logger.debug(String.format("selected l3s for portforwarding[uuid:%s] attach: %s", ruleUuid, l3Uuids));
                }


                // 1.select private l3
                String rulePublicL3Uuid = sql("select l3NetworkUuid from VipVO where uuid = (select vipUuid from PortForwardingRuleVO where uuid = :ruleUuid)", String.class)
                        .param("ruleUuid", ruleUuid).find();

                List<String> vrouterUuids = sql("select uuid from VirtualRouterVmVO where publicNetworkUuid = :publicNetworkUuid",String.class)
                        .param("publicNetworkUuid", rulePublicL3Uuid).list();
                if(vrouterUuids.isEmpty()){
                    return new ArrayList<>();
                }

                List<String> privateL3Uuids = new ArrayList<>();
                for(String vrouterUuid : vrouterUuids){
                    List<String> vrouterPrivateL3Uuids = sql("select l3NetworkUuid from VmNicVO " +
                            " where vmInstanceUuid = :vrouterUuid" +
                            " and l3NetworkUuid != :rulePublicL3Uuid " +
                            " and l3NetworkUuid != (select managementNetworkUuid from ApplianceVmVO where uuid = :vrouterUuid)", String.class)
                            .param("vrouterUuid", vrouterUuid)
                            .param("rulePublicL3Uuid", rulePublicL3Uuid)
                            .list();
                    privateL3Uuids.addAll(vrouterPrivateL3Uuids);
                }
                if(privateL3Uuids.isEmpty()){
                    return new ArrayList<>();
                }

                l3Uuids = l3Uuids.stream()
                        .filter(l3 -> privateL3Uuids.contains(l3))
                        .collect(Collectors.toList());
                if (l3Uuids.isEmpty()) {
                    return new ArrayList<>();
                }

                //2.check the state of port forwarding and vm
                t = sql("select pf.privatePortStart, pf.privatePortEnd, pf.protocolType" +
                        " from PortForwardingRuleVO pf" +
                        " where pf.uuid = :ruleUuid",Tuple.class)
                        .param("ruleUuid", ruleUuid).find();
                int sport = t.get(0, Integer.class);
                int eport = t.get(1, Integer.class);
                PortForwardingProtocolType protocol = t.get(2, PortForwardingProtocolType.class);

                List<VmNicVO> nics = sql("select nic from VmNicVO nic, VmInstanceVO vm where nic.l3NetworkUuid in (:l3Uuids)" +
                        " and nic.vmInstanceUuid = vm.uuid and vm.type = :vmType and vm.state in (:vmStates)" +
                        " and nic.uuid not in (select pf.vmNicUuid from PortForwardingRuleVO pf" +
                        " where pf.protocolType = :protocol and pf.vmNicUuid is not null and" +
                        " ((pf.privatePortStart >= :sport and pf.privatePortStart <= :eport) or" +
                        " (pf.privatePortStart <= :sport and :sport <= pf.privatePortEnd)))")
                        .param("l3Uuids", l3Uuids)
                        .param("vmType", VmInstanceConstant.USER_VM_TYPE)
                        .param("vmStates", asList(VmInstanceState.Running, VmInstanceState.Stopped))
                        .param("sport", sport)
                        .param("eport", eport)
                        .param("protocol", protocol).list();

                if(nics.isEmpty()){
                    return new ArrayList<>();
                }

                //3.exclude the vm which  has port forwarding rules that have different VIPs
                List<String> usedVm = sql("select nic.vmInstanceUuid" +
                        " from PortForwardingRuleVO pf1, VmNicVO nic" +
                        " where pf1.vipUuid != :vipUuid" +
                        " and pf1.vmNicUuid is not null" +
                        " and pf1.vmNicUuid = nic.uuid")
                        .param("vipUuid",vipUuid).list();

                return VmNicInventory.valueOf(nics.stream().filter(nic -> !usedVm.contains(nic.getVmInstanceUuid())).collect(Collectors.toList()));
            }
        }.execute();
    }

    private void handle(APIGetPortForwardingAttachableVmNicsMsg msg) {
        APIGetPortForwardingAttachableVmNicsReply reply = new APIGetPortForwardingAttachableVmNicsReply();
        reply.setInventories(getAttachableVmNics(msg.getRuleUuid()));
        bus.reply(msg, reply);
    }

    private void handle(APIChangePortForwardingRuleStateMsg msg) {
        PortForwardingRuleVO vo = dbf.findByUuid(msg.getUuid(), PortForwardingRuleVO.class);
        vo.setState(vo.getState().nextState(PortForwardingRuleStateEvent.valueOf(msg.getStateEvent())));
        vo = dbf.updateAndRefresh(vo);

        APIChangePortForwardingRuleStateEvent evt = new APIChangePortForwardingRuleStateEvent(msg.getId());
        evt.setInventory(PortForwardingRuleInventory.valueOf(vo));
        bus.publish(evt);
    }

    private void handle(APIDetachPortForwardingRuleMsg msg) {
        final APIDetachPortForwardingRuleEvent evt = new APIDetachPortForwardingRuleEvent(msg.getId());
        final PortForwardingRuleVO vo = dbf.findByUuid(msg.getUuid(), PortForwardingRuleVO.class);

        PortForwardingRuleInventory inv = PortForwardingRuleInventory.valueOf(vo);
        final PortForwardingStruct struct = makePortForwardingStruct(inv);
        struct.setReleaseVmNicInfoWhenDetaching(true);
        final NetworkServiceProviderType providerType = nwServiceMgr.getTypeOfNetworkServiceProviderForService(struct.getGuestL3Network().getUuid(),
                NetworkServiceType.PortForwarding);

        detachPortForwardingRule(struct, providerType.toString(), new Completion(msg) {
            @Override
            public void success() {
                PortForwardingRuleVO prvo = dbf.reload(vo);
                evt.setInventory(PortForwardingRuleInventory.valueOf(prvo));
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });
    }

    @Transactional
    private VmInstanceState getVmStateFromVmNicUuid(String vmNicUuid) {
        String sql = "select vm.state from VmInstanceVO vm, VmNicVO nic where vm.uuid = nic.vmInstanceUuid and nic.uuid = :nicuuid";
        TypedQuery<VmInstanceState> q = dbf.getEntityManager().createQuery(sql, VmInstanceState.class);
        q.setParameter("nicuuid", vmNicUuid);
        return q.getSingleResult();
    }

    private void handle(final APIAttachPortForwardingRuleMsg msg) {
        final APIAttachPortForwardingRuleEvent evt = new APIAttachPortForwardingRuleEvent(msg.getId());
        PortForwardingRuleVO vo = dbf.findByUuid(msg.getRuleUuid(), PortForwardingRuleVO.class);
        VmNicVO nicvo = dbf.findByUuid(msg.getVmNicUuid(), VmNicVO.class);
        vo.setVmNicUuid(nicvo.getUuid());
        vo.setGuestIp(nicvo.getIp());
        final PortForwardingRuleVO prvo = dbf.updateAndRefresh(vo);
        final PortForwardingRuleInventory inv = PortForwardingRuleInventory.valueOf(prvo);

        VmInstanceState vmState = getVmStateFromVmNicUuid(msg.getVmNicUuid());
        if (VmInstanceState.Running != vmState) {
            Vip vip = new Vip(vo.getVipUuid());
            ModifyVipAttributesStruct struct = new ModifyVipAttributesStruct();
            struct.setUseFor(PortForwardingConstant.PORTFORWARDING_NETWORK_SERVICE_TYPE);
            final NetworkServiceProviderType providerType =
                    nwServiceMgr.getTypeOfNetworkServiceProviderForService(
                            nicvo.getL3NetworkUuid(), NetworkServiceType.PortForwarding);
            struct.setServiceProvider(providerType.toString());
            struct.setPeerL3NetworkUuid(nicvo.getL3NetworkUuid());
            vip.setStruct(struct);
            vip.acquire(new Completion(msg) {
                @Override
                public void success() {
                    evt.setInventory(inv);
                    bus.publish(evt);
                    return;
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    evt.setError(errorCode);
                    bus.publish(evt);
                }
            });
        }

        final PortForwardingStruct struct = makePortForwardingStruct(inv);
        final NetworkServiceProviderType providerType = nwServiceMgr.getTypeOfNetworkServiceProviderForService(struct.getGuestL3Network().getUuid(), NetworkServiceType.PortForwarding);
        attachPortForwardingRule(struct, providerType.toString(), new Completion(msg) {
            @Override
            public void success() {
                evt.setInventory(inv);
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                prvo.setVmNicUuid(null);
                prvo.setGuestIp(null);
                dbf.update(prvo);
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });
    }

    private void handle(APIListPortForwardingRuleMsg msg) {
        List<PortForwardingRuleVO> vos = dbf.listByApiMessage(msg, PortForwardingRuleVO.class);
        List<PortForwardingRuleInventory> invs = PortForwardingRuleInventory.valueOf(vos);
        APIListPortForwardingRuleReply reply = new APIListPortForwardingRuleReply();
        reply.setInventories(invs);
        bus.reply(msg, reply);
    }

    private boolean isNeedRemoveVip(PortForwardingRuleInventory inv) {
        SimpleQuery q = dbf.createQuery(PortForwardingRuleVO.class);
        q.add(PortForwardingRuleVO_.vipUuid, Op.EQ, inv.getVipUuid());
        q.add(PortForwardingRuleVO_.vmNicUuid, Op.NOT_NULL);
        return q.count() == 1;
    }

    private void removePortforwardingRule(String ruleUuid, final Completion complete) {
        final PortForwardingRuleVO vo = dbf.findByUuid(ruleUuid, PortForwardingRuleVO.class);
        final PortForwardingRuleInventory inv = PortForwardingRuleInventory.valueOf(vo);

        if (vo.getVmNicUuid() == null) {
            ModifyVipAttributesStruct struct = new ModifyVipAttributesStruct();
            struct.setUseFor(PortForwardingConstant.PORTFORWARDING_NETWORK_SERVICE_TYPE);
            Vip v = new Vip(vo.getVipUuid());
            v.setStruct(struct);
            v.release(new Completion(complete) {
                @Override
                public void success() {
                    dbf.remove(vo);
                    complete.success();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    complete.fail(errorCode);
                }
            });

            return;
        }

        final PortForwardingStruct struct = makePortForwardingStruct(inv);
        final NetworkServiceProviderType providerType = nwServiceMgr.getTypeOfNetworkServiceProviderForService(struct.getGuestL3Network().getUuid(),
                NetworkServiceType.PortForwarding);

        for (RevokePortForwardingRuleExtensionPoint extp : revokeRuleExts) {
            try {
                extp.preRevokePortForwardingRule(inv, providerType);
            } catch (PortForwardingException e) {
                String err = String.format("unable to revoke port forwarding rule[uuid:%s]", inv.getUuid());
                logger.warn(err, e);
                complete.fail(errf.throwableToOperationError(e));
                return;
            }
        }

        CollectionUtils.safeForEach(revokeRuleExts, new ForEachFunction<RevokePortForwardingRuleExtensionPoint>() {
            @Override
            public void run(RevokePortForwardingRuleExtensionPoint extp) {
                extp.beforeRevokePortForwardingRule(inv, providerType);
            }
        });

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "delete-portforwarding-rule";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        PortForwardingBackend bkd = getPortForwardingBackend(providerType);
                        bkd.revokePortForwardingRule(struct, new Completion(trigger) {
                            @Override
                            public void success() {
                                logger.debug(String.format("successfully detached %s", struct.toString()));

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
                    String __name__ = "release-vip-if-no-rules";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {

                        ModifyVipAttributesStruct struct = new ModifyVipAttributesStruct();
                        struct.setUseFor(PortForwardingConstant.PORTFORWARDING_NETWORK_SERVICE_TYPE);
                        Vip v = new Vip(inv.getVipUuid());
                        v.setStruct(struct);
                        v.release(new Completion(trigger){
                            @Override
                            public void success() {
                                logger.debug(String.format("delete backend for VIP[uuid:%s] from port forwarding", vo.getVipUuid()));
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                //TODO: GC this VIP
                                logger.warn(errorCode.toString());
                                trigger.next();
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(complete) {
                    @Override
                    public void handle(Map data) {
                        dbf.remove(vo);

                        CollectionUtils.safeForEach(revokeRuleExts, new ForEachFunction<RevokePortForwardingRuleExtensionPoint>() {
                            @Override
                            public void run(RevokePortForwardingRuleExtensionPoint extp) {
                                extp.afterRevokePortForwardingRule(inv, providerType);
                            }
                        });

                        logger.debug(String.format("successfully revoked port forwarding rule[uuid:%s]", inv.getUuid()));
                        complete.success();
                    }
                });

                error(new FlowErrorHandler(complete) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        CollectionUtils.safeForEach(revokeRuleExts, new ForEachFunction<RevokePortForwardingRuleExtensionPoint>() {
                            @Override
                            public void run(RevokePortForwardingRuleExtensionPoint extp) {
                                extp.failToRevokePortForwardingRule(inv, providerType);
                            }
                        });

                        logger.warn(String.format("failed to revoke port forwarding rule[uuid:%s] because %s", inv.getUuid(), errCode));
                        complete.fail(errCode);
                    }
                });
            }
        }).start();
    }

    private void handle(APIDeletePortForwardingRuleMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                String vipUuid = Q.New(PortForwardingRuleVO.class).eq(PortForwardingRuleVO_.uuid, msg.getUuid()).select(PortForwardingRuleVO_.vipUuid).findValue();
                return String.format("api-delete-portforwardingrule-vip-%s", vipUuid);
            }

            @Override
            public void run(SyncTaskChain chain) {
                final APIDeletePortForwardingRuleEvent evt = new APIDeletePortForwardingRuleEvent(msg.getId());
                removePortforwardingRule(msg.getUuid(), new Completion(msg) {
                    @Override
                    public void success() {
                        bus.publish(evt);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        evt.setError(errorCode);
                        bus.publish(evt);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("api-delete-portforwardingrule-%s", msg.getUuid());
            }
        });
    }


    private void handle(APICreatePortForwardingRuleMsg msg) {
        final APICreatePortForwardingRuleEvent evt = new APICreatePortForwardingRuleEvent(msg.getId());

        int vipPortEnd = msg.getVipPortEnd() == null ? msg.getVipPortStart() : msg.getVipPortEnd();
        int privatePortEnd = msg.getPrivatePortEnd() == null ? msg.getPrivatePortStart() : msg.getPrivatePortEnd();

        VipVO vip = dbf.findByUuid(msg.getVipUuid(), VipVO.class);
        final PortForwardingRuleVO vo = new PortForwardingRuleVO();
        if (msg.getResourceUuid() != null) {
            vo.setUuid(msg.getResourceUuid());
        } else {
            vo.setUuid(Platform.getUuid());
        }
        vo.setName(msg.getName());
        vo.setDescription(msg.getDescription());
        vo.setState(PortForwardingRuleState.Enabled);
        vo.setAllowedCidr(msg.getAllowedCidr());
        vo.setVipUuid(vip.getUuid());
        vo.setVipIp(vip.getIp());
        vo.setVipPortStart(msg.getVipPortStart());
        vo.setVipPortEnd(vipPortEnd);
        vo.setPrivatePortEnd(privatePortEnd);
        vo.setPrivatePortStart(msg.getPrivatePortStart());
        vo.setProtocolType(PortForwardingProtocolType.valueOf(msg.getProtocolType()));
        vo.setAccountUuid(msg.getSession().getAccountUuid());

        new SQLBatch() {
            @Override
            protected void scripts() {
                persist(vo);
                tagMgr.createTagsFromAPICreateMessage(msg, vo.getUuid(), PortForwardingRuleVO.class.getSimpleName());
            }
        }.execute();

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName("create-portforwading");
        VipInventory vipInventory = VipInventory.valueOf(vip);
        if (msg.getVmNicUuid() == null) {
            ModifyVipAttributesStruct struct = new ModifyVipAttributesStruct();
            struct.setUseFor(PortForwardingConstant.PORTFORWARDING_NETWORK_SERVICE_TYPE);
            Vip v = new Vip(vo.getVipUuid());
            v.setStruct(struct);
            v.acquire(new Completion(msg) {
                @Override
                public void success() {
                    evt.setInventory(PortForwardingRuleInventory.valueOf(vo));
                    bus.publish(evt);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    dbf.remove(vo);
                    evt.setError(errorCode);
                    bus.publish(evt);
                }
            });

            return;
        }

        VmNicVO vmNic = dbf.findByUuid(msg.getVmNicUuid(), VmNicVO.class);
        SimpleQuery<VmInstanceVO> q = dbf.createQuery(VmInstanceVO.class);
        q.select(VmInstanceVO_.state);
        q.add(VmInstanceVO_.uuid, Op.EQ, vmNic.getVmInstanceUuid());
        VmInstanceState vmState = q.findValue();
        if (VmInstanceState.Running != vmState) {
            ModifyVipAttributesStruct struct = new ModifyVipAttributesStruct();
            struct.setUseFor(PortForwardingConstant.PORTFORWARDING_NETWORK_SERVICE_TYPE);
            Vip v = new Vip(vo.getVipUuid());
            v.setStruct(struct);
            v.acquire(new Completion(msg) {
                @Override
                public void success() {
                    evt.setInventory(PortForwardingRuleInventory.valueOf(vo));
                    bus.publish(evt);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    dbf.remove(vo);
                    evt.setError(errorCode);
                    bus.publish(evt);
                }
            });

            return;
        }

        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                vo.setVmNicUuid(vmNic.getUuid());
                vo.setGuestIp(vmNic.getIp());
                final PortForwardingRuleVO pvo = dbf.updateAndRefresh(vo);
                final PortForwardingRuleInventory ruleInv = PortForwardingRuleInventory.valueOf(pvo);

                flow(new NoRollbackFlow() {
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        final NetworkServiceProviderType providerType = nwServiceMgr.getTypeOfNetworkServiceProviderForService(vmNic.getL3NetworkUuid(),
                                NetworkServiceType.PortForwarding);

                        for (AttachPortForwardingRuleExtensionPoint extp : attachRuleExts) {
                            try {
                                extp.preAttachPortForwardingRule(ruleInv, providerType);
                            } catch (PortForwardingException e) {
                                String err = String.format("unable to create port forwarding rule, extension[%s] refused it because %s", extp.getClass().getName(), e.getMessage());
                                logger.warn(err, e);
                                trigger.fail(errf.instantiateErrorCode(SysErrors.CREATE_RESOURCE_ERROR, err));
                                return;
                            }
                        }
                        data.put("providerType", providerType);
                        trigger.next();
                    }
                });

                flow(new NoRollbackFlow() {
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        NetworkServiceProviderType providerType = (NetworkServiceProviderType)data.get("providerType");
                        CollectionUtils.safeForEach(attachRuleExts, new ForEachFunction<AttachPortForwardingRuleExtensionPoint>() {
                            @Override
                            public void run(AttachPortForwardingRuleExtensionPoint extp) {
                                extp.beforeAttachPortForwardingRule(ruleInv, providerType);
                            }
                        });
                        trigger.next();
                    }
                });

                flow(new NoRollbackFlow() {
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        NetworkServiceProviderType providerType = (NetworkServiceProviderType)data.get("providerType");
                        final PortForwardingStruct struct = makePortForwardingStruct(ruleInv);
                        attachPortForwardingRule(struct, providerType.toString(), new Completion(msg) {
                            @Override
                            public void success() {
                                CollectionUtils.safeForEach(attachRuleExts, new ForEachFunction<AttachPortForwardingRuleExtensionPoint>() {
                                    @Override
                                    public void run(AttachPortForwardingRuleExtensionPoint extp) {
                                        extp.afterAttachPortForwardingRule(ruleInv, providerType);
                                    }
                                });
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                CollectionUtils.safeForEach(attachRuleExts, extp -> extp.failToAttachPortForwardingRule(ruleInv, providerType));

                                logger.debug(String.format("failed to create port forwarding rule %s, because %s", JSONObjectUtil.toJsonString(ruleInv), errorCode));

                /* pf is deleted, then release vip */
                                ModifyVipAttributesStruct vipStruct = new ModifyVipAttributesStruct();
                                vipStruct.setUseFor(PortForwardingConstant.PORTFORWARDING_NETWORK_SERVICE_TYPE);
                                Vip v = new Vip(struct.getVip().getUuid());
                                v.setStruct(vipStruct);
                                v.release(new NopeCompletion());
                                trigger.fail(errf.instantiateErrorCode(SysErrors.CREATE_RESOURCE_ERROR, errorCode));
                            }
                        });
                    }
                });
            }
        });


        chain.done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                evt.setInventory(PortForwardingRuleInventory.valueOf(dbf.reload(vo)));
                bus.publish(evt);
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                dbf.remove(vo);
                evt.setError(errCode);
                bus.publish(evt);
            }
        }).start();
    }

    private void populateExtensions() {
        for (PortForwardingBackend extp : pluginRgty.getExtensionList(PortForwardingBackend.class)) {
            PortForwardingBackend old = backends.get(extp.getProviderType().toString());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate PortForwardingBackend[%s, %s] for type[%s]",
                        extp.getClass().getName(), old.getClass().getName(), extp.getProviderType()));
            }
            backends.put(extp.getProviderType().toString(), extp);
        }

        attachRuleExts = pluginRgty.getExtensionList(AttachPortForwardingRuleExtensionPoint.class);
        revokeRuleExts = pluginRgty.getExtensionList(RevokePortForwardingRuleExtensionPoint.class);
    }

    @Override
    public boolean start() {
        populateExtensions();

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    public PortForwardingBackend getPortForwardingBackend(NetworkServiceProviderType nspType) {
        return getPortForwardingBackend(nspType.toString());
    }

    @Override
    public String getVipUse() {
        return PortForwardingConstant.PORTFORWARDING_NETWORK_SERVICE_TYPE;
    }

    private void releaseServicesOnVip(final Iterator<PortForwardingRuleVO> it, final Completion completion) {
        if (!it.hasNext()) {
            completion.success();
            return;
        }

        final PortForwardingRuleVO rule = it.next();
        if (rule.getVmNicUuid() == null) {
            dbf.remove(rule);
            releaseServicesOnVip(it, completion);
            return;
        }

        PortForwardingStruct struct = makePortForwardingStruct(PortForwardingRuleInventory.valueOf(rule));
        final NetworkServiceProviderType providerType = nwServiceMgr.getTypeOfNetworkServiceProviderForService(struct.getGuestL3Network().getUuid(),
                NetworkServiceType.PortForwarding);
        PortForwardingBackend bkd = getPortForwardingBackend(providerType);
        bkd.revokePortForwardingRule(struct, new Completion(completion) {
            @Override
            public void success() {
                dbf.remove(rule);
                releaseServicesOnVip(it, completion);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public void releaseServicesOnVip(VipInventory vip, Completion complete) {
        SimpleQuery<PortForwardingRuleVO> q = dbf.createQuery(PortForwardingRuleVO.class);
        q.add(PortForwardingRuleVO_.vipUuid, Op.EQ, vip.getUuid());
        List<PortForwardingRuleVO> rules = q.list();
        releaseServicesOnVip(rules.iterator(), complete);
    }

    private PortForwardingStruct makePortForwardingStruct(PortForwardingRuleInventory rule) {
        VipVO vipvo = dbf.findByUuid(rule.getVipUuid(), VipVO.class);

        L3NetworkVO vipL3vo = dbf.findByUuid(vipvo.getL3NetworkUuid(), L3NetworkVO.class);
        VmNicVO nic = dbf.findByUuid(rule.getVmNicUuid(), VmNicVO.class);
        L3NetworkVO guestL3vo = dbf.findByUuid(nic.getL3NetworkUuid(), L3NetworkVO.class);

        PortForwardingStruct struct = new PortForwardingStruct();
        struct.setRule(rule);
        struct.setVip(VipInventory.valueOf(vipvo));
        struct.setGuestIp(nic.getIp());
        struct.setGuestMac(nic.getMac());
        struct.setGuestL3Network(L3NetworkInventory.valueOf(guestL3vo));
        struct.setSnatInboundTraffic(PortForwardingGlobalConfig.SNAT_INBOUND_TRAFFIC.value(Boolean.class));
        struct.setVipL3Network(L3NetworkInventory.valueOf(vipL3vo));

        return struct;
    }

    @Override
    public PortForwardingBackend getPortForwardingBackend(String providerType) {
        PortForwardingBackend bkd = backends.get(providerType);
        DebugUtils.Assert(bkd != null, String.format("cannot find PortForwardingBackend[type:%s]", providerType));
        return bkd;
    }

    @Override
    public void attachPortForwardingRule(PortForwardingStruct struct, String providerType, final Completion completion) {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("attach-portforwarding-%s-vm-nic-%s", struct.getRule().getUuid(), struct.getRule().getVmNicUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "prepare-vip";

                    boolean s = false;

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        ModifyVipAttributesStruct vipStruct = new ModifyVipAttributesStruct();
                        vipStruct.setUseFor(PortForwardingConstant.PORTFORWARDING_NETWORK_SERVICE_TYPE);
                        vipStruct.setServiceProvider(providerType);
                        vipStruct.setPeerL3NetworkUuid(struct.getGuestL3Network().getUuid());
                        Vip vip = new Vip(struct.getVip().getUuid());
                        vip.setStruct(vipStruct);
                        vip.acquire(new Completion(trigger) {
                            @Override
                            public void success() {
                                s = true;
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
                    String __name__ = "attach-portfowarding-rule";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        PortForwardingBackend bkd = getPortForwardingBackend(providerType);
                        bkd.applyPortForwardingRule(struct, new Completion(trigger) {
                            @Override
                            public void success() {
                                logger.debug(String.format("successfully attached %s", struct.toString()));
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    @Override
    public void detachPortForwardingRule(final PortForwardingStruct struct, String providerType, final Completion completion) {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("detach-portforwarding-%s-vm-nic-%s", struct.getRule().getUuid(), struct.getRule().getVmNicUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "detach-portforwarding-rule";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        PortForwardingBackend bkd = getPortForwardingBackend(providerType);
                        bkd.revokePortForwardingRule(struct, new Completion(trigger) {
                            @Override
                            public void success() {
                                logger.debug(String.format("successfully detached %s", struct.toString()));
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        if (struct.isReleaseVmNicInfoWhenDetaching()) {
                            PortForwardingRuleVO vo = dbf.findByUuid(struct.getRule().getUuid(), PortForwardingRuleVO.class);
                            vo.setVmNicUuid(null);
                            vo.setGuestIp(null);
                            dbf.updateAndRefresh(vo);
                        }

                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    @Override
    public List<ExpandedQueryStruct> getExpandedQueryStructs() {
        List<ExpandedQueryStruct> structs = new ArrayList<ExpandedQueryStruct>();

        ExpandedQueryStruct struct = new ExpandedQueryStruct();
        struct.setInventoryClassToExpand(VmNicInventory.class);
        struct.setExpandedField("portForwarding");
        struct.setInventoryClass(PortForwardingRuleInventory.class);
        struct.setForeignKey("uuid");
        struct.setExpandedInventoryKey("vmNicUuid");
        structs.add(struct);

        struct = new ExpandedQueryStruct();
        struct.setInventoryClassToExpand(VipInventory.class);
        struct.setExpandedField("portForwarding");
        struct.setInventoryClass(PortForwardingRuleInventory.class);
        struct.setForeignKey("uuid");
        struct.setExpandedInventoryKey("vipUuid");
        structs.add(struct);

        return structs;
    }

    @Override
    public List<ExpandedQueryAliasStruct> getExpandedQueryAliasesStructs() {
        return null;
    }

    @Override
    public List<Quota> reportQuota() {
        QuotaOperator checker = new QuotaOperator() {
            @Override
            public void checkQuota(APIMessage msg, Map<String, QuotaPair> pairs) {
                if (!new QuotaUtil().isAdminAccount(msg.getSession().getAccountUuid())) {
                    if (msg instanceof APICreatePortForwardingRuleMsg) {
                        check((APICreatePortForwardingRuleMsg) msg, pairs);
                    }
                }
            }

            @Override
            public void checkQuota(NeedQuotaCheckMessage msg, Map<String, QuotaPair> pairs) {

            }

            @Override
            public List<Quota.QuotaUsage> getQuotaUsageByAccount(String accountUuid) {
                Quota.QuotaUsage usage = new Quota.QuotaUsage();
                usage.setName(PortForwardingQuotaConstant.PF_NUM);
                usage.setUsed(getUsedPf(accountUuid));
                return list(usage);
            }

            @Transactional(readOnly = true)
            private long getUsedPf(String accountUuid) {
                String sql = "select count(pf) from PortForwardingRuleVO pf, AccountResourceRefVO ref where pf.uuid = ref.resourceUuid" +
                        " and ref.accountUuid = :auuid and ref.resourceType = :rtype";
                TypedQuery<Long> q = dbf.getEntityManager().createQuery(sql, Long.class);
                q.setParameter("auuid", accountUuid);
                q.setParameter("rtype", PortForwardingRuleVO.class.getSimpleName());
                Long pfn = q.getSingleResult();
                pfn = pfn == null ? 0 : pfn;
                return pfn;
            }

            private void check(APICreatePortForwardingRuleMsg msg, Map<String, QuotaPair> pairs) {
                long pfNum = pairs.get(PortForwardingQuotaConstant.PF_NUM).getValue();
                long pfn = getUsedPf(msg.getSession().getAccountUuid());

                if (pfn + 1 > pfNum) {
                    throw new ApiMessageInterceptionException(new QuotaUtil().buildQuataExceedError(
                                    msg.getSession().getAccountUuid(), PortForwardingQuotaConstant.PF_NUM, pfNum));
                }
            }
        };

        Quota quota = new Quota();
        quota.setOperator(checker);
        quota.addMessageNeedValidation(APICreatePortForwardingRuleMsg.class);

        QuotaPair p = new QuotaPair();
        p.setName(PortForwardingQuotaConstant.PF_NUM);
        p.setValue(PortFowardingQuotaGlobalConfig.PF_NUM.defaultValue(Long.class));
        quota.addPair(p);
        return list(quota);
    }

    @Override
    public RangeSet getVipUsePortRange(String vipUuid, String protocol, VipUseForList useForList){
        RangeSet portRangeList = new RangeSet();
        List<RangeSet.Range> portRanges = new ArrayList<RangeSet.Range>();

        if (protocol.toUpperCase().equals(PortForwardingProtocolType.UDP.toString()) || protocol.toUpperCase().equals(PortForwardingProtocolType.TCP.toString())) {
            List<Tuple> pfPortList = Q.New(PortForwardingRuleVO.class).select(PortForwardingRuleVO_.vipPortStart, PortForwardingRuleVO_.vipPortEnd)
                    .eq(PortForwardingRuleVO_.vipUuid, vipUuid).eq(PortForwardingRuleVO_.protocolType, PortForwardingProtocolType.valueOf(protocol.toUpperCase())).listTuple();
            Iterator<Tuple> it = pfPortList.iterator();
            while (it.hasNext()){
                Tuple strRange = it.next();
                int start = strRange.get(0, Integer.class);
                int end = strRange.get(1, Integer.class);

                RangeSet.Range range = new RangeSet.Range(start, end);
                portRanges.add(range);
            }
        }

        portRangeList.setRanges(portRanges);
        return portRangeList;
    }

    @Override
    public ServiceReference getServiceReference(String vipUuid) {
        long count = Q.New(PortForwardingRuleVO.class).eq(PortForwardingRuleVO_.vipUuid, vipUuid).count();
        return new VipGetServiceReferencePoint.ServiceReference(PortForwardingConstant.PORTFORWARDING_NETWORK_SERVICE_TYPE, count);
    }
}
