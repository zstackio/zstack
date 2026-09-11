package org.zstack.storage.zbs;

import org.apache.commons.lang.StringUtils;
import org.springframework.util.CollectionUtils;
import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.message.APIMessage;
import org.zstack.header.storage.addon.primary.APIAddExternalPrimaryStorageMsg;
import org.zstack.header.storage.addon.primary.APIUpdateExternalPrimaryStorageMsg;
import org.zstack.header.storage.addon.primary.ExternalPrimaryStorageVO;
import org.zstack.header.storage.addon.primary.ExternalPrimaryStorageVO_;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.argerr;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_STORAGE_ZBS_10025;

public class ZbsApiInterceptor implements GlobalApiMessageInterceptor {
    @Override
    public List<Class> getMessageClassToIntercept() {
        return Arrays.asList(APIAddExternalPrimaryStorageMsg.class, APIUpdateExternalPrimaryStorageMsg.class);
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.END;
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIAddExternalPrimaryStorageMsg) {
            validate((APIAddExternalPrimaryStorageMsg) msg);
        } else if (msg instanceof APIUpdateExternalPrimaryStorageMsg) {
            validate((APIUpdateExternalPrimaryStorageMsg) msg);
        }
        return msg;
    }

    private void validate(APIAddExternalPrimaryStorageMsg msg) {
        if (ZbsConstants.IDENTITY.equals(msg.getIdentity())) {
            validateMdsAddresses(msg.getConfig(), null);
        }
    }

    private void validate(APIUpdateExternalPrimaryStorageMsg msg) {
        if (msg.getConfig() == null) {
            return;
        }
        String identity = Q.New(ExternalPrimaryStorageVO.class).select(ExternalPrimaryStorageVO_.identity)
                .eq(ExternalPrimaryStorageVO_.uuid, msg.getUuid()).findValue();
        if (ZbsConstants.IDENTITY.equals(identity)) {
            validateMdsAddresses(msg.getConfig(), msg.getUuid());
        }
    }

    private void validateMdsAddresses(String config, String storageUuid) {
        if (StringUtils.isBlank(config)) {
            return;
        }
        Config current = JSONObjectUtil.toObject(config, Config.class);
        if (current == null || CollectionUtils.isEmpty(current.getMdsUrls())) {
            return;
        }
        Set<String> addresses = MdsInfo.valueOf(current.getMdsUrls()).stream()
                .map(MdsInfo::getAddr).collect(Collectors.toSet());
        Q query = Q.New(ExternalPrimaryStorageVO.class).select(ExternalPrimaryStorageVO_.config)
                .eq(ExternalPrimaryStorageVO_.identity, ZbsConstants.IDENTITY);
        if (storageUuid != null) {
            query.notEq(ExternalPrimaryStorageVO_.uuid, storageUuid);
        }
        for (String existingConfig : query.<String>listValues()) {
            Config existing = JSONObjectUtil.toObject(existingConfig, Config.class);
            for (MdsInfo mds : MdsInfo.valueOf(existing.getMdsUrls())) {
                if (addresses.contains(mds.getAddr())) {
                    throw new ApiMessageInterceptionException(argerr(ORG_ZSTACK_STORAGE_ZBS_10025,
                            "do not allow to add duplicate MDS[%s]", mds.getAddr()));
                }
            }
        }
    }
}
