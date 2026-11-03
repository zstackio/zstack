package org.zstack.core.search;

import org.hibernate.search.exception.impl.LogErrorHandler;

import java.util.concurrent.atomic.AtomicLong;

public class SearchErrorHandler extends LogErrorHandler {
    private static final AtomicLong errorCount = new AtomicLong();

    public static long getErrorCount() {
        return errorCount.get();
    }

    @Override
    public void handleException(String message, Throwable exception) {
        errorCount.incrementAndGet();
        super.handleException(message, exception);
    }
}
