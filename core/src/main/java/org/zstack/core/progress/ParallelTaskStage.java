package org.zstack.core.progress;

import org.zstack.header.core.progress.TaskProgressRange;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by MaJin on 2019/10/15.
 */
public class ParallelTaskStage {
    private final Map<Integer, Integer> subTaskPercent = new ConcurrentHashMap<>();
    private final List<TaskProgressRange> subTaskStages;
    private int start;
    private int end;
    private int taskStartPercentSum;

    ParallelTaskStage(TaskProgressRange exactStage, List<? extends Number> weight) {
        this.subTaskStages = Collections.synchronizedList(ProgressReportService.splitTaskStage(exactStage, weight));
        this.subTaskStages.forEach(it -> this.subTaskPercent.put(it.getStart(), it.getStart()));
        this.taskStartPercentSum = subTaskPercent.values().stream().mapToInt(it -> it).sum();
        this.start = exactStage.getStart();
        this.end = exactStage.getEnd();
    }

    public synchronized void markSubStage() {
        ProgressReportService.markTaskStage(subTaskStages.remove(0));
    }

    public synchronized void skipSubStage() {
        taskStartPercentSum -= subTaskStages.remove(0).getStart();
    }

    synchronized int calculatePercent(int percent) {
        if (percent < start || percent > end || percent >= 100) {
            return percent;
        }

        int key = subTaskPercent.keySet().stream().filter(it -> it <= percent).max(Comparator.comparingInt(it -> it)).orElse(0);
        if (subTaskPercent.get(key) < percent) {
            subTaskPercent.put(key, percent);
        }
        return subTaskPercent.values().stream().mapToInt(it -> it).sum() - taskStartPercentSum;
    }

    boolean isOver(int percent) {
        return percent > end || percent >= 100;
    }
}
