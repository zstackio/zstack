package org.zstack.network.service.virtualrouter.lb;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;
import org.stringtemplate.v4.ST;
import org.zstack.compute.vm.VmInstanceManager;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.header.vm.*;
import org.zstack.network.service.lb.*;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.network.service.vip.VipVO;
import org.zstack.network.service.vip.VipVO_;
import org.zstack.network.service.virtualrouter.*;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant.Param;
import org.zstack.network.service.virtualrouter.vip.VirtualRouterVipBackend;
import org.zstack.network.service.virtualrouter.vyos.VyosConstants;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.function.Function;
import org.zstack.utils.function.ListFunction;

import javax.persistence.TypedQuery;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * Created by frank on 8/17/2015.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VirtualRouterSyncLbOnStartFlow implements Flow {
    @Autowired
    private VirtualRouterManager vrMgr;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    @Qualifier("VirtualRouterLoadBalancerBackend")
    private VirtualRouterLoadBalancerBackend bkd;
    @Autowired
    @Qualifier("VirtualRouterVipBackend")
    protected VirtualRouterVipBackend vipExt;
    @Autowired
    private LbConfigProxy proxy;
    @Autowired
    private LoadBalancerManager lbMgr;

    @Override
    public void run(final FlowTrigger outterTrigger, final Map data) {
        final VirtualRouterVmInventory vr = (VirtualRouterVmInventory) data.get(VirtualRouterConstant.Param.VR.toString());
        final List<VmNicInventory> guestNics = vr.getGuestNics();
        if (guestNics == null || guestNics.isEmpty()) {
            outterTrigger.next();
            return;
        }
        List<String> l3Uuids = guestNics.stream().map(n -> n.getL3NetworkUuid()).collect(Collectors.toList());
        if (!vrMgr.isL3NetworksNeedingNetworkServiceByVirtualRouter(l3Uuids, LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING)) {
            outterTrigger.next();
            return;
        }

        if (VirtualRouterSystemTags.DEDICATED_ROLE_VR.hasTag(vr.getUuid()) && !VirtualRouterSystemTags.VR_LB_ROLE.hasTag(vr.getUuid())) {
            outterTrigger.next();
            return;
        }

        new VirtualRouterRoleManager().makeLoadBalancerRole(vr.getUuid());

        LoadBalancerType lbType = LoadBalancerType.Shared;
        LoadBalancerFactory factory = lbMgr.getLoadBalancerFactoryByApplianceVmType(vr.getApplianceVmType());
        if (factory != null) {
            lbType = LoadBalancerType.valueOf(factory.getType());
        }

        LoadBalancerType finalLbType = lbType;
        Collection<LoadBalancerVO> lbs = new Callable<List<LoadBalancerVO>>() {
            @Override
            @Transactional(readOnly = true)
            public List<LoadBalancerVO> call() {
                String sql = "select lb from LoadBalancerVO lb, LoadBalancerListenerVO l, LoadBalancerServerGroupVO g, " +
                        " LoadBalancerListenerServerGroupRefVO lgref, LoadBalancerServerGroupVmNicRefVO nicRef, VmNicVO nic, L3NetworkVO l3" +
                        " where lb.uuid = l.loadBalancerUuid and l.uuid = lgref.listenerUuid " +
                        " and lgref.serverGroupUuid = g.uuid and nicRef.serverGroupUuid= g.uuid " +
                        " and nicRef.vmNicUuid = nic.uuid and nic.l3NetworkUuid = l3.uuid" +
                        " and l3.uuid in (:l3uuids) and lb.state = :state and lb.uuid not in (select t.resourceUuid from SystemTagVO t" +
                        " where t.tag = :tag and t.resourceType = :rtype) and lb.type = :lbType";

                TypedQuery<LoadBalancerVO>  vq = dbf.getEntityManager().createQuery(sql, LoadBalancerVO.class);

                if (!data.containsKey(Param.IS_NEW_CREATED.toString())) {
                    // start/reboot the vr, handle the case that it is the separate lb vr
                    List<String> lbuuids = proxy.getServiceUuidsByRouterUuid(vr.getUuid(), LoadBalancerVO.class.getSimpleName());

                    if (!lbuuids.isEmpty()) {
                        sql = "select lb from LoadBalancerVO lb, LoadBalancerListenerVO l, LoadBalancerServerGroupVO g, " +
                                " LoadBalancerListenerServerGroupRefVO lgref, LoadBalancerServerGroupVmNicRefVO nicRef, VmNicVO nic, L3NetworkVO l3" +
                                " where lb.uuid = l.loadBalancerUuid and l.uuid = lgref.listenerUuid " +
                                " and lgref.serverGroupUuid = g.uuid and nicRef.serverGroupUuid= g.uuid " +
                                " and nicRef.vmNicUuid = nic.uuid and nic.l3NetworkUuid = l3.uuid" +
                                " and l3.uuid in (:l3uuids) and lb.state = :state and lb.uuid not in (select t.resourceUuid from SystemTagVO t" +
                                " where t.tag = :tag and t.resourceType = :rtype and t.resourceUuid not in (:mylbs)) and lb.type = :lbType";
                        vq = dbf.getEntityManager().createQuery(sql, LoadBalancerVO.class);
                        vq.setParameter("mylbs", lbuuids);
                    }
                }

                vq.setParameter("tag", LoadBalancerSystemTags.SEPARATE_VR.getTagFormat());
                vq.setParameter("rtype", LoadBalancerVO.class.getSimpleName());
                vq.setParameter("state", LoadBalancerState.Enabled);
                vq.setParameter("l3uuids", l3Uuids);
                vq.setParameter("lbType", finalLbType);
                return vq.getResultList();
            }
        }.call();

        if (lbs.isEmpty()) {
            outterTrigger.next();
            return;
        }

        Map<String, LoadBalancerVO> tmp = new HashMap<>();
        lbs.forEach(vo -> tmp.put(String.format("%s-%s", vo.getUuid(), vr.getUuid()), vo));
        lbs = tmp.values();

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("sync-lb-on-vr-%s", vr.getUuid()));
        Collection<LoadBalancerVO> finalLbs = lbs;
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "create-vip-for-lbs";

                    private void createVip(final Iterator<VipInventory> it, final FlowTrigger trigger) {
                        if (!it.hasNext()) {
                            trigger.next();
                            return;
                        }

                        vipExt.acquireVipOnVirtualRouterVm(vr, it.next(), new Completion(trigger) {
                            @Override
                            public void success() {
                                createVip(it, trigger);
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        /* other virtual router doesn't need this operation */
                        if (!vr.getApplianceVmType().equals(VyosConstants.VYOS_VM_TYPE)
                                && !vr.getApplianceVmType().equals(VirtualRouterConstant.VIRTUAL_ROUTER_PROVIDER_TYPE)){
                            trigger.next();
                            return;
                        }

                        SimpleQuery<VipVO> q = dbf.createQuery(VipVO.class);
                        q.add(VipVO_.uuid, Op.IN, CollectionUtils.transformToList(finalLbs, new ListFunction<String, LoadBalancerVO>() {
                            @Override
                            public List<String> call(LoadBalancerVO arg) {
                                ArrayList<String> vipUuids = new ArrayList<>();
                                if (!StringUtils.isEmpty(arg.getVipUuid())) {
                                    vipUuids.add(arg.getVipUuid());
                                }
                                if (!StringUtils.isEmpty(arg.getIpv6VipUuid())) {
                                    vipUuids.add(arg.getIpv6VipUuid());
                                }
                                return vipUuids;
                            }
                        }));
                        List<VipVO> vipvos = q.list();

                        createVip(VipInventory.valueOf(vipvos).iterator(), trigger);
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "create-lbs";

                    @Override
                    public void run(final FlowTrigger trigger, final Map data) {
                        List<LoadBalancerStruct> structs = new ArrayList<LoadBalancerStruct>();
                        for (LoadBalancerVO vo : finalLbs) {
                            structs.add(lbMgr.makeStruct(vo));
                        }

                        bkd.syncOnStart(vr, false, structs, new Completion(trigger) {
                            @Override
                            public void success() {
                                List<String> lbUuids = finalLbs.stream().map(LoadBalancerVO::getUuid).collect(Collectors.toList());
                                proxy.attachNetworkService(vr.getUuid(), LoadBalancerVO.class.getSimpleName(), lbUuids);
                                data.put(VirtualRouterSyncLbOnStartFlow.class, lbUuids);
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(outterTrigger) {
                    @Override
                    public void handle(Map data) {
                        outterTrigger.next();
                    }
                });

                error(new FlowErrorHandler(outterTrigger) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        outterTrigger.fail(errCode);
                    }
                });
            }
        }).start();
    }

    @Override
    public void rollback(FlowRollback trigger, Map data) {
        final VirtualRouterVmInventory vr = (VirtualRouterVmInventory) data.get(VirtualRouterConstant.Param.VR.toString());
        List<String> lbUuids = (List<String>) data.get(VirtualRouterSyncLbOnStartFlow.class);
        if (lbUuids != null) {
            proxy.detachNetworkService(vr.getUuid(), LoadBalancerVO.class.getSimpleName(), lbUuids);
        }
        trigger.rollback();
    }
}
