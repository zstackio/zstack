package org.zstack.storage.primary.nfs;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.message.APIMessage;
import org.zstack.header.storage.primary.APIUpdatePrimaryStorageMsg;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.storage.primary.PrimaryStorageSystemTags;
import org.zstack.utils.network.NetworkUtils;

import javax.persistence.Tuple;
import java.util.List;

import static org.zstack.core.Platform.argerr;


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
        }else if(msg instanceof APIUpdatePrimaryStorageMsg){
            validate((APIUpdatePrimaryStorageMsg) msg);
        }

        return msg;
    }

    private void validate(APIAddNfsPrimaryStorageMsg msg) {
        new NfsApiParamChecker().checkUrl(msg.getZoneUuid(), msg.getUrl());
        String[] results = msg.getUrl().split(":");
        if (results.length == 2 && (
                results[1].startsWith("/dev") || results[1].startsWith("/proc") || results[1].startsWith("/sys"))) {
            throw new ApiMessageInterceptionException(argerr(" the url contains an invalid folder[/dev or /proc or /sys]"));
        }
    }

    private void validate(APIUpdatePrimaryStorageMsg msg){
        Tuple t = Q.New(PrimaryStorageVO.class)
                .select(PrimaryStorageVO_.type, PrimaryStorageVO_.url, PrimaryStorageVO_.zoneUuid)
                .eq(PrimaryStorageVO_.uuid, msg.getUuid())
                .findTuple();

        String type = t.get(0, String.class);
        String url = t.get(1, String.class);
        String zoneUuid = t.get(2, String.class);
        if(!type.equals(NfsPrimaryStorageConstant.NFS_PRIMARY_STORAGE_TYPE)){
            return;
        }

        if (msg.getUrl() != null && !msg.getUrl().equals(url)){
            new SQLBatch(){
                @Override
                protected void scripts() {
                    NfsApiParamChecker checker = new NfsApiParamChecker();
                    checker.checkUrl(zoneUuid, msg.getUrl());
                    checker.checkRunningVmForUpdateUrl(msg.getUuid());
                }
            }.execute();
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
