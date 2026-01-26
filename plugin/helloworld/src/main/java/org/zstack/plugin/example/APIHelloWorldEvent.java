package org.zstack.plugin.example;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "greeting")
public class APIHelloWorldEvent extends APIEvent {
    private String greeting;

    public APIHelloWorldEvent() {
    }

    public APIHelloWorldEvent(String apiId) {
        super(apiId);
    }

    public String getGreeting() {
        return greeting;
    }

    public void setGreeting(String greeting) {
        this.greeting = greeting;
    }
}