package org.zstack.storage.ceph;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.message.APIMessage;
import org.zstack.storage.ceph.backup.APIAddCephBackupStorageMsg;
import org.zstack.storage.ceph.primary.APIAddCephPrimaryStorageMsg;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by frank on 7/29/2015.
 */
public class CephApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    private ErrorFacade errf;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIAddCephBackupStorageMsg) {
            validate((APIAddCephBackupStorageMsg) msg);
        } else if (msg instanceof APIAddCephPrimaryStorageMsg) {
            validate((APIAddCephPrimaryStorageMsg) msg);
        }
        
        return msg;
    }

    private List<String> normalizeMonUrls(List<String> monUrls) {
        List<String> urls = new ArrayList<String>();
        for (String monUrl : monUrls) {
            String url = String.format("ssh://%s", monUrl);
            try {
                new URI(url);
                urls.add(url);
            } catch (Exception e) {
                throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                        String.format("invalid monUrl[%s]. A valid url is in format of sshUsername:sshPassword@hostname:hostPort", monUrl)
                ));
            }
        }
        return urls;
    }

    private void validate(APIAddCephPrimaryStorageMsg msg) {
        msg.setMonUrls(normalizeMonUrls(msg.getMonUrls()));
    }

    private void validate(APIAddCephBackupStorageMsg msg) {
        msg.setMonUrls(normalizeMonUrls(msg.getMonUrls()));
    }
}
