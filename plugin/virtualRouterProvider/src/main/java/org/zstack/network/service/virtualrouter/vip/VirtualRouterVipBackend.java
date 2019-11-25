package org.zstack.network.service.virtualrouter.vip;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.service.VirtualRouterAfterAttachNicExtensionPoint;
import org.zstack.header.network.service.VirtualRouterBeforeDetachNicExtensionPoint;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.vip.VipBackend;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.network.service.vip.VipVO;
import org.zstack.network.service.virtualrouter.*;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.list;

public class VirtualRouterVipBackend extends AbstractVirtualRouterBackend implements
        VipBackend, VirtualRouterAfterAttachNicExtensionPoint, VirtualRouterBeforeDetachNicExtensionPoint {
    private static final CLogger logger = Utils.getLogger(VirtualRouterVipBackend.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected PluginRegistry pluginRgty;
    @Autowired
    protected ErrorFacade errf;
    @Autowired
    private ApiTimeoutManager apiTimeoutManager;
    @Autowired
    private VipConfigProxy proxy;

    private String getOwnerMac(VirtualRouterVmInventory vr, VipInventory vip) {
        for (VmNicInventory nic : vr.getVmNics()) {
            if (nic.getL3NetworkUuid().equals(vip.getL3NetworkUuid())) {
                return nic.getMac();
            }
        }

        throw new CloudRuntimeException(String.format("virtual router vm[uuid:%s] has no nic on l3Network[uuid:%s] for vip[uuid:%s, ip:%s]",
                vr.getUuid(), vip.getL3NetworkUuid(), vip.getUuid(), vip.getIp()));
    }

    public void createVipOnVirtualRouterVm(final VirtualRouterVmInventory vr, List<VipInventory> vips, final Completion completion) {
        final List<VipTO> tos = new ArrayList<VipTO>(vips.size());
        for (VipInventory vip : vips) {
            String mac = getOwnerMac(vr, vip);
            VipTO to = VipTO.valueOf(vip, mac);
            tos.add(to);
        }

        CreateVipCmd cmd = new CreateVipCmd();
        cmd.setVips(tos);

        VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
        msg.setVmInstanceUuid(vr.getUuid());
        msg.setCommand(cmd);
        msg.setPath(VirtualRouterConstant.VR_CREATE_VIP);
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vr.getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                VirtualRouterAsyncHttpCallReply re = reply.castReply();
                CreateVipRsp ret = re.toResponse(CreateVipRsp.class);
                if (!ret.isSuccess()) {
                    ErrorCode err = operr("failed to create vip%s on virtual router[uuid:%s], because %s", tos, vr.getUuid(), ret.getError());
                    completion.fail(err);
                } else {
                    completion.success();
                }
            }
        });
    }

    public void releaseVipOnVirtualRouterVm(final VirtualRouterVmInventory vr, VipInventory vip, final Completion completion) {
        List<VipInventory> invs = new ArrayList<VipInventory>();
        invs.add(vip);
        releaseVipOnVirtualRouterVm(vr, invs, completion);
    }

    public void releaseVipOnVirtualRouterVm(final VirtualRouterVmInventory vr, List<VipInventory> vips, final Completion completion) {
        final List<VipTO> tos = new ArrayList<VipTO>(vips.size());
        for (VipInventory vip : vips) {
            String mac = getOwnerMac(vr, vip);
            VipTO to = VipTO.valueOf(vip, mac);
            tos.add(to);
        }

        RemoveVipCmd cmd = new RemoveVipCmd();
        cmd.setVips(tos);

        VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
        msg.setPath(VirtualRouterConstant.VR_REMOVE_VIP);
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
                RemoveVipRsp ret = re.toResponse(RemoveVipRsp.class);
                if (ret.isSuccess()) {
                    completion.success();
                } else {
                    ErrorCode err = operr("failed to remove vip%s, because %s", tos, ret.getError());
                    completion.fail(err);
                }
            }
        });
    }

    public void acquireVipOnVirtualRouterVm(final VirtualRouterVmInventory vr, final VipInventory vip, final Completion completion) {
        createVipOnVirtualRouterVm(vr, list(vip), new Completion(completion) {
            @Override
            public void success() {
                proxy.attachNetworkService(vr.getUuid(), VipVO.class.getSimpleName(), asList(vip.getUuid()));
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public void afterAttachNicRollback(VmNicInventory nic, NoErrorCompletion completion) {
        completion.done();
    }

    @Override
    public void afterAttachNic(VmNicInventory nic, Completion completion) {
        if (!VirtualRouterNicMetaData.GUEST_NIC_MASK_STRING_LIST.contains(nic.getMetaData())) {
            completion.success();
            return;
        }

        if (VirtualRouterSystemTags.DEDICATED_ROLE_VR.hasTag(nic.getVmInstanceUuid())) {
            completion.success();
            return;
        }

        List<VipTO> vips = findVipsOnVirtualRouter(nic);

        if (vips == null || vips.isEmpty()) {
            completion.success();
            return;
        }

        CreateVipCmd cmd = new CreateVipCmd();
        cmd.setVips(vips);

        VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
        msg.setVmInstanceUuid(nic.getVmInstanceUuid());
        msg.setCommand(cmd);
        msg.setPath(VirtualRouterConstant.VR_CREATE_VIP);
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, nic.getVmInstanceUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                VirtualRouterAsyncHttpCallReply re = reply.castReply();
                CreateVipRsp ret = re.toResponse(CreateVipRsp.class);
                if (!ret.isSuccess()) {
                    ErrorCode err = operr("failed to sync vips[ips: %s] on virtual router[uuid:%s]" +
                            " for attaching nic[uuid: %s, ip: %s], because %s",
                            vips.stream().map(v -> v.getIp()).collect(Collectors.toList()),
                            nic.getVmInstanceUuid(), nic.getUuid(), nic.getIp(), ret.getError());
                    completion.fail(err);
                } else {
                    List<String> vipUuids = vips.stream().map(v -> v.getVipUuid()).distinct().collect(Collectors.toList());
                    proxy.attachNetworkService(nic.getVmInstanceUuid(), VipVO.class.getSimpleName(), vipUuids);
                    completion.success();
                }
            }
        });
    }

    private List<VipTO> findVipsOnVirtualRouter(VmNicInventory nic) {
        List<VipVO> vips = SQL.New("select vip from VipVO vip, VipPeerL3NetworkRefVO ref " +
                "where ref.vipUuid = vip.uuid " +
                "and ref.l3NetworkUuid = :l3Uuid")
                .param("l3Uuid", nic.getL3NetworkUuid())
                .list();

        if (vips == null || vips.isEmpty()) {
            return null;
        }

        VirtualRouterVmInventory vr = VirtualRouterVmInventory.valueOf((VirtualRouterVmVO)
                Q.New(VirtualRouterVmVO.class).eq(VirtualRouterVmVO_.uuid, nic.getVmInstanceUuid()).find());

        List<VipTO> vipTOS = new ArrayList<>();
        for (VipVO vip : vips) {
            if (vipTOS.stream().anyMatch(v -> v.getIp().equals(vip.getIp()))) {
                logger.warn(String.format(
                        "found duplicate vip ip[uuid; %s, uuids: %s] for vr[uuid: %s]",
                        vip.getIp(),
                        vips.stream().
                                filter(v -> v.getIp().equals(vip.getIp()))
                                .map(v -> v.getUuid())
                                .collect(Collectors.toSet()),
                        nic.getVmInstanceUuid()));
                continue;
            }

            VipTO to = new VipTO();
            to.setIp(vip.getIp());
            to.setGateway(vip.getGateway());
            to.setNetmask(vip.getNetmask());
            to.setOwnerEthernetMac(vr.getVmNics().stream()
                    .filter(n -> n.getL3NetworkUuid().equals(vip.getL3NetworkUuid()))
                    .findFirst().get().getMac());
            to.setVipUuid(vip.getUuid());
            vipTOS.add(to);
        }

        return vipTOS;
    }

    @Override
    public String getServiceProviderTypeForVip() {
        return VirtualRouterConstant.VIRTUAL_ROUTER_PROVIDER_TYPE;
    }

    @Override
    public void beforeDetachNic(VmNicInventory nic, Completion completion) {
        if (!VirtualRouterNicMetaData.GUEST_NIC_MASK_STRING_LIST.contains(nic.getMetaData())) {
            completion.success();
            return;
        }

        if (VirtualRouterSystemTags.DEDICATED_ROLE_VR.hasTag(nic.getVmInstanceUuid())) {
            completion.success();
            return;
        }

        VirtualRouterVmInventory vr = VirtualRouterVmInventory.valueOf((VirtualRouterVmVO)
                Q.New(VirtualRouterVmVO.class).eq(VirtualRouterVmVO_.uuid, nic.getVmInstanceUuid()).find());

        List<VipVO> vips = SQL.New("select vip from VipVO vip, VipPeerL3NetworkRefVO ref " +
                "where ref.vipUuid = vip.uuid and ref.l3NetworkUuid in (:routerNetworks) " +
                "and vip.l3NetworkUuid = :l3Uuid")
                              .param("l3Uuid", nic.getL3NetworkUuid())
                              .param("routerNetworks", vr.getAllL3Networks())
                              .list();

        if (vips.isEmpty()) {
            completion.success();
            return;
        }

        releaseVipOnVirtualRouterVm(vr, VipInventory.valueOf(vips), completion);
    }

    @Override
    public void beforeDetachNicRollback(VmNicInventory nic, NoErrorCompletion completion) {
        completion.done();
    }
}
