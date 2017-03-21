package org.zstack.network.service.virtualrouter.eip;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.eip.EipConstant;
import org.zstack.network.service.eip.EipGlobalConfig;
import org.zstack.network.service.virtualrouter.*;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands.SyncEipRsp;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant.Param;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.operr;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VirtualRouterSyncEipOnStartFlow implements Flow {
    private static CLogger logger = Utils.getLogger(VirtualRouterSyncEipOnStartFlow.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private VirtualRouterManager vrMgr;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private ApiTimeoutManager apiTimeoutManager;

    @Transactional(readOnly = true)
    private List<EipTO> findEipOnThisRouter(VirtualRouterVmInventory vr, List<String> eipUuids) {
        String sql = "select vip.ip, nic.l3NetworkUuid, nic.ip from EipVO eip, VipVO vip, VmNicVO nic, VmInstanceVO vm where nic.vmInstanceUuid = vm.uuid and vm.state = :vmState and eip.vipUuid = vip.uuid and eip.vmNicUuid = nic.uuid and eip.uuid in (:euuids)";
        TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
        q.setParameter("euuids", eipUuids);
        q.setParameter("vmState", VmInstanceState.Running);
        List<Tuple> tuples =  q.getResultList();
        List<EipTO> ret = new ArrayList<EipTO>();
        for (Tuple t : tuples) {
            String vipIp = t.get(0, String.class);
            final String l3Uuid = t.get(1, String.class);
            String guestIp = t.get(2, String.class);
            String privMac = CollectionUtils.find(vr.getVmNics(), new Function<String, VmNicInventory>() {
                @Override
                public String call(VmNicInventory arg) {
                    if (arg.getL3NetworkUuid().equals(l3Uuid)) {
                        return arg.getMac();
                    }
                    return null;
                }
            });

            DebugUtils.Assert(privMac!=null, String.format("cannot find private nic[l3NetworkUuid:%s] on virtual router[uuid:%s]",
                    l3Uuid, vr.getUuid()));
            EipTO to = new EipTO();
            to.setVipIp(vipIp);
            to.setGuestIp(guestIp);
            to.setPrivateMac(privMac);
            to.setSnatInboundTraffic(EipGlobalConfig.SNAT_INBOUND_TRAFFIC.value(Boolean.class));
            ret.add(to);
        }

        return ret;
    }

    private List<EipTO> findEipOnThisRouter(final VirtualRouterVmInventory vr, Map<String, Object> data, boolean isNewCreated) {
        List<String> eipUuids;
        if (isNewCreated) {
            final VmNicInventory guestNic = vr.getGuestNic();
            final VmNicInventory publicNic = vr.getPublicNic();

            eipUuids = new Callable<List<String>>() {
                @Override
                @Transactional(readOnly = true)
                public List<String> call() {
                    String sql = "select eip.uuid from EipVO eip, VipVO vip, VmNicVO nic, VmInstanceVO vm where vm.uuid = nic.vmInstanceUuid and vm.state = :vmState and eip.vipUuid = vip.uuid and eip.vmNicUuid = nic.uuid and vip.l3NetworkUuid = :vipL3Uuid and nic.l3NetworkUuid = :guestL3Uuid";
                    TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
                    q.setParameter("vipL3Uuid", publicNic.getL3NetworkUuid());
                    q.setParameter("guestL3Uuid", guestNic.getL3NetworkUuid());
                    q.setParameter("vmState", VmInstanceState.Running);
                    return q.getResultList();
                }
            }.call();

            if (!eipUuids.isEmpty()) {
                List<VirtualRouterEipRefVO> refs = new ArrayList<VirtualRouterEipRefVO>();
                for (String eipUuid : eipUuids) {
                    VirtualRouterEipRefVO ref = new VirtualRouterEipRefVO();
                    ref.setEipUuid(eipUuid);
                    ref.setVirtualRouterVmUuid(vr.getUuid());
                    refs.add(ref);
                }

                dbf.persistCollection(refs);
                data.put(VirtualRouterSyncEipOnStartFlow.class.getName(), refs);
            }
        } else {
            SimpleQuery<VirtualRouterEipRefVO> q = dbf.createQuery(VirtualRouterEipRefVO.class);
            q.select(VirtualRouterEipRefVO_.eipUuid);
            q.add(VirtualRouterEipRefVO_.virtualRouterVmUuid, SimpleQuery.Op.EQ, vr.getUuid());
            eipUuids = q.listValue();
        }

        if (eipUuids.isEmpty()) {
            return new ArrayList<>();
        }

        return findEipOnThisRouter(vr, eipUuids);
    }

    public void run(final FlowTrigger trigger, Map data) {
        final VirtualRouterVmInventory vr = (VirtualRouterVmInventory) data.get(VirtualRouterConstant.Param.VR.toString());
        VmNicInventory guestNic = vr.getGuestNic();
        if (!vrMgr.isL3NetworkNeedingNetworkServiceByVirtualRouter(guestNic.getL3NetworkUuid(), EipConstant.EIP_NETWORK_SERVICE_TYPE)) {
            trigger.next();
            return;
        }

        if (VirtualRouterSystemTags.DEDICATED_ROLE_VR.hasTag(vr.getUuid()) && !VirtualRouterSystemTags.VR_EIP_ROLE.hasTag(vr.getUuid())) {
            trigger.next();
            return;
        }

        new VirtualRouterRoleManager().makeEipRole(vr.getUuid());

        boolean isNewCreated = data.containsKey(Param.IS_NEW_CREATED.toString());
        List<EipTO> eips = findEipOnThisRouter(vr, data, isNewCreated);
        if (eips.isEmpty()) {
            trigger.next();
            return;
        }

        VirtualRouterCommands.SyncEipCmd cmd = new VirtualRouterCommands.SyncEipCmd();
        cmd.setEips(eips);

        VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
        msg.setPath(VirtualRouterConstant.VR_SYNC_EIP);
        msg.setCommand(cmd);
        msg.setCommandTimeout(apiTimeoutManager.getTimeout(cmd.getClass(), "30m"));
        msg.setVmInstanceUuid(vr.getUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vr.getUuid());
        bus.send(msg, new CloudBusCallBack(trigger) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    trigger.fail(reply.getError());
                    return;
                }

                VirtualRouterAsyncHttpCallReply re = reply.castReply();
                SyncEipRsp ret = re.toResponse(SyncEipRsp.class);
                if (!ret.isSuccess()) {
                    ErrorCode err = operr("failed to sync eip on virtual router[uuid:%s], %s",
                            vr.getUuid(), ret.getError());
                    trigger.fail(err);
                } else {
                    String info = String.format("failed to sync eip on virtual router[uuid:%s]",
                            vr.getUuid());
                    logger.debug(info);
                    trigger.next();
                }
            }
        });
    }

    @Override
    public void rollback(FlowRollback trigger, Map data) {
        List<VirtualRouterEipRefVO> refs = (List<VirtualRouterEipRefVO>) data.get(VirtualRouterSyncEipOnStartFlow.class.getName());
        if (refs != null) {
            dbf.removeCollection(refs, VirtualRouterEipRefVO.class);
        }

        trigger.rollback();
    }
}
