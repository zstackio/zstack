package org.zstack.utils.ssh;

import com.jcraft.jsch.*;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;
import static org.zstack.utils.StringDSL.ln;
import static org.zstack.utils.StringDSL.s;
import static org.zstack.utils.path.PathUtil.setFilePosixPermissions;
import static org.zstack.utils.path.PathUtil.writeFile;

/**
 */
public class Ssh {
    private static final CLogger logger = Utils.getLogger(Ssh.class);
    private static final int MAX_CHANNEL_SETUP_TIMEOUT_MILLIS = 5000;
    // Channel open, PTY and exec requests each use the channel timeout.
    private static final int CHANNEL_SETUP_REQUEST_COUNT = 3;
    public static final int SCRIPT_CHANNEL_SETUP_TIMEOUT_SECONDS = (int) TimeUnit.MILLISECONDS.toSeconds(
            (long) CHANNEL_SETUP_REQUEST_COUNT * MAX_CHANNEL_SETUP_TIMEOUT_MILLIS);

    private String hostname;
    private String username;
    private String privateKey;
    private String password;
    private int port = 22;
    /**
     * used for ssh channel connection
     *
     * jsch uses this value as timeout waiting for
     * ssh connection message which contains
     * SSH_MSG_CHANNEL_OPEN_CONFIRMATION.
     *
     * try to extend timeout if fails to open the channel
     */
    private int timeout = 5;
    private int execTimeout = 604800;
    private int execTimeoutKillAfter;
    private long executionTimeout;
    private SshExecutionDeadline executionDeadline;
    private int socketTimeout = 300;
    private List<SshRunner> commands = new ArrayList<SshRunner>();
    private Session session;
    private File privateKeyFile;
    private boolean closed = false;
    private boolean suppressException = false;
    private ScriptRunner script;
    private String language = "LANG=\"en_US.UTF-8\"; ";

    private boolean init = false;
    private boolean executed = false;

    private interface SshRunner {
        SshResult run();
        String getCommand();
        String getCommandWithoutPassword();
    }

    private class ScriptRunner {
        String scriptName;
        File scriptFile;
        SshRunner scriptCommand;
        String scriptContent;

