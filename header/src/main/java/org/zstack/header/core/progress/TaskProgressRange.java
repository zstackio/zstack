package org.zstack.header.core.progress;

public class TaskProgressRange {
    private int start, end;

    public Integer getEnd() {
        return end;
    }

    public Integer getStart() {
        return start;
    }

    public TaskProgressRange() {
    }

    public TaskProgressRange(int start, int end){
        this.start = start;
        this.end = end;
    }

    public static TaskProgressRange valueOf(String stage) {
        TaskProgressRange t = new TaskProgressRange();
        t.start = Integer.parseInt(stage.split("-")[0]);
        t.end = Integer.parseInt(stage.split("-")[1]);
        return t;
    }

    @Override
    public String toString() {
        return start + "-" + end;
    }
}
