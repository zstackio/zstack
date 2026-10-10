package org.zstack.core.config;

import java.util.Objects;

/** One immutable standard configuration row mutation, resolved by the server. */
public final class ConfigMutation {
    private final String category, name, resourceUuid, resourceType, newValue;
    private final boolean delete;

    public ConfigMutation(String category, String name, String resourceUuid, String resourceType,
            String newValue, boolean delete) {
        this.category = Objects.requireNonNull(category, "category");
        this.name = Objects.requireNonNull(name, "name");
        if ((resourceUuid == null) != (resourceType == null)) {
            throw new IllegalArgumentException("resource UUID and type must both be present or absent");
        }
        if (delete && (resourceUuid == null || newValue != null)) {
            throw new IllegalArgumentException("only a resource override without a new value can be deleted");
        }
        this.resourceUuid = resourceUuid; this.resourceType = resourceType;
        this.newValue = newValue; this.delete = delete;
    }

    public String getCategory() { return category; }
    public String getName() { return name; }
    public String getResourceUuid() { return resourceUuid; }
    public String getResourceType() { return resourceType; }
    public String getNewValue() { return newValue; }
    public boolean isDelete() { return delete; }
}
