package org.zstack.storage.ceph;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.zstack.header.exception.CloudRuntimeException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * Created by frank on 7/27/2015.
 */
public class MonUri {
    private String hostname;
    private int monPort = 6789;
    private String sshUsername;
    private String sshPassword;

    public MonUri(String url) {
        try {
            URI uri = new URI(url);
            hostname = uri.getHost();
            String[] ssh = uri.getUserInfo().split(":");
            sshUsername = ssh[0];
            sshPassword = ssh[1];

            List<NameValuePair> params = URLEncodedUtils.parse(uri, "UTF-8");
            for (NameValuePair p : params) {
                if (p.getName().equals("monPort")) {
                    monPort = Integer.valueOf(p.getValue());
                }
            }
        } catch (URISyntaxException e) {
            throw new CloudRuntimeException(e);
        }
    }

    public int getMonPort() {
        return monPort;
    }

    public void setMonPort(int monPort) {
        this.monPort = monPort;
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
