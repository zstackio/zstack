package org.zstack.header.longjob;

import org.zstack.header.core.progress.LongJobProgressDetail;
import org.zstack.header.core.progress.LongJobProgressDetailBuilder;
import org.zstack.header.core.progress.TaskProgressInventory;
import org.zstack.header.core.progress.TaskProgressVO;

import java.io.Serializable;

/**
 * Notification message for Long Job state change and progress update.
 * Reuses LongJobInventory and TaskProgressInventory for UI consistency.
 *
 * <p>Webhook/BFF contract: the payload should include a 0-100 progress number.
 * BFF uses {@link #getProgress()} (Integer) directly; full taskProgress is optional for future use.
 */
public class LongJobProgressNotificationMessage implements Serializable {
    public enum EventType {
        STATE_CHANGED,
        PROGRESS_UPDATED
    }

    private LongJobInventory longJob;
    /** Latest progress 0-100, derived from type=Progress latest record content. For BFF webhook. */
    private Integer progress;
    /** Full progress detail; optional, BFF current version only needs {@link #getProgress()}. */
    private TaskProgressInventory taskProgress;
    /** Standardized progress detail parsed from opaque; null when opaque is absent. */
    private LongJobProgressDetail progressDetail;
    private EventType eventType;
    private Long timestamp;

    public LongJobInventory getLongJob() {
        return longJob;
    }

    public void setLongJob(LongJobInventory longJob) {
        this.longJob = longJob;
    }

    /** Progress 0-100 for BFF webhook; from latest type=Progress content. */
    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    /** Full task progress inventory; optional for BFF. */
    public TaskProgressInventory getTaskProgress() {
        return taskProgress;
    }

    public void setTaskProgress(TaskProgressInventory taskProgress) {
        this.taskProgress = taskProgress;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public LongJobProgressDetail getProgressDetail() {
        return progressDetail;
    }

    public void setProgressDetail(LongJobProgressDetail progressDetail) {
        this.progressDetail = progressDetail;
    }

    public static LongJobProgressNotificationMessage stateChanged(LongJobVO vo) {
        LongJobProgressNotificationMessage msg = new LongJobProgressNotificationMessage();
        msg.longJob = LongJobInventory.valueOf(vo);
        msg.eventType = EventType.STATE_CHANGED;
        msg.timestamp = System.currentTimeMillis();
        return msg;
    }

    public static LongJobProgressNotificationMessage progressUpdated(LongJobVO vo, TaskProgressVO progressVO) {
        LongJobProgressNotificationMessage msg = new LongJobProgressNotificationMessage();
        msg.longJob = LongJobInventory.valueOf(vo);
        int percent = parseProgressContent(progressVO.getContent());
        msg.progress = percent;
        TaskProgressInventory inv = new TaskProgressInventory(progressVO);
        if (progressVO.getContent() != null) {
            inv.setContent(progressVO.getContent());
        }
        inv.setProgressDetail(LongJobProgressDetailBuilder.fromTaskProgressVO(progressVO));
        msg.taskProgress = inv;
        msg.progressDetail = inv.getProgressDetail();
        msg.eventType = EventType.PROGRESS_UPDATED;
        msg.timestamp = System.currentTimeMillis();
        return msg;
    }

    private static int parseProgressContent(String content) {
        if (content == null || content.isEmpty()) {
            return 0;
        }
        try {
            int v = Integer.parseInt(content.trim());
            return Math.min(100, Math.max(0, v));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
