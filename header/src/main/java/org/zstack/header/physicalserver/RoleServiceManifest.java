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
    private String sliceName;
    private Integer defaultCpuCount;
    private List<Service> services = new ArrayList<>();

    public static RoleServiceManifest load(
            String resourcePath,
            String expectedRoleType) {
        RoleServiceManifest manifest = loadManifest(resourcePath);
        manifest.validateControl(resourcePath, expectedRoleType);
        return manifest.freeze();
    }

    public static RoleServiceManifest loadObservation(
            String resourcePath,
            String expectedRoleType) {
        RoleServiceManifest manifest = loadManifest(resourcePath);
        manifest.validateObservation(resourcePath, expectedRoleType);
        return manifest.freeze();
    }

    private static RoleServiceManifest loadManifest(String resourcePath) {
        RoleServiceManifest manifest = YamlUtils.load(
                read(resourcePath), RoleServiceManifest.class);
        if (manifest == null) {
            throw new IllegalStateException(String.format(
                    "role service manifest[%s] is empty", resourcePath));
        }
        return manifest;
    }

    private RoleServiceManifest freeze() {
        services = Collections.unmodifiableList(new ArrayList<>(services));
        return this;
    }

    public List<ResourceConsumerHandle> handles(String consumerKey) {
        return handles(consumerKey, Collections.emptyMap());
    }

    public List<ResourceConsumerHandle> handles(
            String consumerKey,
            Map<String, String> values) {
        List<ResourceConsumerHandle> result = new ArrayList<>();
        for (Service service : services) {
            ResourceConsumerHandle handle = new ResourceConsumerHandle();
            handle.setHandleType(service.getHandleType());
            handle.setValue(service.resolveValue(values));
            handle.setServiceName(service.getName());
            handle.setConsumerKey(consumerKey);
            handle.setOptional(!service.required());
            handle.setRestartable(service.restartable());
            handle.setExpectedCommandToken(service.getExpectedCommandToken());
            result.add(handle);
        }
        return result;
    }

    public List<ResourceConsumerHandle> handlesByServiceNames(
            Collection<String> serviceNames,
            String consumerKey,
            Map<String, String> values) {
        if (serviceNames == null || serviceNames.isEmpty()) {
            throw new IllegalArgumentException("service names must not be empty");
        }
        Set<String> selected = new HashSet<>(serviceNames);
        if (selected.size() != serviceNames.size()) {
            throw new IllegalArgumentException("service names must not be duplicated");
        }
        List<ResourceConsumerHandle> result = handles(consumerKey, values).stream()
                .filter(handle -> selected.remove(handle.getServiceName()))
                .collect(Collectors.toList());
        if (!selected.isEmpty()) {
            throw new IllegalArgumentException(String.format(
                    "services%s are not defined by role[%s]", selected, roleType));
        }
        return result;
    }

    public List<ManagedServiceResourceUsage> managedServiceUsages(String state) {
        List<ManagedServiceResourceUsage> result = new ArrayList<>();
        for (Service service : services) {
            ManagedServiceResourceUsage usage = new ManagedServiceResourceUsage();
            usage.setRoleType(roleType);
            usage.setServiceName(service.getName());
            usage.setRestartable(service.restartable());
            usage.setState(state);
            result.add(usage);
        }
        return result;
    }

    private void validateControl(
            String resourcePath,
            String expectedRoleType) {
        validateRoleAndServices(resourcePath, expectedRoleType);
        if (empty(sliceName)
                || !sliceName.matches("[A-Za-z0-9][A-Za-z0-9_.@:-]{0,248}\\.slice")) {
            throw invalid(resourcePath, String.format(
                    "sliceName[%s] is invalid", sliceName));
        }
        if (defaultCpuCount != null && defaultCpuCount < 1) {
            throw invalid(resourcePath,
                    "defaultCpuCount must be greater than zero");
        }
        for (Service service : services) {
            validateHandle(resourcePath, service);
        }
    }

    private void validateObservation(
            String resourcePath,
            String expectedRoleType) {
        validateRoleAndServices(resourcePath, expectedRoleType);
        if (!empty(sliceName) || defaultCpuCount != null) {
            throw invalid(resourcePath,
                    "observation-only role cannot define allocation defaults");
        }
        for (Service service : services) {
            validateObservationService(resourcePath, service);
        }
    }

    private void validateRoleAndServices(
            String resourcePath,
            String expectedRoleType) {
        if (!expectedRoleType.equals(roleType)) {
            throw invalid(resourcePath, String.format(
                    "roleType[%s] does not match expected roleType[%s]",
                    roleType, expectedRoleType));
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

        public String getExpectedCommandToken() {
            return expectedCommandToken;
        }

        public void setExpectedCommandToken(String expectedCommandToken) {
            this.expectedCommandToken = expectedCommandToken;
        }
    }
}
