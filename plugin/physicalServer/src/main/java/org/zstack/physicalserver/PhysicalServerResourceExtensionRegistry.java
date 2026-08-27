package org.zstack.physicalserver;

import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.physicalserver.PhysicalServerResourceAssignmentController;
import org.zstack.header.physicalserver.PhysicalServerResourceAssignmentObserver;
import org.zstack.header.physicalserver.PhysicalServerResourceUsageObserver;
import org.zstack.header.physicalserver.PhysicalServerRoleAssociationProvider;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class PhysicalServerResourceExtensionRegistry {
    private static final CLogger logger = Utils.getLogger(
            PhysicalServerResourceExtensionRegistry.class);

    private final Map<String, PhysicalServerRoleAssociationProvider> associations;
    private final Map<String, PhysicalServerResourceAssignmentObserver> observers;
    private final Map<String, PhysicalServerResourceAssignmentController> controllers;
    private final Map<String, PhysicalServerResourceUsageObserver> usageObservers;
    private final Map<String, String> controllerErrors;

    private PhysicalServerResourceExtensionRegistry(
            Map<String, PhysicalServerRoleAssociationProvider> associations,
            Map<String, PhysicalServerResourceAssignmentObserver> observers,
            Map<String, PhysicalServerResourceAssignmentController> controllers,
            Map<String, PhysicalServerResourceUsageObserver> usageObservers,
            Map<String, String> controllerErrors) {
        this.associations = associations;
        this.observers = observers;
        this.controllers = controllers;
        this.usageObservers = usageObservers;
        this.controllerErrors = controllerErrors;
    }

    public static PhysicalServerResourceExtensionRegistry load(
            PluginRegistry pluginRegistry) {
        List<PhysicalServerResourceAssignmentController> controllerExtensions =
                pluginRegistry.getExtensionList(
                        PhysicalServerResourceAssignmentController.class);
        LoadResult<PhysicalServerResourceAssignmentController> controllers =
                loadControllers(controllerExtensions);

        List<PhysicalServerResourceAssignmentObserver> observerExtensions =
                new ArrayList<>(pluginRegistry.getExtensionList(
                        PhysicalServerResourceAssignmentObserver.class));
        for (PhysicalServerResourceAssignmentController controller :
                controllerExtensions) {
            if (!observerExtensions.contains(controller)) {
                observerExtensions.add(controller);
            }
        }

        Map<String, PhysicalServerRoleAssociationProvider> associations = unique(
                "RESOURCE_ASSOCIATION_PROVIDER",
                pluginRegistry.getExtensionList(
                        PhysicalServerRoleAssociationProvider.class),
                PhysicalServerRoleAssociationProvider::getRoleType);
        Map<String, PhysicalServerResourceAssignmentObserver> observers = unique(
                "RESOURCE_ASSIGNMENT_OBSERVER",
                observerExtensions,
                PhysicalServerResourceAssignmentObserver::getRoleType);
        Map<String, PhysicalServerResourceUsageObserver> usageObservers = unique(
                "RESOURCE_USAGE_OBSERVER",
                pluginRegistry.getExtensionList(
                        PhysicalServerResourceUsageObserver.class),
                PhysicalServerResourceUsageObserver::getRoleType);

        removeControllersWithoutAssociations(
                controllers.values, controllers.errors, associations);
        removeObserversWithoutAssociations(observers, associations);
        removeInvalidTopologyControllers(
                controllers.values, controllers.errors);
        return new PhysicalServerResourceExtensionRegistry(
                associations,
                observers,
                controllers.values,
                usageObservers,
                controllers.errors);
    }

    private static LoadResult<PhysicalServerResourceAssignmentController>
            loadControllers(
                    List<PhysicalServerResourceAssignmentController> extensions) {
        Map<String, List<PhysicalServerResourceAssignmentController>> candidates =
                candidates(extensions,
                        PhysicalServerResourceAssignmentController::getRoleType);
        Map<String, PhysicalServerResourceAssignmentController> result =
                new LinkedHashMap<>();
        Map<String, String> errors = new LinkedHashMap<>();
        for (Map.Entry<String, List<PhysicalServerResourceAssignmentController>> entry :
                candidates.entrySet()) {
            String error = validateController(entry.getKey(), entry.getValue());
            if (error == null) {
                result.put(entry.getKey(), entry.getValue().get(0));
            } else {
                errors.put(entry.getKey(), error);
                logger.error(error);
            }
        }
        return new LoadResult<>(result, errors);
    }

    private static String validateController(
            String roleType,
            List<PhysicalServerResourceAssignmentController> candidates) {
        if (candidates.size() != 1) {
            return String.format(
                    "RESOURCE_ASSIGNMENT_CONTROLLER_AMBIGUOUS: %s controllers are registered for roleType[%s]",
                    candidates.size(), roleType);
        }
        PhysicalServerResourceAssignmentController controller = candidates.get(0);
        if (controller.getIsolationMode() == null) {
            return String.format(
                    "RESOURCE_ASSIGNMENT_ISOLATION_MODE_MISSING: roleType[%s]",
                    roleType);
        }
        if (empty(controller.getTopologyRoleType())) {
            return String.format(
                    "RESOURCE_ASSIGNMENT_TOPOLOGY_ROLE_MISSING: roleType[%s]",
                    roleType);
        }
        return null;
    }

    private static void removeControllersWithoutAssociations(
            Map<String, PhysicalServerResourceAssignmentController> controllers,
            Map<String, String> errors,
            Map<String, PhysicalServerRoleAssociationProvider> associations) {
        for (String roleType : new ArrayList<>(controllers.keySet())) {
            if (associations.containsKey(roleType)) {
                continue;
            }
            String error = String.format(
                    "RESOURCE_ASSIGNMENT_ASSOCIATION_PROVIDER_MISSING: roleType[%s]",
                    roleType);
            controllers.remove(roleType);
            errors.put(roleType, error);
            logger.error(error);
        }
    }

    private static void removeObserversWithoutAssociations(
            Map<String, PhysicalServerResourceAssignmentObserver> observers,
            Map<String, PhysicalServerRoleAssociationProvider> associations) {
        for (String roleType : new ArrayList<>(observers.keySet())) {
            if (!associations.containsKey(roleType)) {
                observers.remove(roleType);
                logger.error(String.format(
                        "RESOURCE_ASSIGNMENT_ASSOCIATION_PROVIDER_MISSING: roleType[%s]",
                        roleType));
            }
        }
    }

    private static void removeInvalidTopologyControllers(
            Map<String, PhysicalServerResourceAssignmentController> controllers,
            Map<String, String> errors) {
        int previousSize;
        do {
            previousSize = controllers.size();
            for (Map.Entry<String, PhysicalServerResourceAssignmentController> entry :
                    new ArrayList<>(controllers.entrySet())) {
                String topologyRoleType = entry.getValue().getTopologyRoleType();
                if (controllers.containsKey(topologyRoleType)) {
                    continue;
                }
                String error = String.format(
                        "RESOURCE_ASSIGNMENT_TOPOLOGY_ROLE_UNAVAILABLE: roleType[%s], topologyRoleType[%s]",
                        entry.getKey(), topologyRoleType);
                controllers.remove(entry.getKey());
                errors.put(entry.getKey(), error);
                logger.error(error);
            }
        } while (previousSize != controllers.size());
    }

    private static <T> Map<String, T> unique(
            String capability,
            List<T> extensions,
            Function<T, String> roleTypeReader) {
        Map<String, List<T>> candidates = candidates(
                extensions, roleTypeReader);
        Map<String, T> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<T>> entry : candidates.entrySet()) {
            if (entry.getValue().size() == 1) {
                result.put(entry.getKey(), entry.getValue().get(0));
            } else {
                logger.error(String.format(
                        "%s_AMBIGUOUS: %s extensions are registered for roleType[%s]",
                        capability, entry.getValue().size(), entry.getKey()));
            }
        }
        return result;
    }

    private static <T> Map<String, List<T>> candidates(
            List<T> extensions,
            Function<T, String> roleTypeReader) {
        Map<String, List<T>> result = new LinkedHashMap<>();
        if (extensions == null) {
            return result;
        }
        for (T extension : extensions) {
            String roleType;
            try {
                roleType = roleTypeReader.apply(extension);
            } catch (RuntimeException error) {
                logger.error(String.format(
                        "failed to read roleType from extension[%s]: %s",
                        extension.getClass().getName(), error.getMessage()));
                continue;
            }
            if (empty(roleType)
                    || !roleType.trim().matches("[A-Z][A-Z0-9_]{0,63}")) {
                logger.error(String.format(
                        "extension[%s] returned invalid roleType[%s]",
                        extension.getClass().getName(), roleType));
                continue;
            }
            List<T> roleExtensions = result.computeIfAbsent(
                    roleType.trim(), ignored -> new ArrayList<>());
            if (!roleExtensions.contains(extension)) {
                roleExtensions.add(extension);
            }
        }
        return result;
    }

    private static boolean empty(String value) {
        return value == null || value.trim().isEmpty();
    }

    public PhysicalServerRoleAssociationProvider associationProvider(
            String roleType) {
        return associations.get(roleType);
    }

    public PhysicalServerResourceAssignmentObserver observer(String roleType) {
        return observers.get(roleType);
    }

    public PhysicalServerResourceAssignmentController controller(String roleType) {
        return controllers.get(roleType);
    }

    public PhysicalServerResourceUsageObserver usageObserver(String roleType) {
        return usageObservers.get(roleType);
    }

    public String controllerError(String roleType) {
        return controllerErrors.get(roleType);
    }

    public List<PhysicalServerResourceAssignmentController> orderedControllers() {
        List<PhysicalServerResourceAssignmentController> result =
                new ArrayList<>(controllers.values());
        result.sort(Comparator.comparing(
                PhysicalServerResourceAssignmentController::getRoleType));
        return result;
    }

    public List<PhysicalServerResourceAssignmentObserver> orderedReadOnlyObservers() {
        List<PhysicalServerResourceAssignmentObserver> result = new ArrayList<>();
        for (Map.Entry<String, PhysicalServerResourceAssignmentObserver> entry :
                observers.entrySet()) {
            if (!controllers.containsKey(entry.getKey())) {
                result.add(entry.getValue());
            }
        }
        result.sort(Comparator.comparing(
                PhysicalServerResourceAssignmentObserver::getRoleType));
        return result;
    }

    public List<PhysicalServerResourceUsageObserver> orderedUsageObservers() {
        List<PhysicalServerResourceUsageObserver> result =
                new ArrayList<>(usageObservers.values());
        result.sort(Comparator.comparing(
                PhysicalServerResourceUsageObserver::getRoleType));
        return result;
    }

    public Collection<String> controllerErrors() {
        return controllerErrors.values();
    }

    private static class LoadResult<T> {
        private final Map<String, T> values;
        private final Map<String, String> errors;

        private LoadResult(
                Map<String, T> values,
                Map<String, String> errors) {
            this.values = values;
            this.errors = errors;
        }
    }
}
