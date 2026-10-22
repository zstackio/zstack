package org.zstack.test.unittest.vfs

import org.junit.Test
import org.zstack.testlib.vfs.Raw
import org.zstack.testlib.vfs.VFS

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class VFSMetadataReadWriteTest {
    @Test
    void testReadersSeeCompleteMetadataDuringConcurrentVolumeUpdates() {
        VFS vfs = new VFS()
        def executor = Executors.newFixedThreadPool(3)
        try {
            vfs.createDirectories("/disk")
            Raw volume = vfs.createRaw("/disk/volume.raw", 0L)
            def start = new CountDownLatch(1)
            def writer = executor.submit({
                start.await()
                2000.times { value ->
                    volume.actualSize = value
                    volume.update()
                }
            } as Runnable)
            def readers = (1..2).collect {
                executor.submit({
                    start.await()
                    2000.times {
                        Raw current = vfs.getFile(volume.pathString(), true)
                        assert current.virtualSize == 0L
                        assert current.actualSize >= 0L && current.actualSize < 2000L
                    }
                } as Runnable)
            }
            start.countDown()
            writer.get(30, TimeUnit.SECONDS)
            readers.each { it.get(30, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
            assert executor.awaitTermination(30, TimeUnit.SECONDS)
            vfs.destroy()
        }
    }
}
