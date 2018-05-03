package org.zstack.zql1.ast

class ZQLError extends Exception {
    ZQLError() {
    }

    ZQLError(String message) {
        super(message)
    }

    ZQLError(String message, Throwable cause) {
        super(message, cause)
    }

    ZQLError(Throwable cause) {
        super(cause)
    }

    ZQLError(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace)
    }
}
