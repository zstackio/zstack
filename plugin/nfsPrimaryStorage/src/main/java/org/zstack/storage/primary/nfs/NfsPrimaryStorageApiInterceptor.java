package org.zstack.storage.primary.nfs;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.APIMessage;
import org.zstack.storage.primary.PrimaryStorageSystemTags;
import org.zstack.utils.network.NetworkUtils;

import java.util.List;

/**
 */
public class NfsPrimaryStorageApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIAddNfsPrimaryStorageMsg) {
            validate((APIAddNfsPrimaryStorageMsg) msg);
        }

        return msg;
    }

    private void validate(APIAddNfsPrimaryStorageMsg msg) {
        ErrorCode err = new NfsApiParamChecker().checkUrl(msg.getZoneUuid(), msg.getUrl());
        if (err != null) {
            throw new ApiMessageInterceptionException(err);
        }

        List<String> systemTags = msg.getSystemTags();
        if (systemTags != null) {
            boolean found = false;
            for (String sysTag: systemTags) {
                if (PrimaryStorageSystemTags.PRIMARY_STORAGE_GATEWAY.isMatch(sysTag)) {
                    if (found) {
                        throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError("found multiple CIDR"));
                    }

                    validateCidrTag(sysTag);
                    found = true;
                }
            }
        }
    }

    private void validateCidrTag(String sysTag) {
        String cidr = PrimaryStorageSystemTags.PRIMARY_STORAGE_GATEWAY.getTokenByTag(
                sysTag, PrimaryStorageSystemTags.PRIMARY_STORAGE_GATEWAY_TOKEN);
        if (!NetworkUtils.isCidr(cidr)) {
            throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                    String.format("invalid CIDR: %s", cidr)));
        }
    }
}
