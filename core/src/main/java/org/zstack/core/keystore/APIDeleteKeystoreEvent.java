package org.zstack.core.keystore;

import org.zstack.header.message.APIEvent;

/**
 * Created by miao on 16-8-15.
 */
public class APIDeleteKeystoreEvent extends APIEvent {
    public APIDeleteKeystoreEvent(String apiId) {
        super(apiId);
    }

    public APIDeleteKeystoreEvent() {
        super(null);
    }
}
