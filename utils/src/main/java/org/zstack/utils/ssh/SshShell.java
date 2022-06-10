package org.zstack.utils.ssh;

import org.apache.commons.io.FileUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.ShellResult;
import org.zstack.utils.ShellUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.zstack.utils.StringDSL.ln;
import static org.zstack.utils.StringDSL.s;

/**
 * Created by frank on 12/5/2015.
 */
public class SshShell {
    private static final CLogger logger = Utils.getLogger(SshShell.class);

    private String hostname;
    private String username;
    private String password;
    private String privateKeyFile;
    private int port = 22;
    private Boolean withSudo = true;

    private void checkParams() {
        DebugUtils.Assert(hostname != null && !hostname.trim().equals(""), "hostname cannot be null");
        DebugUtils.Assert(username != null && !username.trim().equals(""), "username cannot be null");
        DebugUtils.Assert(password != null || privateKeyFile != null, "password and privateKeyFile must have at least one set");
    }

    public SshResult runCommand(String cmd) {
        checkParams();
        String ssh;
        File tempPasswordFile = null;

        try {
            if (privateKeyFile != null) {
                ssh = String.format("ssh -q -i %s -o UserKnownHostsFile=/dev/null -o PasswordAuthentication=no -o StrictHostKeyChecking=no -p %s %s@%s '%s'",
                        privateKeyFile, port, username, hostname, cmd);
            } else {
                tempPasswordFile = File.createTempFile("zstack", "tmp");
                FileUtils.writeStringToFile(tempPasswordFile, password);
                ssh = String.format("sshpass -f%s ssh -q -o UserKnownHostsFile=/dev/null -o PubkeyAuthentication=no -o StrictHostKeyChecking=no -p %s %s@%s '%s'",
                        tempPasswordFile.getAbsolutePath(), port, username, hostname, cmd);
            }

            if (logger.isTraceEnabled()) {
                logger.trace(String.format("[ssh shell]: %s", ssh));
            }

            ShellResult ret = ShellUtils.runAndReturn(ssh, withSudo);
            SshResult sret = new SshResult();
            sret.setCommandToExecute(cmd);
            sret.setReturnCode(ret.getRetCode());
            sret.setStderr(ret.getStderr());
            sret.setStdout(ret.getStdout());
            if (sret.getReturnCode() == 255 && privateKeyFile != null) {
                sret.setSshFailure(true);
            } else if (sret.getReturnCode() == 5 && privateKeyFile == null) {
                sret.setSshFailure(true);
            }
            return sret;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (tempPasswordFile != null) {
                tempPasswordFile.delete();
            }
        }
    }

    public SshResult runScript(String script) {
        checkParams();
        String ssh;
        File tempPasswordFile = null;
        try {
            if (privateKeyFile != null) {
                ssh = ln(
                        "ssh -q -i {0} -o UserKnownHostsFile=/dev/null -o PasswordAuthentication=no -o StrictHostKeyChecking=no -p {1} -T {2}@{3} << 'EOF'",
                        "s=`mktemp`",
                        "cat << 'EOT' > $s",
                        "{4}",
                        "EOT",
                        "bash $s",
                        "ret=$?",
                        "rm -f $s",
                        "exit $ret",
                        "EOF"
                ).format(privateKeyFile, port, username, hostname, script);
            } else {
                tempPasswordFile = File.createTempFile("zstack", "tmp");
                FileUtils.writeStringToFile(tempPasswordFile, password);
                ssh = ln(
                        "sshpass -f{0} ssh -q -o UserKnownHostsFile=/dev/null -o PubkeyAuthentication=no -o StrictHostKeyChecking=no -p {1} -T {2}@{3} << 'EOF'",
                        "s=`mktemp`",
                        "cat << 'EOT' > $s",
                        "{4}",
                        "EOT",
                        "bash $s",
                        "ret=$?",
                        "rm -f $s",
                        "exit $ret",
                        "EOF"
                ).format(tempPasswordFile.getAbsolutePath(), port, username, hostname, script);
            }

            if (logger.isTraceEnabled()) {
                logger.trace(String.format("[ssh shell]: %s", ssh));
            }

            ShellResult ret = ShellUtils.runAndReturn(ssh);
            SshResult sret = new SshResult();
            sret.setCommandToExecute(script);
            sret.setReturnCode(ret.getRetCode());
            sret.setStderr(ret.getStderr());
            sret.setStdout(ret.getStdout());
            if (sret.getReturnCode() == 255 && privateKeyFile != null) {
                sret.setSshFailure(true);
            } else if (sret.getReturnCode() == 5 && privateKeyFile == null) {
                sret.setSshFailure(true);
            }
            return sret;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (tempPasswordFile != null) {
                tempPasswordFile.delete();
            }
        }
    }

    public SshResult runScriptWithToken(String scriptName, Map token) {
        String scriptPath = PathUtil.findFileOnClassPath(scriptName, true).getAbsolutePath();
        if (token == null) {
            token = new HashMap();
        }
        String contents = null;
        try {
            contents = FileUtils.readFileToString(new File(scriptPath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String script = s(contents).formatByMap(token);
        return runScript(script);
    }


    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPrivateKeyFile() {
        return privateKeyFile;
    }

    public void setPrivateKeyFile(String privateKeyFile) {
        this.privateKeyFile = privateKeyFile;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
    public Boolean getWithSudo() {
        return withSudo;
    }

    public void setWithSudo(Boolean withSudo) {
        this.withSudo = withSudo;
    }
}