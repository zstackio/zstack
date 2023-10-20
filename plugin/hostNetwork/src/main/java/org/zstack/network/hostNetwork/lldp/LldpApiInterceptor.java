package org.zstack.network.hostNetwork.lldp;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.message.APIMessage;
import org.zstack.network.hostNetwork.HostNetworkInterfaceVO;
import org.zstack.network.hostNetwork.HostNetworkInterfaceVO_;
import org.zstack.network.hostNetwork.lldp.api.APIChangeHostNetworkInterfaceLldpModeMsg;
import org.zstack.network.hostNetwork.lldp.api.APIGetHostNetworkInterfaceLldpMsg;
import org.zstack.network.hostNetwork.lldp.entity.HostNetworkInterfaceLldpVO;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.logging.CLoggerImpl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.zstack.core.Platform.argerr;

public class LldpApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;

    private static final CLogger logger = CLoggerImpl.getLogger(LldpApiInterceptor.class);

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIChangeHostNetworkInterfaceLldpModeMsg) {
            validate((APIChangeHostNetworkInterfaceLldpModeMsg)msg);
        } else if (msg instanceof APIGetHostNetworkInterfaceLldpMsg) {
            validate((APIGetHostNetworkInterfaceLldpMsg)msg);
        }

        return msg;
    }

    private void validate(APIChangeHostNetworkInterfaceLldpModeMsg msg) {
        List<String> hostUuids = Q.New(HostNetworkInterfaceVO.class).select(HostNetworkInterfaceVO_.hostUuid)
                .in(HostNetworkInterfaceVO_.uuid, msg.getInterfaceUuids())
                .listValues();
        Set<String> set = new HashSet<>(hostUuids);
        if (set.size() > 1) {
            throw new ApiMessageInterceptionException((argerr("could not change lldp mode for the interfaces of different hosts")));
        }
    }

    private void validate(APIGetHostNetworkInterfaceLldpMsg msg) {
        HostNetworkInterfaceLldpVO interfaceLldpVO = dbf.findByUuid(msg.getInterfaceUuid(), HostNetworkInterfaceLldpVO.class);
        if (interfaceLldpVO != null && !interfaceLldpVO.getMode().contains("rx")) {
            throw new ApiMessageInterceptionException((argerr("could not get interface lldp info which mode is not in receive mode")));
        }
    }
}
