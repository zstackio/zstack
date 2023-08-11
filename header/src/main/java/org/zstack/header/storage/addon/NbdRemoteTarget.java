package org.zstack.header.storage.addon;

public class NbdRemoteTarget implements RemoteTarget {
    private String installPath;

    @Override
    public String getInstallPath() {
        return installPath;
    }
}
