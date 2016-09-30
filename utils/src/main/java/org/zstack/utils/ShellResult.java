package org.zstack.utils;

/**
 */
public class ShellResult {
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

    public void raiseExceptionIfFail() {
        raiseExceptionIfFail(0);
    }

    public void raiseExceptionIfFail(int expectedRetCode) {
        if (retCode != expectedRetCode) {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("\nshell command[%s] failed", command));
            sb.append(String.format("\nret code: %s", retCode));
            sb.append(String.format("\nstderr: %s", stderr));
            sb.append(String.format("\nstdout: %s", stdout));
            throw new RuntimeException(sb.toString());
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
