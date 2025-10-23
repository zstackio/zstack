package org.zstack.header.storage.addon;

import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.net.URI;

public class IscsiRemoteTarget extends BlockRemoteTarget {
    private final static CLogger logger = Utils.getLogger(IscsiRemoteTarget.class);
    private String transport = "tcp";

    private String iqn;

    private String ip;

    private int port;

    private String diskId;

    private String diskIdType;

    public String getDiskIdType() {
        return diskIdType;
    }

    public void setDiskIdType(String diskIdType) {
        this.diskIdType = diskIdType;
    }

    public String getTransport() {
        return transport;
    }

    public void setTransport(String transport) {
        this.transport = transport;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String getResourceURI() {
        return String.format("iscsi://%s:%s/%s/%s_%s", ip, port, iqn, diskIdType, diskId);
    }

    public String getDiskId() {
        return diskId;
    }

    public void setDiskId(String diskId) {
        this.diskId = diskId;
    }

    public String getIqn() {
        return iqn;
    }

    public void setIqn(String iqn) {
        this.iqn = iqn;
    }

    public enum DiskIdType {
        wwn,
        serial
    }

    public static IscsiRemoteTarget fromUri(String uriString) {
        try {
            URI uri = URI.create(uriString);

            if (!"iscsi".equalsIgnoreCase(uri.getScheme())) {
                logger.info("Invalid URI scheme. Expected 'iscsi', got: " + uri.getScheme());
                return null;
            }

            IscsiRemoteTarget target = new IscsiRemoteTarget();
            String authority = uri.getAuthority();
            if (authority == null || authority.isEmpty()) {
                logger.info("Invalid URI authority: " + uri.getAuthority());
                return null;
            }
            String[] serverHostNames = authority.split(":")[0].split(",");
            target.setIp(serverHostNames[0]);
            target.setPort(uri.getPort() == -1 ? 3260 : uri.getPort());

            // parse: /{iqn}/{diskIdType}_{diskId}
            String path = uri.getPath();
            if (path != null && path.startsWith("/")) {
                String[] pathParts = path.substring(1).split("/");
                if (pathParts.length >= 2) {
                    target.setIqn(pathParts[0]);
                    String[] diskParts = pathParts[1].split("_", 2);
                    if (diskParts.length == 2) {
                        target.setDiskIdType(diskParts[0]);
                        target.setDiskId(diskParts[1]);
                    } else {
                        logger.info("Invalid diskId format in URI path: " + pathParts[1]);
                        return null;
                    }
                } else {
                    logger.info("Invalid URI path format: " + path);
                    return null;
                }
            }

            return target;
        } catch (Exception e) {
            logger.error("Failed to parse URI: " + uriString, e);
            return null;
        }
    }
}
