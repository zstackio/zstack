package org.zstack.header.vm.additions;

/**
 * @author Zdream
 * @date 2026-03-28
 * @since 0.0.5
 */
public class VmHostFileBackupJob {
    private String srcPath;
    private String destPath;
    private String type;

    public String getSrcPath() {
        return srcPath;
    }

    public void setSrcPath(String srcPath) {
        this.srcPath = srcPath;
    }

    public String getDestPath() {
        return destPath;
    }

    public void setDestPath(String destPath) {
        this.destPath = destPath;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
