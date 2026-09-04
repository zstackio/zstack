package org.zstack.physicalserver;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestResponse(fieldsTo = {"services", "roleErrors"})
public class APIGetPhysicalServerManagedServicesReply extends APIReply {
    private List<PhysicalServerManagedServiceInventory> services = new ArrayList<>();
    private Map<String, ErrorCode> roleErrors = new LinkedHashMap<>();

    public List<PhysicalServerManagedServiceInventory> getServices() {
        return services;
    }

    public void setServices(List<PhysicalServerManagedServiceInventory> services) {
        this.services = services;
    }

    public Map<String, ErrorCode> getRoleErrors() {
        return roleErrors;
    }

    public void setRoleErrors(Map<String, ErrorCode> roleErrors) {
        this.roleErrors = roleErrors;
    }

    public static APIGetPhysicalServerManagedServicesReply __example__() {
        PhysicalServerManagedServiceInventory service = new PhysicalServerManagedServiceInventory();
        service.setRoleType("COMPUTE");
        service.setServiceName("node-exporter");
        service.setRestartable(true);
        service.setRestartRequired(true);
        service.setState("RUNNING");
        service.setCpuSet("0-3");
        service.setCpuTime(12000000000L);
        service.setMemory(96L * 1024 * 1024);
        service.setMemoryLimit(4L * 1024 * 1024 * 1024);
        APIGetPhysicalServerManagedServicesReply reply = new APIGetPhysicalServerManagedServicesReply();
        reply.setServices(java.util.Collections.singletonList(service));
        reply.getRoleErrors().put("ZBS", new ErrorCode(
                PhysicalServerConstant.ERROR_CODE, "Operation Error", "failed to query ZBS managed services"));
        return reply;
    }
}
