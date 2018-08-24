package org.zstack.identity;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.header.identity.*;

public class AccessKey {
    @Autowired
    private DatabaseFacade dbf;

    public static AccessKeyInventory createAccessKey(APICreateAccessKeyMsg msg) {

        final AccountAccessKeyVO vo = new AccountAccessKeyVO();
        vo.setUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());
        vo.setAccountUuid(msg.getAccountUuid());
        vo.setUserUuid(msg.getUserUuid());
        vo.setDescription(msg.getDescription());
        /* access key and secret can not include ":" */
        vo.setAccessKeyID(Platform.randomAlphanumeric(AccountConstant.ACCESSKEY_ID_LEN));
        vo.setAccessKeySecret(Platform.randomAlphanumeric(AccountConstant.ACCESSKEY_SECRET_LEN));
        return new SQLBatchWithReturn<AccessKeyInventory>() {
            @Override
            protected AccessKeyInventory scripts() {
                persist(vo);
                return AccessKeyInventory.valueOf(reload(vo));
            }
        }.execute();
    }

    public static AccessKeyInventory getAccessKey(String accessKeyId) {
        AccountAccessKeyVO vo = Q.New(AccountAccessKeyVO.class).eq(AccountAccessKeyVO_.AccessKeyID, accessKeyId).find();
        if (vo != null) {
            return AccessKeyInventory.valueOf(vo);
        } else {
            return null;
        }
    }
}
