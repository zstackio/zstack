package org.zstack.core.progress;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public abstract class TaskTracker {
    private String resourceUuid;
    private Map<String, Object> params = new HashMap<>();

    public static class Task {
        public String resourceType;
        public String resourceUuid;
        public String info;
        public Map<String, Object> parameters;
    }

    public TaskTracker(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    private static Map<String, List<Consumer>> taskConsumers = new HashMap<>();

    public static void registerConsumer(String resourceType, Consumer consumer) {
        List<Consumer> lst = taskConsumers.computeIfAbsent(resourceType, k->new ArrayList<>());
        lst.add(consumer);
    }

    public void track(String info) {
        track(info, null);
    }

    public TaskTracker param(String name, Object value) {
        params.put(name, value);
        return this;
    }

    protected abstract String getResourceType();

    public void track(String info, Map<String, Object> params) {
        List<Consumer> consumers = taskConsumers.get(getResourceType());
        if (consumers == null || consumers.isEmpty()) {
            return;
        }

        Task task = new Task();
        task.resourceType = getResourceType();
        task.resourceUuid = resourceUuid;
        task.info = info;
        if (params != null) {
            this.params.putAll(params);
        }
        task.parameters = this.params;

        consumers.forEach(c -> c.accept(task));
    }
}
