package org.zstack.console;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.console.APIRequestConsoleAccessMsg;
import org.zstack.header.console.APIUpdateConsoleProxyAgentMsg;
import org.zstack.header.console.ConsoleConstants;
import org.zstack.header.message.APIMessage;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.utils.network.IPv6NetworkUtils;
import org.zstack.utils.network.NetworkUtils;

import java.util.Arrays;
import java.util.List;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.*;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 4:46 PM
 * To change this template use File | Settings | File Templates.
 */
public class ConsoleApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIRequestConsoleAccessMsg) {
            validate((APIRequestConsoleAccessMsg) msg);
        } else if (msg instanceof APIUpdateConsoleProxyAgentMsg) {
            validate((APIUpdateConsoleProxyAgentMsg) msg);
        }

        return msg;
    }

    private List<VmInstanceState> consoleAvailableStates = Arrays.asList(
            VmInstanceState.Running,
            VmInstanceState.Crashed,
            VmInstanceState.VolumeRecovering,
            VmInstanceState.Paused,
            VmInstanceState.NoState
    );

    private void validate(APIRequestConsoleAccessMsg msg) {
        SimpleQuery<VmInstanceVO> q = dbf.createQuery(VmInstanceVO.class);
        q.select(VmInstanceVO_.state);
        q.add(VmInstanceVO_.uuid, Op.EQ, msg.getVmInstanceUuid());
        VmInstanceState state = q.findValue();
        if (!consoleAvailableStates.contains(state)) {
            throw new ApiMessageInterceptionException(operr(ORG_ZSTACK_CONSOLE_10010, "vm[uuid:%s] is not in state of %s, current state is %s",
                    msg.getVmInstanceUuid(), consoleAvailableStates, state));
        }
        bus.makeTargetServiceIdByResourceUuid(msg, ConsoleConstants.SERVICE_ID, msg.getVmInstanceUuid());
    }

    private void validate(APIUpdateConsoleProxyAgentMsg msg) {
        msg.setConsoleProxyOverriddenIp(normalizeOptionalConsoleProxyHost(msg.getConsoleProxyOverriddenIp()));
        msg.setConsoleProxyOverriddenIpv4(normalizeOptionalConsoleProxyHost(msg.getConsoleProxyOverriddenIpv4()));
        msg.setConsoleProxyOverriddenIpv6(normalizeOptionalConsoleProxyHost(msg.getConsoleProxyOverriddenIpv6()));
        validateFamilySpecificAddress("consoleProxyOverriddenIpv4", msg.getConsoleProxyOverriddenIpv4(), true);
        validateFamilySpecificAddress("consoleProxyOverriddenIpv6", msg.getConsoleProxyOverriddenIpv6(), false);
        validateLegacyHostnameConflict(msg);
    }

    private String normalizeOptionalConsoleProxyHost(String host) {
        if (host == null) {
            return null;
        }

        String normalized = IPv6NetworkUtils.stripHostUrlBrackets(host.trim());
        return normalized;
    }

    private void validateFamilySpecificAddress(String fieldName, String host, boolean ipv4) {
        if (host == null || host.isEmpty()) {
            return;
        }
        if (ipv4 && "0.0.0.0".equals(host)) {
            return;
        }
        if (!ipv4 && "::".equals(host)) {
            return;
        }

        boolean valid = ipv4 ? NetworkUtils.isIpv4Address(host) : IPv6NetworkUtils.isIpv6Address(host);
        if (!valid) {
            throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_CONSOLE_10016,
                    "%s must be an %s address", fieldName, ipv4 ? "IPv4" : "IPv6"));
        }
    }

    private void validateLegacyHostnameConflict(APIUpdateConsoleProxyAgentMsg msg) {
        String legacy = msg.getConsoleProxyOverriddenIp();
        if (legacy == null || legacy.isEmpty() || "0.0.0.0".equals(legacy) || "::".equals(legacy)) {
            return;
        }
        if (NetworkUtils.isIpv4Address(legacy) || IPv6NetworkUtils.isIpv6Address(legacy)) {
            return;
        }

        if (msg.getConsoleProxyOverriddenIpv4() != null && !msg.getConsoleProxyOverriddenIpv4().isEmpty()) {
            throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_CONSOLE_10017,
                    "%s conflicts with %s; hostname legacy must be used by all clients",
                    "consoleProxyOverriddenIp", "consoleProxyOverriddenIpv4"));
        }
        if (msg.getConsoleProxyOverriddenIpv6() != null && !msg.getConsoleProxyOverriddenIpv6().isEmpty()) {
            throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_CONSOLE_10017,
                    "%s conflicts with %s; hostname legacy must be used by all clients",
                    "consoleProxyOverriddenIp", "consoleProxyOverriddenIpv6"));
        }
    }
}
