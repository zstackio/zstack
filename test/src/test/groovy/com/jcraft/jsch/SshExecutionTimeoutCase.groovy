package com.jcraft.jsch

import org.junit.Test
import org.zstack.utils.ssh.Ssh
import org.zstack.utils.ssh.SshException
import org.zstack.utils.ssh.SshResult

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.lang.reflect.Field
import java.net.ServerSocket
import java.net.Socket
import java.io.IOException
import java.util.concurrent.TimeUnit

import static org.junit.Assert.*

// Same package as JSch to construct a Session without opening a real SSH connection.
class SshExecutionTimeoutCase {
    private static class FakeChannel extends ChannelExec {
        InputStream stdout = new ByteArrayInputStream(new byte[0])
        InputStream stderr = new ByteArrayInputStream(new byte[0])
        boolean finished
        boolean disconnected
        int status
        int connectTimeout
        int connections
        String submittedCommand

        @Override void connect(int timeout) { connectTimeout = timeout; connections++; }
        @Override void setCommand(String command) { submittedCommand = command }
        @Override public InputStream getInputStream() { return stdout; }
        @Override public InputStream getErrStream() { return stderr; }
        @Override public boolean isClosed() { return finished; }
        @Override public int getExitStatus() { return status; }
        @Override void disconnect() { disconnected = true; }
    }

    private static class FakeSession extends Session {
        final FakeChannel channel
        boolean disconnected
        FakeSession(FakeChannel channel) throws JSchException {
            super(new JSch(), "test", "localhost", 22)
            this.channel = channel
        }
        @Override public Channel openChannel(String type) { return channel; }
        @Override void disconnect() { disconnected = true; }
    }

    private Ssh ssh(FakeChannel channel) {
        Ssh ssh = new Ssh().setHostname("localhost").setUsername("test")
                .setPassword("unused").setSuppressException(true)
        ssh.setSession(new FakeSession(channel))
        Field init = Ssh.class.getDeclaredField("init")
        init.setAccessible(true)
        init.set(ssh, true)
        return ssh
    }

