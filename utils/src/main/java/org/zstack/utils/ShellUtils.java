package org.zstack.utils;

import org.apache.commons.io.input.BoundedInputStream;
import org.apache.commons.lang.time.StopWatch;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ShellUtils {
    private static final CLogger logger = Utils.getSafeLogger(ShellUtils.class);

    public static class ShellException extends RuntimeException {
        public ShellException(String msg, Throwable t) {
            super(msg, t);
        }

        public ShellException(String msg) {
            super(msg);
        }

        public ShellException(Throwable t) {
            super(t);
        }
    }

    public static class ShellTimeoutException extends ShellException {
        public ShellTimeoutException(String command, long timeoutSeconds) {
            super(String.format("Shell command timed out after %s seconds: %s",
                    timeoutSeconds, Utils.maskSensitiveInfo(command)));
        }
    }

    private static class StreamConsumer extends Thread {
        final InputStream in;
        final PrintWriter out;
        final boolean flush;

        StreamConsumer(InputStream in, PrintWriter out, boolean flushEveryWrite) {
            this.in = in;
            this.out = out;
            flush = flushEveryWrite;
        }

        @Override
        public void run() {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
                String line;
                while ( (line = br.readLine()) != null) {
                    out.println(line);
                    if (flush) {
                        out.flush();
                    }
                }
            } catch (Exception e) {
                logger.warn(e.getMessage(), e);
            }
        }
    }

    public static String runVerbose(String cmdstr) {
        return runVerbose(cmdstr, null);
    }

    public static String runVerbose(String cmdstr, String baseDir) {
        return doRun(cmdstr, baseDir, true);
    }

    public static String runVerbose(String cmdstr, String baseDir, boolean withSudo) {
        return doRun(cmdstr, baseDir, withSudo, true);
    }

    public static String run(String cmdstr, String baseDir) {
        return doRun(cmdstr, baseDir, false);
    }

    public static String run(String cmdstr, String baseDir, boolean withSudo) {
        return doRun(cmdstr, baseDir, withSudo, false);
    }

    public static String run(String cmdsr) {
        return run(cmdsr, null);
    }

    public static String run(String cmdsr, boolean withSudo) {
        return run(cmdsr, null, withSudo);
    }

    public static class ShellRunner {
        private String command;
        private String baseDir;
        private boolean verbose;
        private boolean suppressTraceLog;
        private String stderrFile;
        private String stdoutFile;
        private Process process;
        private boolean withSudo = true;

        public void terminate() {
            DebugUtils.Assert(process!=null, String.format("you can only can call terminate() after calling run()"));
            process.destroy();
        }

        public boolean isWithSudo() {
            return withSudo;
        }

        public void setWithSudo(boolean withSudo) {
            this.withSudo = withSudo;
        }

        public String getStderrFile() {
            return stderrFile;
        }

        public void setStderrFile(String stderrFile) {
            this.stderrFile = stderrFile;
        }

        public String getStdoutFile() {
            return stdoutFile;
        }

        public void setStdoutFile(String stdoutFile) {
            this.stdoutFile = stdoutFile;
        }

        public String getCommand() {
            return command;
        }

        public void setCommand(String command) {
            this.command = command;
        }

        public String getBaseDir() {
            return baseDir;
        }

        public void setBaseDir(String baseDir) {
            this.baseDir = baseDir;
        }

        public boolean isVerbose() {
            return verbose;
        }

        public void setVerbose(boolean verbose) {
            this.verbose = verbose;
        }

        public boolean isSuppressTraceLog() {
            return suppressTraceLog;
        }

        public void setSuppressTraceLog(boolean suppressTraceLog) {
            this.suppressTraceLog = suppressTraceLog;
        }

        private static final int LOG_TO_FILE = 0;
        private static final int LOG_TO_SCREEN = 1;
        private static final int LOG_TO_STRING = 2;

        private int logStrategy(String fileToCheck) {
            if (fileToCheck != null) {
                return LOG_TO_FILE;
            } else if (verbose) {
                return LOG_TO_SCREEN;
            } else {
                return LOG_TO_STRING;
            }
        }

        private int stdoutLogStrategy() {
            return logStrategy(stdoutFile);
        }

        private int stderrLogStrategy() {
            return logStrategy(stderrFile);
        }

        public Integer obtainUnixPid() {
            try {
                Class clz = process.getClass();
                if (!clz.getName().equals("java.lang.UNIXProcess")) {
                    return null;
                }

                Field pidField = clz.getDeclaredField("pid");
                pidField.setAccessible(true);
                Object value = pidField.get(process);
                return (Integer) value;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public ShellResult run() {
            StopWatch watch = new StopWatch();
            watch.start();
            try {
                if (withSudo) {
                    command = String.format("sudo %s", command);
                }

                ProcessBuilder pb = new ProcessBuilder(Arrays.asList("/bin/bash", "-c", command));
                if (baseDir == null) {
                    baseDir = System.getProperty("user.home");
                }
                pb.directory(new File(baseDir));

                process = pb.start();
                command = Utils.maskSensitiveInfo(command);
                if (!suppressTraceLog && logger.isTraceEnabled()) {
                    logger.debug(String.format("exec shell command[%s]", command));
                }

                Writer stdout;
                int stdoutLog = stdoutLogStrategy();
                if (stdoutLog == LOG_TO_FILE) {
                    stdout = new BufferedWriter(new FileWriter(stdoutFile));
                } else if (stdoutLog == LOG_TO_SCREEN) {
                    stdout = new BufferedWriter(new OutputStreamWriter(System.out));
                } else {
                    stdout = new StringWriter();
                }

                Writer stderr;
                int stderrLog = stderrLogStrategy();
                if (stderrLog == LOG_TO_FILE) {
                    stderr = new BufferedWriter(new FileWriter(stderrFile));
                } else if (stderrLog == LOG_TO_SCREEN) {
                    stderr = new BufferedWriter(new OutputStreamWriter(System.err));
                } else {
                    stderr = new StringWriter();
                }

                StreamConsumer stdoutConsumer = new StreamConsumer(process.getInputStream(), new PrintWriter(stdout, true), stdoutLog != LOG_TO_FILE);
                StreamConsumer stderrConsumer = new StreamConsumer(process.getErrorStream(), new PrintWriter(stderr, true), stderrLog != LOG_TO_FILE);

                stderrConsumer.start();
                stdoutConsumer.start();
                process.waitFor();
                stderrConsumer.join(TimeUnit.SECONDS.toMillis(30));
                stdoutConsumer.join(TimeUnit.SECONDS.toMillis(30));

                ShellResult ret = new ShellResult();
                ret.setCommand(command);
                ret.setRetCode(process.exitValue());
                if (stderrLog == LOG_TO_STRING) {
                    ret.setStderr(stderr.toString());
                } else if (stderrLog == LOG_TO_FILE) {
                    stderr.close();
                }
                if (stdoutLog == LOG_TO_STRING) {
                    ret.setStdout(stdout.toString());
                } else if (stdoutLog == LOG_TO_FILE) {
                    stdout.close();
                }

                return ret;
            } catch (Exception e) {
                StringBuilder sb = new StringBuilder();
                sb.append("Shell command failed:\n");
                sb.append(command);
                Thread.currentThread().interrupt();
                throw new ShellException(sb.toString(), e);
            } finally {
                if (process != null) {
                    process.destroy();
                }
                watch.stop();
                if (!suppressTraceLog && logger.isTraceEnabled()) {
                    logger.trace(String.format("shell command[%s] costs %sms to finish", command, watch.getTime()));
                }
            }
        }

        private ShellResult runWithTimeout(long timeoutSeconds) {
            File stdout = null;
            File stderr = null;
            boolean finished = false;
            boolean interrupted = false;
            Integer pid = null;
            ProcessTreeCleanup cleanup = null;
            String maskedCommand = Utils.maskSensitiveInfo(withSudo ? "sudo " + command : command);
            try {
                // File redirection avoids waiting on pipes inherited by detached descendants.
                // NIO creates these files with owner-only permissions on Unix.
                stdout = Files.createTempFile("zstack-shell-", ".out").toFile();
                stderr = Files.createTempFile("zstack-shell-", ".err").toFile();
                ProcessBuilder builder = new ProcessBuilder("/bin/bash", "-c",
                        withSudo ? "sudo " + command : command);
                builder.directory(new File(System.getProperty("user.home")));
                builder.redirectInput(new File("/dev/null"));
                builder.redirectOutput(stdout);
                builder.redirectError(stderr);
                process = builder.start();
                long started = System.nanoTime();
                pid = obtainUnixPid();
                cleanup = new ProcessTreeCleanup(process, withSudo);
                cleanup.rememberRoot(pid);
                if (cleanup.interrupted) {
                    throw new InterruptedException();
                }
                long remaining = TimeUnit.SECONDS.toNanos(timeoutSeconds) - (System.nanoTime() - started);
                if (!process.waitFor(Math.max(0, remaining), TimeUnit.NANOSECONDS)) {
                    throw new ShellTimeoutException(maskedCommand, timeoutSeconds);
                }
                finished = true;
                ShellResult result = new ShellResult();
                result.setCommand(maskedCommand);
                result.setRetCode(process.exitValue());
                result.setStdout(readOutput(stdout));
                result.setStderr(readOutput(stderr));
                return result;
            } catch (ShellTimeoutException e) {
                throw e;
            } catch (InterruptedException e) {
                interrupted = true;
                throw new ShellException("Shell command interrupted:\n" + maskedCommand, e);
            } catch (Exception e) {
                throw new ShellException("Shell command failed:\n" + maskedCommand, e);
            } finally {
                if (process != null && !finished) {
                    if (cleanup == null) {
                        cleanup = new ProcessTreeCleanup(process, withSudo);
                    }
                    try {
                        cleanup.terminate(pid);
                    } finally {
                        interrupted |= cleanup.interrupted;
                    }
                }
                deleteOutput(stdout);
                deleteOutput(stderr);
                if (interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private static String readOutput(File file) throws IOException {
        StringWriter output = new StringWriter();
        // A child can keep writing after the shell exits. Read only the completed
        // file snapshot instead of following that writer indefinitely.
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new BoundedInputStream(new FileInputStream(file), file.length())))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
        }
        return output.toString();
    }

    private static void deleteOutput(File file) {
        if (file != null && !file.delete()) {
            logger.warn(String.format("failed to delete shell output file[%s]", file));
        }
    }

    private static class ProcessTreeCleanup {
        private final Process process;
        private final boolean withSudo;
        private long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        private final boolean procfs = new File("/proc/self/stat").isFile();
        private ProcessIdentity rootIdentity;
        private boolean interrupted;

        private ProcessTreeCleanup(Process process, boolean withSudo) {
            this.process = process;
            this.withSudo = withSudo;
        }

        private static class ProcessIdentity {
            private final String pid;
            private final String parent;
            private final String started;

            private ProcessIdentity(String[] fields) {
                pid = fields[0];
                parent = fields[1];
                started = fields[2];
            }

            private boolean matches(ProcessIdentity other) {
                return other != null && pid.equals(other.pid) && started.equals(other.started);
            }
        }

        private void rememberRoot(Integer pid) {
            if (pid == null) {
                return;
            }
            try {
                ProcessIdentity identity = procfs ? readProcIdentity(new File("/proc", pid.toString()))
                        : snapshot().get(pid.toString());
                // Check after reading: never record the identity of a reused PID.
                if (process.isAlive()) {
                    rootIdentity = identity;
                }
            } catch (IOException e) {
                // A short-lived command may already have exited. Without its identity,
                // cleanup can still safely destroy the Process object itself.
                if (process.isAlive()) {
                    logger.warn("could not record shell process identity", e);
                }
            }
        }

        private ProcessIdentity readProcIdentity(File directory) throws IOException {
            String stat = new String(Files.readAllBytes(new File(directory, "stat").toPath()), StandardCharsets.UTF_8);
            // comm can contain spaces and parentheses. Fields after its last ')' start
            // at state (3); ppid is field 4 and the kernel start-time ticks are field 22.
            int end = stat.lastIndexOf(')');
            String[] fields = end < 0 ? new String[0] : stat.substring(end + 1).trim().split("\\s+");
            if (fields.length < 20) {
                throw new IOException("invalid process stat: " + directory);
            }
            return new ProcessIdentity(new String[]{directory.getName(), fields[1], fields[19]});
        }

        private boolean waitFor(Process child, long maxMillis) {
            long until = Math.min(deadline, System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(maxMillis));
            while (System.nanoTime() < until) {
                try {
                    return child.waitFor(until - System.nanoTime(), TimeUnit.NANOSECONDS);
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
            return !child.isAlive();
        }

        private Map<String, ProcessIdentity> snapshot() throws IOException {
            if (procfs) {
                File[] processes = new File("/proc").listFiles();
                if (processes == null) {
                    throw new IOException("could not list process tree");
                }
                Map<String, ProcessIdentity> result = new LinkedHashMap<>();
                for (File directory : processes) {
                    if (System.nanoTime() >= deadline) {
                        throw new IOException("could not obtain process tree before cleanup deadline");
                    }
                    if (!directory.getName().matches("[0-9]+")) {
                        continue;
                    }
                    try {
                        ProcessIdentity identity = readProcIdentity(directory);
                        result.put(identity.pid, identity);
                    } catch (IOException ignored) {
                        // Processes can exit while /proc is being enumerated.
                    }
                }
                return result;
            }
            File output = Files.createTempFile("zstack-processes-", ".out").toFile();
            Process ps = null;
            try {
                ProcessBuilder builder = new ProcessBuilder("/bin/ps", "-e", "-o", "pid=", "-o", "ppid=", "-o", "lstart=");
                builder.environment().put("LC_ALL", "C");
                builder.redirectInput(new File("/dev/null"));
                builder.redirectOutput(output);
                builder.redirectError(new File("/dev/null"));
                ps = builder.start();
                if (!waitFor(ps, 500) || ps.exitValue() != 0) {
                    throw new IOException("could not obtain process tree before cleanup deadline");
                }
                Map<String, ProcessIdentity> result = new LinkedHashMap<>();
                try (BufferedReader reader = new BufferedReader(new FileReader(output))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] fields = line.trim().split("\\s+", 3);
                        if (fields.length == 3) {
                            ProcessIdentity identity = new ProcessIdentity(fields);
                            result.put(identity.pid, identity);
                        }
                    }
                }
                return result;
            } finally {
                if (ps != null && ps.isAlive()) {
                    ps.destroyForcibly();
                }
                deleteOutput(output);
            }
        }

        private void addDescendants(Map<String, ProcessIdentity> owned, Map<String, ProcessIdentity> current) {
            boolean added;
            do {
                added = false;
                for (ProcessIdentity candidate : current.values()) {
                    ProcessIdentity parent = owned.get(candidate.parent);
                    if (!owned.containsKey(candidate.pid) && parent != null && parent.matches(current.get(parent.pid))) {
                        owned.put(candidate.pid, candidate);
                        added = true;
                    }
                }
            } while (added);
        }

        private void signal(String signal, Map<String, ProcessIdentity> owned, Map<String, ProcessIdentity> current) throws IOException {
            List<String> pids = new ArrayList<>();
            for (ProcessIdentity identity : owned.values()) {
                // Keep an exited root as an ancestry anchor, but never signal its
                // numeric PID: it may have been reused since the snapshot.
                if (identity == rootIdentity && !process.isAlive()) {
                    continue;
                }
                if (identity.matches(current.get(identity.pid))) {
                    pids.add(identity.pid);
                }
            }
            if (pids.isEmpty() || System.nanoTime() >= deadline) {
                return;
            }
            // Signal children first, retaining their identities when their parents exit.
            Collections.reverse(pids);
            List<String> command = new ArrayList<>();
            if (withSudo) {
                command.addAll(Arrays.asList("sudo", "-n"));
            }
            command.addAll(Arrays.asList("/bin/kill", "-" + signal));
            command.addAll(pids);
            Process kill = new ProcessBuilder(command).redirectInput(new File("/dev/null"))
                    .redirectOutput(new File("/dev/null")).redirectError(new File("/dev/null")).start();
            if (!waitFor(kill, 500)) {
                kill.destroyForcibly();
            }
        }

        private void terminate(Integer rootPid) {
            deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
            try {
                terminate(rootPid, snapshot());
            } catch (Exception e) {
                logger.warn("failed to clean up shell process tree", e);
            } finally {
                if (process.isAlive()) {
                    process.destroyForcibly();
                }
                waitFor(process, 500);
            }
        }

        private void terminate(Integer rootPid, Map<String, ProcessIdentity> current) throws IOException {
            Map<String, ProcessIdentity> owned = new LinkedHashMap<>();
            ProcessIdentity root = current.get(String.valueOf(rootPid));
            // The root can exit after this snapshot. Its startup identity still proves
            // ownership of the captured descendants without adopting a reused PID.
            if (rootIdentity != null && rootIdentity.matches(root)) {
                owned.put(root.pid, rootIdentity);
                // sshpass may create a new session, so process groups are insufficient.
                addDescendants(owned, current);
                signal("TERM", owned, current);
                waitFor(process, 200);
                current = snapshot();
                addDescendants(owned, current);
                signal("KILL", owned, current);
            }
        }
    }

    private static String doRun(String cmdstr, String baseDir, boolean isVerbose) {
        return doRun(cmdstr, baseDir, true, isVerbose);
    }

    private static String doRun(String cmdstr, String baseDir, boolean withRoot, boolean isVerbose) {
        ShellRunner runner = new ShellRunner();
        runner.command = cmdstr;
        runner.baseDir = baseDir;
        runner.verbose = isVerbose;
        runner.withSudo = withRoot;
        ShellResult ret = runner.run();

        ret.raiseExceptionIfFail();

        StringBuilder sb = new StringBuilder(String.format("exec shell: %s\n", cmdstr));

        // if isVerbose is set, there is nothing to read as stream has been closed
        if (!isVerbose) {
            sb.append(String.format("stdout: %s\n", ret.getStdout()));
            sb.append(String.format("stderr: %s\n", ret.getStderr()));
        }

        return sb.toString();
    }

    public static ShellResult runAndReturn(String cmdstr, String baseDir, boolean withSudo) {
        return doRunAndReturn(cmdstr, baseDir, withSudo);
    }

    public static ShellResult runAndReturn(String cmdstr, String baseDir) {
        return doRunAndReturn(cmdstr, baseDir, true);
    }

    public static ShellResult runAndReturn(String cmdsr) {
        return runAndReturn(cmdsr, null);
    }

    public static ShellResult runAndReturn(String cmdsr, boolean withSudo) {
        return doRunAndReturn(cmdsr, null, withSudo);
    }

    public static ShellResult runAndReturn(String cmdstr, boolean withSudo, long timeoutSeconds) {
        if (timeoutSeconds <= 0) {
            throw new IllegalArgumentException("timeoutSeconds must be positive");
        }
        ShellRunner runner = new ShellRunner();
        runner.command = cmdstr;
        runner.withSudo = withSudo;
        return runner.runWithTimeout(timeoutSeconds);
    }

    private static ShellResult doRunAndReturn(String cmdstr, String baseDir, boolean withSudo) {
        ShellRunner runner = new ShellRunner();
        runner.command = cmdstr;
        runner.baseDir = baseDir;
        runner.withSudo = withSudo;
        return runner.run();
    }

    public static String getScriptsPath(String scriptName) {
        return PathUtil.findFileOnClassPath(scriptName, true).getAbsolutePath();
    }
}
