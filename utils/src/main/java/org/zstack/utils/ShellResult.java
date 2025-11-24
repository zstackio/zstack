package org.zstack.utils;

import org.zstack.utils.opaque.OpaqueScripts;

import java.util.Map;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;
import static org.zstack.utils.opaque.OpaqueConstants.OPAQUE_KEY_BASH_CMD;
import static org.zstack.utils.opaque.OpaqueConstants.OPAQUE_KEY_BASH_CODE;
import static org.zstack.utils.opaque.OpaqueConstants.OPAQUE_KEY_BASH_ERROR;
import static org.zstack.utils.opaque.OpaqueConstants.OPAQUE_KEY_BASH_OUTPUT;

/**
 */
public class ShellResult implements OpaqueScripts {
    private int retCode;
    private String stderr;
    private String stdout;
    private String command;

    public int getRetCode() {
        return retCode;
    }

    public void setRetCode(int retCode) {
        this.retCode = retCode;
    }

    public String getStderr() {
        return stderr;
    }

    public void setStderr(String stderr) {
        this.stderr = stderr;
    }

    public String getStdout() {
        return stdout;
    }

    public void setStdout(String stdout) {
        this.stdout = stdout;
    }

    public String getExecutionLog() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("\nshell command[%s]", command));
        sb.append(String.format("\nret code: %s", retCode));
        sb.append(String.format("\nstderr: %s", stderr));
        sb.append(String.format("\nstdout: %s", stdout));
        return sb.toString();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> opaqueScripts() {
        return map(
            e(OPAQUE_KEY_BASH_CMD, command),
            e(OPAQUE_KEY_BASH_CODE, retCode),
            e(OPAQUE_KEY_BASH_OUTPUT, stdout),
            e(OPAQUE_KEY_BASH_ERROR, stderr)
        );
    }

    public void raiseExceptionIfFail() {
        raiseExceptionIfFail(0);
    }

    public void raiseExceptionIfFail(int expectedRetCode) {
        if (retCode != expectedRetCode) {
            if (stderr != null && stderr.contains("Account expired")) {
                throw new ShellUtils.ShellException(String.format("local account '%s' has expired", System.getProperty("user.name")));
            }

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("\nshell command[%s] failed", command));
            sb.append(String.format("\nret code: %s", retCode));
            sb.append(String.format("\nstderr: %s", stderr));
            sb.append(String.format("\nstdout: %s", stdout));
            throw new ShellUtils.ShellException(sb.toString());
        }
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public boolean isReturnCode(int code) {
        return retCode == code;
    }
}
