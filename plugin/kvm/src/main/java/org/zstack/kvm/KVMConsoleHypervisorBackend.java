package org.zstack.kvm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.header.console.ConsoleHypervisorBackend;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.kvm.KVMAgentCommands.GetVncPortResponse;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.operr;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 9:25 PM
 * To change this template use File | Settings | File Templates.
 */
public class KVMConsoleHypervisorBackend implements ConsoleHypervisorBackend {
    private static final CLogger logger = Utils.getLogger(KVMConsoleHypervisorBackend.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private ApiTimeoutManager timeoutMgr;

    @Override
    public HypervisorType getConsoleBackendHypervisorType() {
        return HypervisorType.valueOf(KVMConstant.KVM_HYPERVISOR_TYPE);
    }

    @Override
    public void generateConsoleUrl(final VmInstanceInventory vm, final ReturnValueCompletion<URI> complete) {
        KVMAgentCommands.GetVncPortCmd cmd = new KVMAgentCommands.GetVncPortCmd();
        cmd.setVmUuid(vm.getUuid());

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setHostUuid(vm.getHostUuid());
        msg.setCommand(cmd);
        msg.setCommandTimeout(timeoutMgr.getTimeout(cmd.getClass(), "5m"));
        msg.setPath(KVMConstant.KVM_GET_VNC_PORT_PATH);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, vm.getHostUuid());
        bus.send(msg, new CloudBusCallBack(complete) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    complete.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply kreply = reply.castReply();
                GetVncPortResponse rsp = kreply.toResponse(GetVncPortResponse.class);
                if (!rsp.isSuccess()) {
                    complete.fail(operr(rsp.getError()));
                    return;
                }

                SimpleQuery<HostVO> q = dbf.createQuery(HostVO.class);
                q.select(HostVO_.managementIp);
                q.add(HostVO_.uuid, Op.EQ, vm.getHostUuid());
                String mgmtIp = q.findValue();
                try {
                    URI uri = new URI(String.format("http://%s:%s/", mgmtIp, rsp.getPort()));
                    complete.success(uri);
                } catch (URISyntaxException e) {
                    complete.fail(errf.throwableToInternalError(e));
                }
            }
        });
    }
}
