package org.zstack.header.configuration;

import org.zstack.header.message.APIEvent;

/**
 */
public class APIGenerateSqlVOViewEvent extends APIEvent {
    public APIGenerateSqlVOViewEvent() {
        super(null);
    }

    public APIGenerateSqlVOViewEvent(String apiId) {
        super(apiId);
    }
}
