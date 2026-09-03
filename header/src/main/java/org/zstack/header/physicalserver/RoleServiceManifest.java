package org.zstack.header.physicalserver;

import org.zstack.utils.YamlUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class RoleServiceManifest {
    private static final AtomicReference<Map<ManifestKey, RoleServiceManifest>>
            MANIFESTS = new AtomicReference<>(Collections.emptyMap());

    private String roleType;
    private PhysicalServerResourceIsolationMode isolationMode = PhysicalServerResourceIsolationMode.SHARED;
    private String sliceName;
    private Integer defaultCpuCount;
    private List<Service> services = new ArrayList<>();

    public static RoleServiceManifest load(String resourcePath, String expectedRoleType) {
        return load(new ManifestKey(resourcePath, expectedRoleType, ValidationMode.CONTROLLED));
    }

    public static RoleServiceManifest loadObservation(String resourcePath, String expectedRoleType) {
        return load(new ManifestKey(resourcePath, expectedRoleType, ValidationMode.OBSERVED));
    }

    public static void reloadAll() {
        while (true) {
            Map<ManifestKey, RoleServiceManifest> current = MANIFESTS.get();
            if (current.isEmpty()) {
                return;
            }
            Map<ManifestKey, RoleServiceManifest> reloaded = new LinkedHashMap<>();
            for (ManifestKey key : current.keySet()) {
                RoleServiceManifest manifest = key.read();
                RoleServiceManifest previous = current.get(key);
                if (!Objects.equals(previous.getSliceName(), manifest.getSliceName())) {
                    throw invalid(key.resourcePath, String.format(
                            "sliceName cannot be changed from[%s] to[%s]",
                            previous.getSliceName(), manifest.getSliceName()));
                }
                reloaded.put(key, manifest);
            }
            Map<ManifestKey, RoleServiceManifest> snapshot = Collections.unmodifiableMap(reloaded);
            if (MANIFESTS.compareAndSet(current, snapshot)) {
                return;
            }
        }
    }

    private static RoleServiceManifest load(ManifestKey key) {
        while (true) {
            Map<ManifestKey, RoleServiceManifest> current = MANIFESTS.get();
            RoleServiceManifest manifest = current.get(key);
            if (manifest != null) {
                return manifest;
            }
            RoleServiceManifest loaded = key.read();
            Map<ManifestKey, RoleServiceManifest> updated = new LinkedHashMap<>(current);
            updated.put(key, loaded);
            Map<ManifestKey, RoleServiceManifest> snapshot = Collections.unmodifiableMap(updated);
            if (MANIFESTS.compareAndSet(current, snapshot)) {
                return loaded;
            }
        }
    }

    private static RoleServiceManifest loadManifest(String resourcePath) {
        RoleServiceManifest manifest = YamlUtils.load(read(resourcePath), RoleServiceManifest.class);
        if (manifest == null) {
            throw new IllegalStateException(String.format("role service manifest[%s] is empty", resourcePath));
        }
        return manifest;
    }

    private RoleServiceManifest freeze() {
        services = Collections.unmodifiableList(new ArrayList<>(services));
        return this;
    }

    public List<ResourceConsumerHandle> handles() {
        List<ResourceConsumerHandle> result = new ArrayList<>();
        for (Service service : services) {
            ResourceConsumerHandle handle = new ResourceConsumerHandle();
            handle.setHandleType(service.getHandleType());
            handle.setValue(service.getValue());
            handle.setServiceName(service.getName());
            handle.setOptional(!service.required());
            handle.setRestartable(service.restartable());
            result.add(handle);
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

    private void validateControl(String resourcePath, String expectedRoleType) {
        validateRoleAndServices(resourcePath, expectedRoleType);
        if (empty(sliceName) || !sliceName.matches("[A-Za-z0-9][A-Za-z0-9_.@:-]{0,248}\\.slice")) {
            throw invalid(resourcePath, String.format("sliceName[%s] is invalid", sliceName));
        }
        if (defaultCpuCount != null && defaultCpuCount < 1) {
            throw invalid(resourcePath, "defaultCpuCount must be greater than zero");
        }
        for (Service service : services) {
            validateHandle(resourcePath, service);
        }
    }

    private void validateObservation(String resourcePath, String expectedRoleType) {
        validateRoleAndServices(resourcePath, expectedRoleType);
        if (!empty(sliceName) || defaultCpuCount != null) {
            throw invalid(resourcePath, "observation-only role cannot define allocation defaults");
        }
        for (Service service : services) {
            validateObservationService(resourcePath, service);
        }
    }

    private void validateRoleAndServices(String resourcePath, String expectedRoleType) {
        if (!expectedRoleType.equals(roleType)) {
            throw invalid(resourcePath, String.format(
                    "roleType[%s] does not match expected roleType[%s]", roleType, expectedRoleType));
        }
        if (isolationMode == null) {
            throw invalid(resourcePath, "isolationMode must not be empty");
        }
        if (services == null || services.isEmpty()) {
            throw invalid(resourcePath, "services must not be empty");
        }
        Set<String> names = new HashSet<>();
        for (Service service : services) {
            if (service == null || empty(service.getName())
                    || !service.getName().matches("[A-Za-z0-9][A-Za-z0-9_.-]{0,63}")) {
                throw invalid(resourcePath, "service name must not be empty");
            }
            if (!names.add(service.getName())) {
                throw invalid(resourcePath, String.format("service name[%s] is duplicated", service.getName()));
            }
        }
    }

    private void validateObservationService(String resourcePath, Service service) {
        if (!empty(service.getHandleType())
                || !empty(service.getValue()) || service.getRequired() != null || service.getRestartable() != null) {
            throw invalid(resourcePath, String.format(
                    "observation-only service[%s] cannot define a control handle", service.getName()));
        }
    }

    private void validateHandle(String resourcePath, Service service) {
        if (!ResourceConsumerHandle.SYSTEMD_UNIT.equals(service.getHandleType())) {
            throw invalid(resourcePath, String.format(
                    "service[%s] has unsupported handleType[%s]", service.getName(), service.getHandleType()));
        }
        if (empty(service.getValue())) {
            throw invalid(resourcePath, String.format("service[%s] value must not be empty", service.getName()));
        }
        if (service.getRequired() == null) {
            throw invalid(resourcePath, String.format("service[%s] required must be specified", service.getName()));
        }
        if (service.getRestartable() == null) {
            throw invalid(resourcePath, String.format("service[%s] restartable must be specified", service.getName()));
        }
    }

    private static IllegalStateException invalid(String resourcePath, String reason) {
        return new IllegalStateException(String.format("invalid role service manifest[%s]: %s", resourcePath, reason));
    }

    private static boolean empty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String read(String resourcePath) {
        ClassLoader context = Thread.currentThread().getContextClassLoader();
        InputStream stream = context == null ? null : context.getResourceAsStream(resourcePath);
        if (stream == null) {
            stream = RoleServiceManifest.class.getClassLoader().getResourceAsStream(resourcePath);
        }
        if (stream == null) {
            throw new IllegalStateException(String.format("role service manifest[%s] was not found", resourcePath));
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
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

    public PhysicalServerResourceIsolationMode getIsolationMode() {
        return isolationMode;
    }

    public void setIsolationMode(PhysicalServerResourceIsolationMode isolationMode) {
        this.isolationMode = isolationMode;
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
        private Boolean required;
        private Boolean restartable;

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

    }

    private enum ValidationMode {
        CONTROLLED, OBSERVED
    }

    private static class ManifestKey {
        private final String resourcePath;
        private final String expectedRoleType;
        private final ValidationMode validationMode;

        private ManifestKey(String resourcePath, String expectedRoleType, ValidationMode validationMode) {
            this.resourcePath = resourcePath;
            this.expectedRoleType = expectedRoleType;
            this.validationMode = validationMode;
        }

        private RoleServiceManifest read() {
            RoleServiceManifest manifest = loadManifest(resourcePath);
            if (validationMode == ValidationMode.CONTROLLED) {
                manifest.validateControl(resourcePath, expectedRoleType);
            } else {
                manifest.validateObservation(resourcePath, expectedRoleType);
            }
            return manifest.freeze();
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof ManifestKey)) {
                return false;
            }
            ManifestKey key = (ManifestKey) other;
            return Objects.equals(resourcePath, key.resourcePath)
                    && Objects.equals(expectedRoleType, key.expectedRoleType) && validationMode == key.validationMode;
        }

        @Override
        public int hashCode() {
            return Objects.hash(resourcePath, expectedRoleType, validationMode);
        }
    }
}
