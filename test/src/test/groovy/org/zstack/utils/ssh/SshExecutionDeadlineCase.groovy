package org.zstack.utils.ssh

import org.junit.Test

import java.io.IOException
import java.net.ServerSocket
import java.net.Socket

import static org.junit.Assert.*

class SshExecutionDeadlineCase {
    @Test(timeout = 3000L)
    void channelSetupDeadlineClosesSocketDuringBlockedIo() {
        SshExecutionDeadline deadline = new SshExecutionDeadline(10000, 1000)
        deadline.watch()
        ServerSocket server = new ServerSocket(0)
        Socket socket = null
        SshExecutionDeadline.PhaseTimeout phase = null
        try {
            socket = deadline.createSocket("127.0.0.1", server.getLocalPort())
            phase = deadline.watchPhase(80)
            try {
                socket.getInputStream().read()
                fail("expected the setup deadline to close the socket")
            } catch (IOException expected) {
                assertTrue(socket.isClosed())
                assertFalse(deadline.finish())
            }
        } finally {
            phase?.close()
            socket?.close()
            server.close()
            deadline.finish()
        }
    }

    @Test(timeout = 3000L)
    void finishedSetupDoesNotCloseSuccessfulConnectionLater() {
        SshExecutionDeadline deadline = new SshExecutionDeadline(10000, 1000)
        deadline.watch()
        ServerSocket server = new ServerSocket(0)
        Socket socket = null
        try {
            socket = deadline.createSocket("127.0.0.1", server.getLocalPort())
            deadline.watchPhase(80).close()
            Thread.sleep(120)
            assertFalse(socket.isClosed())
            assertTrue(deadline.finish())
        } finally {
            socket?.close()
            server.close()
            deadline.finish()
        }
    }
}
