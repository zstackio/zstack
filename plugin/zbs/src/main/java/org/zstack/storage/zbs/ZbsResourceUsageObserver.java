package org.zstack.storage.zbs;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.physicalserver.ManagedServiceResourceUsage;
import org.zstack.header.physicalserver.PhysicalServerCpuSet;
import org.zstack.header.physicalserver.PhysicalServerResourceAssignmentObserver;
import org.zstack.header.physicalserver.PhysicalServerResourceBoundary;
import org.zstack.header.physicalserver.PhysicalServerResourceIsolationMode;
import org.zstack.header.physicalserver.PhysicalServerResourceUsageObserver;
import org.zstack.header.physicalserver.PhysicalServerRoleAssociationProvider;
import org.zstack.header.physicalserver.PhysicalServerRoleType;
import org.zstack.header.physicalserver.RoleServiceManifest;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_CORE_10000;

public class ZbsResourceUsageObserver implements
        PhysicalServerResourceUsageObserver,
        PhysicalServerResourceAssignmentObserver, PhysicalServerRoleAssociationProvider {
    public static final PhysicalServerRoleType type = new PhysicalServerRoleType("ZBS");
    public static final String ROLE_SERVICE_MANIFEST_PATH = "physical-server-roles/zbs.yaml";
    private static final CLogger logger = Utils.getLogger(ZbsResourceUsageObserver.class);
    private final AtomicReference<Map<String, ZbsNodeRef>> zbsRefs = new AtomicReference<>(Collections.emptyMap());

    @Autowired
    private PluginRegistry pluginRgty;
    @Override
    public PhysicalServerRoleType getRoleType() {
        return type;
    }

    @Override
    public PhysicalServerResourceIsolationMode getIsolationMode() {
        return roleServices().getIsolationMode();
    }

    @Override
    public Set<String> discoverAssociations(Collection<String> serverUuids) {
        return discoverRefs(serverUuids);
    }

    @Override
    public void collectResourceAssignment(
            String serverUuid, ReturnValueCompletion<PhysicalServerResourceBoundary> completion) {
        RoleServiceManifest roleServices = roleServices();
        ZbsNodeRef ref;
        ZbsResourceUsageProvider provider;
        try {
            ref = requireRef(serverUuid);
            provider = requireProvider(serverUuid, ref);
        } catch (OperationFailureException error) {
            completion.fail(error.getErrorCode());
            return;
        }
        queryProvider(
                provider,
                ref,
                expectedCgroups(roleServices), new ReturnValueCompletion<List<ZbsCgroupResourceUsage>>(completion) {
                    @Override
                    public void success(List<ZbsCgroupResourceUsage> usages) {
                        try {
                            PhysicalServerResourceBoundary boundary = new PhysicalServerResourceBoundary();
                            String cpuSet = "";
                            for (ZbsCgroupResourceUsage usage : usagesByCgroup(usages, roleServices).values()) {
                                String current = normalizeCpuSet(usage.getCpuSet());
                                if (!current.isEmpty()) {
                                    cpuSet = PhysicalServerCpuSet.union(cpuSet, current);
                                }
                            }
                            boundary.setCpuSet(cpuSet);
                            completion.success(boundary);
                        } catch (RuntimeException error) {
                            completion.fail(operr(ORG_ZSTACK_CORE_10000, "%s", error.getMessage()));
                        }
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                    }
                });
    }

    @Override
    public void collectManagedServiceUsage(
            String serverUuid, ReturnValueCompletion<List<ManagedServiceResourceUsage>> completion) {
        RoleServiceManifest roleServices = roleServices();
        ZbsNodeRef ref;
        ZbsResourceUsageProvider provider;
        try {
            ref = requireRef(serverUuid);
            provider = requireProvider(serverUuid, ref);
        } catch (OperationFailureException error) {
            completion.fail(error.getErrorCode());
            return;
        }
        queryProvider(
                provider,
                ref,
                expectedCgroups(roleServices), new ReturnValueCompletion<List<ZbsCgroupResourceUsage>>(completion) {
                    @Override
                    public void success(List<ZbsCgroupResourceUsage> usages) {
                        try {
                            completion.success(toManagedServiceUsages(usages, roleServices));
                        } catch (RuntimeException error) {
                            completion.fail(operr(ORG_ZSTACK_CORE_10000, "%s", error.getMessage()));
                        }
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                    }
                });
    }

    private List<ManagedServiceResourceUsage> toManagedServiceUsages(
            List<ZbsCgroupResourceUsage> usages, RoleServiceManifest roleServices) {
        Map<String, ZbsCgroupResourceUsage> byCgroup = usagesByCgroup(usages, roleServices);

        List<ManagedServiceResourceUsage> result = roleServices.managedServiceUsages("NOT_FOUND");
        for (ManagedServiceResourceUsage service : result) {
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
            List<ZbsCgroupResourceUsage> usages, RoleServiceManifest roleServices) {
        if (usages == null) {
            throw new IllegalArgumentException("ZBS resource usage provider returned null");
        }
        Set<String> expected = new HashSet<>(expectedCgroups(roleServices));
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

    private Set<String> discoverRefs(Collection<String> serverUuids) {
        List<ZbsNodeRefContributor> contributors = pluginRgty.getExtensionList(ZbsNodeRefContributor.class);
        Map<String, ZbsNodeRef> loaded = new HashMap<>();
        RuntimeException discoveryFailure = null;
        for (ZbsNodeRefContributor contributor : contributors) {
            Map<String, ZbsNodeRef> contribution;
            try {
                contribution = contributor.bulkList(serverUuids);
            } catch (RuntimeException error) {
                if (discoveryFailure == null) {
                    discoveryFailure = error;
                }
                logger.warn(String.format(
                        "failed to discover ZBS node relations from contributor[%s]: %s",
                        contributor.getClass().getName(), error.getMessage()));
                continue;
            }
            for (Map.Entry<String, ZbsNodeRef> entry : contribution.entrySet()) {
                if (loaded.put(entry.getKey(), entry.getValue()) != null) {
                    entry.getValue().setUnavailableError(operr(
                            ORG_ZSTACK_CORE_10000,
                            "multiple ZBS node relation contributors returned physical server[uuid:%s]",
                            entry.getKey()));
                }
            }
        }
        if (discoveryFailure != null) {
            throw discoveryFailure;
        }
        if (serverUuids == null || serverUuids.isEmpty()) {
            zbsRefs.set(Collections.unmodifiableMap(loaded));
            return new HashSet<>(loaded.keySet());
        }
        zbsRefs.updateAndGet(current -> {
            Map<String, ZbsNodeRef> replacement = new HashMap<>(current);
            serverUuids.forEach(replacement::remove);
            replacement.putAll(loaded);
            return Collections.unmodifiableMap(replacement);
        });
        return new HashSet<>(loaded.keySet());
    }

    private ZbsNodeRef requireRef(String serverUuid) {
        ZbsNodeRef ref = zbsRefs.get().get(serverUuid);
        if (ref == null) {
            throw new OperationFailureException(operr(
                    ORG_ZSTACK_CORE_10000, "Physical server[uuid:%s] is not associated with ZBS", serverUuid));
        }
        if (ref.getUnavailableError() != null) {
            throw new OperationFailureException(ref.getUnavailableError());
        }
        return ref;
    }

    private ZbsResourceUsageProvider requireProvider(String serverUuid, ZbsNodeRef ref) {
        ZbsResourceUsageProvider provider = resolveProvider(ref);
        if (provider == null) {
            throw new OperationFailureException(operr(
                    ORG_ZSTACK_CORE_10000,
                    "Physical server[uuid:%s] has no unique available ZBS resource usage provider", serverUuid));
        }
        return provider;
    }

    private ZbsResourceUsageProvider resolveProvider(ZbsNodeRef ref) {
        if (ref == null || ref.getUnavailableError() != null) {
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

    private void queryProvider(
            ZbsResourceUsageProvider provider,
            ZbsNodeRef ref,
            Collection<String> cgroupNames, ReturnValueCompletion<List<ZbsCgroupResourceUsage>> completion) {
        try {
            provider.query(ref, cgroupNames, completion);
        } catch (RuntimeException error) {
            completion.fail(operr(
                    ORG_ZSTACK_CORE_10000,
                    "ZBS resource usage provider[%s] query failed: %s",
                    provider.getClass().getName(), error.getMessage()));
        }
    }

    private List<String> expectedCgroups(RoleServiceManifest roleServices) {
        List<String> result = new ArrayList<>();
        for (RoleServiceManifest.Service service : roleServices.getServices()) {
            result.add(service.getName());
        }
        return result;
    }

    private RoleServiceManifest roleServices() {
        return RoleServiceManifest.loadObservation(ROLE_SERVICE_MANIFEST_PATH, type.toString());
    }

}
