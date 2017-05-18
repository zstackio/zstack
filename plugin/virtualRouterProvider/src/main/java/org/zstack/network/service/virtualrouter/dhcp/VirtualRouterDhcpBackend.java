package org.zstack.network.service.virtualrouter.dhcp;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.service.DhcpStruct;
import org.zstack.header.network.service.NetworkServiceDhcpBackend;
import org.zstack.header.network.service.NetworkServiceProviderType;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.virtualrouter.*;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands.AddDhcpEntryRsp;
import org.zstack.utils.CollectionDSL;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.operr;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class VirtualRouterDhcpBackend extends AbstractVirtualRouterBackend implements NetworkServiceDhcpBackend {
    private final CLogger logger = Utils.getLogger(VirtualRouterDhcpBackend.class);

    @Autowired
    protected ErrorFacade errf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected ApiTimeoutManager apiTimeoutManager;

    @Override
    public NetworkServiceProviderType getProviderType() {
        return VirtualRouterConstant.PROVIDER_TYPE;
    }

    private void applyDhcpEntry(final Iterator<DhcpStruct> it, final VmInstanceSpec spec, final Completion completion) {
        if (!it.hasNext()) {
            completion.success();
            return;
        }

        final DhcpStruct struct = it.next();

        VirtualRouterStruct s = new VirtualRouterStruct();
        s.setL3Network(struct.getL3Network());

        acquireVirtualRouterVm(s, new ReturnValueCompletion<VirtualRouterVmInventory>(completion) {
            @Override
            public void success(final VirtualRouterVmInventory vr) {
                VirtualRouterCommands.DhcpInfo e = new VirtualRouterCommands.DhcpInfo();
                e.setGateway(struct.getGateway());
                e.setIp(struct.getIp());
                e.setDefaultL3Network(struct.isDefaultL3Network());
                e.setMac(struct.getMac());
                e.setGateway(struct.getGateway());
                e.setNetmask(struct.getNetmask());
                e.setDnsDomain(struct.getDnsDomain());
                e.setHostname(struct.getHostname());
                e.setMtu(struct.getMtu());

                if (e.isDefaultL3Network()) {
                    if (e.getHostname() == null) {
                        e.setHostname(e.getIp().replaceAll("\\.", "-"));
                    }

                    if (e.getDnsDomain() != null) {
                        e.setHostname(String.format("%s.%s", e.getHostname(), e.getDnsDomain()));
                    }
                }

                VmNicInventory vrNic = CollectionUtils.find(vr.getVmNics(), new Function<VmNicInventory, VmNicInventory>() {
                    @Override
                    public VmNicInventory call(VmNicInventory arg) {
                        return arg.getL3NetworkUuid().equals(struct.getL3Network().getUuid()) ? arg : null;
                    }
                });
                e.setVrNicMac(vrNic.getMac());
                if (struct.isDefaultL3Network()) {
                    e.setDns(CollectionDSL.list(vrNic.getIp()));
                }

                VirtualRouterCommands.AddDhcpEntryCmd cmd = new VirtualRouterCommands.AddDhcpEntryCmd();
                cmd.setDhcpEntries(Arrays.asList(e));
                VirtualRouterAsyncHttpCallMsg cmsg = new VirtualRouterAsyncHttpCallMsg();
                cmsg.setCommand(cmd);
                cmsg.setCommandTimeout(apiTimeoutManager.getTimeout(cmd.getClass(), "30m"));
                cmsg.setPath(VirtualRouterConstant.VR_ADD_DHCP_PATH);
                cmsg.setVmInstanceUuid(vr.getUuid());
                cmsg.setCheckStatus(true);
                bus.makeTargetServiceIdByResourceUuid(cmsg, VmInstanceConstant.SERVICE_ID, vr.getUuid());
                bus.send(cmsg, new CloudBusCallBack(completion) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            completion.fail(reply.getError());
                            return;
                        }

                        VirtualRouterAsyncHttpCallReply re = reply.castReply();
                        AddDhcpEntryRsp rsp =  re.toResponse(AddDhcpEntryRsp.class);
                        if (rsp.isSuccess()) {
                            new VirtualRouterRoleManager().makeDhcpRole(vr.getUuid());
                            logger.debug(String.format("successfully add dhcp entry[%s] to virtual router vm[uuid:%s, ip:%s]", struct, vr.getUuid(), vr.getManagementNic()
                                    .getIp()));
                            applyDhcpEntry(it, spec, completion);
                        } else {
                            ErrorCode err = operr("unable to add dhcp entries to virtual router vm[uuid:%s ip:%s], because %s, dhcp entry[%s]",
                                    vr.getUuid(), vr.getManagementNic().getIp(), rsp.getError(), struct);
                            completion.fail(err);
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
    public void applyDhcpService(final List<DhcpStruct> dhcpStructList, final VmInstanceSpec spec, final Completion completion) {
        if (dhcpStructList.isEmpty()) {
            completion.success();
            return;
        }

        applyDhcpEntry(dhcpStructList.iterator(), spec, completion);
    }

    private void releaseDhcp(final Iterator<DhcpStruct> it, final VmInstanceSpec spec, final NoErrorCompletion completion) {
        if (!it.hasNext()) {
            completion.done();
            return;
        }

        final DhcpStruct struct = it.next();
        if (!vrMgr.isVirtualRouterRunningForL3Network(struct.getL3Network().getUuid())) {
            logger.debug(String.format("virtual router for l3Network[uuid:%s] is not running, skip releasing DHCP", struct.getL3Network().getUuid()));
            releaseDhcp(it, spec, completion);
            return;
        }

        final VirtualRouterVmInventory vr = vrMgr.getVirtualRouterVm(struct.getL3Network());
        VmNicInventory vrNic = CollectionUtils.find(vr.getVmNics(), new Function<VmNicInventory, VmNicInventory>() {
            @Override
            public VmNicInventory call(VmNicInventory arg) {
                return arg.getL3NetworkUuid().equals(struct.getL3Network().getUuid()) ? arg : null;
            }
        });

        VirtualRouterCommands.DhcpInfo e = new VirtualRouterCommands.DhcpInfo();
        e.setGateway(struct.getGateway());
        e.setDefaultL3Network(struct.isDefaultL3Network());
        e.setIp(struct.getIp());
        e.setMac(struct.getMac());
        e.setNetmask(struct.getNetmask());
        e.setVrNicMac(vrNic.getMac());

        VirtualRouterCommands.RemoveDhcpEntryCmd cmd = new VirtualRouterCommands.RemoveDhcpEntryCmd();
        cmd.setDhcpEntries(Arrays.asList(e));

        VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
        msg.setCheckStatus(true);
        msg.setVmInstanceUuid(vr.getUuid());
        msg.setPath(VirtualRouterConstant.VR_REMOVE_DHCP_PATH);
        msg.setCommand(cmd);
        msg.setCommandTimeout(apiTimeoutManager.getTimeout(cmd.getClass(), "30m"));
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vr.getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("unable to remove dhcp entry[%s] from virtual router vm[uuid:%s, ip:%s], %s", struct, vr.getUuid(), vr
                            .getManagementNic().getIp(), reply.getError()));
                    //TODO: GC
                } else {
                    VirtualRouterAsyncHttpCallReply ret = reply.castReply();
                    if (ret.isSuccess()) {
                        logger.debug(String.format("successfully removed dhcp entry[%s] from virtual router vm[uuid:%s, ip:%s]", struct, vr.getUuid(), vr
                                .getManagementNic().getIp()));
                    } else {
                        logger.warn(String.format("unable to remove dhcp entry[%s] from virtual router vm[uuid:%s, ip:%s], %s", struct, vr.getUuid(), vr
                                .getManagementNic().getIp(), ret.getError()));
                        //TODO: GC
                    }
                }

                releaseDhcp(it, spec, completion);
            }
        });
    }

    @Override
    public void releaseDhcpService(List<DhcpStruct> dhcpStructList, VmInstanceSpec spec, NoErrorCompletion completion) {
        if (dhcpStructList.isEmpty()) {
            completion.done();
            return;
        }

        releaseDhcp(dhcpStructList.iterator(), spec, completion);
    }

    @Override
    public void vmDefaultL3NetworkChanged(VmInstanceInventory vm, String previousL3, String nowL3, Completion completion) {
        return;
    }
}
