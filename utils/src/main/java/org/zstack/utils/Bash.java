package org.zstack.utils;

import org.apache.commons.io.FileUtils;
import org.zstack.utils.logging.CLogger;

import java.io.File;
import java.io.IOException;

public abstract class Bash {
    private static final CLogger logger = Utils.getLogger(Bash.class);

    protected abstract void scripts();
    protected int lastReturnCode;
    protected String lastStdout;
    protected String lastStderr;
    protected String lastCommand;
    protected boolean SET_E;

    protected class BashBuilder {
        private boolean useSudo;
        private String path;

        public BashBuilder sudo() {
            useSudo = true;
            return this;
        }

        public BashBuilder cwd(String v) {
            path = v;
            return this;
        }

        public int run(String cmd, Object...args) {
            if (args != null) {
                cmd = String.format(cmd, args);
            }

            lastCommand = Utils.maskSensitiveInfo(cmd);
            ShellResult res = ShellUtils.runAndReturn(cmd, path, useSudo);
            lastReturnCode = res.getRetCode();
            lastStdout = res.getStdout();
            lastStderr = res.getStderr();

            if (SET_E) {
                errorOnFailure();
            }

            return lastReturnCode;
        }
    }

    /**
     * @return return true if @content is equal to @filePath's content
     */
    protected boolean compareWithFile(String filePath, String content) {
        File f = new File(filePath);
        if (!f.exists() || f.isDirectory()) {
            return false;
        }
        try {
            String fileContent = FileUtils.readFileToString(f);
            return fileContent.trim().equals(content.trim());
        } catch (IOException e) {
            logger.warn(e.getLocalizedMessage());
            return false;
        }
    }

    protected void writeFile(String filePath, String content) {
        mkdirs(dirname(filePath));

        try {
            FileUtils.writeStringToFile(new File(filePath), content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void mkdirs(String path) {
        File f = new File(path);
        if (!f.exists()) {
            f.mkdirs();
        }
    }

    protected String dirname(String path) {
        return new File(path).getParentFile().getPath();
    }

    protected void copyDir(String src, String dst) {
        try {
            FileUtils.copyDirectory(new File(src), new File(dst));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected BashBuilder bash() {
        return new BashBuilder();
    }

    protected int run(String cmd, Object...args) {
        return run(cmd, true, args);
    }

    protected int run(String cmd, boolean sudo, Object...args) {
        if (args != null) {
            cmd = String.format(cmd, args);
        }

        lastCommand = cmd;
        ShellResult res = ShellUtils.runAndReturn(cmd, sudo);
        lastReturnCode = res.getRetCode();
        lastStdout = res.getStdout();
        lastStderr = res.getStderr();

        if (SET_E) {
            errorOnFailure();
        }

        return lastReturnCode;
    }

    protected void errorOnFailure() {
        if (lastReturnCode != 0) {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("\nshell command[%s] failed", lastCommand));
            sb.append(String.format("\nret code: %s", lastReturnCode));
            sb.append(String.format("\nstderr: %s", lastStderr));
            sb.append(String.format("\nstdout: %s", lastStdout));
            throw new RuntimeException(sb.toString());
        }
    }

    // similar to set -e in shell script
    protected void setE() {
        SET_E = true;
    }

    // similar to set +e in shell script
    protected void unsetE() {
        SET_E = false;
    }

    protected String stdout() {
        return lastStdout;
    }

    protected String stderr() {
        return lastStderr;
    }

    public void execute() {
        scripts();
    }
}
