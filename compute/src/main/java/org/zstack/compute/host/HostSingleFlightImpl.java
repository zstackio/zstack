package org.zstack.compute.host;

import org.zstack.core.singleflight.CompletionSingleFlight;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.host.ConnectHostReply;

import java.util.function.Consumer;

/**
 * Created by Wenhao.Zhang on 20/11/11
 */
public class HostSingleFlightImpl implements HostSingleFlight {
    
    CompletionSingleFlight<ConnectHostReply> singleFlight = new CompletionSingleFlight<>();
    
    @Override
    public void executeConnect(HostBase host,
            Consumer<ReturnValueCompletion<ConnectHostReply>> getter,
            ReturnValueCompletion<ConnectHostReply> completion) {
        singleFlight.execute(hostKey(host), getter, completion);
    }
    
    private String hostKey(HostBase host) {
        return String.format("host-connect-%s", host.self.getUuid());
    }
    
}
