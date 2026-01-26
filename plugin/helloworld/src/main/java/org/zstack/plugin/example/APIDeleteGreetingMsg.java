package org.zstack.plugin.example;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.rest.RestRequest;

@RestRequest(path = "/helloworld/greetings/{uuid}", method = HttpMethod.DELETE, responseClass = APIDeleteGreetingEvent.class)
public class APIDeleteGreetingMsg extends APIDeleteMessage implements GreetingMessage {
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getGreetingUuid() {
        return uuid;
    }
}
