package org.zstack.physicalserver;

import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.physicalserver.PhysicalServerResourceControlAdapter;
import org.zstack.header.physicalserver.PhysicalServerResourceUsageObserver;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PhysicalServerResourceUsageObserverRegistry {
    private static final CLogger logger = Utils.getLogger(
            PhysicalServerResourceUsageObserverRegistry.class);

    private final Map<String, PhysicalServerResourceUsageObserver> observers;

    private PhysicalServerResourceUsageObserverRegistry(
            Map<String, PhysicalServerResourceUsageObserver> observers) {
        this.observers = observers;
    }

    public static PhysicalServerResourceUsageObserverRegistry load(
            PluginRegistry pluginRegistry) {
        Map<String, List<PhysicalServerResourceUsageObserver>> candidates =
                new LinkedHashMap<>();
        addCandidates(
                candidates,
                pluginRegistry.getExtensionList(
                        PhysicalServerResourceControlAdapter.class));
        addCandidates(
                candidates,
                pluginRegistry.getExtensionList(
                        PhysicalServerResourceUsageObserver.class));

        Map<String, PhysicalServerResourceUsageObserver> observers =
                new LinkedHashMap<>();
        for (Map.Entry<String, List<PhysicalServerResourceUsageObserver>> entry :
                candidates.entrySet()) {
            if (entry.getValue().size() == 1) {
                observers.put(entry.getKey(), entry.getValue().get(0));
                continue;
            }
            logger.error(String.format(
                    "RESOURCE_USAGE_OBSERVER_AMBIGUOUS: %s observers are registered for roleType[%s]",
                    entry.getValue().size(), entry.getKey()));
        }
        return new PhysicalServerResourceUsageObserverRegistry(observers);
    }

    private static void addCandidates(
            Map<String, List<PhysicalServerResourceUsageObserver>> candidates,
            List<? extends PhysicalServerResourceUsageObserver> extensions) {
        if (extensions == null) {
            return;
        }
        for (PhysicalServerResourceUsageObserver observer : extensions) {
            String roleType = roleType(observer);
            if (roleType == null) {
                continue;
            }
            List<PhysicalServerResourceUsageObserver> roleObservers =
                    candidates.computeIfAbsent(
                            roleType, ignored -> new ArrayList<>());
            if (!roleObservers.contains(observer)) {
                roleObservers.add(observer);
            }
        }
    }

    private static String roleType(
            PhysicalServerResourceUsageObserver observer) {
        String value;
        try {
            value = observer.getRoleType();
        } catch (RuntimeException error) {
            logger.error(String.format(
                    "failed to read role type from resource usage observer[%s]: %s",
                    observer.getClass().getName(), error.getMessage()));
            return null;
        }
        if (value == null || !value.trim().matches("[A-Z][A-Z0-9_]{0,63}")) {
            logger.error(String.format(
                    "resource usage observer[%s] returned invalid roleType[%s]",
                    observer.getClass().getName(), value));
            return null;
        }
        return value.trim();
    }

    public PhysicalServerResourceUsageObserver get(String roleType) {
        return observers.get(roleType);
    }

    public List<PhysicalServerResourceUsageObserver> orderedObservers() {
        List<PhysicalServerResourceUsageObserver> result =
                new ArrayList<>(observers.values());
        result.sort(Comparator.comparing(
                PhysicalServerResourceUsageObserver::getRoleType));
        return result;
    }
}
