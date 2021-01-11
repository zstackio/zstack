package org.zstack.compute.cluster;

/**
 * Created by MaJin on 2021/1/11.
 */
public enum ArchitectureType {
    aarch64,
    x86_64,
    mips64el;

    private static String defaultArch = System.getProperty("os.arch").equals("amd64") ?
            "x86_64" : System.getProperty("os.arch");

    public static String defaultArch() {
        return defaultArch;
    }
}
