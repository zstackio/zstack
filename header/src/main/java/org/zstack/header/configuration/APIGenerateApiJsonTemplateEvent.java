package org.zstack.header.configuration;

import org.zstack.header.message.APIEvent;

public class APIGenerateApiJsonTemplateEvent extends APIEvent {
    public APIGenerateApiJsonTemplateEvent(String apiId) {
        super(apiId);
    }
    
    public APIGenerateApiJsonTemplateEvent() {
        super(null);
    }
}
