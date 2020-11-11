package org.zstack.compute.host;

import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.host.ConnectHostReply;

import java.util.function.Consumer;

public interface HostSingleFlight {
    
    void executeConnect(HostBase host,
                        Consumer<ReturnValueCompletion<ConnectHostReply>> getter,
                        ReturnValueCompletion<ConnectHostReply> completion);
    
}
