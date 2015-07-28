package org.zstack.storage.ceph;

import org.zstack.header.exception.CloudRuntimeException;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by frank on 7/27/2015.
 */
public class MonUri {
    private String hostname;
    private String sshUsername;
    private String sshPassword;

    public MonUri(String url) {
        try {
            URI uri = new URI(url);
            hostname = uri.getHost();
            String[] ssh = uri.getRawAuthority().split(":");
            sshUsername = ssh[0];
            sshPassword = ssh[1];
        } catch (URISyntaxException e) {
            throw new CloudRuntimeException(e);
        }
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getSshUsername() {
        return sshUsername;
    }

    public void setSshUsername(String sshUsername) {
        this.sshUsername = sshUsername;
    }

    public String getSshPassword() {
        return sshPassword;
    }

    public void setSshPassword(String sshPassword) {
        this.sshPassword = sshPassword;
    }
}
