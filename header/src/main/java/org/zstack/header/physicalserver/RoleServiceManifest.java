package org.zstack.header.physicalserver;

import org.zstack.utils.YamlUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class RoleServiceManifest {
    private String roleType;
    private String applicationMode;
    private String sliceName;
    private Integer defaultCpuCount;
    private List<Service> services = new ArrayList<>();

    public static RoleServiceManifest load(
            String resourcePath,
            String expectedRoleType,
            PhysicalServerResourceApplicationMode expectedApplicationMode) {
        RoleServiceManifest manifest = YamlUtils.load(
                read(resourcePath), RoleServiceManifest.class);
        if (manifest == null) {
            throw new IllegalStateException(String.format(
                    "role service manifest[%s] is empty", resourcePath));
        }
        manifest.validate(
                resourcePath, expectedRoleType, expectedApplicationMode);
        manifest.services = Collections.unmodifiableList(
                new ArrayList<>(manifest.services));
        return manifest;
    }

    public static RoleServiceManifest loadObservation(
            String resourcePath,
            String expectedRoleType) {
        RoleServiceManifest manifest = YamlUtils.load(
                read(resourcePath), RoleServiceManifest.class);
        if (manifest == null) {
            throw new IllegalStateException(String.format(
                    "role service manifest[%s] is empty", resourcePath));
        }
        manifest.validate(resourcePath, expectedRoleType, null);
        manifest.services = Collections.unmodifiableList(
                new ArrayList<>(manifest.services));
        return manifest;
    }

    public List<ResourceConsumerHandle> handles(
            String primaryConsumerKey,
            String auxiliaryConsumerKey,
            boolean includeAuxiliaryServices) {
        return handles(
                primaryConsumerKey,
                auxiliaryConsumerKey,
                includeAuxiliaryServices,
                Collections.emptyMap());
    }

    public List<ResourceConsumerHandle> handles(
            String primaryConsumerKey,
            String auxiliaryConsumerKey,
            boolean includeAuxiliaryServices,
            Map<String, String> values) {
        if (!PhysicalServerResourceApplicationMode.RESOURCE_HANDLES.name()
                .equals(applicationMode)) {
            throw new IllegalStateException(String.format(
                    "role[%s] does not use resource handles", roleType));
        }
        List<ResourceConsumerHandle> result = new ArrayList<>();
        for (Service service : services) {
            if (service.isAuxiliary() && !includeAuxiliaryServices) {
                continue;
            }
            result.add(new ResourceConsumerHandle(
                    service.getHandleType(),
                    service.resolveValue(values),
                    service.getName(),
                    service.isAuxiliary()
                            ? auxiliaryConsumerKey : primaryConsumerKey,
                    !service.required(),
                    service.restartable(),
                    service.getExpectedCommandToken()));
        }
        return result;
    }

    public List<ResourceConsumerHandle> handlesByServiceNames(
            Collection<String> serviceNames,
            String primaryConsumerKey,
            String auxiliaryConsumerKey,
            boolean includeAuxiliaryServices,
            Map<String, String> values) {
        if (serviceNames == null || serviceNames.isEmpty()) {
            throw new IllegalArgumentException("service names must not be empty");
        }
        Set<String> selected = new HashSet<>(serviceNames);
        if (selected.size() != serviceNames.size()) {
            throw new IllegalArgumentException("service names must not be duplicated");
        }
        List<ResourceConsumerHandle> available = handles(
                primaryConsumerKey, auxiliaryConsumerKey,
                includeAuxiliaryServices, values);
        List<ResourceConsumerHandle> result = available.stream()
                .filter(handle -> selected.remove(handle.getServiceName()))
                .collect(Collectors.toList());
        if (!selected.isEmpty()) {
            throw new IllegalArgumentException(String.format(
                    "services%s are not defined by role[%s]", selected, roleType));
        }
        return result;
    }

    public List<ManagedServiceResourceUsage> managedServiceUsages(
            boolean includeAuxiliaryServices, String state) {
        List<ManagedServiceResourceUsage> result = new ArrayList<>();
        for (Service service : services) {
            if (service.isAuxiliary() && !includeAuxiliaryServices) {
                continue;
            }
            ManagedServiceResourceUsage usage =
                    new ManagedServiceResourceUsage();
            usage.setRoleType(roleType);
            usage.setServiceName(service.getName());
            usage.setRestartable(service.restartable());
            usage.setState(state);
            result.add(usage);
        }
        return result;
    }

    private void validate(
            String resourcePath,
            String expectedRoleType,
            PhysicalServerResourceApplicationMode expectedApplicationMode) {
        if (!expectedRoleType.equals(roleType)) {
            throw invalid(resourcePath, String.format(
                    "roleType[%s] does not match expected roleType[%s]",
                    roleType, expectedRoleType));
        }
        if (expectedApplicationMode == null && !empty(applicationMode)) {
            throw invalid(resourcePath, String.format(
                    "observation-only role cannot define applicationMode[%s]",
                    applicationMode));
        }
        if (expectedApplicationMode != null
                && !expectedApplicationMode.name().equals(applicationMode)) {
            throw invalid(resourcePath, String.format(
                    "applicationMode[%s] does not match expected mode[%s]",
                    applicationMode, expectedApplicationMode));
        }
        if (expectedApplicationMode == null
                && (!empty(sliceName) || defaultCpuCount != null)) {
            throw invalid(resourcePath,
                    "observation-only role cannot define allocation defaults");
        }
        if (expectedApplicationMode
                == PhysicalServerResourceApplicationMode.RESOURCE_HANDLES
                && (empty(sliceName)
                || !sliceName.matches("[A-Za-z0-9][A-Za-z0-9_.@:-]{0,248}\\.slice"))) {
            throw invalid(resourcePath, String.format(
                    "sliceName[%s] is invalid", sliceName));
        }
        if (expectedApplicationMode
                == PhysicalServerResourceApplicationMode.PROVIDER_MANAGED
                && !empty(sliceName)) {
            throw invalid(resourcePath,
                    "provider-managed role cannot define sliceName");
        }
        if (defaultCpuCount != null && defaultCpuCount < 1) {
            throw invalid(resourcePath,
                    "defaultCpuCount must be greater than zero");
        }
        if (expectedApplicationMode
                == PhysicalServerResourceApplicationMode.PROVIDER_MANAGED
                && defaultCpuCount != null) {
            throw invalid(resourcePath,
                    "provider-managed role cannot define defaultCpuCount");
        }
        if (services == null || services.isEmpty()) {
            throw invalid(resourcePath, "services must not be empty");
        }
        Set<String> names = new HashSet<>();
        for (Service service : services) {
            if (service == null || empty(service.getName())
                    || !service.getName().matches(
                    "[A-Za-z0-9][A-Za-z0-9_.-]{0,63}")) {
                throw invalid(resourcePath, "service name must not be empty");
            }
            if (!names.add(service.getName())) {
                throw invalid(resourcePath, String.format(
                        "service name[%s] is duplicated", service.getName()));
            }
            if (expectedApplicationMode
                    == PhysicalServerResourceApplicationMode.RESOURCE_HANDLES) {
                validateHandle(resourcePath, service);
            } else if (expectedApplicationMode == null) {
                validateObservationService(resourcePath, service);
            }
        }
    }

    private void validateObservationService(
            String resourcePath, Service service) {
        if (!empty(service.getHandleType())
                || !empty(service.getValue())
                || !empty(service.getValueFrom())
                || service.getRequired() != null
                || service.getRestartable() != null
                || !empty(service.getExpectedCommandToken())) {
            throw invalid(resourcePath, String.format(
                    "observation-only service[%s] cannot define a control handle",
                    service.getName()));
        }
    }

    private void validateHandle(String resourcePath, Service service) {
        if (!ResourceConsumerHandle.SYSTEMD_UNIT.equals(service.getHandleType())
                && !ResourceConsumerHandle.OWNER_PID_FILE.equals(
                service.getHandleType())) {
            throw invalid(resourcePath, String.format(
                    "service[%s] has unsupported handleType[%s]",
                    service.getName(), service.getHandleType()));
        }
        if (empty(service.getValue()) == empty(service.getValueFrom())) {
            throw invalid(resourcePath, String.format(
                    "service[%s] must specify exactly one of value and valueFrom",
                    service.getName()));
        }
        if (service.getRequired() == null) {
            throw invalid(resourcePath, String.format(
                    "service[%s] required must be specified", service.getName()));
        }
        if (service.getRestartable() == null) {
            throw invalid(resourcePath, String.format(
                    "service[%s] restartable must be specified",
                    service.getName()));
        }
        if (service.restartable()
                && !ResourceConsumerHandle.SYSTEMD_UNIT.equals(
                service.getHandleType())) {
            throw invalid(resourcePath, String.format(
                    "service[%s] restartable requires a systemd handle",
                    service.getName()));
        }
        if (ResourceConsumerHandle.OWNER_PID_FILE.equals(
                service.getHandleType())
                && empty(service.getExpectedCommandToken())) {
            throw invalid(resourcePath, String.format(
                    "service[%s] command token must not be empty",
                    service.getName()));
        }
        if (ResourceConsumerHandle.SYSTEMD_UNIT.equals(service.getHandleType())
                && !empty(service.getExpectedCommandToken())) {
            throw invalid(resourcePath, String.format(
                    "service[%s] systemd handle cannot have a command token",
                    service.getName()));
        }
    }

    private static IllegalStateException invalid(
            String resourcePath, String reason) {
        return new IllegalStateException(String.format(
                "invalid role service manifest[%s]: %s", resourcePath, reason));
    }

    private static boolean empty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String read(String resourcePath) {
        ClassLoader context = Thread.currentThread().getContextClassLoader();
        InputStream stream = context == null
                ? null : context.getResourceAsStream(resourcePath);
        if (stream == null) {
            stream = RoleServiceManifest.class.getClassLoader()
                    .getResourceAsStream(resourcePath);
        }
        if (stream == null) {
            throw new IllegalStateException(String.format(
                    "role service manifest[%s] was not found", resourcePath));
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                stream, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (IOException error) {
            throw new IllegalStateException(String.format(
                    "cannot read role service manifest[%s]", resourcePath), error);
        }
    }

    public String getRoleType() {
        return roleType;
    }

    public void setRoleType(String roleType) {
        this.roleType = roleType;
    }

    public String getApplicationMode() {
        return applicationMode;
    }

    public void setApplicationMode(String applicationMode) {
        this.applicationMode = applicationMode;
    }

    public String getSliceName() {
        return sliceName;
    }

    public void setSliceName(String sliceName) {
        this.sliceName = sliceName;
    }

    public Integer getDefaultCpuCount() {
        return defaultCpuCount;
    }

    public void setDefaultCpuCount(Integer defaultCpuCount) {
        this.defaultCpuCount = defaultCpuCount;
    }

    public List<Service> getServices() {
        return services;
    }

    public void setServices(List<Service> services) {
        this.services = services;
    }

    public static class Service {
        private String name;
        private String handleType;
        private String value;
        private String valueFrom;
        private Boolean required;
        private Boolean restartable;
        private boolean auxiliary;
        private String expectedCommandToken;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getHandleType() {
            return handleType;
        }

        public void setHandleType(String handleType) {
            this.handleType = handleType;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getValueFrom() {
            return valueFrom;
        }

        public void setValueFrom(String valueFrom) {
            this.valueFrom = valueFrom;
        }

        private String resolveValue(Map<String, String> values) {
            if (!empty(value)) {
                return value;
            }
            String resolved = values.get(valueFrom);
            if (empty(resolved)) {
                throw new IllegalStateException(String.format(
                        "role service[%s] valueFrom[%s] cannot be resolved",
                        name, valueFrom));
            }
            return resolved;
        }

        public Boolean getRequired() {
            return required;
        }

        public void setRequired(Boolean required) {
            this.required = required;
        }

        private boolean required() {
            return Boolean.TRUE.equals(required);
        }

        public Boolean getRestartable() {
            return restartable;
        }

        public void setRestartable(Boolean restartable) {
            this.restartable = restartable;
        }

        private boolean restartable() {
            return Boolean.TRUE.equals(restartable);
        }

        public boolean isAuxiliary() {
            return auxiliary;
        }

        public void setAuxiliary(boolean auxiliary) {
            this.auxiliary = auxiliary;
        }

        public String getExpectedCommandToken() {
            return expectedCommandToken;
        }

        public void setExpectedCommandToken(String expectedCommandToken) {
            this.expectedCommandToken = expectedCommandToken;
        }
    }
}
