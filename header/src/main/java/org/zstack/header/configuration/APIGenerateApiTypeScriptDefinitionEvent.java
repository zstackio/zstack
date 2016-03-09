package org.zstack.header.configuration;

import org.zstack.header.message.APIEvent;

/**
 */
public class APIGenerateApiTypeScriptDefinitionEvent extends APIEvent {
    public APIGenerateApiTypeScriptDefinitionEvent(String apiId) {
        super(apiId);
    }

    public APIGenerateApiTypeScriptDefinitionEvent() {
        super(null);
    }
}
