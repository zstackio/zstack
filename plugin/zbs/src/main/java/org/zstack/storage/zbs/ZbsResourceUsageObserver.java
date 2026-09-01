package org.zstack.storage.zbs;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.physicalserver.ManagedServiceResourceUsage;
import org.zstack.header.physicalserver.PhysicalServerCpuSet;
import org.zstack.header.physicalserver.PhysicalServerResourceAssignmentObserver;
import org.zstack.header.physicalserver.PhysicalServerResourceBoundary;
import org.zstack.header.physicalserver.PhysicalServerResourceUsageObserver;
import org.zstack.header.physicalserver.PhysicalServerRoleAssociationProvider;
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
        PhysicalServerResourceAssignmentObserver,
        PhysicalServerRoleAssociationProvider {
    public static final String ROLE_TYPE = "ZBS";
    public static final String ROLE_SERVICE_MANIFEST_PATH =
            "physical-server-roles/zbs.yaml";
    private static final CLogger logger = Utils.getLogger(
            ZbsResourceUsageObserver.class);
    private static final RoleServiceManifest ROLE_SERVICES =
            RoleServiceManifest.loadObservation(
                    ROLE_SERVICE_MANIFEST_PATH, ROLE_TYPE);

    private final AtomicReference<Map<String, ZbsNodeRef>> zbsRefs =
            new AtomicReference<>(Collections.emptyMap());

    @Autowired
    private PluginRegistry pluginRgty;
    @Override
    public String getRoleType() {
        return ROLE_TYPE;
    }

    @Override
    public Set<String> discoverAssociations(Collection<String> serverUuids) {
        return discoverRefs(serverUuids);
    }

    @Override
    public void collectResourceAssignment(
            String serverUuid,
            ReturnValueCompletion<PhysicalServerResourceBoundary> completion) {
        ZbsNodeRef ref = zbsRefs.get().get(serverUuid);
        if (ref == null) {
            completion.fail(operr(
                    ORG_ZSTACK_CORE_10000,
                    "ZBS_RESOURCE_RELATION_MISSING: physical server[uuid:%s] is not associated with ZBS",
                    serverUuid));
            return;
        }
        if (ref.getUnavailableError() != null) {
            completion.fail(ref.getUnavailableError());
            return;
        }
        ProviderResolution resolution = resolveProvider(ref);
        if (resolution.provider == null) {
            completion.fail(operr(
                    ORG_ZSTACK_CORE_10000,
                    "ZBS_RESOURCE_USAGE_PROVIDER_UNAVAILABLE: physical server[uuid:%s] has no unique available Provider",
                    serverUuid));
            return;
        }
        queryProvider(
                resolution.provider,
                ref,
                expectedCgroups(),
                new ReturnValueCompletion<List<ZbsCgroupResourceUsage>>(completion) {
                    @Override
                    public void success(List<ZbsCgroupResourceUsage> usages) {
                        try {
                            PhysicalServerResourceBoundary boundary =
                                    new PhysicalServerResourceBoundary();
                            String cpuSet = "";
                            for (ZbsCgroupResourceUsage usage :
                                    usagesByCgroup(usages).values()) {
                                String current = normalizeCpuSet(
                                        usage.getCpuSet());
                                if (!current.isEmpty()) {
                                    cpuSet = PhysicalServerCpuSet.union(
                                            cpuSet, current);
                                }
                            }
                            boundary.setCpuSet(cpuSet);
                            completion.success(boundary);
                        } catch (RuntimeException error) {
                            completion.fail(operr(
                                    ORG_ZSTACK_CORE_10000,
                                    "%s", error.getMessage()));
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
            String serverUuid,
            ReturnValueCompletion<List<ManagedServiceResourceUsage>> completion) {
        ZbsNodeRef ref = zbsRefs.get().get(serverUuid);
        ProviderResolution resolution = resolveProvider(ref);
        if (resolution.provider == null) {
            completion.success(unavailableUsages());
            return;
        }
        queryProvider(
                resolution.provider,
                ref,
                expectedCgroups(),
                new ReturnValueCompletion<List<ZbsCgroupResourceUsage>>(completion) {
                    @Override
                    public void success(List<ZbsCgroupResourceUsage> usages) {
                        try {
                            completion.success(toManagedServiceUsages(
                                    usages));
                        } catch (RuntimeException error) {
                            logger.warn(String.format(
                                    "invalid ZBS cgroup usage on physical server[uuid:%s]: %s",
                                    serverUuid, error.getMessage()));
                            completion.success(unavailableUsages());
                        }
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        logger.warn(String.format(
                                "failed to query ZBS cgroup usage on physical server[uuid:%s]: %s",
                                serverUuid, errorCode));
                        completion.success(unavailableUsages());
                    }
                });
    }

    private List<ManagedServiceResourceUsage> toManagedServiceUsages(
            List<ZbsCgroupResourceUsage> usages) {
        Map<String, ZbsCgroupResourceUsage> byCgroup =
                usagesByCgroup(usages);

        List<ManagedServiceResourceUsage> result =
                ROLE_SERVICES.managedServiceUsages("UNAVAILABLE");
        for (ManagedServiceResourceUsage service : result) {
            ZbsCgroupResourceUsage usage = byCgroup.get(
                    service.getServiceName());
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
            List<ZbsCgroupResourceUsage> usages) {
        if (usages == null) {
            throw new IllegalArgumentException(
                    "ZBS_RESOURCE_USAGE_INVALID: Provider returned null");
        }
        Set<String> expected = new HashSet<>(expectedCgroups());
        Map<String, ZbsCgroupResourceUsage> byCgroup = new LinkedHashMap<>();
        for (ZbsCgroupResourceUsage usage : usages) {
            validateUsage(usage, expected);
            if (byCgroup.put(usage.getCgroupName(), usage) != null) {
                throw new IllegalArgumentException(String.format(
                        "ZBS_RESOURCE_USAGE_INVALID: cgroupName[%s] is duplicated",
                        usage.getCgroupName()));
            }
        }
        return byCgroup;
    }

    private void validateUsage(
            ZbsCgroupResourceUsage usage,
            Set<String> expectedCgroups) {
        if (usage == null || usage.getCgroupName() == null
                || !expectedCgroups.contains(usage.getCgroupName())) {
            throw new IllegalArgumentException(String.format(
                    "ZBS_RESOURCE_USAGE_INVALID: cgroupName[%s] is not a configured ZBS cgroup",
                    usage == null ? null : usage.getCgroupName()));
        }
        normalizeCpuSet(usage.getCpuSet());
        requireNonNegative("cpuTime", usage.getCpuTime());
        requireNonNegative("memory", usage.getMemory());
        requireNonNegative("memoryLimit", usage.getMemoryLimit());
    }

    private void requireNonNegative(String field, Long value) {
        if (value != null && value < 0) {
            throw new IllegalArgumentException(String.format(
                    "ZBS_RESOURCE_USAGE_INVALID: %s[%s] must not be negative",
                    field, value));
        }
    }

    private String normalizeCpuSet(String cpuSet) {
        return cpuSet == null || cpuSet.trim().isEmpty()
                ? "" : PhysicalServerCpuSet.normalize(cpuSet);
    }

    private List<ManagedServiceResourceUsage> unavailableUsages() {
        return ROLE_SERVICES.managedServiceUsages("UNAVAILABLE");
    }

    private Set<String> discoverRefs(Collection<String> serverUuids) {
        List<ZbsNodeRefContributor> contributors =
                pluginRgty.getExtensionList(ZbsNodeRefContributor.class);
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
        while (true) {
            Map<String, ZbsNodeRef> current = zbsRefs.get();
            Map<String, ZbsNodeRef> replacement = new HashMap<>(current);
            for (String serverUuid : serverUuids) {
                replacement.remove(serverUuid);
            }
            replacement.putAll(loaded);
            if (zbsRefs.compareAndSet(
                    current, Collections.unmodifiableMap(replacement))) {
                return new HashSet<>(loaded.keySet());
            }
        }
    }

    private ProviderResolution resolveProvider(ZbsNodeRef ref) {
        if (ref == null || ref.getUnavailableError() != null) {
            return new ProviderResolution(null);
        }
        List<ZbsResourceUsageProvider> available = new ArrayList<>();
        for (ZbsResourceUsageProvider provider :
                pluginRgty.getExtensionList(ZbsResourceUsageProvider.class)) {
            if (provider.isAvailable(ref)) {
                available.add(provider);
            }
        }
        if (available.size() != 1) {
            return new ProviderResolution(null);
        }
        return new ProviderResolution(available.get(0));
    }

    private void queryProvider(
            ZbsResourceUsageProvider provider,
            ZbsNodeRef ref,
            Collection<String> cgroupNames,
            ReturnValueCompletion<List<ZbsCgroupResourceUsage>> completion) {
        try {
            provider.query(ref, cgroupNames, completion);
        } catch (RuntimeException error) {
            completion.fail(operr(
                    ORG_ZSTACK_CORE_10000,
                    "ZBS_RESOURCE_USAGE_QUERY_FAILED: Provider[%s] Query failed: %s",
                    provider.getProviderType(), error.getMessage()));
        }
    }

    private List<String> expectedCgroups() {
        List<String> result = new ArrayList<>();
        for (RoleServiceManifest.Service service : ROLE_SERVICES.getServices()) {
            result.add(service.getName());
        }
        return result;
    }

    private static class ProviderResolution {
        private final ZbsResourceUsageProvider provider;

        private ProviderResolution(ZbsResourceUsageProvider provider) {
            this.provider = provider;
        }
    }
}
