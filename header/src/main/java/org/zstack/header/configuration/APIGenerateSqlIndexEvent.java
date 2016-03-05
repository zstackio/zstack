package org.zstack.header.configuration;

import org.zstack.header.message.APIEvent;

/**
 */
public class APIGenerateSqlIndexEvent extends APIEvent {
    public APIGenerateSqlIndexEvent(String apiId) {
        super(apiId);
    }

    public APIGenerateSqlIndexEvent() {
        super(null);
    }
}
