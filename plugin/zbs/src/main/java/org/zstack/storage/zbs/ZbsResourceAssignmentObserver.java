package org.zstack.storage.zbs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.physicalserver.*;
import static org.zstack.core.Platform.operr;
import static org.zstack.storage.zbs.ZbsResourceAssignmentFactory.*;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_CORE_10000;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE, dependencyCheck = true)
public class ZbsResourceAssignmentObserver implements PhysicalServerResourceAssignmentObserver,
        PhysicalServerResourceUsageObserver {
    private final String serverUuid;
    private ZbsNodeRef ref;

    public ZbsResourceAssignmentObserver(String serverUuid) {
        this.serverUuid = serverUuid;
    }

    @Autowired
    private PluginRegistry pluginRgty;

    @Override
    public void collectResourceAssignment(String serverUuid, List<String> serviceNames,
            ReturnValueCompletion<PhysicalServerResourceBoundary> completion) {
        ZbsNodeRef ref = requireRef(serverUuid);
        ZbsResourceUsageProvider provider = requireProvider(serverUuid, ref);
        provider.query(ref, serviceNames, new ReturnValueCompletion<List<ZbsCgroupResourceUsage>>(completion) {
                    @Override
                    public void success(List<ZbsCgroupResourceUsage> usages) {
                        PhysicalServerResourceBoundary boundary = new PhysicalServerResourceBoundary();
                        String cpuSet = "";
                        for (ZbsCgroupResourceUsage usage : usagesByCgroup(usages, serviceNames).values()) {
                            String current = normalizeCpuSet(usage.getCpuSet());
                            if (!current.isEmpty()) {
                                cpuSet = PhysicalServerCpuSet.union(cpuSet, current);
                            }
                        }
                        boundary.setCpuSet(cpuSet);
                        completion.success(boundary);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                    }
                });
    }

    @Override
    public void collectManagedServiceUsage(
            String serverUuid, ResourceControlCommand command,
            ReturnValueCompletion<List<ManagedServiceResourceUsage>> completion) {
        List<String> serviceNames = command.getHandles().stream().map(ResourceConsumerHandle::getServiceName)
                .collect(Collectors.toList());
        ZbsNodeRef ref = requireRef(serverUuid);
        ZbsResourceUsageProvider provider = requireProvider(serverUuid, ref);
        provider.query(ref, serviceNames, new ReturnValueCompletion<List<ZbsCgroupResourceUsage>>(completion) {
                    @Override
                    public void success(List<ZbsCgroupResourceUsage> usages) {
                        completion.success(toManagedServiceUsages(usages, serviceNames));
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                    }
                });
    }

    private List<ManagedServiceResourceUsage> toManagedServiceUsages(
            List<ZbsCgroupResourceUsage> usages, List<String> serviceNames) {
        Map<String, ZbsCgroupResourceUsage> byCgroup = usagesByCgroup(usages, serviceNames);

        List<ManagedServiceResourceUsage> result = new ArrayList<>();
        for (String serviceName : serviceNames) {
            ManagedServiceResourceUsage service = new ManagedServiceResourceUsage();
            service.setRoleType(type.toString());
            service.setServiceName(serviceName);
            service.setState("NOT_FOUND");
            result.add(service);
            ZbsCgroupResourceUsage usage = byCgroup.get(service.getServiceName());
            if (usage == null) {
                continue;
            }
            service.setState("RUNNING");
            service.setCpuSet(normalizeCpuSet(usage.getCpuSet()));
            service.setCpuTime(usage.getCpuTime());
            service.setMemory(usage.getMemory());
            service.setMemoryLimit(usage.getMemoryLimit());
        }
        return result;
    }

    private Map<String, ZbsCgroupResourceUsage> usagesByCgroup(
            List<ZbsCgroupResourceUsage> usages, List<String> serviceNames) {
        if (usages == null) {
            throw new IllegalArgumentException("ZBS resource usage provider returned null");
        }
        Set<String> expected = new HashSet<>(serviceNames);
        Map<String, ZbsCgroupResourceUsage> byCgroup = new LinkedHashMap<>();
        for (ZbsCgroupResourceUsage usage : usages) {
            validateUsage(usage, expected);
            if (byCgroup.put(usage.getCgroupName(), usage) != null) {
                throw new IllegalArgumentException(String.format(
                        "ZBS resource usage contains duplicate cgroupName[%s]", usage.getCgroupName()));
            }
        }
        return byCgroup;
    }

    private void validateUsage(ZbsCgroupResourceUsage usage, Set<String> expectedCgroups) {
        if (usage == null || usage.getCgroupName() == null || !expectedCgroups.contains(usage.getCgroupName())) {
            throw new IllegalArgumentException(String.format(
                    "CgroupName[%s] is not configured for the ZBS role", usage == null ? null : usage.getCgroupName()));
        }
        normalizeCpuSet(usage.getCpuSet());
        requireNonNegative("cpuTime", usage.getCpuTime());
        requireNonNegative("memory", usage.getMemory());
        requireNonNegative("memoryLimit", usage.getMemoryLimit());
    }

    private void requireNonNegative(String field, Long value) {
        if (value != null && value < 0) {
            throw new IllegalArgumentException(String.format(
                    "ZBS resource usage %s[%s] must not be negative", field, value));
        }
    }

    private String normalizeCpuSet(String cpuSet) {
        return cpuSet == null || cpuSet.trim().isEmpty() ? "" : PhysicalServerCpuSet.normalize(cpuSet);
    }

    private ZbsNodeRef requireRef(String serverUuid) {
        if (!resourceExists()) {
            throw new OperationFailureException(operr(
                    ORG_ZSTACK_CORE_10000, "Physical server[uuid:%s] is not associated with ZBS", serverUuid));
        }
        return ref;
    }

    @Override
    public PhysicalServerRoleType getRoleType() {
        return type;
    }

    @Override
    public boolean resourceExists() {
        if (ref == null) {
            ref = findRef(serverUuid);
        }
        return ref != null;
    }

    private ZbsNodeRef findRef(String serverUuid) {
        ZbsNodeRef ref = null;
        for (ZbsNodeRefContributor contributor : pluginRgty.getExtensionList(ZbsNodeRefContributor.class)) {
            ZbsNodeRef candidate = contributor.getNodesByServerUuids(Collections.singleton(serverUuid)).get(serverUuid);
            if (candidate != null) {
                if (ref != null) {
                    throw new OperationFailureException(operr(ORG_ZSTACK_CORE_10000,
                            "Multiple ZBS node contributors returned server[%s]", serverUuid));
                }
                ref = candidate;
            }
        }
        return ref;
    }

    private ZbsResourceUsageProvider requireProvider(String serverUuid, ZbsNodeRef ref) {
        ZbsResourceUsageProvider provider = resolveProvider(ref);
        if (provider == null) {
            throw new OperationFailureException(operr(ORG_ZSTACK_CORE_10000,
                    "Physical server[uuid:%s] has no unique available ZBS resource usage provider", serverUuid));
        }
        return provider;
    }

    private ZbsResourceUsageProvider resolveProvider(ZbsNodeRef ref) {
        if (ref == null) {
            return null;
        }
        List<ZbsResourceUsageProvider> available = new ArrayList<>();
        for (ZbsResourceUsageProvider provider : pluginRgty.getExtensionList(ZbsResourceUsageProvider.class)) {
            if (provider.isAvailable(ref)) {
                available.add(provider);
            }
        }
        if (available.size() != 1) {
            return null;
        }
        return available.get(0);
    }

}
