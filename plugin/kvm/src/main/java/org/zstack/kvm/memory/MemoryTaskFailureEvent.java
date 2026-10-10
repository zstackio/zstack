package org.zstack.kvm.memory;

/** Canonical event emitted only after a persisted Host task enters Failed. */
public class MemoryTaskFailureEvent {
    public static final String PATH = "/zstack/kvm/memory-optimization/task-failed";

    private String hostUuid;
    private String taskUuid;
    private String action;
    private String reason;
    private String status;
    private long createTimeMillis;

    public static MemoryTaskFailureEvent fromTask(MemoryTaskVO task) {
        return fromTask(task, null);
    }

    public static MemoryTaskFailureEvent fromTask(MemoryTaskVO task, String reasonOverride) {
        if (task == null || !isHostExecutionScope(task.getScope()) || !"Failed".equals(task.getStatus())
                || task.getHostUuid() == null || task.getHostUuid().trim().isEmpty()
                || task.getUuid() == null || task.getUuid().trim().isEmpty()
                || task.getAction() == null || task.getAction().trim().isEmpty()) {
            return null;
        }
        MemoryTaskFailureEvent event = new MemoryTaskFailureEvent();
        event.hostUuid = task.getHostUuid();
        event.taskUuid = task.getUuid();
        event.action = task.getAction();
        event.reason = reasonOverride == null ? task.getReason() : reasonOverride;
        event.status = task.getStatus();
        event.createTimeMillis = System.currentTimeMillis();
        return event;
    }

    private static boolean isHostExecutionScope(String scope) {
        return "Host".equals(scope) || "Cluster".equals(scope) || "Global".equals(scope);
    }

    public String getHostUuid() { return hostUuid; }
    public void setHostUuid(String value) { hostUuid = value; }
    public String getTaskUuid() { return taskUuid; }
    public void setTaskUuid(String value) { taskUuid = value; }
    public String getAction() { return action; }
    public void setAction(String value) { action = value; }
    public String getReason() { return reason; }
    public void setReason(String value) { reason = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public long getCreateTimeMillis() { return createTimeMillis; }
    public void setCreateTimeMillis(long value) { createTimeMillis = value; }
}
