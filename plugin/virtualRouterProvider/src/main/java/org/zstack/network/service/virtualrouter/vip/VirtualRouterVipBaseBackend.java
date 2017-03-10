package org.zstack.network.service.virtualrouter.vip;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.L3Network;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.vm.*;
import org.zstack.network.service.vip.*;
import org.zstack.network.service.virtualrouter.*;
import org.zstack.network.service.virtualrouter.vyos.VyosConstants;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.operr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.codehaus.groovy.runtime.InvokerHelper.asList;

/**
 * Created by xing5 on 2016/11/20.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VirtualRouterVipBaseBackend extends VipBaseBackend {
    private static final CLogger logger = Utils.getLogger(VirtualRouterVipBaseBackend.class);

    @Autowired
    protected VirtualRouterManager vrMgr;
    @Autowired
    private ApiTimeoutManager apiTimeoutManager;

    public VirtualRouterVipBaseBackend(VipVO self) {
        super(self);
    }

    @Override
    protected void releaseVipOnBackend(Completion completion) {
        final VirtualRouterVipVO vrvip = dbf.findByUuid(self.getUuid(), VirtualRouterVipVO.class);
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

        releaseVipOnVirtualRouterVm(VirtualRouterVmInventory.valueOf(vrvo), asList(getSelfInventory()), new Completion(completion) {
            @Override
            public void success() {
                logger.debug(String.format("successfully released vip[uuid:%s, name:%s, ip:%s] on virtual router vm[uuid:%s]",
                        self.getUuid(), self.getName(), self.getIp(), vrvo.getUuid()));
                dbf.removeByPrimaryKey(vrvip.getUuid(), vrvip.getClass());
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.warn(errorCode.toString());
                // NOTE: we don't know if the failure happens before or after the VIP deleted on the real device,
                // for example, a message timeout failure happens after the VIP gets really deleted, but an internal error
                // may happen before so. In both cases, we delete the database reference here so next time the backend
                // will try to apply the VIP again. It's virtualrouter/vyos's responsibility to succeed if a VIP is applied
                // while it exists
                dbf.removeByPrimaryKey(vrvip.getUuid(), vrvip.getClass());
                completion.fail(errorCode);
            }
        });
    }

    private String getOwnerMac(VirtualRouterVmInventory vr, VipInventory vip) {
        for (VmNicInventory nic : vr.getVmNics()) {
            if (nic.getL3NetworkUuid().equals(vip.getL3NetworkUuid())) {
                return nic.getMac();
            }
        }

        throw new CloudRuntimeException(String.format("virtual router vm[uuid:%s] has no nic on l3Network[uuid:%s] for vip[uuid:%s, ip:%s]",
                vr.getUuid(), vip.getL3NetworkUuid(), vip.getUuid(), vip.getIp()));
    }

    private void releaseVipOnVirtualRouterVm(final VirtualRouterVmInventory vr, List<VipInventory> vips, final Completion completion) {
        final List<VirtualRouterCommands.VipTO> tos = new ArrayList<VirtualRouterCommands.VipTO>(vips.size());
        for (VipInventory vip : vips) {
            String mac = getOwnerMac(vr, vip);
            VirtualRouterCommands.VipTO to = VirtualRouterCommands.VipTO.valueOf(vip, mac);
            tos.add(to);
        }

        VirtualRouterCommands.RemoveVipCmd cmd = new VirtualRouterCommands.RemoveVipCmd();
        cmd.setVips(tos);

        VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
        msg.setPath(VirtualRouterConstant.VR_REMOVE_VIP);
        msg.setCommand(cmd);
        msg.setCommandTimeout(apiTimeoutManager.getTimeout(cmd.getClass(), "30m"));
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
                VirtualRouterCommands.RemoveVipRsp ret = re.toResponse(VirtualRouterCommands.RemoveVipRsp.class);
                if (ret.isSuccess()) {
                    completion.success();
                } else {
                    ErrorCode err = operr("failed to remove vip%s, because %s", tos, ret.getError());
                    completion.fail(err);
                }
            }
        });
    }

    public void createVipOnVirtualRouterVm(final VirtualRouterVmInventory vr, List<VipInventory> vips, final Completion completion) {
        final List<VirtualRouterCommands.VipTO> tos = new ArrayList<VirtualRouterCommands.VipTO>(vips.size());
        for (VipInventory vip : vips) {
            String mac = getOwnerMac(vr, vip);
            VirtualRouterCommands.VipTO to = VirtualRouterCommands.VipTO.valueOf(vip, mac);
            tos.add(to);
        }

        VirtualRouterCommands.CreateVipCmd cmd = new VirtualRouterCommands.CreateVipCmd();
        cmd.setVips(tos);

        VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
        msg.setVmInstanceUuid(vr.getUuid());
        msg.setCommand(cmd);
        msg.setCommandTimeout(apiTimeoutManager.getTimeout(cmd.getClass(), "30m"));
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
                VirtualRouterCommands.CreateVipRsp ret = re.toResponse(VirtualRouterCommands.CreateVipRsp.class);
                if (!ret.isSuccess()) {
                    ErrorCode err = operr("failed to create vip%s on virtual router[uuid:%s], because %s", tos, vr.getUuid(), ret.getError());
                    completion.fail(err);
                } else {
                    completion.success();
                }
            }
        });
    }

    @Override
    protected void acquireVipOnBackend(Completion completion) {
        VirtualRouterVipVO vipvo = dbf.findByUuid(self.getUuid(), VirtualRouterVipVO.class);

        if (vipvo != null) {
            SimpleQuery<VmInstanceVO> q = dbf.createQuery(VmInstanceVO.class);
            q.select(VmInstanceVO_.state);
            q.add(VmInstanceVO_.uuid, SimpleQuery.Op.EQ, vipvo.getVirtualRouterVmUuid());
            VmInstanceState vrState = q.findValue();

            if (VmInstanceState.Running != vrState) {
                completion.fail(operr("virtual router[uuid:%s, state:%s] is not running",
                        vipvo.getVirtualRouterVmUuid(), vrState));
            } else {
                completion.success();
            }

            return;
        }

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("prepare-vr-for-vip-%s-%s", self.getUuid(), self.getIp()));
        chain.then(new NoRollbackFlow() {
            @Override
            public void run(final FlowTrigger trigger, final Map data) {
                DebugUtils.Assert(self.getPeerL3NetworkUuid() != null, "peerL3NetworkUuid cannot be null");

                VirtualRouterStruct s = new VirtualRouterStruct();
                s.setL3Network(L3NetworkInventory.valueOf(dbf.findByUuid(self.getPeerL3NetworkUuid(), L3NetworkVO.class)));

                String appVmType;
                if (self.getServiceProvider().equals(VirtualRouterConstant.VIRTUAL_ROUTER_PROVIDER_TYPE)) {
                    appVmType = VirtualRouterConstant.VIRTUAL_ROUTER_VM_TYPE;
                } else if (self.getServiceProvider().equals(VyosConstants.VYOS_ROUTER_PROVIDER_TYPE)) {
                    appVmType = VyosConstants.VYOS_VM_TYPE;
                } else {
                    throw new CloudRuntimeException(String.format("unknown network service provider type[%s]", self.getServiceProvider()));
                }

                s.setApplianceVmType(appVmType);
                s.setProviderType(self.getServiceProvider());
                s.setOfferingValidator(offering -> {
                    if (!offering.getPublicNetworkUuid().equals(self.getL3NetworkUuid())) {
                        throw new OperationFailureException(operr("found a virtual router offering[uuid:%s] for L3Network[uuid:%s] in zone[uuid:%s]; however, the network's public network[uuid:%s] is not the same to VIP[uuid:%s]'s; you may need to use system tag" +
                                        " guestL3Network::l3NetworkUuid to specify a particular virtual router offering for the L3Network",
                                offering.getUuid(), s.getL3Network().getUuid(), s.getL3Network().getZoneUuid(),
                                self.getL3NetworkUuid(), self.getUuid()));
                    }
                });

                vrMgr.acquireVirtualRouterVm(s, new ReturnValueCompletion<VirtualRouterVmInventory>(trigger){
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
                createVipOnVirtualRouterVm(vr, Arrays.asList(getSelfInventory()), new Completion(trigger) {
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

                if (!dbf.isExist(self.getUuid(), VirtualRouterVipVO.class)) {
                    VirtualRouterVipVO vrvip = new VirtualRouterVipVO();
                    vrvip.setUuid(self.getUuid());
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
    protected void handleBackendSpecificMessage(Message msg) {
        bus.dealWithUnknownMessage(msg);
    }
}
