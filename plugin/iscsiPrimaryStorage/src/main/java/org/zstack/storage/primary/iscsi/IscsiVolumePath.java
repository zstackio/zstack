package org.zstack.storage.primary.iscsi;

import org.zstack.utils.DebugUtils;

/**
 * Created by frank on 4/24/2015.
 */
public class IscsiVolumePath {
    private String target;
    private String hostname;
    private int port = 3260;
    private int lun = 1;
    private String installPath;

    private String fullPath;

    public IscsiVolumePath(String fullPath) {
        this.fullPath = fullPath;
    }

    public IscsiVolumePath() {
    }

    public String assembleIscsiPath() {
        return String.format("iscsi://%s:%s/%s/%s", hostname, port, target, lun);
    }

    public String assemble() {
        DebugUtils.Assert(target!=null, "target cannot be null");
        DebugUtils.Assert(installPath!=null, "installPath cannot be null");
        DebugUtils.Assert(hostname!=null, "hostname cannot be null");
        fullPath =  String.format("iscsi://%s:%s/%s/%s;file://%s", hostname, port, target, lun, installPath);
        return fullPath;
    }

    public IscsiVolumePath disassemble() {
        DebugUtils.Assert(fullPath!=null, "fullPath cannot be null");
        String[] ps = fullPath.split(";");
        String iscsi = ps[0].replaceAll("iscsi://", "");
        String[] is = iscsi.split("/");
        String portal = is[0];
        String[] hh = portal.split(":");

        hostname = hh[0];
        port = Integer.valueOf(hh[1]);
        target = is[1];
        lun = Integer.valueOf(is[2]);

        installPath = ps[1].replaceAll("file://", "");
        return this;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getLun() {
        return lun;
    }

    public void setLun(int lun) {
        this.lun = lun;
    }

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }
}
