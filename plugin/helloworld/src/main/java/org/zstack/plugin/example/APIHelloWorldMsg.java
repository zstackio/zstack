package org.zstack.plugin.example;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(path = "/helloworld/greetings", method = HttpMethod.POST, responseClass = APIHelloWorldEvent.class)
public class APIHelloWorldMsg extends APIMessage {
    @APIParam(maxLength = 255)
    private String greeting;

    public String getGreeting() {
        return greeting;
    }

    public void setGreeting(String greeting) {
        this.greeting = greeting;
    }
}