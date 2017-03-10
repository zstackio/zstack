package org.zstack.network.service.virtualrouter.dns;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.L3NetworkDnsVO;
import org.zstack.header.network.l3.L3NetworkDnsVO_;
import org.zstack.header.network.service.NetworkServiceType;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.network.service.virtualrouter.*;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands.DnsInfo;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands.SetDnsCmd;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands.SetDnsRsp;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.operr;

import java.util.*;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VirtualRouterSyncDnsOnStartFlow extends NoRollbackFlow {
    private static final CLogger logger = Utils.getLogger(VirtualRouterSyncDnsOnStartFlow.class);

    @Autowired
    private CloudBus bus;
	@Autowired
	private VirtualRouterManager vrMgr;
	@Autowired
	private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private ApiTimeoutManager apiTimeoutManager;

    @Override
    public void run(final FlowTrigger chain, final Map data) {
        final VirtualRouterVmInventory vr = (VirtualRouterVmInventory) data.get(VirtualRouterConstant.Param.VR.toString());

        List<String> nwServed = vr.getGuestL3Networks();
        List<String> l3Uuids = vrMgr.selectL3NetworksNeedingSpecificNetworkService(nwServed, NetworkServiceType.DNS);
        if (l3Uuids.isEmpty()) {
            chain.next();
            return;
        }

        if (VirtualRouterSystemTags.DEDICATED_ROLE_VR.hasTag(vr.getUuid()) && !VirtualRouterSystemTags.VR_DNS_ROLE.hasTag(vr.getUuid())) {
            chain.next();
            return;
        }

        new VirtualRouterRoleManager().makeDnsRole(vr.getUuid());

        SimpleQuery<L3NetworkDnsVO> query = dbf.createQuery(L3NetworkDnsVO.class);
        query.select(L3NetworkDnsVO_.dns);
        query.add(L3NetworkDnsVO_.l3NetworkUuid, Op.IN, l3Uuids);
        List<String> lst = query.listValue();
        if (lst.isEmpty()) {
            chain.next();
            return;
        }

        Set<String> dnsAddresses = new HashSet<String>(lst.size());
        dnsAddresses.addAll(lst);

        final List<DnsInfo> dns = new ArrayList<DnsInfo>(dnsAddresses.size());
        for (String d : dnsAddresses) {
            DnsInfo dinfo = new DnsInfo();
            dinfo.setDnsAddress(d);
            dinfo.setNicMac(vr.getGuestNic().getMac());
            dns.add(dinfo);
        }

        SetDnsCmd cmd = new SetDnsCmd();
        cmd.setDns(dns);

        VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
        msg.setVmInstanceUuid(vr.getUuid());
        msg.setPath(VirtualRouterConstant.VR_SET_DNS_PATH);
        msg.setCommand(cmd);
        msg.setCommandTimeout(apiTimeoutManager.getTimeout(cmd.getClass(), "30m"));
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vr.getUuid());
        bus.send(msg, new CloudBusCallBack(chain) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    chain.fail(reply.getError());
                    return;
                }

                VirtualRouterAsyncHttpCallReply re = reply.castReply();
                SetDnsRsp ret = re.toResponse(SetDnsRsp.class);
                if (ret.isSuccess()) {
                    chain.next();
                } else {
                    ErrorCode err = operr("virtual router[name: %s, uuid: %s] failed to configure dns%s, %s ",
                            vr.getName(), vr.getUuid(), JSONObjectUtil.toJsonString(dns), ret.getError());
                    chain.fail(err);
                }
            }
        });
    }
}
