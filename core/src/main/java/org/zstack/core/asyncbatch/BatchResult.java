package org.zstack.core.asyncbatch;

import org.zstack.header.errorcode.ErrorCode;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class BatchResult<I, R> {
    private final BatchTermination termination;
    private final List<ItemResult<I, R>> itemResults;
    private final String stopReason;

    BatchResult(BatchTermination termination, List<ItemResult<I, R>> itemResults, String stopReason) {
        this.termination = termination;
        this.itemResults = Collections.unmodifiableList(new ArrayList<>(itemResults));
        this.stopReason = stopReason;
    }

    public BatchTermination getTermination() {
        return termination;
    }

    public List<ItemResult<I, R>> getItemResults() {
        return itemResults;
    }

    public String getStopReason() {
        return stopReason;
    }

    public boolean hasFailure() {
        return failedCount() > 0;
    }

    /**
     * Returns whether this batch contains no items.
     *
     * <p>An empty batch is deliberately not classified as all succeeded, all failed, partially
     * succeeded, or no success.</p>
     */
    public boolean isEmpty() {
        return itemResults.isEmpty();
    }

    /**
     * Returns whether every item in this batch succeeded.
     */
    public boolean isAllSucceeded() {
        return !isEmpty() && successCount() == totalCount();
    }

    /**
     * Returns whether every item in this batch failed.
     */
    public boolean isAllFailed() {
        return !isEmpty() && failedCount() == totalCount();
    }

    /**
     * Returns whether this batch has at least one successful item and at least one non-successful
     * item.
     */
    public boolean isPartiallySucceeded() {
        return successCount() > 0 && successCount() < totalCount();
    }

    /**
     * Returns whether this batch has no successful item, but is not entirely failed. This covers
     * batches containing skipped or not-started items.
     */
    public boolean isNoSuccess() {
        return !isEmpty() && successCount() == 0 && failedCount() < totalCount();
    }

    public int totalCount() {
        return itemResults.size();
    }

    public long successCount() {
        return count(ItemStatus.SUCCEEDED);
    }

    public long failedCount() {
        return count(ItemStatus.FAILED);
    }

    public long skippedCount() {
        return count(ItemStatus.SKIPPED);
    }

    public long notStartedCount() {
        return count(ItemStatus.NOT_STARTED);
    }

    public List<ErrorCode> getErrors() {
        return itemResults.stream()
                .filter(it -> it.getStatus() == ItemStatus.FAILED)
                .map(ItemResult::getError)
                .collect(Collectors.toList());
    }

    private long count(ItemStatus status) {
        return itemResults.stream().filter(it -> it.getStatus() == status).count();
    }
}
