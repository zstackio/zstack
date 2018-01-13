package org.zstack.network.service.virtualrouter.dns;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.appliancevm.ApplianceVmStatus;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.L3NetworkDnsVO;
import org.zstack.header.network.l3.L3NetworkDnsVO_;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.service.NetworkServiceProviderType;
import org.zstack.header.network.service.DnsStruct;
import org.zstack.header.network.service.NetworkServiceDnsBackend;
import org.zstack.header.vm.*;
import org.zstack.network.service.virtualrouter.*;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands.*;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.operr;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.Tuple;

/**
 */
public class VirtualRouterDnsBackend extends AbstractVirtualRouterBackend implements NetworkServiceDnsBackend,
        VmDetachNicExtensionPoint {
    private final CLogger logger = Utils.getLogger(VirtualRouterDnsBackend.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private ApiTimeoutManager apiTimeoutManager;
    @Autowired
    protected DatabaseFacade dbf;

    @Override
    public NetworkServiceProviderType getProviderType() {
        return VirtualRouterConstant.PROVIDER_TYPE;
    }

    public List<VirtualRouterCommands.DnsInfo> getDnsInfoOfVr(String vrUuid, String execludeL3NetworkUuid) {
        /* for vpc network, there are multiple network, so there may have multiple dns for a virtual router,
         * for duplicated dns address, choose the dns with minimum index which is added first */
        if (execludeL3NetworkUuid == null) {
            execludeL3NetworkUuid = "";
        }
        String sql = "select l3.dns, nic.mac, min(l3.id) as idx from L3NetworkDnsVO l3, VmNicVO nic where l3.l3NetworkUuid = nic.l3NetworkUuid " +
                "and l3.l3NetworkUuid != :execludeL3NetworkUuid and nic.vmInstanceUuid = :vrUuid group by l3.dns order by idx";
        List<Tuple> tuples = SQL.New(sql, Tuple.class).param("execludeL3NetworkUuid", execludeL3NetworkUuid).param("vrUuid", vrUuid).list();

        List<VirtualRouterCommands.DnsInfo> dnsInfos = new ArrayList<>();
        if (tuples != null && !tuples.isEmpty()) {
            for (Tuple tuple: tuples) {
                VirtualRouterCommands.DnsInfo info = new VirtualRouterCommands.DnsInfo();
                info.setDnsAddress(tuple.get(0, String.class));
                info.setNicMac(tuple.get(1, String.class));
                dnsInfos.add(info);
            }
        }

        return dnsInfos;
    }

    @Override
    public void addDns(L3NetworkInventory l3, List<String> dns, final Completion completion) {
        VirtualRouterVmInventory vr = vrMgr.getVirtualRouterVm(l3);
        if (vr == null || !VmInstanceState.Running.toString().equals(vr.getState()) || !ApplianceVmStatus.Connected.toString().equals(vr.getStatus())) {
            completion.success();
            return;
        }

        SetDnsCmd cmd = new SetDnsCmd();
        cmd.setDns(getDnsInfoOfVr(vr.getUuid(), null));

        VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
        msg.setVmInstanceUuid(vr.getUuid());
        msg.setPath(VirtualRouterConstant.VR_SET_DNS_PATH);
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

                VirtualRouterAsyncHttpCallReply r = reply.castReply();
                SetDnsRsp rsp = r.toResponse(SetDnsRsp.class);
                if (!rsp.isSuccess()) {
                    completion.fail(operr("operation error, because:%s", rsp.getError()));
                    return;
                }

                completion.success();
            }
        });
    }

    @Override
    public void removeDns(L3NetworkInventory l3, List<String> dns, final Completion completion) {
        VirtualRouterVmInventory vr = vrMgr.getVirtualRouterVm(l3);
        if (vr == null || !VmInstanceState.Running.toString().equals(vr.getState()) || !ApplianceVmStatus.Connected.toString().equals(vr.getStatus())) {
            completion.success();
            return;
        }

        RemoveDnsCmd cmd = new RemoveDnsCmd();

        List<VirtualRouterCommands.DnsInfo> vrDns = getDnsInfoOfVr(vr.getUuid(), null);
        cmd.setDns(vrDns);

        VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
        msg.setVmInstanceUuid(vr.getUuid());
        msg.setPath(VirtualRouterConstant.VR_SET_DNS_PATH);
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

                VirtualRouterAsyncHttpCallReply r = reply.castReply();
                RemoveDnsRsp rsp = r.toResponse(RemoveDnsRsp.class);
                if (!rsp.isSuccess()) {
                    completion.fail(operr("operation error, because:%s", rsp.getError()));
                    return;
                }

                completion.success();
            }
        });
    }

    private void applyDns(final Iterator<DnsStruct> it, final VmInstanceSpec spec, final Completion completion) {
        if (!it.hasNext()) {
            completion.success();
            return;
        }

        final DnsStruct struct = it.next();
        final L3NetworkInventory l3 = struct.getL3Network();

        VirtualRouterStruct s = new VirtualRouterStruct();
        s.setL3Network(l3);

        acquireVirtualRouterVm(s, new ReturnValueCompletion<VirtualRouterVmInventory>(completion) {
            @Override
            public void success(final VirtualRouterVmInventory vr) {
                VirtualRouterCommands.SetDnsCmd cmd = new VirtualRouterCommands.SetDnsCmd();
                cmd.setDns(getDnsInfoOfVr(vr.getUuid(), null));

                VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
                msg.setVmInstanceUuid(vr.getUuid());
                msg.setPath(VirtualRouterConstant.VR_SET_DNS_PATH);
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
                        SetDnsRsp ret = re.toResponse(SetDnsRsp.class);
                        if (ret.isSuccess()) {
                            new VirtualRouterRoleManager().makeDnsRole(vr.getUuid());
                            logger.debug(String.format("successfully add dns entry[%s] to virtual router vm[uuid:%s, ip:%s]", struct, vr.getUuid(), vr.getManagementNic()
                                    .getIp()));
                            applyDns(it, spec, completion);
                        } else {
                            ErrorCode err = operr("virtual router[uuid:%s, ip:%s] failed to configure dns%s for L3Network[uuid:%s, name:%s], %s",
                                    vr.getUuid(), vr.getManagementNic().getIp(), struct, l3.getUuid(), l3.getName(), ret.getError());
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
    public void applyDnsService(List<DnsStruct> dnsStructList, VmInstanceSpec spec, Completion completion) {
        if (dnsStructList.isEmpty()) {
            completion.success();
            return;
        }

        applyDns(dnsStructList.iterator(), spec, completion);
    }


    private void releaseDns(final Iterator<DnsStruct> it, final VmInstanceSpec spec, final NoErrorCompletion completion) {
        if (!it.hasNext()) {
            completion.done();
            return;
        }

        DnsStruct struct = it.next();
        if (!vrMgr.isVirtualRouterRunningForL3Network(struct.getL3Network().getUuid())) {
            logger.debug(String.format("virtual router for l3Network[uuid:%s] is not running, skip releasing DNS", struct.getL3Network().getUuid()));
            releaseDns(it, spec, completion);
            return;
        }

        final VirtualRouterVmInventory vr = vrMgr.getVirtualRouterVm(struct.getL3Network());

        final List<VirtualRouterCommands.DnsInfo> info = new ArrayList<VirtualRouterCommands.DnsInfo>();
        for (String dns : struct.getDns()) {
            VirtualRouterCommands.DnsInfo i = new VirtualRouterCommands.DnsInfo();
            i.setDnsAddress(dns);
            i.setNicMac(vr.getGuestNicByL3NetworkUuid(struct.getL3Network().getUuid()).getMac());
            info.add(i);
        }

        VirtualRouterCommands.RemoveDnsCmd cmd = new VirtualRouterCommands.RemoveDnsCmd();
        cmd.setDns(info);

        VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
        msg.setCheckStatus(true);
        msg.setPath(VirtualRouterConstant.VR_REMOVE_DNS_PATH);
        msg.setCommand(cmd);
        msg.setCommandTimeout(apiTimeoutManager.getTimeout(cmd.getClass(), "30m"));
        msg.setVmInstanceUuid(vr.getUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vr.getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("virtual router[name: %s, uuid: %s] failed to remove dns%s, because %s",
                            vr.getName(), vr.getUuid(), JSONObjectUtil.toJsonString(info), reply.getError()));
                    // TODO GC
                } else {
                    VirtualRouterAsyncHttpCallReply re = reply.castReply();
                    RemoveDnsRsp ret = re.toResponse(RemoveDnsRsp.class);
                    if (ret.isSuccess()) {
                        logger.warn(String.format("virtual router[name: %s, uuid: %s] successfully removed dns%s",
                                vr.getName(), vr.getUuid(), JSONObjectUtil.toJsonString(info)));
                    } else {
                        logger.warn(String.format("virtual router[name: %s, uuid: %s] failed to remove dns%s, because %s",
                                vr.getName(), vr.getUuid(), JSONObjectUtil.toJsonString(info), ret.getError()));
                        //TODO GC
                    }
                }

                releaseDns(it, spec, completion);
            }
        });
    }

    @Override
    public void releaseDnsService(List<DnsStruct> dnsStructList, VmInstanceSpec spec, NoErrorCompletion completion) {
        if (dnsStructList.isEmpty()) {
            completion.done();
            return;
        }

        releaseDns(dnsStructList.iterator(), spec, completion);
    }

    @Override
    public void preDetachNic(VmNicInventory nic) {

    }

    @Override
    public void beforeDetachNic(VmNicInventory nic) {
        if (!VirtualRouterNicMetaData.GUEST_NIC_MASK_STRING_LIST.contains(nic.getMetaData())) {
            return;
        }

        VirtualRouterCommands.SetDnsCmd cmd = new VirtualRouterCommands.SetDnsCmd();
        cmd.setDns(getDnsInfoOfVr(nic.getVmInstanceUuid(), nic.getL3NetworkUuid()));

        VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
        msg.setCheckStatus(true);
        msg.setPath(VirtualRouterConstant.VR_SET_DNS_PATH);
        msg.setCommand(cmd);
        msg.setCommandTimeout(apiTimeoutManager.getTimeout(cmd.getClass(), "30m"));
        msg.setVmInstanceUuid(nic.getVmInstanceUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, nic.getVmInstanceUuid());
        bus.send(msg, new CloudBusCallBack(null) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("virtual router[uuid: %s] failed to remove dns, because %s",
                            nic.getVmInstanceUuid(), reply.getError()));
                } else {
                    VirtualRouterAsyncHttpCallReply re = reply.castReply();
                    RemoveDnsRsp ret = re.toResponse(RemoveDnsRsp.class);
                    if (ret.isSuccess()) {
                        logger.warn(String.format("virtual router[uuid: %s] successfully removed dns",
                                nic.getVmInstanceUuid()));
                    } else {
                        logger.warn(String.format("virtual router[uuid: %s] failed to remove dns, because %s",
                                nic.getVmInstanceUuid(), ret.getError()));
                    }
                }
            }
        });
    }

    @Override
    public void afterDetachNic(VmNicInventory nic) {

    }

    @Override
    public void failedToDetachNic(VmNicInventory nic, ErrorCode error) {

    }
}
