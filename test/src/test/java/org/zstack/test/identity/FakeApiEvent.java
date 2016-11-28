package org.zstack.test.identity;

import org.zstack.header.message.APIEvent;

public class FakeApiEvent extends APIEvent {
    public FakeApiEvent(String apiId) {
        super(apiId);
    }

    public FakeApiEvent() {
        super(null);
    }
}
