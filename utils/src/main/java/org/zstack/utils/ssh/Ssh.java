package org.zstack.utils.ssh;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.ConnectionException;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.transport.verification.HostKeyVerifier;
import org.apache.commons.io.FileUtils;
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
import java.security.PublicKey;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;
import static org.zstack.utils.StringDSL.ln;
import static org.zstack.utils.StringDSL.s;

/**
 */
public class Ssh {
    private static final CLogger logger = Utils.getLogger(Ssh.class);

    private String hostname;
    private String username;
    private String privateKey;
    private String password;
    private int port = 22;
    private int timeout = Integer.MAX_VALUE;
    private List<SshRunner> commands = new ArrayList<SshRunner>();
    private SSHClient ssh;
    private File privateKeyFile;
    private boolean closed = false;
    private boolean suppressException = false;
    private ScriptRunner script;

    private boolean init = false;


    private interface SshRunner {
        SshResult run();
        String getCommand();
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

                String remoteScript = ln(
                        "/bin/bash << EOF",
                        "cat << EOF1 > {remotePath}",
                        "{scriptContent}",
                        "EOF1",
                        "/bin/bash {remotePath} {parameters} 1>{stdout} 2>{stderr}",
                        "ret=$?",
                        "test -f {stdout} && cat {stdout}",
                        "test -f {stderr} && cat {stderr} 1>&2",
                        "rm -f {remotePath}",
                        "rm -f {stdout}",
                        "rm -f {stderr}",
                        "exit $ret",
                        "EOF"
                ).formatByMap(map(e("remotePath", String.format("/tmp/%s", UUID.randomUUID().toString())),
                        e("scriptContent", scriptContent),
                        e("parameters", parameters),
                        e("stdout", String.format("/tmp/%s", UUID.randomUUID().toString())),
                        e("stderr", String.format("/tmp/%s", UUID.randomUUID().toString()))
                ));

                scriptCommand = createCommand(remoteScript);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        ScriptRunner(String script) {
            String remoteScript = ln(
                    "/bin/bash << EOF",
                    "cat << EOF1 > {remotePath}",
                    "{scriptContent}",
                    "EOF1",
                    "/bin/bash {remotePath} 1>{stdout} 2>{stderr}",
                    "ret=$?",
                    "test -f {stdout} && cat {stdout}",
                    "test -f {stderr} && cat {stderr} 1>&2",
                    "rm -f {remotePath}",
                    "rm -f {stdout}",
                    "rm -f {stderr}",
                    "exit $ret",
                    "EOF"
            ).formatByMap(map(e("remotePath", String.format("/tmp/%s", UUID.randomUUID().toString())),
                    e("scriptContent", script),
                    e("stdout", String.format("/tmp/%s", UUID.randomUUID().toString())),
                    e("stderr", String.format("/tmp/%s", UUID.randomUUID().toString()))
            ));
            scriptCommand = createCommand(remoteScript);
        }

        SshResult run() {
            return scriptCommand.run();
        }

        void cleanup() {
            if (scriptFile != null) {
                scriptFile.delete();
            }
        }
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

    public Ssh command(String...cmds) {
        for (String cmd : cmds) {
            commands.add(createCommand(cmd));
        }
        return this;
    }

    private SshRunner createCommand(final String cmd) {
       return new SshRunner() {
           @Override
           public SshResult run() {
               SshResult ret = new SshResult();
               ret.setCommandToExecute(cmd);

               Session.Command sshCmd = null;
               try {
                   Session session = null;
                   try {
                       session = ssh.startSession();
                       if (logger.isTraceEnabled()) {
                           logger.trace(String.format("[start SSH] %s", cmd));
                       }
                       sshCmd = session.exec(cmd);
                       sshCmd.join(timeout, TimeUnit.SECONDS);
                       String output = IOUtils.readFully(sshCmd.getInputStream()).toString();
                       String stderr = IOUtils.readFully(sshCmd.getErrorStream()).toString();
                       ret.setReturnCode(sshCmd.getExitStatus());
                       ret.setStderr(stderr);
                       ret.setStdout(output);
                       if (logger.isTraceEnabled()) {
                           logger.trace(String.format("[end SSH] %s", cmd));
                       }
                   } finally {
                       if (session != null) {
                           session.close();
                       }
                   }
               } catch (Exception e) {
                   if (e instanceof ConnectionException || e instanceof IOException || e instanceof TransportException) {
                       ret.setSshFailure(true);
                   }

                   StringBuilder sb = new StringBuilder(String.format("exec ssh command: %s, exception\n", cmd));
                   sb.append(String.format("[host:%s, port:%s, user:%s, timeout:%s]\n", hostname, port, username, timeout));
                   if (!suppressException) {
                       logger.warn(sb.toString(), e);
                   }
                   ret.setExitErrorMessage(e.getMessage());
                   ret.setReturnCode(1);
               } finally {
                   if (sshCmd != null) {
                       try {
                           sshCmd.close();
                       } catch (Exception e) {
                           logger.warn(String.format("failed close ssh channel for command[%s, host:%s, port:%s]", cmd, hostname, port), e);
                       }
                   }
               }

               return ret;
           }

           @Override
           public String getCommand() {
               return cmd;
           }
       };
    }