    @Test(timeout = 3000L)
    void stalledChannelFailsAndDisconnects() {
        FakeChannel channel = new FakeChannel()
        // A read without available data must never be attempted.
        channel.stdout = new InputStream() {
            @Override public int read() { throw new AssertionError("blocking read"); }
        }
        Ssh ssh = ssh(channel).setExecutionTimeout(80).command("true")
        long start = System.nanoTime()
        SshResult result = ssh.runAndClose()
        assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) < 2000)
        assertNotEquals(0, result.getReturnCode())
        assertTrue(result.getExitErrorMessage().contains("execution timed out"))
        assertTrue(channel.disconnected)
        assertTrue(((FakeSession) ssh.getSession()).disconnected)
        assertTrue(channel.connectTimeout > 0 && channel.connectTimeout <= 80)
    }

    @Test(timeout = 3000L)
    void continuousOutputCannotExtendDeadline() {
        FakeChannel channel = new FakeChannel()
        channel.stdout = new InputStream() {
            @Override public int available() { return 1; }
            @Override public int read() { return 120 }
        }
        SshResult result = ssh(channel).setExecutionTimeout(40).command("true").runAndClose()
        assertNotEquals(0, result.getReturnCode())
        assertTrue(result.getStdout().length() > 0)
        assertTrue(channel.disconnected)
    }

    @Test
    void completedCommandPreservesBothStreamsAndExitStatus() {
        FakeChannel channel = new FakeChannel()
        channel.stdout = new ByteArrayInputStream("out".getBytes("UTF-8"))
        channel.stderr = new ByteArrayInputStream("err".getBytes("UTF-8"))
        channel.finished = true
        channel.status = 7
        SshResult result = ssh(channel).setExecutionTimeout(1000).command("false").runAndClose()
        assertEquals(7, result.getReturnCode())
        assertEquals("out", result.getStdout())
        assertEquals("err", result.getStderr())
        assertTrue(channel.disconnected)
    }

    @Test
    void killGraceIsOptInAndCapturedBeforeShell() {
        FakeChannel ordinary = new FakeChannel()
        ordinary.finished = true
        ssh(ordinary).shell("true").runAndClose()
        assertTrue(ordinary.submittedCommand.contains("timeout 604800 /bin/bash"))
        assertFalse(ordinary.submittedCommand.contains("--kill-after"))

        FakeChannel bounded = new FakeChannel()
        bounded.finished = true
        ssh(bounded).setExecTimeout(90).setExecTimeoutKillAfter(5)
                .setExecutionTimeout(120000).shell("true").runAndClose()
        assertTrue(bounded.submittedCommand.contains("timeout --kill-after=5 90 /bin/bash"))
    }

    @Test(timeout = 3000L)
    void continuousHandshakeTrafficCannotExtendDeadline() {
        ServerSocket server = new ServerSocket(0)
        try {
            Thread peer = new Thread({
                Socket socket = null
                try {
                    socket = server.accept()
                    while (!Thread.currentThread().isInterrupted()) {
                        socket.getOutputStream().write("pre-SSH banner\r\n".getBytes("UTF-8"))
                        socket.getOutputStream().flush()
                        Thread.sleep(20)
                    }
                } catch (IOException | InterruptedException expected) {
                    // Client deadline closes the socket, or test cleanup interrupts the peer.
                } finally {
                    socket?.close()
                }
            } as Runnable)
            peer.setDaemon(true)
            peer.start()
            try {
                long started = System.nanoTime()
                SshResult result = new Ssh().setHostname("127.0.0.1").setPort(server.getLocalPort())
                        .setUsername("test").setPassword("unused").setTimeout(30)
                        .setExecutionTimeout(200).setSuppressException(true).command("true").runAndClose()
                assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started) < 1500)
                assertNotEquals(0, result.getReturnCode())
                assertTrue(result.getExitErrorMessage().contains("execution timed out"))
            } finally {
                peer.interrupt()
                peer.join(500)
            }
        } finally {
            server.close()
        }
    }

    @Test
    void scriptReservesChannelConnectionAndKillGrace() {
        FakeChannel channel = new FakeChannel()
        channel.finished = true
        SshResult result = ssh(channel).setExecTimeout(114).setExecTimeoutKillAfter(5)
                .setExecutionTimeout(25000).shell("true").runAndClose()
        assertEquals(0, result.getReturnCode())
        assertEquals(5000, channel.connectTimeout)
        assertTrue(channel.submittedCommand.matches("(?s).*timeout --kill-after=5 [34] /bin/bash.*"))
    }

    @Test
    void insufficientBudgetDoesNotLaunchScript() {
        FakeChannel channel = new FakeChannel()
        channel.finished = true
        SshResult result = ssh(channel).setExecTimeout(114).setExecTimeoutKillAfter(5)
                .setExecutionTimeout(5000).shell("true").runAndClose()
        assertNotEquals(0, result.getReturnCode())
        assertEquals(0, channel.connectTimeout)
        assertNull(channel.submittedCommand)
        assertTrue(channel.disconnected)
    }

    @Test
    void totalTimeoutRejectsReuseBeforeOpeningAnotherChannel() {
        for (boolean initiallyBounded : [true, false]) {
            FakeChannel channel = new FakeChannel()
            channel.finished = true
            Ssh ssh = ssh(channel).command("true")
            if (initiallyBounded) {
                ssh.setExecutionTimeout(1000)
            }
            try {
                assertEquals(0, ssh.run().getReturnCode())
                ssh.reset().setExecutionTimeout(1000).command("true")
                try {
                    ssh.run()
                    fail("a reused session must not bypass the new socket deadline")
                } catch (SshException expected) {
                    assertTrue(expected.getMessage().contains("new Ssh instance"))
                }
                assertEquals(1, channel.connections)
            } finally {
                ssh.close()
            }
        }
    }

    @Test
    void ordinarySessionStillSupportsRunResetRun() {
        FakeChannel channel = new FakeChannel()
        channel.finished = true
        Ssh ssh = ssh(channel).command("true")
        try {
            assertEquals(0, ssh.run().getReturnCode())
            assertEquals(0, ssh.reset().command("true").run().getReturnCode())
            assertEquals(2, channel.connections)
        } finally {
            ssh.close()
        }
    }

    @Test
    void closeRunsWhenRunThrows() {
        final boolean[] closed = [false] as boolean[]
        Ssh ssh = new Ssh() {
            @Override public SshResult run() { throw new IllegalArgumentException("test"); }
            @Override void close() { closed[0] = true; }
        }
        try {
            ssh.runErrorByExceptionAndClose()
            fail("expected failure")
        } catch (IllegalArgumentException expected) {
            assertTrue(closed[0])
        }
    }
}
