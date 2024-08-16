package org.zstack.header.storage.addon;

public class NbdRemoteTarget extends BlockRemoteTarget {
    private String installPath;

    @Override
    public String getInstallPath() {
        return installPath;
    }

    @Override
    public String getResourceURI() {
        return null;
    }
}
