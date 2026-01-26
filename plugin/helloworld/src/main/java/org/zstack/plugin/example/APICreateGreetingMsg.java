package org.zstack.plugin.example;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;


@RestRequest(path = "/helloworld/greetings", method = HttpMethod.POST, responseClass = APICreateGreetingEvent.class, parameterName = "params")
public class APICreateGreetingMsg extends APICreateMessage {
    @APIParam(emptyString = false)
    private String greeting;

    public String getGreeting() {
        return greeting;
    }

    public void setGreeting(String greeting) {
        this.greeting = greeting;
    }
}
