package org.zstack.console;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.db.Q;
import org.zstack.core.gc.GC;
import org.zstack.core.gc.GCCompletion;
import org.zstack.core.gc.TimeBasedGarbageCollector;
import org.zstack.header.console.ConsoleBackend;
import org.zstack.header.console.ConsoleConstants;
import org.zstack.header.console.ConsoleProxyAgentStatus;
import org.zstack.header.console.ConsoleProxyAgentVO;
import org.zstack.header.console.ConsoleProxyAgentVO_;
import org.zstack.header.console.ConsoleProxyInventory;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.*;

public class DeleteConsoleProxyGcJob extends TimeBasedGarbageCollector {
    private static final CLogger logger = Utils.getLogger(DeleteConsoleProxyGcJob.class);

    @GC
    public ConsoleProxyInventory consoleProxy;

    @Autowired
    private ConsoleManager consoleMgr;

    private ConsoleProxyAgentStatus findAgentStatus(String agentIp) {
        ConsoleProxyAgentStatus status = Q.New(ConsoleProxyAgentVO.class)
                .select(ConsoleProxyAgentVO_.status)
                .eq(ConsoleProxyAgentVO_.managementIp, agentIp)
                .findValue();
        if (status != null) {
            return status;
        }

        status = Q.New(ConsoleProxyAgentVO.class)
                .select(ConsoleProxyAgentVO_.status)
                .eq(ConsoleProxyAgentVO_.consoleProxyOverriddenIp, agentIp)
                .findValue();
        if (status != null) {
            return status;
        }

        status = Q.New(ConsoleProxyAgentVO.class)
                .select(ConsoleProxyAgentVO_.status)
                .eq(ConsoleProxyAgentVO_.consoleProxyOverriddenIpv4, agentIp)
                .findValue();
        if (status != null) {
            return status;
        }

        return Q.New(ConsoleProxyAgentVO.class)
                .select(ConsoleProxyAgentVO_.status)
                .eq(ConsoleProxyAgentVO_.consoleProxyOverriddenIpv6, agentIp)
                .findValue();
    }

    @Override
    protected void triggerNow(GCCompletion completion) {
        ConsoleBackend backend = consoleMgr.getConsoleBackend();
        if (backend == null) {
            // no available backend, cancel the gc job
            completion.cancel();
            return;
        }

        ConsoleProxyAgentStatus status = findAgentStatus(consoleProxy.getAgentIp());
        if (status == null && ConsoleConstants.MANAGEMENT_SERVER_CONSOLE_PROXY_BACKEND.equals(consoleProxy.getAgentType())) {
            status = Q.New(ConsoleProxyAgentVO.class)
                    .select(ConsoleProxyAgentVO_.status)
                    .eq(ConsoleProxyAgentVO_.uuid, Platform.getManagementServerId())
                    .findValue();
        }
        if (status == null) {
            logger.debug(String.format("console proxy not found on agent[ip: %s, uuid: %s]," +
                    " assume it has been deleted",
                    consoleProxy.getAgentIp(),
                    consoleProxy.getUuid()
            ));
            completion.cancel();
            return;
        }

        if (status != ConsoleProxyAgentStatus.Connected) {
            completion.fail(operr(ORG_ZSTACK_CONSOLE_10013, "console proxy[uuid: %s, status: %s] on agent[ip: %s]" +
                            " is not Connected, fail to delete it",
                    consoleProxy.getUuid(),
                    status,
                    consoleProxy.getAgentIp())
            );
            return;
        }

        backend.deleteConsoleSession(consoleProxy, new Completion(completion) {
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
}
