package org.zstack.core.config;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/global-configurations/{category}/{name}/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIUpdateGlobalConfigEvent.class
)
public class APIUpdateGlobalConfigMsg extends APIMessage {
    @APIParam
    private String category;
    @APIParam
    private String name;
    @APIParam(required = false)
    private String value;

    public String getIdentity() {
        return GlobalConfig.produceIdentity(category, name);
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public static APIUpdateGlobalConfigMsg __example__() {
        APIUpdateGlobalConfigMsg msg = new APIUpdateGlobalConfigMsg();
        msg.setCategory("quota");
        msg.setName("scheduler.num");
        msg.setValue("90");
        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;
        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                ntfy("Updating a global configuration[category:%s, name:%s]", category, name)
                        .resource(null, GlobalConfigVO.class.getSimpleName())
                        .messageAndEvent(that, evt).done();
            }
        };
    }
}
