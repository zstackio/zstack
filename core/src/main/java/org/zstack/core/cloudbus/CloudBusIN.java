package org.zstack.core.cloudbus;

import com.rabbitmq.client.Connection;
import org.zstack.header.Component;
import org.zstack.header.Service;

public interface CloudBusIN extends CloudBus {
    Connection getConnection();
    
    void activeService(Service serv);

    void activeService(String id);
    
    void deActiveService(Service serv);
    
    void deActiveService(String id);
}
