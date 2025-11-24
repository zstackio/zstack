package org.zstack.utils.ssh;

import org.zstack.utils.opaque.OpaqueScripts;

import java.io.Serializable;
import java.util.Map;

import static org.zstack.utils.CollectionDSL.map;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.opaque.OpaqueConstants.OPAQUE_KEY_SSH_CMD;
import static org.zstack.utils.opaque.OpaqueConstants.OPAQUE_KEY_SSH_CODE;
import static org.zstack.utils.opaque.OpaqueConstants.OPAQUE_KEY_SSH_ERROR;
import static org.zstack.utils.opaque.OpaqueConstants.OPAQUE_KEY_SSH_OUTPUT;

public class SshResult implements OpaqueScripts, Serializable {
	private int returnCode;
	private String stdout;
	private String stderr;
	private String exitErrorMessage;
	private String commandToExecute;
    private boolean isSshFailure;

    public void raiseExceptionIfFailed(int retCode) {
        if (retCode != returnCode) {
            throw new SshException(toString()).withResult(this);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("\nssh command failed");
        sb.append(String.format("\ncommand: %s", commandToExecute));
        sb.append(String.format("\nreturn code: %s", returnCode));
        sb.append(String.format("\nstdout: %s", stdout));
        sb.append(String.format("\nstderr: %s", stderr));
        sb.append(String.format("\nexitErrorMessage: %s", exitErrorMessage));
		sb.append("\nThe problem may be caused by an incorrect user name or password or SSH port");
        return sb.toString();
    }

    public boolean isSshFailure() {
        return isSshFailure;
    }

    public void setSshFailure(boolean isSshFailure) {
        this.isSshFailure = isSshFailure;
    }

    public void raiseExceptionIfFailed() {
        raiseExceptionIfFailed(0);
    }

    public int getReturnCode() {
		return returnCode;
	}
	public void setReturnCode(int returnCode) {
		this.returnCode = returnCode;
	}
	public String getStdout() {
		return stdout;
	}
	public void setStdout(String stdout) {
		this.stdout = stdout;
	}
	public String getStderr() {
		return stderr;
	}
	public void setStderr(String stderr) {
		this.stderr = stderr;
	}
	public String getExitErrorMessage() {
		return exitErrorMessage;
	}
	public void setExitErrorMessage(String exitErrorMessage) {
		this.exitErrorMessage = exitErrorMessage;
	}
	public String getCommandToExecute() {
		return commandToExecute;
	}
	public void setCommandToExecute(String commandToExecute) {
		this.commandToExecute = commandToExecute;
	}

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> opaqueScripts() {
        return map(
            e(OPAQUE_KEY_SSH_CMD, commandToExecute),
            e(OPAQUE_KEY_SSH_CODE, returnCode),
            e(OPAQUE_KEY_SSH_OUTPUT, stdout),
            e(OPAQUE_KEY_SSH_ERROR, stderr)
        );
    }
}
