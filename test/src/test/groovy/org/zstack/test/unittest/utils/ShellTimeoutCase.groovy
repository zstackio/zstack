package org.zstack.test.unittest.utils

import org.junit.Test
import org.zstack.utils.ShellResult
import org.zstack.utils.ShellUtils
import org.zstack.utils.Utils
import org.zstack.utils.ssh.SshShell

import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class ShellTimeoutCase {
    @Test
    void returnsOutputAndExitStatus() {
        ShellResult result = ShellUtils.runAndReturn("printf 'hello'; printf 'failure' >&2; exit 7", false, 5)
        assert result.retCode == 7
        assert result.stdout == "hello\n"
        assert result.stderr == "failure\n"
        assert !Thread.currentThread().isInterrupted()
    }

    @Test
    void rejectsNonPositiveTimeout() {
        for (int timeout : [0, -1]) {
            try {
                ShellUtils.runAndReturn("true", false, timeout)
                assert false : "accepted a nonpositive timeout"
            } catch (IllegalArgumentException expected) {
                // Validation happens before creating any process or secret file.
            }
            try {
                new SshShell().runCommand("true", timeout)
                assert false : "accepted a nonpositive SSH timeout"
            } catch (IllegalArgumentException expected) {
                // No SSH configuration is needed to validate the timeout.
            }
        }
    }

    @Test(timeout = 10000L)
    void timeoutIsTypedMaskedAndDoesNotInterrupt() {
        Set<String> originalFiles = outputFiles()
        long start = System.nanoTime()
        Utils.MaskWords mask = new Utils.MaskWords([secret: "******"])
        try {
            try {
                ShellUtils.runAndReturn("printf '%s' 'secret'; sleep 60", false, 1)
                assert false : "expected timeout"
            } catch (ShellUtils.ShellTimeoutException expected) {
                assert expected.message.contains("1 seconds")
                assert expected.message.contains("'******'")
                assert !expected.message.contains("'secret'")
            }
        } finally {
            mask.close()
        }
        assert elapsedMillis(start) < 6000 : "timeout and cleanup exceeded their bound"
        assert !Thread.currentThread().isInterrupted() : "expiry must not interrupt the caller"
        assert outputFiles() == originalFiles
    }

    @Test(timeout = 15000L)
    void killsDetachedTermResistantTreeAndLeavesUnrelatedProcess() {
        Path directory = Files.createTempDirectory("shell-tree-test-")
        Path rootPid = directory.resolve("root.pid")
        Path detachedPid = directory.resolve("detached.pid")
        Path childPid = directory.resolve("child.pid")
        Process unrelated = new ProcessBuilder("/bin/sleep", "60").start()
        try {
            // sshpass also uses setsid. Both descendants ignore TERM and survive
            // their original parent, so KILL must use the retained identities.
            String perl = '''use POSIX qw(setsid); setsid() or die $!; $SIG{TERM}='IGNORE';
                open(F, '>', $ARGV[0]) or die $!; print F $$; close(F);
                my $child=fork(); defined($child) or die $!;
                if (!$child) { open(F, '>', $ARGV[1]) or die $!; print F $$; close(F); sleep 60; exit; }
                waitpid($child, 0);'''
            String command = 'echo $$ > ' + quote(rootPid.toString()) + '; /usr/bin/perl -e ' + quote(perl) +
                    ' ' + quote(detachedPid.toString()) + ' ' + quote(childPid.toString()) + ' & wait'
            long start = System.nanoTime()
            try {
                ShellUtils.runAndReturn(command, false, 1)
                assert false : "expected timeout"
            } catch (ShellUtils.ShellTimeoutException expected) {
                assert elapsedMillis(start) < 6000
            }
            assert Files.exists(detachedPid) : "detached process did not start"
            assert Files.exists(childPid) : "detached child did not start"
            assertStopped(read(rootPid))
            assertStopped(read(detachedPid))
            assertStopped(read(childPid))
            assert unrelated.isAlive() : "an unrelated process was killed"
        } finally {
            unrelated.destroyForcibly()
            killRecorded(rootPid)
            killRecorded(detachedPid)
            killRecorded(childPid)
            deleteDirectory(directory)
        }
    }

    @Test(timeout = 10000L)
    void interruptionCleansProcessesAndRestoresFlag() {
        Path pidFile = Files.createTempFile("shell-interrupt-test-", ".pid")
        AtomicBoolean interrupted = new AtomicBoolean()
        AtomicReference<Throwable> failure = new AtomicReference<>()
        Thread worker = new Thread({
            try {
                ShellUtils.runAndReturn('echo $$ > ' + quote(pidFile.toString()) + '; sleep 60', false, 60)
                assert false : "interrupted command returned normally"
            } catch (ShellUtils.ShellException e) {
                if (!(e.cause instanceof InterruptedException)) {
                    failure.set(e)
                }
                interrupted.set(Thread.currentThread().isInterrupted())
            } catch (Throwable e) {
                failure.set(e)
            }
        } as Runnable)
        try {
            worker.start()
            long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(3)
            while (Files.size(pidFile) == 0 && System.nanoTime() < until) {
                Thread.sleep(10)
            }
            assert Files.size(pidFile) > 0 : "command did not start"
            worker.interrupt()
            worker.join(5000)
            assert !worker.isAlive() : "cleanup did not return"
            assert failure.get() == null
            assert interrupted.get() : "interruption flag was lost"
            assertStopped(read(pidFile))
        } finally {
            worker.interrupt()
            killRecorded(pidFile)
            Files.deleteIfExists(pidFile)
        }
    }

    @Test(timeout = 5000L)
    void exitedRootCannotAdoptAReusedPid() {
        Process exited = new ProcessBuilder("/bin/bash", "-c", "exit 0").start()
        Process unrelated = new ProcessBuilder("/bin/sleep", "60").start()
        try {
            assert exited.waitFor(1, TimeUnit.SECONDS)
            ShellUtils.ShellRunner runner = new ShellUtils.ShellRunner()
            Field process = ShellUtils.ShellRunner.getDeclaredField("process")
            process.setAccessible(true)
            process.set(runner, unrelated)
            Integer reusedPid = runner.obtainUnixPid()
            assert reusedPid != null

            // Deterministically simulate the original shell exiting and its numeric PID
            // now identifying an unrelated process. No real PID-reuse race is needed.
            Class<?> cleanup = Class.forName('org.zstack.utils.ShellUtils$ProcessTreeCleanup')
            Constructor<?> constructor = cleanup.getDeclaredConstructor(Process, Boolean.TYPE)
            constructor.setAccessible(true)
            Object cleaner = constructor.newInstance(exited, false)
            Field rootIdentity = cleanup.getDeclaredField("rootIdentity")
            rootIdentity.setAccessible(true)
            Constructor<?> identity = rootIdentity.type.getDeclaredConstructor(String[].class)
            identity.setAccessible(true)
            Object previous = identity.newInstance(
                    [([reusedPid.toString(), "1", "previous-process-lifetime"] as String[])] as Object[])
            rootIdentity.set(cleaner, previous)
            Method terminate = cleanup.getDeclaredMethod("terminate", Integer)
            terminate.setAccessible(true)
            terminate.invoke(cleaner, reusedPid)
            assert unrelated.isAlive() : "an exited root adopted and killed a reused PID"

            // The initial snapshot can also predate PID reuse and still match the
            // recorded root identity. Retain ancestry without signalling that PID.
            Method terminateCaptured = cleanup.getDeclaredMethod("terminate", Integer, Map)
            terminateCaptured.setAccessible(true)
            terminateCaptured.invoke(cleaner, reusedPid, [(reusedPid.toString()): previous])
            assert !unrelated.waitFor(100, TimeUnit.MILLISECONDS) :
                    "a historical snapshot caused an exited root PID to be signalled"
        } finally {
            unrelated.destroyForcibly()
            unrelated.waitFor(1, TimeUnit.SECONDS)
        }
    }

    @Test(timeout = 10000L)
    void cleansCapturedDescendantsWhenRootExitsAfterSnapshot() {
        Path directory = Files.createTempDirectory("shell-root-exit-test-")
        Path childPid = directory.resolve("child.pid")
        Path grandchildPid = directory.resolve("grandchild.pid")
        String perl = '''use POSIX qw(setsid); setsid() or die $!; $SIG{TERM}='IGNORE';
            open(F, '>', $ARGV[0]) or die $!; print F $$; close(F);
            my $child=fork(); defined($child) or die $!;
            if (!$child) { open(F, '>', $ARGV[1]) or die $!; print F $$; close(F); sleep 60; exit; }
            waitpid($child, 0);'''
        Process root = new ProcessBuilder("/bin/bash", "-c", '/usr/bin/perl -e ' + quote(perl) +
                ' ' + quote(childPid.toString()) + ' ' + quote(grandchildPid.toString()) + ' & wait').start()
        Process unrelated = new ProcessBuilder("/bin/sleep", "60").start()
        try {
            long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(3)
            while ((!Files.exists(grandchildPid) || Files.size(grandchildPid) == 0) && System.nanoTime() < until) {
                Thread.sleep(10)
            }
            assert Files.exists(grandchildPid) && Files.size(grandchildPid) > 0
            ShellUtils.ShellRunner runner = new ShellUtils.ShellRunner()
            Field process = ShellUtils.ShellRunner.getDeclaredField("process")
            process.setAccessible(true)
            process.set(runner, root)
            Integer rootPid = runner.obtainUnixPid()
            assert rootPid != null

            Class<?> cleanup = Class.forName('org.zstack.utils.ShellUtils$ProcessTreeCleanup')
            Constructor<?> constructor = cleanup.getDeclaredConstructor(Process, Boolean.TYPE)
            constructor.setAccessible(true)
            Object cleaner = constructor.newInstance(root, false)
            Method remember = cleanup.getDeclaredMethod("rememberRoot", Integer)
            remember.setAccessible(true)
            remember.invoke(cleaner, rootPid)
            Method snapshot = cleanup.getDeclaredMethod("snapshot")
            snapshot.setAccessible(true)
            Map captured = (Map) snapshot.invoke(cleaner)
            assert captured.containsKey(rootPid.toString())
            assert captured.containsKey(read(childPid))
            assert captured.containsKey(read(grandchildPid))
            AtomicBoolean exitedAfterSnapshot = new AtomicBoolean()
            Map exiting = new LinkedHashMap(captured) {
                @Override
                Object get(Object key) {
                    // Force the exact race: the snapshot contains the owned tree,
                    // but the root exits before cleanup checks its ownership.
                    if (key == rootPid.toString() && exitedAfterSnapshot.compareAndSet(false, true)) {
                        root.destroyForcibly()
                        assert root.waitFor(1, TimeUnit.SECONDS)
                    }
                    return super.get(key)
                }
            }
            Method terminate = cleanup.getDeclaredMethod("terminate", Integer, Map)
            terminate.setAccessible(true)
            long start = System.nanoTime()
            terminate.invoke(cleaner, rootPid, exiting)
            assert exitedAfterSnapshot.get() && !root.isAlive()
            assert elapsedMillis(start) < 4000
            assertStopped(read(childPid))
            assertStopped(read(grandchildPid))
            assert unrelated.isAlive() : "an unrelated process was killed"
        } finally {
            root.destroyForcibly()
            unrelated.destroyForcibly()
            root.waitFor(1, TimeUnit.SECONDS)
            unrelated.waitFor(1, TimeUnit.SECONDS)
            killRecorded(childPid)
            killRecorded(grandchildPid)
            deleteDirectory(directory)
        }
    }

    @Test
    void readsPreciseProcIdentityWithParenthesesInCommandName() {
        Path directory = Files.createTempDirectory("shell-proc-stat-test-")
        try {
            // Fields 3..22, with a command name that must not be split on spaces
            // or on the first closing parenthesis. Distinguish subsecond starts.
            List<String> fields = ["S", "42"] + Collections.nCopies(17, "0") + ["123456"]
            Path stat = directory.resolve("stat")
            Files.write(stat, ("123 (worker )\n name) " + fields.join(" ") + "\n").getBytes(StandardCharsets.UTF_8))
            Class<?> cleanup = Class.forName('org.zstack.utils.ShellUtils$ProcessTreeCleanup')
            Constructor<?> constructor = cleanup.getDeclaredConstructor(Process, Boolean.TYPE)
            constructor.setAccessible(true)
            Object cleaner = constructor.newInstance(null, false)
            Method readIdentity = cleanup.getDeclaredMethod("readProcIdentity", File)
            readIdentity.setAccessible(true)
            Object original = readIdentity.invoke(cleaner, directory.toFile())
            Field parent = original.class.getDeclaredField("parent")
            parent.setAccessible(true)
            assert parent.get(original) == "42"
            Method matches = original.class.getDeclaredMethod("matches", original.class)
            matches.setAccessible(true)
            assert matches.invoke(original, readIdentity.invoke(cleaner, directory.toFile()))
            fields[19] = "123457"
            Files.write(stat, ("123 (worker )\n name) " + fields.join(" ") + "\n").getBytes(StandardCharsets.UTF_8))
            assert !matches.invoke(original, readIdentity.invoke(cleaner, directory.toFile())) :
                    "a reused PID with a different kernel start time matched"
        } finally {
            deleteDirectory(directory)
        }
    }

    @Test(timeout = 10000L)
    void legacyOverloadStillWaitsWithoutOptIn() {
        ShellResult result = ShellUtils.runAndReturn("sleep 1.2; printf 'legacy'", false)
        assert result.retCode == 0
        assert result.stdout == "legacy\n"
    }

    @Test(timeout = 5000L)
    void outputCollectionDoesNotWaitForInheritedDescriptors() {
        Path pid = Files.createTempFile("shell-output-test-", ".pid")
        try {
            long start = System.nanoTime()
            ShellResult result = ShellUtils.runAndReturn('sleep 60 & echo $! > ' +
                    quote(pid.toString()) + "; printf 'complete'; exit 0", false, 1)
            assert result.stdout == "complete\n"
            assert elapsedMillis(start) < 2000 : "waited for a background process holding output open"
        } finally {
            killRecorded(pid)
            Files.deleteIfExists(pid)
        }
    }

    @Test(timeout = 20000L)
    void sshTimeoutPreservesTypeAndDeletesBothKindsOfSecretFile() {
        Path directory = Files.createTempDirectory("ssh-timeout-test-")
        Path record = directory.resolve("secret.path")
        Path pid = directory.resolve("ssh.pid")
        Path log = directory.resolve("jvm.log")
        writeExecutable(directory.resolve("sshpass"), '''#!/bin/bash
printf '%s' "${1#-f}" > "$TEST_SECRET_PATH"
shift
exec "$@"
''')
        writeExecutable(directory.resolve("ssh"), '''#!/bin/bash
while [ $# -gt 0 ]; do if [ "$1" = '-i' ]; then shift; printf '%s' "$1" > "$TEST_SECRET_PATH"; fi; shift; done
echo $$ > "$TEST_SSH_PID"
trap '' TERM
while :; do sleep 1; done
''')
        try {
            for (String credential : ["password", "key"]) {
                ProcessBuilder builder = new ProcessBuilder(System.getProperty("java.home") + "/bin/java",
                        "-cp", System.getProperty("java.class.path"), SshTimeoutMain.name, credential)
                // An inherited SSH session makes noninteractive bash source .bashrc,
                // which can replace PATH and bypass the fake SSH executables.
                for (String variable : ["SSH_CLIENT", "SSH_CONNECTION", "SSH_TTY"]) {
                    builder.environment().remove(variable)
                }
                builder.environment().put("PATH", directory.toString() + File.pathSeparator + System.getenv("PATH"))
                builder.environment().put("TEST_SECRET_PATH", record.toString())
                builder.environment().put("TEST_SSH_PID", pid.toString())
                builder.redirectErrorStream(true).redirectOutput(log.toFile())
                Process child = builder.start()
                try {
                    assert child.waitFor(8, TimeUnit.SECONDS) : "SSH test JVM did not return"
                    assert child.exitValue() == 0 : read(log)
                    assert read(log).contains("typed timeout; interrupted=false") : read(log)
                    assert Files.size(record) > 0 : "fake SSH did not see a secret file"
                    assert !Files.exists(new File(read(record)).toPath()) : "secret file was retained on timeout"
                    assert Files.exists(pid) : credential + " fake SSH did not start: " + read(log)
                    assertStopped(read(pid))
                } finally {
                    child.destroyForcibly()
                    killRecorded(pid)
                    Files.deleteIfExists(record)
                    Files.deleteIfExists(pid)
                }
            }
        } finally {
            deleteDirectory(directory)
        }
    }

    static class SshTimeoutMain {
        static void main(String[] args) {
            SshShell shell = new SshShell()
            shell.withSudo = false
            shell.hostname = "unused.invalid"
            shell.username = "test"
            if (args[0] == "password") {
                shell.password = "password-content"
            } else {
                shell.privateKey = "private-key-content"
            }
            try {
                shell.runCommand("echo hello", 2)
                assert false : "SSH command did not time out"
            } catch (ShellUtils.ShellTimeoutException expected) {
                println "typed timeout; interrupted=" + Thread.currentThread().isInterrupted()
            }
        }
    }

    private static long elapsedMillis(long start) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)
    }

    private static String quote(String text) {
        return "'" + text.replace("'", "'\"'\"'") + "'"
    }

    private static String read(Path path) {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8).trim()
    }

    private static void assertStopped(String pid) {
        long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(2)
        while (System.nanoTime() < until) {
            Process ps = new ProcessBuilder("/bin/ps", "-p", pid, "-o", "stat=").start()
            assert ps.waitFor(1, TimeUnit.SECONDS)
            byte[] bytes = new byte[128]
            int count = ps.inputStream.read(bytes)
            if (count <= 0 || new String(bytes, 0, count, StandardCharsets.UTF_8).trim().startsWith("Z")) {
                return
            }
            Thread.sleep(20)
        }
        assert false : "owned process still running: " + pid
    }

    private static void killRecorded(Path file) {
        if (Files.exists(file) && Files.size(file) > 0) {
            new ProcessBuilder("/bin/kill", "-KILL", read(file)).start().waitFor(1, TimeUnit.SECONDS)
        }
    }

    private static void writeExecutable(Path path, String text) {
        Files.write(path, text.getBytes(StandardCharsets.UTF_8))
        assert path.toFile().setExecutable(true, true)
    }

    private static Set<String> outputFiles() {
        Set<String> result = new HashSet<>()
        String[] files = new File(System.getProperty("java.io.tmpdir")).list()
        assert files != null
        for (String name : files) {
            if (name.startsWith("zstack-shell-") || name.startsWith("zstack-processes-")) {
                result.add(name)
            }
        }
        return result
    }

    private static void deleteDirectory(Path directory) {
        File[] files = directory.toFile().listFiles()
        assert files != null
        for (File file : files) {
            Files.deleteIfExists(file.toPath())
        }
        Files.deleteIfExists(directory)
    }
}
