package org.zstack.core.asyncbatch;

import org.zstack.header.errorcode.ErrorCode;

public final class ItemResult<I, R> {
    private final int index;
    private final I item;
    private final ItemStatus status;
    private final R result;
    private final ErrorCode error;
    private final String reason;

    private ItemResult(int index, I item, ItemStatus status, R result, ErrorCode error, String reason) {
        this.index = index;
        this.item = item;
        this.status = status;
        this.result = result;
        this.error = error;
        this.reason = reason;
    }

    static <I, R> ItemResult<I, R> succeeded(int index, I item, R result) {
        return new ItemResult<>(index, item, ItemStatus.SUCCEEDED, result, null, null);
    }

    static <I, R> ItemResult<I, R> failed(int index, I item, ErrorCode error) {
        return new ItemResult<>(index, item, ItemStatus.FAILED, null, error, null);
    }

    static <I, R> ItemResult<I, R> skipped(int index, I item, String reason) {
        return new ItemResult<>(index, item, ItemStatus.SKIPPED, null, null, reason);
    }

    static <I, R> ItemResult<I, R> notStarted(int index, I item) {
        return new ItemResult<>(index, item, ItemStatus.NOT_STARTED, null, null, null);
    }

    public int getIndex() {
        return index;
    }

    public I getItem() {
        return item;
    }

    public ItemStatus getStatus() {
        return status;
    }

    public R getResult() {
        return result;
    }

    public ErrorCode getError() {
        return error;
    }

    public String getReason() {
        return reason;
    }
}
