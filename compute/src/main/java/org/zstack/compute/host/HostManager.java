package org.zstack.compute.host;

import org.zstack.header.Service;
import org.zstack.header.host.HostMessageHandlerExtensionPoint;
import org.zstack.header.host.HypervisorFactory;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.message.Message;

public interface HostManager {
    HypervisorFactory getHypervisorFactory(HypervisorType type);
    
	void handleMessage(Message msg);
	
	HostMessageHandlerExtensionPoint getHostMessageHandlerExtension(Message msg);
}
