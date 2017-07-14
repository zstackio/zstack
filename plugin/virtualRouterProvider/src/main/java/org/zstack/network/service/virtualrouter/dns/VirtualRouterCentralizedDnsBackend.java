package org.zstack.network.service.virtualrouter.dns;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.host.HostConstant;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.service.*;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.kvm.KVMSystemTags;
import org.zstack.network.service.virtualrouter.AbstractVirtualRouterBackend;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant;
import org.zstack.network.service.virtualrouter.VirtualRouterVmInventory;
import org.zstack.network.service.virtualrouter.vyos.VyosConstants;
import org.zstack.utils.TagUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Iterator;
import java.util.List;

/**
 * Created by AlanJager on 2017/7/8.
 */
public class VirtualRouterCentralizedDnsBackend extends AbstractVirtualRouterBackend implements NetworkServiceCentralizedDnsBackend {
    private final CLogger logger = Utils.getLogger(VirtualRouterCentralizedDnsBackend.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private ApiTimeoutManager apiTimeoutManager;

    public static final String SET_DNS_FORWARD_PATH = "/dns/forward/set";
    public static final String REMOVE_DNS_FORWARD_PATH = "/dns/forward/remove";

    @Override
    public NetworkServiceProviderType getProviderType() {
        return VirtualRouterConstant.PROVIDER_TYPE;
    }

    public static String makeNamespaceName(String brName, String l3Uuid) {
        return String.format("%s_%s", brName, l3Uuid);
    }

    private List<ForwardDnsStruct> beforeDoService(List<ForwardDnsStruct> forwardDnsStructs) {
        Iterator<ForwardDnsStruct> iterator = forwardDnsStructs.iterator();

        while (iterator.hasNext()) {
            boolean shouldRemove = false;
            for (NetworkServiceL3NetworkRefInventory ref : iterator.next().getL3Network().getNetworkServices()) {
                if (!ref.getNetworkServiceType().equals(NetworkServiceType.DHCP.toString())) {
                    continue;
                }

                NetworkServiceProviderVO vo = Q.New(NetworkServiceProviderVO.class).eq(NetworkServiceProviderVO_.uuid, ref.getNetworkServiceProviderUuid()).find();
                if (vo.getType().equals(VirtualRouterConstant.VIRTUAL_ROUTER_PROVIDER_TYPE) || vo.getType().equals(VyosConstants.VYOS_ROUTER_PROVIDER_TYPE)) {
                    shouldRemove = true;
                }
            }

            if (shouldRemove) {
                iterator.remove();
            }
        }

        return forwardDnsStructs;
    }

    @Override
    public void applyForwardDnsService(List<ForwardDnsStruct> forwardDnsStructs, VmInstanceSpec spec, Completion completion) {
        forwardDnsStructs = beforeDoService(forwardDnsStructs);

        if (forwardDnsStructs.isEmpty()) {
            completion.success();
            return;
        }

        new While<>(forwardDnsStructs).each((forwardDnsStruct, whileComplection) -> {
            VirtualRouterVmInventory vr = vrMgr.getVirtualRouterVm(forwardDnsStruct.getL3Network());
            VirtualRouterCommands.SetForwardDnsCmd cmd = new VirtualRouterCommands.SetForwardDnsCmd();
            cmd.setMac(forwardDnsStruct.getMac());
            String bridgeName = SQL.New("select t.tag from SystemTagVO t, L3NetworkVO l3 where t.resourceType = :ttype and t.tag like :tag" +
                    " and t.resourceUuid = l3.l2NetworkUuid and l3.uuid = :l3Uuid", String.class)
                    .param("tag", TagUtils.tagPatternToSqlPattern(KVMSystemTags.L2_BRIDGE_NAME.getTagFormat()))
                    .param("l3Uuid", forwardDnsStruct.getL3Network().getUuid())
                    .param("ttype", L2NetworkVO.class.getSimpleName())
                    .find();
            cmd.setBridgeName(KVMSystemTags.L2_BRIDGE_NAME.getTokenByTag(bridgeName, KVMSystemTags.L2_BRIDGE_NAME_TOKEN));
            cmd.setNameSpace(makeNamespaceName(
                    cmd.getBridgeName(),
                    forwardDnsStruct.getL3Network().getUuid()
            ));
            for (VmNicInventory nic : vr.getVmNics()) {
                if (nic.getL3NetworkUuid().equals(forwardDnsStruct.getL3Network().getUuid())) {
                    cmd.setDns(nic.getIp());
                }
            }
            cmd.setWrongDns(forwardDnsStruct.getL3Network().getDns());

            KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
            kmsg.setCommand(cmd);
            kmsg.setCommandTimeout(apiTimeoutManager.getTimeout(cmd.getClass(), "5m"));
            kmsg.setPath(SET_DNS_FORWARD_PATH);
            kmsg.setHostUuid(spec.getDestHost().getUuid());
            bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, spec.getDestHost().getUuid());
            bus.send(kmsg, new CloudBusCallBack(completion) {
                @Override
                public void run(MessageReply reply) {
                    VirtualRouterCommands.SetForwardDnsRsp rsp = !reply.isSuccess() ? null :
                            ((KVMHostAsyncHttpCallReply) reply).toResponse(VirtualRouterCommands.SetForwardDnsRsp.class);
                    if (!reply.isSuccess() || !rsp.isSuccess()) {
                        logger.warn(String.format("set forwarding error on host[uuid:%s]", spec.getVmInventory().getHostUuid()));
                    }

                    whileComplection.done();

                }
            });

        }).run(new NoErrorCompletion() {
            @Override
            public void done() {
                completion.success();
            }
        });
    }

