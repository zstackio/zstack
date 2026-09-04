package org.zstack.physicalserver;

import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.physicalserver.PhysicalServerResourceAssignmentController;
import org.zstack.header.physicalserver.PhysicalServerResourceAssignmentObserver;
import org.zstack.header.physicalserver.PhysicalServerResourceUsageObserver;
import org.zstack.header.physicalserver.PhysicalServerRoleType;
import org.zstack.header.physicalserver.PhysicalServerRoleAssociationProvider;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PhysicalServerResourceExtensionRegistry {
    private final Map<String, PhysicalServerRoleAssociationProvider> associations;
    private final Map<String, PhysicalServerResourceAssignmentObserver> observers;
    private final Map<String, PhysicalServerResourceAssignmentController> controllers;
    private final Map<String, PhysicalServerResourceUsageObserver> usageObservers;

    private PhysicalServerResourceExtensionRegistry(
            Map<String, PhysicalServerRoleAssociationProvider> associations,
            Map<String, PhysicalServerResourceAssignmentObserver> observers,
            Map<String, PhysicalServerResourceAssignmentController> controllers,
            Map<String, PhysicalServerResourceUsageObserver> usageObservers) {
        this.associations = associations;
        this.observers = observers;
        this.controllers = controllers;
        this.usageObservers = usageObservers;
    }

    public static PhysicalServerResourceExtensionRegistry load(PluginRegistry pluginRegistry) {
        Map<String, PhysicalServerRoleAssociationProvider> associations = new LinkedHashMap<>();
        for (PhysicalServerRoleAssociationProvider extension :
                pluginRegistry.getExtensionList(PhysicalServerRoleAssociationProvider.class)) {
            register(associations, extension.getRoleType(), extension);
        }

        List<PhysicalServerResourceAssignmentController> controllerExtensions =
                pluginRegistry.getExtensionList(PhysicalServerResourceAssignmentController.class);
        Map<String, PhysicalServerResourceAssignmentController> controllers = new LinkedHashMap<>();
        for (PhysicalServerResourceAssignmentController extension : controllerExtensions) {
            if (extension.getIsolationMode() == null) {
                throw new CloudRuntimeException(String.format(
                        "PhysicalServer Role extension[%s] did not declare " +
                                "an isolation mode for roleType[%s]",
                        extension.getClass().getName(), extension.getRoleType()));
            }
            register(controllers, extension.getRoleType(), extension);
        }

        List<PhysicalServerResourceAssignmentObserver> observerExtensions =
                new ArrayList<>(pluginRegistry.getExtensionList(PhysicalServerResourceAssignmentObserver.class));
        for (PhysicalServerResourceAssignmentController controller : controllerExtensions) {
            if (!observerExtensions.contains(controller)) {
                observerExtensions.add(controller);
            }
        }
        Map<String, PhysicalServerResourceAssignmentObserver> observers = new LinkedHashMap<>();
        for (PhysicalServerResourceAssignmentObserver extension : observerExtensions) {
            register(observers, extension.getRoleType(), extension);
        }
        Map<String, PhysicalServerResourceUsageObserver> usageObservers = new LinkedHashMap<>();
        for (PhysicalServerResourceUsageObserver extension :
                pluginRegistry.getExtensionList(PhysicalServerResourceUsageObserver.class)) {
            register(usageObservers, extension.getRoleType(), extension);
        }

        for (String roleType : observers.keySet()) {
            if (!associations.containsKey(roleType)) {
                throw new CloudRuntimeException(String.format(
                        "no PhysicalServer Role association provider is " + "registered for roleType[%s]", roleType));
            }
        }

        return new PhysicalServerResourceExtensionRegistry(associations, observers, controllers, usageObservers);
    }

    private static <T> void register(Map<String, T> extensions, PhysicalServerRoleType roleType, T extension) {
        if (roleType == null) {
            throw new CloudRuntimeException(String.format(
                    "PhysicalServer Role extension[%s] did not declare a role type", extension.getClass().getName()));
        }
        String typeName = roleType.toString();
        T old = extensions.get(typeName);
        if (old != null && old != extension) {
            throw new CloudRuntimeException(String.format(
                    "duplicate PhysicalServer Role extensions[%s, %s] " +
                            "for roleType[%s]", old.getClass().getName(), extension.getClass().getName(), typeName));
        }
        extensions.put(typeName, extension);
    }

    public PhysicalServerRoleAssociationProvider associationProvider(String roleType) {
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

    public List<PhysicalServerResourceAssignmentController> orderedControllers() {
        List<PhysicalServerResourceAssignmentController> result = new ArrayList<>(controllers.values());
        result.sort(Comparator.comparing(extension -> extension.getRoleType().toString()));
        return result;
    }

    public List<PhysicalServerResourceAssignmentObserver> orderedReadOnlyObservers() {
        List<PhysicalServerResourceAssignmentObserver> result = new ArrayList<>();
        for (Map.Entry<String, PhysicalServerResourceAssignmentObserver> entry : observers.entrySet()) {
            if (!controllers.containsKey(entry.getKey())) {
                result.add(entry.getValue());
            }
        }
        result.sort(Comparator.comparing(extension -> extension.getRoleType().toString()));
        return result;
    }
}
