package org.zstack.header.core.encrypt;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @Author: DaoDao
 * @Date: 2021/10/28
 */
@RestResponse
public class APIStartDataProtectionEvent extends APIEvent {

    public APIStartDataProtectionEvent() {
    }

    public APIStartDataProtectionEvent(String apiId) {
        super(apiId);
    }
    public static APIStartDataProtectionEvent __example__() {
        APIStartDataProtectionEvent ret = new APIStartDataProtectionEvent();
        return ret;
    }
}
