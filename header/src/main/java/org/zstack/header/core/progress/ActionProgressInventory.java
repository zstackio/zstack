package org.zstack.header.core.progress;

import org.zstack.utils.gson.JSONObjectUtil;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ActionProgressInventory {
    private String apiId;
    private String content;
    private Map<String, Object> opaque;
    private long createTime;
    private long lastOpTime;
    private long currentStep;
    private long totalStep;

    @SuppressWarnings("unchecked")
    public static ActionProgressInventory valueOf(ActionProgressVO vo) {
        ActionProgressInventory inv = new ActionProgressInventory();
        inv.setApiId(vo.getApiId());
        inv.setContent(vo.getContent());
        inv.setOpaque(JSONObjectUtil.rehashObject(vo.getOpaque(), Map.class));
        inv.setCreateTime(vo.getCreateTime());
        inv.setLastOpTime(vo.getLastOpTime());
        inv.setCurrentStep(vo.getCurrentStep());
        inv.setTotalStep(vo.getTotalStep());
        return inv;
    }

    @Deprecated
    public TaskProgressInventory toTaskProgress() {
        TaskProgressInventory inv = new TaskProgressInventory();
        inv.setTaskUuid(apiId);
        inv.setParentUuid(null);

        if (totalStep <= 1) {
            inv.setType("Task");
            inv.setContent(content);
            inv.setTaskName("task-" + apiId);
        } else {
            inv.setType("Progress");
            inv.setContent(Integer.toString((int) (100 * currentStep / (double) totalStep)));
            inv.setTaskName(content);
        }

        inv.setOpaque(new LinkedHashMap<>(opaque));
        inv.setTime(lastOpTime);
        inv.setSubTasks(Collections.emptyList());
        inv.setArguments(null);
        return inv;
    }

    public String getApiId() {
        return apiId;
    }

    public void setApiId(String apiId) {
        this.apiId = apiId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Map<String, Object> getOpaque() {
        return opaque;
    }

    public void setOpaque(Map<String, Object> opaque) {
        this.opaque = opaque;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public long getLastOpTime() {
        return lastOpTime;
    }

    public void setLastOpTime(long lastOpTime) {
        this.lastOpTime = lastOpTime;
    }

    public long getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(long currentStep) {
        this.currentStep = currentStep;
    }

    public long getTotalStep() {
        return totalStep;
    }

    public void setTotalStep(long totalStep) {
        this.totalStep = totalStep;
    }
}
