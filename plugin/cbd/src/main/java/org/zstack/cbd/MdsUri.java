package org.zstack.cbd;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static org.zstack.core.Platform.argerr;
import static org.zstack.utils.CollectionDSL.list;

/**
 * @author Xingwei Yu
 * @date 2024/3/21 13:10
 */
public class MdsUri {
    private String hostname;
    private int mdsPort = DEFAULT_MDS_PORT;
    private int sshPort = DEFAULT_SSH_PORT;
    private String sshUsername;
    private String sshPassword;

    private static final String MDS_URL_FORMAT = "sshUsername:sshPassword@hostname:[sshPort]/?[mdsPort=]";
    private static final Integer DEFAULT_MDS_PORT = 6666;
    private static final Integer DEFAULT_SSH_PORT = 22;

    private static List<String> allowedQueryParameter;
    static {
        allowedQueryParameter = list(CbdConstants.MDS_PARAM_MDS_PORT);
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

    private ErrorCode errorCode(String err) {
        return argerr(err);
    }

    public MdsUri(String url) {
        try {
            int at = url.lastIndexOf("@");
            if (at == -1) {
                throw new OperationFailureException(errorCode(String.format("invalid mdsUrl[%s], the sshUsername:sshPassword part is invalid. A valid mdsUrl is" +
                        " in format of %s", url, MDS_URL_FORMAT)));
            }

            String userInfo = url.substring(0, at);
            if (!userInfo.contains(":")) {
                throw new OperationFailureException(errorCode(String.format("invalid mdsUrl[%s], the sshUsername:sshPassword part is invalid. A valid mdsUrl is" +
                        " in format of %s", url, MDS_URL_FORMAT)));
            }

            String rest = url.substring(at+1, url.length());
            String[] ssh = userInfo.split(":");
            sshUsername = ssh[0];
            sshPassword = ssh[1];

            URI uri = new URI(String.format("ssh://%s", rest));
            hostname = uri.getHost();
            if (hostname == null) {
                throw new OperationFailureException(errorCode(
                        String.format("invalid mdsUrl[%s], hostname cannot be null. A valid mdsUrl is" +
                                " in format of %s", url, MDS_URL_FORMAT)
                ));
            }

            sshPort = uri.getPort() == -1 ? sshPort : uri.getPort();
            if (sshPort < 1 || sshPort > 65535) {
                throw new OperationFailureException(errorCode(
                        String.format("invalid mdsUrl[%s], the ssh port is greater than 65535 or smaller than 1. A valid mdsUrl is" +
                                " in format of %s", url, MDS_URL_FORMAT)
                ));
            }
            String v = getQueryValue(uri, CbdConstants.MDS_PARAM_MDS_PORT);
            mdsPort = v == null ? mdsPort : Integer.parseInt(v);
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

    public int getMdsPort() {
        return mdsPort;
    }

    public void setMdsPort(int mdsPort) {
        this.mdsPort = mdsPort;
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

    public int getSshPort() {
        return sshPort;
    }

    public void setSshPort(int sshPort) {
        this.sshPort = sshPort;
    }
}
