package org.zstack.header.core.progress;

import org.zstack.utils.DebugUtils;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.max;
import static java.util.Collections.min;

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
        t.start = Integer.valueOf(stage.split("-")[0]);
        t.end = Integer.valueOf(stage.split("-")[1]);
        return t;
    }

    @Override
    public String toString() {
        return start + "-" + end;
    }
}
