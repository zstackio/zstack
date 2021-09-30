package org.zstack.network.service.virtualrouter.nat;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.appliancevm.ApplianceVm;
import org.zstack.appliancevm.ApplianceVmFactory;
import org.zstack.appliancevm.ApplianceVmSubTypeFactory;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.Q;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.L3NetworkVO_;
import org.zstack.header.network.l3.UsedIpInventory;
import org.zstack.header.network.service.*;
import org.zstack.header.vm.*;
import org.zstack.network.service.NetworkServiceManager;
import org.zstack.network.service.vip.ModifyVipAttributesStruct;
import org.zstack.network.service.vip.Vip;
import org.zstack.network.service.vip.VipVO;
import org.zstack.network.service.vip.VipVO_;
import org.zstack.network.service.virtualrouter.*;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands.RemoveSNATRsp;
import org.zstack.network.service.virtualrouter.ha.VirtualRouterHaBackend;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6Constants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.zstack.core.Platform.operr;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 10:37 PM
 * To change this template use File | Settings | File Templates.
 */
public class VirtualRouterSnatBackend extends AbstractVirtualRouterBackend implements
        NetworkServiceSnatBackend, VirtualRouterAfterAttachNicExtensionPoint, VirtualRouterBeforeDetachNicExtensionPoint {
    private static final CLogger logger = Utils.getLogger(VirtualRouterSnatBackend.class);

    @Autowired
    private ErrorFacade errf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ApiTimeoutManager apiTimeoutManager;
    @Autowired
    private NetworkServiceManager nwServiceMgr;
    @Autowired
    private VirtualRouterManager vrMgr;
    @Autowired
    protected ThreadFacade thdf;
    @Autowired
    protected PluginRegistry pluginRgty;
    @Autowired
    protected VirtualRouterHaBackend haBackend;
    @Autowired
    private ApplianceVmFactory apvmFactory;

    @Override
    public NetworkServiceProviderType getProviderType() {
        return VirtualRouterConstant.PROVIDER_TYPE;
    }

    private void applySnat(final Iterator<SnatStruct> it, final VmInstanceSpec spec, final Completion completion) {
        if (!it.hasNext()) {
            completion.success();
            return;
        }

        final SnatStruct struct = it.next();
        final L3NetworkInventory guestL3 = struct.getL3Network();

        VirtualRouterStruct s = new VirtualRouterStruct();
        s.setL3Network(guestL3);
        s.setOfferingValidator(new VirtualRouterOfferingValidator() {
            @Override
            public void validate(VirtualRouterOfferingInventory offering) throws OperationFailureException {
                if (offering.getPublicNetworkUuid().equals(guestL3.getUuid())) {
                    throw new OperationFailureException(operr("guest l3Network[uuid:%s, name:%s] needs SNAT service provided by virtual router, but public l3Network[uuid:%s] of virtual router offering[uuid: %s, name:%s] is the same to this guest l3Network",
                            guestL3.getUuid(), guestL3.getName(), offering.getPublicNetworkUuid(), offering.getUuid(), offering.getName()));
                }
            }
        });

        acquireVirtualRouterVm(s, new ReturnValueCompletion<VirtualRouterVmInventory>(completion) {
            @Override
            public void success(final VirtualRouterVmInventory vr) {
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public void applySnatService(List<SnatStruct> snatStructList, VmInstanceSpec spec, Completion completion) {
        if (snatStructList.isEmpty()) {
            completion.success();
            return;
        }

        applySnat(snatStructList.iterator(), spec, completion);
    }

    private String getNicIpv4Address(VmNicInventory nic) {
        for (UsedIpInventory ip : nic.getUsedIps()) {
            if (ip.getIpVersion() == IPv6Constants.IPv4) {
                return ip.getIp();
            }
        }
        return null;
    }

    private void releaseSnat(final Iterator<SnatStruct> it, final VmInstanceInventory vmInstanceInventory, final NoErrorCompletion completion) {
        if (!it.hasNext()) {
            completion.done();
            return;
        }

        final SnatStruct struct = it.next();
        VirtualRouterVmVO vrVO = Q.New(VirtualRouterVmVO.class).eq(VirtualRouterVmVO_.uuid, vmInstanceInventory.getUuid()).find();
        VirtualRouterVmInventory vrInv = null;
        if (vrVO != null) {
            vrInv = VirtualRouterVmInventory.valueOf(vrVO);
        } else {
            vrInv = vrMgr.getVirtualRouterVm(struct.getL3Network());
        }
        final VirtualRouterVmInventory vr = vrInv;

        ApplianceVmSubTypeFactory subTypeFactory = apvmFactory.getApplianceVmSubTypeFactory(vrVO.getApplianceVmType());
        ApplianceVm app = subTypeFactory.getSubApplianceVm(vrVO);
        /*
         * snat disabled and skip directly by zhanyong.miao ZSTAC-18373
         * */
        if (!app.getSnatStateOnRouter(vrVO.getUuid())) {
            releaseSnat(it, vmInstanceInventory, completion);
            return;
        }

        VmNicInventory privateNic = CollectionUtils.find(vr.getVmNics(), new Function<VmNicInventory, VmNicInventory>() {
            @Override
            public VmNicInventory call(VmNicInventory arg) {
                if (arg.getL3NetworkUuid().equals(struct.getL3Network().getUuid())) {
                    return arg;
                }
                return null;
            }
        });
        DebugUtils.Assert(privateNic!=null, String.format("cannot find private nic[ip:%s] on virtual router[uuid:%s, name:%s]",
                struct.getGuestGateway(), vr.getUuid(), vr.getName()));
        List<String> l3Uuids = app.getSnatL3NetworkOnRouter(vrVO.getUuid());
        List<VmNicInventory> publicNicList = new ArrayList<>();
        l3Uuids.forEach(l3 -> {
            publicNicList.add(vrMgr.getSnatPubicInventoryByUuid(vr, l3));
        });
        publicNicList.removeIf(filter -> getNicIpv4Address(privateNic) == null);
        if (publicNicList.isEmpty() || privateNic.isIpv6OnlyNic()) {
            /* only ipv4 has snat */
            releaseSnat(it, vmInstanceInventory, completion);
            return;
        }
        final List<VirtualRouterCommands.SNATInfo> snatInfo = new ArrayList<VirtualRouterCommands.SNATInfo>();
        publicNicList.forEach(publicNic -> {
            VirtualRouterCommands.SNATInfo info = new VirtualRouterCommands.SNATInfo();
            info.setPrivateNicIp(privateNic.getIp());
            info.setPrivateNicMac(privateNic.getMac());
            info.setPublicIp(publicNic.getIp());
            info.setPublicNicMac(publicNic.getMac());
            info.setSnatNetmask(privateNic.getNetmask());
            snatInfo.add(info);
        });
        VirtualRouterCommands.RemoveSNATCmd cmd = new VirtualRouterCommands.RemoveSNATCmd();
        cmd.setNatInfo(snatInfo);

        VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
        msg.setCheckStatus(true);
        msg.setVmInstanceUuid(vr.getUuid());
        msg.setCommand(cmd);
        msg.setPath(VirtualRouterConstant.VR_REMOVE_SNAT_PATH);
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vr.getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("failed to release snat[%s] on virtual router[name:%s, uuid:%s] for vm[uuid: %s, name: %s], %s",
                            struct, vr.getName(), vr.getUuid(), vmInstanceInventory.getUuid(), vmInstanceInventory.getName(), reply.getError()));
                    //TODO GC
                } else {
                    VirtualRouterAsyncHttpCallReply re = reply.castReply();
                    RemoveSNATRsp ret = re.toResponse(RemoveSNATRsp.class);
                    if (!ret.isSuccess()) {
                        String err = String.format(
                                "virtual router[uuid:%s, ip:%s] failed to release snat[%s] for vm[uuid:%s, name:%s] on L3Network[uuid:%s, name:%s], because %s",
                                vr.getUuid(), vr.getManagementNic().getIp(), JSONObjectUtil.toJsonString(snatInfo), vmInstanceInventory.getUuid(), vmInstanceInventory.getName(),
                                struct.getL3Network().getUuid(), struct.getL3Network().getName(), ret.getError());
                        logger.warn(err);
                        //TODO GC
                    } else {
                        String msg = String.format(
                                "virtual router[uuid:%s, ip:%s] released snat[%s] for vm[uuid:%s, name:%s] on L3Network[uuid:%s, name:%s], because %s",
                                vr.getUuid(), vr.getManagementNic().getIp(), JSONObjectUtil.toJsonString(snatInfo), vmInstanceInventory.getUuid(), vmInstanceInventory.getName(),
                                struct.getL3Network().getUuid(), struct.getL3Network().getName(), ret.getError());
                        logger.warn(msg);
                    }
                }

                releaseSnat(it, vmInstanceInventory, completion);
            }
        });
    }

    @Override
    public void releaseSnatService(List<SnatStruct> snatStructList, VmInstanceSpec spec, NoErrorCompletion completion) {
        completion.done();
    }

    @Override
    public void beforeDetachNic(VmNicInventory nic, Completion completion) {
        if (!VirtualRouterNicMetaData.GUEST_NIC_MASK_STRING_LIST.contains(nic.getMetaData())) {
            completion.success();
            return;
        }

        try {
            nwServiceMgr.getTypeOfNetworkServiceProviderForService(nic.getL3NetworkUuid(), NetworkServiceType.SNAT);
        } catch (OperationFailureException e) {
            completion.success();
            return;
        }

        L3NetworkInventory l3 = L3NetworkInventory.valueOf(
                (L3NetworkVO) Q.New(L3NetworkVO.class)
                        .eq(L3NetworkVO_.uuid, nic.getL3NetworkUuid())
                        .find());

        SnatStruct struct = new SnatStruct();
        struct.setL3Network(l3);
        struct.setGuestGateway(nic.getGateway());
        struct.setGuestIp(nic.getIp());
        struct.setGuestMac(nic.getMac());
        struct.setGuestNetmask(nic.getNetmask());

        VmInstanceInventory vm = VmInstanceInventory.valueOf(
                (VmInstanceVO) Q.New(VmInstanceVO.class)
                        .eq(VmInstanceVO_.uuid, nic.getVmInstanceUuid())
                        .find());

        releaseSnat(Arrays.asList(struct).iterator(), vm, new NoErrorCompletion() {
            @Override
            public void done() {
                completion.success();
            }
        });
    }

    @Override
    public void beforeDetachNicRollback(VmNicInventory nic, NoErrorCompletion completion) {
        completion.done();
    }

    @Override
    public void afterAttachNic(VmNicInventory nic, Completion completion) {
        if (!VirtualRouterNicMetaData.GUEST_NIC_MASK_STRING_LIST.contains(nic.getMetaData())) {
            completion.success();
            return;
        }

        VirtualRouterVmVO vrVO = Q.New(VirtualRouterVmVO.class).eq(VirtualRouterVmVO_.uuid, nic.getVmInstanceUuid()).find();
        DebugUtils.Assert(vrVO != null,
                String.format("can not find virtual router[uuid: %s] for nic[uuid: %s, ip: %s, l3NetworkUuid: %s]",
                        nic.getVmInstanceUuid(), nic.getUuid(), nic.getIp(), nic.getL3NetworkUuid()));

        ApplianceVmSubTypeFactory subTypeFactory = apvmFactory.getApplianceVmSubTypeFactory(vrVO.getApplianceVmType());
        ApplianceVm app = subTypeFactory.getSubApplianceVm(vrVO);
        /*
         * snat disabled and skip directly by zhanyong.miao ZSTAC-18373
         * */
        if (!app.getSnatStateOnRouter(vrVO.getUuid())) {
            completion.success();
            return;
        }

        VirtualRouterVmInventory vr = VirtualRouterVmInventory.valueOf(vrVO);

        List<String> nwServed = vr.getAllL3Networks();
        nwServed = vrMgr.selectL3NetworksNeedingSpecificNetworkService(nwServed, NetworkServiceType.SNAT);
        if (nwServed.isEmpty()) {
            completion.success();
            return;
        }

        new VirtualRouterRoleManager().makeSnatRole(vr.getUuid());

        VmNicInventory publicNic = vrMgr.getSnatPubicInventory(vr);
        String publicIpv4 = getNicIpv4Address(publicNic);
        if (publicIpv4 == null) {
            /* only ipv4 has snat */
            completion.success();
            return;
        }

        final List<VirtualRouterCommands.SNATInfo> snatInfo = new ArrayList<VirtualRouterCommands.SNATInfo>();
        for (VmNicInventory vnic : vr.getVmNics()) {
            if (nwServed.contains(vnic.getL3NetworkUuid()) && !vnic.isIpv6OnlyNic()) {
                VirtualRouterCommands.SNATInfo info = new VirtualRouterCommands.SNATInfo();
                info.setPrivateNicIp(vnic.getIp());
                info.setPrivateNicMac(vnic.getMac());
                info.setPublicIp(publicIpv4);
                info.setPublicNicMac(publicNic.getMac());
                info.setSnatNetmask(vnic.getNetmask());
                snatInfo.add(info);
            }
        }

        if (snatInfo.isEmpty()) {
            completion.success();
            return;
        }

        VirtualRouterCommands.SyncSNATCmd cmd = new VirtualRouterCommands.SyncSNATCmd();
        cmd.setSnats(snatInfo);
        cmd.setEnable(true);
        VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
        msg.setPath(VirtualRouterConstant.VR_SYNC_SNAT_PATH);
        msg.setCommand(cmd);
        msg.setVmInstanceUuid(vr.getUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vr.getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                VirtualRouterAsyncHttpCallReply re = reply.castReply();
                VirtualRouterCommands.SyncSNATRsp ret = re.toResponse(VirtualRouterCommands.SyncSNATRsp.class);
                if (!ret.isSuccess()) {
                    ErrorCode err = operr("virtual router[name: %s, uuid: %s] failed to sync snat%s, %s",
                            vr.getName(), vr.getUuid(), JSONObjectUtil.toJsonString(snatInfo), ret.getError());
                    completion.fail(err);
                    return;
                }

                Vip vip = getVipWithSnatService(vr, nic);
                if (vip == null) {
                    completion.success();
                    return;
                }

                vip.acquire(new Completion(completion) {
                    @Override
                    public void success() {
                        completion.success();
                    }
                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                    }
                });
            }
        });
    }

    private Vip getVipWithSnatService(VirtualRouterVmInventory vr, VmNicInventory nic){
        String vipUuid = null;
        for (VirtualRouterHaGroupExtensionPoint ext : pluginRgty.getExtensionList(VirtualRouterHaGroupExtensionPoint.class)) {
            vipUuid = ext.getPublicIpUuid(vr.getUuid(), vr.getDefaultRouteL3NetworkUuid());
        }

        if (vipUuid == null) {
            VmNicInventory publicNic = vrMgr.getSnatPubicInventory(vr);
            vipUuid = Q.New(VipVO.class).eq(VipVO_.ip, publicNic.getIp()).select(VipVO_.uuid).findValue();
        }
        if (vipUuid == null){
            return null;
        }

        ModifyVipAttributesStruct struct = new ModifyVipAttributesStruct();
        struct.setUseFor(NetworkServiceType.SNAT.toString());
        struct.setServiceUuid(vr.getUuid());
        Vip vip = new Vip(vipUuid);
        String l3NetworkUuuid = nic.getL3NetworkUuid();
        try {
            NetworkServiceProviderType providerType = nwServiceMgr.getTypeOfNetworkServiceProviderForService(l3NetworkUuuid, NetworkServiceType.SNAT);
            struct.setPeerL3NetworkUuid(l3NetworkUuuid);
            struct.setServiceProvider(providerType.toString());
        } catch (OperationFailureException e){
            logger.debug(String.format("Get providerType exception %s", e.toString()));
        }
        vip.setStruct(struct);
        return vip;
    }

    @Override
    public void afterAttachNicRollback(VmNicInventory nic, NoErrorCompletion completion) {
        completion.done();
    }
}
