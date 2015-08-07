package org.zstack.storage.ceph;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by frank on 7/27/2015.
 */
public class MonUri {
    private String hostname;
    private int monPort = 6789;
    private String sshUsername;
    private String sshPassword;
    private int sshPort = 22;

    private static List<String> allowedQueryParameter;
    static {
        allowedQueryParameter = list("monPort");
    }

    public static void checkQuery(URI uri) {
        List<NameValuePair> params = URLEncodedUtils.parse(uri, "UTF-8");
        for (NameValuePair p : params) {
            if (!allowedQueryParameter.contains(p.getName())) {
                throw new CloudRuntimeException(String.format("unknown parameter[%s]", p.getName()));
            }
        }
    }

    public static String getQueryValue(URI uri, String name) {
        List<NameValuePair> params = URLEncodedUtils.parse(uri, "UTF-8");
        for (NameValuePair p : params) {
            if (p.getName().equals(name)) {
                return p.getValue();
            }
        }

        return null;
    }

    public MonUri(String url) {
        try {
            URI uri = new URI(url);
            hostname = uri.getHost();
            String[] ssh = uri.getUserInfo().split(":");
            sshUsername = ssh[0];
            sshPassword = ssh[1];
            sshPort = uri.getPort() == -1 ? sshPort : uri.getPort();

            String v = getQueryValue(uri, CephConstants.MON_PARAM_MON_PORT);
            monPort = v == null ? monPort : Integer.valueOf(v);
        } catch (URISyntaxException e) {
            throw new CloudRuntimeException(e);
        }
    }

    public int getSshPort() {
        return sshPort;
    }

    public void setSshPort(int sshPort) {
        this.sshPort = sshPort;
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
