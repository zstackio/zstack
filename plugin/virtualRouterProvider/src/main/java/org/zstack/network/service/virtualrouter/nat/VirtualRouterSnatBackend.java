package org.zstack.network.service.virtualrouter.nat;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
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
import org.zstack.header.network.service.*;
import org.zstack.header.vm.*;
import org.zstack.network.service.NetworkServiceManager;
import org.zstack.network.service.vip.ModifyVipAttributesStruct;
import org.zstack.network.service.vip.Vip;
import org.zstack.network.service.vip.VipVO;
import org.zstack.network.service.vip.VipVO_;
import org.zstack.network.service.virtualrouter.*;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands.RemoveSNATRsp;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands.SetSNATRsp;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

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
                /*
                 * snat disabled and skip directly by zhanyong.miao ZSTAC-18373
                 */
                if ( VirtualRouterSystemTags.VR_DISABLE_NETWORK_SERVICE_SNAT.hasTag(vr.getUuid())) {
                    completion.success();
                    return;
                }

                final VirtualRouterCommands.SNATInfo info = new VirtualRouterCommands.SNATInfo();
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

                info.setPrivateNicIp(privateNic.getIp());
                info.setPrivateNicMac(privateNic.getMac());
                info.setPublicNicMac(vr.getPublicNic().getMac());
                info.setPublicIp(vr.getPublicNic().getIp());
                info.setSnatNetmask(struct.getGuestNetmask());

                VirtualRouterCommands.SetSNATCmd cmd = new VirtualRouterCommands.SetSNATCmd();
                cmd.setSnat(info);

                VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
                msg.setVmInstanceUuid(vr.getUuid());
                msg.setPath(VirtualRouterConstant.VR_SET_SNAT_PATH);
                msg.setCommand(cmd);
                msg.setCheckStatus(true);
                bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vr.getUuid());
                bus.send(msg, new CloudBusCallBack(completion) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            completion.fail(reply.getError());
                            return;
                        }

                        VirtualRouterAsyncHttpCallReply re = reply.castReply();
                        SetSNATRsp ret = re.toResponse(SetSNATRsp.class);
                        if (!ret.isSuccess()) {
                            new VirtualRouterRoleManager().makeSnatRole(vr.getUuid());

                            ErrorCode err = operr("virtual router[uuid:%s, ip:%s] failed to apply snat[%s] for vm[uuid:%s, name:%s] on L3Network[uuid:%s, name:%s], because %s",
                                    vr.getUuid(), vr.getManagementNic().getIp(), JSONObjectUtil.toJsonString(info), spec.getVmInventory().getUuid(), spec.getVmInventory().getName(),
                                    struct.getL3Network().getUuid(), struct.getL3Network().getName(), ret.getError());
                            completion.fail(err);
                        } else {
                            applySnat(it, spec, completion);
                        }
                    }
                });
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

    private void releaseSnat(final Iterator<SnatStruct> it, final VmInstanceInventory vmInstanceInventory, final NoErrorCompletion completion) {
        if (!it.hasNext()) {
            completion.done();
            return;
        }

        final SnatStruct struct = it.next();
        final VirtualRouterVmInventory vr = vrMgr.getVirtualRouterVm(struct.getL3Network());
        /*
         * snat disabled and skip directly by zhanyong.miao ZSTAC-18373
         * */
        if ( VirtualRouterSystemTags.VR_DISABLE_NETWORK_SERVICE_SNAT.hasTag(vr.getUuid())) {
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

        final VirtualRouterCommands.SNATInfo info = new VirtualRouterCommands.SNATInfo();
        info.setPrivateNicIp(privateNic.getIp());
        info.setPrivateNicMac(privateNic.getMac());
        info.setPublicNicMac(vr.getPublicNic().getMac());
        info.setPublicIp(vr.getPublicNic().getIp());
        info.setSnatNetmask(struct.getGuestNetmask());

        VirtualRouterCommands.RemoveSNATCmd cmd = new VirtualRouterCommands.RemoveSNATCmd();
        cmd.setNatInfo(Arrays.asList(info));

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
                                vr.getUuid(), vr.getManagementNic().getIp(), JSONObjectUtil.toJsonString(info), vmInstanceInventory.getUuid(), vmInstanceInventory.getName(),
                                struct.getL3Network().getUuid(), struct.getL3Network().getName(), ret.getError());
                        logger.warn(err);
                        //TODO GC
                    } else {
                        String msg = String.format(
                                "virtual router[uuid:%s, ip:%s] released snat[%s] for vm[uuid:%s, name:%s] on L3Network[uuid:%s, name:%s], because %s",
                                vr.getUuid(), vr.getManagementNic().getIp(), JSONObjectUtil.toJsonString(info), vmInstanceInventory.getUuid(), vmInstanceInventory.getName(),
                                struct.getL3Network().getUuid(), struct.getL3Network().getName(), ret.getError());
                        logger.warn(msg);
                    }
                }

                releaseSnat(it, vmInstanceInventory, completion);
            }
        });
    }

    private void releaseSnat(final Iterator<SnatStruct> it, final VmInstanceSpec spec, final NoErrorCompletion completion) {
        releaseSnat(it, spec.getVmInventory(), completion);
    }

    @Override
    public void releaseSnatService(List<SnatStruct> snatStructList, VmInstanceSpec spec, NoErrorCompletion completion) {
        if (snatStructList.isEmpty()) {
            completion.done();
            return;
        }

        releaseSnat(snatStructList.iterator(), spec, completion);
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

        /*
         * snat disabled and skip directly by zhanyong.miao ZSTAC-18373
         * */
        if ( VirtualRouterSystemTags.VR_DISABLE_NETWORK_SERVICE_SNAT.hasTag(vrVO.getUuid())) {
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

        final List<VirtualRouterCommands.SNATInfo> snatInfo = new ArrayList<VirtualRouterCommands.SNATInfo>();
        for (VmNicInventory vnic : vr.getVmNics()) {
            if (nwServed.contains(vnic.getL3NetworkUuid())) {
                VirtualRouterCommands.SNATInfo info = new VirtualRouterCommands.SNATInfo();
                info.setPrivateNicIp(vnic.getIp());
                info.setPrivateNicMac(vnic.getMac());
                info.setPublicIp(vr.getPublicNic().getIp());
                info.setPublicNicMac(vr.getPublicNic().getMac());
                info.setSnatNetmask(vnic.getNetmask());
                snatInfo.add(info);
            }
        }

        VirtualRouterCommands.SyncSNATCmd cmd = new VirtualRouterCommands.SyncSNATCmd();
        cmd.setSnats(snatInfo);
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
        String vipUuid = Q.New(VipVO.class).eq(VipVO_.usedIpUuid, vr.getPublicNic().getUsedIpUuid()).select(VipVO_.uuid).findValue();
        if (vipUuid == null){
            return null;
        }

        ModifyVipAttributesStruct struct = new ModifyVipAttributesStruct();
        struct.setUseFor(NetworkServiceType.SNAT.toString());
        struct.setServiceUuid(vipUuid);
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
