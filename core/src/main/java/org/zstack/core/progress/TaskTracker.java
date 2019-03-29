package org.zstack.core.progress;

import org.zstack.header.errorcode.ErrorCode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public abstract class TaskTracker {
    public static final String PARAM_ERROR = "error";

    private String resourceUuid;
    private Map<String, Object> params = new HashMap<>();

    protected TaskTracker() {
    }

    public static class Task {
        public String taskName;
        public String resourceType;
        public String resourceUuid;
        public String info;
        public Map<String, Object> parameters;
    }

    public TaskTracker(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    protected String getResourceUuid() {
        return resourceUuid;
    }

    private static Map<String, List<Consumer>> taskConsumers = new HashMap<>();

    public static void registerConsumer(String taskName, Consumer<Task> consumer) {
        List<Consumer> lst = taskConsumers.computeIfAbsent(taskName, k->new ArrayList<>());
        lst.add(consumer);
    }

    public TaskTracker track(String info) {
        return track(info, null);
    }

    public TaskTracker param(String name, Object value) {
        params.put(name, value);
        return this;
    }

    public TaskTracker error(ErrorCode err) {
        if (err != null) {
            params.put(PARAM_ERROR, err.getReadableDetails());
        }

        return this;
    }

    public TaskTracker error(List<ErrorCode> errs) {
        if (errs != null) {
            List<String> errStrings = errs.stream().map(ErrorCode::getReadableDetails).collect(Collectors.toList());
            params.put(PARAM_ERROR, String.format("%s", errStrings));
        }

        return this;
    }

    protected abstract String getResourceType();

    protected abstract String getTaskName();

    public TaskTracker track(String info, Map<String, Object> params) {
        List<Consumer> consumers = taskConsumers.get(getTaskName());
        if (consumers == null || consumers.isEmpty()) {
            return this;
        }

        Task task = new Task();
        task.resourceType = getResourceType();
        task.taskName = getTaskName();
        task.resourceUuid = resourceUuid;
        task.info = info;
        if (params != null) {
            this.params.putAll(params);
        }
        task.parameters = this.params;

        consumers.forEach(c -> c.accept(task));
        return this;
    }
}
