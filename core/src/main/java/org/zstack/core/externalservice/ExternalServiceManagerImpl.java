package org.zstack.core.externalservice;

import org.zstack.header.errorcode.OperationFailureException;

import static org.zstack.core.Platform.operr;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ExternalServiceManagerImpl implements ExternalServiceManager {
    private Map<String, ExternalService> services = new HashMap<>();

    @Override
    public ExternalService registerService(ExternalService service) {
        if (services.containsKey(service.getName())) {
            throw new OperationFailureException(operr("service[%s] has been registered", service.getName()));
        }

        services.put(service.getName(), service);
        return service;
    }

    @Override
    public void deregisterService(String name) {
        services.remove(name);
    }

    @Override
    public ExternalService getService(String name) {
        return services.get(name);
    }

    @Override
    public ExternalService getService(String name, Supplier<ExternalService> supplier) {
        ExternalService service = services.get(name);
        if (service != null) {
            return service;
        }

        service = supplier.get();
        return registerService(service);
    }
}
