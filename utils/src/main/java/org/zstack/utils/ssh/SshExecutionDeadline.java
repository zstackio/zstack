package org.zstack.utils.ssh;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.SocketFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

/** A local wall-clock budget; remote timeout alone cannot bound a stalled SSH channel. */
class SshExecutionDeadline implements SocketFactory {
    private static final ScheduledThreadPoolExecutor WATCHDOG = new ScheduledThreadPoolExecutor(2, task -> {
        Thread thread = new Thread(task, "ssh-execution-deadline");
        thread.setDaemon(true);
        return thread;
    });
    static {
        WATCHDOG.setRemoveOnCancelPolicy(true);
    }

    private final long started = System.nanoTime();
    private final long timeoutMillis;
    private final int connectTimeoutMillis;
    // 0: running, 1: finished, 2: timed out. The watchdog never invokes a completion.
    private final AtomicInteger state = new AtomicInteger();
    private volatile Socket socket;
    private ScheduledFuture<?> watchdog;

    SshExecutionDeadline(long timeoutMillis, int connectTimeoutMillis) {
        this.timeoutMillis = timeoutMillis;
        this.connectTimeoutMillis = connectTimeoutMillis;
    }

    void watch() {
        watchdog = WATCHDOG.schedule(() -> {
            if (state.compareAndSet(0, 2)) {
                closeSocket();
            }
        }, timeoutMillis, TimeUnit.MILLISECONDS);
    }

    final class PhaseTimeout implements AutoCloseable {
        private final AtomicBoolean active = new AtomicBoolean(true);
        private final ScheduledFuture<?> future;

        private PhaseTimeout(long milliseconds) {
            future = WATCHDOG.schedule(() -> {
                if (active.compareAndSet(true, false) && state.compareAndSet(0, 2)) {
                    closeSocket();
                }
            }, milliseconds, TimeUnit.MILLISECONDS);
        }

        @Override
        public void close() {
            active.set(false);
            future.cancel(false);
        }
    }

    PhaseTimeout watchPhase(long milliseconds) throws SocketTimeoutException {
        return new PhaseTimeout(Math.min(milliseconds, remainingMillis()));
    }

    boolean finish() {
        if (TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started) >= timeoutMillis) {
            state.compareAndSet(0, 2);
        } else {
            state.compareAndSet(0, 1);
        }
        if (watchdog != null) {
            watchdog.cancel(false);
        }
        if (state.get() == 2) {
            closeSocket();
        }
        return state.get() == 1;
    }

    private void closeSocket() {
        Socket current = socket;
        if (current != null) {
            try {
                current.close();
            } catch (IOException ignored) {
                // The caller also closes the channel/session in its finally block.
            }
        }
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        Socket created = new Socket();
        socket = created;
        try {
            // Register before checking the deadline: expiry before/during connect
            // closes this socket, and a closed socket cannot be connected later.
            created.connect(new InetSocketAddress(host, port), connectionTimeout(connectTimeoutMillis));
            remainingMillis();
            return created;
        } catch (IOException | RuntimeException e) {
            created.close();
            throw e;
        }
    }

    @Override public InputStream getInputStream(Socket socket) throws IOException { return socket.getInputStream(); }
    @Override public OutputStream getOutputStream(Socket socket) throws IOException { return socket.getOutputStream(); }

    int remainingMillis() throws SocketTimeoutException {
        long remaining = timeoutMillis - TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
        if (remaining <= 0 || state.get() == 2) {
            throw new SocketTimeoutException(String.format("SSH execution timed out after %s ms", timeoutMillis));
        }
        return (int) Math.min(Integer.MAX_VALUE, remaining);
    }

    int connectionTimeout(int configuredMillis) throws SocketTimeoutException {
        return configuredMillis <= 0 ? remainingMillis() : Math.min(configuredMillis, remainingMillis());
    }

    void readOutput(Channel channel, InputStream stdout, InputStream stderr, SshResult result) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        try {
            while (true) {
                remainingMillis();
                // Drain both streams fairly. A blocking read of one can deadlock the other.
                boolean read = drainAvailable(stdout, out, buffer);
                read |= drainAvailable(stderr, err, buffer);
                if (channel.isClosed() && stdout.available() == 0 && stderr.available() == 0) {
                    remainingMillis();
                    return;
                }
                if (!read) {
                    try {
                        Thread.sleep(Math.min(10, remainingMillis()));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IOException("SSH execution interrupted", e);
                    }
                }
            }
        } finally {
            result.setStdout(out.toString("UTF-8"));
            result.setStderr(err.toString("UTF-8"));
        }
    }

    private boolean drainAvailable(InputStream input, ByteArrayOutputStream output, byte[] buffer) throws IOException {
        int available = input.available();
        if (available == 0) {
            return false;
        }
        int count = input.read(buffer, 0, Math.min(available, buffer.length));
        if (count <= 0) {
            return false;
        }
        output.write(buffer, 0, count);
        return true;
    }
}
