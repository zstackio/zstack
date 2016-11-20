package org.zstack.network.service.virtualrouter.vip;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.core.workflow.*;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.*;
import org.zstack.network.service.vip.VipBackend;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.network.service.virtualrouter.*;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.zstack.utils.CollectionDSL.list;

public class VirtualRouterVipBackend extends AbstractVirtualRouterBackend implements VipBackend {
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
        msg.setCommandTimeout(apiTimeoutManager.getTimeout(cmd.getClass(), "5m"));
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
                    String err = String.format("failed to create vip%s on virtual router[uuid:%s], because %s", tos, vr.getUuid(), ret.getError());
                    logger.warn(err);
                    completion.fail(errf.stringToOperationError(err));
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
        msg.setCommandTimeout(apiTimeoutManager.getTimeout(cmd.getClass(), "5m"));
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
                    String err = String.format("failed to remove vip%s, because %s", tos, ret.getError());
                    logger.warn(err);
                    completion.fail(errf.stringToOperationError(err));
                }
            }
        });
    }

    public void acquireVipOnVirtualRouterVm(final VirtualRouterVmInventory vr, final VipInventory vip, final Completion completion) {
        createVipOnVirtualRouterVm(vr, list(vip), new Completion(completion) {
            @Override
            public void success() {
                if (!dbf.isExist(vip.getUuid(), VirtualRouterVipVO.class)) {
                    VirtualRouterVipVO vrvip = new VirtualRouterVipVO();
                    vrvip.setUuid(vip.getUuid());
                    vrvip.setVirtualRouterVmUuid(vr.getUuid());
                    dbf.persist(vrvip);
                }
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public void acquireVip(final VipInventory vip, final L3NetworkInventory guestNw, final Completion completion) {
        VirtualRouterVipVO vipvo = dbf.findByUuid(vip.getUuid(), VirtualRouterVipVO.class);
        if (vipvo != null) {
            SimpleQuery<VmInstanceVO> q = dbf.createQuery(VmInstanceVO.class);
            q.select(VmInstanceVO_.state);
            q.add(VmInstanceVO_.uuid, SimpleQuery.Op.EQ, vipvo.getVirtualRouterVmUuid());
            VmInstanceState vrState = q.findValue();
            if (VmInstanceState.Running != vrState) {
                completion.fail(errf.stringToOperationError(
                        String.format("virtual router[uuid:%s, state:%s] is not running, current HA has not been supported, please manually start this virtual router",
                                vipvo.getVirtualRouterVmUuid(), vrState)
                ));
            } else {
                completion.success();
            }

            return;
        }

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("prepare-vr-for-vip-%s-%s", vip.getUuid(), vip.getIp()));
        chain.then(new NoRollbackFlow() {
            @Override
            public void run(final FlowTrigger trigger, final Map data) {
                VirtualRouterStruct s = new VirtualRouterStruct();
                s.setL3Network(guestNw);
                s.setOfferingValidator(new VirtualRouterOfferingValidator() {
                    @Override
                    public void validate(VirtualRouterOfferingInventory offering) throws OperationFailureException {
                        if (!offering.getPublicNetworkUuid().equals(vip.getL3NetworkUuid())) {
                            throw new OperationFailureException(errf.stringToOperationError(
                                    String.format("found a virtual router offering[uuid:%s] for L3Network[uuid:%s] in zone[uuid:%s]; however, the network's public network[uuid:%s] is not the same to VIP[uuid:%s]'s; you may need to use system tag" +
                                            " guestL3Network::l3NetworkUuid to specify a particular virtual router offering for the L3Network", offering.getUuid(), guestNw.getUuid(), guestNw.getZoneUuid(), vip.getL3NetworkUuid(), vip.getUuid())
                            ));
                        }
                    }
                });

                acquireVirtualRouterVm(s, new ReturnValueCompletion<VirtualRouterVmInventory>(trigger){
                    @Override
                    public void success(VirtualRouterVmInventory returnValue) {
                        data.put(VirtualRouterConstant.Param.VR.toString(), returnValue);
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
            public void run(final FlowTrigger trigger, Map data) {
                final VirtualRouterVmInventory vr = (VirtualRouterVmInventory) data.get(VirtualRouterConstant.Param.VR.toString());
                createVipOnVirtualRouterVm(vr, Arrays.asList(vip), new Completion(trigger) {
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
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                final VirtualRouterVmInventory vr = (VirtualRouterVmInventory) data.get(VirtualRouterConstant.Param.VR.toString());

                if (!dbf.isExist(vip.getUuid(), VirtualRouterVipVO.class)) {
                    VirtualRouterVipVO vrvip = new VirtualRouterVipVO();
                    vrvip.setUuid(vip.getUuid());
                    vrvip.setVirtualRouterVmUuid(vr.getUuid());
                    dbf.persist(vrvip);
                }

                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    @Override
    public void releaseVip(final VipInventory vip, final Completion completion) {
        final VirtualRouterVipVO vrvip = dbf.findByUuid(vip.getUuid(), VirtualRouterVipVO.class);
        if (vrvip == null) {
            completion.success();
            return;
        }

        if (vrvip.getVirtualRouterVmUuid() == null) {
            // the vr has been deleted
            dbf.remove(vrvip);
            completion.success();
            return;
        }

        final VirtualRouterVmVO vrvo = dbf.findByUuid(vrvip.getVirtualRouterVmUuid(), VirtualRouterVmVO.class);
        if (vrvo.getState() != VmInstanceState.Running) {
            // vr will sync when becomes Running
            dbf.remove(vrvip);
            completion.success();
            return;
        }

        final VirtualRouterVmInventory vr = VirtualRouterVmInventory.valueOf(vrvo);

        releaseVipOnVirtualRouterVm(vr, vip, new Completion(completion) {
            @Override
            public void success() {
                logger.debug(String.format("successfully released vip[uuid:%s, name:%s, ip:%s] on virtual router vm[uuid:%s]",
                        vip.getUuid(), vip.getName(), vip.getIp(), vrvo.getUuid()));
                dbf.remove(vrvip);
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.warn(String.format("failed to release vip[uuid:%s, name:%s, ip:%s] on virtual router vm[uuid:%s], because %s",
                        vip.getUuid(), vip.getName(), vip.getIp(),
                        vrvo.getUuid(), errorCode));
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public String getServiceProviderTypeForVip() {
        return VirtualRouterConstant.VIRTUAL_ROUTER_PROVIDER_TYPE;
    }
}
