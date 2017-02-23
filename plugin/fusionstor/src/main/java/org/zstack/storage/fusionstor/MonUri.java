package org.zstack.storage.fusionstor;

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
            if (!allowedQueryParameter.contains(p.getName())) {
                throw new CloudRuntimeException(String.format("unknown parameter[%s]", p.getName()));
            }

            if (p.getName().equals(name)) {
                return p.getValue();
            }
        }

        return null;
    }

    private static final String MON_URL_FORMAT = "sshUsername:sshPassword@hostname:[sshPort]/?[monPort=]";

    private ErrorCode errorCode(String err) {
        ErrorFacade errf = Platform.getComponentLoader().getComponent(ErrorFacade.class);
        return errf.stringToInvalidArgumentError(err);
    }

    public MonUri(String url) {
        try {
            int at = url.lastIndexOf("@");
            if (at == -1) {
                throw new OperationFailureException(errorCode(String.format("invalid monUrl[%s], the sshUsername:sshPassword part is invalid. A valid monUrl is" +
                        " in format of %s", url, MON_URL_FORMAT)));
            }

            String userInfo = url.substring(0, at);
            if (!userInfo.contains(":")) {
                throw new OperationFailureException(errorCode(String.format("invalid monUrl[%s], the sshUsername:sshPassword part is invalid. A valid monUrl is" +
                        " in format of %s", url, MON_URL_FORMAT)));
            }

            String rest = url.substring(at+1, url.length());
            String[] ssh = userInfo.split(":");
            sshUsername = ssh[0];
            sshPassword = ssh[1];

            URI uri = new URI(String.format("ssh://%s", rest));
            hostname = uri.getHost();
            if (hostname == null) {
                throw new OperationFailureException(errorCode(
                        String.format("invalid monUrl[%s], hostname cannot be null. A valid monUrl is" +
                                " in format of %s", url, MON_URL_FORMAT)
                ));
            }

            sshPort = uri.getPort() == -1 ? sshPort : uri.getPort();
            if (sshPort < 1 || sshPort > 65536) {
                throw new OperationFailureException(errorCode(
                        String.format("invalid monUrl[%s], the ssh port is greater than 65536 or lesser than 0. A valid monUrl is" +
                                " in format of %s", url, MON_URL_FORMAT)
                ));
            }
            String v = getQueryValue(uri, FusionstorConstants.MON_PARAM_MON_PORT);
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
