package org.zstack.storage.ceph;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.APIMessage;
import org.zstack.storage.ceph.backup.APIAddCephBackupStorageMsg;
import org.zstack.storage.ceph.backup.APIAddMonToCephBackupStorageMsg;
import org.zstack.storage.ceph.primary.APIAddCephPrimaryStorageMsg;
import org.zstack.storage.ceph.primary.APIAddMonToCephPrimaryStorageMsg;
import org.zstack.utils.URLBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by frank on 7/29/2015.
 */
public class CephApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    private ErrorFacade errf;

    private static final String MON_URL_FOMRAT = "sshUsername:sshPassword@hostname:[sshPort]/?[monPort=]";

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIAddCephBackupStorageMsg) {
            validate((APIAddCephBackupStorageMsg) msg);
        } else if (msg instanceof APIAddCephPrimaryStorageMsg) {
            validate((APIAddCephPrimaryStorageMsg) msg);
        } else if (msg instanceof APIAddMonToCephBackupStorageMsg) {
            validate((APIAddMonToCephBackupStorageMsg) msg);
        } else if (msg instanceof APIAddMonToCephPrimaryStorageMsg) {
            validate((APIAddMonToCephPrimaryStorageMsg) msg);
        }
        
        return msg;
    }

    private void validate(APIAddMonToCephPrimaryStorageMsg msg) {
        msg.setMonUrls(normalizeMonUrls(msg.getMonUrls()));
    }

    private void validate(APIAddMonToCephBackupStorageMsg msg) {
        msg.setMonUrls(normalizeMonUrls(msg.getMonUrls()));
    }

    private List<String> normalizeMonUrls(List<String> monUrls) {
        List<String> urls = new ArrayList<String>();
        for (String monUrl : monUrls) {
            String url = String.format("ssh://%s", monUrl);
            try {
                URI uri = new URI(url);
                String userInfo = uri.getUserInfo();
                if (userInfo == null || !userInfo.contains(":")) {
                    throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                            String.format("invalid monUrl[%s], the sshUsername:sshPassword part is invalid. A valid monUrl is" +
                                    " in format of %s", monUrl, MON_URL_FOMRAT)
                    ));
                }
                if (uri.getHost() == null) {
                    throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                            String.format("invalid monUrl[%s], hostname cannot be null. A valid monUrl is" +
                                    " in format of %s", monUrl, MON_URL_FOMRAT)
                    ));
                }

                try {
                    MonUri.checkQuery(uri);
                } catch (CloudRuntimeException e) {
                    throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                            String.format("invalid monUrl[%s], %s. A valid monUrl is" +
                                    " in format of %s", monUrl, e.getMessage(), MON_URL_FOMRAT)
                    ));
                }

                int sshPort = uri.getPort();
                if (sshPort > 0 && sshPort > 65536) {
                    throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                            String.format("invalid monUrl[%s], the ssh port is greater than 65536. A valid monUrl is" +
                                    " in format of %s", monUrl, MON_URL_FOMRAT)
                    ));
                }

                urls.add(url);
            } catch (Exception e) {
                throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                        String.format("invalid monUrl[%s]. A valid url is in format of %s", monUrl, MON_URL_FOMRAT)
                ));
            }
        }
        return urls;
    }

    private void validate(APIAddCephPrimaryStorageMsg msg) {
        if (msg.getDataVolumePoolName() != null && msg.getDataVolumePoolName().isEmpty()) {
            throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                    "dataVolumePoolName can be null but cannot be an empty string"
            ));
        }
        if (msg.getRootVolumePoolName() != null && msg.getRootVolumePoolName().isEmpty()) {
            throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                    "rootVolumePoolName can be null but cannot be an empty string"
            ));
        }
        if (msg.getImageCachePoolName() != null && msg.getImageCachePoolName().isEmpty()) {
            throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                    "imageCachePoolName can be null but cannot be an empty string"
            ));
        }

        msg.setMonUrls(normalizeMonUrls(msg.getMonUrls()));
    }

    private void validate(APIAddCephBackupStorageMsg msg) {
        if (msg.getPoolName() != null && msg.getPoolName().isEmpty()) {
            throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                    "poolName can be null but cannot be an empty string"
            ));
        }
        msg.setMonUrls(normalizeMonUrls(msg.getMonUrls()));
    }
}
