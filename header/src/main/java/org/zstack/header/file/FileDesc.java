package org.zstack.header.file;

public class FileDesc {
    String path;
    String md5;
    long lastModified;

    public FileDesc(String path, String md5, long lastModified) {
        this.path = path;
        this.md5 = md5;
        this.lastModified = lastModified;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }
}