    @Override
    public void releaseForwardDnsService(List<ForwardDnsStruct> forwardDnsStructs, VmInstanceSpec spec, NoErrorCompletion completion) {
        forwardDnsStructs = beforeDoService(forwardDnsStructs);

        if (forwardDnsStructs.isEmpty()) {
            completion.done();
            return;
        }

        new While<>(forwardDnsStructs).each((forwardDnsStruct, whileComplection) -> {
            VirtualRouterCommands.RemoveForwardDnsCmd cmd = new VirtualRouterCommands.RemoveForwardDnsCmd();
            cmd.setMac(forwardDnsStruct.getMac());
            String bridgeName = SQL.New("select t.tag from SystemTagVO t, L3NetworkVO l3 where t.resourceType = :ttype and t.tag like :tag" +
                    " and t.resourceUuid = l3.l2NetworkUuid and l3.uuid = :l3Uuid", String.class)
                    .param("tag", TagUtils.tagPatternToSqlPattern(KVMSystemTags.L2_BRIDGE_NAME.getTagFormat()))
                    .param("l3Uuid", forwardDnsStruct.getL3Network().getUuid())
                    .param("ttype", L2NetworkVO.class.getSimpleName())
                    .find();
            cmd.setBridgeName(KVMSystemTags.L2_BRIDGE_NAME.getTokenByTag(bridgeName, KVMSystemTags.L2_BRIDGE_NAME_TOKEN));
            cmd.setNameSpace(makeNamespaceName(
                    cmd.getBridgeName(),
                    forwardDnsStruct.getL3Network().getUuid()
            ));

            KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
            kmsg.setCommand(cmd);
            kmsg.setCommandTimeout(apiTimeoutManager.getTimeout(cmd.getClass(), "5m"));
            kmsg.setPath(REMOVE_DNS_FORWARD_PATH);
            kmsg.setHostUuid(spec.getDestHost().getUuid());
            bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, spec.getDestHost().getUuid());
            bus.send(kmsg, new CloudBusCallBack(completion) {
                @Override
                public void run(MessageReply reply) {
                    VirtualRouterCommands.RemoveForwardDnsRsp rsp = !reply.isSuccess() ? null :
                            ((KVMHostAsyncHttpCallReply) reply).toResponse(VirtualRouterCommands.RemoveForwardDnsRsp.class);
                    if (!reply.isSuccess() || !rsp.isSuccess()) {
                        logger.warn(String.format("set forwarding error on host[uuid:%s]", spec.getVmInventory().getHostUuid()));
                    }

                    whileComplection.done();

                }
            });

        }).run(new NoErrorCompletion() {
            @Override
            public void done() {
                completion.done();
            }
        });
    }

    @Override
    public void vmDefaultL3NetworkChanged(VmInstanceInventory vm, String previousL3, String nowL3, Completion completion) {

    }
}
