package org.zstack.header.identity;

import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;

import java.util.List;

/**
 * Created by xing5 on 2016/3/10.
 */
@Action(category = AccountConstant.ACTION_CATEGORY, names = {"read"})
public class APICheckApiPermissionMsg extends APISyncCallMessage {
    @APIParam(required = false, resourceType = UserVO.class)
    private String userUuid;
    @APIParam(nonempty = true)
    private List<String> apiNames;

    public String getUserUuid() {
        return userUuid;
    }

    public void setUserUuid(String userUuid) {
        this.userUuid = userUuid;
    }

    public List<String> getApiNames() {
        return apiNames;
    }

    public void setApiNames(List<String> apiNames) {
        this.apiNames = apiNames;
    }
}
