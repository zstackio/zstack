package org.zstack.header.identity;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vo.ResourceVO;

import java.util.List;

/**
 * @ Author : yh.w
 * @ Date   : Created in 14:40 2025/8/11
 */
@Action(category = AccountConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/resources/responsible/actions",
        method = HttpMethod.PUT,
        responseClass = APISetResourceResponsibleEvent.class,
        isAction = true
)
public class APISetResourceResponsibleMsg extends APIMessage implements AccountMessage {
    @APIParam(resourceType = ResourceVO.class)
    private String uuid;
    @APIParam
    private List<String> responsibleUuids;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getAccountUuid() {
        return getSession().getAccountUuid();
    }

    public List<String> getResponsibleUuids() {
        return responsibleUuids;
    }

    public void setResponsibleUuids(List<String> responsibleUuids) {
        this.responsibleUuids = responsibleUuids;
    }
}
