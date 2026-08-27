package org.zstack.physicalserver;

import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.physicalserver.PhysicalServerResourceControlAdapter;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PhysicalServerResourceControlAdapterRegistry {
    private static final CLogger logger = Utils.getLogger(
            PhysicalServerResourceControlAdapterRegistry.class);

    private final Map<String, PhysicalServerResourceControlAdapter> adapters;
    private final Map<String, String> errors;

    private PhysicalServerResourceControlAdapterRegistry(
            Map<String, PhysicalServerResourceControlAdapter> adapters,
            Map<String, String> errors) {
        this.adapters = adapters;
        this.errors = errors;
    }

    public static PhysicalServerResourceControlAdapterRegistry load(
            PluginRegistry pluginRegistry) {
        Map<String, List<PhysicalServerResourceControlAdapter>> candidates =
                new LinkedHashMap<>();
        for (PhysicalServerResourceControlAdapter adapter :
                pluginRegistry.getExtensionList(
                        PhysicalServerResourceControlAdapter.class)) {
            String roleType = roleType(adapter);
            if (roleType != null) {
                candidates.computeIfAbsent(
                        roleType, ignored -> new ArrayList<>()).add(adapter);
            }
        }

        Map<String, PhysicalServerResourceControlAdapter> adapters =
                new LinkedHashMap<>();
        Map<String, String> errors = new LinkedHashMap<>();
        for (Map.Entry<String, List<PhysicalServerResourceControlAdapter>> entry :
                candidates.entrySet()) {
            String error;
            try {
                error = validate(entry.getKey(), entry.getValue());
            } catch (RuntimeException failure) {
                error = String.format(
                        "RESOURCE_ASSIGNMENT_ADAPTER_INVALID: roleType[%s], error[%s]",
                        entry.getKey(), failure.getMessage());
            }
            if (error == null) {
                adapters.put(entry.getKey(), entry.getValue().get(0));
            } else {
                errors.put(entry.getKey(), error);
                logger.error(error);
            }
        }
        boolean removed;
        do {
            removed = false;
            for (Map.Entry<String, PhysicalServerResourceControlAdapter> entry :
                    new ArrayList<>(adapters.entrySet())) {
                String topologyRoleType = entry.getValue().getTopologyRoleType();
                if (!adapters.containsKey(topologyRoleType)) {
                    String error = String.format(
                            "RESOURCE_ASSIGNMENT_TOPOLOGY_ROLE_UNAVAILABLE: roleType[%s], topologyRoleType[%s]",
                            entry.getKey(), topologyRoleType);
                    adapters.remove(entry.getKey());
                    errors.put(entry.getKey(), error);
                    logger.error(error);
                    removed = true;
                }
            }
        } while (removed);
        return new PhysicalServerResourceControlAdapterRegistry(adapters, errors);
    }

    private static String roleType(PhysicalServerResourceControlAdapter adapter) {
        String value;
        try {
            value = adapter.getRoleType();
        } catch (RuntimeException error) {
            logger.error(String.format(
                    "failed to read role type from resource assignment adapter[%s]: %s",
                    adapter.getClass().getName(), error.getMessage()));
            return null;
        }
        if (value == null || !value.trim().matches("[A-Z][A-Z0-9_]{0,63}")) {
            logger.error(String.format(
                    "resource assignment adapter[%s] returned invalid roleType[%s]",
                    adapter.getClass().getName(), value));
            return null;
        }
        return value.trim();
    }

    private static String validate(
            String roleType,
            List<PhysicalServerResourceControlAdapter> candidates) {
        if (candidates.size() > 1) {
            return String.format(
                    "RESOURCE_ASSIGNMENT_ADAPTER_AMBIGUOUS: %s adapters are registered for roleType[%s]",
                    candidates.size(), roleType);
        }
        PhysicalServerResourceControlAdapter adapter = candidates.get(0);
        if (adapter.getIsolationMode() == null) {
            return String.format(
                    "RESOURCE_ASSIGNMENT_ISOLATION_MODE_MISSING: roleType[%s]",
                    roleType);
        }
        if (adapter.getApplicationMode() == null) {
            return String.format(
                    "RESOURCE_ASSIGNMENT_APPLICATION_MODE_MISSING: roleType[%s]",
                    roleType);
        }
        if (adapter.getTopologyRoleType() == null
                || adapter.getTopologyRoleType().trim().isEmpty()) {
            return String.format(
                    "RESOURCE_ASSIGNMENT_TOPOLOGY_ROLE_MISSING: roleType[%s]",
                    roleType);
        }
        return null;
    }

    public PhysicalServerResourceControlAdapter get(String roleType) {
        return adapters.get(roleType);
    }

    public String getError(String roleType) {
        return errors.get(roleType);
    }

    public boolean contains(String roleType) {
        return adapters.containsKey(roleType);
    }

    public List<String> orderedRoleTypes() {
        List<String> result = new ArrayList<>(adapters.keySet());
        result.sort(String::compareTo);
        return result;
    }

    public List<PhysicalServerResourceControlAdapter> orderedAdapters() {
        List<PhysicalServerResourceControlAdapter> result =
                new ArrayList<>(adapters.values());
        result.sort(Comparator.comparing(
                PhysicalServerResourceControlAdapter::getRoleType));
        return result;
    }

    public Collection<String> errors() {
        return errors.values();
    }
}
