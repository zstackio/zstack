package org.zstack.header.configuration;

import org.zstack.header.message.APIEvent;

/**
 */
public class APIGenerateSqlForeignKeyEvent extends APIEvent {
    public APIGenerateSqlForeignKeyEvent(String apiId) {
        super(apiId);
    }

    public APIGenerateSqlForeignKeyEvent() {
        super(null);
    }
}
