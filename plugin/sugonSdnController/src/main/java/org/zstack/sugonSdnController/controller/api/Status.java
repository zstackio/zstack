package org.zstack.sugonSdnController.controller.api;

public abstract class Status {
    private Status() {
    }

    public abstract boolean isSuccess();
    public abstract void ifFailure(ErrorHandler handler);

    private final static Status success = new Success();

    public static Status success() {
        return success;
    }

    public static Status failure(String message) {
        return new Failure(message);
    }

    private static class Success extends Status {
        @Override
        public boolean isSuccess() {
            return true;
        }

        @Override
        public void ifFailure(ErrorHandler handler) {
            // do nothing
        }
    }

    private static class Failure extends Status {
        private final String message;

        private Failure(String message) {
            this.message = message;
        }

        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public void ifFailure(ErrorHandler handler) {
            handler.handle(message);
        }
    }

    public interface ErrorHandler {
        void handle(String errorMessage);
    }
}
