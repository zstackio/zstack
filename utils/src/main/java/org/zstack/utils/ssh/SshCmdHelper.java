package org.zstack.utils.ssh;

public class SshCmdHelper {
    public static String wrapSudoCmd(String cmd, String username, String password) {
        if ("root".equals(username)) {
            return cmd;
        } else if (password == null) {
            return String.format("sudo %s", cmd);
        } else {
            return String.format("echo %s | sudo -S %s", shellQuote(password), cmd);
        }
    }

    public static String removeSensitiveInfoFromCmd(String cmd) {
        return cmd.replaceAll("echo .*?\\s*\\|\\s*sudo -S", "echo ****** | sudo -S");
    }

    public static String shellQuote(String s) {
        return "'" + s.replace("'", "'\\''") + "'";
    }
}