    public Ssh scp(final String src, final String dst) {
        commands.add(createScpCommand(src, dst));
        return this;
    }

    private SshRunner createScpCommand(final String src, final String dst) {
        return new SshRunner() {
            @Override
            public SshResult run() {
                SshResult ret = new SshResult();
                String cmd = getCommand();
                ret.setCommandToExecute(cmd);

                try {
                    ssh.newSCPFileTransfer().upload(src, dst);
                    if (logger.isTraceEnabled()) {
                        logger.trace(String.format("[SCP done]: %s", cmd));
                    }
                    ret.setReturnCode(0);
                } catch (IOException e) {
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
                return String.format("scp -P %d %s %s@%s:%s", port, src, username, hostname, dst);
            }
        };
    }

    public Ssh checkTool(String...toolNames) {
        String tool = StringUtils.join(Arrays.asList(toolNames), " ");
        String cmdstr = s("EXIT (){ echo \"$1\"; exit 1;}; cmds=\"{0}\"; for cmd in $cmds; do which $cmd >/dev/null 2>&1 || EXIT \"Not find command: $cmd\";  done").format(tool);
        return command(cmdstr);
    }

    public Ssh shell(String script) {
        DebugUtils.Assert(this.script==null, String.format("every Ssh object can only specify one script"));
        this.script = new ScriptRunner(script);
        return this;
    }

    public Ssh script(String scriptName, String parameters, Map token) {
        DebugUtils.Assert(script==null, String.format("every Ssh object can only specify one script"));
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

    private void build() throws IOException {
        if (init) {
            return;
        }

        ssh = new SSHClient();
        ssh.addHostKeyVerifier(new HostKeyVerifier() {
            @Override
            public boolean verify(String arg0, int arg1, PublicKey arg2) {
                return true;
            }
        });
        ssh.connect(hostname, port);
        if (privateKey != null) {
            privateKeyFile = File.createTempFile("zstack", "tmp");
            FileUtils.writeStringToFile(privateKeyFile, privateKey);
            ssh.authPublickey(username, privateKeyFile.getAbsolutePath());
        } else {
            ssh.authPassword(username, password);
        }

        init = true;
    }

    public void close() {
        if (closed) {
            return;
        }

        closed = true;

        try {
            ssh.disconnect();

            if (privateKeyFile != null) {
                privateKeyFile.delete();
            }
            if (script != null) {
                script.cleanup();
            }
        } catch (IOException e) {
            StringBuilder sb = new StringBuilder(String.format("failed to close connection"));
            sb.append(String.format("[host:%s, port:%s, user:%s, timeout:%s]\n", hostname, port, username, timeout));
            logger.warn(sb.toString(), e);
        }
    }

    public SshResult run() {
        if (closed) {
            throw new SshException("this Ssh instance has been closed, you can not call run() after close()");
        }

        StopWatch watch = new StopWatch();
        watch.start();
        try {
            build();
            if (commands.isEmpty() && script == null) {
                throw new IllegalArgumentException(String.format("no command or scp command or script specified"));
            }

            if (!commands.isEmpty() && script != null) {
                throw new IllegalArgumentException(String.format("you cannot use script with command or scp"));
            }

            if (privateKey == null && password == null) {
                throw new IllegalArgumentException(String.format("no password and private key specified"));
            }

            if (username == null) {
                throw new IllegalArgumentException(String.format("no username specified"));
            }

            if (hostname == null) {
                throw new IllegalArgumentException(String.format("no hostname specified"));
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
            StringBuilder sb = new StringBuilder(String.format("ssh exception\n"));
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
                            return arg.getCommand();
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

    public Ssh reset() {
        commands = new ArrayList<SshRunner>();
        return this;
    }

    public SshResult runAndClose() {
        SshResult ret = run();
        close();
        return ret;
    }

    public void runErrorByExceptionAndClose() {
        SshResult ret = run();
        close();
        ret.raiseExceptionIfFailed();
    }

    public void runErrorByException() {
        SshResult ret = run();
        try {
            ret.raiseExceptionIfFailed();
        } catch (SshException e) {
            close();
            throw e;
        }
    }
}