        ScriptRunner(String scriptName, String parameters, Map token) {
            this.scriptName = scriptName;
            String scriptPath = PathUtil.findFileOnClassPath(scriptName, true).getAbsolutePath();
            try {
                if (parameters == null) {
                    parameters = "";
                }
                if (token == null) {
                    token = new HashMap();
                }

                String contents = FileUtils.readFileToString(new File(scriptPath));
                String srcScript = String.format("zstack-script-%s", UUID.randomUUID().toString());
                scriptFile = new File(PathUtil.join(PathUtil.getFolderUnderZStackHomeFolder("temp-scripts"), srcScript));
                scriptContent = s(contents).formatByMap(token);

                scriptCommand = createScriptCommand(scriptContent, parameters);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        ScriptRunner(String script) {
            scriptCommand = createScriptCommand(script, "");
        }

        private SshRunner createScriptCommand(String content, String parameters) {
            final int seconds = execTimeout;
            final int killAfter = execTimeoutKillAfter;
            final Map tokens = map(
                    e("remotePath", String.format("/tmp/%s", UUID.randomUUID())),
                    e("scriptContent", content), e("parameters", parameters),
                    e("execTimeout", remoteTimeoutOptions(seconds, killAfter)),
                    e("stdout", String.format("/tmp/%s", UUID.randomUUID())),
                    e("stderr", String.format("/tmp/%s", UUID.randomUUID())));
            final org.zstack.utils.StringDSL.StringWrapper template = ln(
                    "cat << EOF1 > {remotePath}",
                    "PATH=/usr/local/bin:/bin:/usr/bin:/usr/local/sbin:/usr/sbin:/sbin",
                    "{scriptContent}", "EOF1",
                    "timeout {execTimeout} /bin/bash {remotePath} {parameters} 1>{stdout} 2>{stderr}",
                    "ret=$?", "test -f {stdout} && cat {stdout}",
                    "test -f {stderr} && cat {stderr} 1>&2",
                    "rm -f {remotePath}", "rm -f {stdout}", "rm -f {stderr}", "exit $ret");
            return createCommand(template.formatByMap(tokens), channelTimeout -> {
                // Session connection has finished. Also reserve the full channel-open
                // allowance (open, PTY and exec requests each use this timeout),
                // remote KILL grace, and one second to collect the exit status.
                long remaining = executionDeadline.remainingMillis()
                        - (long) CHANNEL_SETUP_REQUEST_COUNT * channelTimeout;
                int remoteSeconds = (int) TimeUnit.MILLISECONDS.toSeconds(remaining) - killAfter - 1;
                if (remoteSeconds <= 0) {
                    throw new java.net.SocketTimeoutException("Insufficient SSH execution time to start remote script");
                }
                tokens.put("execTimeout", remoteTimeoutOptions(Math.min(seconds, remoteSeconds), killAfter));
                return template.formatByMap(tokens);
            });
        }

        SshResult run() {
            return scriptCommand.run();
        }

        void cleanup() {
            if (scriptFile != null) {
                if (!scriptFile.delete()) {
                    logger.warn("delete file failed: " +scriptFile.getAbsolutePath());
                }
            }
        }
    }

    public int getExecTimeout() {
        return execTimeout;
    }

    public Ssh setExecTimeout(int execTimeout) {
        this.execTimeout = execTimeout;
        return this;
    }

    /** Configure before shell()/script(), which capture the remote command. */
    public Ssh setExecTimeoutKillAfter(int seconds) {
        if (seconds <= 0) {
            throw new IllegalArgumentException("kill-after must be positive");
        }
        this.execTimeoutKillAfter = seconds;
        return this;
    }

    /**
     * Opt-in local budget in milliseconds, including session/channel connection.
     * Requires a fresh instance: a reused session's socket is not owned by a new deadline.
     */
    public Ssh setExecutionTimeout(long milliseconds) {
        if (milliseconds <= 0) {
            throw new IllegalArgumentException("execution timeout must be positive");
        }
        this.executionTimeout = milliseconds;
        return this;
    }

    private static String remoteTimeoutOptions(int seconds, int killAfter) {
        return killAfter == 0 ? String.valueOf(seconds)
                : String.format("--kill-after=%s %s", killAfter, seconds);
    }

    private int connectionTimeout() throws IOException {
        int configured = getTimeoutInMilli(timeout);
        return executionDeadline == null ? configured : executionDeadline.connectionTimeout(configured);
    }

    public String getHostname() {
        return hostname;
    }

    public Ssh setHostname(String hostname) {
        this.hostname = hostname;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public Ssh setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public Ssh setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public Ssh setPassword(String password) {
        this.password = password;
        return this;
    }

    public int getPort() {
        return port;
    }

    public Ssh setPort(int port) {
        this.port = port;
        return this;
    }

    public boolean isSuppressException() {
        return suppressException;
    }

    public Ssh setSuppressException(boolean suppressException) {
        this.suppressException = suppressException;
        return this;
    }

    /**
       make sure user has permissions or use sudoCommand
    */
    @Deprecated
    public Ssh command(String...cmds) {
        for (String cmd : cmds) {
            commands.add(createCommand(cmd));
        }
        return this;
    }

    public Ssh sudoCommand(String...cmds) {
        for (String cmd : cmds) {
            commands.add(createCommand(SshCmdHelper.wrapSudoCmd(cmd, username, password)));
        }
        return this;
    }

    private interface CommandBuilder {
        String build(int channelTimeout) throws IOException;
    }

    private SshRunner createCommand(final String cmdWithoutPrefix) {
        return createCommand(cmdWithoutPrefix, null);
    }

    private SshRunner createCommand(final String cmdWithoutPrefix, final CommandBuilder builder) {
       final String cmd = language + cmdWithoutPrefix;
       return new SshRunner() {
           @Override
           public SshResult run() {
               SshResult ret = new SshResult();
               String cmdWithoutPassword = getCommandWithoutPassword();
               ret.setCommandToExecute(cmdWithoutPassword);

               try {
                   ChannelExec channel = null;
                   SshExecutionDeadline.PhaseTimeout setupDeadline = null;
                   try {
                       channel = (ChannelExec) session.openChannel("exec");
                       channel.setPty(true);
                       int channelTimeout = connectionTimeout();
                       String executionCommand = cmd;
                       if (executionDeadline != null && builder != null) {
                           channelTimeout = Math.min(MAX_CHANNEL_SETUP_TIMEOUT_MILLIS, channelTimeout);
                           setupDeadline = executionDeadline.watchPhase(
                                   (long) CHANNEL_SETUP_REQUEST_COUNT * channelTimeout);
                           executionCommand = language + builder.build(channelTimeout);
                       }
                       ret.setCommandToExecute(SshCmdHelper.removeSensitiveInfoFromCmd(executionCommand));
                       channel.setCommand(executionCommand);
                       if (logger.isTraceEnabled()) {
                           logger.trace(String.format("[start SSH] %s", cmdWithoutPassword));
                       }

                       try (InputStream ins = channel.getInputStream();
                            InputStream errs = channel.getErrStream()) {
                           if (executionDeadline != null) {
                               executionDeadline.remainingMillis();
                           }
                           channel.connect(channelTimeout);
                           if (setupDeadline != null) {
                               setupDeadline.close();
                           }

                           if (executionDeadline == null) {
                               ret.setStdout(IOUtils.toString(ins, Charsets.UTF_8));
                               ret.setStderr(IOUtils.toString(errs, Charsets.UTF_8));
                           } else {
                               executionDeadline.readOutput(channel, ins, errs, ret);
                           }
                           ret.setReturnCode(channel.getExitStatus());
                           if (logger.isTraceEnabled()) {
                               logger.trace(String.format("[end SSH] %s, return code: %d", cmdWithoutPassword, ret.getReturnCode()));
                           }
                       }
                   } finally {
                       if (setupDeadline != null) {
                           setupDeadline.close();
                       }
                       if (channel != null) {
                           channel.disconnect();
                       }
                   }
               } catch (Exception e) {
                   if (e instanceof IOException) {
                       ret.setSshFailure(true);
                   }

                   StringBuilder sb = new StringBuilder(String.format("exec ssh command: %s, exception\n", cmdWithoutPassword));
                   sb.append(String.format("[host:%s, port:%s, user:%s, timeout:%s]\n", hostname, port, username, timeout));
                   if (!suppressException) {
                       logger.warn(sb.toString(), e);
                   }
                   ret.setExitErrorMessage(e.getMessage());
                   ret.setReturnCode(1);
               }

               return ret;
           }

           @Override
           public String getCommand() {
               return cmd;
           }

           @Override
           public String getCommandWithoutPassword() {
               return SshCmdHelper.removeSensitiveInfoFromCmd(cmd);
           }
       };
    }

    public Ssh scpUpload(final String local, final String remote) {
        commands.add(createScpCommand(local, remote, false));
        return this;
    }

    public Ssh scpDownload(final String remote, final String local) {
        commands.add(createScpCommand(remote, local, true));
        return this;
    }

    private SshRunner createScpCommand(final String src, final String dst, boolean download) {
        return new SshRunner() {
            @Override
            public SshResult run() {
                SshResult ret = new SshResult();
                String cmd = getCommand();
                ret.setCommandToExecute(cmd);

                try {
                    ChannelSftp channel = null;
                    try {
                        channel = (ChannelSftp) session.openChannel("sftp");
                        channel.connect(getTimeoutInMilli(timeout));

                        if (download) {
                            channel.get(src, dst);
                        } else {
                            channel.put(src, dst);
                        }
                        ret.setReturnCode(0);
                    } finally {
                        if (channel != null) {
                            channel.disconnect();
                        }
                    }
                } catch (JSchException | SftpException e) {
                    if (!suppressException) {
                        logger.warn(String.format("[SCP failed]: %s", cmd), e);
                    }
                    ret.setSshFailure(true);
                    ret.setReturnCode(1);
                    ret.setExitErrorMessage(e.getMessage());
                }

                return ret;
            }

            @Override
            public String getCommand() {
                String target = SshShell.formatScpTarget(username, hostname);
                if (download) {
                    return String.format("scp -P %d %s:%s %s", port, target, src, dst);
                } else {
                    return String.format("scp -P %d %s %s:%s", port, src, target, dst);
                }
            }

            @Override
            public String getCommandWithoutPassword() {
                return getCommand();
            }
        };
    }

    public Ssh checkTool(String...toolNames) {
        String tool = StringUtils.join(Arrays.asList(toolNames), " ");
        String cmdstr = s("EXIT (){ echo \"$1\"; exit 1;}; cmds=\"{0}\"; for cmd in $cmds; do which $cmd >/dev/null 2>&1 || EXIT \"Not find command: $cmd\";  done").format(tool);
        return command(cmdstr);
    }

    public Ssh shell(String script) {
        DebugUtils.Assert(this.script==null, "every Ssh object can only specify one script");
        this.script = new ScriptRunner(script);
        return this;
    }

    public Ssh script(String scriptName, String parameters, Map token) {
        DebugUtils.Assert(script==null, "every Ssh object can only specify one script");
        script = new ScriptRunner(scriptName, parameters, token);
        return this;
    }

    public Ssh script(String scriptName, Map tokens) {
        return script(scriptName, null, tokens);
    }

    public Ssh script(String scriptName, String parameters) {
        return script(scriptName, parameters, null);
    }

    public Ssh script(String scriptName) {
        return script(scriptName, null, null);
    }

    private static int getTimeoutInMilli(int seconds) {
        if (seconds <= 0) {
            return 0;
        }

        long timeo = TimeUnit.SECONDS.toMillis(seconds);
        if (timeo < Integer.MAX_VALUE) {
            return (int)timeo;
        }

        return Integer.MAX_VALUE;
    }

    private void build() throws IOException {
        if (init) {
            return;
        }

        try {
            JSch jSch = new JSch();
            if (privateKey != null) {
                privateKeyFile = File.createTempFile("zstack", "tmp");
                writeSecretFile(privateKeyFile, privateKey);
                jSch.addIdentity(privateKeyFile.getAbsolutePath());
            }

            session = jSch.getSession(username, hostname, port);
            session.setConfig("StrictHostKeyChecking", "no");
            session.setPassword(password);
            session.setConfig("PreferredAuthentications", privateKey == null ? "password" : "publickey,password");
            session.setConfig("server_host_key", session.getConfig("server_host_key") + ",ssh-rsa,ssh-dss");
            session.setConfig("PubkeyAcceptedKeyTypes", session.getConfig("PubkeyAcceptedKeyTypes") + ",ssh-rsa,ssh-dss");
            session.setTimeout(getTimeoutInMilli(socketTimeout)/2);
            if (executionDeadline != null) {
                session.setSocketFactory(executionDeadline);
            }
            session.connect(connectionTimeout());
        } catch (JSchException ex) {
            throw new IOException(ex);
        }

        init = true;
    }

    public void close() {
        if (closed) {
            return;
        }

        closed = true;

        if (privateKeyFile != null) {
            if (!privateKeyFile.delete()) {
                logger.warn("delete file failed: " + privateKeyFile.getAbsolutePath());
            }
        }

        if (script != null) {
            script.cleanup();
        }

        if (session != null) {
            session.disconnect();
        }
    }

    public SshResult run() {
        if (closed) {
            throw new SshException("this Ssh instance has been closed, you can not call run() after close()");
        }
        if (executionTimeout > 0 && executed) {
            throw new SshException("use a new Ssh instance for each run() with an execution timeout");
        }
        executed = true;
        executionDeadline = executionTimeout > 0
                ? new SshExecutionDeadline(executionTimeout, getTimeoutInMilli(timeout)) : null;
        if (executionDeadline != null) {
            executionDeadline.watch();
        }
        try {
            SshResult result = runInternal();
            if (executionDeadline != null && !executionDeadline.finish()) {
                result.setSshFailure(true);
                result.setReturnCode(1);
                result.setExitErrorMessage("SSH execution timed out (total budget " + executionTimeout + " ms)");
            }
            return result;
        } finally {
            if (executionDeadline != null) {
                executionDeadline.finish();
            }
        }
    }

    private SshResult runInternal() {
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            build();
            if (commands.isEmpty() && script == null) {
                throw new IllegalArgumentException("no command or scp command or script specified");
            }

            if (!commands.isEmpty() && script != null) {
                throw new IllegalArgumentException("you cannot use script with command or scp");
            }

            if (privateKey == null && password == null) {
                throw new IllegalArgumentException("no password and private key specified");
            }

            if (username == null) {
                throw new IllegalArgumentException("no username specified");
            }

            if (hostname == null) {
                throw new IllegalArgumentException("no hostname specified");
            }

            if (script != null) {
                if (logger.isTraceEnabled()) {
                    logger.trace(String.format("run script remotely[ip: %s, port: %s]:\n%s\n", hostname, port, script.scriptContent));
                }
                return script.run();
            } else {
                SshResult ret = null;
                for (SshRunner runner : commands) {
                    ret = runner.run();
                    if (ret.getReturnCode() != 0) {
                        return ret;
                    }
                }

                return ret;
            }

        } catch (IOException e) {
            StringBuilder sb = new StringBuilder("ssh exception\n");
            sb.append(String.format("[host:%s, port:%s, user:%s, timeout:%s]\n", hostname, port, username, timeout));
            if (!suppressException) {
                logger.warn(sb.toString(), e);
            }
            SshResult ret = new SshResult();
            ret.setSshFailure(true);
            ret.setExitErrorMessage(e.getMessage());
            ret.setReturnCode(1);
            return ret;
        } finally {
            watch.stop();
            if (logger.isTraceEnabled()) {
                if (script != null) {
                    logger.trace(String.format("execute script[%s], cost time:%s", script.scriptName, watch.getTime()));
                } else {
                    String cmd = StringUtils.join(CollectionUtils.transformToList(commands, new Function<String, SshRunner>() {
                        @Override
                        public String call(SshRunner arg) {
                            return arg.getCommandWithoutPassword();
                        }
                    }), ",");
                    String info = s(
                            "\nssh execution[host: {0}, port:{1}]\n",
                            "command: {2}\n",
                            "cost time: {3}ms\n"
                    ).format(hostname, port, cmd, watch.getTime());
                    logger.trace(info);
                }
            }
        }
    }

    public int getTimeout() {
        return timeout;
    }

    public Ssh setTimeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    public int getSocketTimeout() {
        return socketTimeout;
    }

    public Ssh setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
        return this;
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public Ssh reset() {
        commands = new ArrayList<SshRunner>();
        return this;
    }

    public SshResult runAndClose() {
        try {
            return run();
        } finally {
            close();
        }
    }

    public void runErrorByExceptionAndClose() {
        runAndClose().raiseExceptionIfFailed();
    }

    @Deprecated
    public void runErrorByException() {
        SshResult ret = run();
        try {
            ret.raiseExceptionIfFailed();
        } catch (SshException e) {
            close();
            throw e;
        }
    }

    private static void writeSecretFile(File file, String content) throws IOException {
        writeFile(file, new byte[0]);
        setFilePosixPermissions(file, "rw-------");
        writeFile(file, content);
    }
}
