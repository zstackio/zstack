package org.zstack.storage.surfs;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.zstack.core.Platform;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.keyvalue.Op;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;

import static org.zstack.core.Platform.argerr;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by zhouhaiping 2017-09-05
 */
public class NodeUri {
    private String hostname;
    private int nodePort = 6543;
    private String sshUsername;
    private String sshPassword;
    private int sshPort = 22;

    private static List<String> allowedQueryParameter;
    static {
        allowedQueryParameter = list("nodePort");
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
            if (!allowedQueryParameter.contains(p.getName())) {
                throw new CloudRuntimeException(String.format("unknown parameter[%s]", p.getName()));
            }

            if (p.getName().equals(name)) {
                return p.getValue();
            }
        }

        return null;
    }

    private static final String NODE_URL_FORMAT = "sshUsername:sshPassword@hostname:[sshPort]/?[monPort=]";

    private ErrorCode errorCode(String err) {
        return argerr(err);
    }

    public NodeUri(String url) {
        try {
            int at = url.lastIndexOf("@");
            if (at == -1) {
                throw new OperationFailureException(errorCode(String.format("invalid nodeUrl[%s], the sshUsername:sshPassword part is invalid. A valid nodeUrl is" +
                        " in format of %s", url, NODE_URL_FORMAT)));
            }

            String userInfo = url.substring(0, at);
            if (!userInfo.contains(":")) {
                throw new OperationFailureException(errorCode(String.format("invalid nodeUrl[%s], the sshUsername:sshPassword part is invalid. A valid nodeUrl is" +
                        " in format of %s", url, NODE_URL_FORMAT)));
            }

            String rest = url.substring(at+1, url.length());
            String[] ssh = userInfo.split(":");
            sshUsername = ssh[0];
            sshPassword = ssh[1];

            URI uri = new URI(String.format("ssh://%s", rest));
            hostname = uri.getHost();
            if (hostname == null) {
                throw new OperationFailureException(errorCode(
                        String.format("invalid nodeUrl[%s], hostname cannot be null. A valid nodeUrl is" +
                                " in format of %s", url, NODE_URL_FORMAT)
                ));
            }

            sshPort = uri.getPort() == -1 ? sshPort : uri.getPort();
            if (sshPort < 1 || sshPort > 65535) {
                throw new OperationFailureException(errorCode(
                        String.format("invalid nodeUrl[%s], the ssh port is greater than 65535 or smaller than 1. A valid nodeUrl is" +
                                " in format of %s", url, NODE_URL_FORMAT)
                ));
            }
            String v = getQueryValue(uri, SurfsConstants.NODE_PARAM_NODE_PORT);
            nodePort = v == null ? nodePort : Integer.valueOf(v);
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

    public int getNodePort() {
        return nodePort;
    }

    public void setNodePort(int nodePort) {
        this.nodePort = nodePort;
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
