package org.zstack.physicalserver;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.message.APIMessage;
import org.zstack.header.physicalserver.PhysicalServerCpuSet;
import org.zstack.header.physicalserver.PhysicalServerResourceControlAdapter;
import org.zstack.utils.data.SizeUnit;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.zstack.core.Platform.argerr;

public class PhysicalServerApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    private CloudBus bus;
    @Autowired
    private PluginRegistry pluginRgty;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIUpdatePhysicalServerResourceAssignmentMsg) {
            validate((APIUpdatePhysicalServerResourceAssignmentMsg) msg);
        } else if (msg instanceof APIRefreshPhysicalServerResourceAssignmentsMsg) {
            validate((APIRefreshPhysicalServerResourceAssignmentsMsg) msg);
        }

        if (msg instanceof PhysicalServerMessage) {
            bus.makeTargetServiceIdByResourceUuid(
                    msg,
                    PhysicalServerConstant.SERVICE_ID,
                    PhysicalServerConstant.CONTROL_OWNER_KEY);
        }
        return msg;
    }

    private void validate(APIRefreshPhysicalServerResourceAssignmentsMsg msg) {
        List<String> serviceNames = msg.getServiceNames();
        boolean restartServices = serviceNames != null && !serviceNames.isEmpty();
        if (!restartServices) {
            if (msg.getRoleType() != null) {
                throw new ApiMessageInterceptionException(argerr(
                        PhysicalServerConstant.ERROR_CODE,
                        "SERVICE_NAMES_REQUIRED: roleType is only valid when serviceNames are specified"));
            }
            return;
        }
        if (msg.getRoleType() == null || adapters().get(msg.getRoleType()) == null) {
            throw new ApiMessageInterceptionException(argerr(
                    PhysicalServerConstant.ERROR_CODE,
                    "ROLE_TYPE_NOT_SUPPORTED: roleType[%s] is not registered",
                    msg.getRoleType()));
        }
        if (serviceNames.size() > 64) {
            throw new ApiMessageInterceptionException(argerr(
                    PhysicalServerConstant.ERROR_CODE,
                    "SERVICE_NAME_SET_INVALID: at most 64 services can be restarted"));
        }
        Set<String> unique = new HashSet<>();
        for (String serviceName : serviceNames) {
            if (serviceName == null
                    || !serviceName.matches("[A-Za-z0-9][A-Za-z0-9_.-]{0,63}")
                    || !unique.add(serviceName)) {
                throw new ApiMessageInterceptionException(argerr(
                        PhysicalServerConstant.ERROR_CODE,
                        "SERVICE_NAME_SET_INVALID: service names must be unique stable names"));
            }
        }
    }

    private void validate(APIUpdatePhysicalServerResourceAssignmentMsg msg) {
        PhysicalServerResourceControlAdapter adapter =
                adapters().get(msg.getRoleType());
        if (adapter == null) {
            throw new ApiMessageInterceptionException(argerr(
                    PhysicalServerConstant.ERROR_CODE,
                    "ROLE_TYPE_NOT_SUPPORTED: roleType[%s] is not registered", msg.getRoleType()));
        }

        if (msg.getCpuSet() == null && msg.getMemory() == null) {
            throw new ApiMessageInterceptionException(argerr(
                    PhysicalServerConstant.ERROR_CODE,
                    "RESOURCE_ASSIGNMENT_UPDATE_EMPTY: cpuSet or memory must be specified"));
        }

        if (msg.getMemory() != null) {
            long mebibyte = SizeUnit.MEGABYTE.toByte(1);
            if (msg.getMemory() < 0 || msg.getMemory() % mebibyte != 0) {
                throw new ApiMessageInterceptionException(argerr(
                        PhysicalServerConstant.ERROR_CODE,
                        "MEMORY_INVALID: memory[%s] must be 0 or a positive multiple of 1 MiB",
                        msg.getMemory()));
            }
        }

        if (msg.getCpuSet() == null) {
            return;
        }
        try {
            msg.setCpuSet(PhysicalServerCpuSet.normalize(msg.getCpuSet()));
        } catch (IllegalArgumentException error) {
            throw new ApiMessageInterceptionException(argerr(
                    PhysicalServerConstant.ERROR_CODE,
                    "%s", error.getMessage()));
        }
    }

    private PhysicalServerResourceControlAdapterRegistry adapters() {
        return PhysicalServerResourceControlAdapterRegistry.load(pluginRgty);
    }
}
