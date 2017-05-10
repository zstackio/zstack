package org.zstack.network.service.virtualrouter.nat;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.service.NetworkServiceProviderType;
import org.zstack.header.network.service.NetworkServiceSnatBackend;
import org.zstack.header.network.service.SnatStruct;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.virtualrouter.*;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands.RemoveSNATRsp;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands.SetSNATRsp;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.operr;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 10:37 PM
 * To change this template use File | Settings | File Templates.
 */
public class VirtualRouterSnatBackend extends AbstractVirtualRouterBackend implements NetworkServiceSnatBackend {
    private static final CLogger logger = Utils.getLogger(VirtualRouterSnatBackend.class);

    @Autowired
    private ErrorFacade errf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ApiTimeoutManager apiTimeoutManager;

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
                final VirtualRouterCommands.SNATInfo info = new VirtualRouterCommands.SNATInfo();
                VmNicInventory privateNic = CollectionUtils.find(vr.getVmNics(), new Function<VmNicInventory, VmNicInventory>() {
                    @Override
                    public VmNicInventory call(VmNicInventory arg) {
                        if (arg.getIp().equals(struct.getGuestGateway())) {
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
                msg.setCommandTimeout(apiTimeoutManager.getTimeout(cmd.getClass(), "30m"));
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

    private void releaseSnat(final Iterator<SnatStruct> it, final VmInstanceSpec spec, final NoErrorCompletion completion) {
        if (!it.hasNext()) {
            completion.done();
            return;
        }

        final SnatStruct struct = it.next();
        final VirtualRouterVmInventory vr = vrMgr.getVirtualRouterVm(struct.getL3Network());
        VmNicInventory privateNic = CollectionUtils.find(vr.getVmNics(), new Function<VmNicInventory, VmNicInventory>() {
            @Override
            public VmNicInventory call(VmNicInventory arg) {
                if (arg.getIp().equals(struct.getGuestGateway())) {
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
        msg.setCommandTimeout(apiTimeoutManager.getTimeout(cmd.getClass(), "30m"));
        msg.setPath(VirtualRouterConstant.VR_REMOVE_SNAT_PATH);
        bus.makeTargetServiceIdByResourceUuid(msg, VirtualRouterConstant.SERVICE_ID, vr.getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("failed to release snat[%s] on virtual router[name:%s, uuid:%s] for vm[uuid: %s, name: %s], %s",
                            struct, vr.getName(), vr.getUuid(), spec.getVmInventory().getUuid(), spec.getVmInventory().getName(), reply.getError()));
                    //TODO GC
                } else {
                    VirtualRouterAsyncHttpCallReply re = reply.castReply();
                    RemoveSNATRsp ret = re.toResponse(RemoveSNATRsp.class);
                    if (!ret.isSuccess()) {
                        String err = String.format(
                                "virtual router[uuid:%s, ip:%s] failed to release snat[%s] for vm[uuid:%s, name:%s] on L3Network[uuid:%s, name:%s], because %s",
                                vr.getUuid(), vr.getManagementNic().getIp(), JSONObjectUtil.toJsonString(info), spec.getVmInventory().getUuid(), spec.getVmInventory().getName(),
                                struct.getL3Network().getUuid(), struct.getL3Network().getName(), ret.getError());
                        logger.warn(err);
                        //TODO GC
                    } else {
                        String msg = String.format(
                                "virtual router[uuid:%s, ip:%s] released snat[%s] for vm[uuid:%s, name:%s] on L3Network[uuid:%s, name:%s], because %s",
                                vr.getUuid(), vr.getManagementNic().getIp(), JSONObjectUtil.toJsonString(info), spec.getVmInventory().getUuid(), spec.getVmInventory().getName(),
                                struct.getL3Network().getUuid(), struct.getL3Network().getName(), ret.getError());
                        logger.warn(msg);
                    }
                }

                releaseSnat(it, spec, completion);
            }
        });
    }

    @Override
    public void releaseSnatService(List<SnatStruct> snatStructList, VmInstanceSpec spec, NoErrorCompletion completion) {
        if (snatStructList.isEmpty()) {
            completion.done();
            return;
        }

        releaseSnat(snatStructList.iterator(), spec, completion);
    }
}
